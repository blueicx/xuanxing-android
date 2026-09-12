package com.xuanji.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun MysticCultureBackdrop(
    skinId: String,
    gold: Color,
    moodLevel: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier.fillMaxSize()) { drawCultureBackdrop(MysticCultureSpec.forSkin(skinId), gold, moodLevel) }
}

private fun DrawScope.drawCultureBackdrop(spec: MysticCultureSpec, gold: Color, moodLevel: Float) {
    val w = size.width
    val h = size.height
    val ink = Color(0xFF0D0817)
    drawRect(Brush.verticalGradient(listOf(ink, Color(0xFF211433), Color(0xFF120C1D))))
    val horizon = h * .70f
    when (spec.scene) {
        CulturalScene.JIANGNAN_GARDEN -> {
            repeat(3) { i -> drawArc(Color(0xFF6CA7A1).copy(alpha = .30f), 180f, 180f, false, style = Stroke(w * .006f), topLeft = Offset(w * (.08f + i * .28f), horizon), size = Size(w * .22f, h * .10f)) }
            drawLine(gold.copy(alpha = .36f), Offset(w * .10f, horizon - h * .10f), Offset(w * .38f, horizon - h * .10f), w * .012f)
        }
        CulturalScene.ACADEMY_ARCHIVE -> repeat(4) { i -> drawRect(Color(0xFF6C587F).copy(alpha = .28f), Offset(w * (.05f + i * .14f), horizon - h * .22f), Size(w * .10f, h * .22f)) }
        CulturalScene.SILKROAD_CARAVANSERAI -> drawArc(Color(0xFFD18D58).copy(alpha = .40f), 180f, 180f, false, style = Stroke(w * .02f), topLeft = Offset(w * .08f, horizon - h * .22f), size = Size(w * .26f, h * .28f))
        CulturalScene.NORTHLAND_FIRE -> drawPath(Path().apply { moveTo(w * .38f, horizon); lineTo(w * .44f, horizon - h * .18f); lineTo(w * .50f, horizon); close() }, Color(0xFFE6A04E).copy(alpha = .42f))
        CulturalScene.DAOIST_CLOUD_TERRACE -> repeat(3) { i -> drawArc(Color.White.copy(alpha = .24f), 180f, 180f, false, style = Stroke(w * .012f), topLeft = Offset(w * (.10f + i * .28f), horizon - h * .10f), size = Size(w * .24f, h * .10f)) }
        CulturalScene.CITY_NIGHT -> listOf(.08f to .18f, .22f to .30f, .40f to .24f, .62f to .34f, .82f to .20f).forEach { (x, height) -> drawRect(Color(0xFF5664B8).copy(alpha = .35f), Offset(w * x, horizon - h * height), Size(w * .10f, h * height)) }
        CulturalScene.DESERT_DUSK -> drawPath(Path().apply { moveTo(0f, horizon); quadraticBezierTo(w * .25f, horizon - h * .15f, w * .52f, horizon); quadraticBezierTo(w * .78f, horizon - h * .12f, w, horizon); lineTo(w, h); lineTo(0f, h); close() }, Color(0xFFAA704C).copy(alpha = .32f))
        CulturalScene.FESTIVAL_COURTYARD -> drawLine(Color(0xFFD26A65).copy(alpha = .50f), Offset(w * .08f, horizon - h * .18f), Offset(w * .92f, horizon - h * .14f), w * .010f)
        CulturalScene.NEUTRAL_STAGE -> Unit
    }
    drawCircle(gold.copy(alpha = .06f + .04f * (moodLevel + 1f)), w * .48f, Offset(w * .5f, h * .25f))
}
