package com.xuanji.app.data.local

import com.xuanji.app.domain.MysticCharacterId
import com.xuanji.app.domain.game.CompanionGameProgressCodec
import com.xuanji.app.domain.game.CompanionGameProgressEnvelope
import com.xuanji.app.domain.game.CompanionGameProgressState
import java.security.MessageDigest

class CompanionGameProgressStore(
    private val bridge: PreferenceBridge,
    private val now: () -> Long = { System.currentTimeMillis() }
) {
    suspend fun save(profileKey: String, characterId: MysticCharacterId, state: CompanionGameProgressState) {
        val fingerprint = fingerprint(profileKey)
        val envelope = CompanionGameProgressCodec.envelope(fingerprint, characterId, state, now())
        bridge.write(key(profileKey, characterId, state.gameId), CompanionGameProgressCodec.encode(envelope))
    }

    suspend fun load(profileKey: String, characterId: MysticCharacterId, gameId: String): CompanionGameProgressState? {
        val envelope = CompanionGameProgressCodec.decode(bridge.read(key(profileKey, characterId, gameId))) ?: return null
        if (envelope.profileKey != fingerprint(profileKey) || envelope.characterId != characterId.key || envelope.gameId != gameId) return null
        return CompanionGameProgressCodec.decodeState(envelope)
    }

    suspend fun clear(profileKey: String, characterId: MysticCharacterId, gameId: String) {
        bridge.delete(key(profileKey, characterId, gameId))
    }

    fun key(profileKey: String, characterId: MysticCharacterId, gameId: String): String =
        KEY_PREFIX + fingerprint(profileKey) + "_" + characterId.key + "_" + gameId

    private fun fingerprint(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    companion object { const val KEY_PREFIX = "companion_game_progress_" }
}
