package com.xuanji.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.action.ClimateDataKind
import com.xuanji.app.domain.action.LifeProfileDetails
import com.xuanji.app.domain.action.OfflineRegionComparison
import com.xuanji.app.domain.action.RegionPreference
import java.util.Locale

/** Separate factual comparison from symbolic matching; only expanded rows show full attribution. */
@Composable
fun RegionPreferenceComparisonSection(profileKey: String) {
    var climate by rememberSaveable(profileKey) { mutableStateOf("未选择") }
    var rhythm by rememberSaveable(profileKey) { mutableStateOf("未选择") }
    var budget by rememberSaveable(profileKey) { mutableStateOf("未选择") }
    var showAll by rememberSaveable(profileKey) { mutableStateOf(false) }
    var evidenceCity by rememberSaveable(profileKey) { mutableStateOf<String?>(null) }
    listOf("气候" to listOf("未选择", "温暖", "凉爽"), "节奏" to listOf("未选择", "舒缓", "活跃"),
        "预算" to listOf("未选择", "低预算", "中预算", "宽裕")).forEach { (label, options) ->
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { option ->
                val selected = when (label) { "气候" -> climate; "节奏" -> rhythm; else -> budget }
                FilterChip(selected == option, onClick = {
                    when (label) { "气候" -> climate = option; "节奏" -> rhythm = option; else -> budget = option }
                }, label = { Text("$label·$option") })
            }
        }
    }
    val preference = RegionPreference(climate, rhythm, budget)
    val comparisons = OfflineRegionComparison.compare(preference)
    Text(LifeProfileDetails.regionExplanation(preference), style = MaterialTheme.typography.bodySmall)
    Text("现实偏好对比 · 与文化匹配分开", style = MaterialTheme.typography.titleSmall)
    Text("同为1991–2020参考期；测站与网格方法不同，仅作粗略温度阈值比较，不是精确城市排名。",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    comparisons.take(if (showAll) comparisons.size else 6).forEach { comparison ->
        val key = comparison.city.key
        val normal = comparison.climate
        TextButton(onClick = { evidenceCity = key.takeUnless { evidenceCity == key } },
            modifier = Modifier.fillMaxWidth().testTag("region-source-$key")) {
            Column(Modifier.fillMaxWidth()) {
                Text("${comparison.city.city} · ${when (comparison.climateMatch) {
                    true -> "符合气候阈值"; false -> "不符合气候阈值"; null -> "未筛选气候"
                }}")
                Text(if (normal == null) "气候资料未收录" else
                    "${if (normal.dataKind == ClimateDataKind.GriddedReanalysis) "网格再分析" else "测站常年值"} · 12月均值简单平均 ${String.format(Locale.ROOT, "%.1f", normal.annualMonthMeanC)}°C · ${if (evidenceCity == key) "收起依据" else "查看依据"}",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        if (evidenceCity == key) Text(comparison.explanation,
            modifier = Modifier.testTag("region-evidence-$key"), style = MaterialTheme.typography.bodySmall)
    }
    TextButton(onClick = { showAll = !showAll }, modifier = Modifier.testTag("region-show-all")) {
        Text(if (showAll) "收起城市列表" else "查看全部${comparisons.size}个城市")
    }
}
