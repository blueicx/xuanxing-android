package com.xuanji.app.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xuanji.app.data.model.Element
import com.xuanji.app.domain.elementColorCompose
import com.xuanji.app.domain.elementName
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/** Compose Color → android.graphics ARGB int */
private fun Color.toNativeInt(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt().coerceIn(0, 255),
    (red * 255).toInt().coerceIn(0, 255),
    (green * 255).toInt().coerceIn(0, 255),
    (blue * 255).toInt().coerceIn(0, 255)
)

/** 顺时针相生顺序：木 → 火 → 土 → 金 → 水 → 木 */
private val CYCLE = listOf(
    Element.WOOD, Element.FIRE, Element.EARTH, Element.METAL, Element.WATER
)

private val SHENG_COLOR = Color(0xFF5FB87A)
private val KE_COLOR = Color(0xFFE0594E)

/**
 * 五行圆盘：按占比展示五行强弱，并绘出相生（外环顺时针绿箭头）与相克（内部五角星红虚线箭头）关系。
 * @param counts 五行出现次数
 */
@Composable
fun WuxingWheel(counts: Map<Element, Int>) {
    val total = CYCLE.sumOf { counts[it] ?: 0 }.coerceAtLeast(1)
    val maxV = (CYCLE.maxOf { counts[it] ?: 0 }).coerceAtLeast(1)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val outline = MaterialTheme.colorScheme.outline
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val elemColors = CYCLE.associateWith { elementColorCompose(it) }

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(4.dp)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val cx = w / 2f
            val cy = w / 2f
            val ringR = w * 0.33f          // 五节点所在圆半径
            val maxNode = w * 0.115f       // 节点最大半径
            val minNode = w * 0.048f       // 节点最小半径（含缺失）

            // 节点圆心（木在正上方，顺时针每 72°）
            val centers = CYCLE.mapIndexed { i, _ ->
                val deg = -90.0 + i * 72.0
                val r = Math.toRadians(deg)
                Offset(cx + ringR * cos(r).toFloat(), cy + ringR * sin(r).toFloat())
            }
            val radii = CYCLE.map { e ->
                val v = counts[e] ?: 0
                minNode + (maxNode - minNode) * (v.toFloat() / maxV)
            }

            // 背景圆
            drawCircle(
                color = surfaceVariant.copy(alpha = 0.30f),
                radius = ringR + maxNode * 0.9f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = outline.copy(alpha = 0.22f),
                style = Stroke(1.dp.toPx()),
                radius = ringR + maxNode * 0.9f,
                center = Offset(cx, cy)
            )

            // ===== 相克（内部五角星，红色虚线 + 箭头）=====
            // 木克土(0→2) 土克水(2→4) 水克火(4→1) 火克金(1→3) 金克木(3→0)
            val keDash = PathEffect.dashPathEffect(floatArrayOf(w * 0.018f, w * 0.014f), 0f)
            for (i in CYCLE.indices) {
                val from = i
                val to = (i + 2) % 5
                val s = shrink(centers[from], centers[to], radii[from] + w * 0.012f)
                val e = shrink(centers[to], centers[from], radii[to] + w * 0.028f)
                drawLine(
                    color = KE_COLOR.copy(alpha = 0.65f),
                    start = s, end = e,
                    strokeWidth = 1.6.dp.toPx(),
                    pathEffect = keDash
                )
                drawArrow(e, s, KE_COLOR.copy(alpha = 0.85f), w * 0.026f, 1.6.dp.toPx())
            }

            // ===== 相生（外环顺时针，绿色弧 + 箭头）=====
            val shengR = ringR + maxNode * 0.62f
            for (i in CYCLE.indices) {
                val a0 = -90.0 + i * 72.0 + 22.0
                val a1 = -90.0 + (i + 1) * 72.0 - 22.0
                drawArc(
                    color = SHENG_COLOR.copy(alpha = 0.75f),
                    startAngle = a0.toFloat(),
                    sweepAngle = (a1 - a0).toFloat(),
                    useCenter = false,
                    topLeft = Offset(cx - shengR, cy - shengR),
                    size = androidx.compose.ui.geometry.Size(shengR * 2, shengR * 2),
                    style = Stroke(1.8.dp.toPx())
                )
                // 弧末端箭头（切线方向）
                val ar = Math.toRadians(a1)
                val tip = Offset(cx + shengR * cos(ar).toFloat(), cy + shengR * sin(ar).toFloat())
                val backA = Math.toRadians(a1 - 6.0)
                val back = Offset(cx + shengR * cos(backA).toFloat(), cy + shengR * sin(backA).toFloat())
                drawArrow(tip, back, SHENG_COLOR, w * 0.026f, 1.8.dp.toPx())
            }

            // ===== 五行节点 =====
            val namePaint = Paint().apply {
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
                textSize = w * 0.062f
                color = android.graphics.Color.WHITE
            }
            val pctPaint = Paint().apply {
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
                textSize = w * 0.036f
                color = onSurface.toNativeInt()
            }
            CYCLE.forEachIndexed { i, e ->
                val v = counts[e] ?: 0
                val c = centers[i]
                val r = radii[i]
                val col = elemColors.getValue(e)
                drawCircle(color = col.copy(alpha = if (v == 0) 0.22f else 0.90f), radius = r, center = c)
                drawCircle(
                    color = col,
                    style = Stroke(1.6.dp.toPx()),
                    radius = r, center = c
                )
                drawContext.canvas.nativeCanvas.drawText(
                    elementName(e), c.x, c.y + namePaint.textSize * 0.35f, namePaint
                )
                // 百分比标在节点外侧
                val outDeg = Math.toRadians(-90.0 + i * 72.0)
                val lr = ringR + maxNode * 1.28f
                val lp = Offset(cx + lr * cos(outDeg).toFloat(), cy + lr * sin(outDeg).toFloat())
                val pct = (v * 100f / total).roundToInt()
                drawContext.canvas.nativeCanvas.drawText(
                    "$pct%", lp.x, lp.y + pctPaint.textSize * 0.35f, pctPaint
                )
            }

            // 中心说明
            val hubPaint = Paint().apply {
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT
                textSize = w * 0.030f
                color = onSurface.copy(alpha = 0.55f).toNativeInt()
            }
            drawContext.canvas.nativeCanvas.drawText("相生 ↻", cx, cy - w * 0.008f, hubPaint)
            drawContext.canvas.nativeCanvas.drawText("相克 ⤫", cx, cy + w * 0.038f, hubPaint)
        }
    }
}

/** 由 a 指向 b 的方向上，从 a 前进 d 距离得到的点 */
private fun shrink(a: Offset, b: Offset, d: Float): Offset {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val len = hypot(dx, dy).coerceAtLeast(0.0001f)
    return Offset(a.x + dx / len * d, a.y + dy / len * d)
}

/** 在 tip 处画一个指向 tip（自 from 来）的箭头 */
private fun DrawScope.drawArrow(tip: Offset, from: Offset, color: Color, len: Float, stroke: Float) {
    val ang = atan2((tip.y - from.y).toDouble(), (tip.x - from.x).toDouble())
    val a1 = ang + Math.toRadians(150.0)
    val a2 = ang - Math.toRadians(150.0)
    drawLine(
        color = color,
        start = tip,
        end = Offset(tip.x + len * cos(a1).toFloat(), tip.y + len * sin(a1).toFloat()),
        strokeWidth = stroke
    )
    drawLine(
        color = color,
        start = tip,
        end = Offset(tip.x + len * cos(a2).toFloat(), tip.y + len * sin(a2).toFloat()),
        strokeWidth = stroke
    )
}

/** 五行占比条列表（百分比 + 数量 + 强弱标签） */
@Composable
fun WuxingRatioList(counts: Map<Element, Int>) {
    val total = CYCLE.sumOf { counts[it] ?: 0 }.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        CYCLE.forEach { e ->
            val v = counts[e] ?: 0
            val pct = v * 100f / total
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(elementColorCompose(e))
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    elementName(e),
                    Modifier.width(20.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (v > 0) Box(
                        Modifier
                            .fillMaxWidth((pct / 100f).coerceIn(0.02f, 1f))
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(elementColorCompose(e))
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "${"%.1f".format(pct)}%",
                    Modifier.width(52.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    ratioTag(v, total),
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        v == 0 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

private fun ratioTag(v: Int, total: Int): String {
    if (v == 0) return "缺"
    val pct = v * 100f / total
    return when {
        pct >= 35f -> "过旺"
        pct >= 25f -> "偏旺"
        pct >= 15f -> "均衡"
        else -> "偏弱"
    }
}
