package com.xuanji.app.domain.game

/**
 * Bridges natural-language game commands into real game events and grounds every
 * character reply in rule facts: piece names, coordinates, captures, checks, and
 * outcomes are read from [BoardMove] / [RuleResult] / [GameOutcome] — never invented.
 * Fortune data must never leak into board conclusions (and vice versa).
 */
class GameDialogueBridge(
    private val engine: BoardEngine = OfflineBoardEngine()
) {

    /** Result of handling one user input inside the game domain. */
    data class Result(
        val event: GameEvent?,
        val reply: String,
        val state: GameSessionState,
        val grounded: Boolean
    )

    /** True while a board game session is open (started but not exited, or has moves). */
    fun activeGame(state: GameSessionState): Boolean =
        state.sessionToken != 0L || state.history.isNotEmpty() ||
            state.outcome is GameOutcome.Checkmate || state.outcome is GameOutcome.Stalemate

    fun handle(state: GameSessionState, raw: String): Result {
        val input = normalize(raw)
        if (input.isEmpty()) {
            return Result(null, "想下棋的话，可以说「来一盘象棋」。", state, grounded = false)
        }
        val trimmed = input.take(200)

        // global commands (work inside and outside an active game)
        when (classifyGlobal(trimmed)) {
            GlobalCommand.START_XIANGQI -> return startGame(state, trimmed)
            GlobalCommand.START_GO -> return unavailable(state, "围棋")
            GlobalCommand.START_CHESS -> return unavailable(state, "国际象棋")
            GlobalCommand.EXIT -> return exitGame(state)
            GlobalCommand.SAVE -> return saveGame(state)
            null -> Unit
        }

        // in-game commands require an active session (non-initial or explicitly started)
        val inGame = state.sessionToken != 0L || state.history.isNotEmpty()
        if (!inGame) {
            return Result(null, "还没有棋局。说「来一盘象棋」开始对弈。", state, grounded = false)
        }

        return when (classifyInGame(trimmed)) {
            InGameCommand.UNDO -> undo(state)
            InGameCommand.HINT -> hint(state)
            InGameCommand.REVIEW -> review(state)
            InGameCommand.COLOR_BLACK -> colorChoice(state, black = true)
            InGameCommand.COLOR_RED -> colorChoice(state, black = false)
            InGameCommand.CHAT -> Result(null, "棋局进行中——可以说着法（如「走炮二平五」）、「悔棋」、「提示」、「复盘」，或「退出棋局」。", state, grounded = true)
            null -> tryMove(state, trimmed)
        }
    }

    // ---- commands ----------------------------------------------------------------

    private fun startGame(state: GameSessionState, input: String): Result {
        val token = state.sessionToken + 1
        val next = reduceGame(state, GameEvent.Start(GameType.XIANGQI, token))
        val colorText = if (playerWantsBlack(input)) "你执黑，我执红先行。" else "你执红先行。"
        return Result(
            event = GameEvent.Start(GameType.XIANGQI, token),
            reply = "中国象棋开局。$colorText 轮到你走子，例如「走炮二平五」。",
            state = next,
            grounded = true
        )
    }

    private fun unavailable(state: GameSessionState, name: String): Result {
        val reason = GoContracts.availability()
        return Result(
            null,
            "$name 尚未启用（${reason.reason}）。目前只支持中国象棋，说「来一盘象棋」开始。",
            state,
            grounded = true
        )
    }

    private fun exitGame(state: GameSessionState): Result {
        return Result(GameEvent.Exit, "棋局已退出，盘面与走法已清空。", GameSessionState(), grounded = true)
    }

    private fun saveGame(state: GameSessionState): Result {
        if (state.history.isEmpty()) return Result(null, "还没有可保存的走法。", state, grounded = false)
        return Result(
            null,
            "已保存局面与 ${state.history.size} 手走法（仅棋谱数据，不含角色评语）。",
            state,
            grounded = true
        )
    }

    private fun undo(state: GameSessionState): Result {
        if (state.history.isEmpty()) return Result(null, "还没有走法可以悔。", state, grounded = true)
        val undone = reduceGame(state, GameEvent.Undo(state.sessionToken))
        return Result(GameEvent.Undo(state.sessionToken), "已悔一手，回到你上次走子前。", undone, grounded = true)
    }

    private fun hint(state: GameSessionState): Result {
        val color = state.position.sideToMove
        val result = kotlinx.coroutines.runBlocking { engine.bestMove(state.position, color, state.sessionToken) }
        return when (result) {
            is EngineResult.Move -> {
                val move = result.turn.move
                val notation = move.notation.ifEmpty { XiangqiNotation.format(move, state.position) }
                Result(null, "离线应手建议：$notation（基于当前局面，非强度评级）。", state, grounded = true)
            }
            is EngineResult.NoMove -> Result(null, "当前局面已无合法走法：${describeOutcome(state.outcome)}", state, grounded = true)
        }
    }

    private fun review(state: GameSessionState): Result {
        val last = state.history.lastOrNull()
            ?: return Result(null, "还没有走法可复盘。", state, grounded = true)
        val notation = last.notation.ifEmpty { XiangqiNotation.format(last, state.position) }
        val mover = if (last.player == PlayerColor.RED) "红方" else "黑方"
        val capture = last.captured?.let { "，吃掉$it" } ?: ""
        return Result(null, "复盘：最后一手是$mover「$notation」$capture。基于当前局面。", state, grounded = true)
    }

    private fun colorChoice(state: GameSessionState, black: Boolean): Result {
        val side = if (black) "黑方" else "红方"
        return Result(null, "本局你执$side。开局阶段由红方先行，可以用「悔棋」或重开一局调整。", state, grounded = true)
    }

    private fun tryMove(state: GameSessionState, input: String): Result {
        val position = state.position

        // cross-domain claims (fortune ⇒ board outcome) are refused inside the game
        if (input.contains("运势") || input.contains("必赢") || input.contains("一定赢") || input.contains("必胜")) {
            return Result(
                null,
                "局面信息不足，先看合法走法：棋局结果只取决于盘面，与运势无关。可以说「提示」。",
                state,
                grounded = true
            )
        }

        // legality questions like「马八进七这步能走吗」— answer from real rules, no turn consumed
        val askedMove = extractMoveQuestion(input)
        if (askedMove != null) {
            val parsed = XiangqiNotation.parseOrNull(askedMove, position)
            val legal = parsed != null && parsed.player == position.sideToMove &&
                XiangqiRules.legalMoves(position, position.sideToMove).any {
                    it.from == parsed.from && it.to == parsed.to
                }
            val notation = parsed?.let { XiangqiNotation.format(it, position) } ?: askedMove
            val answer = if (legal) "可以走。「$notation」是当前局面的合法走法。" else "「$notation」在当前局面不能走。"
            return Result(null, answer, state, grounded = true)
        }

        val notationText = input.removePrefix("走").trim()
        val parsed = XiangqiNotation.parseOrNull(notationText, position)
            ?: return classifyUnrecognized(state, notationText)
        if (parsed.player != position.sideToMove) {
            return Result(null, "现在轮到${if (position.sideToMove == PlayerColor.RED) "红方" else "黑方"}，你走的这手是${if (parsed.player == PlayerColor.RED) "红方" else "黑方"}的棋。", state, grounded = true)
        }
        val result = XiangqiRules.apply(position, parsed)
        return when (result) {
            is RuleResult.Applied -> {
                val next = reduceGame(state, GameEvent.ApplyMove(state.sessionToken, result.move))
                val move = result.move
                val capture = move.captured?.let { "，吃掉${it}" } ?: ""
                val outcome = XiangqiRules.outcome(result.position)
                val outcomeText = when (outcome) {
                    is GameOutcome.Checkmate -> "绝杀！${if (outcome.winner == PlayerColor.RED) "红方" else "黑方"}胜。"
                    is GameOutcome.Stalemate -> "困毙！${if (outcome.winner == PlayerColor.RED) "红方" else "黑方"}胜。"
                    is GameOutcome.Check -> "将军！"
                    else -> ""
                }
                Result(
                    GameEvent.ApplyMove(state.sessionToken, move),
                    "已走「${move.notation}」$capture。$outcomeText",
                    next,
                    grounded = true
                )
            }
            is RuleResult.Rejected -> Result(
                null,
                describeRejection(result.code),
                state,
                grounded = true
            )
        }
    }

    /**「…这步能走吗」-style questions carry an embedded notation prefix. */
    private fun extractMoveQuestion(input: String): String? {
        if (!input.contains("能走") && !input.contains("可以走") && !input.contains("能走吗")) return null
        for (length in minOf(5, input.length) downTo 3) {
            val candidate = input.take(length)
            val body = candidate.removePrefix("走").trim()
            if (body.length in 3..5 && isLikelyNotation(body)) return body
        }
        return null
    }

    private fun isLikelyNotation(text: String): Boolean {
        if (text.isEmpty()) return false
        if (text.first() !in "车俥马傌相象仕士帅将炮砲兵卒") return false
        return text.any { it in "一二三四五六七八九123456789" } && text.any { it in "进退平" }
    }

    /**
     * Distinguish "not a move at all" from "move-shaped but illegal": both replies must be
     * grounded, but the latter names the real rule violation instead of pretending.
     */
    private fun classifyUnrecognized(state: GameSessionState, notationText: String): Result {
        val moveShaped = notationText.length in 3..5 && isLikelyNotation(notationText)
        return if (moveShaped) {
            Result(null, "「$notationText」这手棋不能走：不符合当前局面的合法走法。", state, grounded = true)
        } else {
            Result(
                null,
                "没有识别出这手棋。可以说「走炮二平五」这样的着法，或说「提示」看离线应手。",
                state,
                grounded = false
            )
        }
    }

    // ---- grounding helpers ---------------------------------------------------------

    private fun describeRejection(code: String): String = when (code) {
        XiangqiRules.ERR_FROM_EMPTY -> "这手棋不能走：起始位置没有你的棋子。"
        XiangqiRules.ERR_WRONG_TURN -> "这手棋不能走：现在不是该方行棋。"
        XiangqiRules.ERR_SELF_CHECK -> "这手棋不能走：走完后己方会被将军（送将）。"
        XiangqiRules.ERR_GAME_OVER -> "棋局已经结束，不能继续走子。可以「复盘」或「退出棋局」。"
        else -> "这手棋不合法，请检查走法。"
    }

    private fun describeOutcome(outcome: GameOutcome): String = when (outcome) {
        is GameOutcome.Checkmate -> "绝杀，${if (outcome.winner == PlayerColor.RED) "红方" else "黑方"}胜"
        is GameOutcome.Stalemate -> "困毙，${if (outcome.winner == PlayerColor.RED) "红方" else "黑方"}胜"
        is GameOutcome.Check -> "被将军"
        else -> "对局进行中"
    }

    private fun normalize(raw: String): String = raw.trim()

    // ---- classification ------------------------------------------------------------

    private enum class GlobalCommand { START_XIANGQI, START_GO, START_CHESS, EXIT, SAVE }

    private enum class InGameCommand { UNDO, HINT, REVIEW, COLOR_BLACK, COLOR_RED, CHAT }

    private fun classifyGlobal(input: String): GlobalCommand? = when {
        input.contains("围棋") -> GlobalCommand.START_GO
        input.contains("国际象棋") -> GlobalCommand.START_CHESS
        containsXiangqiStart(input) -> GlobalCommand.START_XIANGQI
        input.contains("退出棋局") || input == "退出" -> GlobalCommand.EXIT
        input.contains("保存棋局") || input == "保存" -> GlobalCommand.SAVE
        else -> null
    }

    private fun containsXiangqiStart(input: String): Boolean {
        val mentionsXiangqi = input.contains("象棋")
        if (!mentionsXiangqi) return false
        val starters = listOf("来一盘", "来一局", "下一盘", "下一局", "下一把", "开一盘", "开一局", "陪我下", "开始")
        return starters.any { input.contains(it) } || input.replace("中国象棋", "").replace("象棋", "").length <= 3
    }

    private fun playerWantsBlack(input: String): Boolean = input.contains("执黑") || input.contains("黑方")

    private fun classifyInGame(input: String): InGameCommand? = when {
        input == "悔棋" || input.contains("悔棋") -> InGameCommand.UNDO
        input.contains("提示") -> InGameCommand.HINT
        input.contains("复盘") -> InGameCommand.REVIEW
        input.contains("执黑") || input.contains("走黑") -> InGameCommand.COLOR_BLACK
        input.contains("执红") || input.contains("走红") -> InGameCommand.COLOR_RED
        isCasualChat(input) -> InGameCommand.CHAT
        else -> null
    }

    private fun isCasualChat(input: String): Boolean {
        // inside a game, short chit-chat that cannot possibly be a move is surfaced as chat
        return input.length <= 12 && !input.contains("走") && !input.contains("平") &&
            !input.contains("进") && !input.contains("退") &&
            !CHATTABLE.none { input.contains(it) }
    }

    companion object {
        private val CHATTABLE = listOf("象", "马", "车", "炮", "兵", "卒", "士", "将", "帅")
    }
}
