package com.xuanji.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 玄学主题：神秘紫 + 流金
val Purple80 = Color(0xFFB69CFF)
val Purple40 = Color(0xFF7C5CFF)
val Gold80 = Color(0xFFE9D8A6)
val Gold40 = Color(0xFFC9A227)
val Jade80 = Color(0xFF8FE3C2)
val Indigo90 = Color(0xFF1A1230)
val Indigo80 = Color(0xFF2A1F4A)

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = Gold80,
    tertiary = Jade80,
    background = Indigo90,
    surface = Indigo80,
    surfaceVariant = Color(0xFF3A2F5C),
    onBackground = Color(0xFFF2ECFF),
    onSurface = Color(0xFFF2ECFF),
    onSurfaceVariant = Color(0xFFC8BEE8)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = Gold40,
    tertiary = Color(0xFF2E8B6F),
    background = Color(0xFFF6F2FF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFECE6FB),
    onBackground = Color(0xFF1A1230),
    onSurface = Color(0xFF1A1230),
    onSurfaceVariant = Color(0xFF5B4F7A)
)

@Composable
fun XuanjiTheme(
    darkTheme: Boolean = true, // 玄学应用默认深色，营造神秘氛围
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
