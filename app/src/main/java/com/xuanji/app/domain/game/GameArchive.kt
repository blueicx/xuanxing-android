package com.xuanji.app.domain.game

/**
 * Persisted form of one board session. Only rule data crosses this boundary — the opening
 * position as [XiangqiBoard.encode] plus every halfmove as UCI — so a resume replays the
 * whole game through [XiangqiRules] instead of trusting a stored position, and character
 * commentary can never be written into (or read out of) an archive.
 */
data class GameSave(
    val version: Int = GameArchive.VERSION,
    val start: String = "",
    val moves: List<String> = emptyList(),
    val playerColor: String = PlayerColor.RED.name,
    val difficulty: String = SmartBoardEngine.NORMAL,
    val title: String = "",
    val savedAt: Long = 0L
)

sealed interface GameRestore {
    /** Session rebuilt by replaying the archive; [dropped] halfmoves failed verification. */
    data class Loaded(val state: GameSessionState, val dropped: Int) : GameRestore
    data class Rejected(val reason: String) : GameRestore
}

object GameArchive {

    const val VERSION = 1

    fun saveOf(state: GameSessionState, savedAt: Long = 0L): GameSave = GameSave(
        start = XiangqiBoard.encode(state.startPosition),
        moves = state.history.mapNotNull { move ->
            move.from?.let { XiangqiNotation.toUci(it, move.to) }
        },
        playerColor = state.playerColor.name,
        difficulty = state.difficulty,
        title = state.title,
        savedAt = savedAt
    )

    /**
     * Rebuild a session from [save]. Every halfmove goes back through the reducer, so
     * captures, checks, the draw log and the terminal outcome are recomputed rather than
     * trusted, and replay stops at the first move the rules reject.
     */
    fun restore(save: GameSave, token: Long): GameRestore {
        if (save.version > VERSION || save.start.isEmpty()) return GameRestore.Rejected("unreadable")
        val start = runCatching { XiangqiBoard.decode(save.start) }.getOrNull()
            ?: return GameRestore.Rejected("unreadable")
        val startVerdict = XiangqiRules.outcome(start)
        if (startVerdict is GameOutcome.IllegalPosition) {
            return GameRestore.Rejected(startVerdict.reason)
        }
        val playerColor = runCatching { PlayerColor.valueOf(save.playerColor) }.getOrNull()
            ?: PlayerColor.RED
        var state = reduceGame(
            GameSessionState(sessionToken = token),
            GameEvent.Start(
                type = GameType.XIANGQI,
                token = token,
                playerColor = playerColor,
                difficulty = save.difficulty.takeIf { it in SmartBoardEngine.LEVELS }
                    ?: SmartBoardEngine.NORMAL,
                position = start,
                title = save.title
            )
        )
        var dropped = 0
        for ((index, uci) in save.moves.withIndex()) {
            val squares = XiangqiNotation.fromUciOrNull(uci)
            val move = squares?.let {
                BoardMove(it.first, it.second, "", player = state.position.sideToMove)
            }
            val next = move?.let { reduceGame(state, GameEvent.ApplyMove(token, it)) } ?: state
            if (next == state) {
                dropped = save.moves.size - index
                break
            }
            state = next
        }
        return GameRestore.Loaded(state, dropped)
    }
}

/** Scoreboard: counts settled results only, so a commentary line can never move a number. */
data class GameRecord(
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val lastResult: String = ""
) {
    val games: Int get() = wins + losses + draws

    fun tally(result: String?): GameRecord = when (result) {
        "win" -> copy(wins = wins + 1, lastResult = resultLabel(result))
        "loss" -> copy(losses = losses + 1, lastResult = resultLabel(result))
        "draw" -> copy(draws = draws + 1, lastResult = resultLabel(result))
        else -> this
    }

    fun summaryText(): String = if (games == 0) {
        "还没有已结束的棋局。"
    } else {
        "战绩 $wins 胜 $losses 负 $draws 和，共 $games 局。" + if (lastResult.isNotEmpty()) "上一局$lastResult。" else ""
    }

    companion object {
        /** 结算令牌到战绩板上那一个字；没有对应令牌就没有说法。 */
        fun resultLabel(result: String?): String = when (result) {
            "win" -> "胜"
            "loss" -> "负"
            "draw" -> "和"
            else -> ""
        }

        /** 长期记忆里的终局一行；没结算就返回空串，调用方因此无从伪造。 */
        fun settledNote(result: String?): String {
            val label = resultLabel(result)
            return if (label.isEmpty()) "" else "象棋·$label"
        }
    }
}
