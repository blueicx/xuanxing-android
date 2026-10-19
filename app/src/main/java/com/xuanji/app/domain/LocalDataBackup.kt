package com.xuanji.app.domain

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.xuanji.app.domain.action.ActionFeedback
import com.xuanji.app.domain.action.FoodPreference
import com.xuanji.app.domain.action.ActivityPreference
import com.xuanji.app.domain.game.CompanionGameCatalog
import com.xuanji.app.domain.game.CompanionGameProgressCodec
import com.xuanji.app.domain.game.CompanionGameProgressEnvelope
import com.xuanji.app.domain.game.GameArchive
import com.xuanji.app.domain.game.GameRestore
import com.xuanji.app.domain.game.GameSave
import java.security.MessageDigest

data class LocalBackupSelection(
    val favorites: Boolean = false,
    val actionFeedback: Boolean = false,
    val companionGames: Boolean = false,
    val xiangqiSave: Boolean = false,
    val foodPreferences: Boolean = false,
    val activityPreferences: Boolean = false,
    val agenda: Boolean = false,
    val notes: Boolean = false,
    val snapshots: Boolean = false
) {
    fun includes(other: LocalBackupSelection): Boolean =
        (!other.favorites || favorites) && (!other.actionFeedback || actionFeedback) &&
            (!other.companionGames || companionGames) && (!other.xiangqiSave || xiangqiSave) &&
            (!other.foodPreferences || foodPreferences) && (!other.activityPreferences || activityPreferences) &&
            (!other.agenda || agenda) && (!other.notes || notes) && (!other.snapshots || snapshots)
}

data class CompanionGameBackup(
    val characterId: String,
    val gameId: String,
    val stateJson: String,
    val updatedAt: Long,
    val stateVersion: Int = CompanionGameProgressCodec.VERSION
) {
    val conflictKey: String get() = "$characterId:$gameId"
}

data class LocalBackupPayload(
    val favorites: List<ReadingFavorite> = emptyList(),
    val actionFeedback: List<ActionFeedback> = emptyList(),
    val companionGames: List<CompanionGameBackup> = emptyList(),
    val xiangqiSave: GameSave? = null,
    val foodPreference: FoodPreference? = null,
    val activityPreference: ActivityPreference? = null,
    val agenda: List<DailyAgendaItem> = emptyList(),
    val snapshots: List<ReadingSnapshot> = emptyList(),
    val pantry: List<PantryItem> = emptyList(),
    val shoppingPurchases: List<ShoppingPurchase> = emptyList(),
    val weeklyMenuDraft: com.xuanji.app.domain.action.WeeklyMenuDraft? = null,
    val journal: List<DailyJournalEntry> = emptyList()
)

data class LocalDataBackup(
    val version: Int,
    val profileFingerprint: String,
    val exportedAtEpochMs: Long,
    val selection: LocalBackupSelection,
    val payload: LocalBackupPayload,
    val checksum: String
)

enum class LocalBackupDecodeError { Malformed, UnsupportedVersion, ChecksumMismatch, ProfileMismatch, Oversized }
data class LocalBackupDecodeResult(val backup: LocalDataBackup? = null, val error: LocalBackupDecodeError? = null)

/** JSON backup for user-selected local records. The SHA-256 is an integrity check, not encryption or authentication. */
object LocalDataBackupCodec {
    const val VERSION = 8
    const val MAX_BYTES = 2_000_000
    const val MAX_FAVORITES = 200
    const val MAX_FEEDBACK = 100
    const val MAX_GAMES = 8
    private val gson = Gson()

    fun encode(
        profileKey: String,
        selection: LocalBackupSelection,
        payload: LocalBackupPayload,
        exportedAtEpochMs: Long
    ): String {
        require(profileKey.isNotBlank())
        require(!selection.notes || selection.agenda) { "notes_require_agenda" }
        val bounded = LocalBackupPayload(
            favorites = if (selection.favorites) payload.favorites.distinctBy(ReadingFavorite::id) else emptyList(),
            actionFeedback = if (selection.actionFeedback) payload.actionFeedback.distinctBy(ActionFeedback::id) else emptyList(),
            companionGames = if (selection.companionGames) payload.companionGames.distinctBy(CompanionGameBackup::conflictKey) else emptyList(),
            xiangqiSave = payload.xiangqiSave.takeIf { selection.xiangqiSave },
            foodPreference = payload.foodPreference.takeIf { selection.foodPreferences },
            activityPreference = payload.activityPreference.takeIf { selection.activityPreferences },
            agenda = if (selection.agenda) payload.agenda.map { if (selection.notes) it else it.copy(note = "") } else emptyList(),
            snapshots = if (selection.snapshots) payload.snapshots else emptyList(),
            pantry = if (selection.agenda) payload.pantry.map { if (selection.notes) it else it.copy(note = "") } else emptyList(),
            shoppingPurchases = if (selection.agenda) payload.shoppingPurchases else emptyList(),
            weeklyMenuDraft = payload.weeklyMenuDraft.takeIf { selection.agenda },
            journal = if (selection.notes) payload.journal else emptyList()
        )
        require(validPayload(selection, bounded)) { "invalid_or_oversized_backup" }
        val unsigned = unsignedJson(fingerprint(profileKey), exportedAtEpochMs, selection, bounded)
        val checksum = sha256(unsigned.toString())
        unsigned.addProperty("checksum", checksum)
        return unsigned.toString().also { require(it.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "backup_oversized" } }
    }

    fun decode(json: String?, profileKey: String): LocalBackupDecodeResult {
        if (json.isNullOrBlank()) return LocalBackupDecodeResult(error = LocalBackupDecodeError.Malformed)
        if (json.toByteArray(Charsets.UTF_8).size > MAX_BYTES) return LocalBackupDecodeResult(error = LocalBackupDecodeError.Oversized)
        return runCatching {
            val root = JsonParser.parseString(json).asJsonObject
            val version = root.intValue("version") ?: return LocalBackupDecodeResult(error = LocalBackupDecodeError.Malformed)
            if (version !in 1..VERSION) return LocalBackupDecodeResult(error = LocalBackupDecodeError.UnsupportedVersion)
            val expectedChecksum = root.stringValue("checksum") ?: return LocalBackupDecodeResult(error = LocalBackupDecodeError.Malformed)
            val unsigned = root.deepCopy().apply { remove("checksum") }
            if (!constantTimeEquals(expectedChecksum, sha256(unsigned.toString()))) {
                return LocalBackupDecodeResult(error = LocalBackupDecodeError.ChecksumMismatch)
            }
            val fingerprint = root.stringValue("profileFingerprint") ?: return LocalBackupDecodeResult(error = LocalBackupDecodeError.Malformed)
            if (profileKey.isBlank() || fingerprint != fingerprint(profileKey)) {
                return LocalBackupDecodeResult(error = LocalBackupDecodeError.ProfileMismatch)
            }
            val selection = root.getAsJsonObject("selection")?.let { decodeSelection(it, version) }
                ?: return LocalBackupDecodeResult(error = LocalBackupDecodeError.Malformed)
            val payload = root.getAsJsonObject("payload")?.let { decodePayload(it, version) }
                ?: return LocalBackupDecodeResult(error = LocalBackupDecodeError.Malformed)
            if (!validPayload(selection, payload)) return LocalBackupDecodeResult(error = LocalBackupDecodeError.Malformed)
            LocalBackupDecodeResult(
                backup = LocalDataBackup(
                    version = version,
                    profileFingerprint = fingerprint,
                    exportedAtEpochMs = root.longValue("exportedAtEpochMs") ?: return LocalBackupDecodeResult(error = LocalBackupDecodeError.Malformed),
                    selection = selection,
                    payload = payload,
                    checksum = expectedChecksum
                )
            )
        }.getOrElse { LocalBackupDecodeResult(error = LocalBackupDecodeError.Malformed) }
    }

    fun fingerprint(profileKey: String): String = sha256(profileKey)

    private fun unsignedJson(
        profileFingerprint: String,
        exportedAtEpochMs: Long,
        selection: LocalBackupSelection,
        payload: LocalBackupPayload
    ) = JsonObject().apply {
        addProperty("version", VERSION)
        addProperty("profileFingerprint", profileFingerprint)
        addProperty("exportedAtEpochMs", exportedAtEpochMs)
        add("selection", gson.toJsonTree(selection))
        add("payload", gson.toJsonTree(payload))
    }

    private fun decodeSelection(value: JsonObject, version: Int): LocalBackupSelection = LocalBackupSelection(
        favorites = value.requiredBoolean("favorites"),
        actionFeedback = value.requiredBoolean("actionFeedback"),
        companionGames = value.requiredBoolean("companionGames"),
        xiangqiSave = value.requiredBoolean("xiangqiSave"),
        foodPreferences = version >= 2 && value.requiredBoolean("foodPreferences"),
        activityPreferences = version >= 2 && value.requiredBoolean("activityPreferences"),
        agenda = version >= 2 && value.requiredBoolean("agenda"),
        notes = version >= 2 && value.requiredBoolean("notes"),
        snapshots = version >= 2 && value.requiredBoolean("snapshots")
    )

    private fun decodePayload(value: JsonObject, version: Int): LocalBackupPayload = LocalBackupPayload(
        favorites = value.arrayOrEmpty("favorites").map { gson.fromJson(it, ReadingFavorite::class.java) },
        actionFeedback = value.arrayOrEmpty("actionFeedback").map { gson.fromJson(it, ActionFeedback::class.java) },
        companionGames = value.arrayOrEmpty("companionGames").map {
            val game = gson.fromJson(it, CompanionGameBackup::class.java)
            val stateVersion = if (version == 1) 2 else it.asJsonObject.intValue("stateVersion") ?: error("missing_state_version")
            val envelope = CompanionGameProgressCodec.decode(CompanionGameProgressCodec.encode(
                CompanionGameProgressEnvelope(stateVersion, "backup-validation", game.characterId, game.gameId, game.stateJson, game.updatedAt)
            )) ?: error("invalid_game_version")
            game.copy(stateJson = envelope.stateJson, stateVersion = envelope.version)
        },
        xiangqiSave = value.get("xiangqiSave")?.takeUnless { it.isJsonNull }?.let { gson.fromJson(it, GameSave::class.java) },
        foodPreference = if (version >= 2) value.get("foodPreference")?.takeUnless { it.isJsonNull }?.let { com.xuanji.app.domain.action.FoodPreferenceCodec.decode(it) } else null,
        activityPreference = if (version >= 2) value.get("activityPreference")?.takeUnless { it.isJsonNull }?.let { gson.fromJson(it, ActivityPreference::class.java) } else null,
        agenda = if (version >= 2) value.arrayOrEmpty("agenda").map { entry ->
            val migrated = entry.asJsonObject.deepCopy().apply { if (!has("portions")) addProperty("portions", 1) }
            gson.fromJson(migrated, DailyAgendaItem::class.java)
        } else emptyList(),
        snapshots = if (version >= 2) value.arrayOrEmpty("snapshots").map { gson.fromJson(it, ReadingSnapshot::class.java) } else emptyList(),
        pantry = if (version >= 3) value.arrayOrEmpty("pantry").map {
            val row = it.asJsonObject.deepCopy()
            if (version < 6) PantryRules.addLegacyDefaults(row)
            gson.fromJson(row, PantryItem::class.java)
        } else emptyList(),
        shoppingPurchases = if (version >= 6) value.arrayOrEmpty("shoppingPurchases").map { gson.fromJson(it, ShoppingPurchase::class.java) } else emptyList(),
        weeklyMenuDraft = if (version >= 6) value.get("weeklyMenuDraft")?.takeUnless { it.isJsonNull }?.let {
            gson.fromJson(it, com.xuanji.app.domain.action.WeeklyMenuDraft::class.java)
        } else null,
        journal = if (version >= 8) value.arrayOrEmpty("journal").map { gson.fromJson(it, DailyJournalEntry::class.java) } else emptyList()
    )

    private fun validPayload(selection: LocalBackupSelection, value: LocalBackupPayload): Boolean {
        if (!selection.favorites && value.favorites.isNotEmpty()) return false
        if (!selection.actionFeedback && value.actionFeedback.isNotEmpty()) return false
        if (!selection.companionGames && value.companionGames.isNotEmpty()) return false
        if (!selection.xiangqiSave && value.xiangqiSave != null) return false
        if ((!selection.foodPreferences && value.foodPreference != null) || (!selection.activityPreferences && value.activityPreference != null)) return false
        if ((!selection.agenda && (value.agenda.isNotEmpty() || value.pantry.isNotEmpty() || value.shoppingPurchases.isNotEmpty() || value.weeklyMenuDraft != null)) || (!selection.snapshots && value.snapshots.isNotEmpty())) return false
        if (!selection.notes && value.journal.isNotEmpty()) return false
        if (selection.notes && !selection.agenda || !selection.notes &&
            (value.agenda.any { it.note.isNotEmpty() } || value.pantry.any { it.note.isNotEmpty() })) return false
        if (!DailyLifeCodec.valid(DailyLifeDocument(agenda = value.agenda, snapshots = value.snapshots, pantry = value.pantry,
                shoppingPurchases = value.shoppingPurchases, weeklyMenuDraft = value.weeklyMenuDraft, journal = value.journal))) return false
        value.foodPreference?.let { food ->
            if (food.excludedIngredients.size > 12 || food.allergens.size > 12 ||
                (food.excludedIngredients + food.allergens).any { it.isBlank() || it.length > 100 } ||
                food.maxMealBudgetCents?.let { it !in 100..1_000_000 } == true || food.maxPrepMinutes?.let { it !in 5..240 } == true) return false
        }
        value.activityPreference?.let { activity ->
            if (activity.energy.name !in com.xuanji.app.domain.action.EnergyLevel.entries.map { it.name } || activity.maxDurationMinutes?.let { it !in 5..480 } == true) return false
        }
        if (value.favorites.size > MAX_FAVORITES || value.actionFeedback.size > MAX_FEEDBACK || value.companionGames.size > MAX_GAMES) return false
        if (value.favorites.any { it.id.isBlank() || it.id.length > 120 || it.title.isBlank() || it.title.length > 180 || it.type.length > 40 ||
                it.dateKey.length > 100 || it.period.length > 80 || it.mode.length > 100 || it.algorithmVersion.length > 100 ||
                it.body.length > 6_000 || it.evidence.length > 3_000 }) return false
        if (value.actionFeedback.any { it.id.isBlank() || it.id.length > 160 || it.candidateKey.isBlank() || it.candidateKey.length > 180 ||
                it.dateKey.length > 32 || it.category.length > 40 || (it.note?.length ?: 0) > 500 }) return false
        if (value.companionGames.any { game ->
                val characterId = MysticCharacterId.entries.firstOrNull { it.key == game.characterId }
                val profile = CompanionGameCatalog.byId(game.gameId)
                characterId == null || profile == null || profile.characterId != characterId ||
                    game.gameId == "xiangqi" || game.stateJson.isBlank() || game.stateJson.length > 100_000
                    || CompanionGameProgressCodec.decodeState(
                        CompanionGameProgressEnvelope(
                            CompanionGameProgressCodec.VERSION,
                            profileKey = "backup-validation",
                            characterId = game.characterId,
                            gameId = game.gameId,
                            stateJson = game.stateJson,
                            updatedAt = game.updatedAt
                        )
                    ) == null
            }) return false
        value.xiangqiSave?.let { save ->
            if (save.moves.size > 2_000 || GameArchive.restore(save, 1) is GameRestore.Rejected) return false
        }
        if (value.favorites.map(ReadingFavorite::id).distinct().size != value.favorites.size) return false
        if (value.actionFeedback.map(ActionFeedback::id).distinct().size != value.actionFeedback.size) return false
        if (value.companionGames.map(CompanionGameBackup::conflictKey).distinct().size != value.companionGames.size) return false
        return true
    }

    private fun sanitize(value: LocalBackupPayload) = value.copy(
        favorites = value.favorites.map { it.copy(
            id = it.id.take(120), type = it.type.take(40), title = it.title.take(180), dateKey = it.dateKey.take(100),
            period = it.period.take(80), mode = it.mode.take(100), characterId = it.characterId?.take(80),
            gameId = it.gameId?.take(80), body = it.body.take(6_000), evidence = it.evidence.take(3_000),
            algorithmVersion = it.algorithmVersion.take(100)
        ) },
        actionFeedback = value.actionFeedback.map { it.copy(
            id = it.id.take(160), dateKey = it.dateKey.take(32), category = it.category.take(40),
            candidateKey = it.candidateKey.take(180), note = it.note?.take(500)
        ) },
        companionGames = value.companionGames.map { it.copy(stateJson = it.stateJson.take(100_000)) }
    )

    private fun JsonObject.requiredBoolean(name: String): Boolean = get(name)?.takeUnless { it.isJsonNull }?.asBoolean
        ?: error("missing_$name")

    private fun JsonObject.arrayOrEmpty(name: String) = getAsJsonArray(name) ?: error("missing_$name")
    private fun JsonObject.stringValue(name: String): String? = get(name)?.takeUnless { it.isJsonNull }?.asString
    private fun JsonObject.intValue(name: String): Int? = runCatching { get(name)?.asInt }.getOrNull()
    private fun JsonObject.longValue(name: String): Long? = runCatching { get(name)?.asLong }.getOrNull()

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    private fun constantTimeEquals(left: String, right: String): Boolean =
        MessageDigest.isEqual(left.toByteArray(Charsets.US_ASCII), right.toByteArray(Charsets.US_ASCII))
}
