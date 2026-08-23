package com.xuanji.app.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.ZodiacCalculator
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private val SIGN_NAMES = listOf(
    "白羊", "金牛", "双子", "巨蟹", "狮子", "处女",
    "天秤", "天蝎", "射手", "摩羯", "水瓶", "双鱼"
)
private val SIGN_COLORS = listOf(
    Color(0xFFE0594E), Color(0xFFC9A227), Color(0xFF4A90D9), Color(0xFF5FB87A),
    Color(0xFFE0594E), Color(0xFFC9A227), Color(0xFF4A90D9), Color(0xFF5FB87A),
    Color(0xFFE0594E), Color(0xFFC9A227), Color(0xFF4A90D9), Color(0xFF5FB87A)
)
private val PLANET_SHORT = mapOf(
    "太阳" to "日", "月亮" to "月", "水星" to "水", "金星" to "金",
    "火星" to "火", "木星" to "木", "土星" to "土", "天王星" to "天",
    "海王星" to "海", "冥王星" to "冥", "北交" to "北"
)
private val ASPECT_COLORS = mapOf(
    "合" to Color(0xFFB388FF),
    "六合" to Color(0xFF81C784),
    "拱" to Color(0xFF64B5F6),
    "刑" to Color(0xFFE57373),
    "冲" to Color(0xFFFFB74D)
)

/** Compose Color → android.graphics.Color ARGB int（避免 toArgb 包解析歧义） */
private fun Color.toNative(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt().coerceIn(0, 255),
    (red * 255).toInt().coerceIn(0, 255),
    (green * 255).toInt().coerceIn(0, 255),
    (blue * 255).toInt().coerceIn(0, 255)
)

/**
 * 圆盘本命星盘（Canvas 绘制）：
 * - 外圈：12 星座（按黄经均分，锚定黄道，整体随上升点旋转）
 * - 中圈：12 宫位（等宫制，自上升点起逆时针）
 * - 内区：十大行星落点（按黄经弧度分布，邻近自动错开）
 * - 标注：ASC / MC、行星相位连线
 */
@Composable
fun NatalWheelChart(chart: ZodiacCalculator.NatalChart) {
    val asc = chart.ascendant
    var selected by remember(chart) { mutableStateOf<ZodiacCalculator.PlanetPosition?>(null) }
    val planetMeanings = remember(chart) { ZodiacCalculator.interpretChart(chart).planets }
    val colorSurface = MaterialTheme.colorScheme.surface
    val colorOnSurface = MaterialTheme.colorScheme.onSurface
    val colorVariant = MaterialTheme.colorScheme.surfaceVariant
    val colorPrimary = MaterialTheme.colorScheme.primary
    val colorSecondary = MaterialTheme.colorScheme.secondary

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(8.dp)
        ) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(chart) {
                        detectTapGestures { tapOffset ->
                            val canvasWidth = size.width.toFloat()
                            val centerX = canvasWidth / 2f
                            val centerY = canvasWidth / 2f
                            val outerRadius = canvasWidth / 2f - 4.dp.toPx()
                            val innerRadius = outerRadius * 0.80f
                            val hubRadius = outerRadius * 0.17f
                            val planetRing = (hubRadius + innerRadius) / 2f

                            fun angleFor(lon: Double): Double {
                                val normalized = (((lon - asc) % 360.0) + 360.0) % 360.0
                                return Math.toRadians(270.0 - normalized)
                            }
                            fun positionAt(lon: Double): Offset {
                                val angle = angleFor(lon)
                                return Offset(
                                    centerX + planetRing * sin(angle).toFloat(),
                                    centerY - planetRing * cos(angle).toFloat()
                                )
                            }

                            var hitName = ""
                            var nearestDistance = canvasWidth * 0.055f
                            chart.planets.forEach { planet ->
                                val point = positionAt(planet.longitude)
                                val distance = hypot(
                                    tapOffset.x - point.x,
                                    tapOffset.y - point.y
                                )
                                if (distance < nearestDistance) {
                                    nearestDistance = distance
                                    hitName = planet.name
                                }
                            }
                            selected = chart.planets.firstOrNull { it.name == hitName }
                        }
                    }
            ) {
            val w = size.width
            val cx = w / 2f
            val cy = w / 2f
            val outer = w / 2f - 4.dp.toPx()
            val signOuter = outer
            val signInner = outer * 0.80f
            val houseOuter = signInner
            val hub = outer * 0.17f
            val planetR = (hub + houseOuter) / 2f

            fun angleFor(lon: Double): Double {
                val d = (((lon - asc) % 360.0) + 360.0) % 360.0
                return 270.0 - d // 以「上北」为 0、顺时针为正 → 上升点落在 9 点钟（左）
            }
            fun pt(r: Float, lon: Double): Offset {
                val phi = Math.toRadians(angleFor(lon))
                return Offset(cx + r * sin(phi).toFloat(), cy - r * cos(phi).toFloat())
            }
            fun canvasDeg(lon: Double): Double = angleFor(lon) - 90.0

            // 底盘
            drawCircle(color = colorSurface, radius = outer, center = Offset(cx, cy))
            drawCircle(
                color = colorOnSurface.copy(alpha = 0.5f),
                style = Stroke(2.dp.toPx()),
                radius = outer, center = Offset(cx, cy)
            )

            // 星座扇区 + 分界线
            for (i in 0..11) {
                val a = i * 30.0
                drawArc(
                    color = SIGN_COLORS[i].copy(alpha = 0.16f),
                    startAngle = canvasDeg(a).toFloat(),
                    sweepAngle = -30f,
                    useCenter = true,
                    topLeft = Offset(cx - signOuter, cy - signOuter),
                    size = Size(signOuter * 2, signOuter * 2)
                )
                val p1 = pt(signOuter, a)
                val p2 = pt(signInner, a)
                drawLine(
                    color = colorOnSurface.copy(alpha = 0.25f),
                    start = p1, end = p2, strokeWidth = 1.dp.toPx()
                )
            }
            drawCircle(
                color = colorOnSurface.copy(alpha = 0.5f),
                style = Stroke(1.5.dp.toPx()),
                radius = signInner, center = Offset(cx, cy)
            )

            // 星座名
            val signPaint = Paint().apply {
                color = colorOnSurface.copy(alpha = 0.92f).toNative()
                textAlign = Paint.Align.CENTER
                textSize = w * 0.032f
                typeface = Typeface.DEFAULT
            }
            for (i in 0..11) {
                val mid = i * 30.0 + 15.0
                val p = pt((signOuter + signInner) / 2f, mid)
                drawContext.canvas.nativeCanvas.drawText(
                    SIGN_NAMES[i], p.x, p.y + signPaint.textSize / 3, signPaint
                )
            }

            // 宫位分界线 + 宫号
            val hPaint = Paint().apply {
                color = colorSecondary.copy(alpha = 0.95f).toNative()
                textAlign = Paint.Align.CENTER
                textSize = w * 0.028f
                typeface = Typeface.DEFAULT_BOLD
            }
            for (h in 1..12) {
                val cusp = asc + (h - 1) * 30.0
                drawLine(
                    color = colorOnSurface.copy(alpha = 0.32f),
                    start = pt(houseOuter, cusp), end = pt(hub, cusp),
                    strokeWidth = 1.dp.toPx()
                )
                val hMid = asc + (h - 1) * 30.0 + 15.0
                val hp = pt(houseOuter - w * 0.05f, hMid)
                drawContext.canvas.nativeCanvas.drawText(
                    h.toString(), hp.x, hp.y + hPaint.textSize / 3, hPaint
                )
            }

            // 相位连线
            chart.aspects.forEach { a ->
                val pa = chart.planets.firstOrNull { it.name == a.p1 } ?: return@forEach
                val pb = chart.planets.firstOrNull { it.name == a.p2 } ?: return@forEach
                val c = ASPECT_COLORS[a.type] ?: colorOnSurface
                drawLine(
                    color = c.copy(alpha = 0.45f),
                    start = pt(planetR, pa.longitude), end = pt(planetR, pb.longitude),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // 行星落点（邻近错开）
            val sorted = chart.planets.sortedBy { it.longitude }
            val groups = mutableListOf<MutableList<ZodiacCalculator.PlanetPosition>>()
            sorted.forEach { p ->
                val last = groups.lastOrNull()
                if (last != null && ((p.longitude - last.last().longitude + 360.0) % 360.0) < 5.0) last.add(p)
                else groups.add(mutableListOf(p))
            }
            val planetPaint = Paint().apply {
                color = colorPrimary.toNative()
                textAlign = Paint.Align.CENTER
                textSize = w * 0.034f
                typeface = Typeface.DEFAULT_BOLD
            }
            groups.forEach { g ->
                g.forEachIndexed { idx, p ->
                    val offset = if (g.size == 1) 0f else (idx - (g.size - 1) / 2f) * (w * 0.05f)
                    val r = (planetR + offset).coerceIn(hub + w * 0.05f, houseOuter - w * 0.05f)
                    val pp = pt(r, p.longitude)
                    if (selected?.name == p.name) {
                        drawCircle(
                            color = colorSecondary,
                            style = Stroke(
                                width = 1.6.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 5f))
                            ),
                            radius = w * 0.042f,
                            center = pp
                        )
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        PLANET_SHORT[p.name] ?: p.name, pp.x, pp.y + planetPaint.textSize / 3, planetPaint
                    )
                }
            }

            // 中心枢纽
            drawCircle(color = colorVariant, radius = hub, center = Offset(cx, cy))
            drawCircle(
                color = colorOnSurface.copy(alpha = 0.4f),
                style = Stroke(1.dp.toPx()), radius = hub, center = Offset(cx, cy)
            )

            // ASC / MC 标注（贴外缘）
            val markPaint = Paint().apply {
                textAlign = Paint.Align.CENTER
                textSize = w * 0.03f
                typeface = Typeface.DEFAULT_BOLD
            }
            markPaint.color = colorPrimary.toNative()
            val ascP = pt(outer - 2.dp.toPx(), asc)
            drawContext.canvas.nativeCanvas.drawText("ASC", ascP.x, ascP.y, markPaint)
            markPaint.color = colorSecondary.toNative()
            val mcP = pt(outer - 2.dp.toPx(), chart.midheaven)
            drawContext.canvas.nativeCanvas.drawText("MC", mcP.x, mcP.y, markPaint)
        }

        selected?.let { planet ->
            val meaning = planetMeanings.firstOrNull { it.name == planet.name }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${planet.name} · ${planet.sign} ${planet.degreeInSign}°",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "第 ${planet.house} 宫",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                    )
                }
                Text(
                    meaning?.text ?: "行星揭示该领域的心理功能与现实课题；结合星座与宫位理解它的表达方式。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
                )
            }
        }
    }
}
}
