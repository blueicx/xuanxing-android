package com.xuanji.app.data.local

import com.xuanji.app.domain.LocalBackupSelection
import com.xuanji.app.domain.ReadingFavorite
import com.xuanji.app.domain.action.ActionFeedback
import com.xuanji.app.domain.action.ActionFeedbackKind
import com.xuanji.app.domain.MysticCharacterId
import com.xuanji.app.domain.game.CompanionGameProgressState
import com.xuanji.app.domain.game.GameSave
import com.xuanji.app.domain.game.PoetryChainState
import com.xuanji.app.domain.game.XiangqiBoard
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalDataBackupRepositoryTest {
    @Test fun journal_conflicts_are_opt_in_confirmed_and_legacy_restore_preserves_local_entries() = runTest {
        val bridge = MemoryBridge()
        val life = DailyLifeStore(bridge)
        val keys = LocalBackupProfileKeys("fav", "local")
        val repository = LocalDataBackupRepository(ReadingFavoriteStore(bridge), ActionFeedbackDataStore(bridge),
            CompanionGameProgressStore(bridge), MemoryXiangqiStore(), atomicBridge = bridge, dailyLifeStore = life)
        val local = com.xuanji.app.domain.DailyJournalEntry("2026-10-19", "本机日记")
        val incoming = local.copy(text = "明确选择的备份日记")
        life.saveJournal(keys.localProfileKey, local)
        val selected = LocalBackupSelection(agenda = true, notes = true)
        val json = com.xuanji.app.domain.LocalDataBackupCodec.encode(keys.fingerprintKey, selected,
            com.xuanji.app.domain.LocalBackupPayload(journal = listOf(incoming)), 1)
        val preview = repository.preview(json, keys)
        assertEquals(setOf("journal:2026-10-19"), preview.conflicts)
        assertEquals(LocalBackupRestoreStatus.ConfirmationRequired, repository.restore(json, keys, selected, emptySet()).status)
        assertEquals(local, life.load(keys.localProfileKey).journal.single())
        assertEquals(LocalBackupRestoreStatus.Completed, repository.restore(json, keys, LocalBackupSelection(agenda = true), emptySet()).status)
        assertEquals(local, life.load(keys.localProfileKey).journal.single())
        assertEquals(LocalBackupRestoreStatus.Completed, repository.restore(json, keys, selected, preview.conflicts).status)
        assertEquals(incoming, life.load(keys.localProfileKey).journal.single())
        val root = com.google.gson.JsonParser.parseString(json).asJsonObject
        root.addProperty("version", 6)
        root.getAsJsonObject("payload").remove("journal")
        root.remove("checksum")
        root.addProperty("checksum", com.xuanji.app.domain.action.StableScore.sha256(root.toString()))
        assertEquals(LocalBackupRestoreStatus.Completed, repository.restore(root.toString(), keys, selected, emptySet()).status)
        assertEquals(incoming, life.load(keys.localProfileKey).journal.single())
    }
    private data class Fixture(
        val repository: LocalDataBackupRepository,
        val favorites: ReadingFavoriteStore,
        val feedback: ActionFeedbackDataStore,
        val games: CompanionGameProgressStore
    )
    private class MemoryBridge : PreferenceBridge {
        val values = mutableMapOf<String, String>()
        override suspend fun read(key: String): String? = values[key]
        override suspend fun write(key: String, value: String) { values[key] = value }
        override suspend fun delete(key: String) { values.remove(key) }
    }

    private class MemoryXiangqiStore : XiangqiSaveBackupStore {
        var save: GameSave? = null
        override suspend fun load(profileKey: String): GameSave? = save
        override suspend fun save(profileKey: String, save: GameSave) { this.save = save }
    }

    @Test
    fun preview_is_read_only_and_restore_requires_explicit_confirmation_for_game_overwrites() = runTest {
        val keys = LocalBackupProfileKeys("favorite-profile", "local-profile")
        val exportBridge = MemoryBridge()
        val exportXiangqi = MemoryXiangqiStore()
        val exporting = fixture(exportBridge, exportXiangqi)
        exporting.favorites.upsert(keys.favoriteProfileKey, favorite("fav-1", "portable favorite"))
        exporting.feedback.append(keys.localProfileKey, feedback("feedback-1", "oat", 11L))
        exporting.games.save(keys.localProfileKey, MysticCharacterId.ShenYanzhou, CompanionGameProgressState.Poetry(PoetryChainState()))
        exportXiangqi.save = xiangqiSave("portable board")
        val json = exporting.repository.export(keys, LocalBackupSelection(true, true, true, true), 50L).json!!

        val importBridge = MemoryBridge()
        val importXiangqi = MemoryXiangqiStore().apply { save = xiangqiSave("existing board") }
        val importing = fixture(importBridge, importXiangqi)
        importing.favorites.upsert(keys.favoriteProfileKey, favorite("fav-1", "keep until merge"))
        importing.favorites.upsert(keys.favoriteProfileKey, favorite("fav-other", "unrelated favorite"))
        importing.feedback.append(keys.localProfileKey, feedback("feedback-1", "old-candidate", 10L))
        importing.games.save(
            keys.localProfileKey, MysticCharacterId.ShenYanzhou,
            CompanionGameProgressState.Poetry(PoetryChainState(promptIndex = 1, selected = "灯影落江中", score = 1))
        )
        val before = importBridge.values.toMap()

        val preview = importing.repository.preview(json, keys)
        assertTrue(preview.valid)
        assertEquals(setOf("favorite:fav-1", "feedback:feedback-1", "shen_yanzhou:poetry_chain", "xiangqi"), preview.conflicts)
        assertEquals(before, importBridge.values)

        val needsConfirmation = importing.repository.restore(json, keys, LocalBackupSelection(true, true, true, true), emptySet())
        assertEquals(LocalBackupRestoreStatus.ConfirmationRequired, needsConfirmation.status)
        assertEquals(before, importBridge.values)

        val restored = importing.repository.restore(
            json, keys, LocalBackupSelection(true, true, true, true),
            setOf("favorite:fav-1", "feedback:feedback-1", "shen_yanzhou:poetry_chain", "xiangqi")
        )
        assertEquals(LocalBackupRestoreStatus.Completed, restored.status)
        assertEquals("portable favorite", importing.favorites.list(keys.favoriteProfileKey).entries.single { it.id == "fav-1" }.title)
        assertTrue(importing.favorites.list(keys.favoriteProfileKey).entries.any { it.id == "fav-other" })
        assertEquals("oat", importing.feedback.list(keys.localProfileKey).single { it.id == "feedback-1" }.candidateKey)
        assertEquals(0, (importing.games.load(keys.localProfileKey, MysticCharacterId.ShenYanzhou, "poetry_chain") as CompanionGameProgressState.Poetry).value.promptIndex)
        assertEquals("portable board", importXiangqi.save?.title)
    }

    @Test
    fun profile_mismatch_and_unselected_sections_never_write_anything() = runTest {
        val keys = LocalBackupProfileKeys("favorite-profile", "local-profile")
        val source = fixture(MemoryBridge(), MemoryXiangqiStore())
        source.favorites.upsert(keys.favoriteProfileKey, favorite("fav-1", "portable favorite"))
        val json = source.repository.export(keys, LocalBackupSelection(favorites = true), 50L).json!!

        val targetBridge = MemoryBridge()
        val target = fixture(targetBridge, MemoryXiangqiStore())
        assertFalse(target.repository.preview(json, LocalBackupProfileKeys("other-favorite", "other-local")).valid)
        val before = targetBridge.values.toMap()
        val result = target.repository.restore(json, keys, LocalBackupSelection(actionFeedback = true), emptySet())
        assertEquals(LocalBackupRestoreStatus.InvalidBackup, result.status)
        assertEquals(before, targetBridge.values)
    }

    @Test
    fun encrypted_backup_requires_password_and_never_writes_before_authenticated_restore() = runTest {
        val keys = LocalBackupProfileKeys("favorite-profile", "local-profile")
        val source = fixture(MemoryBridge(), MemoryXiangqiStore())
        source.favorites.upsert(keys.favoriteProfileKey, favorite("fav-secret", "private snapshot"))
        val encrypted = source.repository.export(
            keys, LocalBackupSelection(favorites = true), exportedAtEpochMs = 80L,
            passphrase = "long-local-backup-pass".toCharArray()
        ).json!!

        val targetBridge = MemoryBridge()
        val target = fixture(targetBridge, MemoryXiangqiStore())
        val noPassword = target.repository.preview(encrypted, keys)
        val wrongPassword = target.repository.preview(encrypted, keys, "different-local-password".toCharArray())

        assertEquals(LocalBackupRepositoryError.PasswordRequired, noPassword.error)
        assertEquals(LocalBackupRepositoryError.EncryptedBackupInvalid, wrongPassword.error)
        assertTrue(targetBridge.values.isEmpty())
        val restored = target.repository.restore(
            encrypted, keys, LocalBackupSelection(favorites = true), emptySet(), "long-local-backup-pass".toCharArray()
        )
        assertEquals(LocalBackupRestoreStatus.Completed, restored.status)
        assertEquals("private snapshot", target.favorites.list(keys.favoriteProfileKey).entries.single().title)
    }

    private fun fixture(bridge: MemoryBridge, xiangqi: MemoryXiangqiStore): Fixture {
        val favorites = ReadingFavoriteStore(bridge)
        val feedback = ActionFeedbackDataStore(bridge)
        val games = CompanionGameProgressStore(bridge) { 12L }
        return Fixture(LocalDataBackupRepository(favorites, feedback, games, xiangqi), favorites, feedback, games)
    }

    private fun favorite(id: String, title: String) = ReadingFavorite(
        id = id, type = "fortune", title = title, body = "portable result", createdAt = 10L
    )

    private fun feedback(id: String, candidate: String, at: Long) = ActionFeedback(
        id = id, dateKey = "2026-10-03", category = "meal", candidateKey = candidate,
        kind = ActionFeedbackKind.Accepted, createdAtEpochMs = at
    )

    private fun xiangqiSave(title: String) = GameSave(start = XiangqiBoard.encode(XiangqiBoard.initial()), title = title)
}
