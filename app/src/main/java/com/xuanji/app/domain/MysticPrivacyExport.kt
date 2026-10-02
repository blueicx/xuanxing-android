package com.xuanji.app.domain

import com.google.gson.JsonArray
import com.google.gson.JsonObject

data class MysticPrivacyFavorite(
    val id: String,
    val type: String,
    val title: String,
    val dateKey: String,
    val period: String,
    val mode: String,
    val characterId: String?,
    val gameId: String?,
    val body: String,
    val evidence: String,
    val algorithmVersion: String,
    val createdAt: Long
)

/** Redacted, portable view of local companion data for user inspection/export. */
data class MysticPrivacyExport(
    val version: Int = 1,
    val profileFingerprint: String,
    val exportedDateKey: String,
    val characterSessionSnapshot: String? = null,
    val longTermMemory: List<String> = emptyList(),
    val softMemoryLabels: List<String> = emptyList(),
    val actionFeedbackCount: Int = 0,
    val readingFavoriteCount: Int = 0,
    val readingFavorites: List<MysticPrivacyFavorite> = emptyList()
)

object MysticPrivacyExportCodec {
    fun encode(export: MysticPrivacyExport): String = JsonObject().apply {
        addProperty("version", export.version)
        addProperty("profileFingerprint", export.profileFingerprint)
        addProperty("exportedDateKey", export.exportedDateKey)
        export.characterSessionSnapshot?.let { addProperty("characterSessionSnapshot", it) }
        add("longTermMemory", JsonArray().also { values -> export.longTermMemory.take(20).forEach { values.add(it.take(240)) } })
        add("softMemoryLabels", JsonArray().also { values -> export.softMemoryLabels.take(20).forEach { values.add(it.take(120)) } })
        addProperty("actionFeedbackCount", export.actionFeedbackCount.coerceAtLeast(0))
        addProperty("readingFavoriteCount", export.readingFavoriteCount.coerceAtLeast(export.readingFavorites.size).coerceAtMost(200))
        addProperty(
            "readingFavoritesTruncated",
            export.readingFavoriteCount.coerceAtLeast(export.readingFavorites.size) > MAX_EXPORTED_FAVORITES
        )
        add("readingFavorites", JsonArray().also { values ->
            export.readingFavorites.take(MAX_EXPORTED_FAVORITES).forEach { favorite ->
                values.add(JsonObject().apply {
                    addProperty("id", favorite.id.take(120))
                    addProperty("type", favorite.type.take(40))
                    addProperty("title", favorite.title.take(180))
                    addProperty("dateKey", favorite.dateKey.take(100))
                    addProperty("period", favorite.period.take(80))
                    addProperty("mode", favorite.mode.take(100))
                    favorite.characterId?.let { addProperty("characterId", it.take(80)) }
                    favorite.gameId?.let { addProperty("gameId", it.take(80)) }
                    addProperty("body", favorite.body.take(1_500))
                    addProperty("evidence", favorite.evidence.take(800))
                    addProperty("algorithmVersion", favorite.algorithmVersion.take(100))
                    addProperty("createdAt", favorite.createdAt)
                })
            }
        })
    }.toString()

    const val MAX_EXPORTED_FAVORITES = 50
}

fun privacyFingerprint(value: String): String {
    var hash = 5381L
    value.forEach { hash = (hash * 33L + it.code) % 2_147_483_647L }
    return hash.toString(16)
}
