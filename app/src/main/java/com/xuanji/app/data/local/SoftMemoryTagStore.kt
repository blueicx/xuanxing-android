package com.xuanji.app.data.local

import com.xuanji.app.domain.SoftMemorySource
import com.xuanji.app.domain.SoftMemoryTag
import com.xuanji.app.domain.SoftMemoryTagCodec
import java.security.MessageDigest

data class SoftMemoryTagSnapshot(
    val tags: List<SoftMemoryTag> = emptyList(),
    val unreadable: Boolean = false
)

class SoftMemoryTagStore(private val bridge: PreferenceBridge) {
    suspend fun read(profileKey: String): SoftMemoryTagSnapshot {
        val decoded = SoftMemoryTagCodec.decodeResult(bridge.read(memoryKey(profileKey)))
        return SoftMemoryTagSnapshot(decoded.tags, decoded.unreadable)
    }

    suspend fun upsertInferred(profileKey: String, tag: SoftMemoryTag): SoftMemoryTagSnapshot {
        val current = read(profileKey)
        val next = (current.tags.filterNot { it.id == tag.id } + tag.copy(source = SoftMemorySource.Inferred, revoked = false))
            .takeLast(SoftMemoryTagCodec.MAX_TAGS)
        bridge.write(memoryKey(profileKey), SoftMemoryTagCodec.encode(profileKey, next))
        return SoftMemoryTagSnapshot(next)
    }

    suspend fun revoke(profileKey: String, id: String): SoftMemoryTagSnapshot {
        val current = read(profileKey)
        if (current.unreadable) return current
        val next = SoftMemoryTagCodec.revoke(current.tags, id)
        if (next != current.tags) bridge.write(memoryKey(profileKey), SoftMemoryTagCodec.encode(profileKey, next))
        return SoftMemoryTagSnapshot(next)
    }

    suspend fun clear(profileKey: String) {
        bridge.delete(memoryKey(profileKey))
    }

    fun memoryKey(profileKey: String): String = KEY_PREFIX + fingerprint(profileKey)

    companion object {
        const val KEY_PREFIX = "soft_memory_"

        fun fingerprint(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
