package com.xuanji.app.ui.components.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xuanji.app.domain.game.BoardPosition
import com.xuanji.app.domain.game.GameOutcome
import com.xuanji.app.domain.game.Piece
import com.xuanji.app.domain.game.PlayerColor
import com.xuanji.app.domain.game.Square
import com.xuanji.app.domain.game.XiangqiRules

/**
 * Pure Kotlin mapping from a game session position to what the board renders: selected
 * piece, legal target squares, and the current mover. UI tests target this mapper first.
 */
data class GameBoardUiModel(
    val sideToMove: PlayerColor,
    val pieces: Map<Square, Piece>,
    val legalTargets: Set<Square>,
    val outcomeText: String?
) {
    companion object {
        fun from(position: BoardPosition, selected: Square?): GameBoardUiModel {
            val pieces = buildMap {
                for (rank in 0..9) for (file in 0..8) {
                    val piece = position.pieceAt(Square(file, rank))
                    if (piece != null) put(Square(file, rank), piece)
                }
            }
            val legalTargets = if (selected == null) {
                emptySet()
            } else {
                val piece = position.pieceAt(selected)
                if (piece == null || piece.color != position.sideToMove) {
                    emptySet()
                } else {
                    XiangqiRules.legalMoves(position, position.sideToMove)
                        .filter { it.from == selected }
                        .map { it.to }
                        .toSet()
                }
            }
            val outcomeText = when (val outcome = XiangqiRules.outcome(position)) {
                is GameOutcome.Checkmate -> "绝杀，${if (outcome.winner == PlayerColor.RED) "红方" else "黑方"}胜"
                is GameOutcome.Stalemate -> "困毙，${if (outcome.winner == PlayerColor.RED) "红方" else "黑方"}胜"
                is GameOutcome.Check -> "将军"
                else -> null
            }
            return GameBoardUiModel(position.sideToMove, pieces, legalTargets, outcomeText)
        }
    }
}

/**
 * 9x10 xiangqi board card. Only squares rendered from the session position; tapping a
 * own piece shows legal targets from [GameBoardUiModel]; tapping a target commits the
 * move through [onSquareTap]. Buttons keep >= 48dp touch targets with TalkBack labels.
 */
@Composable
fun GameBoardCard(
    position: BoardPosition,
    modifier: Modifier = Modifier,
    boardBackground: Color = Color(0xFFE8D5B0),
    redPieceColor: Color = Color(0xFFB3261E),
    blackPieceColor: Color = Color(0xFF1C1B1F),
    onSquareTap: (Pair<Square, Square>) -> Unit = {},
    onUndo: () -> Unit = {},
    onHint: () -> Unit = {},
    onExit: () -> Unit = {},
    footer: (@Composable () -> Unit)? = null
) {
    var selected by remember(position) { mutableStateOf<Square?>(null) }
    val uiModel = GameBoardUiModel.from(position, selected)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = boardBackground,
        tonalElevation = 2.dp
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (uiModel.sideToMove) {
                        PlayerColor.RED -> "红方行棋"
                        PlayerColor.BLACK -> "黑方行棋"
                        PlayerColor.WHITE -> ""
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4A3728),
                    modifier = Modifier.semantics { contentDescription = "当前行棋方" }
                )
                uiModel.outcomeText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = redPieceColor
                    )
                }
            }

            // board: rank 9 (red back rank) rendered at the bottom
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                for (rank in 9 downTo 0) {
                    Row(Modifier.fillMaxWidth().aspectRatio(9f / 1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        for (file in 0..8) {
                            val square = Square(file, rank)
                            val piece = uiModel.pieces[square]
                            val isTarget = square in uiModel.legalTargets
                            val isSelected = selected == square
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .background(
                                        when {
                                            isSelected -> Color(0xFFFFF3C4)
                                            else -> boardBackground
                                        },
                                        RoundedCornerShape(4.dp)
                                    )
                                    .then(
                                        if (isTarget) {
                                            Modifier.border(1.5.dp, Color(0xFF2E7D32), RoundedCornerShape(4.dp))
                                        } else Modifier
                                    )
                                    .clickable {
                                        when {
                                            piece != null && piece.color == uiModel.sideToMove -> selected = square
                                            isTarget -> {
                                                selected?.let { from -> onSquareTap(from to square) }
                                                selected = null
                                            }
                                            else -> selected = null
                                        }
                                    }
                                    .semantics {
                                        contentDescription = piece?.let { XiangqiPieceGlyphs.description(it) }
                                            ?: if (isTarget) "可落子" else "空格"
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (piece != null) {
                                    Box(
                                        Modifier
                                            .fillMaxSize(0.86f)
                                            .background(Color(0xFFFFFBF0), CircleShape)
                                            .border(1.dp, Color(0xFF8D6E63), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = XiangqiPieceGlyphs.glyph(piece),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (piece.color == PlayerColor.RED) redPieceColor else blackPieceColor
                                        )
                                    }
                                } else if (isTarget) {
                                    Box(
                                        Modifier
                                            .size(10.dp)
                                            .background(Color(0xFF2E7D32).copy(alpha = 0.55f), CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GameBoardActionButton("撤销上一手", Modifier.weight(1f), onUndo)
                GameBoardActionButton("请求提示", Modifier.weight(1f), onHint)
                GameBoardActionButton("退出棋局", Modifier.weight(1f), onExit)
            }

            footer?.invoke()
        }
    }
}

@Composable
private fun GameBoardActionButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics { contentDescription = label },
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF4A3728),
        onClick = onClick
    ) {
        Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFF5EFE4)
            )
        }
    }
}
