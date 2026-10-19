package com.xuanji.app.domain.game

/** Describes one verified halfmove, not a claimed forced winning variation. */
object BoardStepExplanation {
    fun describe(before: BoardPosition, move: BoardMove, step: Int): String {
        val applied = XiangqiRules.apply(before, move) as? RuleResult.Applied
            ?: return "这一步无法通过当前局面的规则校验，不生成复盘结论。"
        val from = move.from ?: return "未提供起点，不生成复盘结论。"
        val piece = before.pieceAt(from) ?: return "起点没有棋子，不生成复盘结论。"
        val capture = before.pieceAt(move.to)?.let { "；吃掉${BoardAnalysis.pieceName(it)}" } ?: "；没有吃子"
        val check = when (val outcome = XiangqiRules.outcome(applied.position)) {
            is GameOutcome.Check -> "；下一方处于将军状态"
            is GameOutcome.Checkmate -> "；规则判定将死，${if (outcome.winner == PlayerColor.RED) "红方" else "黑方"}胜"
            is GameOutcome.Stalemate -> "；规则判定困毙"
            else -> ""
        }
        return "第 $step 步：${BoardAnalysis.pieceName(piece)}从${XiangqiNotation.coordinate(from)}到${XiangqiNotation.coordinate(move.to)}，是当前局面的合法走法$capture$check。可逐步查看前后棋盘；提示不是必胜路线证明。"
    }
}
