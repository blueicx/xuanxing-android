package com.xuanji.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.xuanji.app.data.model.BaziFull
import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.domain.MysticGuideGenerator
import kotlin.math.roundToInt

@Composable
fun MysticFloatingGuide(
    bazi: BaziFull?,
    fortune: CompositeDailyFortune?,
    modifier: Modifier = Modifier,
    content: @Composable (ScrollState) -> Unit
) {
    val guideAvailable = bazi != null && fortune != null
    val companionKey = remember(bazi, fortune) {
        if (bazi == null || fortune == null) "unavailable" else "${bazi.hashCode()}|${fortune.hashCode()}"
    }
    val mode = if (guideAvailable) {
        remember(companionKey) { MysticGuideGenerator.suggestedMode("composite", fortune!!) }
    } else ""
    val skin = if (guideAvailable) {
        remember(companionKey) { MysticGuideGenerator.defaultMysticSkin(mode, fortune!!) }
    } else null
    var detailOpen by rememberSaveable(companionKey) { mutableStateOf(false) }
    val pageScroll = rememberScrollState()
    val detailBazi = bazi
    val detailFortune = fortune

    Box(modifier.fillMaxSize()) {
        content(pageScroll)

        if (skin != null) {
            MysticOrb(
                roleName = if (mode == "half") "半仙" else "玄学家",
                skinLabel = skin.label,
                color = Color(skin.garment),
                backColor = Color(skin.back),
                trimColor = Color(skin.trim),
                onClick = { detailOpen = true },
                scrollValue = pageScroll.value,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }

        AnimatedVisibility(
            visible = detailOpen && detailBazi != null && detailFortune != null,
            enter = fadeIn() + scaleIn(initialScale = 0.96f),
            exit = fadeOut() + scaleOut(targetScale = 0.97f)
        ) {
            Surface(
                Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 6.dp, top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (mode == "half") "半仙现场" else "玄学家现场",
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = { detailOpen = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "关闭玄师详情")
                        }
                    }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                    ) {
                        MysticGuideCard(detailBazi!!, detailFortune!!)
                    }
                }
            }
        }
    }
}

@Composable
private fun MysticOrb(
    roleName: String,
    skinLabel: String,
    color: Color,
    backColor: Color,
    trimColor: Color,
    onClick: () -> Unit,
    scrollValue: Int = 0,
    modifier: Modifier = Modifier
) {
    val drift = rememberInfiniteTransition(label = "mysticDrift")
    val wave by drift.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mysticWave"
    )

    Column(
        modifier
            .offset {
                IntOffset(
                    (Math.sin(wave.toDouble()) * 5).roundToInt(),
                    -(scrollValue * 0.035f).roundToInt() + (Math.cos(wave.toDouble()) * 3).roundToInt()
                )
            }
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            Modifier
                .size(62.dp)
                .shadow(14.dp, CircleShape)
                .clip(CircleShape)
                .background(backColor)
                .border(1.5.dp, trimColor.copy(alpha = 0.75f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(color)
            )
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 19.dp)
                    .height(2.dp)
                    .background(trimColor)
            )
            Text(
                if (roleName == "半仙") "半" else "玄",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (roleName == "半仙") {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        }
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Text(
                "$skinLabel · 对话",
                Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
