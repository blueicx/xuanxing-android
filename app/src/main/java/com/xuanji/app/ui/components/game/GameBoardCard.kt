package com.xuanji.app.ui.components.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xuanji.app.domain.game.BoardMove
import com.xuanji.app.domain.game.BoardPosition
import com.xuanji.app.domain.game.GameOutcome
import com.xuanji.app.domain.game.Piece
import com.xuanji.app.domain.game.PieceKind
import com.xuanji.app.domain.game.PlayerColor
import com.xuanji.app.domain.game.SmartBoardEngine
import com.xuanji.app.domain.game.Square
import com.xuanji.app.domain.game.XiangqiRules
import com.xuanji.app.ui.components.rememberReducedMotion
import kotlin.math.roundToInt

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
            history: List<BoardMove> = emptyList(),
            /** Session verdict; null derives it from [position] (rules only, no draw rules). */
            outcome: GameOutcome? = null
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
            val verdict = outcome ?: XiangqiRules.outcome(position)
            val outcomeText = when (verdict) {
                is GameOutcome.Checkmate ->
                    "绝杀，${if (verdict.winner == PlayerColor.RED) "红方" else "黑方"}胜"
                is GameOutcome.Stalemate ->
                    "困毙，${if (verdict.winner == PlayerColor.RED) "红方" else "黑方"}胜"
                is GameOutcome.Check -> "将军"
                GameOutcome.Draw -> "和棋"
                else -> null
            }
            // whose general is attacked right now (drives the pulsing highlight)
            val sideInCheck = when (verdict) {
                is GameOutcome.Check -> verdict.byColor
                is GameOutcome.Checkmate -> verdict.winner.opponent()
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
    /** Halfmove being reviewed; null (or history.size) means the live position. */
    viewPly: Int? = null,
    /** Live session verdict (including draw rules); review frames ignore it. */
    outcome: GameOutcome? = null,
    lineColor: Color = Color(0xFF7A5C43),
    boardColor: Color = Color(0xFFE8D5B0),
    panelColor: Color = Color(0xFF221A13),
    redPieceColor: Color = Color(0xFFB3261E),
    blackPieceColor: Color = Color(0xFF26211C),
    pieceFont: FontFamily = FontFamily.Serif,
    difficulty: String = SmartBoardEngine.NORMAL,
    thinking: Boolean = false,
    canRedo: Boolean = false,
    onSquareTap: (Pair<Square, Square>) -> Unit = {},
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    onHint: () -> Unit = {},
    onExit: () -> Unit = {},
    onRestart: (() -> Unit)? = null,
    onDifficultyChange: ((String) -> Unit)? = null,
    onStep: ((Int) -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null
) {
    var selected by remember(position) { mutableStateOf<Square?>(null) }
    val totalPly = history.size
    val ply = (viewPly ?: totalPly).coerceIn(0, totalPly)
    val isReviewing = ply < totalPly
    // review frames show their own history slice: capture trays never leak the future
    val shownHistory = history.take(ply)
    val uiModel = GameBoardUiModel.from(
        position = position,
        selected = selected.takeUnless { isReviewing },
        history = shownHistory,
        outcome = outcome.takeUnless { isReviewing }
    )
    val haptics = LocalHapticFeedback.current
    // last move read from real history only
    val lastMove = shownHistory.lastOrNull()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = panelColor,
        tonalElevation = 2.dp
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // ---- status bar -------------------------------------------------------
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val moverText = when {
                    thinking -> "引擎推演中···"
                    isReviewing -> "回放第 $ply/$totalPly 手"
                    uiModel.sideToMove == PlayerColor.RED -> "红方行棋"
                    uiModel.sideToMove == PlayerColor.BLACK -> "黑方行棋"
                    else -> ""
                }
                Text(
                    text = moverText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (uiModel.sideToMove == PlayerColor.RED && !isReviewing) {
                        Color(0xFFE8A87C)
                    } else {
                        Color(0xFFCFC5B8)
                    },
                    modifier = Modifier.semantics { contentDescription = moverText }
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
                lastMove = lastMove,
                ply = ply,
                animateLastMove = !isReviewing && !thinking,
                sideInCheck = uiModel.sideInCheck,
                lineColor = lineColor,
                boardColor = boardColor,
                redPieceColor = redPieceColor,
                blackPieceColor = blackPieceColor,
                pieceFont = pieceFont,
                onSquareTap = { square ->
                    if (isReviewing || thinking) {
                        selected = null
                    } else {
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
                }
            )

            // ---- difficulty ---------------------------------------------------------
            if (onDifficultyChange != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmartBoardEngine.LEVELS.forEach { level ->
                        GameBoardChip(
                            label = SmartBoardEngine.labelOf(level),
                            selected = level == difficulty,
                            modifier = Modifier.weight(1f),
                            onClick = { if (!thinking) onDifficultyChange(level) }
                        )
                    }
                }
            }

            // ---- playback ------------------------------------------------------------
            if (onStep != null && totalPly > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GameBoardActionButton(
                        label = "上一手",
                        description = "回看上一步",
                        modifier = Modifier.weight(1f),
                        enabled = ply > 0,
                        onClick = { onStep(ply - 1) }
                    )
                    GameBoardActionButton(
                        label = "下一手",
                        description = "看下一步",
                        modifier = Modifier.weight(1f),
                        enabled = ply < totalPly,
                        onClick = { onStep(ply + 1) }
                    )
                    if (isReviewing) {
                        GameBoardActionButton(
                            label = "最新",
                            description = "回到当前局面",
                            modifier = Modifier.weight(1f),
                            onClick = { onStep(totalPly) }
                        )
                    }
                }
            }

            // ---- controls ----------------------------------------------------------
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GameBoardActionButton(
                    label = "悔棋",
                    description = "撤销上一手",
                    modifier = Modifier.weight(1f),
                    enabled = !thinking && totalPly > 0,
                    onClick = onUndo
                )
                GameBoardActionButton(
                    label = "重做",
                    description = "恢复刚悔掉的走法",
                    modifier = Modifier.weight(1f),
                    enabled = !thinking && canRedo,
                    onClick = onRedo
                )
                GameBoardActionButton(
                    label = "提示",
                    description = "请求提示",
                    modifier = Modifier.weight(1f),
                    enabled = !thinking && !isReviewing,
                    onClick = onHint
                )
                if (onRestart != null) {
                    GameBoardActionButton(
                        label = "重开",
                        description = "再来一盘",
                        modifier = Modifier.weight(1f),
                        enabled = !thinking,
                        onClick = onRestart
                    )
                }
                GameBoardActionButton(
                    label = "退出",
                    description = "退出棋局",
                    modifier = Modifier.weight(1f),
                    onClick = onExit
                )
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
    lastMove: BoardMove?,
    ply: Int,
    animateLastMove: Boolean,
    sideInCheck: PlayerColor?,
    lineColor: Color,
    boardColor: Color,
    redPieceColor: Color,
    blackPieceColor: Color,
    pieceFont: FontFamily,
    onSquareTap: (Square) -> Unit
) {
    val hairline = 0.5.dp
    val reducedMotion = rememberReducedMotion()
    // only a live move slides; review frames and reduced motion render the final position
    val canSlide = animateLastMove && !reducedMotion && lastMove != null && lastMove.from != lastMove.to
    val slideFrom = if (canSlide) lastMove?.from else null
    val slideTo = if (canSlide) lastMove?.to else null
    Column(
        Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 10f)
            .background(boardColor, RoundedCornerShape(10.dp))
            .border(hairline, lineColor, RoundedCornerShape(10.dp))
            .padding(4.dp)
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val cellW = maxWidth / 9f
            // the 楚河汉界 strip is a fixed 22.dp; the ten rank rows split what is left
            val cellH = (maxHeight - 22.dp) / 10f
            Column(Modifier.fillMaxSize()) {
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
                            val isLastMove = square == lastMove?.from || square == lastMove?.to
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
                                    val glow = if (reducedMotion) {
                                        0.55f
                                    } else {
                                        val transition = rememberInfiniteTransition(label = "check")
                                        transition.animateFloat(
                                            initialValue = 0.25f,
                                            targetValue = 0.8f,
                                            animationSpec = infiniteRepeatable(
                                                tween(620),
                                                RepeatMode.Reverse
                                            ),
                                            label = "checkGlow"
                                        ).value
                                    }
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
                                    val arriving = square == slideTo
                                    val slide = remember(square, ply) { Animatable(if (arriving) 1f else 0f) }
                                    LaunchedEffect(square, ply) {
                                        if (arriving) {
                                            slide.animateTo(
                                                targetValue = 0f,
                                                animationSpec = tween(
                                                    durationMillis = 190,
                                                    easing = FastOutSlowInEasing
                                                )
                                            )
                                        }
                                    }
                                    val lift by animateFloatAsState(
                                        targetValue = if (isSelected) 1f else 0f,
                                        animationSpec = tween(120),
                                        label = "pieceLift"
                                    )
                                    Box(
                                        Modifier
                                            .fillMaxSize(0.88f)
                                            .offset {
                                                val progress = if (arriving) slide.value else 0f
                                                val origin = slideFrom
                                                IntOffset(
                                                    if (arriving && origin != null) {
                                                        ((origin.file - square.file) * cellW.toPx() * progress)
                                                            .roundToInt()
                                                    } else {
                                                        0
                                                    },
                                                    if (arriving && origin != null) {
                                                        ((origin.rank - square.rank) * cellH.toPx() * progress)
                                                            .roundToInt()
                                                    } else {
                                                        0
                                                    } + (2 - 2 * lift).dp.toPx().roundToInt()
                                                )
                                            }
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
                                            fontFamily = pieceFont,
                                            fontWeight = FontWeight.Bold,
                                            color = if (piece.color == PlayerColor.RED) {
                                                redPieceColor
                                            } else {
                                                blackPieceColor
                                            }
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
    description: String = label,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics { contentDescription = description },
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF4A3728),
        enabled = enabled,
        onClick = onClick
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) Color(0xFFF5EFE4) else Color(0xFFF5EFE4).copy(alpha = 0.35f)
            )
        }
    }
}

/** Difficulty selector chip: the active level is the only filled one. */
@Composable
private fun GameBoardChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .heightIn(min = 40.dp)
            .semantics {
                contentDescription = if (selected) "$label 难度，已选择" else "选择$label 难度"
            },
        shape = RoundedCornerShape(999.dp),
        color = if (selected) Color(0xFF6B4A2F) else Color(0xFF33261C),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) Color(0xFFD9A05B) else Color(0xFF4A3728)
        ),
        onClick = onClick
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) Color(0xFFFFE7C2) else Color(0xFFB9A99A)
            )
        }
    }
}
