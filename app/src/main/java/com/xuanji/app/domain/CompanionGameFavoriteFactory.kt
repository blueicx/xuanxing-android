package com.xuanji.app.domain

import com.xuanji.app.domain.game.CompanionGameCatalog
import com.xuanji.app.domain.game.GameOutcome
import com.xuanji.app.domain.game.PlayerColor

/** Creates a compact snapshot only when the user explicitly chooses to save a finished game. */
object CompanionGameFavoriteFactory {
    const val ALGORITHM_VERSION = "local-game-summary-v1"

    fun fromResult(
        id: String,
        characterId: MysticCharacterId,
        gameId: String,
        dateKey: String,
        summary: String,
        sourceNote: String
    ): ReadingFavorite? {
        val game = CompanionGameCatalog.byId(gameId)?.takeIf { it.characterId == characterId } ?: return null
        val boundedSummary = summary.trim().take(6_000).ifBlank { return null }
        return ReadingFavorite(
            id = id.trim().take(120).ifBlank { return null },
            type = "game",
            title = "${game.title}·本局摘要",
            dateKey = dateKey.take(100),
            period = "本局",
            mode = MysticCharacterCatalog.byId(characterId).displayName,
            characterId = characterId.key,
            gameId = game.id,
            body = boundedSummary,
            evidence = sourceNote.trim().take(3_000),
            algorithmVersion = ALGORITHM_VERSION,
            createdAt = 0L
        )
    }

    fun xiangqiSummary(outcome: GameOutcome, halfMoves: Int, title: String): String? {
        val ending = when (outcome) {
            is GameOutcome.Checkmate -> "${winner(outcome.winner)}胜"
            is GameOutcome.Stalemate -> "${winner(outcome.winner)}困毙胜"
            GameOutcome.Draw -> "和棋"
            GameOutcome.InProgress, is GameOutcome.Check, is GameOutcome.IllegalPosition -> return null
        }
        val puzzle = title.trim().take(48).takeIf(String::isNotBlank)?.let { "；残局：$it" }.orEmpty()
        return "中国象棋本局结束：$ending；棋谱复核后共 ${halfMoves.coerceAtLeast(0)} 个有效半回合$puzzle。"
    }

    private fun winner(color: PlayerColor): String = if (color == PlayerColor.RED) "红方" else "黑方"
}
