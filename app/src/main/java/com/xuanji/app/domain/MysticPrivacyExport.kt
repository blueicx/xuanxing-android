package com.xuanji.app.domain

import com.google.gson.JsonArray
import com.google.gson.JsonObject

/** Redacted, portable view of local companion data for user inspection/export. */
data class MysticPrivacyExport(
    val version: Int = 1,
    val profileFingerprint: String,
    val exportedDateKey: String,
    val characterSessionSnapshot: String? = null,
    val longTermMemory: List<String> = emptyList(),
    val softMemoryLabels: List<String> = emptyList(),
    val actionFeedbackCount: Int = 0
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
    }.toString()
}

fun privacyFingerprint(value: String): String {
    var hash = 5381L
    value.forEach { hash = (hash * 33L + it.code) % 2_147_483_647L }
    return hash.toString(16)
}
