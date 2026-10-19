package com.xuanji.app.domain

import com.xuanji.app.domain.action.ActionFeedback
import com.xuanji.app.domain.action.ActionFeedbackKind
import com.xuanji.app.domain.game.GameSave
import com.xuanji.app.domain.game.CompanionGameProgressCodec
import com.xuanji.app.domain.game.CompanionGameProgressState
import com.xuanji.app.domain.game.PoetryChainState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalDataBackupCodecTest {
    @Test
    fun round_trips_only_selected_sections_and_never_exports_profile_or_conversation_data() {
        val selection = LocalBackupSelection(favorites = true, actionFeedback = false, companionGames = true, xiangqiSave = false)
        val favorite = favorite("fav-1")
        val payload = LocalBackupPayload(
            favorites = listOf(favorite),
            actionFeedback = listOf(feedback()),
            companionGames = listOf(CompanionGameBackup(
                "shen_yanzhou", "poetry_chain",
                CompanionGameProgressCodec.encodeState(CompanionGameProgressState.Poetry(PoetryChainState())), 10L
            )),
            xiangqiSave = GameSave(title = "不应导出")
        )

        val encoded = LocalDataBackupCodec.encode("private-profile-key", selection, payload, exportedAtEpochMs = 50L)
        val decoded = LocalDataBackupCodec.decode(encoded, "private-profile-key")

        assertNotNull(decoded.backup)
        assertEquals(listOf(favorite), decoded.backup!!.payload.favorites)
        assertTrue(decoded.backup!!.payload.actionFeedback.isEmpty())
        assertEquals(1, decoded.backup!!.payload.companionGames.size)
        assertNull(decoded.backup!!.payload.xiangqiSave)
        assertFalse(encoded.contains("private-profile-key"))
        assertFalse(encoded.contains("birthYear"))
        assertFalse(encoded.contains("conversation"))
        assertFalse(encoded.contains("longTermMemory"))
        assertFalse(encoded.contains("scoreboard"))
    }

    @Test
    fun checksum_profile_fingerprint_and_truncation_are_checked_without_partial_payload() {
        val encoded = LocalDataBackupCodec.encode(
            "profile-a", LocalBackupSelection(favorites = true),
            LocalBackupPayload(favorites = listOf(favorite("fav-1"))), 100L
        )
        val damaged = encoded.replace("快照正文", "篡改正文")

        assertNull(LocalDataBackupCodec.decode(damaged, "profile-a").backup)
        assertEquals(LocalBackupDecodeError.ChecksumMismatch, LocalDataBackupCodec.decode(damaged, "profile-a").error)
        assertEquals(LocalBackupDecodeError.ProfileMismatch, LocalDataBackupCodec.decode(encoded, "profile-b").error)
        assertEquals(LocalBackupDecodeError.Malformed, LocalDataBackupCodec.decode("{broken", "profile-a").error)
        assertEquals(LocalBackupDecodeError.Malformed, LocalDataBackupCodec.decode(encoded.take(12), "profile-a").error)
    }

    @Test
    fun duplicate_or_unselected_entries_are_rejected_or_stripped_deterministically() {
        val selection = LocalBackupSelection(favorites = true, actionFeedback = false)
        val encoded = LocalDataBackupCodec.encode(
            "profile-a", selection,
            LocalBackupPayload(favorites = listOf(favorite("same"), favorite("same")), actionFeedback = listOf(feedback())), 1L
        )
        val decoded = LocalDataBackupCodec.decode(encoded, "profile-a")

        assertNotNull(decoded.backup)
        assertEquals(1, decoded.backup!!.payload.favorites.size)
        assertTrue(decoded.backup!!.payload.actionFeedback.isEmpty())
    }

    private fun favorite(id: String) = ReadingFavorite(
        id = id, type = "fortune", title = "测试收藏", body = "快照正文", createdAt = 10L
    )

    private fun feedback() = ActionFeedback(
        id = "feedback-1", dateKey = "2026-10-03", category = "meal", candidateKey = "oat", kind = ActionFeedbackKind.Accepted
    )
}
