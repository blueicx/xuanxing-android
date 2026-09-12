package com.xuanji.app.domain

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.text.Normalizer

enum class SoftMemorySource(val wire: String) {
    Inferred("inferred"),
    UserConfirmed("user_confirmed")
}

/** A local, visible hint inferred from the user's own conversation choices. */
data class SoftMemoryTag(
    val id: String,
    val key: String,
    val label: String,
    val value: String,
    val source: SoftMemorySource,
    val createdAt: String,
    val lastSeenAt: String,
    val revoked: Boolean = false
)

data class SoftMemoryTagDecodeResult(
    val tags: List<SoftMemoryTag> = emptyList(),
    val unreadable: Boolean = false
)

object SoftMemoryTagCodec {
    const val VERSION = 1
    const val MAX_TAGS = 20
    private const val MAX_TEXT = 80

    fun encode(tags: List<SoftMemoryTag>): String = encode("", tags)

    fun encode(profileKey: String, tags: List<SoftMemoryTag>): String {
        val root = JsonObject()
        root.addProperty("version", VERSION)
        root.addProperty("profileKeyDigest", profileKeyDigest(profileKey))
        val array = JsonArray()
        tags.takeLast(MAX_TAGS).forEach { tag ->
            val item = JsonObject()
            item.addProperty("id", clean(tag.id))
            item.addProperty("key", clean(tag.key))
            item.addProperty("label", clean(tag.label))
            item.addProperty("value", clean(tag.value))
            item.addProperty("source", tag.source.wire)
            item.addProperty("createdAt", clean(tag.createdAt, 32))
            item.addProperty("lastSeenAt", clean(tag.lastSeenAt, 32))
            item.addProperty("revoked", tag.revoked)
            array.add(item)
        }
        root.add("tags", array)
        return root.toString()
    }

    fun decode(json: String?): List<SoftMemoryTag> = decodeResult(json).tags

    fun decodeResult(json: String?): SoftMemoryTagDecodeResult {
        if (json.isNullOrBlank()) return SoftMemoryTagDecodeResult()
        val root = runCatching { JsonParser.parseString(json) }.getOrNull()
            ?: return SoftMemoryTagDecodeResult(unreadable = true)
        if (!root.isJsonObject) return SoftMemoryTagDecodeResult(unreadable = true)
        val obj = root.asJsonObject
        val version = obj.intMember("version") ?: return SoftMemoryTagDecodeResult(unreadable = true)
        if (version > VERSION || version < 1) return SoftMemoryTagDecodeResult(unreadable = true)
        val array = obj.get("tags")
        if (array == null || !array.isJsonArray) return SoftMemoryTagDecodeResult(unreadable = true)

        var unreadable = false
        val tags = array.asJsonArray.mapNotNull { element ->
            if (!element.isJsonObject) {
                unreadable = true
                return@mapNotNull null
            }
            val item = element.asJsonObject
            val id = item.stringMember("id")
            val key = item.stringMember("key")
            val label = item.stringMember("label")
            val value = item.stringMember("value")
            val source = SoftMemorySource.entries.firstOrNull { it.wire == item.stringMember("source") }
            val createdAt = item.stringMember("createdAt")
            val lastSeenAt = item.stringMember("lastSeenAt")
            if (id.isBlank() || key.isBlank() || label.isBlank() || value.isBlank() || source == null ||
                createdAt.isBlank() || lastSeenAt.isBlank()
            ) {
                unreadable = true
                null
            } else {
                SoftMemoryTag(
                    id = clean(id),
                    key = clean(key),
                    label = clean(label),
                    value = clean(value),
                    source = source,
                    createdAt = clean(createdAt, 32),
                    lastSeenAt = clean(lastSeenAt, 32),
                    revoked = item.booleanMember("revoked")
                )
            }
        }.distinctBy { it.id }.takeLast(MAX_TAGS)
        return SoftMemoryTagDecodeResult(tags, unreadable)
    }

    fun revoke(tags: List<SoftMemoryTag>, id: String): List<SoftMemoryTag> =
        tags.map { if (it.id == id) it.copy(revoked = true) else it }

    private fun clean(value: String, max: Int = MAX_TEXT): String {
        val normalized = Normalizer.normalize(value, Normalizer.Form.NFC).trim()
        return normalized.take(max)
    }

    private fun profileKeyDigest(value: String): String = if (value.isBlank()) "" else {
        java.security.MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun JsonObject.stringMember(name: String): String = runCatching {
        val element: JsonElement? = get(name)
        if (element != null && element.isJsonPrimitive) element.asString.orEmpty() else ""
    }.getOrDefault("")

    private fun JsonObject.intMember(name: String): Int? = runCatching {
        val element: JsonElement? = get(name)
        if (element != null && element.isJsonPrimitive) element.asInt else null
    }.getOrNull()

    private fun JsonObject.booleanMember(name: String): Boolean = runCatching {
        val element: JsonElement? = get(name)
        if (element != null && element.isJsonPrimitive) element.asBoolean else false
    }.getOrDefault(false)
}
