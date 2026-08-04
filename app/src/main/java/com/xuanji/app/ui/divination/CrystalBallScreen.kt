package com.xuanji.app.ui.divination

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.divination.CrystalBall
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlin.math.cos
import kotlin.math.sin

/**
 * 水晶球占卜：绘制一颗水晶球，点击后内部云雾/光芒"凝视"数秒，
 * 随后浮现当日象征与解读。再点可重新凝视（换一次象征）。
 */
@Composable
fun CrystalBallScreen() {
    var gazing by remember { mutableStateOf(false) }
    var askCount by remember { mutableIntStateOf(0) }

    // 凝视中的旋涡/呼吸动画
    val infinite = rememberInfiniteTransition(label = "crystal")
    val swirl by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "swirl"
    )
    val glow by infinite.animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )

    LaunchedEffect(gazing) {
        if (gazing) {
            delay(2600)   // 凝视数秒
            askCount++    // 出结果（并作为下一次重新凝视的种子偏移）
            gazing = false
        }
    }

    val result = if (askCount > 0) CrystalBall.gaze(LocalDate.now(), askCount - 1) else null

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("水晶球占卜", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "凝视水晶球，观想心中浮现的象征（确定性选取，重凝可换象）。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SystemExplanation("crystalball")

        FortuneCard {
            SectionTitle(if (gazing) "凝视中…" else "凝视水晶球")
            Spacer(Modifier.height(16.dp))
            // 水晶球
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentAlignment = Alignment.Center
            ) {
                CrystalBallArt(gazing = gazing, swirl = swirl, glow = glow)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { if (!gazing) gazing = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(when {
                    gazing -> "凝心凝视…"
                    askCount == 0 -> "开始凝视"
                    else -> "再次凝视（换一象）"
                })
            }
            Spacer(Modifier.height(4.dp))
            Text(
                if (gazing) "保持心境澄明，观想水晶球内浮现的画面…"
                else "点击「凝视」，数秒后水晶球会浮现你今日所见之象征。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (result != null && !gazing) {
            FortuneCard {
                SectionTitle("今日所见象征 · ${result.symbol}")
                Spacer(Modifier.height(8.dp))
                Text(result.meaning, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Text(result.verdict, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "凝视水晶球时，你心中所浮现的第一个念头往往最为重要。此象征提示你当前阶段的能量倾向，请结合现实情况灵活领悟。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** 用 Canvas 绘制水晶球：球体、高光、内部旋涡与辉光。 */
@Composable
private fun CrystalBallArt(gazing: Boolean, swirl: Float, glow: Float) {
    Canvas(
        Modifier
            .size(220.dp)
            .graphicsLayer {
                alpha = if (gazing) 0.85f + 0.15f * swirl else 1f
            }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension * 0.42f

        // 底座辉光
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF7C5CFF).copy(alpha = glow * 0.35f), Color.Transparent),
                center = Offset(cx, cy)
            ),
            radius = r * 1.5f
        )

        // 球体（半透明渐变）
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    Color(0xFFB69CFF).copy(alpha = 0.55f),
                    Color(0xFF2A1F4A).copy(alpha = 0.85f),
                    Color(0xFF1A1230)
                ),
                center = Offset(cx - r * 0.3f, cy - r * 0.3f)
            ),
            radius = r
        )

        // 内部旋涡（凝视时旋转浮现）
        val swirlAlpha = if (gazing) glow else 0.25f + 0.1f * swirl
        drawCircle(
            color = Color(0xFFE9D8A6).copy(alpha = swirlAlpha * 0.5f),
            radius = r * (0.15f + 0.5f * swirl),
            center = Offset(cx, cy),
            style = Stroke(width = 2.5f)
        )
        val n = 3
        for (i in 0 until n) {
            val angle = swirl * (Math.PI * 2) + i * (Math.PI * 2 / n)
            val sx = cx + cos(angle).toFloat() * r * 0.5f
            val sy = cy + sin(angle).toFloat() * r * 0.5f
            drawCircle(
                color = Color(0xFF8FE3C2).copy(alpha = swirlAlpha * 0.6f),
                radius = r * (0.12f + 0.08f * ((swirl + i.toFloat() / n) % 1f)),
                center = Offset(sx, sy)
            )
        }

        // 高光
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.7f), Color.White.copy(alpha = 0f)),
                center = Offset(cx - r * 0.38f, cy - r * 0.42f)
            ),
            radius = r * 0.35f
        )

        // 球体描边
        drawCircle(
            color = Color(0xFFE9D8A6).copy(alpha = 0.6f),
            radius = r,
            style = Stroke(width = 2f)
        )
    }
}
