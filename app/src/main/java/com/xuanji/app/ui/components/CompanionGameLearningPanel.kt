package com.xuanji.app.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.xuanji.app.domain.game.*

@Composable
internal fun PoetryStructurePanel(prompt: PoetryPrompt) {
    var open by rememberSaveable { mutableStateOf(false) }
    var input by rememberSaveable(prompt.id) { mutableStateOf("") }
    TextButton(onClick = { open = !open }) { Text(if (open) "收起句式练习" else "句式与对仗练习") }
    if (open) {
        Text("出处：${prompt.source}；不是借用古代诗人的原文。")
        OutlinedTextField(input, { input = it.take(80) }, label = { Text("试写自己的下句") }, modifier = Modifier.testTag("poetry-own-line"))
        if (input.isNotBlank()) Text(CompanionGameTeaching.poetryStructure(prompt.line, input).explanation,
            modifier = Modifier.testTag("poetry-structure-result"))
    }
}

@Composable
internal fun StarLogicLearningPanel(state: StarMapState) {
    var open by rememberSaveable(state.puzzleIndex) { mutableStateOf(false) }
    TextButton(onClick = { open = !open }, modifier = Modifier.testTag("star-solution-toggle")) { Text(if (open) "收起逻辑解法" else "查看本题逻辑解法") }
    if (open) {
        val path = remember(state) { CompanionGameTeaching.shortestStarPath(state) }
        Text("先把星点当作图的节点，连接当作边。广度优先搜索按步数逐层寻找当前点到目标的最短路径。")
        Text(if (path.isEmpty()) "当前存档没有可验证的路线，未编造解法。" else "从当前点至少还需 ${path.size - 1} 步：${path.joinToString(" → ")}")
        Text("你的已走路线：${state.path.joinToString(" → ")}。查看解法不会替你行走；可用撤销重新尝试。此图是自编逻辑题，不是真实星位或占星结论。")
    }
}

@Composable
internal fun XiangqiRuleLearningPanel(position: BoardPosition, reviewing: Boolean) {
    var open by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { open = !open }, modifier = Modifier.testTag("xiangqi-rule-lesson")) { Text(if (open) "收起规则演练" else if (reviewing) "解释回放局面的走法" else "当前局面逐步规则演练") }
    if (open) {
        val moves = remember(position) { XiangqiRules.legalMoves(position, position.sideToMove) }
        var selected by remember(position) { mutableStateOf<BoardMove?>(null) }
        moves.forEachIndexed { index, move -> TextButton(onClick = { selected = move }, modifier = Modifier.testTag("lesson-move-$index")) {
            Text(XiangqiNotation.format(move, position))
        } }
        if (moves.isEmpty()) Text("当前局面没有合法走法。")
        selected?.let { move -> CompanionGameTeaching.xiangqiSteps(position, move).forEach { Text(it) } }
        Text("仅解释此帧，不执行落子，也不改变残局、棋谱或旧存档。")
    }
}

@Composable
internal fun SilkRoadReferencePanel(state: SilkRoadState) {
    var open by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { open = !open }) { Text(if (open) "收起驿站资料" else "查看本地驿站资料与资源复盘") }
    if (open) {
        val visited = SilkRoadGeography.cities.filter { it.id in state.path }
        visited.forEach { city -> Text("${city.id} · 纬度${city.latitude} / 经度${city.longitude}；来源 ${city.source}") }
        Text(SilkRoadGeography.ATTRIBUTION)
        Text("路线任务及驿站事件为项目自编；天数/补给是游戏资源，不能换算现实旅行费用、安全或历史商旅耗时。")
        SilkRoadRouteEngine.replay(state).forEach { Text("${it.from} → ${it.to}：${it.days} 天、${it.cost} 补给；累计${it.daysSpent}天/${it.budgetSpent}补给") }
    }
}
