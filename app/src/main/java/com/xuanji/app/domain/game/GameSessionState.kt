package com.xuanji.app.domain.game

private const val NO_CAPTURE_LIMIT = 60

/**
 * Generic game session state and reducer. Token discipline mirrors MysticSessionState:
 * any event whose token does not match the current session token is dropped untouched,
 * so stale async engine replies can never pollute a newer context.
 */
data class GameSessionState(
    val sessionToken: Long = 0L,
    val gameType: GameType = GameType.XIANGQI,
    val position: BoardPosition = XiangqiBoard.initial(),
    /** Opening position of this session: endgame puzzles start away from the standard array. */
    val startPosition: BoardPosition = XiangqiBoard.initial(),
    val history: List<BoardMove> = emptyList(),
    val redo: List<BoardMove> = emptyList(),
    val request: GameRequest = GameRequest.Idle,
    val outcome: GameOutcome = GameOutcome.InProgress,
    /** Side the human controls; [PlayerColor.WHITE] means the engine plays both sides. */
    val playerColor: PlayerColor = PlayerColor.RED,
    val difficulty: String = SmartBoardEngine.NORMAL,
    /** Endgame puzzle title, empty for regular games. */
    val title: String = "",
    /** Every position encoding since this session opened, first entry = [startPosition]. */
    val positionLog: List<String> = listOf(XiangqiBoard.encode(XiangqiBoard.initial()))
) {

    /**
     * Draw rule that ended this session: "repetition" (threefold) or "no_capture_limit"
     * (60 quiet halfmoves). Null when neither applies, so dialogue wording never guesses.
     */
    fun drawReason(): String? = when {
        positionLog.isNotEmpty() &&
            positionLog.count { it == positionLog.last() } >= 3 -> "repetition"
        history.takeLastWhile { it.captured == null }.size >= NO_CAPTURE_LIMIT -> "no_capture_limit"
        else -> null
    }
}

sealed interface GameEvent {
    data class Start(
        val type: GameType,
        val token: Long,
        val playerColor: PlayerColor = PlayerColor.RED,
        val difficulty: String = SmartBoardEngine.NORMAL,
        val position: BoardPosition = XiangqiBoard.initial(),
        val title: String = ""
    ) : GameEvent
    data class ApplyMove(val token: Long, val move: BoardMove) : GameEvent
    data class EngineReply(val token: Long, val turn: EngineTurn) : GameEvent
    data class Undo(val token: Long) : GameEvent
    data class Redo(val token: Long) : GameEvent
    data class SetDifficulty(val token: Long, val difficulty: String) : GameEvent
    data class SetColor(val token: Long, val playerColor: PlayerColor) : GameEvent
    data class Cancel(val token: Long) : GameEvent
    data object Exit : GameEvent
}

fun reduceGame(state: GameSessionState, event: GameEvent): GameSessionState = when (event) {
    is GameEvent.Start -> state.copy(
        sessionToken = event.token,
        gameType = event.type,
        position = event.position,
        startPosition = event.position,
        playerColor = event.playerColor,
        difficulty = event.difficulty,
        title = event.title,
        history = emptyList(),
        redo = emptyList(),
        request = GameRequest.Idle,
        outcome = GameOutcome.InProgress,
        positionLog = listOf(XiangqiBoard.encode(event.position))
    )
    is GameEvent.ApplyMove -> if (event.token != state.sessionToken) {
        state
    } else {
        state.applyPlayerMove(event.move)
    }
    is GameEvent.EngineReply -> if (event.token != state.sessionToken) {
        state
    } else {
        state.applyEngineTurn(event.turn)
    }
    is GameEvent.Undo -> if (event.token != state.sessionToken) state else state.undoLastPair()
    is GameEvent.Redo -> if (event.token != state.sessionToken) state else state.redoNextMove()
    is GameEvent.SetDifficulty -> if (event.token != state.sessionToken) {
        state
    } else {
        state.copy(difficulty = event.difficulty)
    }
    is GameEvent.SetColor -> if (event.token != state.sessionToken) {
        state
    } else {
        state.copy(playerColor = event.playerColor)
    }
    is GameEvent.Cancel -> if (event.token != state.sessionToken) {
        state
    } else {
        state.copy(request = GameRequest.Idle)
    }
    GameEvent.Exit -> GameSessionState()
}

private fun GameSessionState.applyPlayerMove(move: BoardMove): GameSessionState {
    if (outcome.isTerminal()) return this
    val side = position.sideToMove
    val result = XiangqiRules.apply(position, move.copy(player = side))
    val applied = result as? RuleResult.Applied ?: return this
    return advance(applied.position, applied.move, clearedRedo = true)
}

private fun GameSessionState.applyEngineTurn(turn: EngineTurn): GameSessionState {
    if (outcome.isTerminal()) return this
    val side = position.sideToMove
    // engine replies are re-verified against local rules before touching state
    val result = XiangqiRules.apply(position, turn.move.copy(player = side))
    val applied = result as? RuleResult.Applied ?: return this
    return advance(applied.position, applied.move, clearedRedo = true)
}

private fun GameSessionState.advance(
    next: BoardPosition,
    move: BoardMove,
    clearedRedo: Boolean
): GameSessionState {
    val newHistory = history + move
    val newLog = positionLog + XiangqiBoard.encode(next)
    return copy(
        position = next,
        history = newHistory,
        redo = if (clearedRedo) emptyList() else redo,
        positionLog = newLog,
        outcome = evaluateOutcome(next, newLog, newHistory),
        request = GameRequest.Idle
    )
}

private fun GameSessionState.redoNextMove(): GameSessionState {
    val next = redo.firstOrNull() ?: return this
    val result = XiangqiRules.apply(position, next.copy(player = position.sideToMove))
    val applied = result as? RuleResult.Applied ?: return this
    val newHistory = history + applied.move
    val newLog = positionLog + XiangqiBoard.encode(applied.position)
    return copy(
        position = applied.position,
        history = newHistory,
        redo = redo.drop(1),
        positionLog = newLog,
        outcome = evaluateOutcome(applied.position, newLog, newHistory)
    )
}

internal fun GameOutcome.isTerminal(): Boolean =
    this is GameOutcome.Checkmate || this is GameOutcome.Stalemate ||
        this is GameOutcome.Draw || this is GameOutcome.IllegalPosition

/**
 * Outcome evaluation for settled positions: pure rule checks first (mate/stalemate/
 * illegal), then the draw rules read from the session's own position log.
 */
private fun evaluateOutcome(
    position: BoardPosition,
    positionLog: List<String>,
    history: List<BoardMove>
): GameOutcome {
    val ruleOutcome = XiangqiRules.outcome(position)
    when (ruleOutcome) {
        is GameOutcome.Checkmate, is GameOutcome.Stalemate, is GameOutcome.IllegalPosition ->
            return ruleOutcome
        else -> Unit
    }
    val repetition = positionLog.isNotEmpty() &&
        positionLog.count { it == positionLog.last() } >= 3
    val quiet = history.takeLastWhile { it.captured == null }.size >= NO_CAPTURE_LIMIT
    return if (repetition || quiet) GameOutcome.Draw else ruleOutcome
}

private fun GameSessionState.undoLastPair(): GameSessionState {
    if (history.isEmpty()) return this
    // Normal play rewinds a full round (player move + engine reply) so the human is back
    // in front of their own turn. While spectating, every ply is engine-played: one back.
    // Rebuilding by replaying from the session's opening position guarantees an exact
    // restoration, captured pieces included.
    val plies = if (playerColor == PlayerColor.WHITE) {
        1
    } else if (position.sideToMove == history.last().player.opponent()) {
        2
    } else {
        1
    }
    val target = (history.size - plies).coerceAtLeast(0)
    var rebuilt = startPosition
    for (move in history.take(target)) {
        val result = XiangqiRules.apply(rebuilt, move) as? RuleResult.Applied ?: return this
        rebuilt = result.position
    }
    val undoneMoves = history.drop(target)
    return copy(
        position = rebuilt,
        history = history.take(target),
        // chronological, so redo replays the round in the original order
        redo = (undoneMoves + redo),
        positionLog = positionLog.take(target + 1),
        outcome = GameOutcome.InProgress
    )
}

private fun PlayerColor.opponent(): PlayerColor =
    if (this == PlayerColor.RED) PlayerColor.BLACK else PlayerColor.RED
