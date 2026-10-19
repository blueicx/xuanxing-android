package com.xuanji.app.domain.game

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonArray
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
    const val VERSION = 5
    private val gson = Gson()

    fun encode(envelope: CompanionGameProgressEnvelope): String = gson.toJson(envelope)

    fun decode(json: String?): CompanionGameProgressEnvelope? {
        if (json.isNullOrBlank()) return null
        return runCatching {
            val root = JsonParser.parseString(json).asJsonObject
            val version = root.get("version")?.asInt ?: return null
            if (version !in 1..VERSION) return null
            val gameId = root.get("gameId")?.asString.orEmpty()
            val stateJson = root.get("stateJson")?.asString.orEmpty()
            val migratedState = migrateState(gameId, stateJson, version)
            CompanionGameProgressEnvelope(
                VERSION,
                root.get("profileKey")?.asString.orEmpty(),
                root.get("characterId")?.asString.orEmpty(),
                gameId,
                migratedState,
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
        if (envelope.version != VERSION) return null
        when (envelope.gameId) {
            "poetry_chain" -> gson.fromJson(envelope.stateJson, PoetryChainState::class.java).takeIf(PoetryChainEngine::isValidState)?.let(CompanionGameProgressState::Poetry)
            "star_map_observation" -> gson.fromJson(envelope.stateJson, StarMapState::class.java).takeIf(StarMapPuzzleEngine::isValidState)?.let(CompanionGameProgressState::StarMap)
            "silkroad_route" -> gson.fromJson(envelope.stateJson, SilkRoadState::class.java).takeIf(SilkRoadRouteEngine::isValidState)?.let(CompanionGameProgressState::SilkRoad)
            else -> null
        }
    }.getOrNull()

    fun envelope(profileKey: String, characterId: MysticCharacterId, state: CompanionGameProgressState, now: Long): CompanionGameProgressEnvelope =
        CompanionGameProgressEnvelope(VERSION, profileKey, characterId.key, state.gameId, encodeState(state), now)

    private fun migrateState(gameId: String, stateJson: String, version: Int): String {
        val state = JsonParser.parseString(stateJson).asJsonObject
        if (gameId == "poetry_chain" && version < 3 && (!state.has("undoHistory") || state.get("undoHistory").isJsonNull)) {
            state.add("undoHistory", JsonArray())
        }
        if (gameId == "poetry_chain") {
            if (version < 3 && !state.has("levelCountAtStart")) state.addProperty("levelCountAtStart", 5)
            require(state.has("levelCountAtStart") && !state.get("levelCountAtStart").isJsonNull)
        }
        if (gameId == "silkroad_route") {
            if (version < 5 && !state.has("plannedPath")) state.add("plannedPath", JsonArray())
            require(state.has("plannedPath") && !state.get("plannedPath").isJsonNull)
            if (version < 4 && !state.has("missionId")) state.addProperty("missionId", "silk-01")
            require(state.has("missionId") && !state.get("missionId").isJsonNull)
        }
        return state.toString()
    }
}
