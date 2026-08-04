package com.xuanji.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 面相 / 手相的简笔示意图：用 Canvas 勾勒五官分区与掌纹，
 * 并用 Compose Text 在对应位置叠加标注（避免 raw-canvas 文字缩放错位）。
 * 支持传入「当前选中特征」，高亮对应区域。
 */

/** 面相分区示意图（正脸：额头/眼睛/鼻子/嘴巴/下巴）。highlight 为当前选中特征名，用于高亮对应区域。 */
@Composable
fun FaceSchematic(labelColor: Color, highlight: String? = null) {
    val stroke = labelColor
    val hi = Color(0xFFE9D8A6)   // 高亮金
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        val w = maxWidth.value          // dp
        val h = maxHeight.value
        val cx = w / 2f

        // 各个区域标签的定位（占位的分数，便于与下方 Canvas 对齐）
        val labels = listOf(
            "额头" to Offset(cx, h * 0.13f),
            "眼睛" to Offset(cx - w * 0.16f, h * 0.36f),
            "鼻子" to Offset(cx + w * 0.17f, h * 0.52f),
            "嘴巴" to Offset(cx - w * 0.15f, h * 0.70f),
            "下巴" to Offset(cx, h * 0.88f)
        )

        // 绘制脸部 + 标注
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            val cxv = size.width / 2f
            val topv = size.height * 0.03f
            val hv = size.height * 0.94f

            // 脸型（椭圆）
            drawOval(
                color = stroke.copy(alpha = 0.8f),
                topLeft = Offset(cxv - size.width * 0.24f, topv),
                size = Size(size.width * 0.48f, hv),
                style = Stroke(width = 2f)
            )
            // 发际线
            drawArc(
                color = stroke.copy(alpha = 0.5f), startAngle = 180f, sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cxv - size.width * 0.22f, topv + hv * 0.05f),
                size = Size(size.width * 0.44f, hv * 0.26f),
                style = Stroke(width = 1.5f)
            )
            // 眉毛
            drawLine(stroke, Offset(cxv - size.width * 0.15f, topv + hv * 0.30f), Offset(cxv - size.width * 0.04f, topv + hv * 0.28f), 2.5f)
            drawLine(stroke, Offset(cxv + size.width * 0.04f, topv + hv * 0.28f), Offset(cxv + size.width * 0.15f, topv + hv * 0.30f), 2.5f)
            // 眼睛
            drawOval(stroke, Offset(cxv - size.width * 0.15f, topv + hv * 0.34f), Size(size.width * 0.11f, hv * 0.05f), style = Stroke(2f))
            drawOval(stroke, Offset(cxv + size.width * 0.04f, topv + hv * 0.34f), Size(size.width * 0.11f, hv * 0.05f), style = Stroke(2f))
            // 鼻子
            val nosePath = Path().apply {
                moveTo(cxv, topv + hv * 0.42f)
                lineTo(cxv - size.width * 0.05f, topv + hv * 0.55f)
                lineTo(cxv + size.width * 0.05f, topv + hv * 0.55f)
            }
            drawPath(nosePath, stroke, style = Stroke(2f))
            // 嘴巴
            drawArc(stroke, startAngle = 180f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(cxv - size.width * 0.08f, topv + hv * 0.66f),
                size = Size(size.width * 0.16f, hv * 0.06f), style = Stroke(2f))
            // 下巴
            drawArc(stroke.copy(alpha = 0.6f), startAngle = 0f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(cxv - size.width * 0.10f, topv + hv * 0.72f),
                size = Size(size.width * 0.20f, hv * 0.18f), style = Stroke(1.5f))
        }

        // 叠加区域标注
        labels.forEach { (name, pos) ->
            val isHi = name == highlight
            Text(
                name,
                color = if (isHi) hi else stroke.copy(alpha = 0.9f),
                fontSize = 15.sp,
                modifier = Modifier.offset(x = pos.x.dp, y = pos.y.dp).align(Alignment.TopStart)
            )
        }
    }
}

/** 手掌示意图（四大主线 + 形状）。highlight 为当前选中特征名。 */
@Composable
fun PalmSchematic(labelColor: Color, highlight: String? = null) {
    val stroke = labelColor
    val hi = Color(0xFFE9D8A6)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(320.dp)
    ) {
        val w = maxWidth.value
        val h = maxHeight.value
        val cxm = w * 0.44f

        val labels = listOf(
            "感情线" to Offset(cxm + w * 0.12f, h * 0.22f),
            "智慧线" to Offset(cxm + w * 0.16f, h * 0.42f),
            "生命线" to Offset(cxm - w * 0.30f, h * 0.42f),
            "命运线" to Offset(cxm + w * 0.24f, h * 0.62f),
            "掌形" to Offset(cxm, h * 0.82f)
        )

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {
            val cx = size.width * 0.44f
            val cy = size.height * 0.5f

            // 掌心椭圆（掌面轮廓）
            drawOval(stroke.copy(alpha = 0.8f),
                Offset(cx - size.width * 0.22f, cy - size.height * 0.18f),
                Size(size.width * 0.44f, size.height * 0.36f), style = Stroke(2f))
            // 四指
            val fingerW = size.width * 0.07f
            val fingerGap = size.width * 0.012f
            val baseY = cy - size.height * 0.16f
            for (i in 0 until 4) {
                val fx = cx - size.width * 0.15f + i * (fingerW + fingerGap)
                drawRoundRect(stroke.copy(alpha = 0.7f),
                    topLeft = Offset(fx, baseY - size.height * 0.26f),
                    size = Size(fingerW, size.height * 0.28f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(fingerW / 2f, fingerW / 2f),
                    style = Stroke(1.8f))
            }
            // 拇指
            drawRoundRect(stroke.copy(alpha = 0.7f),
                topLeft = Offset(cx - size.width * 0.30f, cy - size.height * 0.10f),
                size = Size(size.width * 0.09f, size.height * 0.20f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                style = Stroke(1.8f))

            // 感情线
            drawArc(stroke.copy(alpha = 0.85f), 200f, 130f, false,
                Offset(cx - size.width * 0.18f, cy - size.height * 0.16f),
                Size(size.width * 0.38f, size.height * 0.18f), style = Stroke(2.2f))
            // 智慧线
            drawArc(stroke.copy(alpha = 0.85f), 210f, 120f, false,
                Offset(cx - size.width * 0.20f, cy - size.height * 0.04f),
                Size(size.width * 0.40f, size.height * 0.16f), style = Stroke(2.2f))
            // 生命线
            drawArc(stroke.copy(alpha = 0.85f), 60f, 130f, false,
                Offset(cx - size.width * 0.26f, cy - size.height * 0.04f),
                Size(size.width * 0.34f, size.height * 0.30f), style = Stroke(2.2f))
            // 命运线
            drawLine(stroke.copy(alpha = 0.7f),
                Offset(cx + size.width * 0.02f, cy - size.height * 0.16f),
                Offset(cx + size.width * 0.02f, cy + size.height * 0.18f), 2f)
        }

        labels.forEach { (name, pos) ->
            val isHi = name == highlight
            Text(
                name,
                color = if (isHi) hi else stroke.copy(alpha = 0.9f),
                fontSize = 13.sp,
                modifier = Modifier.offset(x = pos.x.dp, y = pos.y.dp).align(Alignment.TopStart)
            )
        }
    }
}
