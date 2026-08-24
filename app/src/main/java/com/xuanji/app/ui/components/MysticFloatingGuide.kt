package com.xuanji.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    val mode = if (guideAvailable) {
        remember(companionKey) { MysticGuideGenerator.suggestedMode("composite", fortune!!) }
    } else ""
    val skin = if (guideAvailable) {
        remember(companionKey) { MysticGuideGenerator.defaultMysticSkin(mode, fortune!!) }
    } else null
    var detailOpen by rememberSaveable(companionKey) { mutableStateOf(false) }
    val pageScroll = rememberScrollState()
    val detailBazi = bazi
    val detailFortune = fortune

    Box(modifier.fillMaxSize()) {
        content(pageScroll)

        if (skin != null) {
            MysticOrb(
                roleName = if (mode == "half") "半仙" else "玄学家",
                skinLabel = skin.label,
                skinId = skin.id,
                color = Color(skin.garment),
                backColor = Color(skin.back),
                trimColor = Color(skin.trim),
                onClick = { detailOpen = true },
                scrollValue = pageScroll.value,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }

        if (detailOpen && detailBazi != null && detailFortune != null && skin != null) {
            Dialog(
                onDismissRequest = { detailOpen = false },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                MysticImmersiveStage(
                    half = mode == "half",
                    skinId = skin.id,
                    garment = Color(skin.garment),
                    backColor = Color(skin.back),
                    trimColor = Color(skin.trim),
                    roleName = if (mode == "half") "半仙" else "玄学家",
                    onClose = { detailOpen = false }
                ) {
                    MysticGuideCard(detailBazi!!, detailFortune!!, immersive = true)
                }
            }
        }
    }
}

@Composable
private fun MysticOrb(
    roleName: String,
    skinLabel: String,
    skinId: String,
    color: Color,
    backColor: Color,
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
                .size(60.dp, 74.dp)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            MysticFigure(
                half = roleName == "半仙",
                skinId = skinId,
                garment = color,
                trim = trimColor
            )
        }
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Text(
                "$roleName · $skinLabel · 对话",
                Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun MysticFigure(
    half: Boolean,
    skinId: String,
    garment: Color,
    trim: Color
) {
    Canvas(Modifier.fillMaxSize()) {
        drawMysticFigure(half, skinId, garment, trim)
    }
}

private fun DrawScope.drawMysticFigure(
    half: Boolean,
    skinId: String,
    garment: Color,
    trim: Color
) {
    val ink = Color(0xFF2C2137)
    val skinTone = Color(0xFFF6EDE2)
    val width = size.width
    val height = size.height

    drawRoundRect(
        color = garment,
        topLeft = Offset(width * .16f, height * .52f),
        size = Size(width * .68f, height * .42f),
        cornerRadius = CornerRadius(width * .20f, width * .20f)
    )
    drawRoundRect(
        color = ink,
        topLeft = Offset(width * .24f, height * .08f),
        size = Size(width * .52f, height * .13f),
        cornerRadius = CornerRadius(width * .08f, width * .08f)
    )
    drawOval(
        color = skinTone,
        topLeft = Offset(width * .27f, height * .11f),
        size = Size(width * .46f, height * .43f)
    )

    if (half) {
        drawCircle(ink, radius = width * .055f, center = Offset(width * .50f, height * .06f))
    }

    val eyeY = height * .30f
    drawCircle(ink, radius = width * .025f, center = Offset(width * .40f, eyeY))
    drawCircle(ink, radius = width * .025f, center = Offset(width * .60f, eyeY))
    drawArc(
        color = ink,
        startAngle = if (half) 25f else 205f,
        sweepAngle = 130f,
        useCenter = false,
        style = Stroke(width * .025f),
        topLeft = Offset(width * .42f, height * .31f),
        size = Size(width * .16f, height * .10f)
    )

    when (skinId) {
        "academy-gown" -> {
            drawLine(trim, Offset(width * .23f, height * .15f), Offset(width * .77f, height * .15f), width * .05f)
            drawLine(trim, Offset(width * .74f, height * .15f), Offset(width * .74f, height * .27f), width * .03f)
        }
        "silkroad-robe" -> {
            drawArc(trim, startAngle = 190f, sweepAngle = 160f, useCenter = true,
                topLeft = Offset(width * .21f, height * .04f), size = Size(width * .58f, height * .29f))
        }
        "northland-mantle" -> {
            drawArc(garment, startAngle = 180f, sweepAngle = 180f, useCenter = true,
                topLeft = Offset(width * .18f, height * .06f), size = Size(width * .64f, height * .48f))
            repeat(4) { index ->
                drawCircle(trim, radius = width * .025f, center = Offset(width * (.26f + index * .16f), height * .53f))
            }
        }
        "street-jacket" -> {
            drawLine(trim, Offset(width * .34f, height * .56f), Offset(width * .66f, height * .56f), width * .04f)
            drawCircle(trim, radius = width * .03f, center = Offset(width * .50f, height * .65f))
        }
        "desert-traveler" -> {
            drawArc(trim, startAngle = 170f, sweepAngle = 200f, useCenter = false, style = Stroke(width * .05f),
                topLeft = Offset(width * .22f, height * .05f), size = Size(width * .56f, height * .33f))
        }
        "festival-costume" -> {
            drawRoundRect(
                color = trim,
                topLeft = Offset(width * .32f, height * .02f),
                size = Size(width * .36f, height * .12f),
                cornerRadius = CornerRadius(width * .05f, width * .05f)
            )
            drawCircle(trim, radius = width * .04f, center = Offset(width * .50f, height * .01f))
        }
        else -> {
            drawLine(trim, Offset(width * .36f, height * .54f), Offset(width * .64f, height * .54f), width * .04f)
            drawCircle(trim, radius = width * .035f, center = Offset(width * .50f, height * .10f))
        }
    }

    drawLine(trim, Offset(width * .34f, height * .60f), Offset(width * .66f, height * .60f), width * .035f)
}

@Composable
private fun MysticImmersiveStage(
    half: Boolean,
    skinId: String,
    garment: Color,
    backColor: Color,
    trimColor: Color,
    roleName: String,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    val breath = rememberInfiniteTransition(label = "stageBreath")
    val breathValue by breath.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing)),
        label = "breathValue"
    )
    val breathe = 1f + Math.sin(breathValue.toDouble()).toFloat() * 0.018f
    val ink = Color(0xFF120D1C)
    val deep = Color(0xFF221733)
    val warm = Color(0xFF6A4A3B)

    Surface(Modifier.fillMaxSize(), color = ink) {
        Box {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    Brush.verticalGradient(
                        listOf(ink, deep.copy(alpha = .94f), warm.copy(alpha = .38f), ink),
                        startY = 0f,
                        endY = size.height
                    )
                )
                drawOval(
                    brush = Brush.radialGradient(listOf(trimColor.copy(alpha = .20f), Color.Transparent)),
                    topLeft = Offset(size.width * .18f, size.height * .48f),
                    size = Size(size.width * .64f, size.height * .13f)
                )
            }

            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 58.dp)
                    .size(210.dp, 240.dp)
            ) {
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = breathe
                            scaleY = breathe
                            alpha = 0.98f
                        }
                ) {
                    drawOval(
                        color = trimColor.copy(alpha = .12f),
                        topLeft = Offset(size.width * .10f, size.height * .88f),
                        size = Size(size.width * .80f, size.height * .07f)
                    )
                    drawMysticFigure(half, skinId, garment, trimColor)
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .heightIn(min = 330.dp),
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                color = Color(0xFF1B1328).copy(alpha = 0.96f),
                contentColor = Color(0xFFF4EEE5)
            ) {
                MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFD9C58B), tertiary = Color(0xFFE3A579))) {
                    Column(Modifier.fillMaxSize().padding(top = 4.dp)) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(start = 18.dp, end = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "$roleName · 现场对话",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = Color(0xFFEFE6D8)
                            )
                            IconButton(onClick = onClose) {
                                Icon(Icons.Filled.Close, contentDescription = "关闭玄师详情", tint = Color(0xFFD9C58B))
                            }
                        }
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 14.dp)
                                .navigationBarsPadding()
                        ) {
                            content()
                        }
                    }
                }
            }
        }
    }
}
