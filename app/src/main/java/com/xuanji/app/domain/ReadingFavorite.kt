package com.xuanji.app.domain

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.security.MessageDigest

/** Only a user-selected, self-contained snapshot is persisted; profile identifiers stay in the hashed store key. */
data class ReadingFavorite(
    val id: String,
    val type: String,
    val title: String,
    val dateKey: String = "",
    val period: String = "",
    val mode: String = "",
    val characterId: String? = null,
    val gameId: String? = null,
    val body: String,
    val evidence: String = "",
    val algorithmVersion: String = "",
    val createdAt: Long
)

enum class ReadingFavoriteDataState {
    Missing,
    Ready,
    Migrated,
    Corrupt,
    UnsupportedVersion,
    ProfileMismatch,
    InvalidProfile
}

data class ReadingFavoriteSnapshot(
    val state: ReadingFavoriteDataState,
    val entries: List<ReadingFavorite> = emptyList()
)

data class DecodedReadingFavorites(
    val state: ReadingFavoriteDataState,
    val profileFingerprint: String? = null,
    val entries: List<ReadingFavorite> = emptyList()
)

object ReadingFavoriteCodec {
    const val CURRENT_VERSION = 2
    const val MAX_ENTRIES = 200

    fun stableId(type: String, context: MysticDetailContext): String {
        val source = listOf(type, context.mode, context.period, context.dateKey, context.title, context.result).joinToString("|")
        val digest = MessageDigest.getInstance("SHA-256").digest(source.toByteArray(Charsets.UTF_8))
        return "favorite-" + digest.take(16).joinToString("") { "%02x".format(it) }
    }

    fun encode(profileFingerprint: String, entries: List<ReadingFavorite>): String = JsonObject().apply {
        addProperty("version", CURRENT_VERSION)
        addProperty("profileFingerprint", profileFingerprint.take(128))
        add("entries", JsonArray().also { array ->
            entries.asSequence()
                .map { it.bounded() }
                .distinctBy { it.id }
                .sortedByDescending { it.createdAt }
                .take(MAX_ENTRIES)
                .forEach { array.add(encodeFavorite(it)) }
        })
    }.toString()

    fun decode(raw: String?): DecodedReadingFavorites {
        if (raw.isNullOrBlank()) return DecodedReadingFavorites(ReadingFavoriteDataState.Missing)
        return runCatching {
            val root = JsonParser.parseString(raw).asJsonObject
            val version = root.intValue("version") ?: return DecodedReadingFavorites(ReadingFavoriteDataState.Corrupt)
            if (version > CURRENT_VERSION) return DecodedReadingFavorites(ReadingFavoriteDataState.UnsupportedVersion)
            if (version !in 1..CURRENT_VERSION) return DecodedReadingFavorites(ReadingFavoriteDataState.Corrupt)
            val fingerprint = root.stringValue("profileFingerprint")
                ?: return DecodedReadingFavorites(ReadingFavoriteDataState.Corrupt)
            val array = root.getAsJsonArray("entries")
                ?: return DecodedReadingFavorites(ReadingFavoriteDataState.Corrupt)
            val entries = array.map { parseFavorite(it.asJsonObject, version).bounded() }
            val unique = entries.distinctBy { it.id }.sortedByDescending { it.createdAt }.take(MAX_ENTRIES)
            DecodedReadingFavorites(
                state = if (version == CURRENT_VERSION) ReadingFavoriteDataState.Ready else ReadingFavoriteDataState.Migrated,
                profileFingerprint = fingerprint,
                entries = unique
            )
        }.getOrElse { DecodedReadingFavorites(ReadingFavoriteDataState.Corrupt) }
    }

    private fun encodeFavorite(item: ReadingFavorite): JsonObject = JsonObject().apply {
        addProperty("id", item.id)
        addProperty("type", item.type)
        addProperty("title", item.title)
        addProperty("dateKey", item.dateKey)
        addProperty("period", item.period)
        addProperty("mode", item.mode)
        item.characterId?.let { addProperty("characterId", it) }
        item.gameId?.let { addProperty("gameId", it) }
        addProperty("body", item.body)
        addProperty("evidence", item.evidence)
        addProperty("algorithmVersion", item.algorithmVersion)
        addProperty("createdAt", item.createdAt)
    }

    private fun parseFavorite(item: JsonObject, envelopeVersion: Int): ReadingFavorite {
        val id = item.stringValue("id")?.takeIf(String::isNotBlank) ?: error("missing_id")
        val type = item.stringValue("type")?.takeIf(String::isNotBlank) ?: error("missing_type")
        val title = item.stringValue("title")?.takeIf(String::isNotBlank) ?: error("missing_title")
        return ReadingFavorite(
            id = id,
            type = type,
            title = title,
            dateKey = item.stringValue("dateKey").orEmpty(),
            period = item.stringValue("period").orEmpty(),
            mode = item.stringValue("mode").orEmpty(),
            characterId = item.stringValue("characterId"),
            gameId = item.stringValue("gameId"),
            body = item.stringValue("body") ?: if (envelopeVersion == 1) item.stringValue("summary").orEmpty() else error("missing_body"),
            evidence = item.stringValue("evidence") ?: item.stringValue("basis").orEmpty(),
            algorithmVersion = item.stringValue("algorithmVersion").orEmpty(),
            createdAt = item.longValue("createdAt") ?: item.longValue("savedAt") ?: 0L
        )
    }

    private fun ReadingFavorite.bounded() = copy(
        id = id.trim().take(120),
        type = type.trim().take(40),
        title = title.trim().take(180),
        dateKey = dateKey.take(100),
        period = period.take(80),
        mode = mode.take(100),
        characterId = characterId?.take(80),
        gameId = gameId?.take(80),
        body = body.take(6_000),
        evidence = evidence.take(3_000),
        algorithmVersion = algorithmVersion.take(100)
    )

    private fun JsonObject.stringValue(name: String): String? =
        get(name)?.takeUnless { it.isJsonNull }?.asString

    private fun JsonObject.intValue(name: String): Int? =
        runCatching { get(name)?.asInt }.getOrNull()

    private fun JsonObject.longValue(name: String): Long? =
        runCatching { get(name)?.asLong }.getOrNull()
}
