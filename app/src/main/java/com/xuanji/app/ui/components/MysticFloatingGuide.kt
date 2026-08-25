package com.xuanji.app.ui.components

import android.view.Gravity
import android.view.WindowManager
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.xuanji.app.data.model.BaziFull
import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.domain.MysticGuideGenerator
import kotlin.math.roundToInt

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
    val pageScroll = rememberScrollState()

    Box(modifier.fillMaxSize()) {
        content(pageScroll)

        if (skin != null) {
                MysticOrb(
                    roleName = if (stageMode == "half") "半仙" else "玄学家",
                    skinId = skin.id,
                color = Color(skin.garment),
                trimColor = Color(skin.trim),
                onClick = { detailOpen = true },
                scrollValue = pageScroll.value,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }

        if (detailOpen && bazi != null && fortune != null && skin != null) {
            Dialog(
                onDismissRequest = { detailOpen = false },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                MysticImmersiveStage(
                    half = stageMode == "half",
                    skinId = skin.id,
                    garment = Color(skin.garment),
                    trimColor = Color(skin.trim),
                    roleName = if (stageMode == "half") "半仙" else "玄学家",
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
private fun MysticOrb(
    roleName: String,
    skinId: String,
    color: Color,
    trimColor: Color,
    onClick: () -> Unit,
    scrollValue: Int = 0,
    modifier: Modifier = Modifier
) {
    val drift = rememberInfiniteTransition(label = "mysticDrift")
    val wave by drift.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mysticWave"
    )

    Column(
        modifier
            .offset {
                IntOffset(
                    (Math.sin(wave.toDouble()) * 3).roundToInt(),
                    (Math.sin(scrollValue / 72.0) * 5).roundToInt() +
                        (Math.cos(wave.toDouble()) * 2).roundToInt()
                )
            }
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            Modifier
                .size(62.dp, 78.dp)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawMysticFigure(roleName == "半仙", skinId, color, trimColor)
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
    SideEffect {
        (view.parent as? DialogWindowProvider)?.window?.let { win ->
            win.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
            win.attributes = win.attributes.apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
                x = 0
                y = 0
                gravity = Gravity.FILL
            }
            win.setBackgroundDrawableResource(android.R.color.transparent)
            WindowCompat.setDecorFitsSystemWindows(win, false)
            WindowCompat.getInsetsController(win, win.decorView).apply {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    Surface(Modifier.fillMaxSize(), color = ink, contentColor = Color(0xFFF4EEE5)) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize()) { StageBackdrop(gold) }

            StageFigure(
                half = half,
                skinId = skinId,
                garment = garment,
                trimColor = trimColor,
                breathValue = breathValue,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 84.dp)
                    .fillMaxWidth(0.62f)
                    .aspectRatio(0.72f)
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .navigationBarsPadding()
            ) {
                Spacer(Modifier.height(44.dp))
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
                    .padding(start = 12.dp, top = 8.dp)
            ) {
                topStartContent()
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 16.dp, top = 10.dp),
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
    val modes = listOf("scholar" to "玄学家", "half" to "半仙")
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
    modifier: Modifier = Modifier
) {
    val breathe = 1f + Math.sin(breathValue.toDouble()).toFloat() * .016f
    val sway = Math.sin(breathValue.toDouble() * .68f).toFloat() * 2.2f

    Box(modifier) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = breathe
                    scaleY = breathe
                    translationX = sway
                    alpha = .99f
                }
        ) {
            drawMysticFigure(half, skinId, garment, trimColor)
        }
    }
}

private fun Color.darken(f: Float): Color = copy(
    red = red * f,
    green = green * f,
    blue = blue * f
)

private fun DrawScope.drawMysticFigure(
    half: Boolean,
    skinId: String,
    garment: Color,
    trim: Color
) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f

    val skin = Color(0xFFF8EFE3)
    val skinShade = Color(0xFFE9D6C3)
    val hair = if (half) Color(0xFF2B1D36) else Color(0xFF221831)
    val ink = Color(0xFF1B1226)
    val blush = Color(0xFFD9938C)

    drawCircle(
        brush = Brush.radialGradient(
            listOf(trim.copy(alpha = 0.15f), Color.Transparent),
            center = Offset(cx, h * 0.16f),
            radius = w * 0.34f
        ),
        radius = w * 0.34f,
        center = Offset(cx, h * 0.16f)
    )

    drawOval(
        brush = Brush.radialGradient(
            listOf(Color.Black.copy(alpha = 0.30f), Color.Transparent)
        ),
        topLeft = Offset(cx - w * 0.24f, h * 0.895f),
        size = Size(w * 0.48f, h * 0.035f)
    )

    drawRobe(w, h, cx, garment, trim)

    drawRect(
        brush = Brush.verticalGradient(
            listOf(skinShade, skin),
            startY = h * 0.195f,
            endY = h * 0.245f
        ),
        topLeft = Offset(cx - w * 0.022f, h * 0.195f),
        size = Size(w * 0.044f, h * 0.052f)
    )

    val headL = cx - w * 0.100f
    val headT = h * 0.082f
    val headW = w * 0.200f
    val headH = h * 0.118f
    drawPath(
        Path().apply {
            moveTo(cx - headW * 0.46f, headT + headH * 0.18f)
            cubicTo(
                cx - headW * 0.52f, headT + headH * 0.02f,
                cx - headW * 0.28f, headT - headH * 0.10f,
                cx, headT - headH * 0.08f
            )
            cubicTo(
                cx + headW * 0.28f, headT - headH * 0.10f,
                cx + headW * 0.52f, headT + headH * 0.02f,
                cx + headW * 0.46f, headT + headH * 0.18f
            )
            cubicTo(
                cx + headW * 0.42f, headT + headH * 0.82f,
                cx + headW * 0.20f, headT + headH * 1.08f,
                cx, headT + headH * 1.06f
            )
            cubicTo(
                cx - headW * 0.20f, headT + headH * 1.08f,
                cx - headW * 0.42f, headT + headH * 0.82f,
                cx - headW * 0.46f, headT + headH * 0.18f
            )
            close()
        },
        Brush.verticalGradient(
            listOf(skin, skinShade),
            startY = headT,
            endY = headT + headH
        )
    )
    drawOval(
        brush = Brush.verticalGradient(
            listOf(skin, skinShade),
            startY = headT,
            endY = headT + headH
        ),
        topLeft = Offset(headL, headT),
        size = Size(headW, headH)
    )
    drawOval(
        brush = Brush.radialGradient(
            listOf(skinShade.copy(alpha = 0.35f), Color.Transparent)
        ),
        topLeft = Offset(headL + headW * 0.58f, headT + headH * 0.38f),
        size = Size(headW * 0.34f, headH * 0.48f)
    )

    drawHair(half, cx, headL, headT, headW, headH, w, h, hair, trim)
    drawFace(half, cx, w, h, ink, blush)
    drawSkinAccessory(skinId, half, cx, w, h, trim, hair)
}

private fun DrawScope.drawRobe(
    w: Float,
    h: Float,
    cx: Float,
    garment: Color,
    trim: Color
) {
    val robe = Path().apply {
        moveTo(cx - w * 0.105f, h * 0.245f)
        cubicTo(
            cx - w * 0.165f, h * 0.315f,
            cx - w * 0.215f, h * 0.580f,
            cx - w * 0.235f, h * 0.890f
        )
        quadraticBezierTo(cx, h * 0.930f, cx + w * 0.235f, h * 0.890f)
        cubicTo(
            cx + w * 0.215f, h * 0.580f,
            cx + w * 0.165f, h * 0.315f,
            cx + w * 0.105f, h * 0.245f
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
            moveTo(cx - w * 0.055f, h * 0.290f)
            quadraticBezierTo(cx - w * 0.080f, h * 0.590f, cx - w * 0.065f, h * 0.885f)
        },
        fold,
        style = Stroke(w * 0.010f)
    )
    drawPath(
        Path().apply {
            moveTo(cx + w * 0.055f, h * 0.290f)
            quadraticBezierTo(cx + w * 0.080f, h * 0.590f, cx + w * 0.065f, h * 0.885f)
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
            moveTo(cx - w * 0.105f, h * 0.255f)
            cubicTo(
                cx - w * 0.165f, h * 0.300f,
                cx - w * 0.205f, h * 0.440f,
                cx - w * 0.192f, h * 0.565f
            )
            quadraticBezierTo(cx - w * 0.148f, h * 0.590f, cx - w * 0.110f, h * 0.550f)
            cubicTo(
                cx - w * 0.125f, h * 0.440f,
                cx - w * 0.112f, h * 0.320f,
                cx - w * 0.105f, h * 0.255f
            )
            close()
        },
        sleeve
    )
    drawPath(
        Path().apply {
            moveTo(cx + w * 0.105f, h * 0.255f)
            cubicTo(
                cx + w * 0.165f, h * 0.300f,
                cx + w * 0.205f, h * 0.440f,
                cx + w * 0.192f, h * 0.565f
            )
            quadraticBezierTo(cx + w * 0.148f, h * 0.590f, cx + w * 0.110f, h * 0.550f)
            cubicTo(
                cx + w * 0.125f, h * 0.440f,
                cx + w * 0.112f, h * 0.320f,
                cx + w * 0.105f, h * 0.255f
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
    drawOval(
        hair.copy(alpha = 0.92f),
        topLeft = Offset(headL - w * 0.014f, headT - h * 0.010f),
        size = Size(headW + w * 0.028f, headH + h * 0.045f)
    )

    drawArc(
        hair,
        180f,
        180f,
        true,
        topLeft = Offset(headL - w * 0.010f, headT - h * 0.014f),
        size = Size(headW + w * 0.020f, headH * 1.05f)
    )

    val fringeY = headT + headH * 0.42f
    val fringeW = headW * 0.075f
    for (i in 0..3) {
        val fx = headL + headW * (0.14f + i * 0.24f)
        drawPath(
            Path().apply {
                moveTo(fx, fringeY - headH * 0.06f)
                lineTo(fx + fringeW * 0.5f, fringeY + headH * 0.05f)
                lineTo(fx + fringeW, fringeY - headH * 0.06f)
                close()
            },
            hair
        )
    }

    if (half) {
        drawPath(
            Path().apply {
                moveTo(headL - w * 0.005f, headT + headH * 0.10f)
                quadraticBezierTo(
                    headL - w * 0.030f, headT + headH * 0.35f,
                    headL - w * 0.012f, headT + headH * 0.58f
                )
            },
            hair,
            style = Stroke(w * 0.014f)
        )
        drawPath(
            Path().apply {
                moveTo(headL + headW + w * 0.005f, headT + headH * 0.10f)
                quadraticBezierTo(
                    headL + headW + w * 0.030f, headT + headH * 0.35f,
                    headL + headW + w * 0.012f, headT + headH * 0.58f
                )
            },
            hair,
            style = Stroke(w * 0.014f)
        )
    } else {
        drawRoundRect(
            hair,
            topLeft = Offset(headL - w * 0.008f, headT + headH * 0.18f),
            size = Size(w * 0.040f, headH * 0.52f),
            cornerRadius = CornerRadius(w * 0.020f, w * 0.020f)
        )
        drawRoundRect(
            hair,
            topLeft = Offset(headL + headW - w * 0.032f, headT + headH * 0.18f),
            size = Size(w * 0.040f, headH * 0.52f),
            cornerRadius = CornerRadius(w * 0.020f, w * 0.020f)
        )
        drawCircle(hair, w * 0.042f, Offset(cx, headT - h * 0.018f))
        drawCircle(trim.copy(alpha = 0.70f), w * 0.012f, Offset(cx, headT - h * 0.030f))
    }
}

private fun DrawScope.drawFace(
    half: Boolean,
    cx: Float,
    w: Float,
    h: Float,
    ink: Color,
    blush: Color
) {
    val eyeDx = w * 0.042f
    val eyeRx = w * 0.017f
    val eyeRy = h * 0.009f
    val eyeY = h * 0.155f

    for (side in listOf(-1, 1)) {
        val ex = cx + side * eyeDx
        drawOval(
            brush = Brush.verticalGradient(
                listOf(ink, ink.copy(alpha = 0.82f)),
                startY = eyeY - eyeRy,
                endY = eyeY + eyeRy
            ),
            topLeft = Offset(ex - eyeRx, eyeY - eyeRy),
            size = Size(eyeRx * 2, eyeRy * 2)
        )
        drawCircle(
            Color.White.copy(alpha = 0.92f),
            eyeRx * 0.36f,
            Offset(ex - eyeRx * 0.30f, eyeY - eyeRy * 0.35f)
        )
        drawCircle(
            Color.White.copy(alpha = 0.45f),
            eyeRx * 0.18f,
            Offset(ex + eyeRx * 0.28f, eyeY + eyeRy * 0.30f)
        )
    }

    val browY = h * 0.138f
    val browW = w * 0.005f
    if (half) {
        drawLine(
            ink.copy(alpha = 0.72f),
            Offset(cx - eyeDx - eyeRx, browY + h * 0.003f),
            Offset(cx - eyeDx + eyeRx, browY - h * 0.003f),
            browW
        )
        drawLine(
            ink.copy(alpha = 0.72f),
            Offset(cx + eyeDx - eyeRx, browY - h * 0.007f),
            Offset(cx + eyeDx + eyeRx, browY - h * 0.001f),
            browW
        )
    } else {
        drawLine(
            ink.copy(alpha = 0.72f),
            Offset(cx - eyeDx - eyeRx, browY - h * 0.002f),
            Offset(cx - eyeDx + eyeRx, browY),
            browW
        )
        drawLine(
            ink.copy(alpha = 0.72f),
            Offset(cx + eyeDx - eyeRx, browY),
            Offset(cx + eyeDx + eyeRx, browY - h * 0.003f),
            browW
        )
    }

    drawOval(
        blush.copy(alpha = 0.24f),
        topLeft = Offset(cx - eyeDx - eyeRx - w * 0.014f, eyeY + h * 0.012f),
        size = Size(w * 0.034f, h * 0.012f)
    )
    drawOval(
        blush.copy(alpha = 0.24f),
        topLeft = Offset(cx + eyeDx + eyeRx - w * 0.017f, eyeY + h * 0.012f),
        size = Size(w * 0.034f, h * 0.012f)
    )

    val mouthY = h * 0.181f
    if (half) {
        drawArc(
            color = ink,
            startAngle = 15f,
            sweepAngle = 130f,
            useCenter = false,
            style = Stroke(w * 0.005f),
            topLeft = Offset(cx - w * 0.016f, mouthY - h * 0.006f),
            size = Size(w * 0.032f, h * 0.013f)
        )
    } else {
        drawArc(
            color = ink,
            startAngle = 25f,
            sweepAngle = 130f,
            useCenter = false,
            style = Stroke(w * 0.004f),
            topLeft = Offset(cx - w * 0.014f, mouthY - h * 0.005f),
            size = Size(w * 0.028f, h * 0.011f)
        )
    }
}

private fun DrawScope.drawSkinAccessory(
    skinId: String,
    half: Boolean,
    cx: Float,
    width: Float,
    height: Float,
    trim: Color,
    hair: Color
) {
    when {
        half -> {
            drawRotatedHat(cx, width, height, Color(0xFF37283F), trim)
            drawFan(cx + width * .225f, height * .555f, width, trim)
        }
        skinId == "academy-gown" -> {
            drawRoundRect(
                hair,
                Offset(cx - width * .175f, height * .075f),
                Size(width * .350f, height * .035f),
                CornerRadius(width * .02f, width * .02f)
            )
            drawRect(
                trim,
                Offset(cx + width * .148f, height * .105f),
                Size(width * .018f, height * .048f)
            )
        }
        skinId == "silkroad-robe" -> {
            drawArc(
                trim.copy(alpha = .94f),
                startAngle = 190f,
                sweepAngle = 160f,
                useCenter = true,
                topLeft = Offset(cx - width * .185f, height * .050f),
                size = Size(width * .370f, height * .100f)
            )
        }
        skinId == "northland-mantle" -> {
            drawArc(
                Color(0xFF64764B),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(cx - width * .200f, height * .065f),
                size = Size(width * .400f, height * .140f)
            )
        }
        skinId == "cloud-daoist" -> {
            drawCircle(Color(0xFFE8E2D4), width * .055f, Offset(cx, height * .075f))
            drawRect(
                hair,
                Offset(cx - width * .012f, height * .075f),
                Size(width * .024f, height * .042f)
            )
        }
        skinId == "street-jacket" -> {
            drawRoundRect(
                Color(0xFF39303F),
                Offset(cx - width * .140f, height * .085f),
                Size(width * .280f, height * .026f),
                CornerRadius(width * .03f, width * .03f)
            )
        }
        skinId == "desert-traveler" -> {
            drawArc(
                trim,
                195f,
                150f,
                false,
                style = Stroke(width * .035f),
                topLeft = Offset(cx - width * .160f, height * .065f),
                size = Size(width * .320f, height * .090f)
            )
        }
        skinId == "festival-costume" -> {
            drawRoundRect(
                trim,
                Offset(cx - width * .105f, height * .070f),
                Size(width * .210f, height * .040f),
                CornerRadius(width * .02f, width * .01f)
            )
        }
        else -> {
            drawCircle(trim, width * .030f, Offset(cx, height * .080f))
            drawRect(
                hair,
                Offset(cx - width * .007f, height * .080f),
                Size(width * .014f, height * .032f)
            )
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

private fun DrawScope.StageBackdrop(gold: Color) {
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
            listOf(gold.copy(alpha = .09f), Color.Transparent),
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
