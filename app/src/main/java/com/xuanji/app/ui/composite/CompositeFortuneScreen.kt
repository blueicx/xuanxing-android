package com.xuanji.app.ui.composite

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.data.model.FortuneDimension
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.MysticGuideGenerator
import com.xuanji.app.ui.components.PeriodToggleRow
import com.xuanji.app.ui.components.ResultShare
import com.xuanji.app.ui.components.ShareButton
import com.xuanji.app.ui.viewmodel.CompositeFortuneViewModel
import com.xuanji.app.ui.viewmodel.CompositeUiState
import com.xuanji.app.ui.xuanjiViewModel

@Composable
fun CompositeFortuneScreen(
    viewModel: CompositeFortuneViewModel = xuanjiViewModel { CompositeFortuneViewModel(AppModule.repository) }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val s = state) {
        is CompositeUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("正在综合推算…", style = MaterialTheme.typography.bodyMedium)
        }
        is CompositeUiState.Empty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "尚未设置出生信息，请先在「我的」中填写生日。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        is CompositeUiState.Ready -> CompositeContent(
            bazi = s.bazi,
            fortune = s.fortune,
            period = s.period,
            onPeriodChange = viewModel::setPeriod
        )
    }
}

@Composable
private fun CompositeContent(
    bazi: com.xuanji.app.data.model.BaziFull,
    fortune: com.xuanji.app.data.model.CompositeDailyFortune,
    period: String,
    onPeriodChange: (String) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 顶部综合分
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Box(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 第一行：标题 + 分数（同一个 Row，避免分数单独换到第二行）
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            when (period) {
                                "week" -> "本周综合运势"
                                "month" -> "本月综合运势"
                                else -> "今日综合运势"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "${fortune.overallScore}",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = scoreColor(fortune.overallScore)
                        )
                        Text(
                            "分",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            scoreEmoji(fortune.overallScore),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                    Text(
                        "融合东方八字与西方星座",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                val shareText = remember(period, fortune.overallScore) {
                    ResultShare.fortuneTitle("综合运势", period, fortune.overallScore)
                }
                ShareButton(
                    sharedText = shareText,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 14.dp, end = 14.dp)
                )
            }
        }

        PeriodToggleRow(period, onPeriodChange)

        MysticGuideCard(bazi, fortune)

        // 幸运信息
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LuckChip("幸运数字", fortune.luckyNumber.toString(), Modifier.weight(1f))
            LuckChip("幸运色", fortune.luckyColor, Modifier.weight(1f))
            LuckChip("吉利方位", fortune.luckyDirection, Modifier.weight(1f))
        }

        // 维度
        Text("今日维度", style = MaterialTheme.typography.titleSmall)
        fortune.dimensions.forEach { dim -> DimensionCard(dim) }

        // 注意事项
        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "注意事项",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    fortune.cautions,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            "本页每日运势由出生信息按日期本地确定性推算（离线可用），融合东方八字与西方星座，仅供娱乐参考。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MysticGuideCard(
    bazi: com.xuanji.app.data.model.BaziFull,
    fortune: com.xuanji.app.data.model.CompositeDailyFortune
) {
    val records by AppModule.testRecordRepository.records.collectAsStateWithLifecycle(initialValue = emptyList())
    var mode by rememberSaveable { mutableStateOf("scholar") }
    var topic by rememberSaveable { mutableStateOf("composite") }
    val latestTest = records.maxByOrNull { it.date }
    val guide = remember(mode, topic, bazi, fortune, latestTest) {
        MysticGuideGenerator.generate(mode, topic, bazi, fortune, latestTest)
    }
    val accent by animateColorAsState(
        targetValue = if (mode == "half") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        animationSpec = tween(260),
        label = "mysticAccent"
    )

    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val scholarAccent = MaterialTheme.colorScheme.primary
                val halfAccent = MaterialTheme.colorScheme.tertiary
                PersonaButton("玄学家", "心理按摩", mode == "scholar", scholarAccent, Modifier.weight(1f)) { mode = "scholar" }
                PersonaButton("半仙", "浮夸吐槽", mode == "half", halfAccent, Modifier.weight(1f)) { mode = "half" }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MysticGuideGenerator.topicLabels().forEach { (key, label) ->
                    Surface(
                        onClick = { topic = key },
                        shape = RoundedCornerShape(999.dp),
                        color = if (topic == key) accent.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Text(
                            label,
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (topic == key) accent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            val readingShape = RoundedCornerShape(16.dp)
            Surface(
                Modifier.fillMaxWidth().background(
                    Brush.linearGradient(
                        listOf(accent.copy(alpha = 0.16f), Color.Transparent, accent.copy(alpha = 0.07f)),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    ),
                    readingShape
                ),
                shape = readingShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f)
            ) {
                Crossfade(targetState = guide, label = "mysticReading") { current ->
                    Column(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✦", style = MaterialTheme.typography.titleMedium, color = accent)
                            Text(
                                "${current.roleName} · ${current.headline}",
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            current.body,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            current.signature,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonaButton(
    title: String,
    subtitle: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LuckChip(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DimensionCard(dim: FortuneDimension) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(dim.label, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${dim.score}分",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor(dim.score)
                )
            }
            LinearProgressIndicator(
                progress = { dim.score / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = scoreColor(dim.score),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                dim.interpretation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun scoreColor(score: Int): Color = when {
    score >= 80 -> Color(0xFF1B5E20) // 极佳：深绿
    score >= 65 -> Color(0xFF2E7D32) // 良好：绿
    score >= 50 -> Color(0xFF1565C0) // 平稳：蓝
    score >= 35 -> Color(0xFFEF6C00) // 偏弱：橙
    else -> Color(0xFFC62828)        // 低迷：红
}

/** 根据分数返回对应表情（高分开心、低分勉励） */
private fun scoreEmoji(score: Int): String = when {
    score >= 80 -> "🌟"  // 极佳
    score >= 65 -> "😊"  // 良好
    score >= 50 -> "🙂"  // 平稳
    score >= 35 -> "💪"  // 偏弱/勉励
    else -> "🍀"         // 低迷/转运
}
