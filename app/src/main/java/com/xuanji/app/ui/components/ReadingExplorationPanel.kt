package com.xuanji.app.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

internal data class ReadingExplorationInputs(val profile: com.xuanji.app.data.model.UserProfile?)
internal val LocalReadingExplorationInputs = staticCompositionLocalOf<ReadingExplorationInputs?> { null }

@Composable
internal fun ReadingExplorationPanel(detail: MysticDetailContext) {
    val period = ReadingExploration.periodCode(detail.period) ?: return
    val date = runCatching { LocalDate.parse(detail.dateKey) }.getOrNull() ?: return
    if (detail.mode !in setOf("综合", "东方", "西方")) return
    var expanded by rememberSaveable(detail) { mutableStateOf(false) }
    TextButton(onClick = { expanded = !expanded }, modifier = Modifier.testTag("reading-explore-toggle")) {
        Text(if (expanded) "收起计算探索" else "探索计算步骤、相邻周期与时辰敏感性")
    }
    if (!expanded) return
    val injected = LocalReadingExplorationInputs.current
    val profile = if (injected != null) injected.profile else {
        val current by AppModule.repository.userProfileState.collectAsStateWithLifecycle()
        current
    }
    var readings by remember(detail, profile) { mutableStateOf<List<CompositeDailyFortune>?>(null) }
    var sensitivity by remember(detail, profile) { mutableStateOf(emptyList<ReadingExploration.SensitivitySample>()) }
    var error by remember(detail, profile) { mutableStateOf(false) }
    LaunchedEffect(detail, profile) {
        val selected = profile ?: return@LaunchedEffect
        runCatching { withContext(Dispatchers.Default) {
            (-1..1).map { direction -> ReadingExploration.calculate(selected, ReadingExploration.adjacent(date, period, direction), period) } to
                if (detail.mode != "西方") ReadingExploration.baziSensitivity(selected) else emptyList()
        } }.onSuccess { (values, samples) -> readings = values; sensitivity = samples }.onFailure { error = true }
    }
    Text("当前算法的离线试算，不覆盖已保存的历史结果；分数是文化规则分，不是现实成功率。")
    if (profile == null) Text("缺少出生档案，无法试算。")
    else if (error) Text("输入超出当前算法范围或试算失败；未补造分数。")
    else if (readings == null) Text("正在本地计算…")
    readings?.let { values ->
        Text("相邻周期对照", style = MaterialTheme.typography.titleMedium)
        values.forEachIndexed { index, fortune ->
            val score = when (detail.mode) { "东方" -> fortune.eastern.overallScore; "西方" -> fortune.western.overallScore; else -> fortune.overallScore }
            Text("${listOf("上一周期", "当前周期", "下一周期")[index]} · ${fortune.dateKey} · $score 分")
        }
        val current = values[1]
        if (detail.mode == "综合") {
            Text("实际计分步骤", style = MaterialTheme.typography.titleMedium)
            CompositeFortuneGenerator.calculationSteps(current).forEach { Text(it.explanation()) }
            Text("总分：(六维均值×4＋东方${current.eastern.overallScore}＋西方${current.western.overallScore})÷6，四舍五入并限于12–97 = ${CompositeFortuneGenerator.calculatedOverall(current)}。原始分项缺失时使用对应体系总分；依据不重复累计。")
        } else {
            val insights = if (detail.mode == "东方") current.eastern.insights else current.western.insights
            insights.forEach { Text("${it.tag} · ${it.title}：${it.body}（信号权重 ${it.weight}，并非总分的独立相加项）") }
        }
    }
    if (sensitivity.isNotEmpty()) {
        Text("八字出生时间 ±60 分钟试算", style = MaterialTheme.typography.titleMedium)
        sensitivity.forEach { Text("${it.time} → ${it.chart}") }
        Text("只重算真实受时辰影响的四柱，不修改出生档案、不做出生时间校正，也不外推到心理、健康或投资；节气边界仍受现有近似误差限制。")
    }
}
