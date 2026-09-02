package com.xuanji.app.ui.components

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Shared reduced-motion probe for both the compact orb and the full stage.
 * Keeping the probe in this small file makes motion policy easy to audit.
 */
@Composable
internal fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
}

@Composable
internal fun MysticOrb(
    roleName: String,
    half: Boolean,
    color: Color,
    trimColor: Color,
    onClick: () -> Unit,
    scrollValue: Int = 0,
    modifier: Modifier = Modifier
) {
    val reducedMotion = rememberReducedMotion()
    val drift = rememberInfiniteTransition(label = "mysticDrift")
    val animatedWave by drift.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (half) 3500 else 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mysticWave"
    )
    val wave = if (reducedMotion) 0f else animatedWave

    Column(
        modifier
            .offset {
                IntOffset(
                    (Math.sin(wave.toDouble()) * 3).roundToInt(),
                    (Math.sin(scrollValue / 72.0) * 5).roundToInt() +
                        (Math.cos(wave.toDouble()) * if (half) 3f else 2f).roundToInt()
                )
            }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clickable(onClick = onClick)
                .semantics {
                    contentDescription = "${roleName}微光浮球，点击召回玄师舞台"
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val breathe = 1f + sin(wave.toDouble()).toFloat() * .008f
                        scaleX = breathe
                        scaleY = breathe
                    }
            ) {
                drawMysticOrb(color = color, trimColor = trimColor, phase = wave)
            }
        }
    }
}

private fun DrawScope.drawMysticOrb(
    color: Color,
    trimColor: Color,
    phase: Float
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = minOf(size.width, size.height) * .34f
    val pulse = (sin(phase.toDouble()).toFloat() + 1f) * .04f
    drawCircle(color.copy(alpha = .12f), radius = radius * (1.45f + pulse), center = center)
    drawCircle(color.copy(alpha = .26f), radius = radius * 1.13f, center = center)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFF8E7B0).copy(alpha = .96f), color.copy(alpha = .7f), trimColor.copy(alpha = .82f)),
            center = center,
            radius = radius * 1.12f
        ),
        radius = radius,
        center = center
    )
    drawCircle(trimColor.copy(alpha = .72f), radius = radius * .23f, center = center)
}
