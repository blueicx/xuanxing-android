package com.xuanji.app.data.local

import com.xuanji.app.domain.MysticCharacterId
import com.xuanji.app.domain.MysticCharacterSessionState
import com.xuanji.app.domain.MysticSessionSnapshotCodec
import java.security.MessageDigest

/** DataStore-backed resumable threads; pending provider work is intentionally not saved. */
class MysticCharacterSessionStore(private val bridge: PreferenceBridge) {
    suspend fun load(profileKey: String): MysticCharacterSessionState? =
        MysticSessionSnapshotCodec.decode(bridge.read(key(profileKey)))

    suspend fun raw(profileKey: String): String? = bridge.read(key(profileKey))

    suspend fun save(profileKey: String, state: MysticCharacterSessionState) {
        bridge.write(key(profileKey), MysticSessionSnapshotCodec.encode(state))
    }

    suspend fun clearCharacter(profileKey: String, characterId: MysticCharacterId) {
        val current = load(profileKey) ?: return
        val remaining = current.sessions - characterId
        val fallback = current.activeCharacterId.takeUnless { it == characterId }
            ?: remaining.keys.firstOrNull()
            ?: MysticCharacterId.ShenYanzhou
        val normalized = current.copy(
            activeCharacterId = fallback,
            sessions = MysticCharacterId.entries.associateWith { remaining[it] ?: com.xuanji.app.domain.MysticSessionState() }
        )
        save(profileKey, normalized)
    }

    suspend fun clearAll(profileKey: String) = bridge.delete(key(profileKey))

    fun key(profileKey: String): String = KEY_PREFIX + fingerprint(profileKey)

    companion object {
        const val KEY_PREFIX = "mystic_character_sessions_"

        fun fingerprint(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}

fun android.content.Context.mysticCharacterSessionStore(): MysticCharacterSessionStore =
    MysticCharacterSessionStore(DataStorePreferenceBridge(this))
