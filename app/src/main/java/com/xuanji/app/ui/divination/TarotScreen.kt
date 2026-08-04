package com.xuanji.app.ui.divination

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.data.model.DrawnTarot
import com.xuanji.app.data.model.TarotCard
import com.xuanji.app.di.AppModule
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import com.xuanji.app.ui.viewmodel.TarotViewModel
import com.xuanji.app.ui.xuanjiViewModel

@Composable
fun TarotScreen() {
    val viewModel = xuanjiViewModel { TarotViewModel(AppModule.tarotRepository) }
    val drawn by viewModel.drawn.collectAsStateWithLifecycle()
    val spread by viewModel.spread.collectAsStateWithLifecycle()

    // 翻牌动画：flipProgress 由 LaunchedEffect(drawKey) 驱动，0→1 翻面
    var flipProgress by remember { mutableFloatStateOf(0f) }
    var drawKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(drawKey) {
        if (drawKey > 0) {
            flipProgress = 0f
            animate(0f, 1f, animationSpec = tween(700)) { value, _ -> flipProgress = value }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("塔罗牌", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "静心洗牌，点「抽牌」，牌背翻开的瞬间即是你的答案。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { viewModel.setSpread("single") },
                modifier = Modifier.weight(1f)
            ) {
                Text("单张", color = if (spread == "single") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = { viewModel.setSpread("three") },
                modifier = Modifier.weight(1f)
            ) {
                Text("三张 · 过去现在未来", color = if (spread == "three") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            }
        }

        Button(onClick = {
            viewModel.draw()
            drawKey++
        }, modifier = Modifier.fillMaxWidth()) {
            Text("抽牌")
        }

        // 牌面动画区
        if (drawn.isNotEmpty()) {
            TarotCardFan(
                cards = drawn,
                flipProgress = flipProgress
            )
        }

        drawn.forEach { d ->
            if (flipProgress >= 0.999f) {
                FortuneCard {
                    SectionTitle("${d.position} · ${if (d.reversed) "逆位" else "正位"}")
                    Spacer(Modifier.height(8.dp))
                    Text(d.card.nameCn, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "${d.card.nameEn}${if (d.card.suit.isNotEmpty()) " · ${d.card.suit}" else " · 大阿尔卡纳"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (d.reversed) d.card.reversed else d.card.upright,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (drawn.isEmpty()) {
            Text(
                "静心凝神，选择牌阵后点「抽牌」。78 张牌（22 大阿尔卡纳 + 56 小阿尔卡纳）随机不重复抽取。仅供娱乐参考。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SystemExplanation("tarot")
    }
}

/** 一排牌背 + 翻转显牌的动画区（正面显示真实牌面）。 */
@Composable
private fun TarotCardFan(cards: List<DrawnTarot>, flipProgress: Float) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        cards.forEachIndexed { idx, d ->
            // 逐张错峰翻转
            val progress = ((flipProgress - idx * 0.18f).coerceIn(0f, 1f))
            val rotation = 180f * progress
            val isFront = rotation >= 90f
            val width = 96.dp
            val height = 160.dp
            Box(
                Modifier
                    .padding(horizontal = 6.dp)
                    .width(width)
                    .height(height)
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 14f * density
                    },
                contentAlignment = Alignment.Center
            ) {
                // 牌背（rotation < 90 时显示）
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            BrushFromTwo(Color(0xFF2A1F4A), Color(0xFF1A1230))
                        )
                        .border(1.5.dp, Color(0xFFE9D8A6).copy(alpha = 0.7f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✦", color = Color(0xFFE9D8A6), style = MaterialTheme.typography.headlineMedium)
                        Text(
                            if (d.reversed) "逆位" else "正位",
                            color = Color(0xFFE9D8A6).copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                // 牌面（rotation >= 90 时显示；内层 -rotation 完全抵消外层镜像，文字始终正向）
                if (isFront) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationY = -rotation }
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                BrushFromTwo(Color(0xFF3A2F5C), Color(0xFF2A1F4A))
                            )
                            .border(1.5.dp, Color(0xFFB69CFF), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                        ) {
                            // 顶部：英文名 + 正逆位角标
                            Text(
                                d.card.nameEn,
                                color = Color(0xFFE9D8A6).copy(alpha = 0.75f),
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                            // 中央：专属图案符号
                            Text(
                                tarotSymbol(d.card),
                                fontSize = 34.sp,
                                lineHeight = 40.sp,
                                textAlign = TextAlign.Center
                            )
                            // 牌名
                            Text(
                                d.card.nameCn,
                                color = Color(0xFFF2ECFF),
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                            // 底部：关键词（正/逆位含义的关键词条）
                            Text(
                                tarotKeywords(d.card, d.reversed),
                                color = Color(0xFFE9D8A6),
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                            Text(
                                if (d.reversed) "逆位" else "正位",
                                color = Color(0xFFB69CFF),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrushFromTwo(a: Color, b: Color): androidx.compose.ui.graphics.Brush =
    androidx.compose.ui.graphics.Brush.verticalGradient(listOf(a, b))

/** 每张牌的专属图案符号：大阿尔卡纳按牌义、小阿尔卡纳按花色+牌级。 */
private fun tarotSymbol(card: TarotCard): String {
    if (card.arcana == "major") {
        return when (card.nameCn) {
            "愚者" -> "🃏"
            "魔术师" -> "🪄"
            "女祭司" -> "🌙"
            "女皇" -> "🌾"
            "皇帝" -> "👑"
            "教皇" -> "🕊️"
            "恋人" -> "💞"
            "战车" -> "🛡️"
            "力量" -> "🦁"
            "隐士" -> "🏮"
            "命运之轮" -> "☸️"
            "正义" -> "⚖️"
            "倒吊人" -> "🪢"
            "死神" -> "💀"
            "节制" -> "⚗️"
            "恶魔" -> "😈"
            "高塔" -> "🗼"
            "星星" -> "✨"
            "月亮" -> "🌕"
            "太阳" -> "☀️"
            "审判" -> "📯"
            "世界" -> "🌍"
            else -> "✦"
        }
    }
    val suitSymbol = when (card.suit) {
        "权杖" -> "🔥"
        "圣杯" -> "💧"
        "宝剑" -> "⚔️"
        "星币" -> "🪙"
        else -> "✦"
    }
    // 宫廷牌加角色符号，数字牌直接显示数字
    return when {
        card.nameCn.contains("国王") -> "🤴"
        card.nameCn.contains("王后") -> "👸"
        card.nameCn.contains("骑士") -> "🐎"
        card.nameCn.contains("侍从") -> "🧑‍🎓"
        else -> suitSymbol
    }
}

/** 从正/逆位含义里提取关键词（逗号/顿号分隔，取前 2 个）。 */
private fun tarotKeywords(card: TarotCard, reversed: Boolean): String {
    val text = if (reversed) card.reversed else card.upright
    val parts = text.split('、', '，', ',', ' ')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    return parts.take(2).joinToString(" · ")
}
