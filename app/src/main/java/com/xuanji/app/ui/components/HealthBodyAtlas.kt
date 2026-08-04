package com.xuanji.app.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Paint

/**
 * 简化人体正面图：只把「命盘提示需留意的部位」在人体上直接标注出来，
 * 没问题的部位不画点、不写字。每个命中部位在点旁直接写中文名，
 * 词条融入部位，不再单独列 chip。
 *
 * 坐标归一化（0..1），由容器等比缩放。
 */
private data class BodyPoint(
    val x: Float, val y: Float,
    val label: String,
    val labelDx: Float = 0f, // 标签相对点的像素偏移（dx>0 靠右，<0 靠左）
    val labelDy: Float = 0f  // 标签相对点的像素偏移（dy<0 在上，>0 在下）
)

/** 五行/部位 key → 人体正面图上的坐标（0..1）与标签偏移。坐标贴合真实解剖位置。 */
private val PART_POINTS: Map<String, BodyPoint> = mapOf(
    // 头面部
    "眼" to BodyPoint(0.50f, 0.070f, "眼", labelDx = 0f, labelDy = 14f),
    // 胸腔（上焦：心肺血脉）
    "心血脉" to BodyPoint(0.47f, 0.305f, "心血脉", labelDx = 0f, labelDy = -12f),
    "肺" to BodyPoint(0.60f, 0.255f, "肺", labelDx = 20f, labelDy = -6f),
    "皮肤" to BodyPoint(0.76f, 0.350f, "皮肤", labelDx = -22f, labelDy = 0f),
    // 上腹（中焦：脾胃消化）
    "脾胃" to BodyPoint(0.50f, 0.440f, "脾胃", labelDx = 0f, labelDy = -12f),
    "消化" to BodyPoint(0.50f, 0.495f, "消化", labelDx = 0f, labelDy = 12f),
    "肌肉" to BodyPoint(0.24f, 0.400f, "肌肉", labelDx = -30f, labelDy = 0f),
    // 下腹（下焦：肝胆肾膀胱泌尿）
    "肝胆" to BodyPoint(0.56f, 0.510f, "肝胆", labelDx = 26f, labelDy = 0f),
    "小肠" to BodyPoint(0.42f, 0.525f, "小肠", labelDx = -28f, labelDy = 0f),
    "肾膀胱" to BodyPoint(0.50f, 0.585f, "肾膀胱", labelDx = 0f, labelDy = 12f),
    "泌尿" to BodyPoint(0.50f, 0.635f, "泌尿", labelDx = 0f, labelDy = 12f),
    "筋" to BodyPoint(0.43f, 0.700f, "筋", labelDx = 0f, labelDy = -10f),
    "骨" to BodyPoint(0.45f, 0.720f, "骨", labelDx = -24f, labelDy = 0f)
)

/**
 * @param parts 命盘给出的、需要在人体图上高亮的身体部位 key 列表
 *              （与 BaziCalculator.HEALTH_PARTS 一致）
 */
@Composable
fun HealthBodyAtlas(
    parts: List<String>,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val chipBg = MaterialTheme.colorScheme.surfaceVariant
    val uniqueParts = parts.distinct()
    val highlightSet = uniqueParts.toSet()

    val density = LocalDensity.current
    val labelTextSizePx = with(density) { 9.sp.toPx() }

    // 脉冲动画
    val infinite = rememberInfiniteTransition(label = "health-atlas-pulse")
    val pulse: Float by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500)),
        label = "pulse"
    )
    val pulseScale: Float by animateFloatAsState(
        targetValue = 0.4f + 0.6f * pulse,
        label = "pulseScale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 顶部文字说明（按命中数量：少=开心，多=难过）
        Text(
            when {
                uniqueParts.isEmpty() -> "五行流通，体质均衡，身体无恙～保持好心情 😊"
                uniqueParts.size <= 3 -> "命盘提示仅少数部位需留意，整体尚可 😌"
                uniqueParts.size <= 6 -> "命盘提示部分部位需注意，宜多保养 😐"
                else -> "命盘提示较多部位需留意，请务必重视调养 😣"
            },
            style = MaterialTheme.typography.bodySmall,
            color = onSurfaceVariant
        )

        // 人体图（Canvas，无命中部位也显示，脸随命中数变化）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.52f)  // 宽:高 ≈ 1:1.92
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(0.52f)) {
                val w = size.width
                val h = size.height

                val outlineColor = onSurfaceVariant.copy(alpha = 0.6f)
                val outlineStroke = 1.4f

                // —— 头部（含表情，随命中数变化）——
                val headCx = 0.50f * w
                val headCy = 0.095f * h
                val headR = 0.11f * h
                drawCircle(
                    color = outlineColor,
                    radius = headR,
                    center = Offset(headCx, headCy),
                    style = Stroke(width = outlineStroke)
                )
                // 表情：眼 + 嘴（drawArc 签名：color, startAngle, sweepAngle, useCenter, topLeft, size, style）
                val eyeY = headCy - headR * 0.1f
                val eyeDX = headR * 0.28f
                val mouthY = headCy + headR * 0.32f
                fun smileEye(centerX: Float, centerY: Float) = drawArc(
                    color = outlineColor,
                    startAngle = 200f, sweepAngle = 140f, useCenter = false,
                    topLeft = Offset(centerX - headR * 0.12f, centerY - headR * 0.16f),
                    size = Size(headR * 0.24f, headR * 0.3f),
                    style = Stroke(width = 1.4f)
                )
                fun sadEye(centerX: Float, centerY: Float) = drawArc(
                    color = outlineColor,
                    startAngle = 20f, sweepAngle = 140f, useCenter = false,
                    topLeft = Offset(centerX - headR * 0.12f, centerY),
                    size = Size(headR * 0.24f, headR * 0.3f),
                    style = Stroke(width = 1.4f)
                )
                when {
                    uniqueParts.isEmpty() -> {
                        // 开心：弯眼 ^ ^ + 微笑
                        smileEye(headCx - eyeDX, eyeY)
                        smileEye(headCx + eyeDX, eyeY)
                        drawArc(
                            color = outlineColor,
                            startAngle = 20f, sweepAngle = 140f, useCenter = false,
                            topLeft = Offset(headCx - headR * 0.28f, mouthY),
                            size = Size(headR * 0.56f, headR * 0.42f),
                            style = Stroke(width = 1.4f)
                        )
                    }
                    uniqueParts.size <= 6 -> {
                        // 平静：圆眼 + 平嘴
                        drawCircle(outlineColor, headR * 0.06f, Offset(headCx - eyeDX, eyeY), style = Stroke(width = 1.4f))
                        drawCircle(outlineColor, headR * 0.06f, Offset(headCx + eyeDX, eyeY), style = Stroke(width = 1.4f))
                        drawLine(outlineColor, Offset(headCx - headR * 0.2f, mouthY), Offset(headCx + headR * 0.2f, mouthY), strokeWidth = 1.4f)
                    }
                    else -> {
                        // 难过：耷拉眼 + 撇嘴
                        sadEye(headCx - eyeDX, eyeY)
                        sadEye(headCx + eyeDX, eyeY)
                        drawArc(
                            color = outlineColor,
                            startAngle = 200f, sweepAngle = 140f, useCenter = false,
                            topLeft = Offset(headCx - headR * 0.28f, mouthY - headR * 0.2f),
                            size = Size(headR * 0.56f, headR * 0.42f),
                            style = Stroke(width = 1.4f)
                        )
                    }
                }
                // 脖子
                drawRect(
                    color = outlineColor,
                    topLeft = Offset(0.46f * w, 0.165f * h),
                    size = Size(0.08f * w, 0.035f * h),
                    style = Stroke(width = outlineStroke)
                )
                    // 躯干（肩到胯，圆角矩形）
                    drawRoundRect(
                        color = outlineColor,
                        topLeft = Offset(0.33f * w, 0.21f * h),
                        size = Size(0.34f * w, 0.36f * h),
                        cornerRadius = CornerRadius(0.05f * w, 0.04f * h),
                        style = Stroke(width = outlineStroke)
                    )
                    // 双臂
                    val leftArm = Path().apply {
                        moveTo(0.35f * w, 0.22f * h)
                        lineTo(0.17f * w, 0.52f * h)
                        lineTo(0.21f * w, 0.53f * h)
                        lineTo(0.39f * w, 0.24f * h)
                        close()
                    }
                    val rightArm = Path().apply {
                        moveTo(0.65f * w, 0.22f * h)
                        lineTo(0.83f * w, 0.52f * h)
                        lineTo(0.79f * w, 0.53f * h)
                        lineTo(0.61f * w, 0.24f * h)
                        close()
                    }
                    drawPath(leftArm, outlineColor, style = Stroke(width = outlineStroke))
                    drawPath(rightArm, outlineColor, style = Stroke(width = outlineStroke))
                    // 双腿
                    val leftLeg = Path().apply {
                        moveTo(0.39f * w, 0.56f * h)
                        lineTo(0.42f * w, 0.97f * h)
                        lineTo(0.49f * w, 0.97f * h)
                        lineTo(0.49f * w, 0.56f * h)
                        close()
                    }
                    val rightLeg = Path().apply {
                        moveTo(0.51f * w, 0.56f * h)
                        lineTo(0.51f * w, 0.97f * h)
                        lineTo(0.58f * w, 0.97f * h)
                        lineTo(0.61f * w, 0.56f * h)
                        close()
                    }
                    drawPath(leftLeg, outlineColor, style = Stroke(width = outlineStroke))
                    drawPath(rightLeg, outlineColor, style = Stroke(width = outlineStroke))

                    // —— 命中部位：点 + 脉冲光圈 + 点旁直接写中文名 ——
                    val labelPaint = Paint().apply {
                        color = android.graphics.Color.argb(
                            235, (primary.red * 255).toInt(),
                            (primary.green * 255).toInt(),
                            (primary.blue * 255).toInt()
                        )
                        textSize = labelTextSizePx
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                        isFakeBoldText = true
                    }

                    // labelDx/labelDy 以 360px 基准人体定义，等比缩放
                    val scale = w / 360f

                    PART_POINTS.forEach { (key, pt) ->
                        if (!highlightSet.contains(key)) return@forEach
                        val cx = pt.x * w
                        val cy = pt.y * h

                        // 外层脉冲光圈
                        val outerR = 0.045f * w * (0.8f + 0.6f * pulseScale)
                        drawCircle(
                            color = primary.copy(alpha = 0.16f * (1f - 0.4f * pulseScale)),
                            radius = outerR,
                            center = Offset(cx, cy)
                        )
                        // 中层环
                        drawCircle(
                            color = primary.copy(alpha = 0.6f),
                            radius = 0.022f * w,
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.4f)
                        )
                        // 内层实心点
                        drawCircle(
                            color = primary,
                            radius = 0.013f * w,
                            center = Offset(cx, cy)
                        )

                        // 标签：半透明圆角底色块 + 中文名（直接标注在部位旁）
                        val lx = cx + pt.labelDx * scale
                        val ly = cy + pt.labelDy * scale
                        val textWidth = labelPaint.measureText(pt.label)
                        val textHeight = labelPaint.textSize
                        val pad = 4f
                        val chipLeft = lx - textWidth / 2f - pad
                        val chipTop = ly - textHeight - pad * 0.5f
                        val chipRight = lx + textWidth / 2f + pad
                        val chipBottom = ly + pad * 0.5f
                        drawRoundRect(
                            color = chipBg.copy(alpha = 0.88f),
                            topLeft = Offset(chipLeft, chipTop),
                            size = Size(chipRight - chipLeft, chipBottom - chipTop),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            pt.label, lx, ly, labelPaint
                        )
                    }
            }
        }
    }
}
