package com.xuanji.app.domain.game

/** Inspect the real current state, never play a move or rewrite an archive as a side effect. */
object CompanionGameTeaching {
    data class PoetryStructure(val upperLength: Int, val lowerLength: Int, val equalLength: Boolean, val explanation: String)
    fun poetryStructure(upper: String, lower: String): PoetryStructure {
        fun count(value: String): Int = value.codePoints().filter { Character.isLetterOrDigit(it) }.count().toInt()
        val a = count(upper); val b = count(lower)
        return PoetryStructure(a, b, a > 0 && a == b,
            "上句${a}字，下句${b}字；${if (a > 0 && a == b) "字数相称" else "字数尚不相称"}。仅检查字数，不判断平仄、词性或严格古典对仗；关卡分数仍按原有简化尾字规则，不能当作诗律认证。")
    }
    fun shortestStarPath(state: StarMapState): List<String> {
        if (!StarMapPuzzleEngine.isValidState(state)) return emptyList()
        val puzzle = StarMapPuzzleEngine.puzzle(state)
        val queue = ArrayDeque<List<String>>()
        val seen = mutableSetOf(state.current)
        queue.add(listOf(state.current))
        while (queue.isNotEmpty()) {
            val path = queue.removeFirst()
            if (path.last() == puzzle.target) return path
            puzzle.edges[path.last()].orEmpty().sorted().forEach { next ->
                if (next in puzzle.edges && seen.add(next)) queue.add(path + next)
            }
        }
        return emptyList()
    }
    fun xiangqiSteps(position: BoardPosition, move: BoardMove): List<String> {
        val from = move.from ?: return emptyList()
        val to = move.to
        val legal = XiangqiRules.legalMoves(position, position.sideToMove).firstOrNull { it.from == from && it.to == to } ?: return emptyList()
        val piece = position.pieceAt(from) ?: return emptyList()
        val applied = XiangqiRules.apply(position, legal) as? RuleResult.Applied ?: return emptyList()
        val captured = position.pieceAt(to)
        val outcome = XiangqiRules.outcome(applied.position)
        return listOf(
            "1. 观察当前行棋方：${if (position.sideToMove == PlayerColor.RED) "红方" else "黑方"}；先检查将帅安全。",
            "2. 合法走法：${XiangqiNotation.format(legal, position)}；移动${BoardAnalysis.pieceName(piece)}，规则引擎已核对阻挡、蹩腿/塞象眼或炮架等适用条件。",
            "3. 目的格：${captured?.let { "可吃${BoardAnalysis.pieceName(it)}" } ?: "没有被吃子"}；不凭空增加棋子。",
            "4. 执行后的规则状态：${when (outcome) {
                is GameOutcome.Checkmate -> "将死"
                is GameOutcome.Check -> "将军"
                is GameOutcome.Stalemate -> "困毙"
                GameOutcome.Draw -> "和棋"
                GameOutcome.InProgress -> "继续行棋"
                is GameOutcome.IllegalPosition -> "非法局面"
            }}。这是一步合法性演练，不宣称已证明多步必胜。"
        )
    }
}
