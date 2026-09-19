package com.xuanji.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.MysticGuideGenerator

@Composable
fun MysticStageLayout(
    mode: String,
    skinId: String,
    garment: Color,
    trimColor: Color,
    moodLevel: Float,
    onClose: () -> Unit,
    topStartContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val reducedMotion = rememberReducedMotion()
    val phase by if (reducedMotion) {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0f) }
    } else {
        rememberInfiniteTransition(label = "stageMotion").animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing), RepeatMode.Restart),
            label = "stagePhase"
        )
    }
    val gold = Color(0xFFD9C58B)
    Surface(Modifier.fillMaxSize(), color = Color(0xFF0D0817), contentColor = Color(0xFFF4EEE5)) {
        Box(Modifier.fillMaxSize()) {
            MysticCultureBackdrop(skinId, gold, moodLevel)
            Column(Modifier.fillMaxSize()) {
                Box(
                    Modifier.fillMaxWidth().fillMaxHeight(.60f).padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MysticFigureCanvas(mode, skinId, garment, trimColor, moodLevel, phase, reducedMotion, Modifier.fillMaxSize(.72f))
                }
                Column(
                    Modifier.fillMaxWidth().weight(1f)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE60D0817), Color(0xFF0D0817))))
                        .navigationBarsPadding().imePadding().padding(horizontal = 18.dp, vertical = 10.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(mode.let { MysticGuideGenerator.personaName(it) }, style = MaterialTheme.typography.headlineSmall, color = gold)
                    MaterialTheme(colorScheme = darkColorScheme(primary = gold, tertiary = Color(0xFFE3A579))) { content() }
                }
            }
            Box(Modifier.align(Alignment.TopStart).statusBarsPadding().padding(start = 12.dp, top = 12.dp)) { topStartContent() }
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 16.dp, top = 14.dp), horizontalArrangement = Arrangement.End) {
                Surface(
                    onClick = onClose,
                    shape = CircleShape,
                    color = Color(0xFF241731).copy(alpha = .80f),
                    border = BorderStroke(1.dp, gold.copy(alpha = .38f)),
                    modifier = Modifier.semantics { contentDescription = "关闭玄师台" }
                ) { Icon(Icons.Filled.Close, contentDescription = "关闭玄师台", modifier = Modifier.padding(10.dp)) }
            }
        }
    }
}
