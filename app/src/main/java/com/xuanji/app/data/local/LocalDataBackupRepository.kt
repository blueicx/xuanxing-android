package com.xuanji.app.data.local

import com.xuanji.app.domain.CompanionGameBackup
import com.xuanji.app.domain.LocalBackupDecodeError
import com.xuanji.app.domain.LocalBackupPayload
import com.xuanji.app.domain.LocalBackupSelection
import com.xuanji.app.domain.LocalDataBackup
import com.xuanji.app.domain.LocalDataBackupCodec
import com.xuanji.app.domain.EncryptedLocalBackupCodec
import com.xuanji.app.domain.EncryptedLocalBackupError
import com.xuanji.app.domain.MysticCharacterId
import com.xuanji.app.domain.ReadingFavoriteDataState
import com.xuanji.app.domain.game.CompanionGameCatalog
import com.xuanji.app.domain.game.CompanionGameProgressCodec
import com.xuanji.app.domain.game.CompanionGameProgressEnvelope
import com.xuanji.app.domain.game.CompanionGameProgressState
import com.xuanji.app.domain.DailyLifeDocument
import com.xuanji.app.domain.DailyLifeCodec
import com.xuanji.app.domain.PantryRules
import com.xuanji.app.domain.game.GameSave
import com.google.gson.Gson

data class LocalBackupProfileKeys(val favoriteProfileKey: String, val localProfileKey: String) {
    val fingerprintKey: String get() = "$favoriteProfileKey\u001f$localProfileKey"
    val valid: Boolean get() = favoriteProfileKey.isNotBlank() && localProfileKey.isNotBlank()
}

enum class LocalBackupRepositoryError {
    InvalidProfile, NoSectionsSelected, CorruptFavorites, CorruptFeedback, InvalidBackup,
    ProfileMismatch, SelectionNotIncluded, ConfirmationRequired, WriteFailed,
    PasswordRequired, PasswordTooShort, EncryptedBackupInvalid
}

data class LocalBackupExportResult(val json: String? = null, val error: LocalBackupRepositoryError? = null)

data class LocalBackupImportPreview(
    val backup: LocalDataBackup? = null,
    val conflicts: Set<String> = emptySet(),
    val error: LocalBackupRepositoryError? = null,
    val items: List<LocalBackupPreviewItem> = emptyList()
) {
    val valid: Boolean get() = backup != null && error == null
    val favoriteCount: Int get() = backup?.payload?.favorites?.size ?: 0
    val feedbackCount: Int get() = backup?.payload?.actionFeedback?.size ?: 0
    val companionGameCount: Int get() = backup?.payload?.companionGames?.size ?: 0
    val hasXiangqiSave: Boolean get() = backup?.payload?.xiangqiSave != null
    val agendaCount: Int get() = backup?.payload?.agenda?.size ?: 0
    val snapshotCount: Int get() = backup?.payload?.snapshots?.size ?: 0
}

enum class LocalBackupRestoreStatus { Completed, ConfirmationRequired, InvalidBackup, Failed }

data class LocalBackupRestoreResult(
    val status: LocalBackupRestoreStatus,
    val importedFavorites: Int = 0,
    val importedFeedback: Int = 0,
    val importedGames: Int = 0,
    val importedXiangqi: Boolean = false,
    val conflicts: Set<String> = emptySet(),
    val error: LocalBackupRepositoryError? = null
)

/** Explicit local backup/restore coordinator. Preview is read-only; import runs only on confirm. */
class LocalDataBackupRepository(
    private val favoriteStore: ReadingFavoriteStore,
    private val feedbackStore: ActionFeedbackDataStore,
    private val gameProgressStore: CompanionGameProgressStore,
    private val xiangqiStore: XiangqiSaveBackupStore,
    private val atomicBridge: PreferenceBridge? = null,
    private val dailyLifeStore: DailyLifeStore? = null,
    private val foodPreferenceStore: FoodPreferenceStore? = null,
    private val activityPreferenceStore: ActionConstraintStore? = null
) {
    suspend fun export(
        keys: LocalBackupProfileKeys,
        selection: LocalBackupSelection,
        exportedAtEpochMs: Long = System.currentTimeMillis(),
        passphrase: CharArray? = null
    ): LocalBackupExportResult {
        if (!keys.valid) return LocalBackupExportResult(error = LocalBackupRepositoryError.InvalidProfile)
        if (selection == LocalBackupSelection()) return LocalBackupExportResult(error = LocalBackupRepositoryError.NoSectionsSelected)
        if (passphrase != null && passphrase.size < EncryptedLocalBackupCodec.MIN_PASSWORD_LENGTH) {
            return LocalBackupExportResult(error = LocalBackupRepositoryError.PasswordTooShort)
        }
        val life = if (selection.agenda || selection.snapshots) {
            val store = dailyLifeStore ?: return LocalBackupExportResult(error = LocalBackupRepositoryError.InvalidBackup)
            runCatching { store.load(keys.localProfileKey) }.getOrNull()?.takeIf { it.readable }
                ?: return LocalBackupExportResult(error = LocalBackupRepositoryError.InvalidBackup)
        } else DailyLifeDocument()
        val gameBackups = mutableListOf<CompanionGameBackup>()
        if (selection.companionGames) {
            for (profile in CompanionGameCatalog.all.filter { it.id != "xiangqi" }) {
                val envelope = gameProgressStore.loadEnvelope(keys.localProfileKey, profile.characterId, profile.id)
                if (envelope != null) {
                    gameBackups += CompanionGameBackup(envelope.characterId, envelope.gameId, envelope.stateJson, envelope.updatedAt)
                }
            }
        }
        val payload = LocalBackupPayload(
            favorites = if (selection.favorites) {
                val snapshot = runCatching { favoriteStore.list(keys.favoriteProfileKey) }.getOrNull()
                    ?: return LocalBackupExportResult(error = LocalBackupRepositoryError.CorruptFavorites)
                if (snapshot.state !in setOf(ReadingFavoriteDataState.Ready, ReadingFavoriteDataState.Migrated, ReadingFavoriteDataState.Missing)) {
                    return LocalBackupExportResult(error = LocalBackupRepositoryError.CorruptFavorites)
                }
                snapshot.entries
            } else emptyList(),
            actionFeedback = if (selection.actionFeedback) {
                if (!runCatching { feedbackStore.isHealthy(keys.localProfileKey) }.getOrDefault(false)) {
                    return LocalBackupExportResult(error = LocalBackupRepositoryError.CorruptFeedback)
                }
                runCatching { feedbackStore.list(keys.localProfileKey, 100) }
                    .getOrElse { return LocalBackupExportResult(error = LocalBackupRepositoryError.CorruptFeedback) }
            } else emptyList(),
            companionGames = gameBackups,
            xiangqiSave = if (selection.xiangqiSave) runCatching { xiangqiStore.load(keys.localProfileKey) }
                .getOrElse { return LocalBackupExportResult(error = LocalBackupRepositoryError.WriteFailed) } else null,
            foodPreference = if (selection.foodPreferences) foodPreferenceStore?.readSnapshot(keys.localProfileKey)?.takeUnless { it.unreadable }?.preference
                ?: return LocalBackupExportResult(error = LocalBackupRepositoryError.InvalidBackup) else null,
            activityPreference = if (selection.activityPreferences) activityPreferenceStore?.readSnapshot(keys.localProfileKey)?.takeUnless { it.unreadable }?.preference
                ?: return LocalBackupExportResult(error = LocalBackupRepositoryError.InvalidBackup) else null,
            agenda = life.agenda, snapshots = life.snapshots, pantry = life.pantry,
            shoppingPurchases = life.shoppingPurchases, weeklyMenuDraft = life.weeklyMenuDraft, journal = life.journal
        )
        return runCatching {
            val plainJson = LocalDataBackupCodec.encode(keys.fingerprintKey, selection, payload, exportedAtEpochMs)
            LocalBackupExportResult(json = passphrase?.let { EncryptedLocalBackupCodec.encode(plainJson, it) } ?: plainJson)
        }.getOrElse { LocalBackupExportResult(error = LocalBackupRepositoryError.InvalidBackup) }
    }

    suspend fun preview(
        json: String,
        keys: LocalBackupProfileKeys,
        passphrase: CharArray? = null
    ): LocalBackupImportPreview {
        if (!keys.valid) return LocalBackupImportPreview(error = LocalBackupRepositoryError.InvalidProfile)
        val plainJson = if (EncryptedLocalBackupCodec.isEncrypted(json)) {
            val password = passphrase ?: return LocalBackupImportPreview(error = LocalBackupRepositoryError.PasswordRequired)
            val unlocked = EncryptedLocalBackupCodec.decode(json, password)
            val plaintext = unlocked.plaintext ?: return LocalBackupImportPreview(
                error = when (unlocked.error) {
                    EncryptedLocalBackupError.PasswordTooShort -> LocalBackupRepositoryError.PasswordTooShort
                    EncryptedLocalBackupError.AuthenticationFailed, EncryptedLocalBackupError.Malformed,
                    EncryptedLocalBackupError.UnsupportedVersion, EncryptedLocalBackupError.Oversized ->
                        LocalBackupRepositoryError.EncryptedBackupInvalid
                    null -> LocalBackupRepositoryError.EncryptedBackupInvalid
                }
            )
            plaintext
        } else json
        val decoded = LocalDataBackupCodec.decode(plainJson, keys.fingerprintKey)
        val backup = decoded.backup ?: return LocalBackupImportPreview(
            error = if (decoded.error == LocalBackupDecodeError.ProfileMismatch) LocalBackupRepositoryError.ProfileMismatch
            else LocalBackupRepositoryError.InvalidBackup
        )
        val items = mutableListOf<LocalBackupPreviewItem>()
        fun add(partition: LocalBackupPartition, existing: Boolean, identical: Boolean = false, conflictKey: String? = null) {
            items += LocalBackupPreviewItem(partition,
                if (!existing) LocalBackupDisposition.Add else if (identical) LocalBackupDisposition.Merge else LocalBackupDisposition.Overwrite,
                conflictKey.takeIf { existing && !identical })
        }
        if (backup.payload.favorites.isNotEmpty()) {
            val current = favoriteStore.list(keys.favoriteProfileKey)
            if (current.state !in setOf(ReadingFavoriteDataState.Ready, ReadingFavoriteDataState.Missing, ReadingFavoriteDataState.Migrated))
                return LocalBackupImportPreview(error = LocalBackupRepositoryError.CorruptFavorites)
            backup.payload.favorites.forEach { incoming ->
                val old = current.entries.firstOrNull { it.id == incoming.id }
                add(LocalBackupPartition.Favorites, old != null, old == incoming, "favorite:${incoming.id}")
            }
        }
        if (backup.payload.actionFeedback.isNotEmpty()) {
            if (!feedbackStore.isHealthy(keys.localProfileKey)) return LocalBackupImportPreview(error = LocalBackupRepositoryError.CorruptFeedback)
            val current = feedbackStore.list(keys.localProfileKey)
            backup.payload.actionFeedback.forEach { incoming ->
                val old = current.firstOrNull { it.id == incoming.id }
                add(LocalBackupPartition.Feedback, old != null, old == incoming, "feedback:${incoming.id}")
            }
        }
        for (item in backup.payload.companionGames) {
            val character = MysticCharacterId.entries.firstOrNull { it.key == item.characterId } ?: continue
            val old = gameProgressStore.loadEnvelope(keys.localProfileKey, character, item.gameId)
            add(LocalBackupPartition.Games, gameProgressStore.hasStoredValue(keys.localProfileKey, character, item.gameId),
                old?.stateJson == item.stateJson, item.conflictKey)
        }
        if (backup.payload.xiangqiSave != null) add(LocalBackupPartition.Xiangqi, xiangqiStore.hasStored(keys.localProfileKey),
            xiangqiStore.load(keys.localProfileKey) == backup.payload.xiangqiSave, XIANGQI_CONFLICT_KEY)
        if (backup.payload.agenda.isNotEmpty() || backup.payload.snapshots.isNotEmpty() || backup.payload.pantry.isNotEmpty() ||
            backup.payload.shoppingPurchases.isNotEmpty() || backup.payload.weeklyMenuDraft != null || backup.payload.journal.isNotEmpty()) {
            val life = dailyLifeStore?.load(keys.localProfileKey)
            if (life == null || !life.readable) return LocalBackupImportPreview(error = LocalBackupRepositoryError.InvalidBackup)
            backup.payload.journal.forEach { incoming ->
                val old = life.journal.firstOrNull { it.dateKey == incoming.dateKey }
                add(LocalBackupPartition.Notes, old != null, old == incoming, "journal:${incoming.dateKey}")
            }
            backup.payload.agenda.forEach { incoming ->
                val old = life.agenda.firstOrNull { it.id == incoming.id }
                add(LocalBackupPartition.Agenda, old != null, old?.copy(note = "") == incoming.copy(note = ""), "agenda:${incoming.id}")
                if (backup.selection.notes && (incoming.note.isNotEmpty() || old?.note?.isNotEmpty() == true))
                    add(LocalBackupPartition.Notes, old != null, old?.note == incoming.note, "notes:${incoming.id}")
            }
            backup.payload.snapshots.forEach { incoming ->
                val old = life.snapshots.firstOrNull { it.id == incoming.id }
                add(LocalBackupPartition.Snapshots, old != null, old == incoming, "snapshot:${incoming.id}")
            }
            backup.payload.pantry.forEach { incoming ->
                val old = life.pantry.firstOrNull { it.ingredient.equals(incoming.ingredient, ignoreCase = true) }
                val effective = PantryRules.preserveMetadataForLegacy(incoming, old, backup.version)
                add(LocalBackupPartition.Agenda, old != null, old?.copy(note = "") == effective.copy(note = ""), "pantry:${PantryRules.nameKey(incoming.ingredient)}")
                if (backup.selection.notes && (incoming.note.isNotEmpty() || old?.note?.isNotEmpty() == true))
                    add(LocalBackupPartition.Notes, old != null, old?.note == incoming.note, "pantry-note:${incoming.ingredient.lowercase()}")
            }
            backup.payload.shoppingPurchases.forEach { incoming ->
                val old = life.shoppingPurchases.firstOrNull { it.key == incoming.key }
                add(LocalBackupPartition.Agenda, old != null, old == incoming, "purchase:${incoming.key}")
            }
            backup.payload.weeklyMenuDraft?.let { incoming ->
                add(LocalBackupPartition.Agenda, life.weeklyMenuDraft != null, life.weeklyMenuDraft == incoming, "weeklyMenuDraft")
            }
        }
        if (backup.payload.foodPreference != null) add(LocalBackupPartition.Food, foodPreferenceStore?.hasStored(keys.localProfileKey) == true,
            foodPreferenceStore?.readSnapshot(keys.localProfileKey)?.preference == backup.payload.foodPreference, "foodPreferences")
        if (backup.payload.activityPreference != null) add(LocalBackupPartition.Activity, activityPreferenceStore?.hasStored(keys.localProfileKey) == true,
            activityPreferenceStore?.readSnapshot(keys.localProfileKey)?.preference == backup.payload.activityPreference, "activityPreferences")
        return LocalBackupImportPreview(backup = backup, items = items, conflicts = items.mapNotNull { it.conflictKey }.toSet())
    }

    suspend fun restore(
        json: String,
        keys: LocalBackupProfileKeys,
        sections: LocalBackupSelection,
        overwriteConflicts: Set<String>,
        passphrase: CharArray? = null
    ): LocalBackupRestoreResult {
        atomicBridge?.let { bridge ->
            if (!LocalRestoreTransaction(bridge).recover()) return LocalBackupRestoreResult(LocalBackupRestoreStatus.Failed, error = LocalBackupRepositoryError.WriteFailed)
            val overlay = RestoreStagingBridge(bridge)
            val stagedXiangqi = object : XiangqiSaveBackupStore {
                val gson = Gson()
                fun key(profile: String) = "game_save_${LocalDataBackupCodec.fingerprint(profile)}"
                override suspend fun load(profileKey: String): GameSave? = overlay.read(key(profileKey))?.let { gson.fromJson(it, GameSave::class.java) }
                override suspend fun save(profileKey: String, save: GameSave) { overlay.write(key(profileKey), gson.toJson(save)) }
                override suspend fun hasStored(profileKey: String): Boolean = overlay.read(key(profileKey)) != null
            }
            val staging = LocalDataBackupRepository(ReadingFavoriteStore(overlay), ActionFeedbackDataStore(overlay),
                CompanionGameProgressStore(overlay), stagedXiangqi, dailyLifeStore = DailyLifeStore(overlay),
                foodPreferenceStore = FoodPreferenceStore(overlay), activityPreferenceStore = ActionConstraintStore(overlay))
            val result = staging.restore(json, keys, sections, overwriteConflicts, passphrase)
            if (result.status != LocalBackupRestoreStatus.Completed) return result
            val committed = LocalRestoreTransaction(bridge).commit(overlay.before, overlay.changes)
            if (committed) { dailyLifeStore?.invalidate(); return result }
            return LocalBackupRestoreResult(LocalBackupRestoreStatus.Failed, error = LocalBackupRepositoryError.WriteFailed)
        }
        val preview = preview(json, keys, passphrase)
        val backup = preview.backup ?: return LocalBackupRestoreResult(
            status = LocalBackupRestoreStatus.InvalidBackup,
            error = preview.error ?: LocalBackupRepositoryError.InvalidBackup
        )
        if (sections == LocalBackupSelection()) return LocalBackupRestoreResult(LocalBackupRestoreStatus.InvalidBackup, error = LocalBackupRepositoryError.NoSectionsSelected)
        if (!backup.selection.includes(sections)) return LocalBackupRestoreResult(LocalBackupRestoreStatus.InvalidBackup, error = LocalBackupRepositoryError.SelectionNotIncluded)
        if (sections.notes && !sections.agenda) return LocalBackupRestoreResult(LocalBackupRestoreStatus.InvalidBackup, error = LocalBackupRepositoryError.SelectionNotIncluded)

        if (sections.favorites) {
            val current = runCatching { favoriteStore.list(keys.favoriteProfileKey) }.getOrNull()
            if (current == null || current.state !in setOf(ReadingFavoriteDataState.Ready, ReadingFavoriteDataState.Migrated, ReadingFavoriteDataState.Missing)) {
                return LocalBackupRestoreResult(LocalBackupRestoreStatus.Failed, error = LocalBackupRepositoryError.CorruptFavorites)
            }
            if ((current.entries.map { it.id } + backup.payload.favorites.map { it.id }).toSet().size > LocalDataBackupCodec.MAX_FAVORITES)
                return LocalBackupRestoreResult(LocalBackupRestoreStatus.InvalidBackup, error = LocalBackupRepositoryError.InvalidBackup)
        }
        if (sections.actionFeedback && !runCatching { feedbackStore.isHealthy(keys.localProfileKey) }.getOrDefault(false)) {
            return LocalBackupRestoreResult(LocalBackupRestoreStatus.Failed, error = LocalBackupRepositoryError.CorruptFeedback)
        }
        if (sections.actionFeedback && (feedbackStore.list(keys.localProfileKey).map { it.id } + backup.payload.actionFeedback.map { it.id }).toSet().size > LocalDataBackupCodec.MAX_FEEDBACK)
            return LocalBackupRestoreResult(LocalBackupRestoreStatus.InvalidBackup, error = LocalBackupRepositoryError.InvalidBackup)
        val currentLife = if (sections.agenda || sections.snapshots) dailyLifeStore?.load(keys.localProfileKey) else null
        if ((sections.agenda || sections.snapshots) && (currentLife == null || !currentLife.readable))
            return LocalBackupRestoreResult(LocalBackupRestoreStatus.InvalidBackup, error = LocalBackupRepositoryError.InvalidBackup)
        val mergedLife = currentLife?.let { current -> current.copy(
            agenda = if (sections.agenda) (current.agenda.filterNot { old -> backup.payload.agenda.any { it.id == old.id } } +
                backup.payload.agenda.map { incoming -> if (sections.notes) incoming else incoming.copy(note = current.agenda.firstOrNull { it.id == incoming.id }?.note.orEmpty()) }) else current.agenda,
            snapshots = if (sections.snapshots) (current.snapshots.filterNot { old -> backup.payload.snapshots.any { it.id == old.id } } + backup.payload.snapshots) else current.snapshots,
            pantry = if (sections.agenda && backup.version >= 3) (current.pantry.filterNot { old -> backup.payload.pantry.any { it.ingredient.equals(old.ingredient, ignoreCase = true) } } +
                backup.payload.pantry.map { incoming ->
                    val old = current.pantry.firstOrNull { it.ingredient.equals(incoming.ingredient, ignoreCase = true) }
                    val effective = PantryRules.preserveMetadataForLegacy(incoming, old, backup.version)
                    if (sections.notes) effective else effective.copy(note = old?.note.orEmpty())
                }) else current.pantry,
            shoppingPurchases = if (sections.agenda && backup.version >= 6)
                current.shoppingPurchases.filterNot { old -> backup.payload.shoppingPurchases.any { it.key == old.key } } + backup.payload.shoppingPurchases
                else current.shoppingPurchases,
            weeklyMenuDraft = if (sections.agenda && backup.version >= 6) backup.payload.weeklyMenuDraft ?: current.weeklyMenuDraft else current.weeklyMenuDraft,
            journal = if (sections.notes && backup.version >= 8)
                current.journal.filterNot { old -> backup.payload.journal.any { it.dateKey == old.dateKey } } + backup.payload.journal
                else current.journal
        ) }
        if (mergedLife != null && !DailyLifeCodec.valid(mergedLife)) return LocalBackupRestoreResult(LocalBackupRestoreStatus.InvalidBackup, error = LocalBackupRepositoryError.InvalidBackup)

        val selectedConflicts = preview.selectedConflicts(sections)
        if (selectedConflicts.any { it !in overwriteConflicts }) {
            return LocalBackupRestoreResult(
                status = LocalBackupRestoreStatus.ConfirmationRequired,
                conflicts = selectedConflicts,
                error = LocalBackupRepositoryError.ConfirmationRequired
            )
        }

        var favoritesDone = 0
        var feedbackDone = 0
        var gamesDone = 0
        var xiangqiDone = false
        return try {
            if (sections.foodPreferences) {
                val store = foodPreferenceStore ?: error("food_store_unavailable")
                backup.payload.foodPreference?.let { store.save(keys.localProfileKey, it) }
            }
            if (sections.activityPreferences) {
                val store = activityPreferenceStore ?: error("activity_store_unavailable")
                backup.payload.activityPreference?.let { store.save(keys.localProfileKey, it) }
            }
            if (mergedLife != null) dailyLifeStore!!.replace(keys.localProfileKey, mergedLife)
            if (sections.favorites) {
                backup.payload.favorites.forEach { favoriteStore.upsert(keys.favoriteProfileKey, it) }
                favoritesDone = backup.payload.favorites.size
            }
            if (sections.actionFeedback) {
                backup.payload.actionFeedback.forEach { feedbackStore.append(keys.localProfileKey, it) }
                feedbackDone = backup.payload.actionFeedback.size
            }
            if (sections.companionGames) {
                backup.payload.companionGames.forEach { item ->
                    val character = MysticCharacterId.entries.single { it.key == item.characterId }
                    val state = CompanionGameProgressCodec.decodeState(
                        CompanionGameProgressEnvelope(
                            version = CompanionGameProgressCodec.VERSION,
                            profileKey = LocalDataBackupCodec.fingerprint(keys.localProfileKey),
                            characterId = item.characterId,
                            gameId = item.gameId,
                            stateJson = item.stateJson,
                            updatedAt = item.updatedAt
                        )
                    ) ?: error("invalid_game_state")
                    gameProgressStore.save(keys.localProfileKey, character, state, item.updatedAt)
                    gamesDone++
                }
            }
            if (sections.xiangqiSave && backup.payload.xiangqiSave != null) {
                xiangqiStore.save(keys.localProfileKey, backup.payload.xiangqiSave)
                xiangqiDone = true
            }
            LocalBackupRestoreResult(LocalBackupRestoreStatus.Completed, favoritesDone, feedbackDone, gamesDone, xiangqiDone)
        } catch (_: Throwable) {
            LocalBackupRestoreResult(
                status = LocalBackupRestoreStatus.Failed,
                importedFavorites = favoritesDone,
                importedFeedback = feedbackDone,
                importedGames = gamesDone,
                importedXiangqi = xiangqiDone,
                error = LocalBackupRepositoryError.WriteFailed
            )
        }
    }

    companion object { const val XIANGQI_CONFLICT_KEY = "xiangqi" }
}
