package com.xuanji.app.ui.components.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xuanji.app.domain.game.BoardMove
import com.xuanji.app.domain.game.BoardPosition
import com.xuanji.app.domain.game.GameOutcome
import com.xuanji.app.domain.game.Piece
import com.xuanji.app.domain.game.PieceKind
import com.xuanji.app.domain.game.PlayerColor
import com.xuanji.app.domain.game.Square
import com.xuanji.app.domain.game.XiangqiRules

/**
 * Pure Kotlin mapping from a game session position to what the board renders: selected
 * piece, legal target squares, check state, capture trays, and the current mover. UI
 * tests target this mapper first; the composable only renders what this model exposes.
 */
data class GameBoardUiModel(
    val sideToMove: PlayerColor,
    val pieces: Map<Square, Piece>,
    val legalTargets: Set<Square>,
    val outcomeText: String?,
    val sideInCheck: PlayerColor?,
    val capturedByRed: List<String>,
    val capturedByBlack: List<String>
) {
    companion object {
        fun from(
            position: BoardPosition,
            selected: Square?,
            history: List<BoardMove> = emptyList()
        ): GameBoardUiModel {
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
            val outcome = XiangqiRules.outcome(position)
            val outcomeText = when (outcome) {
                is GameOutcome.Checkmate -> "绝杀，${if (outcome.winner == PlayerColor.RED) "红方" else "黑方"}胜"
                is GameOutcome.Stalemate -> "困毙，${if (outcome.winner == PlayerColor.RED) "红方" else "黑方"}胜"
                is GameOutcome.Check -> "将军"
                GameOutcome.Draw -> "和棋"
                else -> null
            }
            // whose general is attacked right now (drives the pulsing highlight)
            val sideInCheck = when (outcome) {
                is GameOutcome.Check -> outcome.byColor
                is GameOutcome.Checkmate -> outcome.winner.opponent()
                else -> null
            }
            // captured pieces read from real move history only: never invented
            val capturedByRedList = history.filter { it.player == PlayerColor.RED }.mapNotNull { it.captured }
            val capturedByBlackList = history.filter { it.player == PlayerColor.BLACK }.mapNotNull { it.captured }
            return GameBoardUiModel(
                sideToMove = position.sideToMove,
                pieces = pieces,
                legalTargets = legalTargets,
                outcomeText = outcomeText,
                sideInCheck = sideInCheck,
                capturedByRed = capturedByRedList,
                capturedByBlack = capturedByBlackList
            )
        }
    }
}

private fun PlayerColor.opponent(): PlayerColor =
    if (this == PlayerColor.RED) PlayerColor.BLACK else PlayerColor.RED

/**
 * Traditional xiangqi board card. The board renders rank 0 (Black) at the top and rank
 * 9 (Red) at the bottom with the 楚河汉界 river strip between rows 4 and 5; grid lines
 * and palace diagonals come from per-cell line segments. Tapping an own piece shows
 * legal targets; tapping a target commits the move through [onSquareTap]. Everything
 * shown derives from [position] and [history] — never from UI-side invention.
 */
@Composable
fun GameBoardCard(
    position: BoardPosition,
    modifier: Modifier = Modifier,
    history: List<BoardMove> = emptyList(),
    lineColor: Color = Color(0xFF7A5C43),
    redPieceColor: Color = Color(0xFFB3261E),
    blackPieceColor: Color = Color(0xFF26211C),
    onSquareTap: (Pair<Square, Square>) -> Unit = {},
    onUndo: () -> Unit = {},
    onHint: () -> Unit = {},
    onExit: () -> Unit = {},
    onRestart: (() -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null
) {
    var selected by remember(position) { mutableStateOf<Square?>(null) }
    val uiModel = GameBoardUiModel.from(position, selected, history)
    val haptics = LocalHapticFeedback.current
    // last move read from real history only
    val lastMoveSquares = remember(history) {
        history.lastOrNull()?.let { listOfNotNull(it.from, it.to) }?.toSet().orEmpty()
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF221A13),
        tonalElevation = 2.dp
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // ---- status bar -------------------------------------------------------
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val moverText = when (uiModel.sideToMove) {
                    PlayerColor.RED -> "红方行棋"
                    PlayerColor.BLACK -> "黑方行棋"
                    PlayerColor.WHITE -> ""
                }
                Text(
                    text = moverText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (uiModel.sideToMove == PlayerColor.RED) Color(0xFFE8A87C) else Color(0xFFCFC5B8),
                    modifier = Modifier.semantics { contentDescription = "当前行棋方" }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (uiModel.capturedByRed.isNotEmpty() || uiModel.capturedByBlack.isNotEmpty()) {
                        Text(
                            text = listOf(
                                "红吃: " + uiModel.capturedByRed.joinToString(""),
                                "黑吃: " + uiModel.capturedByBlack.joinToString("")
                            ).filter { it.length > 4 }.joinToString("  "),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB9A99A),
                            modifier = Modifier.semantics { contentDescription = "双方吃子记录" }
                        )
                    }
                    uiModel.outcomeText?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB4AB)
                        )
                    }
                }
            }

            // ---- board -------------------------------------------------------------
            BoardGrid(
                position = position,
                selected = selected,
                legalTargets = uiModel.legalTargets,
                lastMoveSquares = lastMoveSquares,
                sideInCheck = uiModel.sideInCheck,
                lineColor = lineColor,
                redPieceColor = redPieceColor,
                blackPieceColor = blackPieceColor,
                onSquareTap = { square ->
                    val piece = position.pieceAt(square)
                    when {
                        piece != null && piece.color == position.sideToMove -> {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selected = if (selected == square) null else square
                        }
                        selected != null && square in uiModel.legalTargets -> {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSquareTap(selected!! to square)
                            selected = null
                        }
                        else -> selected = null
                    }
                }
            )

            // ---- controls ----------------------------------------------------------
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GameBoardActionButton("撤销上一手", Modifier.weight(1f), onUndo)
                GameBoardActionButton("请求提示", Modifier.weight(1f), onHint)
                if (onRestart != null) {
                    GameBoardActionButton("再来一盘", Modifier.weight(1f), onRestart)
                }
                GameBoardActionButton("退出棋局", Modifier.weight(1f), onExit)
            }

            footer?.invoke()
        }
    }
}

/**
 * The board body: 10 rank rows (rank 0 top / Black … rank 9 bottom / Red) plus the
 * 楚河汉界 river strip between rank 4 and rank 5. Grid lines are drawn per-cell:
 * horizontal lines as row top/bottom borders, vertical segments as per-cell side
 * borders (outer files full height, inner files broken across the river), and palace
 * diagonals as corner dot clusters in the two 3x3 ends. Every cell stays one tap +
 * TalkBack target.
 */
@Composable
private fun BoardGrid(
    position: BoardPosition,
    selected: Square?,
    legalTargets: Set<Square>,
    lastMoveSquares: Set<Square>,
    sideInCheck: PlayerColor?,
    lineColor: Color,
    redPieceColor: Color,
    blackPieceColor: Color,
    onSquareTap: (Square) -> Unit
) {
    val hairline = 0.5.dp
    Column(
        Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 10f)
            .background(Color(0xFFE8D5B0), RoundedCornerShape(10.dp))
            .border(hairline, lineColor, RoundedCornerShape(10.dp))
            .padding(4.dp)
    ) {
        for (row in 0..9) {
            val rank = row // top row = rank 0 (Black), bottom = rank 9 (Red)
            Row(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                for (file in 0..8) {
                    val square = Square(file, rank)
                    val piece = position.pieceAt(square)
                    val isTarget = square in legalTargets
                    val isLastMove = square in lastMoveSquares
                    val isSelected = selected == square
                    val isRiverRow = rank == 4 || rank == 5
                    // horizontal line: top edge of row 0 draws the board's top border
                    val horizontalModifier = if (rank == 0) {
                        Modifier.border(0.5.dp, lineColor)
                    } else {
                        Modifier
                    }
                    // vertical lines: left edge of file 0 cells (outer left), right edge of
                    // file 8 cells (outer right); inner files break across the river strip
                    val drawLeftEdge = file == 0 && rank != 5
                    val drawRightEdge = file == 8 || !(isRiverRow && file in 1..7)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .then(horizontalModifier)
                            .then(
                                if (drawLeftEdge) {
                                    Modifier.border(width = hairline, color = lineColor)
                                } else {
                                    Modifier
                                }
                            )
                            .then(
                                if (drawRightEdge) {
                                    Modifier.border(width = hairline, color = lineColor)
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { onSquareTap(square) }
                            .semantics {
                                contentDescription = piece?.let { XiangqiPieceGlyphs.description(it) }
                                    ?: if (isTarget) "可落子格" else "空格"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // palace diagonals: dot pairs in the eight non-center palace border cells
                        if (rank <= 2 || rank >= 7) {
                            PalaceDiagonal(file, rank, lineColor)
                        }
                        // check glow: pulsing halo behind a general in check
                        if (piece != null && piece.kind == PieceKind.GENERAL && sideInCheck == piece.color) {
                            val transition = rememberInfiniteTransition(label = "check")
                            val glow by transition.animateFloat(
                                initialValue = 0.25f,
                                targetValue = 0.8f,
                                animationSpec = infiniteRepeatable(tween(620), RepeatMode.Reverse),
                                label = "checkGlow"
                            )
                            Box(
                                Modifier
                                    .fillMaxSize(0.98f)
                                    .background(Color(0xFFFF5252).copy(alpha = glow * 0.45f), CircleShape)
                            )
                        }
                        // last-move halo
                        if (isLastMove) {
                            Box(
                                Modifier
                                    .fillMaxSize(0.96f)
                                    .border(1.5.dp, Color(0xFF2E7D32).copy(alpha = 0.65f), CircleShape)
                            )
                        }
                        // the piece token
                        if (piece != null) {
                            val lift by animateFloatAsState(
                                targetValue = if (isSelected) 1f else 0f,
                                animationSpec = tween(120),
                                label = "pieceLift"
                            )
                            Box(
                                Modifier
                                    .fillMaxSize(0.88f)
                                    .offset(y = (2 - 2 * lift).dp)
                                    .background(Color(0xFFFFFBF0), CircleShape)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF2E7D32) else Color(0xFF8D6E63),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = XiangqiPieceGlyphs.glyph(piece),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (piece.color == PlayerColor.RED) redPieceColor else blackPieceColor
                                )
                            }
                        } else if (isTarget) {
                            // legal target ring
                            Box(
                                Modifier
                                    .size(16.dp)
                                    .border(2.dp, Color(0xFF2E7D32), CircleShape)
                            )
                        }
                    }
                }
            }
            // 楚河汉界 river strip between rank 4 and rank 5
            if (row == 4) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(22.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "楚 河",
                        fontSize = 11.sp,
                        letterSpacing = 3.sp,
                        color = lineColor,
                        modifier = Modifier.padding(start = 20.dp)
                    )
                    Text(
                        "汉 界",
                        fontSize = 11.sp,
                        letterSpacing = 3.sp,
                        color = lineColor,
                        modifier = Modifier.padding(end = 20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Palace X: in the two palace ends, the diagonal passes through the corner and edge
 * cells of the 3x3 box; draw a small diagonal dot in each non-center border cell so the
 * eye reads the X without a canvas layer.
 */
@Composable
private fun PalaceDiagonal(file: Int, rank: Int, lineColor: Color) {
    val inTopPalace = rank in 0..2 && file in 3..5
    val inBottomPalace = rank in 7..9 && file in 3..5
    if (!(inTopPalace || inBottomPalace)) return
    val isDiagonalCell = when (rank % 10) {
        0, 2, 7, 9 -> file == 3 || file == 5
        1, 8 -> file == 4
        else -> false
    }
    if (isDiagonalCell) {
        Box(
            Modifier
                .size(4.dp)
                .background(lineColor.copy(alpha = 0.6f), CircleShape)
        )
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
