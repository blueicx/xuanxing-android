package com.xuanji.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * 启动动画页：
 * 1) 两枚五芒星（正立 + 倒置）交叉旋转闪烁（"五芒星交叉闪耀"）；
 * 2) 随即"玄星"二字淡入显现；
 * 3) 停留片刻后回调 onDone 进入主界面。
 */
@Composable
fun StarSplashScreen(onDone: () -> Unit) {
    // 星空色：主星用冰蓝，副星用星紫，标题保留淡金高光
    val starBlue = Color(0xFF9FD8F0)     // 淡星空蓝
    val starViolet = Color(0xFFB7A8E8)   // 淡星紫
    val goldSoft = Color(0xFFC9A227)

    // 五芒星闪耀：旋转角 + 缩放 + 透明度 无限循环
    val infinite = rememberInfiniteTransition(label = "starTwinkle")
    val rotation by infinite.animateFloat(
        initialValue = -18f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )
    val scale by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val twinkle by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle"
    )

    // 标题淡入 + 渐显
    var showTitle by remember { mutableStateOf(false) }
    val titleAlpha by animateFloatAsState(
        targetValue = if (showTitle) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "titleAlpha"
    )

    // 分阶段：标题先单独淡出（宇宙色渐变：白 → 星空蓝紫 → 透明），再整体淡出
    var phase by remember { mutableStateOf(0) }
    val titleFadeOut by animateFloatAsState(
        targetValue = if (phase >= 1) 0f else 1f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "titleFadeOut"
    )
    // 宇宙色渐变：淡出过程中标题颜色从暖白过渡到星空蓝/紫，再随 alpha 一起消失
    val titleColor by androidx.compose.animation.animateColorAsState(
        targetValue = when {
            phase >= 2 -> starViolet.copy(alpha = 0f)
            phase == 1 -> Color(0xFF7FB8E8)   // 过渡到星空蓝
            else -> Color(0xFFF2ECFF)          // 初始暖白
        },
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "titleColor"
    )
    val subColor by androidx.compose.animation.animateColorAsState(
        targetValue = when {
            phase >= 2 -> starBlue.copy(alpha = 0f)
            phase == 1 -> starViolet           // 副标过渡到星紫
            else -> goldSoft
        },
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "subColor"
    )
    LaunchedEffect(Unit) {
        delay(900)          // 五芒星先闪耀
        showTitle = true    // 显现"玄星"
        delay(1800)
        phase = 1           // 1)「玄星」二字先渐变淡出
        delay(800)
        phase = 2           // 2) 再整体淡出
        delay(700)
        onDone()
    }
    val containerAlpha by animateFloatAsState(
        targetValue = if (phase == 2) 0f else 1f,
        animationSpec = tween(500),
        label = "containerAlpha"
    )

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = containerAlpha },
        contentAlignment = Alignment.Center
    ) {
        // 两枚交叉五芒星
        Box(
            Modifier
                .size(220.dp)
                .graphicsLayer {
                    rotationZ = rotation
                    scaleX = scale
                    scaleY = scale
                },
            contentAlignment = Alignment.Center
        ) {
            PentagramCanvas(
                modifier = Modifier.size(220.dp),
                color = starViolet,
                alpha = twinkle * 0.7f,
                inverted = false,
                strokeWidth = 6f
            )
            PentagramCanvas(
                modifier = Modifier.size(150.dp),
                color = starBlue,
                alpha = twinkle,
                inverted = true,
                strokeWidth = 6f
            )
        }

        // 玄星 标题 + 副标（末尾"玄星"二字先单独渐变淡出：颜色从暖白渐变到星空蓝紫）
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "玄星",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor.copy(alpha = (titleAlpha * titleFadeOut).coerceIn(0f, 1f)),
                modifier = Modifier.graphicsLayer { alpha = 1f }
            )
            Text(
                text = "星象 · 五行 · 占卜",
                fontSize = 15.sp,
                letterSpacing = 6.sp,
                color = subColor.copy(alpha = (titleAlpha * titleFadeOut * 0.9f).coerceIn(0f, 1f)),
                modifier = Modifier
                    .graphicsLayer { alpha = 1f }
            )
        }

        // 底部署名（作者）
        Text(
            text = "作者：吴家希 · WJX",
            fontSize = 12.sp,
            color = Color(0xFFF2ECFF).copy(alpha = titleAlpha * 0.6f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .graphicsLayer { alpha = titleAlpha }
        )
    }
}

/** 用 Path 绘制一枚五芒星（描边，支持正立/倒置与透明度）。 */
@Composable
private fun PentagramCanvas(
    modifier: Modifier,
    color: Color,
    alpha: Float,
    inverted: Boolean,
    strokeWidth: Float
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerR = size.minDimension * 0.48f
        val innerR = outerR * 0.382f
        val start = if (inverted) 0.5 else -0.5   // 倒置则从下方角起
        val path = Path()
        var first = true
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) outerR else innerR
            val angle = Math.PI / 2 + start * (Math.PI / 5) + i * (Math.PI / 5)
            val x = cx + r * cos(angle).toFloat()
            val y = cy + r * sin(angle).toFloat()
            if (first) {
                path.moveTo(x, y)
                first = false
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        drawPath(
            path = path,
            color = color.copy(alpha = alpha),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}
