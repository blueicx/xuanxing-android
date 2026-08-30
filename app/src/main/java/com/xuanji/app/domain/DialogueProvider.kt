package com.xuanji.app.domain

/** Request passed to an optional provider. The app remains offline unless a provider is explicitly supplied. */
data class DialogueRequest(
    val context: DialogueContext,
    val input: String,
    val sessionToken: Long
)

sealed interface ProviderResult {
    data class Success(val text: String) : ProviderResult
    data class Failure(val reason: String, val retryable: Boolean = false) : ProviderResult
}

interface DialogueProvider {
    suspend fun complete(request: DialogueRequest): ProviderResult
}

/** Offline provider used by default; it delegates to the deterministic local engine. */
class OfflineDialogueProvider(
    private val engine: MysticDialogueEngine = DefaultMysticDialogueEngine()
) : DialogueProvider {
    override suspend fun complete(request: DialogueRequest): ProviderResult = runCatching {
        ProviderResult.Success(engine.reply(request.context, request.input).text)
    }.getOrElse { ProviderResult.Failure("offline_dialogue_failed", retryable = false) }
}
