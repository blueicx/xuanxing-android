package com.xuanji.app.domain

/**
 * Bridges a provider to the pure session reducer. It never mutates UI state;
 * callers apply the returned events in order and may safely discard stale ones.
 */
class MysticDialogueCoordinator(
    private val provider: DialogueProvider,
    private val engine: MysticDialogueEngine = DefaultMysticDialogueEngine(),
    private val analyzer: MysticDialogueAnalyzer = DefaultMysticDialogueAnalyzer()
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
            is ProviderResult.Success -> {
                val analysis = analyzer.analyze(pending.input, requestContext)
                val validation = if (provider is OfflineDialogueProvider) {
                    ValidationResult.Accept
                } else {
                    DialogueReplyValidator.validate(result.text, requestContext)
                }
                val reply = when (validation) {
                    ValidationResult.Accept -> DialogueReply(
                        intent = analysis.intent,
                        prefix = "",
                        text = result.text,
                        clarifiers = clarifiersFor(analysis),
                        source = if (provider is OfflineDialogueProvider) ReplySource.Offline else ReplySource.OnlineValidated
                    )
                    is ValidationResult.Reject -> offlineReply(requestContext, pending.input, analysis)
                }
                listOf(
                    MysticEvent.SendInput(pending.input),
                    MysticEvent.ReplySucceeded(pending.sessionToken, pending.turnId, reply)
                )
            }
            is ProviderResult.Failure -> listOf(
                MysticEvent.SendInput(pending.input),
                MysticEvent.ReplySucceeded(
                    pending.sessionToken,
                    pending.turnId,
                    offlineReply(
                        requestContext,
                        pending.input,
                        analyzer.analyze(pending.input, requestContext)
                    )
                )
            )
        }
    }

    private fun offlineReply(
        context: DialogueContext,
        input: String,
        analysis: DialogueAnalysis
    ): DialogueReply {
        val reply = runCatching { engine.reply(context, input) }.getOrElse {
            DialogueReply(
                intent = analysis.intent,
                prefix = "",
                text = "我先按离线方式接住这句；盘面资料暂时不完整，我们可以换个角度再看。"
            )
        }
        return reply.copy(
            intent = analysis.intent,
            clarifiers = clarifiersFor(analysis),
            source = ReplySource.OnlineFallback
        )
    }
}
