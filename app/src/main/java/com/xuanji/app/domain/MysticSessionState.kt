package com.xuanji.app.domain

/** Immutable state for one companion conversation. A new token invalidates older async replies. */
data class MysticSessionState(
    val sessionToken: Long = 0L,
    val mode: String = "scholar",
    val topicKey: String = "composite",
    val styleKey: String = "",
    val skinId: String = "",
    val pendingInput: String? = null,
    val recentTurns: List<MysticTurn> = emptyList(),
    val memoryNotes: List<MysticMemoryNote> = emptyList(),
    val messages: List<MysticMessage> = emptyList(),
    val nextTurnId: Long = 0L,
    val requestState: MysticRequestState = MysticRequestState.Idle
)

enum class MysticMessageRole { User, Mystic, System }

data class MysticMessage(
    val turnId: Long,
    val sessionToken: Long,
    val role: MysticMessageRole,
    val text: String,
    val intent: MysticIntent? = null,
    val pending: Boolean = false,
    val error: Boolean = false
)

sealed interface MysticRequestState {
    data object Idle : MysticRequestState
    data class Pending(val sessionToken: Long, val turnId: Long, val input: String) : MysticRequestState
    data class Failed(val sessionToken: Long, val turnId: Long, val input: String, val message: String) : MysticRequestState
}

sealed interface MysticEvent {
    data class Input(val text: String) : MysticEvent
    data class Reply(val token: Long, val turn: MysticTurn) : MysticEvent
    data class Remember(val note: MysticMemoryNote) : MysticEvent
    data class ChangeContext(
        val mode: String? = null,
        val topicKey: String? = null,
        val styleKey: String? = null,
        val skinId: String? = null
    ) : MysticEvent
    data object Clear : MysticEvent
    data class SendInput(val text: String) : MysticEvent
    data class QuickPrompt(val text: String) : MysticEvent
    data class ReplyStarted(val sessionToken: Long, val turnId: Long) : MysticEvent
    data class ReplySucceeded(val sessionToken: Long, val turnId: Long, val reply: DialogueReply) : MysticEvent
    data class ReplyFailed(val sessionToken: Long, val turnId: Long, val message: String) : MysticEvent
    data class CancelReply(val sessionToken: Long, val turnId: Long) : MysticEvent
    data object RetryTurn : MysticEvent
}

/** Pure reducer shared by UI and future providers. Stale replies are dropped by token check. */
fun reduce(state: MysticSessionState, event: MysticEvent): MysticSessionState = when (event) {
    is MysticEvent.Input -> state.copy(
        pendingInput = event.text.trim().take(200).ifBlank { null }
    )
    is MysticEvent.Reply -> if (event.token != state.sessionToken) {
        state
    } else {
        state.copy(
            pendingInput = null,
            recentTurns = (state.recentTurns + event.turn).takeLast(12)
        )
    }
    is MysticEvent.Remember -> state.copy(
        memoryNotes = (state.memoryNotes + event.note).distinctBy { it.id }.takeLast(12)
    )
    is MysticEvent.ChangeContext -> state.copy(
        sessionToken = state.sessionToken + 1,
        mode = event.mode ?: state.mode,
        topicKey = event.topicKey ?: state.topicKey,
        styleKey = event.styleKey ?: state.styleKey,
        skinId = event.skinId ?: state.skinId,
        pendingInput = null,
        recentTurns = emptyList(),
        memoryNotes = emptyList(),
        requestState = MysticRequestState.Idle,
        messages = state.messages + MysticMessage(
            turnId = state.nextTurnId,
            sessionToken = state.sessionToken + 1,
            role = MysticMessageRole.System,
            text = "已切换陪伴上下文，未完成的旧回复已取消"
        )
    )
    MysticEvent.Clear -> state.copy(
        sessionToken = state.sessionToken + 1,
        pendingInput = null,
        recentTurns = emptyList(),
        memoryNotes = emptyList(),
        messages = emptyList(),
        requestState = MysticRequestState.Idle
    )
    is MysticEvent.SendInput -> beginInput(state, event.text)
    is MysticEvent.QuickPrompt -> beginInput(state, event.text)
    is MysticEvent.ReplyStarted -> when (val request = state.requestState) {
        is MysticRequestState.Pending -> if (request.sessionToken == event.sessionToken && request.turnId == event.turnId) state else state
        else -> state
    }
    is MysticEvent.ReplySucceeded -> {
        val request = state.requestState
        if (request !is MysticRequestState.Pending || request.sessionToken != event.sessionToken || request.turnId != event.turnId) {
            state
        } else {
            state.copy(
                requestState = MysticRequestState.Idle,
                messages = state.messages + MysticMessage(event.turnId, event.sessionToken, MysticMessageRole.Mystic, event.reply.text, event.reply.intent),
                recentTurns = (state.recentTurns + MysticTurn(request.input, event.reply.text, event.reply.intent.value)).takeLast(12)
            )
        }
    }
    is MysticEvent.ReplyFailed -> {
        val request = state.requestState
        if (request !is MysticRequestState.Pending || request.sessionToken != event.sessionToken || request.turnId != event.turnId) state
        else state.copy(requestState = MysticRequestState.Failed(event.sessionToken, event.turnId, request.input, event.message))
    }
    is MysticEvent.CancelReply -> {
        val request = state.requestState
        if (request !is MysticRequestState.Pending || request.sessionToken != event.sessionToken || request.turnId != event.turnId) state
        else state.copy(
            requestState = MysticRequestState.Idle,
            messages = state.messages + MysticMessage(event.turnId, event.sessionToken, MysticMessageRole.System, "回复已取消")
        )
    }
    MysticEvent.RetryTurn -> {
        val failed = state.requestState as? MysticRequestState.Failed
        if (failed == null) state else beginInput(state.copy(requestState = MysticRequestState.Idle), failed.input)
    }
}

private fun beginInput(state: MysticSessionState, raw: String): MysticSessionState {
    if (state.requestState is MysticRequestState.Pending) return state
    val input = raw.trim().take(200)
    if (input.isBlank()) return state.copy(pendingInput = null)
    val turnId = state.nextTurnId + 1
    return state.copy(
        pendingInput = null,
        nextTurnId = turnId,
        requestState = MysticRequestState.Pending(state.sessionToken, turnId, input),
        messages = state.messages + MysticMessage(turnId, state.sessionToken, MysticMessageRole.User, input)
    )
}
