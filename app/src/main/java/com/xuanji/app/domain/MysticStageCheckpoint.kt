package com.xuanji.app.domain

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.time.LocalDate

data class MysticStageCheckpoint(
    val dateKey: String,
    val characterId: MysticCharacterId,
    val stageOpen: Boolean,
    val drawers: Map<MysticCharacterId, MysticDrawerState> = emptyMap()
)

object MysticStageCheckpointCodec {
    const val VERSION = 1
    fun encode(value: MysticStageCheckpoint): String = JsonObject().apply {
        addProperty("version", VERSION)
        addProperty("dateKey", value.dateKey)
        addProperty("characterId", value.characterId.key)
        addProperty("stageOpen", value.stageOpen)
        add("drawers", JsonObject().apply { value.drawers.forEach { (id, state) -> addProperty(id.key, state.name) } })
    }.toString()

    fun decode(json: String?): MysticStageCheckpoint? {
        if (json.isNullOrBlank() || json.length > 4096) return null
        return runCatching {
            val root = JsonParser.parseString(json).asJsonObject
            require(root.get("version").asInt == VERSION)
            val date = root.get("dateKey").asString
            require(LocalDate.parse(date).toString() == date)
            val id = MysticCharacterId.entries.first { it.key == root.get("characterId").asString }
            require(root.get("stageOpen").asJsonPrimitive.isBoolean)
            val drawers = root.getAsJsonObject("drawers").entrySet().associate { (key, value) ->
                MysticCharacterId.entries.first { it.key == key } to MysticDrawerState.valueOf(value.asString)
            }
            MysticStageCheckpoint(date, id, root.get("stageOpen").asBoolean, drawers)
        }.getOrNull()
    }
}
