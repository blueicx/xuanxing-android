package com.xuanji.app.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Shared v14 card surfaces. Semantic accents stay restrained and never change card content. */
object FortuneSurfaceTokens {
    val CARD_SURFACE = Color(0xFF251D34)
    val CARD_STROKE = Color(0xFF382F4A)
    val CARD_STROKE_WIDTH = 1.dp
    val CARD_SHAPE = RoundedCornerShape(12.dp)
    val CARD_CONTENT_PADDING = 10.dp

    val COMPACT_SURFACE = Color(0xFF29213A)
    val COMPACT_STROKE = Color(0xFF342A48)
    val COMPACT_SHAPE = RoundedCornerShape(11.dp)

    val HERO_GRADIENT = Brush.radialGradient(
        colors = listOf(
            Color(0xFF674B90),
            Color(0xFF35274D),
            Color(0xFF282039)
        )
    )
    val HERO_SHAPE = RoundedCornerShape(17.dp)

    val MEAL_GRADIENT = Brush.linearGradient(
        colors = listOf(
            Color(0xFF4A3151),
            Color(0xFF2D213E),
            Color(0xFF4D3A26)
        )
    )
    val MEAL_STROKE = Color(0xFF83644D)
    val MEAL_SHAPE = RoundedCornerShape(15.dp)

    val ACTION_GRADIENT = Brush.linearGradient(
        colors = listOf(Color(0xFF28213B), Color(0xFF211B2E))
    )
    val ACTIVITY_STROKE = Color(0xFF476363)
    val OUTING_STROKE = Color(0xFF705F3B)
    val ACTION_SHAPE = RoundedCornerShape(13.dp)
}
