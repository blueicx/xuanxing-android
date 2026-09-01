package com.xuanji.app.domain.game

/**
 * Generic game session state and reducer. Token discipline mirrors MysticSessionState:
 * any event whose token does not match the current session token is dropped untouched,
 * so stale async engine replies can never pollute a newer context.
 */
data class GameSessionState(
    val sessionToken: Long = 0L,
    val gameType: GameType = GameType.XIANGQI,
    val position: BoardPosition = XiangqiBoard.initial(),
    val history: List<BoardMove> = emptyList(),
    val redo: List<BoardMove> = emptyList(),
    val request: GameRequest = GameRequest.Idle,
    val outcome: GameOutcome = GameOutcome.InProgress
)

sealed interface GameEvent {
    data class Start(val type: GameType, val token: Long) : GameEvent
    data class ApplyMove(val token: Long, val move: BoardMove) : GameEvent
    data class EngineReply(val token: Long, val turn: EngineTurn) : GameEvent
    data class Undo(val token: Long) : GameEvent
    data class Cancel(val token: Long) : GameEvent
    data object Exit : GameEvent
}

fun reduceGame(state: GameSessionState, event: GameEvent): GameSessionState = when (event) {
    is GameEvent.Start -> state.copy(
        sessionToken = event.token,
        gameType = event.type,
        position = XiangqiBoard.initial(),
        history = emptyList(),
        redo = emptyList(),
        request = GameRequest.Idle,
        outcome = GameOutcome.InProgress
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
    is GameEvent.Cancel -> if (event.token != state.sessionToken) {
        state
    } else {
        state.copy(request = GameRequest.Idle)
    }
    GameEvent.Exit -> GameSessionState()
}

private fun GameSessionState.applyPlayerMove(move: BoardMove): GameSessionState {
    if (outcome is GameOutcome.Checkmate || outcome is GameOutcome.Stalemate || outcome is GameOutcome.Draw) return this
    val side = position.sideToMove
    val result = XiangqiRules.apply(position, move.copy(player = side))
    val applied = result as? RuleResult.Applied ?: return this
    val newHistory = history + applied.move
    return copy(
        position = applied.position,
        history = newHistory,
        redo = emptyList(),
        outcome = evaluateOutcome(applied.position, newHistory)
    )
}

private fun GameSessionState.applyEngineTurn(turn: EngineTurn): GameSessionState {
    if (outcome is GameOutcome.Checkmate || outcome is GameOutcome.Stalemate || outcome is GameOutcome.Draw) return this
    val side = position.sideToMove
    // engine replies are re-verified against local rules before touching state
    val result = XiangqiRules.apply(position, turn.move.copy(player = side))
    val applied = result as? RuleResult.Applied ?: return this
    val newHistory = history + applied.move
    return copy(
        position = applied.position,
        history = newHistory,
        redo = emptyList(),
        outcome = evaluateOutcome(applied.position, newHistory),
        request = GameRequest.Idle
    )
}

/**
 * Outcome evaluation for settled positions. Pure rule checks first (mate/stalemate/
 * illegal), then draw rules: threefold repetition of any position encoding and the
 * 60-halfmove no-capture limit.
 */
private fun evaluateOutcome(position: BoardPosition, history: List<BoardMove>): GameOutcome {
    val ruleOutcome = XiangqiRules.outcome(position)
    when (ruleOutcome) {
        is GameOutcome.Checkmate, is GameOutcome.Stalemate, is GameOutcome.IllegalPosition ->
            return ruleOutcome
        else -> Unit
    }
    val encodings = mutableListOf(XiangqiBoard.encode(XiangqiBoard.initial()))
    var current = XiangqiBoard.initial()
    for (move in history) {
        val applied = XiangqiRules.apply(current, move) as? RuleResult.Applied ?: break
        current = applied.position
        encodings += XiangqiBoard.encode(current)
    }
    val finalEncoding = encodings.last()
    if (encodings.count { it == finalEncoding } >= 3) return GameOutcome.Draw
    val quietHalfmoves = history.takeLastWhile { it.captured == null }.size
    if (quietHalfmoves >= 60) return GameOutcome.Draw
    return ruleOutcome
}

private fun GameSessionState.undoLastPair(): GameSessionState {
    if (history.isEmpty()) return this
    // Walk back one full round (player + engine reply). Rebuilding by replaying from the
    // initial position is cheap for a single dialogue game and guarantees exact restoration,
    // including captured pieces.
    var target = history.size - 1
    // drop back to the position before the player's last move: if the last mover was the
    // engine (side to move is the player again), also skip the player's move
    if (position.sideToMove == history.last().player.opponent()) {
        target -= 1
    }
    if (target < 0) {
        // nothing left except the undone move(s): return to initial position
        return copy(
            position = XiangqiBoard.initial(),
            history = emptyList(),
            redo = history + redo,
            outcome = GameOutcome.InProgress
        )
    }
    var rebuilt = XiangqiBoard.initial()
    for (move in history.take(target)) {
        val result = XiangqiRules.apply(rebuilt, move) as? RuleResult.Applied ?: return this
        rebuilt = result.position
    }
    val undoneMoves = history.drop(target)
    return copy(
        position = rebuilt,
        history = history.take(target),
        redo = (undoneMoves.reversed() + redo),
        outcome = GameOutcome.InProgress
    )
}

private fun PlayerColor.opponent(): PlayerColor =
    if (this == PlayerColor.RED) PlayerColor.BLACK else PlayerColor.RED
