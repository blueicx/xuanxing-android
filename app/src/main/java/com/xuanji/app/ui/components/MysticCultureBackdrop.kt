package com.xuanji.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Scene plate shared by every character. It is deliberately independent from
 * the figure bitmap so a missing asset still leaves a recognizable cultural
 * setting instead of a transparent or black stage.
 */
@Composable
fun MysticCultureBackdrop(
    scene: MysticSceneSpec,
    gold: Color,
    moodLevel: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier
            .fillMaxSize()
            .semantics { contentDescription = scene.accessibilityDescription }
    ) {
        drawScenePlate(scene, gold, moodLevel)
    }
}

private fun DrawScope.drawScenePlate(scene: MysticSceneSpec, gold: Color, moodLevel: Float) {
    val w = size.width
    val h = size.height
    val base = Color(scene.backgroundColorArgb)
    val accent = Color(scene.accentColorArgb)
    drawRect(Brush.verticalGradient(listOf(base, base.copy(alpha = .78f), Color(0xFF0D0817))))
    val horizon = h * .65f
    val glow = (.07f + moodLevel.coerceIn(-1f, 1f) * .015f).coerceIn(.035f, .10f)
    drawCircle(accent.copy(alpha = glow), w * .44f, Offset(w * .52f, h * .25f))

    when (scene.id) {
        MysticSceneId.Jiangnan -> drawJiangnanScene(w, h, horizon, accent, gold)
        MysticSceneId.InkElder -> drawInkElderScene(w, h, horizon, accent, gold)
        MysticSceneId.Academy -> drawAcademyScene(w, h, horizon, accent, gold)
        MysticSceneId.Silkroad -> drawSilkroadScene(w, h, horizon, accent, gold)
        MysticSceneId.InkFallback -> drawInkFallbackScene(w, h, horizon, accent, gold)
    }
}

private fun DrawScope.drawJiangnanScene(w: Float, h: Float, horizon: Float, accent: Color, gold: Color) {
    drawRect(Color(0xFF15201F).copy(alpha = .48f), Offset(0f, horizon), Size(w, h - horizon))
    drawArc(accent.copy(alpha = .54f), 180f, 180f, false, style = Stroke(w * .014f), topLeft = Offset(w * .18f, horizon - h * .16f), size = Size(w * .52f, h * .22f))
    drawLine(gold.copy(alpha = .45f), Offset(w * .08f, horizon - h * .08f), Offset(w * .92f, horizon - h * .08f), w * .008f)
    drawLine(accent.copy(alpha = .35f), Offset(w * .10f, horizon + h * .03f), Offset(w * .88f, horizon + h * .03f), w * .006f)
    listOf(.12f, .84f).forEach { x ->
        drawRect(gold.copy(alpha = .42f), Offset(w * x, horizon - h * .30f), Size(w * .012f, h * .23f))
        drawRect(Color(0xFFE1B86D).copy(alpha = .68f), Offset(w * (x - .035f), horizon - h * .32f), Size(w * .08f, h * .08f))
    }
    drawPath(Path().apply {
        moveTo(0f, horizon - h * .12f)
        quadraticBezierTo(w * .23f, horizon - h * .23f, w * .44f, horizon - h * .11f)
        quadraticBezierTo(w * .66f, horizon - h * .20f, w, horizon - h * .10f)
        lineTo(w, horizon)
        lineTo(0f, horizon)
        close()
    }, Color(0xFF6D7E78).copy(alpha = .28f))
}

private fun DrawScope.drawInkElderScene(w: Float, h: Float, horizon: Float, accent: Color, gold: Color) {
    drawRect(Color(0xFFE5DED0).copy(alpha = .12f), Offset(w * .08f, h * .18f), Size(w * .84f, h * .44f))
    drawPath(Path().apply {
        moveTo(w * .08f, horizon)
        lineTo(w * .23f, horizon - h * .21f)
        lineTo(w * .34f, horizon - h * .10f)
        lineTo(w * .51f, horizon - h * .27f)
        lineTo(w * .72f, horizon - h * .12f)
        lineTo(w * .90f, horizon - h * .24f)
        lineTo(w * .98f, horizon)
        close()
    }, Color(0xFFB9B4AA).copy(alpha = .28f))
    drawRect(Color(0xFF302E2C).copy(alpha = .60f), Offset(w * .08f, horizon + h * .04f), Size(w * .84f, h * .16f))
    drawRect(Color(0xFFDDD3C2).copy(alpha = .82f), Offset(w * .20f, horizon - h * .035f), Size(w * .38f, h * .055f))
    drawRect(Color(0xFF161616).copy(alpha = .74f), Offset(w * .63f, horizon - h * .015f), Size(w * .12f, h * .045f))
    drawLine(gold.copy(alpha = .42f), Offset(w * .26f, horizon - h * .05f), Offset(w * .26f, horizon - h * .23f), w * .008f)
    drawLine(accent.copy(alpha = .50f), Offset(w * .78f, horizon - h * .05f), Offset(w * .78f, horizon - h * .25f), w * .006f)
}

private fun DrawScope.drawAcademyScene(w: Float, h: Float, horizon: Float, accent: Color, gold: Color) {
    repeat(14) { i ->
        val x = w * ((i * 37 % 91) / 100f + .04f)
        val y = h * ((i * 19 % 46) / 100f + .08f)
        drawCircle(Color(0xFFE1D8FF).copy(alpha = .34f), w * .006f, Offset(x, y))
    }
    drawArc(accent.copy(alpha = .66f), 185f, 170f, false, style = Stroke(w * .012f), topLeft = Offset(w * .08f, h * .16f), size = Size(w * .84f, h * .72f))
    drawCircle(gold.copy(alpha = .58f), w * .10f, Offset(w * .73f, h * .25f), style = Stroke(w * .008f))
    drawCircle(accent.copy(alpha = .42f), w * .17f, Offset(w * .73f, h * .25f), style = Stroke(w * .004f))
    repeat(4) { i ->
        drawRect(Color(0xFF433B67).copy(alpha = .62f), Offset(w * (.06f + i * .23f), horizon - h * .24f), Size(w * .16f, h * .24f))
        drawLine(gold.copy(alpha = .24f), Offset(w * (.08f + i * .23f), horizon - h * .20f), Offset(w * (.19f + i * .23f), horizon - h * .20f), w * .006f)
    }
}

private fun DrawScope.drawSilkroadScene(w: Float, h: Float, horizon: Float, accent: Color, gold: Color) {
    drawPath(Path().apply {
        moveTo(0f, horizon)
        quadraticBezierTo(w * .24f, horizon - h * .16f, w * .52f, horizon)
        quadraticBezierTo(w * .78f, horizon - h * .13f, w, horizon - h * .03f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }, Color(0xFF8E5B3C).copy(alpha = .60f))
    drawArc(accent.copy(alpha = .70f), 180f, 180f, false, style = Stroke(w * .026f), topLeft = Offset(w * .08f, horizon - h * .30f), size = Size(w * .34f, h * .34f))
    drawCircle(gold.copy(alpha = .52f), w * .12f, Offset(w * .73f, horizon - h * .19f), style = Stroke(w * .012f))
    drawCircle(gold.copy(alpha = .24f), w * .08f, Offset(w * .73f, horizon - h * .19f), style = Stroke(w * .006f))
    listOf(.13f, .88f).forEach { x ->
        drawLine(gold.copy(alpha = .42f), Offset(w * x, horizon - h * .22f), Offset(w * x, horizon + h * .01f), w * .009f)
        drawCircle(Color(0xFFE2A76D).copy(alpha = .78f), w * .035f, Offset(w * x, horizon - h * .24f))
    }
}

private fun DrawScope.drawInkFallbackScene(w: Float, h: Float, horizon: Float, accent: Color, gold: Color) {
    drawPath(Path().apply {
        moveTo(0f, horizon)
        lineTo(w * .22f, horizon - h * .20f)
        lineTo(w * .42f, horizon - h * .07f)
        lineTo(w * .62f, horizon - h * .24f)
        lineTo(w, horizon - h * .10f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }, accent.copy(alpha = .28f))
    drawLine(gold.copy(alpha = .30f), Offset(w * .14f, horizon - h * .15f), Offset(w * .86f, horizon - h * .15f), w * .006f)
}
