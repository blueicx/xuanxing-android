package com.xuanji.app.domain.game

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.xuanji.app.domain.MysticCharacterId

data class CompanionGameProgressEnvelope(
    val version: Int,
    val profileKey: String,
    val characterId: String,
    val gameId: String,
    val stateJson: String,
    val updatedAt: Long
)

sealed interface CompanionGameProgressState {
    val gameId: String
    data class Poetry(val value: PoetryChainState) : CompanionGameProgressState { override val gameId = "poetry_chain" }
    data class StarMap(val value: StarMapState) : CompanionGameProgressState { override val gameId = "star_map_observation" }
    data class SilkRoad(val value: SilkRoadState) : CompanionGameProgressState { override val gameId = "silkroad_route" }
}

object CompanionGameProgressCodec {
    const val VERSION = 1
    private val gson = Gson()

    fun encode(envelope: CompanionGameProgressEnvelope): String = gson.toJson(envelope)

    fun decode(json: String?): CompanionGameProgressEnvelope? {
        if (json.isNullOrBlank()) return null
        return runCatching {
            val root = JsonParser.parseString(json).asJsonObject
            val version = root.get("version")?.asInt ?: return null
            if (version != VERSION) return null
            CompanionGameProgressEnvelope(
                version,
                root.get("profileKey")?.asString.orEmpty(),
                root.get("characterId")?.asString.orEmpty(),
                root.get("gameId")?.asString.orEmpty(),
                root.get("stateJson")?.asString.orEmpty(),
                root.get("updatedAt")?.asLong ?: 0L
            ).takeIf { it.profileKey.isNotBlank() && it.characterId.isNotBlank() && it.gameId.isNotBlank() && it.stateJson.isNotBlank() }
        }.getOrNull()
    }

    fun encodeState(state: CompanionGameProgressState): String = when (state) {
        is CompanionGameProgressState.Poetry -> gson.toJson(state.value)
        is CompanionGameProgressState.StarMap -> gson.toJson(state.value)
        is CompanionGameProgressState.SilkRoad -> gson.toJson(state.value)
    }

    fun decodeState(envelope: CompanionGameProgressEnvelope): CompanionGameProgressState? = runCatching {
        when (envelope.gameId) {
            "poetry_chain" -> CompanionGameProgressState.Poetry(gson.fromJson(envelope.stateJson, PoetryChainState::class.java))
            "star_map_observation" -> CompanionGameProgressState.StarMap(gson.fromJson(envelope.stateJson, StarMapState::class.java))
            "silkroad_route" -> CompanionGameProgressState.SilkRoad(gson.fromJson(envelope.stateJson, SilkRoadState::class.java))
            else -> null
        }
    }.getOrNull()

    fun envelope(profileKey: String, characterId: MysticCharacterId, state: CompanionGameProgressState, now: Long): CompanionGameProgressEnvelope =
        CompanionGameProgressEnvelope(VERSION, profileKey, characterId.key, state.gameId, encodeState(state), now)
}
