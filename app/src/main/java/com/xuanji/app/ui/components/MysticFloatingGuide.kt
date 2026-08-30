package com.xuanji.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.xuanji.app.data.model.BaziFull
import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.domain.MysticGuideGenerator
import android.content.Context
import android.content.ContextWrapper
import android.app.Activity
import kotlin.math.sin


/** 外层 Scaffold 读取此状态，沉浸舞台打开时隐藏底部导航栏。 */
val LocalImmersiveStageOpen = compositionLocalOf { mutableStateOf(false) }

/** 全局微光浮球显示状态；默认开启，完整人物仅在点击浮球后出现。 */
val LocalMysticGuideVisible = compositionLocalOf { mutableStateOf(true) }

/** 面部微动作输入：眨眼、视线与嘴角轻变化都从同一位相推导，同输入永远同输出。 */
private data class MysticFaceMotion(
    val blink: Float,
    val gazeX: Float,
    val microSmile: Float
)

@Composable
private fun rememberMysticFaceMotion(half: Boolean, reducedMotion: Boolean): MysticFaceMotion {
    if (reducedMotion) return MysticFaceMotion(blink = 0f, gazeX = 0f, microSmile = 0f)
    val phase by rememberInfiniteTransition(label = "mysticFace").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (half) 4300 else 6400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mysticPhase"
    )
    val p = ((phase % 1f) + 1f) % 1f
    val tau = (2 * Math.PI).toFloat()
    val blink = if (half) {
        maxOf(
            blinkPulse(p, 0.27f, 0.020f),
            blinkPulse(p, 0.71f, 0.016f),
            blinkPulse(p, 0.93f, 0.012f)
        )
    } else {
        maxOf(blinkPulse(p, 0.34f, 0.018f), blinkPulse(p, 0.82f, 0.015f))
    }.coerceIn(0f, 1f)
    val gazeX = if (half) {
        (0.46f * sin(p * tau * 2f + 0.7f) + 0.16f * sin(p * tau * 5f + 2.1f)).coerceIn(-1f, 1f)
    } else {
        (0.24f * sin(p * tau * 2f + 1.9f) + 0.08f * sin(p * tau * 5f + 0.4f)).coerceIn(-1f, 1f)
    }
    val microSmile = 0.06f * sin(p * tau * 3f + if (half) 2.4f else 0.8f)
    return MysticFaceMotion(blink, gazeX, microSmile)
}

private fun blinkPulse(phase: Float, center: Float, halfWidth: Float): Float {
    val distance = kotlin.math.abs(phase - center)
    val wrapped = minOf(distance, 1f - distance)
    return if (wrapped < halfWidth) 1f - wrapped / halfWidth else 0f
}

/** 当日分数折成表情倾向：+1 明快，-1 低落；中间平滑过渡，不改变算法结论。 */
private fun mysticMoodLevel(score: Int): Float = ((score - 55f) / 40f).coerceIn(-1f, 1f)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
 }

@Composable
fun MysticFloatingGuide(
    bazi: BaziFull?,
    fortune: CompositeDailyFortune?,
    modifier: Modifier = Modifier,
    content: @Composable (ScrollState) -> Unit
) {
    val guideAvailable = bazi != null && fortune != null
    val companionKey = remember(bazi, fortune) {
        if (bazi == null || fortune == null) "unavailable" else "${bazi.hashCode()}|${fortune.hashCode()}"
    }
    val suggestedMode = if (guideAvailable) {
        remember(companionKey) { MysticGuideGenerator.suggestedMode("composite", fortune!!) }
    } else ""
    var stageMode by rememberSaveable(companionKey) { mutableStateOf(suggestedMode) }
    var stageSkinId by rememberSaveable(companionKey) {
        mutableStateOf(
            if (guideAvailable) {
                MysticGuideGenerator.defaultMysticSkin(stageMode, fortune!!).id
            } else {
                ""
            }
        )
    }
    var costumeRequest by rememberSaveable(companionKey) { mutableStateOf<Pair<String, String>?>(null) }
    val skin = if (guideAvailable) {
        MysticGuideGenerator.mysticSkinVoice(stageMode, stageSkinId)
            ?: MysticGuideGenerator.defaultMysticSkin(stageMode, fortune!!)
    } else null
    var detailOpen by rememberSaveable(companionKey) { mutableStateOf(false) }
    val guideVisible = LocalMysticGuideVisible.current
    val pageScroll = rememberScrollState()
    val stageOpenState = LocalImmersiveStageOpen.current
    LaunchedEffect(detailOpen) { stageOpenState.value = detailOpen }
    val companionUiState = CompanionUiState(
        presence = if (guideVisible.value) CompanionPresence.OrbVisible else CompanionPresence.FullyHidden,
        stageOpen = detailOpen
    )

    Box(modifier.fillMaxSize()) {
        content(pageScroll)

        if (skin != null && companionUiState.presence == CompanionPresence.OrbVisible && !companionUiState.stageOpen) {
                MysticOrb(
                    roleName = if (stageMode == "half") "魔师" else "慈翁",
                color = Color(skin.garment),
                trimColor = Color(skin.trim),
                onClick = { detailOpen = true },
                scrollValue = pageScroll.value,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .imePadding()
            )
        }

        if (detailOpen && bazi != null && fortune != null && skin != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .zIndex(100f)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* consume clicks so they don't fall through */ }
            ) {
                MysticImmersiveStage(
                    half = stageMode == "half",
                    skinId = skin.id,
                    garment = Color(skin.garment),
                    trimColor = Color(skin.trim),
                    moodLevel = mysticMoodLevel(fortune!!.overallScore),
                    roleName = if (stageMode == "half") "魔师" else "慈翁",
                    onClose = { detailOpen = false },
                    topStartContent = {
                        MysticStageCostumeSwitch(
                            selectedMode = stageMode,
                            selectedSkinId = stageSkinId
                        ) { target ->
                            costumeRequest = target
                        }
                    }
                ) {
                    MysticGuideCard(
                        bazi,
                        fortune,
                        immersive = true,
                        onStageModeChange = { stageMode = it },
                        onStageSkinChange = { stageSkinId = it },
                        stageCostumeRequest = costumeRequest,
                        onStageCostumeConsumed = { costumeRequest = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun MysticImmersiveStage(
    half: Boolean,
    skinId: String,
    garment: Color,
    trimColor: Color,
    moodLevel: Float,
    roleName: String,
    onClose: () -> Unit,
    topStartContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val breath = rememberInfiniteTransition(label = "stageBreath")
    val breathValue by breath.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(4600, easing = LinearEasing)),
        label = "breathValue"
    )
    val ink = Color(0xFF0D0817)
    val gold = Color(0xFFD9C58B)

    val view = LocalView.current
    DisposableEffect(view) {
        try {
            val win = view.context.findActivity()?.window
            if (win != null) {
                WindowCompat.setDecorFitsSystemWindows(win, false)

                val controller = WindowCompat.getInsetsController(win, win.decorView)
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(
                    WindowInsetsCompat.Type.statusBars() or
                        WindowInsetsCompat.Type.navigationBars()
                )
            }
        } catch (_: Exception) {
            // Older devices may not support all window APIs
        }

        onDispose {
            try {
                val win = view.context.findActivity()?.window
                if (win != null) {
                    val controller = WindowCompat.getInsetsController(win, win.decorView)
                    controller.show(
                        WindowInsetsCompat.Type.statusBars() or
                            WindowInsetsCompat.Type.navigationBars()
                    )
                    // 关掉舞台后恢复成「沉浸式但仍让 Scaffold 分发 insets」的全局状态，
                    // 设回 true 会让整个 app 突然多出系统栏内边距，页面跳一下。
                    WindowCompat.setDecorFitsSystemWindows(win, false)
                }
            } catch (_: Exception) { }
        }
    }

    Surface(Modifier.fillMaxSize(), color = ink, contentColor = Color(0xFFF4EEE5)) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize()) { StageBackdrop(gold, moodLevel) }

            StageFigure(
                half = half,
                skinId = skinId,
                garment = garment,
                trimColor = trimColor,
                breathValue = breathValue,
                moodLevel = moodLevel,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 44.dp)
                    .fillMaxWidth(0.70f)
                    .aspectRatio(0.68f)
            )

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.50f)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.22f to ink.copy(alpha = 0.72f),
                            0.48f to ink.copy(alpha = 0.96f),
                            1f to ink
                        )
                    )
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(start = 18.dp, end = 18.dp, top = 44.dp, bottom = 10.dp)
            ) {
                MaterialTheme(
                    colorScheme = darkColorScheme(
                        primary = gold,
                        tertiary = Color(0xFFE3A579)
                    )
                ) {
                    content()
                }
            }

            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 12.dp, top = 12.dp)
            ) {
                topStartContent()
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 16.dp, top = 14.dp),
                horizontalArrangement = Arrangement.End
            ) {
                StageControl(label = "关闭玄师台", onClick = onClose) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "关闭玄师台",
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MysticStageCostumeSwitch(
    selectedMode: String,
    selectedSkinId: String,
    onSelect: (Pair<String, String>) -> Unit
) {
    val modes = listOf("scholar" to "慈翁", "half" to "魔师")
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        modes.forEach { (mode, label) ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MysticGuideGenerator.mysticSkins(mode).forEach { item ->
                    val selected = selectedMode == mode && selectedSkinId == item.id
                    Surface(
                        onClick = { onSelect(mode to item.id) },
                        shape = CircleShape,
                        color = Color(item.garment),
                        border = BorderStroke(
                            if (selected) 2.dp else 1.dp,
                            if (selected) Color(0xFFF4EEE5) else Color(item.trim)
                        ),
                        modifier = Modifier.size(17.dp)
                    ) {
                        Box(Modifier.fillMaxSize().semantics { contentDescription = "$label · ${item.label}" })
                    }
                }
            }
        }
    }
}

@Composable
private fun StageControl(
    label: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color(0xFF241731).copy(alpha = 0.68f),
        contentColor = Color(0xFFD9C58B),
        border = BorderStroke(1.dp, Color(0xFFD9C58B).copy(alpha = .28f)),
        modifier = Modifier.size(38.dp)
    ) {
        Box(contentAlignment = Alignment.Center) { icon() }
    }
}

@Composable
private fun StageFigure(
    half: Boolean,
    skinId: String,
    garment: Color,
    trimColor: Color,
    breathValue: Float,
    moodLevel: Float,
    modifier: Modifier = Modifier
) {
    val breathe = 1f + Math.sin(breathValue.toDouble()).toFloat() * .016f
    val sway = Math.sin(breathValue.toDouble() * .68f).toFloat() * 2.2f
    val tilt = Math.sin(breathValue.toDouble() * .62f).toFloat() * if (half) 1.6f else 0.9f
    val faceMotion = rememberMysticFaceMotion(half, rememberReducedMotion())

    Box(modifier) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = breathe
                    scaleY = breathe
                    translationX = sway
                    rotationZ = tilt
                    alpha = .99f
                }
        ) {
            drawMysticFigure(
                half,
                skinId,
                garment,
                trimColor,
                faceMotion.blink,
                faceMotion.gazeX,
                moodLevel,
                faceMotion.microSmile
            )
        }
    }
}

private fun Color.darken(f: Float): Color = copy(
    red = red * f,
    green = green * f,
    blue = blue * f
)

private fun DrawScope.drawWings(
    half: Boolean,
    w: Float,
    h: Float,
    cx: Float,
    trim: Color
) {
    val shoulderY = h * 0.295f
    for (side in listOf(-1f, 1f)) {
        if (!half) {
            // Angel wings: layered white-gold feathers fanning upward and outward
            val featherCount = 7
            for (i in 0 until featherCount) {
                val t = i / (featherCount - 1).toFloat()
                val baseX = cx + side * w * 0.045f
                val baseY = shoulderY + h * 0.02f + t * h * 0.085f
                val len = w * (0.31f - t * 0.15f)
                val fw = w * (0.068f - t * 0.027f)
                val angle = side * (24f + t * 36f)
                rotate(degrees = angle, pivot = Offset(baseX, baseY)) {
                    drawOval(
                        brush = Brush.verticalGradient(
                            listOf(Color.White, Color(0xFFF4E6BC)),
                            startY = baseY - len,
                            endY = baseY + fw
                        ),
                        topLeft = Offset(baseX - fw / 2f, baseY - len),
                        size = Size(fw, len + fw)
                    )
                }
            }
            drawOval(
                Color.White.copy(alpha = 0.85f),
                topLeft = Offset(cx + side * w * 0.02f - w * 0.05f, shoulderY - h * 0.02f),
                size = Size(w * 0.10f, h * 0.10f)
            )
        } else {
            // A compact, folded wing: sharp enough to read as otherworldly,
            // but quiet enough to stay behind the tailored silhouette.
            val sx = cx + side * w * 0.030f
            val sy = shoulderY + h * 0.012f
            val tipX = cx + side * w * 0.275f
            val tipY = h * 0.170f
            val wing = Path().apply {
                moveTo(sx, sy)
                cubicTo(
                    cx + side * w * 0.13f, h * 0.075f,
                    tipX - side * w * 0.055f, h * 0.125f,
                    tipX, tipY
                )
                cubicTo(
                    tipX - side * w * 0.038f, h * 0.245f,
                    cx + side * w * 0.205f, h * 0.315f,
                    cx + side * w * 0.145f, h * 0.370f
                )
                cubicTo(
                    cx + side * w * 0.105f, h * 0.345f,
                    cx + side * w * 0.070f, h * 0.345f,
                    cx + side * w * 0.045f, h * 0.325f
                )
                cubicTo(cx + side * w * 0.040f, h * 0.250f, sx, sy + h * 0.018f, sx, sy)
                close()
            }
            drawPath(
                wing,
                Brush.verticalGradient(
                    listOf(Color(0xFF571D33), Color(0xFF170712)),
                    startY = h * 0.16f,
                    endY = h * 0.40f
                )
            )
            drawLine(
                Color(0xFF944961).copy(alpha = 0.30f),
                Offset(sx + side * w * 0.006f, sy + h * 0.020f),
                Offset(cx + side * w * 0.235f, h * 0.205f),
                w * 0.0035f,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawHalfBody(
    w: Float,
    h: Float,
    cx: Float,
    garment: Color,
    trim: Color
) {
    val deep = garment.darken(0.58f)
    val deeper = garment.darken(0.38f)
    val lining = Color(0xFF220A1A)
    val shoulderY = h * 0.242f
    val waistY = h * 0.535f
    val hemY = h * 0.885f

    // Long coat: a controlled silhouette with a slight asymmetric sweep.
    val coat = Path().apply {
        moveTo(cx - w * 0.112f, shoulderY)
        cubicTo(
            cx - w * 0.132f, h * 0.330f,
            cx - w * 0.104f, h * 0.450f,
            cx - w * 0.118f, waistY
        )
        cubicTo(
            cx - w * 0.150f, h * 0.650f,
            cx - w * 0.188f, h * 0.780f,
            cx - w * 0.181f, hemY
        )
        quadraticBezierTo(cx - w * 0.020f, h * 0.918f, cx + w * 0.155f, hemY)
        cubicTo(
            cx + w * 0.178f, h * 0.745f,
            cx + w * 0.146f, h * 0.615f,
            cx + w * 0.122f, waistY
        )
        cubicTo(
            cx + w * 0.112f, h * 0.430f,
            cx + w * 0.134f, h * 0.320f,
            cx + w * 0.112f, shoulderY
        )
        quadraticBezierTo(cx, shoulderY - h * 0.028f, cx - w * 0.112f, shoulderY)
        close()
    }
    drawPath(
        coat,
        Brush.verticalGradient(
            listOf(garment, deep, deeper),
            startY = shoulderY,
            endY = hemY
        )
    )

    // Shadowed undercoat keeps the torso from flattening into one brown mass.
    drawPath(
        Path().apply {
            moveTo(cx - w * 0.048f, shoulderY + h * 0.014f)
            lineTo(cx + w * 0.020f, waistY + h * 0.012f)
            lineTo(cx + w * 0.058f, shoulderY + h * 0.016f)
            quadraticBezierTo(cx + w * 0.005f, h * 0.330f, cx - w * 0.048f, shoulderY + h * 0.014f)
            close()
        },
        Brush.verticalGradient(listOf(lining, deep), startY = shoulderY, endY = waistY)
    )

    // Lapels overlap slightly: the right side leads, the left follows.
    drawPath(
        Path().apply {
            moveTo(cx - w * 0.108f, shoulderY + h * 0.004f)
            lineTo(cx - w * 0.018f, h * 0.335f)
            lineTo(cx - w * 0.062f, h * 0.565f)
            lineTo(cx - w * 0.116f, h * 0.375f)
            close()
        },
        garment.darken(0.72f)
    )
    drawPath(
        Path().apply {
            moveTo(cx + w * 0.108f, shoulderY + h * 0.004f)
            lineTo(cx + w * 0.026f, h * 0.345f)
            lineTo(cx + w * 0.078f, h * 0.590f)
            lineTo(cx + w * 0.120f, h * 0.360f)
            close()
        },
        garment.darken(0.64f)
    )

    // Slim sleeves; the right hand is tucked away, the left hangs quiet.
    for (side in listOf(-1, 1)) {
        drawPath(
            Path().apply {
                moveTo(cx + side * w * 0.096f, shoulderY + h * 0.012f)
                cubicTo(
                    cx + side * w * 0.145f, h * 0.315f,
                    cx + side * w * 0.155f, h * 0.410f,
                    cx + side * w * 0.132f, h * 0.520f
                )
                quadraticBezierTo(
                    cx + side * w * 0.108f,
                    h * 0.552f,
                    cx + side * w * 0.076f,
                    h * 0.520f
                )
                cubicTo(
                    cx + side * w * 0.092f, h * 0.420f,
                    cx + side * w * 0.082f, h * 0.330f,
                    cx + side * w * 0.072f, shoulderY + h * 0.018f
                )
                close()
            },
            Brush.verticalGradient(listOf(deep, deeper), startY = shoulderY, endY = h * 0.56f)
        )
    }

    // Waist detail and a single restrained fastening.
    drawRoundRect(
        deep.copy(alpha = 0.94f),
        topLeft = Offset(cx - w * 0.104f, waistY - h * 0.014f),
        size = Size(w * 0.212f, h * 0.032f),
        cornerRadius = CornerRadius(w * 0.012f, w * 0.012f)
    )
    drawRoundRect(
        trim.copy(alpha = 0.72f),
        topLeft = Offset(cx - w * 0.104f, waistY - h * 0.006f),
        size = Size(w * 0.212f, h * 0.005f),
        cornerRadius = CornerRadius(w * 0.004f, w * 0.004f)
    )
    drawCircle(trim.copy(alpha = 0.88f), w * 0.012f, Offset(cx + w * 0.058f, waistY + h * 0.020f))

    // Legs and boots: narrow, grounded, not two crude blocks.
    for (side in listOf(-1, 1)) {
        val x = cx + side * w * 0.048f
        drawRoundRect(
            Brush.verticalGradient(listOf(lining, Color(0xFF150711)), startY = h * 0.56f, endY = h * 0.88f),
            topLeft = Offset(x - w * 0.034f, h * 0.565f),
            size = Size(w * 0.068f, h * 0.290f),
            cornerRadius = CornerRadius(w * 0.026f, w * 0.026f)
        )
        drawRoundRect(
            deeper,
            topLeft = Offset(x - w * 0.040f, h * 0.830f),
            size = Size(w * 0.080f, h * 0.072f),
            cornerRadius = CornerRadius(w * 0.018f, w * 0.024f)
        )
        drawLine(
            trim.copy(alpha = 0.28f),
            Offset(x - side * w * 0.020f, h * 0.845f),
            Offset(x + side * w * 0.020f, h * 0.845f),
            w * 0.0035f,
            cap = StrokeCap.Round
        )
    }

    // High collar frames the jaw without hiding it.
    drawPath(
        Path().apply {
            moveTo(cx - w * 0.068f, h * 0.222f)
            quadraticBezierTo(cx, h * 0.262f, cx + w * 0.068f, h * 0.222f)
            lineTo(cx + w * 0.058f, shoulderY + h * 0.018f)
            quadraticBezierTo(cx, h * 0.300f, cx - w * 0.058f, shoulderY + h * 0.018f)
            close()
        },
        Brush.verticalGradient(listOf(deeper, deep), startY = h * 0.22f, endY = h * 0.30f)
    )
    drawLine(
        trim.copy(alpha = 0.55f),
        Offset(cx - w * 0.052f, h * 0.252f),
        Offset(cx + w * 0.052f, h * 0.252f),
        w * 0.004f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawHalfCostumeAccent(
    skinId: String,
    w: Float,
    h: Float,
    cx: Float,
    trim: Color
) {
    when (skinId) {
        "cloud-daoist" -> {
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFFF7EEDB), Color(0xFFB58A50))),
                w * 0.016f,
                Offset(cx - w * 0.062f, h * 0.335f)
            )
            drawLine(
                trim.copy(alpha = 0.45f),
                Offset(cx - w * 0.050f, h * 0.415f),
                Offset(cx - w * 0.020f, h * 0.455f),
                w * 0.0035f,
                cap = StrokeCap.Round
            )
        }
        "street-jacket" -> {
            drawLine(
                Color(0xFFD8CDBB).copy(alpha = 0.55f),
                Offset(cx + w * 0.030f, h * 0.330f),
                Offset(cx + w * 0.078f, h * 0.470f),
                w * 0.0035f,
                cap = StrokeCap.Round
            )
        }
        "desert-traveler" -> {
            drawArc(
                trim.copy(alpha = 0.55f),
                170f,
                130f,
                false,
                style = Stroke(w * 0.0045f, cap = StrokeCap.Round),
                topLeft = Offset(cx - w * 0.085f, h * 0.300f),
                size = Size(w * 0.170f, h * 0.070f)
            )
        }
        "festival-costume" -> {
            drawCircle(Color(0xFFF4E5BB).copy(alpha = 0.85f), w * 0.011f, Offset(cx - w * 0.070f, h * 0.345f))
            drawCircle(Color(0xFFB8462F).copy(alpha = 0.70f), w * 0.006f, Offset(cx + w * 0.070f, h * 0.365f))
        }
    }
}

private fun DrawScope.drawMysticFigure(
    half: Boolean,
    skinId: String,
    garment: Color,
    trim: Color,
    blink: Float = 0f,
    gazeX: Float = 0f,
    moodLevel: Float = 0f,
    microSmile: Float = 0f,
    attentionGaze: Float = 0f
) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f

    val skin = if (half) Color(0xFFF2E2D8) else Color(0xFFFFE9D4)
    val skinShade = if (half) Color(0xFFC29C93) else Color(0xFFF2D2BA)
    val hair = if (half) Color(0xFF201328) else Color(0xFFEDE7DD)
    val ink = Color(0xFF1B1226)
    val blush = if (half) Color(0xFF8F3050) else Color(0xFFEDA794)

    drawCircle(
        brush = Brush.radialGradient(
            listOf(trim.copy(alpha = 0.12f), Color.Transparent),
            center = Offset(cx, h * 0.14f),
            radius = w * 0.38f
        ),
        radius = w * 0.38f,
        center = Offset(cx, h * 0.14f)
    )

    drawOval(
        brush = Brush.radialGradient(
            listOf(Color.Black.copy(alpha = 0.22f), Color.Transparent)
        ),
        topLeft = Offset(cx - w * 0.18f, h * 0.905f),
        size = Size(w * 0.36f, h * 0.026f)
    )

    // Neck behind robe
    drawRect(
        brush = Brush.verticalGradient(
            listOf(skinShade, skin),
            startY = h * 0.196f,
            endY = h * 0.248f
        ),
        topLeft = Offset(cx - w * 0.017f, h * 0.196f),
        size = Size(w * 0.034f, h * 0.060f)
    )

    drawWings(half, w, h, cx, trim)
    if (half) {
        drawDemonTail(w, h, cx)
        drawHalfBody(w, h, cx, garment, trim)
    } else {
        drawRobe(w, h, cx, garment, trim)
    }

    // A slightly smaller head and a clearer neck-to-shoulder line avoid the doll-like look.
    val headL = cx - w * 0.092f
    val headT = h * 0.072f
    val headW = w * 0.184f
    val headH = h * 0.128f

    drawPath(
        Path().apply {
            if (half) {
                moveTo(cx - headW * 0.50f, headT + headH * 0.24f)
                cubicTo(
                    cx - headW * 0.58f, headT + headH * 0.02f,
                    cx - headW * 0.32f, headT - headH * 0.10f,
                    cx, headT - headH * 0.08f
                )
                cubicTo(
                    cx + headW * 0.32f, headT - headH * 0.10f,
                    cx + headW * 0.58f, headT + headH * 0.02f,
                    cx + headW * 0.50f, headT + headH * 0.24f
                )
                cubicTo(
                    cx + headW * 0.47f, headT + headH * 0.53f,
                    cx + headW * 0.31f, headT + headH * 0.80f,
                    cx + headW * 0.09f, headT + headH * 0.99f
                )
                quadraticBezierTo(cx, headT + headH * 1.06f, cx - headW * 0.08f, headT + headH * 0.99f)
                cubicTo(
                    cx - headW * 0.29f, headT + headH * 0.83f,
                    cx - headW * 0.45f, headT + headH * 0.57f,
                    cx - headW * 0.50f, headT + headH * 0.24f
                )
            } else {
                moveTo(cx - headW * 0.48f, headT + headH * 0.22f)
                cubicTo(
                    cx - headW * 0.54f, headT + headH * 0.04f,
                    cx - headW * 0.32f, headT - headH * 0.06f,
                    cx, headT - headH * 0.05f
                )
                cubicTo(
                    cx + headW * 0.32f, headT - headH * 0.06f,
                    cx + headW * 0.54f, headT + headH * 0.04f,
                    cx + headW * 0.48f, headT + headH * 0.22f
                )
                cubicTo(
                    cx + headW * 0.44f, headT + headH * 0.78f,
                    cx + headW * 0.22f, headT + headH * 1.02f,
                    cx, headT + headH * 1.00f
                )
                cubicTo(
                    cx - headW * 0.22f, headT + headH * 1.02f,
                    cx - headW * 0.44f, headT + headH * 0.78f,
                    cx - headW * 0.48f, headT + headH * 0.22f
                )
            }
            close()
        },
        Brush.verticalGradient(
            listOf(skin, skinShade),
            startY = headT,
            endY = headT + headH
        )
    )

    // Cheek highlight
    drawOval(
        brush = Brush.radialGradient(
            listOf(Color.White.copy(alpha = if (half) 0.11f else 0.20f), Color.Transparent)
        ),
        topLeft = Offset(headL + headW * 0.10f, headT + headH * 0.24f),
        size = Size(headW * 0.34f, headH * 0.38f)
    )

    listOf(-1f, 1f).forEach { side ->
        drawOval(
            brush = Brush.verticalGradient(listOf(skin, skinShade)),
            topLeft = Offset(cx + side * headW * 0.47f - w * 0.007f, headT + headH * 0.44f),
            size = Size(w * 0.014f, headH * 0.16f)
        )
    }

    if (!half) listOf(-1f, 1f).forEach { side ->
        drawPath(
            Path().apply {
                moveTo(cx + side * headW * 0.20f, headT + headH * 0.34f)
                quadraticBezierTo(
                    cx + side * headW * 0.38f,
                    headT + headH * 0.52f,
                    cx + side * headW * 0.19f,
                    headT + headH * 0.74f
                )
            },
            skinShade.copy(alpha = if (half) 0.30f else 0.18f),
            style = Stroke(w * 0.004f, cap = StrokeCap.Round)
        )
    }

    drawHair(half, cx, headL, headT, headW, headH, w, h, hair, trim)
    if (half) {
        drawHalfFace(cx, w, h, headT, headW, headH, blink, gazeX, moodLevel, microSmile, attentionGaze)
        drawHalfCostumeAccent(skinId, w, h, cx, trim)
    } else {
        drawFace(
            half,
            cx,
            w,
            h,
            ink,
            blush,
            blink,
            gazeX,
            moodLevel,
            microSmile,
            attentionGaze,
            headT,
            headW,
            headH
        )
        drawCulturalCostume(skinId, half, cx, w, h, garment, trim)
        drawSkinAccessory(skinId, half, cx, w, h, trim, hair, headL, headT, headW, headH)
    }
}

private fun DrawScope.drawHalfHair(
    cx: Float,
    headL: Float,
    headT: Float,
    headW: Float,
    headH: Float,
    w: Float,
    h: Float
) {
    val headR = headL + headW
    val cap = Path().apply {
        moveTo(headL - w * 0.010f, headT + headH * 0.52f)
        cubicTo(
            headL - w * 0.030f,
            headT - headH * 0.06f,
            cx - headW * 0.30f,
            headT - headH * 0.24f,
            cx + headW * 0.02f,
            headT - headH * 0.22f
        )
        cubicTo(
            cx + headW * 0.31f,
            headT - headH * 0.20f,
            headR + w * 0.024f,
            headT - headH * 0.02f,
            headR + w * 0.004f,
            headT + headH * 0.48f
        )
        quadraticBezierTo(cx + headW * 0.04f, headT + headH * 0.18f, headL - w * 0.010f, headT + headH * 0.52f)
        close()
    }
    drawPath(
        cap,
        Brush.verticalGradient(
            listOf(Color(0xFF35203B), Color(0xFF100714)),
            startY = headT - headH * 0.22f,
            endY = headT + headH * 0.65f
        )
    )

    // One clean highlight instead of a bundle of hair strokes.
    drawPath(
        Path().apply {
            moveTo(cx - headW * 0.24f, headT + headH * 0.02f)
            quadraticBezierTo(cx - headW * 0.02f, headT - headH * 0.20f, cx + headW * 0.26f, headT + headH * 0.06f)
        },
        Color(0xFF8B6C8C).copy(alpha = 0.24f),
        style = Stroke(w * 0.005f, cap = StrokeCap.Round)
    )

    // Asymmetric fringe: the covered left eye gives him a private advantage.
    drawPath(
        Path().apply {
            moveTo(cx - headW * 0.42f, headT + headH * 0.16f)
            cubicTo(
                cx - headW * 0.24f,
                headT + headH * 0.30f,
                cx - headW * 0.20f,
                headT + headH * 0.52f,
                cx - headW * 0.30f,
                headT + headH * 0.72f
            )
            cubicTo(
                cx - headW * 0.16f,
                headT + headH * 0.50f,
                cx - headW * 0.04f,
                headT + headH * 0.30f,
                cx + headW * 0.08f,
                headT + headH * 0.10f
            )
            close()
        },
        Color(0xFF170A1C)
    )
    drawPath(
        Path().apply {
            moveTo(cx + headW * 0.08f, headT + headH * 0.06f)
            quadraticBezierTo(cx + headW * 0.30f, headT + headH * 0.16f, cx + headW * 0.36f, headT + headH * 0.40f)
            quadraticBezierTo(cx + headW * 0.20f, headT + headH * 0.24f, cx + headW * 0.04f, headT + headH * 0.16f)
            close()
        },
        Color(0xFF1E1024)
    )

    // Small, polished horns: evidence, not decoration.
    for (side in listOf(-1 to 0.86f, 1 to 0.58f)) {
        val (sideValue, scale) = side
        val baseX = cx + sideValue * headW * 0.30f
        val baseY = headT + headH * 0.02f
        val horn = Path().apply {
            moveTo(baseX - w * 0.012f, baseY)
            quadraticBezierTo(
                baseX + sideValue * w * 0.008f,
                baseY - headH * 0.30f * scale,
                baseX + sideValue * w * 0.038f,
                baseY - headH * 0.42f * scale
            )
            quadraticBezierTo(
                baseX + sideValue * w * 0.020f,
                baseY - headH * 0.14f * scale,
                baseX + w * 0.014f,
                baseY + headH * 0.03f
            )
            close()
        }
        drawPath(
            horn,
            Brush.linearGradient(
                listOf(Color(0xFF722237), Color(0xFF26060F)),
                start = Offset(baseX, baseY),
                end = Offset(baseX + sideValue * w * 0.030f, baseY - headH * 0.40f * scale)
            )
        )
    }
}

private fun DrawScope.drawHalfFace(
    cx: Float,
    w: Float,
    h: Float,
    headT: Float,
    headW: Float,
    headH: Float,
    blink: Float,
    gazeX: Float,
    moodLevel: Float,
    microSmile: Float,
    attentionGaze: Float
) {
    val skin = Color(0xFFF2E2D8)
    val eyeDx = headW * 0.215f
    val eyeRx = w * 0.017f
    val eyeRy = h * 0.0062f
    val eyeY = headT + headH * 0.56f
    val browY = headT + headH * 0.38f
    val gaze = (gazeX + attentionGaze).coerceIn(-1f, 1f) * eyeRx * 0.38f
    val closed = blink.coerceIn(0f, 1f)

    // Brows: one level and patient, the other lifted just enough to ask “why?”
    drawPath(
        Path().apply {
            moveTo(cx - eyeDx - headW * 0.11f, browY + h * 0.004f)
            quadraticBezierTo(cx - eyeDx, browY - h * 0.006f, cx - eyeDx + headW * 0.10f, browY + h * 0.004f)
        },
        Color(0xFF180914),
        style = Stroke(w * 0.0058f, cap = StrokeCap.Round)
    )
    drawPath(
        Path().apply {
            moveTo(cx + eyeDx - headW * 0.09f, browY + h * 0.006f)
            quadraticBezierTo(cx + eyeDx, browY - h * 0.018f, cx + eyeDx + headW * 0.12f, browY - h * 0.008f)
        },
        Color(0xFF180914),
        style = Stroke(w * 0.0062f, cap = StrokeCap.Round)
    )

    for (side in listOf(-1, 1)) {
        val ex = cx + side * eyeDx
        withTransform({
            scale(1f, (1f - closed).coerceAtLeast(0.06f), pivot = Offset(ex, eyeY - eyeRy))
        }) {
            // Almond eye with a quiet upper lid, not a scar or slit.
            drawPath(
                Path().apply {
                    moveTo(ex - eyeRx * 1.05f, eyeY + eyeRy * 0.16f)
                    quadraticBezierTo(ex, eyeY - eyeRy * 1.10f, ex + eyeRx * 1.10f, eyeY - eyeRy * 0.10f)
                    quadraticBezierTo(ex, eyeY + eyeRy * 0.82f, ex - eyeRx * 1.05f, eyeY + eyeRy * 0.16f)
                    close()
                },
                Color(0xFFFDF4F4)
            )
            drawCircle(Color(0xFFA31D33), eyeRx * 0.62f, Offset(ex + gaze, eyeY + eyeRy * 0.04f))
            drawCircle(Color(0xFF210309), eyeRx * 0.22f, Offset(ex + gaze, eyeY + eyeRy * 0.04f))
            drawCircle(Color.White.copy(alpha = 0.62f), eyeRx * 0.10f, Offset(ex + gaze - eyeRx * 0.24f, eyeY - eyeRy * 0.22f))

            drawPath(
                Path().apply {
                    moveTo(ex - eyeRx * 1.10f, eyeY + eyeRy * 0.10f)
                    quadraticBezierTo(ex, eyeY - eyeRy * 1.22f, ex + eyeRx * 1.16f, eyeY - eyeRy * 0.16f)
                    quadraticBezierTo(ex, eyeY - eyeRy * 0.42f, ex - eyeRx * 1.10f, eyeY + eyeRy * 0.10f)
                    close()
                },
                Color(0xFF200B16)
            )
            drawPath(
                Path().apply {
                    moveTo(ex - eyeRx * 0.82f, eyeY + eyeRy * 0.58f)
                    quadraticBezierTo(ex, eyeY + eyeRy * 0.90f, ex + eyeRx * 0.88f, eyeY + eyeRy * 0.42f)
                },
                Color(0xFF4A1B29).copy(alpha = 0.38f),
                style = Stroke(w * 0.0022f, cap = StrokeCap.Round)
            )
        }
        if (closed > 0.55f) {
            drawLine(
                Color(0xFF200B16),
                Offset(ex - eyeRx * 0.95f, eyeY),
                Offset(ex + eyeRx * 0.95f, eyeY),
                w * 0.0038f,
                cap = StrokeCap.Round
            )
        }
    }

    // Nose and cheek shading stay suggestive; no hard wrinkle lines.
    drawLine(
        Color(0xFFD0A5A2),
        Offset(cx - w * 0.002f, eyeY + h * 0.009f),
        Offset(cx + w * 0.004f, eyeY + h * 0.019f),
        w * 0.0024f,
        cap = StrokeCap.Round
    )
    drawOval(
        Brush.radialGradient(listOf(Color(0xFF33081C).copy(alpha = 0.10f), Color.Transparent)),
        topLeft = Offset(cx - eyeDx - eyeRx - w * 0.010f, eyeY + h * 0.010f),
        size = Size(w * 0.030f, h * 0.012f)
    )
    drawOval(
        Brush.radialGradient(listOf(Color(0xFF33081C).copy(alpha = 0.16f), Color.Transparent)),
        topLeft = Offset(cx + eyeDx + eyeRx - w * 0.014f, eyeY + h * 0.009f),
        size = Size(w * 0.032f, h * 0.013f)
    )

    // The smirk carries the personality; the mouth itself stays small.
    val mouthY = headT + headH * 0.82f
    val smirk = (0.82f + 0.12f * moodLevel + 0.06f * microSmile).coerceIn(0.62f, 1.02f)
    drawPath(
        Path().apply {
            moveTo(cx - w * 0.016f, mouthY + h * 0.003f)
            cubicTo(
                cx - w * 0.002f,
                mouthY + h * 0.005f * smirk,
                cx + w * 0.012f,
                mouthY - h * 0.001f * smirk,
                cx + w * 0.025f,
                mouthY - h * 0.010f * smirk
            )
        },
        Color(0xFF3B0717).copy(alpha = 0.92f),
        style = Stroke(w * 0.0034f, cap = StrokeCap.Round)
    )
    drawLine(
        Color(0xFF3B0717).copy(alpha = 0.55f),
        Offset(cx + w * 0.025f, mouthY - h * 0.010f * smirk),
        Offset(cx + w * 0.030f, mouthY - h * 0.013f * smirk),
        w * 0.0025f,
        cap = StrokeCap.Round
    )
    drawCircle(Color(0xFF43101F).copy(alpha = 0.55f), w * 0.0022f, Offset(cx + headW * 0.27f, eyeY + h * 0.021f))
}

private fun DrawScope.drawDemonTail(w: Float, h: Float, cx: Float) {
    val tail = Path().apply {
        moveTo(cx + w * .12f, h * .85f)
        cubicTo(
            cx + w * .40f, h * .81f,
            cx + w * .46f, h * .58f,
            cx + w * .30f, h * .45f
        )
    }
    drawPath(tail, Color(0xFF360C26), style = Stroke(w * .021f, cap = StrokeCap.Round))
    drawPath(
        Path().apply {
            moveTo(cx + w * .30f, h * .395f)
            lineTo(cx + w * .385f, h * .505f)
            lineTo(cx + w * .225f, h * .475f)
            close()
        },
        Brush.linearGradient(
            listOf(Color(0xFF8A2747), Color(0xFF2B0716)),
            start = Offset(cx + w * .24f, h * .43f),
            end = Offset(cx + w * .37f, h * .51f)
        )
    )
}

private fun DrawScope.drawCulturalCostume(
    skinId: String,
    half: Boolean,
    cx: Float,
    w: Float,
    h: Float,
    garment: Color,
    trim: Color
) {
    if (half) drawDemonTail(w, h, cx)

    when (skinId) {
        "jiangnan-robe" -> {
            drawPath(
                Path().apply {
                    moveTo(cx - w * .072f, h * .250f)
                    lineTo(cx + w * .085f, h * .500f)
                    lineTo(cx + w * .038f, h * .520f)
                    lineTo(cx - w * .100f, h * .275f)
                    close()
                },
                garment.darken(.58f)
            )
            drawLine(trim, Offset(cx - w * .066f, h * .262f), Offset(cx + w * .078f, h * .497f), w * .010f)
            drawRoundRect(trim.copy(alpha = .90f), Offset(cx - w * .125f, h * .480f), Size(w * .250f, h * .020f), CornerRadius(w * .012f))
            listOf(-1f, 1f).forEach { side ->
                drawRoundRect(
                    trim.copy(alpha = .82f),
                    Offset(cx + side * w * .150f - w * .030f, h * .520f),
                    Size(w * .078f, h * .055f),
                    CornerRadius(w * .020f)
                )
            }
        }

        "academy-gown" -> {
            val gown = Path().apply {
                moveTo(cx - w * .115f, h * .248f)
                cubicTo(cx - w * .205f, h * .430f, cx - w * .245f, h * .670f, cx - w * .240f, h * .900f)
                quadraticBezierTo(cx, h * .945f, cx + w * .240f, h * .900f)
                cubicTo(cx + w * .245f, h * .670f, cx + w * .205f, h * .430f, cx + w * .115f, h * .248f)
                quadraticBezierTo(cx, h * .295f, cx - w * .115f, h * .248f)
                close()
            }
            drawPath(gown, Brush.verticalGradient(listOf(Color(0xFF28304A), Color(0xFF141928)), startY = h * .25f, endY = h * .93f))
            drawPath(
                Path().apply {
                    moveTo(cx - w * .055f, h * .255f); lineTo(cx, h * .420f); lineTo(cx + w * .055f, h * .255f)
                    lineTo(cx + w * .105f, h * .320f); lineTo(cx, h * .550f); lineTo(cx - w * .105f, h * .320f); close()
                },
                trim.copy(alpha = .88f)
            )
            drawCircle(Color(0xFFF4E6BC), w * .018f, Offset(cx, h * .300f))
        }

        "silkroad-robe" -> {
            drawPath(
                Path().apply {
                    moveTo(cx - w * .180f, h * .270f)
                    quadraticBezierTo(cx, h * .380f, cx + w * .185f, h * .260f)
                    lineTo(cx + w * .210f, h * .350f)
                    quadraticBezierTo(cx, h * .475f, cx - w * .212f, h * .355f)
                    close()
                },
                trim.copy(alpha = .92f)
            )
            for (i in 0..4) {
                drawCircle(if (i % 2 == 0) Color(0xFFB8462F) else Color(0xFF23514D), w * .016f, Offset(cx - w * .10f + i * w * .05f, h * .500f))
            }
            repeat(7) { i ->
                drawRect(
                    Color(0xFFF0DFB8).copy(alpha = .55f),
                    Offset(cx - w * .19f + i * w * .058f, h * .865f),
                    Size(w * .030f, h * .040f)
                )
            }
        }

        "northland-mantle" -> {
            drawPath(
                Path().apply {
                    moveTo(cx - w * .225f, h * .330f)
                    cubicTo(cx - w * .190f, h * .230f, cx + w * .190f, h * .230f, cx + w * .225f, h * .330f)
                    lineTo(cx + w * .195f, h * .850f)
                    quadraticBezierTo(cx, h * .905f, cx - w * .195f, h * .850f)
                    close()
                },
                garment.darken(.68f)
            )
            listOf(-1f, 1f).forEach { side ->
                drawOval(
                    Brush.verticalGradient(listOf(Color(0xFFEFE7D4), Color(0xFFA79C81))),
                    Offset(cx + side * w * .095f - w * .105f, h * .228f),
                    Size(w * .21f, h * .12f)
                )
            }
            drawCircle(Color(0xFFCBD3DC), w * .023f, Offset(cx, h * .365f))
            drawLine(Color(0xFFCBD3DC), Offset(cx - w * .055f, h * .395f), Offset(cx + w * .055f, h * .395f), w * .008f)
        }

        "cloud-daoist" -> {
            drawPath(
                Path().apply {
                    moveTo(cx - w * .082f, h * .248f); lineTo(cx, h * .315f); lineTo(cx + w * .082f, h * .248f)
                    lineTo(cx + w * .082f, h * .292f); lineTo(cx, h * .362f); lineTo(cx - w * .082f, h * .292f); close()
                },
                Color(0xFF33202A)
            )
            drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFD9C58B))), w * .048f, Offset(cx, h * .460f))
            drawCircle(Color(0xFF211129), w * .017f, Offset(cx - w * .012f, h * .450f))
            drawCircle(Color(0xFFF4EEE5), w * .015f, Offset(cx + w * .013f, h * .472f))
            listOf(.615f to -.05f, .700f to .04f).forEach { (y, dx) ->
                drawOval(trim.copy(alpha = .78f), Offset(cx + w * dx - w * .055f, h * y), Size(w * .110f, h * .026f))
                drawOval(garment.darken(.72f), Offset(cx + w * dx - w * .030f, h * (y + .008f)), Size(w * .060f, h * .016f))
            }
        }

        "street-jacket" -> {
            drawRect(Color(0xFFEDE4D4), Offset(cx - w * .085f, h * .250f), Size(w * .170f, h * .400f))
            val jacket = Path().apply {
                moveTo(cx - w * .130f, h * .250f)
                lineTo(cx + w * .130f, h * .250f)
                lineTo(cx + w * .165f, h * .600f)
                lineTo(cx - w * .165f, h * .600f)
                close()
            }
            drawPath(jacket, Brush.verticalGradient(listOf(garment.darken(.92f), garment.darken(.66f)), startY = h * .25f, endY = h * .60f))
            drawLine(trim, Offset(cx, h * .260f), Offset(cx, h * .590f), w * .010f)
            for (i in 0..2) drawCircle(trim, w * .010f, Offset(cx + if (i % 2 == 0) w * .045f else -w * .045f, h * (.310f + i * .075f)))
            drawRoundRect(garment.darken(.48f), Offset(cx - w * .135f, h * .615f), Size(w * .115f, h * .280f), CornerRadius(w * .03f))
            drawRoundRect(garment.darken(.48f), Offset(cx + w * .020f, h * .615f), Size(w * .115f, h * .280f), CornerRadius(w * .03f))
        }

        "desert-traveler" -> {
            drawPath(
                Path().apply {
                    moveTo(cx - w * .215f, h * .305f)
                    cubicTo(cx - w * .155f, h * .232f, cx + w * .155f, h * .232f, cx + w * .215f, h * .305f)
                    cubicTo(cx + w * .175f, h * .560f, cx + w * .145f, h * .720f, cx + w * .170f, h * .880f)
                    quadraticBezierTo(cx, h * .935f, cx - w * .170f, h * .880f)
                    cubicTo(cx - w * .145f, h * .720f, cx - w * .175f, h * .560f, cx - w * .215f, h * .305f)
                    close()
                },
                garment.darken(.76f)
            )
            drawPath(
                Path().apply {
                    moveTo(cx - w * .160f, h * .265f); quadraticBezierTo(cx, h * .370f, cx + w * .165f, h * .255f)
                    lineTo(cx + w * .120f, h * .435f); quadraticBezierTo(cx, h * .535f, cx - w * .125f, h * .430f); close()
                },
                trim.copy(alpha = .94f)
            )
            drawCircle(Brush.radialGradient(listOf(Color(0xFFF6EBCB), Color(0xFF9A7442))), w * .035f, Offset(cx - w * .070f, h * .520f))
            drawCircle(Color(0xFF734A25), w * .012f, Offset(cx + w * .080f, h * .535f))
        }

        "festival-costume" -> {
            drawPath(
                Path().apply {
                    moveTo(cx - w * .205f, h * .290f)
                    quadraticBezierTo(cx, h * .205f, cx + w * .205f, h * .290f)
                    quadraticBezierTo(cx, h * .360f, cx - w * .205f, h * .290f)
                    close()
                },
                trim
            )
            listOf(-1f, 0f, 1f).forEach { i ->
                drawCircle(Color(0xFFF6E7BE), w * .032f, Offset(cx + i * w * .125f, h * (.245f + kotlin.math.abs(i) * .012f)))
                drawCircle(Color(0xFFB8462F), w * .014f, Offset(cx + i * w * .125f, h * (.245f + kotlin.math.abs(i) * .012f)))
            }
            drawRoundRect(garment.darken(.62f), Offset(cx - w * .140f, h * .500f), Size(w * .280f, h * .038f), CornerRadius(w * .012f))
            listOf(-1f, 1f).forEach { side ->
                drawRoundRect(
                    trim.copy(alpha = .86f),
                    Offset(cx + side * w * .148f - w * .038f, h * .525f),
                    Size(w * .086f, h * .060f),
                    CornerRadius(w * .024f)
                )
            }
            repeat(5) { i -> drawCircle(Color(0xFFF6E7BE), w * .009f, Offset(cx - w * .11f + i * w * .055f, h * .845f)) }
        }
    }
}

private fun DrawScope.drawRobe(
    w: Float,
    h: Float,
    cx: Float,
    garment: Color,
    trim: Color
) {
    val robe = Path().apply {
        moveTo(cx - w * 0.094f, h * 0.252f)
        cubicTo(
            cx - w * 0.150f, h * 0.320f,
            cx - w * 0.195f, h * 0.580f,
            cx - w * 0.215f, h * 0.890f
        )
        quadraticBezierTo(cx, h * 0.930f, cx + w * 0.215f, h * 0.890f)
        cubicTo(
            cx + w * 0.195f, h * 0.580f,
            cx + w * 0.150f, h * 0.320f,
            cx + w * 0.094f, h * 0.252f
        )
        quadraticBezierTo(cx, h * 0.290f, cx - w * 0.105f, h * 0.245f)
        close()
    }
    drawPath(
        robe,
        Brush.verticalGradient(
            listOf(garment, garment.darken(0.72f)),
            startY = h * 0.25f,
            endY = h * 0.93f
        )
    )

    val fold = garment.darken(0.60f).copy(alpha = 0.35f)
    drawPath(
        Path().apply {
            moveTo(cx - w * 0.052f, h * 0.292f)
            quadraticBezierTo(cx - w * 0.076f, h * 0.590f, cx - w * 0.062f, h * 0.885f)
        },
        fold,
        style = Stroke(w * 0.010f)
    )
    drawPath(
        Path().apply {
            moveTo(cx + w * 0.052f, h * 0.292f)
            quadraticBezierTo(cx + w * 0.076f, h * 0.590f, cx + w * 0.062f, h * 0.885f)
        },
        fold,
        style = Stroke(w * 0.010f)
    )

    drawPath(
        Path().apply {
            moveTo(cx - w * 0.068f, h * 0.250f)
            lineTo(cx, h * 0.345f)
            lineTo(cx + w * 0.068f, h * 0.250f)
        },
        trim,
        style = Stroke(w * 0.014f)
    )

    drawLine(
        trim.copy(alpha = 0.45f),
        Offset(cx, h * 0.348f),
        Offset(cx, h * 0.470f),
        w * 0.007f
    )

    drawRoundRect(
        trim,
        topLeft = Offset(cx - w * 0.120f, h * 0.474f),
        size = Size(w * 0.240f, h * 0.022f),
        cornerRadius = CornerRadius(w * 0.014f, w * 0.014f)
    )
    drawCircle(Color(0xFFE8E2D4), w * 0.022f, Offset(cx, h * 0.522f))
    drawCircle(
        Color(0xFFB8AE96),
        w * 0.022f,
        Offset(cx, h * 0.522f),
        style = Stroke(w * 0.005f)
    )

    drawArc(
        trim.copy(alpha = 0.75f),
        195f,
        150f,
        false,
        style = Stroke(w * 0.012f),
        topLeft = Offset(cx - w * 0.220f, h * 0.865f),
        size = Size(w * 0.440f, h * 0.050f)
    )

    val sleeve = garment.darken(0.82f)
    drawPath(
        Path().apply {
            moveTo(cx - w * 0.094f, h * 0.262f)
            cubicTo(
                cx - w * 0.148f, h * 0.306f,
                cx - w * 0.184f, h * 0.440f,
                cx - w * 0.172f, h * 0.565f
            )
            quadraticBezierTo(cx - w * 0.148f, h * 0.590f, cx - w * 0.110f, h * 0.550f)
            cubicTo(
                cx - w * 0.116f, h * 0.440f,
                cx - w * 0.104f, h * 0.320f,
                cx - w * 0.094f, h * 0.262f
            )
            close()
        },
        sleeve
    )
    drawPath(
        Path().apply {
            moveTo(cx + w * 0.094f, h * 0.262f)
            cubicTo(
                cx + w * 0.148f, h * 0.306f,
                cx + w * 0.184f, h * 0.440f,
                cx + w * 0.172f, h * 0.565f
            )
            quadraticBezierTo(cx + w * 0.148f, h * 0.590f, cx + w * 0.110f, h * 0.550f)
            cubicTo(
                cx + w * 0.116f, h * 0.440f,
                cx + w * 0.104f, h * 0.320f,
                cx + w * 0.094f, h * 0.262f
            )
            close()
        },
        sleeve
    )
    drawArc(
        trim.copy(alpha = 0.55f),
        100f,
        80f,
        false,
        style = Stroke(w * 0.008f),
        topLeft = Offset(cx - w * 0.200f, h * 0.500f),
        size = Size(w * 0.085f, h * 0.060f)
    )
    drawArc(
        trim.copy(alpha = 0.55f),
        0f,
        80f,
        false,
        style = Stroke(w * 0.008f),
        topLeft = Offset(cx + w * 0.115f, h * 0.500f),
        size = Size(w * 0.085f, h * 0.060f)
    )
}

private fun DrawScope.drawHair(
    half: Boolean,
    cx: Float,
    headL: Float,
    headT: Float,
    headW: Float,
    headH: Float,
    w: Float,
    h: Float,
    hair: Color,
    trim: Color
) {
    val headR = headL + headW
    if (half) {
        val hairCap = Path().apply {
            moveTo(headL - w * 0.012f, headT + headH * 0.48f)
            cubicTo(
                headL - w * 0.038f,
                headT - headH * 0.08f,
                cx - headW * 0.30f,
                headT - headH * 0.26f,
                cx + headW * 0.02f,
                headT - headH * 0.24f
            )
            cubicTo(
                cx + headW * 0.30f,
                headT - headH * 0.22f,
                headR + w * 0.030f,
                headT - headH * 0.02f,
                headR + w * 0.008f,
                headT + headH * 0.44f
            )
            quadraticBezierTo(cx + headW * 0.02f, headT + headH * 0.16f, headL - w * 0.012f, headT + headH * 0.48f)
            close()
        }
        drawPath(
            hairCap,
            Brush.verticalGradient(
                listOf(Color(0xFF2B1830), Color(0xFF0C0510)),
                startY = headT - headH * .20f,
                endY = headT + headH * .70f
            )
        )

        drawPath(
            Path().apply {
                moveTo(cx - headW * 0.10f, headT - headH * 0.16f)
                quadraticBezierTo(
                    cx - headW * 0.24f,
                    headT + headH * 0.14f,
                    cx - headW * 0.18f,
                    headT + headH * 0.66f
                )
            },
            Color(0xFF180A20).copy(alpha = 0.72f),
            style = Stroke(w * 0.0085f, cap = StrokeCap.Round)
        )
        drawPath(
            Path().apply {
                moveTo(cx - headW * 0.02f, headT - headH * 0.23f)
                quadraticBezierTo(
                    cx + headW * 0.22f,
                    headT + headH * 0.02f,
                    cx + headW * 0.30f,
                    headT + headH * 0.52f
                )
            },
            Color(0xFF4A2B50).copy(alpha = 0.50f),
            style = Stroke(w * 0.0042f, cap = StrokeCap.Round)
        )

        listOf(-1f to 0.68f, 1f to 0.50f).forEach { (side, length) ->
            drawPath(
                Path().apply {
                    moveTo(cx + side * headW * 0.46f, headT + headH * 0.22f)
                    quadraticBezierTo(
                        cx + side * headW * 0.60f,
                        headT + headH * (0.18f + 0.18f * length),
                        cx + side * headW * 0.44f,
                        headT + headH * (0.22f + 0.62f * length)
                    )
                },
                Color(0xFF180A20),
                style = Stroke(w * 0.0072f, cap = StrokeCap.Round)
            )
        }

        drawPath(
            Path().apply {
                moveTo(cx - headW * 0.20f, headT + headH * 0.03f)
                cubicTo(
                    cx - headW * 0.54f,
                    headT - headH * 0.28f,
                    cx - headW * 0.49f,
                    headT - headH * 0.56f,
                    cx - headW * 0.25f,
                    headT - headH * 0.70f
                )
                cubicTo(
                    cx - headW * 0.23f,
                    headT - headH * 0.35f,
                    cx - headW * 0.09f,
                    headT - headH * 0.09f,
                    cx + headW * 0.10f,
                    headT + headH * 0.10f
                )
                close()
            },
            Brush.linearGradient(
                listOf(Color(0xFF100612), Color(0xFF7A2A38)),
                start = Offset(cx - headW * 0.20f, headT + headH * 0.08f),
                end = Offset(cx - headW * 0.27f, headT - headH * 0.66f)
            )
        )

        drawPath(
            Path().apply {
                moveTo(cx + headW * 0.17f, headT + headH * 0.05f)
                cubicTo(
                    cx + headW * 0.43f,
                    headT - headH * 0.15f,
                    cx + headW * 0.47f,
                    headT - headH * 0.31f,
                    cx + headW * 0.37f,
                    headT - headH * 0.41f
                )
                cubicTo(
                    cx + headW * 0.27f,
                    headT - headH * 0.21f,
                    cx + headW * 0.13f,
                    headT - headH * 0.01f,
                    cx + headW * 0.06f,
                    headT + headH * 0.12f
                )
                close()
            },
            Brush.linearGradient(
                listOf(Color(0xFF120818), Color(0xFF682430)),
                start = Offset(cx + headW * 0.16f, headT + headH * 0.08f),
                end = Offset(cx + headW * 0.38f, headT - headH * 0.38f)
            )
        )

        drawArc(
            Color(0xFFB05566).copy(alpha = .28f),
            0f,
            120f,
            false,
            style = Stroke(w * .0024f, cap = StrokeCap.Round),
            topLeft = Offset(cx - headW * .33f, headT - headH * .45f),
            size = Size(headW * .08f, headH * .18f)
        )
        drawArc(
            Color(0xFF9C4758).copy(alpha = .22f),
            0f,
            105f,
            false,
            style = Stroke(w * .0021f, cap = StrokeCap.Round),
            topLeft = Offset(cx + headW * .28f, headT - headH * .29f),
            size = Size(headW * .07f, headH * .15f)
        )

    } else {
        val cap = Path().apply {
            moveTo(headL - w * 0.006f, headT + headH * 0.48f)
            cubicTo(
                headL - w * 0.034f,
                headT + headH * 0.02f,
                cx - headW * 0.33f,
                headT - headH * 0.19f,
                cx,
                headT - headH * 0.17f
            )
            cubicTo(
                cx + headW * 0.33f,
                headT - headH * 0.19f,
                headR + w * 0.034f,
                headT + headH * 0.02f,
                headR + w * 0.006f,
                headT + headH * 0.48f
            )
            quadraticBezierTo(cx, headT + headH * 0.26f, headL - w * 0.006f, headT + headH * 0.48f)
            close()
        }
        drawPath(cap, Brush.verticalGradient(listOf(Color.White, hair), startY = headT - headH * .1f, endY = headT + headH))
        listOf(-0.24f, -0.06f, 0.13f, 0.28f).forEach { dx ->
            drawPath(
                Path().apply {
                    moveTo(cx + headW * dx, headT - headH * 0.12f)
                    quadraticBezierTo(
                        cx + headW * (dx + if (dx < 0f) -0.09f else 0.08f),
                        headT + headH * 0.24f,
                        cx + headW * (dx + if (dx < 0f) -0.03f else 0.04f),
                        headT + headH * 0.50f
                    )
                },
                Color(0xFFD8CFBF).copy(alpha = 0.52f),
                style = Stroke(w * 0.0045f, cap = StrokeCap.Round)
            )
        }
        drawRoundRect(
            hair,
            topLeft = Offset(headL - w * 0.010f, headT + headH * 0.28f),
            size = Size(w * 0.032f, headH * 0.42f),
            cornerRadius = CornerRadius(w * 0.016f, w * 0.016f)
        )
        drawRoundRect(
            hair,
            topLeft = Offset(headR - w * 0.022f, headT + headH * 0.28f),
            size = Size(w * 0.032f, headH * 0.42f),
            cornerRadius = CornerRadius(w * 0.016f, w * 0.016f)
        )

        val beardTop = headT + headH * 0.70f
        drawRoundRect(
            hair,
            topLeft = Offset(cx - w * 0.053f, beardTop),
            size = Size(w * 0.106f, headH * 0.22f),
            cornerRadius = CornerRadius(w * 0.026f, w * 0.026f)
        )
        drawOval(
            Brush.verticalGradient(
                listOf(hair, Color(0xFFD8CFBF)),
                startY = beardTop,
                endY = headT + headH * 1.78f
            ),
            topLeft = Offset(cx - w * 0.077f, headT + headH * 0.80f),
            size = Size(w * 0.154f, headH * 1.02f)
        )
        drawArc(
            Color(0xFFD8CFBF),
            205f,
            130f,
            false,
            style = Stroke(w * 0.006f, cap = StrokeCap.Round),
            topLeft = Offset(cx - w * 0.048f, headT + headH * 0.82f),
            size = Size(w * 0.096f, headH * 0.44f)
        )
    }
}

private fun DrawScope.drawFace(
    half: Boolean,
    cx: Float,
    w: Float,
    h: Float,
    ink: Color,
    blush: Color,
    blink: Float = 0f,
    gazeX: Float = 0f,
    moodLevel: Float = 0f,
    microSmile: Float = 0f,
    attentionGaze: Float = 0f,
    headTop: Float = h * 0.072f,
    headWidth: Float = w * 0.184f,
    headHeight: Float = h * 0.128f
) {
    val eyeDx = headWidth * (if (half) 0.225f else 0.215f)
    val eyeRx = if (half) w * 0.016f else w * 0.014f
    val eyeRy = if (half) h * 0.0058f else h * 0.0075f
    val eyeY = headTop + headHeight * 0.56f
    val browY = headTop + headHeight * 0.42f
    val browHalf = headWidth * 0.115f
    val gazeShift = (gazeX + attentionGaze).coerceIn(-1f, 1f) * eyeRx * 0.45f
    val sadWeight = (-moodLevel).coerceIn(0f, 1f)

    if (half) {
        for (side in listOf(-1, 1)) {
            val bx = cx + side * eyeDx
            val innerX = bx - side * browHalf
            val outerX = bx + side * browHalf
            val browLift = if (side == 1) h * 0.016f else -h * 0.009f
            drawPath(
                Path().apply {
                    moveTo(innerX, browY + h * 0.057f)
                    quadraticBezierTo(
                        bx,
                        browY - h * 0.002f + browLift,
                        outerX,
                        browY - h * 0.048f + browLift
                    )
                },
                Color(0xFF12040E),
                style = Stroke(w * 0.0081f, cap = StrokeCap.Round)
            )
        }
        listOf(-1f to 0.20f, 1f to 0.30f).forEach { (side, alpha) ->
            drawPath(
                Path().apply {
                    val sx = cx + side * headWidth * (if (side < 0) 0.08f else 0.12f)
                    val sy = headTop + headHeight * 0.48f
                    moveTo(sx, sy)
                    quadraticBezierTo(
                        cx + side * headWidth * 0.24f,
                        sy + headHeight * 0.10f,
                        cx + side * headWidth * 0.38f,
                        sy + headHeight * 0.03f
                    )
                },
                Color(0xFF3A0B1F).copy(alpha = alpha),
                style = Stroke(w * 0.0055f, cap = StrokeCap.Round)
            )
        }
    } else {
        for (side in listOf(-1, 1)) {
            val bx = cx + side * eyeDx
            val innerLift = h * 0.004f * sadWeight
            drawPath(
                Path().apply {
                    moveTo(bx - side * browHalf, browY + h * 0.003f)
                    quadraticBezierTo(
                        bx,
                        browY - h * 0.006f - h * 0.005f * sadWeight,
                        bx + side * browHalf,
                        browY + h * 0.001f - innerLift
                    )
                },
                Color(0xFFF0EAE0),
                style = Stroke(w * 0.0072f, cap = StrokeCap.Round)
            )
        }
        drawArc(
            Color(0xFFD9CCBB).copy(alpha = 0.65f),
            185f,
            170f,
            false,
            style = Stroke(w * 0.0022f, cap = StrokeCap.Round),
            topLeft = Offset(cx - w * 0.033f, headTop - h * 0.012f),
            size = Size(w * 0.066f, h * 0.020f)
        )
    }

    for (side in listOf(-1, 1)) {
        val ex = cx + side * eyeDx
        val lidDroop = if (!half) 0.16f * sadWeight else 0f
        val closedAmount = maxOf(blink.coerceIn(0f, 1f), lidDroop)
        withTransform({
            scale(1f, (1f - closedAmount).coerceAtLeast(0.05f), pivot = Offset(ex, eyeY - eyeRy))
        }) {
        if (half) {
            drawPath(
                Path().apply {
                    val innerX = ex - side * eyeRx * 1.06f
                    val outerX = ex + side * eyeRx * 1.16f
                    moveTo(innerX, eyeY + eyeRy * 0.48f)
                    quadraticBezierTo(ex, eyeY - eyeRy * 0.86f, outerX, eyeY - eyeRy * 0.30f)
                    quadraticBezierTo(ex, eyeY + eyeRy * 0.72f, innerX, eyeY + eyeRy * 0.48f)
                    close()
                },
                Color(0xFFFBEDEE)
            )
        } else {
            drawOval(
                Color(0xFFFEFCF8),
                topLeft = Offset(ex - eyeRx, eyeY - eyeRy),
                size = Size(eyeRx * 2, eyeRy * 2)
            )
        }
        val irisColor = if (half) Color(0xFFA81E33) else Color(0xFF68452F)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(irisColor, if (half) Color(0xFF2B030D) else Color(0xFF180B15)),
                center = Offset(ex + gazeShift, eyeY),
                radius = eyeRx * 0.72f
            ),
            radius = eyeRx * 0.72f,
            center = Offset(ex + gazeShift, eyeY)
        )
        if (half) {
            drawCircle(
                Color(0xFF070106),
                eyeRx * 0.11f,
                Offset(ex + gazeShift, eyeY)
            )
            drawLine(
                Color(0xFF070106),
                Offset(ex + gazeShift, eyeY - eyeRy * 1.05f),
                Offset(ex + gazeShift, eyeY + eyeRy * 1.05f),
                w * 0.0018f,
                cap = StrokeCap.Round
            )
        } else {
            drawCircle(
                Color(0xFF100109),
                eyeRx * 0.32f,
                Offset(ex + gazeShift, eyeY)
            )
        }
        drawCircle(
            Color.White.copy(alpha = if (half) 0.54f else 0.90f),
            eyeRx * (if (half) 0.08f else 0.22f),
            Offset(ex + gazeShift - eyeRx * 0.31f, eyeY - eyeRy * 0.38f)
        )

        if (half) {
            drawLine(
                Color(0xFF12040A),
                Offset(ex + gazeShift, eyeY - eyeRy * 0.72f),
                Offset(ex + gazeShift, eyeY + eyeRy * 0.74f),
                w * 0.0026f,
                cap = StrokeCap.Round
            )
            drawArc(
                Color(0xFF1C0510),
                188f,
                152f,
                false,
                style = Stroke(w * 0.0083f, cap = StrokeCap.Round),
                topLeft = Offset(ex - eyeRx, eyeY - eyeRy),
                size = Size(eyeRx * 2, eyeRy * 2)
            )
            drawArc(
                Color(0xFF47152A).copy(alpha = 0.32f),
                20f,
                105f,
                false,
                style = Stroke(w * 0.0022f, cap = StrokeCap.Round),
                topLeft = Offset(ex - eyeRx, eyeY - eyeRy),
                size = Size(eyeRx * 2, eyeRy * 2)
            )
            drawLine(
                Color(0xFF12040A),
                Offset(ex + side * eyeRx * 0.92f, eyeY - eyeRy * 0.42f),
                Offset(ex + side * eyeRx * 1.34f, eyeY - eyeRy * 1.05f),
                w * 0.0028f,
                cap = StrokeCap.Round
            )
        } else {
            drawArc(
                Color(0xFF4C3325).copy(alpha = 0.62f),
                180f,
                180f,
                false,
                style = Stroke(w * 0.0034f, cap = StrokeCap.Round),
                topLeft = Offset(ex - eyeRx, eyeY - eyeRy),
                size = Size(eyeRx * 2, eyeRy * 2)
            )
            drawArc(
                Color(0xFFD9CCBB).copy(alpha = 0.45f),
                25f,
                130f,
                false,
                style = Stroke(w * 0.0018f, cap = StrokeCap.Round),
                topLeft = Offset(ex - eyeRx * 1.7f, eyeY + eyeRy * 1.4f),
                size = Size(eyeRx * 3.4f, eyeRy * 2.8f)
            )
        }
        }
        if (closedAmount > 0.55f) {
            drawLine(
                if (half) Color(0xFF12040A) else Color(0xFF4C3325).copy(alpha = 0.70f),
                Offset(ex - eyeRx * 0.95f, eyeY - eyeRy),
                Offset(ex + eyeRx * 0.95f, eyeY - eyeRy),
                w * (if (half) 0.0052f else 0.0034f),
                cap = StrokeCap.Round
            )
        }
    }

    val noseColor = if (half) Color(0xFFD3A9A9) else Color(0xFFEBD5C0)
    drawLine(
        noseColor,
        Offset(cx - w * 0.0025f, eyeY + h * 0.008f),
        Offset(cx + (if (half) w * 0.0045f else 0f), eyeY + h * 0.018f),
        w * 0.0028f,
        cap = StrokeCap.Round
    )

    if (!half) {
        val blushAlpha = 0.22f * (0.72f + 0.14f * (moodLevel + 1f))
        drawOval(
            brush = Brush.radialGradient(listOf(blush.copy(alpha = blushAlpha), Color.Transparent)),
            topLeft = Offset(cx - eyeDx - eyeRx - w * 0.016f, eyeY + h * 0.011f),
            size = Size(w * 0.036f, h * 0.014f)
        )
        drawOval(
            brush = Brush.radialGradient(listOf(blush.copy(alpha = blushAlpha), Color.Transparent)),
            topLeft = Offset(cx + eyeDx + eyeRx - w * 0.020f, eyeY + h * 0.011f),
            size = Size(w * 0.036f, h * 0.014f)
        )
    } else {
        listOf(-1f to 0.16f, 1f to 0.27f).forEach { (side, alpha) ->
            drawPath(
                Path().apply {
                    val sx = cx + side * headWidth * 0.34f
                    val sy = headTop + headHeight * 0.70f
                    moveTo(sx, sy)
                    cubicTo(
                        cx + side * headWidth * 0.28f,
                        sy + headHeight * 0.16f,
                        cx + side * headWidth * 0.16f,
                        sy + headHeight * 0.27f,
                        cx + side * headWidth * 0.06f,
                        sy + headHeight * 0.31f
                    )
                },
                Color(0xFF42101F).copy(alpha = alpha),
                style = Stroke(w * 0.0031f, cap = StrokeCap.Round)
            )
        }
        drawOval(
            brush = Brush.radialGradient(listOf(Color(0xFF33081C).copy(alpha = 0.10f), Color.Transparent)),
            topLeft = Offset(cx - eyeDx - eyeRx - w * 0.014f, eyeY + h * 0.010f),
            size = Size(w * 0.034f, h * 0.013f)
        )
        drawOval(
            brush = Brush.radialGradient(listOf(Color(0xFF33081C).copy(alpha = 0.24f), Color.Transparent)),
            topLeft = Offset(cx + eyeDx + eyeRx - w * 0.018f, eyeY + h * 0.009f),
            size = Size(w * 0.038f, h * 0.015f)
        )
        listOf(-1f to 0.13f, 1f to 0.20f).forEach { (side, alpha) ->
            drawPath(
                Path().apply {
                    val sx = cx + side * (eyeDx + headWidth * 0.14f)
                    val sy = eyeY + h * 0.009f
                    moveTo(sx, sy)
                    quadraticBezierTo(
                        cx + side * headWidth * 0.30f,
                        sy + h * 0.018f,
                        cx + side * headWidth * 0.36f,
                        sy + h * 0.041f
                    )
                },
                Color(0xFF3A0A20).copy(alpha = alpha),
                style = Stroke(w * 0.0027f, cap = StrokeCap.Round)
            )
        }
    }

    if (half) {
        drawCircle(
            Color(0xFF43101F).copy(alpha = 0.66f),
            w * 0.0028f,
            Offset(cx + headWidth * 0.30f, eyeY + h * 0.018f)
        )
        listOf(-1f to 0.18f, 1f to 0.28f).forEach { (side, alpha) ->
            drawPath(
                Path().apply {
                    val sx = cx + side * headWidth * 0.13f
                    val sy = eyeY + headHeight * 0.26f
                    moveTo(sx, sy)
                    quadraticBezierTo(
                        cx + side * headWidth * 0.27f,
                        sy + headHeight * 0.07f,
                        cx + side * headWidth * 0.42f,
                        sy + headHeight * 0.01f
                    )
                },
                Color(0xFF42101F).copy(alpha = alpha),
                style = Stroke(w * 0.0075f, cap = StrokeCap.Round)
            )
        }
    }

    val mouthY = headTop + headHeight * 0.82f
    if (half) {
        val smirk = (0.86f + 0.14f * moodLevel + 0.07f * microSmile).coerceIn(0.64f, 1.08f)
        drawPath(
            Path().apply {
                moveTo(cx - w * 0.027f, mouthY + h * 0.005f)
                cubicTo(
                    cx - w * 0.004f,
                    mouthY + h * 0.0045f * smirk,
                    cx + w * 0.010f,
                    mouthY - h * 0.0015f * smirk,
                    cx + w * 0.033f,
                    mouthY - h * 0.015f * smirk
                )
                quadraticBezierTo(cx + w * 0.001f, mouthY + h * 0.004f, cx - w * 0.027f, mouthY + h * 0.005f)
                close()
            },
            Color(0xFF3B0717).copy(alpha = 0.92f)
        )
        drawLine(
            Color(0xFF18030B).copy(alpha = 0.82f),
            Offset(cx - w * 0.027f, mouthY + h * 0.005f),
            Offset(cx + w * 0.033f, mouthY - h * 0.015f * smirk),
            w * 0.0034f,
            cap = StrokeCap.Round
        )
        drawPath(
            Path().apply {
                moveTo(cx + w * 0.010f, mouthY - h * 0.0008f * smirk)
                lineTo(cx + w * 0.016f, mouthY - h * 0.0002f * smirk)
                lineTo(cx + w * 0.012f, mouthY + h * 0.007f * smirk)
                close()
            },
            Color(0xFFF7EFE4)
        )
    } else {
        val smileCurve = h * (0.008f + 0.010f * moodLevel + 0.004f * microSmile)
        drawPath(
            Path().apply {
                moveTo(cx - w * 0.014f, mouthY - h * 0.001f)
                quadraticBezierTo(cx, mouthY + smileCurve, cx + w * 0.014f, mouthY - h * 0.002f)
            },
            Color(0xFF744238).copy(alpha = 0.86f),
            style = Stroke(w * 0.0048f, cap = StrokeCap.Round)
        )
    }
}

private fun headTopHint(height: Float): Float = height * 0.072f

private fun DrawScope.drawSkinAccessory(
    skinId: String,
    half: Boolean,
    cx: Float,
    width: Float,
    height: Float,
    trim: Color,
    hair: Color,
    headL: Float,
    headTop: Float,
    headWidth: Float,
    headHeight: Float
) {
    val w = width
    val h = height
    // All accessories are anchored to the head bounding box so they scale with it.
    val hw = headWidth
    val hh = headHeight
    val ht = headTop

    // The halo marks the benevolent form itself; cultural headwear sits in front of it.
    if (!half) {
        drawOval(
            Color(0xFFF4E6BC).copy(alpha = .18f),
            topLeft = Offset(cx - hw * .82f, ht - hh * .42f),
            size = Size(hw * 1.64f, hh * .58f)
        )
        drawOval(
            Color(0xFFF7D98A),
            topLeft = Offset(cx - hw * .68f, ht - hh * .32f),
            size = Size(hw * 1.36f, hh * .44f),
            style = Stroke(w * .011f)
        )
    }

    when (skinId) {
        "jiangnan-robe" -> {
            drawRoundRect(
                Color(0xFF211E27),
                Offset(cx - hw * .56f, ht - hh * .14f),
                Size(hw * 1.12f, hh * .30f),
                CornerRadius(hw * .10f)
            )
            drawRoundRect(
                Color(0xFF342F3B),
                Offset(cx - hw * .60f, ht + hh * .14f),
                Size(hw * 1.20f, hh * .16f),
                CornerRadius(hw * .06f)
            )
            listOf(-1f, 1f).forEach { side ->
                drawRoundRect(
                    Color(0xFF181520),
                    Offset(cx + side * hw * .54f - hw * .07f, ht + hh * .04f),
                    Size(hw * .14f, hh * .26f),
                    CornerRadius(hw * .05f)
                )
            }
            drawLine(
                trim,
                Offset(cx + hw * .06f, ht - hh * .13f),
                Offset(cx + hw * .28f, ht + hh * .18f),
                w * .006f,
                cap = StrokeCap.Round
            )
        }

        "academy-gown" -> {
            drawArc(
                Color(0xFF171A24),
                180f,
                180f,
                true,
                topLeft = Offset(cx - hw * .62f, ht + hh * .02f),
                size = Size(hw * 1.24f, hh * .36f)
            )
            drawPath(
                Path().apply {
                    moveTo(cx - hw * .98f, ht + hh * .06f)
                    lineTo(cx, ht - hh * .30f)
                    lineTo(cx + hw * .98f, ht + hh * .06f)
                    lineTo(cx, ht + hh * .42f)
                    close()
                },
                Color(0xFF101320)
            )
            drawPath(
                Path().apply {
                    moveTo(cx - hw * .98f, ht + hh * .06f)
                    lineTo(cx, ht + hh * .16f)
                    lineTo(cx + hw * .98f, ht + hh * .06f)
                    lineTo(cx, ht - hh * .04f)
                    close()
                },
                Color(0xFF2C3247).copy(alpha = .90f)
            )
            drawLine(
                Color(0xFFE2C275),
                Offset(cx + hw * .88f, ht + hh * .08f),
                Offset(cx + hw * .92f, ht + hh * .48f),
                w * .006f,
                cap = StrokeCap.Round
            )
            drawCircle(Color(0xFFE2C275), hw * .08f, Offset(cx + hw * .92f, ht + hh * .54f))
        }

        "silkroad-robe" -> {
            drawArc(
                Color(0xFF48948F),
                190f,
                160f,
                true,
                topLeft = Offset(cx - hw * .90f, ht - hh * .28f),
                size = Size(hw * 1.80f, hh * .80f)
            )
            drawArc(
                Color(0xFFE7BE68).copy(alpha = .92f),
                205f,
                130f,
                false,
                style = Stroke(w * .016f),
                topLeft = Offset(cx - hw * .84f, ht - hh * .20f),
                size = Size(hw * 1.68f, hh * .68f)
            )
            drawArc(
                Color(0xFF2D6D67).copy(alpha = .88f),
                225f,
                90f,
                false,
                style = Stroke(w * .011f),
                topLeft = Offset(cx - hw * .78f, ht - hh * .12f),
                size = Size(hw * 1.56f, hh * .56f)
            )
            drawCircle(Color(0xFFE7BE68), hw * .12f, Offset(cx + hw * .78f, ht + hh * .10f))
            drawPath(
                Path().apply {
                    moveTo(cx + hw * .76f, ht + hh * .12f)
                    cubicTo(cx + hw * 1.20f, ht + hh * .32f, cx + hw * 1.12f, ht + hh * .76f, cx + hw * 1.28f, ht + hh * 1.36f)
                    lineTo(cx + hw * 1.02f, ht + hh * 1.42f)
                    quadraticBezierTo(cx + hw * .89f, ht + hh * .82f, cx + hw * .59f, ht + hh * .26f)
                    close()
                },
                Color(0xFF48948F).copy(alpha = .90f)
            )
        }

        "northland-mantle" -> {
            drawArc(
                Color(0xFF55663F),
                178f,
                184f,
                false,
                style = Stroke(w * .052f),
                topLeft = Offset(cx - hw * 1.12f, ht - hh * .36f),
                size = Size(hw * 2.24f, hh * 1.28f)
            )
            repeat(12) { i ->
                val angle = Math.toRadians(182.0 + i * 14.6)
                drawCircle(
                    Color(0xFFEFE7D4),
                    hw * (.11f - (i % 2) * .016f),
                    Offset(
                        cx + (Math.cos(angle) * hw * 1.06f).toFloat(),
                        ht + hh * .10f + (Math.sin(angle) * hh * .62f).toFloat()
                    )
                )
            }
            drawLine(
                Color(0xFFCBD3DC),
                Offset(cx - hw * .30f, ht + hh * .32f),
                Offset(cx + hw * .30f, ht + hh * .32f),
                w * .010f,
                cap = StrokeCap.Round
            )
        }

        "cloud-daoist" -> {
            drawOval(
                Color(0xFF1F1420),
                topLeft = Offset(cx - hw * .32f, ht - hh * .22f),
                size = Size(hw * .64f, hh * .36f)
            )
            drawRoundRect(
                Color(0xFF33202A),
                Offset(cx - hw * .28f, ht - hh * .18f),
                Size(hw * .56f, hh * .30f),
                CornerRadius(w * .012f)
            )
            drawRect(
                Color(0xFFF4EEE5),
                Offset(cx - hw * .28f, ht + hh * .02f),
                Size(hw * .56f, hh * .08f)
            )
            drawCircle(Color(0xFFD9C58B), hw * .07f, Offset(cx, ht - hh * .24f))
            drawLine(
                Color(0xFFE8E2D4),
                Offset(cx - hw * .46f, ht + hh * .06f),
                Offset(cx + hw * .46f, ht - hh * .02f),
                w * .005f,
                cap = StrokeCap.Round
            )
        }

        "street-jacket" -> {
            drawArc(
                Color(0xFF39303F),
                180f,
                180f,
                true,
                topLeft = Offset(cx - hw * .70f, ht - hh * .22f),
                size = Size(hw * 1.40f, hh * .58f)
            )
            drawRoundRect(
                Color(0xFF241E2A),
                Offset(cx - hw * .80f, ht + hh * .30f),
                Size(hw * 1.60f, hh * .16f),
                CornerRadius(w * .010f)
            )
            drawLine(trim, Offset(cx, ht - hh * .20f), Offset(cx, ht + hh * .28f), w * .006f)
            drawCircle(Color(0xFFCBA96C), hw * .09f, Offset(cx - hw * .64f, ht + hh * .38f))
            drawLine(
                Color(0xFFCBA96C).copy(alpha = .75f),
                Offset(cx - hw * .64f, ht + hh * .44f),
                Offset(cx - hw * .72f, ht + hh * .66f),
                w * .003f
            )
        }

        "desert-traveler" -> {
            drawPath(
                Path().apply {
                    moveTo(cx - hw * 1.16f, ht + hh * 1.36f)
                    cubicTo(cx - hw * 1.32f, ht + hh * .38f, cx - hw * .80f, ht - hh * .32f, cx, ht - hh * .30f)
                    cubicTo(cx + hw * .80f, ht - hh * .32f, cx + hw * 1.32f, ht + hh * .38f, cx + hw * 1.16f, ht + hh * 1.36f)
                    lineTo(cx + hw * .84f, ht + hh * 1.36f)
                    quadraticBezierTo(cx + hw * .96f, ht + hh * .42f, cx, ht + hh * .06f)
                    quadraticBezierTo(cx - hw * .96f, ht + hh * .42f, cx - hw * .84f, ht + hh * 1.36f)
                    close()
                },
                Color(0xFFE4C57C).copy(alpha = .94f)
            )
            drawPath(
                Path().apply {
                    moveTo(cx + hw * 1.08f, ht + hh * .20f)
                    cubicTo(cx + hw * 1.38f, ht + hh * .50f, cx + hw * 1.30f, ht + hh * 1.10f, cx + hw * 1.44f, ht + hh * 1.62f)
                    lineTo(cx + hw * 1.12f, ht + hh * 1.70f)
                    quadraticBezierTo(cx + hw * 1.04f, ht + hh * .92f, cx + hw * .78f, ht + hh * .34f)
                    close()
                },
                Color(0xFFC79A54).copy(alpha = .95f)
            )
            drawArc(
                Color(0xFF8C5F31),
                200f,
                140f,
                false,
                style = Stroke(w * .014f),
                topLeft = Offset(cx - hw * .86f, ht - hh * .22f),
                size = Size(hw * 1.72f, hh * .68f)
            )
            drawCircle(Color(0xFF9A7442), hw * .10f, Offset(cx, ht + hh * .28f))
        }

        "festival-costume" -> {
            listOf(-1f, 1f).forEach { side ->
                drawPath(
                    Path().apply {
                        val rootX = cx + side * hw * .32f
                        moveTo(rootX, ht + hh * .02f)
                        cubicTo(
                            cx + side * hw * .90f, ht - hh * .38f,
                            cx + side * hw * 1.36f, ht - hh * .56f,
                            cx + side * hw * 1.62f, ht - hh * .40f
                        )
                        lineTo(cx + side * hw * 1.46f, ht - hh * .18f)
                        quadraticBezierTo(cx + side * hw * .90f, ht - hh * .10f, rootX + side * hw * .11f, ht + hh * .18f)
                        close()
                    },
                    Color(0xFF365F62)
                )
            }
            drawRoundRect(
                Color(0xFFA05F63),
                Offset(cx - hw * .68f, ht + hh * .02f),
                Size(hw * 1.36f, hh * .28f),
                CornerRadius(w * .014f)
            )
            drawRect(
                Color(0xFFD9A05B),
                Offset(cx - hw * .68f, ht + hh * .12f),
                Size(hw * 1.36f, hh * .06f)
            )
            listOf(-1f, 0f, 1f).forEach { i ->
                val px = cx + i * hw * .44f
                val py = ht - hh * (.16f + kotlin.math.abs(i) * .04f)
                drawLine(
                    Color(0xFF365F62),
                    Offset(px, py + hh * .14f),
                    Offset(px, py + hh * .28f),
                    w * .005f
                )
                drawCircle(Color(0xFFF6E7BE), hw * .13f, Offset(px, py))
                drawCircle(Color(0xFFB8462F), hw * .055f, Offset(px, py))
            }
        }
    }
}

private fun DrawScope.drawRotatedHat(
    cx: Float,
    width: Float,
    height: Float,
    hatColor: Color,
    trim: Color
) {
    drawArc(
        hatColor,
        188f,
        168f,
        true,
        topLeft = Offset(cx - width * .190f, height * .050f),
        size = Size(width * .380f, height * .120f)
    )
    drawArc(
        trim,
        188f,
        168f,
        false,
        style = Stroke(width * .010f),
        topLeft = Offset(cx - width * .190f, height * .050f),
        size = Size(width * .380f, height * .120f)
    )
    drawCircle(Color(0xFFE3A579), width * .022f, Offset(cx + width * .135f, height * .095f))
}

private fun DrawScope.drawFan(
    x: Float,
    y: Float,
    width: Float,
    trim: Color
) {
    drawArc(
        Color(0xFFF0E5D3).copy(alpha = .92f),
        -70f,
        145f,
        true,
        topLeft = Offset(x - width * .085f, y - width * .075f),
        size = Size(width * .170f, width * .150f)
    )
    drawArc(
        trim,
        -70f,
        145f,
        false,
        style = Stroke(width * .008f),
        topLeft = Offset(x - width * .085f, y - width * .075f),
        size = Size(width * .170f, width * .150f)
    )
}

private fun DrawScope.StageBackdrop(gold: Color, moodLevel: Float) {
    val ink = Color(0xFF0D0817)
    val w = size.width
    val h = size.height

    drawRect(
        Brush.verticalGradient(
            listOf(ink, Color(0xFF1A1029), Color(0xFF2A1B33), Color(0xFF140D1E)),
            startY = 0f,
            endY = h
        )
    )

    repeat(54) { index ->
        val x = ((index * 137.508f) % 100f) / 100f * w
        val y = ((index * 71.303f) % 62f) / 100f * h
        val radius = if (index % 7 == 0) w * .0035f else w * .0020f
        drawCircle(
            Color.White.copy(alpha = if (index % 5 == 0) .30f else .14f),
            radius,
            Offset(x, y)
        )
    }

    drawCircle(
        brush = Brush.radialGradient(
            listOf(gold.copy(alpha = .055f + .04f * (moodLevel + 1f)), Color.Transparent),
            center = Offset(w * .5f, h * .28f),
            radius = w * .48f
        ),
        radius = w * .48f,
        center = Offset(w * .5f, h * .28f)
    )

    val horizonY = h * 0.76f
    val vpX = w * 0.5f
    val floorAlpha = 0.045f
    for (i in -7..7) {
        val bottomX = w * (0.5f + i * 0.16f)
        drawLine(
            Color.White.copy(alpha = floorAlpha),
            Offset(bottomX, h),
            Offset(vpX, horizonY),
            1.2f
        )
    }
    for (i in 1..9) {
        val t = i / 9f
        val y = horizonY + (h - horizonY) * t * t
        drawLine(
            Color.White.copy(alpha = floorAlpha * 0.85f),
            Offset(0f, y),
            Offset(w, y),
            1f
        )
    }

    drawRect(
        Brush.verticalGradient(
            listOf(Color.Transparent, gold.copy(alpha = .06f), Color.Transparent),
            startY = horizonY - h * .03f,
            endY = horizonY + h * .04f
        ),
        topLeft = Offset(0f, horizonY - h * .03f),
        size = Size(w, h * .07f)
    )

    drawPath(
        Path().apply {
            moveTo(-w * .05f, h * .80f)
            quadraticBezierTo(w * .24f, h * .66f, w * .51f, h * .78f)
            quadraticBezierTo(w * .77f, h * .67f, w * 1.05f, h * .81f)
            lineTo(w * 1.05f, h * 1.01f)
            lineTo(-w * .05f, h * 1.01f)
            close()
        },
        Color(0xFF1B1228).copy(alpha = .68f)
    )
}
