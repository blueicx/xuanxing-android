package com.xuanji.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.action.ConfidenceLevel
import com.xuanji.app.domain.action.LifeProfile
import com.xuanji.app.domain.MysticDetailContext
import com.xuanji.app.domain.MysticEvidenceBuilder

data class LifeProfileRegionCardModel(
    val title: String,
    val body: String
)

data class LifeProfileCardModel(
    val confidence: String,
    val regions: List<LifeProfileRegionCardModel>
) {
    companion object {
        fun from(profile: LifeProfile): LifeProfileCardModel = LifeProfileCardModel(
            confidence = when (profile.confidence) {
                ConfidenceLevel.High -> "输入完整度高"
                ConfidenceLevel.Medium -> "输入完整度中"
                ConfidenceLevel.Low -> "输入较少"
            },
            regions = profile.regionCandidates.map { candidate ->
                LifeProfileRegionCardModel(
                    title = "匹配示例：${candidate.country} · ${candidate.city}",
                    body = "符合的标签：${candidate.matchedTags.ifEmpty { listOf("基础环境标签") }.joinToString("、")}；未考虑因素：${candidate.unmatchedFactors.ifEmpty { listOf("实时天气、签证、收入和安全") }.joinToString("、")}"
                )
            }
        )
    }
}

@Composable
fun LifeProfileSection(profile: LifeProfile?, modifier: Modifier = Modifier) {
    if (profile == null) {
        FortuneCard(modifier = modifier, title = "人生画像") {
            SectionTitle("人生画像")
            Spacer(Modifier.height(6.dp))
            Text("填写出生信息后，才会生成五行 + 星座画像。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val model = LifeProfileCardModel.from(profile)
    FortuneCard(
        modifier = modifier,
        title = "人生画像",
        detailContext = MysticDetailContext(
            mode = "人生画像",
            period = "终身",
            dateKey = "",
            title = "人生画像",
            result = buildString {
                append("职业倾向：${profile.careerClusters.take(4).joinToString("、") { "${it.label} ${it.score}分" }}。")
                append("色彩灵感：${profile.colorPalette.take(5).joinToString("、") { "${it.label} ${it.hex}" }}。")
                append("环境画像：${profile.environmentProfile.tags.joinToString("、")}；${profile.environmentProfile.rhythm}。")
                append("地区示例：${profile.regionCandidates.take(4).joinToString("、") { "${it.country}·${it.city}" }}。")
                if (profile.missingInputs.isNotEmpty()) append("缺少输入：${profile.missingInputs.joinToString("、")}。")
            },
            evidence = profile.evidence.joinToString("；") { "${it.source}：${it.label}（${it.contribution}）" },
            sourceStatus = "本地人生画像匹配；${model.confidence}",
            algorithmVersion = MysticEvidenceBuilder.ALGORITHM_VERSION
        )
    ) {
        SectionTitle("人生画像")
        Spacer(Modifier.height(6.dp))
        Text("${model.confidence} · ${profile.disclaimer}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (profile.missingInputs.isNotEmpty()) {
            Text("缺少增强输入：${profile.missingInputs.joinToString("、")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
        Spacer(Modifier.height(10.dp))
        Text("职业倾向簇", style = MaterialTheme.typography.titleMedium)
        profile.careerClusters.take(4).forEach { career ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(career.label)
                Text("${career.score} 分")
            }
            Text(career.reasons.take(2).joinToString("；"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("典型任务：${career.typicalTasks.joinToString("；")}\n环境：${career.workEnvironment}\n技能：${career.skills.joinToString("、")}\n体验：${career.trialActivity}", style = MaterialTheme.typography.bodySmall)
        }
        profile.colorPalette.forEach { color ->
            Text("${color.label} · ${color.applications.entries.joinToString("；") { "${it.key}：${it.value}" }}", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(8.dp))
        Text("色彩灵感", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            profile.colorPalette.take(5).forEach { color ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.size(28.dp).background(parseColor(color.hex), CircleShape))
                    Text(color.label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("环境画像：${profile.environmentProfile.tags.joinToString("、")}", style = MaterialTheme.typography.titleMedium)
        Text(profile.environmentProfile.rhythm, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text("地区匹配示例", style = MaterialTheme.typography.titleMedium)
        RegionPreferenceComparisonSection(profile.profileKey)
        model.regions.forEach { region ->
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(region.title)
                Text(region.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun parseColor(hex: String): Color = runCatching { Color(android.graphics.Color.parseColor(hex)) }
    .getOrDefault(Color.Gray)
