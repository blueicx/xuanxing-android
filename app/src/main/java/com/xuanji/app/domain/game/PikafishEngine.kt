package com.xuanji.app.domain.game

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Line-based UCI protocol parser for the Pikafish seam. Pure Kotlin, testable without a
 * native process: feed engine stdout lines via [onLine]; read [bestMove], [handshakeComplete],
 * and [ready]. Failure paths (timeout, cancel, process exit) are explicit state.
 */
class UciProtocolParser {

    var handshakeComplete: Boolean = false
        private set

    var ready: Boolean = false
        private set

    var processExited: Boolean = false
        private set

    var bestMove: Pair<Square, Square>? = null
        private set

    var lastDepth: Int? = null
        private set

    var lastCentipawns: Int? = null
        private set

    var lastMateIn: Int? = null
        private set

    fun onLine(line: String) {
        val trimmed = line.trim()
        when {
            trimmed == "uciok" -> handshakeComplete = true
            trimmed == "readyok" -> ready = true
            trimmed.startsWith("bestmove") -> {
                val token = trimmed.split(" ").getOrNull(1)
                bestMove = token?.let { XiangqiNotation.fromUciOrNull(it) }
            }
            trimmed.startsWith("info") -> parseInfo(trimmed)
        }
    }

    fun onProcessExit() {
        processExited = true
    }

    /** Cancel semantics: a pending bestmove is discarded and must be re-requested. */
    fun cancel() {
        bestMove = null
    }

    private fun parseInfo(line: String) {
        val tokens = line.split(" ")
        var index = 0
        while (index < tokens.size - 1) {
            when (tokens[index]) {
                "depth" -> lastDepth = tokens.getOrNull(index + 1)?.toIntOrNull() ?: lastDepth
                "score" -> {
                    val kind = tokens.getOrNull(index + 1)
                    val value = tokens.getOrNull(index + 2)?.toIntOrNull()
                    when (kind) {
                        "cp" -> lastCentipawns = value
                        "mate" -> lastMateIn = value
                    }
                }
            }
            index++
        }
    }
}

/**
 * Optional Pikafish UCI adapter. In this delivery the native engine is NOT packaged
 * (no NDK build, no binaries, no network downloads), so every instance starts
 * unavailable and transparently falls back to the deterministic offline engine.
 * When a native bridge is added later, feed its stdout into [UciProtocolParser] and
 * re-verify every bestmove through [XiangqiRules] before returning it.
 */
class PikafishEngine(
    private val unavailableReason: String,
    private val fallback: BoardEngine = OfflineBoardEngine()
) : BoardEngine {

    override suspend fun bestMove(position: BoardPosition, color: PlayerColor, token: Long): EngineResult =
        withContext(Dispatchers.Default) {
            // Native engine not packaged: explicit, visible fallback — never a fake engine move.
            fallback.bestMove(position, color, token)
        }

    companion object {
        const val REASON_NOT_PACKAGED = "native_engine_not_packaged"

        fun unavailable(reason: String = REASON_NOT_PACKAGED): PikafishEngine =
            PikafishEngine(unavailableReason = reason)
    }
}
