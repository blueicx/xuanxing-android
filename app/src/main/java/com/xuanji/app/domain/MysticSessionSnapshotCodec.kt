package com.xuanji.app.domain

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Stable, deliberately boring wire format for resumable character threads.
 * Pending requests are never persisted: a process restart must not resurrect a
 * network/provider operation that no longer owns the session token.
 */
object MysticSessionSnapshotCodec {
    const val VERSION = 1
    private const val MAX_MESSAGES = 40
    private const val MAX_TURNS = 20
    private const val MAX_NOTES = 12

    fun encode(state: MysticCharacterSessionState): String {
        val root = JsonObject().apply {
            addProperty("version", VERSION)
            addProperty("activeCharacterId", state.activeCharacterId.key)
            add("sharedMemoryNotes", notes(state.sharedMemoryNotes))
            add("sessions", JsonArray().also { array ->
                MysticCharacterId.entries.forEach { id ->
                    val session = state.session(id)
                    array.add(session(id, session))
                }
            })
        }
        return root.toString()
    }

    fun decode(json: String?): MysticCharacterSessionState? {
        if (json.isNullOrBlank()) return null
        val root = runCatching { JsonParser.parseString(json) }.getOrNull()?.takeIf { it.isJsonObject }?.asJsonObject
            ?: return null
        if (root.int("version", VERSION) > VERSION) return null
        val active = MysticCharacterId.entries.firstOrNull { it.key == root.string("activeCharacterId") }
            ?: MysticCharacterId.ShenYanzhou
        val sessions = MysticCharacterId.entries.associateWith { MysticSessionState() }.toMutableMap()
        root.array("sessions")?.forEach { element ->
            if (!element.isJsonObject) return@forEach
            val obj = element.asJsonObject
            val id = MysticCharacterId.entries.firstOrNull { it.key == obj.string("characterId") } ?: return@forEach
            sessions[id] = decodeSession(obj)
        }
        return MysticCharacterSessionState(
            activeCharacterId = active,
            sessions = sessions,
            sharedMemoryNotes = decodeNotes(root.array("sharedMemoryNotes")).takeLast(MAX_NOTES)
        )
    }

    private fun session(id: MysticCharacterId, value: MysticSessionState): JsonObject = JsonObject().apply {
        addProperty("characterId", id.key)
        addProperty("sessionToken", value.sessionToken.coerceAtLeast(0))
        addProperty("mode", value.mode.take(40))
        addProperty("topicKey", value.topicKey.take(80))
        addProperty("styleKey", value.styleKey.take(80))
        addProperty("skinId", value.skinId.take(80))
        addProperty("nextTurnId", value.nextTurnId.coerceAtLeast(0))
        add("recentTurns", JsonArray().also { array ->
            value.recentTurns.takeLast(MAX_TURNS).forEach { turn ->
                array.add(JsonObject().apply {
                    addProperty("question", turn.question.take(200))
                    addProperty("answer", turn.answer.take(1600))
                    addProperty("kind", turn.kind.take(40))
                })
            }
        })
        add("memoryNotes", notes(value.memoryNotes))
        add("messages", JsonArray().also { array ->
            value.messages.takeLast(MAX_MESSAGES).forEach { message ->
                array.add(JsonObject().apply {
                    addProperty("turnId", message.turnId.coerceAtLeast(0))
                    addProperty("sessionToken", message.sessionToken.coerceAtLeast(0))
                    addProperty("role", message.role.name)
                    addProperty("text", message.text.take(1600))
                    message.intent?.let { addProperty("intent", it.value) }
                    add("clarifiers", JsonArray().also { hints -> message.clarifiers.take(2).forEach { hints.add(it.take(120)) } })
                })
            }
        })
    }

    private fun decodeSession(obj: JsonObject): MysticSessionState = MysticSessionState(
        sessionToken = obj.long("sessionToken").coerceAtLeast(0),
        mode = obj.string("mode").take(40),
        topicKey = obj.string("topicKey").take(80),
        styleKey = obj.string("styleKey").take(80),
        skinId = obj.string("skinId").take(80),
        pendingInput = null,
        recentTurns = decodeTurns(obj.array("recentTurns")).takeLast(MAX_TURNS),
        memoryNotes = decodeNotes(obj.array("memoryNotes")).takeLast(MAX_NOTES),
        messages = decodeMessages(obj.array("messages")).takeLast(MAX_MESSAGES),
        nextTurnId = obj.long("nextTurnId").coerceAtLeast(0),
        requestState = MysticRequestState.Idle
    )

    private fun notes(values: List<MysticMemoryNote>): JsonArray = JsonArray().also { array ->
        values.takeLast(MAX_NOTES).forEach { note ->
            array.add(JsonObject().apply {
                addProperty("id", note.id.take(120))
                addProperty("text", note.text.take(400))
            })
        }
    }

    private fun decodeNotes(array: JsonArray?): List<MysticMemoryNote> = array?.mapNotNull { element ->
        element.takeIf { it.isJsonObject }?.asJsonObject?.let { obj ->
            val id = obj.string("id").take(120)
            val text = obj.string("text").take(400)
            if (id.isNotBlank() && text.isNotBlank()) MysticMemoryNote(id, text) else null
        }
    }.orEmpty()

    private fun decodeTurns(array: JsonArray?): List<MysticTurn> = array?.mapNotNull { element ->
        element.takeIf { it.isJsonObject }?.asJsonObject?.let { obj ->
            val question = obj.string("question").take(200)
            val answer = obj.string("answer").take(1600)
            if (question.isNotBlank() && answer.isNotBlank()) MysticTurn(question, answer, obj.string("kind").ifBlank { "ask" }.take(40)) else null
        }
    }.orEmpty()

    private fun decodeMessages(array: JsonArray?): List<MysticMessage> = array?.mapNotNull { element ->
        element.takeIf { it.isJsonObject }?.asJsonObject?.let { obj ->
            val text = obj.string("text").take(1600)
            val role = MysticMessageRole.entries.firstOrNull { it.name == obj.string("role") } ?: return@let null
            if (text.isBlank()) return@let null
            MysticMessage(
                turnId = obj.long("turnId").coerceAtLeast(0),
                sessionToken = obj.long("sessionToken").coerceAtLeast(0),
                role = role,
                text = text,
                intent = MysticIntent.entries.firstOrNull { it.value == obj.string("intent") },
                pending = false,
                error = false,
                clarifiers = obj.array("clarifiers")?.mapNotNull { it.asStringOrNull()?.take(120) }?.take(2).orEmpty()
            )
        }
    }.orEmpty()

    private fun JsonObject.string(name: String): String = runCatching {
        get(name)?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
    }.getOrDefault("")

    private fun JsonObject.long(name: String): Long = runCatching {
        get(name)?.takeIf { it.isJsonPrimitive }?.asLong ?: 0L
    }.getOrDefault(0L)

    private fun JsonObject.int(name: String, fallback: Int): Int = runCatching {
        get(name)?.takeIf { it.isJsonPrimitive }?.asInt ?: fallback
    }.getOrDefault(fallback)

    private fun JsonObject.array(name: String): JsonArray? = get(name)?.takeIf { it.isJsonArray }?.asJsonArray

    private fun JsonElement.asStringOrNull(): String? = runCatching {
        takeIf { it.isJsonPrimitive }?.asString
    }.getOrNull()
}
