package com.xuanji.app.ui.components

import com.xuanji.app.domain.MysticGuideGenerator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.sin

/** The restrained human silhouette used by the immersive companion stage. */
@androidx.compose.runtime.Composable
fun MysticFigureCanvas(
    mode: String,
    skinId: String,
    styleId: String,
    garment: Color,
    trimColor: Color,
    moodLevel: Float,
    phase: Float,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val description = "${MysticGuideGenerator.personaName(mode)}人物形象"
    MysticFigureAsset(
        styleId = styleId,
        contentDescription = description,
        modifier = modifier.fillMaxSize()
    ) {
        Canvas(modifier.fillMaxSize().semantics { contentDescription = description }) {
            drawRestrainedFigure(
                half = mode == "half",
                skinId = skinId,
                garment = garment,
                trim = trimColor,
                moodLevel = moodLevel,
                phase = if (reducedMotion) 0f else phase
            )
        }
    }
}

private fun DrawScope.drawRestrainedFigure(
    half: Boolean,
    skinId: String,
    garment: Color,
    trim: Color,
    moodLevel: Float,
    phase: Float
) {
    val w = size.width
    val h = size.height
    val cx = w * .5f
    val sway = sin(phase * 2f * Math.PI).toFloat() * w * .008f
    val skin = if (half) Color(0xFFF0D9D2) else Color(0xFFFFE6D1)
    val hair = if (half) Color(0xFF2A1A31) else Color(0xFF332B3A)
    val shadow = Color.Black.copy(alpha = .22f)

    drawOval(shadow, topLeft = androidx.compose.ui.geometry.Offset(cx - w * .18f, h * .89f), size = androidx.compose.ui.geometry.Size(w * .36f, h * .035f))
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(garment.copy(alpha = .98f), garment.copy(alpha = .72f))),
        topLeft = androidx.compose.ui.geometry.Offset(cx - w * .20f + sway, h * .42f),
        size = androidx.compose.ui.geometry.Size(w * .40f, h * .47f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .06f)
    )
    drawPath(
        Path().apply {
            moveTo(cx - w * .20f + sway, h * .47f)
            lineTo(cx + sway, h * .73f)
            lineTo(cx + w * .20f + sway, h * .47f)
            close()
        },
        trim.copy(alpha = .45f)
    )
    drawRoundRect(
        color = trim.copy(alpha = .85f),
        topLeft = androidx.compose.ui.geometry.Offset(cx - w * .015f + sway, h * .43f),
        size = androidx.compose.ui.geometry.Size(w * .03f, h * .38f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .012f)
    )
    drawLine(trim.copy(alpha = .86f), androidx.compose.ui.geometry.Offset(cx - w * .18f + sway, h * .50f), androidx.compose.ui.geometry.Offset(cx + w * .18f + sway, h * .50f), w * .012f)

    drawRoundRect(skin, androidx.compose.ui.geometry.Offset(cx - w * .045f + sway, h * .31f), androidx.compose.ui.geometry.Size(w * .09f, h * .16f), androidx.compose.ui.geometry.CornerRadius(w * .035f))
    drawOval(hair, topLeft = androidx.compose.ui.geometry.Offset(cx - w * .115f + sway, h * .15f), size = androidx.compose.ui.geometry.Size(w * .23f, h * .24f))
    drawOval(skin, topLeft = androidx.compose.ui.geometry.Offset(cx - w * .092f + sway, h * .19f), size = androidx.compose.ui.geometry.Size(w * .184f, h * .17f))
    drawArc(hair, 180f, 180f, true, topLeft = androidx.compose.ui.geometry.Offset(cx - w * .092f + sway, h * .17f), size = androidx.compose.ui.geometry.Size(w * .184f, h * .10f))

    val eyeY = h * .275f
    val eyeOffset = w * .035f
    drawCircle(Color(0xFF332B3A), w * .009f, androidx.compose.ui.geometry.Offset(cx - eyeOffset + sway, eyeY))
    drawCircle(Color(0xFF332B3A), w * .009f, androidx.compose.ui.geometry.Offset(cx + eyeOffset + sway, eyeY))
    drawLine(Color(0xFF8E5A5B), androidx.compose.ui.geometry.Offset(cx - w * .025f + sway, h * .34f), androidx.compose.ui.geometry.Offset(cx + w * .025f + sway, h * (.34f - moodLevel * .008f)), w * .006f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    drawCircle(trim.copy(alpha = .55f), w * .018f, androidx.compose.ui.geometry.Offset(cx + w * .16f + sway, h * .46f))
    if (skinId == "festival-costume") drawCircle(Color(0xFFB8462F).copy(alpha = .8f), w * .022f, androidx.compose.ui.geometry.Offset(cx - w * .15f + sway, h * .22f))
    if (skinId == "street-jacket") drawArc(trim.copy(alpha = .8f), 180f, 180f, false, style = Stroke(w * .012f), topLeft = androidx.compose.ui.geometry.Offset(cx + w * .06f + sway, h * .24f), size = androidx.compose.ui.geometry.Size(w * .10f, h * .08f))
}
