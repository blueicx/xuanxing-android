package com.xuanji.app.domain

/**
 * Bridges a provider to the pure session reducer. It never mutates UI state;
 * callers apply the returned events in order and may safely discard stale ones.
 */
class MysticDialogueCoordinator(
    private val provider: DialogueProvider,
    private val engine: MysticDialogueEngine = DefaultMysticDialogueEngine()
) {
    suspend fun complete(state: MysticSessionState, context: DialogueContext, input: String): List<MysticEvent> {
        val started = reduce(state, MysticEvent.SendInput(input))
        val pending = started.requestState as? MysticRequestState.Pending
            ?: return emptyList()
        val requestContext = context.copy(
            mode = started.mode,
            styleKey = started.styleKey,
            topicKey = started.topicKey,
            skinId = started.skinId,
            recentTurns = started.recentTurns,
            memoryNotes = started.memoryNotes,
            question = pending.input
        )
        val result = runCatching {
            provider.complete(DialogueRequest(requestContext, pending.input, pending.sessionToken))
        }.getOrElse { ProviderResult.Failure("provider_exception", retryable = true) }
        return when (result) {
            is ProviderResult.Success -> listOf(
                MysticEvent.SendInput(pending.input),
                MysticEvent.ReplySucceeded(
                    pending.sessionToken,
                    pending.turnId,
                    DialogueReply(engine.classify(pending.input), "", result.text)
                )
            )
            is ProviderResult.Failure -> listOf(
                MysticEvent.SendInput(pending.input),
                MysticEvent.ReplyFailed(pending.sessionToken, pending.turnId, result.reason)
            )
        }
    }
}
