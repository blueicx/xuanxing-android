package com.xuanji.app.data.local

import com.xuanji.app.domain.SoftMemorySource
import com.xuanji.app.domain.SoftMemoryTag
import com.xuanji.app.domain.SoftMemoryTagCodec
import java.security.MessageDigest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
        require(profileKey.isNotBlank())
        return mutex.withLock {
            val current = read(profileKey)
            if (current.unreadable) return@withLock current
            // Explicit edits and revocations must survive later topic detection.
            val existing = current.tags.firstOrNull { it.id == tag.id }
            if (existing?.revoked == true || existing?.source == SoftMemorySource.UserConfirmed) return@withLock current
            val next = (current.tags.filterNot { it.id == tag.id } + tag.copy(source = SoftMemorySource.Inferred, revoked = false))
                .takeLast(SoftMemoryTagCodec.MAX_TAGS)
            bridge.write(memoryKey(profileKey), SoftMemoryTagCodec.encode(profileKey, next))
            SoftMemoryTagSnapshot(next)
        }
    }

    suspend fun revoke(profileKey: String, id: String): SoftMemoryTagSnapshot {
        require(profileKey.isNotBlank())
        return mutex.withLock {
            val current = read(profileKey)
            if (current.unreadable) return@withLock current
            val next = SoftMemoryTagCodec.revoke(current.tags, id)
            if (next != current.tags) bridge.write(memoryKey(profileKey), SoftMemoryTagCodec.encode(profileKey, next))
            SoftMemoryTagSnapshot(next)
        }
    }

    suspend fun edit(profileKey: String, tag: SoftMemoryTag): SoftMemoryTagSnapshot {
        require(profileKey.isNotBlank() && tag.id.isNotBlank() && tag.label.isNotBlank() && tag.value.isNotBlank())
        require(tag.label.length <= 80 && tag.value.length <= 80)
        require(tag.expiresAtDate == null || runCatching { java.time.LocalDate.parse(tag.expiresAtDate) }.isSuccess)
        return mutex.withLock {
            val current = read(profileKey)
            if (current.unreadable) return@withLock current
            if (current.tags.none { it.id == tag.id }) return@withLock current
            val next = current.tags.map { old -> if (old.id == tag.id) old.copy(
                label = tag.label.trim(), value = tag.value.trim(), expiresAtDate = tag.expiresAtDate,
                source = SoftMemorySource.UserConfirmed
            ) else old }
            bridge.write(memoryKey(profileKey), SoftMemoryTagCodec.encode(profileKey, next))
            SoftMemoryTagSnapshot(next)
        }
    }

    suspend fun clear(profileKey: String) {
        require(profileKey.isNotBlank())
        mutex.withLock { bridge.delete(memoryKey(profileKey)) }
    }

    fun memoryKey(profileKey: String): String = KEY_PREFIX + fingerprint(profileKey)

    companion object {
        const val KEY_PREFIX = "soft_memory_"
        private val mutex = Mutex()

        fun fingerprint(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
