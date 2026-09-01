package com.xuanji.app.domain.game

/**
 * Bridges natural-language game commands into real game events and grounds every
 * character reply in rule facts: piece names, coordinates, captures, checks, and
 * outcomes are read from [BoardMove] / [RuleResult] / [GameOutcome] — never invented.
 * Fortune data must never leak into board conclusions (and vice versa).
 */
class GameDialogueBridge(
    /** Injected for tests or a native engine; null selects the local search per difficulty. */
    private val engine: BoardEngine? = null
) {

    /** Result of handling one user input inside the game domain. */
    data class Result(
        val event: GameEvent?,
        val reply: String,
        val state: GameSessionState,
        val grounded: Boolean,
        /** True when the position now awaits an engine move: callers must drive [engineReply]. */
        val awaitEngine: Boolean = false
    )

    /** True while a board game session is open (started but not exited, or has moves). */
    fun activeGame(state: GameSessionState): Boolean =
        state.sessionToken != 0L || state.history.isNotEmpty() || state.outcome.isTerminal()

    /**
     * Whether the local engine owes a move: it is not the human's turn and the session
     * is still alive. Spectating (playerColor = WHITE) always returns true until settled.
     */
    fun shouldAskEngine(state: GameSessionState): Boolean =
        activeGame(state) && state.position.sideToMove != state.playerColor &&
            !state.outcome.isTerminal()

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
            GlobalCommand.ENDGAMES -> return listEndgames(state)
            null -> Unit
        }
        startEndgame(trimmed)?.let { return it(state) }

        // in-game commands require an active session (non-initial or explicitly started)
        val inGame = state.sessionToken != 0L || state.history.isNotEmpty()
        if (!inGame) {
            return Result(null, "还没有棋局。说「来一盘象棋」开始对弈。", state, grounded = false)
        }

        return when (classifyInGame(trimmed)) {
            InGameCommand.UNDO -> undo(state)
            InGameCommand.REDO -> redo(state)
            InGameCommand.HINT -> hint(state)
            InGameCommand.REVIEW -> review(state)
            InGameCommand.THREATS -> threats(state)
            InGameCommand.DIFFICULTY -> setDifficulty(state, trimmed)
            InGameCommand.SPECTATE -> spectate(state)
            InGameCommand.COLOR_BLACK -> colorChoice(state, black = true)
            InGameCommand.COLOR_RED -> colorChoice(state, black = false)
            InGameCommand.CHAT -> Result(null, "棋局进行中——可以说着法（如「走炮二平五」）、「悔棋」、「提示」、「有哪些威胁」、「复盘」，或「退出棋局」。", state, grounded = true)
            null -> tryMove(state, trimmed)
        }
    }

    // ---- commands ----------------------------------------------------------------

    /**
     * Grid tap path: the UI already knows from/to squares, so this bypasses notation
     * parsing entirely (eliminating same-file two-rook ambiguity). The move is still
     * fully re-verified by [XiangqiRules] — identical discipline to typed moves.
     */
    fun applySquareMove(state: GameSessionState, from: Square, to: Square): Result {
        val position = state.position
        val piece = position.pieceAt(from)
            ?: return Result(null, "起始格没有棋子。", state, grounded = true)
        if (piece.color != position.sideToMove) {
            return Result(null, turnText(position.sideToMove), state, grounded = true)
        }
        val notation = XiangqiNotation.format(
            BoardMove(from, to, "", player = piece.color),
            position
        )
        val result = XiangqiRules.apply(position, BoardMove(from, to, notation, player = piece.color))
        return when (result) {
            is RuleResult.Applied -> committed(
                state,
                reduceGame(state, GameEvent.ApplyMove(state.sessionToken, result.move)),
                result.move,
                GameEvent.ApplyMove(state.sessionToken, result.move)
            )
            is RuleResult.Rejected -> Result(null, describeRejection(result.code), state, grounded = true)
        }
    }

    /**
     * Play the engine side's reply. The move is produced by the engine seam and then
     * re-verified by [XiangqiRules] inside the reducer, so a bad reply cannot reach the board.
     */
    suspend fun engineReply(state: GameSessionState): Result {
        if (!shouldAskEngine(state)) return Result(null, "", state, grounded = false)
        val mover = state.position.sideToMove
        val result = engineFor(state.difficulty).bestMove(state.position, mover, state.sessionToken)
        return when (result) {
            is EngineResult.Move -> {
                val turn = result.turn
                val next = reduceGame(state, GameEvent.EngineReply(state.sessionToken, turn))
                if (next.history.size <= state.history.size) {
                    Result(
                        null,
                        "引擎回包未通过本地规则校验（${turn.move.notation.ifEmpty { "非法着法" }}），本手已忽略。",
                        state,
                        grounded = true
                    )
                } else {
                    val applied = next.history.last()
                    committed(
                        state,
                        next,
                        applied,
                        GameEvent.EngineReply(state.sessionToken, EngineTurn(applied)),
                        speaker = "我"
                    )
                }
            }
            is EngineResult.NoMove -> Result(
                null,
                "当前局面已无合法走法：${describeOutcome(state)}",
                state,
                grounded = true
            )
        }
    }

    private fun engineFor(difficulty: String): BoardEngine = engine ?: SmartBoardEngine(difficulty)

    /** Build the grounded reply for a move that just landed on the board. */
    private fun committed(
        previous: GameSessionState,
        next: GameSessionState,
        move: BoardMove,
        event: GameEvent,
        speaker: String? = null
    ): Result {
        val notation = move.notation.ifEmpty { XiangqiNotation.format(move, previous.position) }
        val capture = move.captured?.let { "，吃掉${it}" } ?: ""
        val who = speaker ?: (if (move.player == PlayerColor.RED) "红方" else "黑方")
        val prefix = if (speaker != null) "${who}走「$notation」$capture。" else "已走「$notation」$capture。"
        return Result(
            event,
            prefix + resultText(next),
            next,
            grounded = true,
            awaitEngine = shouldAskEngine(next)
        )
    }

    private fun startGame(state: GameSessionState, input: String): Result {
        val token = state.sessionToken + 1
        val playerColor = when {
            input.contains("观战") || input.contains("猜先") -> PlayerColor.WHITE
            playerWantsBlack(input) -> PlayerColor.BLACK
            else -> PlayerColor.RED
        }
        return openSession(
            state,
            GameEvent.Start(
                type = GameType.XIANGQI,
                token = token,
                playerColor = playerColor,
                difficulty = SmartBoardEngine.parseLabel(input) ?: state.difficulty
            ),
            playerColor
        )
    }

    private fun openSession(
        state: GameSessionState,
        event: GameEvent.Start,
        playerColor: PlayerColor
    ): Result {
        val next = reduceGame(state, event)
        val colorText = when (playerColor) {
            PlayerColor.BLACK -> "你执黑，我执红先行。"
            PlayerColor.WHITE -> "我两边都下，你观战。"
            else -> "你执红先行。"
        }
        val title = event.title.ifEmpty { "中国象棋开局" }
        val hint = if (event.title.isEmpty()) " 轮到你走子，例如「走炮二平五」。" else ""
        return Result(
            event = event,
            reply = "$title。$colorText$hint" + resultText(next),
            state = next,
            grounded = true,
            awaitEngine = shouldAskEngine(next)
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
        if (undone == state) return Result(null, "悔棋未生效：已经是最初的局面。", state, grounded = true)
        return Result(
            GameEvent.Undo(state.sessionToken),
            "已悔棋，回到你上次走子前（剩余 ${state.history.size - undone.history.size} 手可重做）。",
            undone,
            grounded = true,
            awaitEngine = shouldAskEngine(undone)
        )
    }

    private fun redo(state: GameSessionState): Result {
        if (state.redo.isEmpty()) return Result(null, "没有可重做的走法。", state, grounded = true)
        val next = reduceGame(state, GameEvent.Redo(state.sessionToken))
        val move = next.history.lastOrNull()
            ?: return Result(null, "重做未通过规则校验，盘面保持不变。", state, grounded = true)
        return Result(
            GameEvent.Redo(state.sessionToken),
            "已重做「${move.notation.ifEmpty { XiangqiNotation.format(move, state.position) }}」。" + resultText(next),
            next,
            grounded = true,
            awaitEngine = shouldAskEngine(next)
        )
    }

    private fun hint(state: GameSessionState): Result {
        val color = state.position.sideToMove
        val result = kotlinx.coroutines.runBlocking {
            engineFor(state.difficulty).bestMove(state.position, color, state.sessionToken)
        }
        return when (result) {
            is EngineResult.Move -> {
                val move = result.turn.move
                val notation = move.notation.ifEmpty { XiangqiNotation.format(move, state.position) }
                Result(
                    null,
                    "建议「$notation」（本地离线搜索 ${SmartBoardEngine.depthOf(state.difficulty)} 层，基于当前局面，非强度评级）。",
                    state,
                    grounded = true
                )
            }
            is EngineResult.NoMove -> Result(
                null,
                "当前局面已无合法走法：${describeOutcome(state)}",
                state,
                grounded = true
            )
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

    /** Threat report reads the real attack map: only pieces enemy legal moves can reach. */
    private fun threats(state: GameSessionState): Result {
        val defender = if (state.playerColor == PlayerColor.WHITE) state.position.sideToMove else state.playerColor
        val threats = BoardAnalysis.threatsAgainst(state.position, defender)
        val side = if (defender == PlayerColor.RED) "红方" else "黑方"
        val text = if (threats.isEmpty()) {
            "当前${side}没有正被攻击的棋子。"
        } else {
            val lines = threats.take(4).joinToString("；") {
                "${it.attackedPiece}（${XiangqiNotation.coordinate(it.attacked)}）正被${it.attackerPiece}（${XiangqiNotation.coordinate(it.attacker)}）盯住"
            }
            "${side}有 ${threats.size} 个子正被攻击：$lines。" +
                if (threats.size > 4) " 其余 ${threats.size - 4} 个可用「提示」逐一查看。" else ""
        }
        return Result(null, text, state, grounded = true)
    }

    private fun listEndgames(state: GameSessionState): Result {
        val lines = EndgameCatalog.ALL.mapIndexed { index, puzzle ->
            "${index + 1}. ${puzzle.title}"
        }.joinToString("；")
        return Result(
            null,
            "残局共 ${EndgameCatalog.ALL.size} 关：$lines。说「开第 1 关」进入。",
            state,
            grounded = true
        )
    }

    /**「开第2关」/「来局残局」/ puzzle title → load a catalog position as a real session. */
    private fun startEndgame(input: String): ((GameSessionState) -> Result)? {
        if (!input.contains("残局") && !input.contains("关")) return null
        val puzzle = EndgameCatalog.ALL.firstOrNull { input.contains(it.title) }
            ?: input.getIntAtEnd(EndgameCatalog.ALL.size)?.let { EndgameCatalog.ALL[it - 1] }
            ?: return null
        return { state ->
            openSession(
                state,
                GameEvent.Start(
                    type = GameType.XIANGQI,
                    token = state.sessionToken + 1,
                    playerColor = puzzle.solver,
                    difficulty = state.difficulty,
                    position = puzzle.position(),
                    title = puzzle.title
                ),
                puzzle.solver
            )
        }
    }

    private fun String.getIntAtEnd(max: Int): Int? {
        val digit = filter { it.isDigit() }.firstOrNull()?.toString()?.toIntOrNull()
        val chinese = CN_NUMBERS.entries.firstOrNull { contains(it.key) }?.value
        val value = digit ?: chinese ?: return null
        return value.takeIf { it in 1..max }
    }

    private fun setDifficulty(state: GameSessionState, input: String): Result {
        val level = SmartBoardEngine.parseLabel(input)
            ?: return Result(null, "难度只支持「轻松 / 普通 / 困难」，当前是${SmartBoardEngine.labelOf(state.difficulty)}。", state, grounded = true)
        val next = reduceGame(state, GameEvent.SetDifficulty(state.sessionToken, level))
        return Result(
            GameEvent.SetDifficulty(state.sessionToken, level),
            "难度已切到${SmartBoardEngine.labelOf(level)}（搜索 ${SmartBoardEngine.depthOf(level)} 层）。",
            next,
            grounded = true
        )
    }

    /** Hand both sides to the engine; the caller keeps driving [engineReply]. */
    private fun spectate(state: GameSessionState): Result {
        val next = reduceGame(state, GameEvent.SetColor(state.sessionToken, PlayerColor.WHITE))
        return Result(
            GameEvent.SetColor(state.sessionToken, PlayerColor.WHITE),
            "这局我两边都下，你观战。说「退出棋局」随时结束。",
            next,
            grounded = true,
            awaitEngine = shouldAskEngine(next)
        )
    }

    private fun colorChoice(state: GameSessionState, black: Boolean): Result {
        val wanted = if (black) PlayerColor.BLACK else PlayerColor.RED
        val side = if (black) "黑方" else "红方"
        if (state.history.isEmpty()) {
            val next = reduceGame(state, GameEvent.SetColor(state.sessionToken, wanted))
            return Result(
                GameEvent.SetColor(state.sessionToken, wanted),
                "本局你执$side，${if (wanted == PlayerColor.BLACK) "由我执红先行" else "轮到你走子"}。",
                next,
                grounded = true,
                awaitEngine = shouldAskEngine(next)
            )
        }
        return Result(
            null,
            "已经走了 ${state.history.size} 手，中途不换色。可以「悔棋」回到起点或「退出棋局」重开。",
            state,
            grounded = true
        )
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
            return Result(null, turnText(position.sideToMove), state, grounded = true)
        }
        val result = XiangqiRules.apply(position, parsed)
        return when (result) {
            is RuleResult.Applied -> committed(
                state,
                reduceGame(state, GameEvent.ApplyMove(state.sessionToken, result.move)),
                result.move,
                GameEvent.ApplyMove(state.sessionToken, result.move)
            )
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
                "没有识别出这手棋。可以说「走炮二平五」这样的着法，或说「提示」看应手建议。",
                state,
                grounded = false
            )
        }
    }

    // ---- grounding helpers ---------------------------------------------------------

    /**
     * Post-move result wording. Every clause comes from [GameSessionState.outcome] or a
     * real draw rule; the win/loss framing is relative to the human's own color only.
     */
    private fun resultText(state: GameSessionState): String = when (val outcome = state.outcome) {
        is GameOutcome.Checkmate -> "绝杀！${winnerName(outcome.winner)}胜${fromPlayerView(state, outcome.winner)}"
        is GameOutcome.Stalemate -> "困毙！${winnerName(outcome.winner)}胜${fromPlayerView(state, outcome.winner)}"
        is GameOutcome.Draw -> "和棋（${drawReasonText(state.drawReason())}）。"
        is GameOutcome.Check -> "将军！"
        is GameOutcome.IllegalPosition -> "局面不合规则（${outcome.reason}），请重开一局。"
        GameOutcome.InProgress -> ""
    }

    private fun winnerName(winner: PlayerColor): String =
        if (winner == PlayerColor.RED) "红方" else "黑方"

    private fun fromPlayerView(state: GameSessionState, winner: PlayerColor): String = when {
        state.playerColor == PlayerColor.WHITE -> "（观战）"
        winner == state.playerColor -> "，你赢了。"
        else -> "，你输了。"
    }

    private fun drawReasonText(reason: String?): String = when (reason) {
        "repetition" -> "双方不变作和"
        "no_capture_limit" -> "无吃子限着判和"
        else -> "判和"
    }

    private fun turnText(side: PlayerColor): String =
        "现在轮到${if (side == PlayerColor.RED) "红方" else "黑方"}行棋。"

    private fun describeRejection(code: String): String = when (code) {
        XiangqiRules.ERR_FROM_EMPTY -> "这手棋不能走：起始位置没有你的棋子。"
        XiangqiRules.ERR_WRONG_TURN -> "这手棋不能走：现在不是该方行棋。"
        XiangqiRules.ERR_SELF_CHECK -> "这手棋不能走：走完后己方会被将军（送将）。"
        XiangqiRules.ERR_GAME_OVER -> "棋局已经结束，不能继续走子。可以「复盘」或「退出棋局」。"
        else -> "这手棋不合法，请检查走法。"
    }

    private fun describeOutcome(state: GameSessionState): String = when (val outcome = state.outcome) {
        is GameOutcome.Checkmate -> "绝杀，${winnerName(outcome.winner)}胜"
        is GameOutcome.Stalemate -> "困毙，${winnerName(outcome.winner)}胜"
        is GameOutcome.Draw -> "和棋（${drawReasonText(state.drawReason())}）"
        is GameOutcome.Check -> "被将军"
        else -> "对局进行中"
    }

    /** Terminal result from the human's side: "win" / "loss" / "draw", null while running. */
    fun settledResult(state: GameSessionState): String? = when (val outcome = state.outcome) {
        is GameOutcome.Checkmate -> scoreFor(state, outcome.winner)
        is GameOutcome.Stalemate -> scoreFor(state, outcome.winner)
        is GameOutcome.Draw -> "draw"
        else -> null
    }

    private fun scoreFor(state: GameSessionState, winner: PlayerColor): String? = when (state.playerColor) {
        PlayerColor.WHITE -> null
        winner -> "win"
        else -> "loss"
    }

    private fun normalize(raw: String): String = raw.trim()

    // ---- classification ------------------------------------------------------------

    private enum class GlobalCommand { START_XIANGQI, START_GO, START_CHESS, EXIT, SAVE, ENDGAMES }

    private enum class InGameCommand {
        UNDO, REDO, HINT, REVIEW, THREATS, DIFFICULTY, SPECTATE,
        COLOR_BLACK, COLOR_RED, CHAT
    }

    private fun classifyGlobal(input: String): GlobalCommand? = when {
        input.contains("围棋") -> GlobalCommand.START_GO
        input.contains("国际象棋") -> GlobalCommand.START_CHESS
        containsXiangqiStart(input) -> GlobalCommand.START_XIANGQI
        input.contains("退出棋局") || input == "退出" -> GlobalCommand.EXIT
        input.contains("保存棋局") || input == "保存" -> GlobalCommand.SAVE
        input.contains("残局") -> GlobalCommand.ENDGAMES
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
        input.contains("悔棋") || input.contains("悔一步") -> InGameCommand.UNDO
        input.contains("重做") || input.contains("下一手") -> InGameCommand.REDO
        input.contains("观战") || input.contains("两边都下") || input.contains("替我下") -> InGameCommand.SPECTATE
        input.contains("威胁") || input.contains("被吃") || input.contains("盯住") -> InGameCommand.THREATS
        SmartBoardEngine.parseLabel(input) != null -> InGameCommand.DIFFICULTY
        input.contains("提示") || input.contains("建议") -> InGameCommand.HINT
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

        private val CN_NUMBERS = mapOf(
            "一" to 1, "二" to 2, "三" to 3, "四" to 4, "五" to 5,
            "六" to 6, "七" to 7, "八" to 8, "九" to 9, "十" to 10
        )
    }
}
