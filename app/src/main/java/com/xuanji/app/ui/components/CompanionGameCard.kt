package com.xuanji.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.xuanji.app.data.local.CompanionGameProgressStore
import com.xuanji.app.data.local.DataStorePreferenceBridge
import com.xuanji.app.domain.MysticCharacterId
import com.xuanji.app.domain.CompanionGameFavoriteFactory
import com.xuanji.app.domain.ReadingFavoriteDataState
import com.xuanji.app.domain.game.CompanionGameCatalog
import com.xuanji.app.domain.game.CompanionGameProgressState
import com.xuanji.app.domain.game.GameAvailability
import com.xuanji.app.domain.game.PoetryChainEngine
import com.xuanji.app.domain.game.PoetryChainEvent
import com.xuanji.app.domain.game.PoetryChainState
import com.xuanji.app.domain.game.SilkRoadEvent
import com.xuanji.app.domain.game.SilkRoadRouteEngine
import com.xuanji.app.domain.game.SilkRoadState
import com.xuanji.app.domain.game.SilkRoadMissionCatalog
import com.xuanji.app.domain.game.StarMapEvent
import com.xuanji.app.domain.game.StarMapPuzzleEngine
import com.xuanji.app.domain.game.StarMapState
import kotlinx.coroutines.launch

/** Small local-game shell; the Chinese chess board keeps using its existing verified UI. */
@Composable
fun CompanionGameCard(
    gameId: String,
    characterId: MysticCharacterId,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    profileKey: String = "",
    progressStoreOverride: CompanionGameProgressStore? = null
) {
    val profile = CompanionGameCatalog.byId(gameId) ?: return
    if (profile.characterId != characterId) return
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val favoriteStore = LocalReadingFavoriteStore.current
    val favoriteProfileKey = LocalReadingFavoriteProfileKey.current ?: profileKey
    val readingMetadata = LocalMysticReadingMetadata.current
    val progressStore = progressStoreOverride ?: remember(context) { CompanionGameProgressStore(DataStorePreferenceBridge(context)) }
    var poetry by remember(gameId, characterId, profileKey) { mutableStateOf(PoetryChainState()) }
    var starMap by remember(gameId, characterId, profileKey) { mutableStateOf(StarMapPuzzleEngine.initial()) }
    var route by remember(gameId, characterId, profileKey) { mutableStateOf(SilkRoadState()) }
    var pendingMission by remember(gameId, characterId, profileKey) { mutableStateOf<String?>(null) }
    var progressNotice by remember(gameId, characterId, profileKey) { mutableStateOf("") }
    var progressLoaded by remember(gameId, characterId, profileKey) { mutableStateOf(false) }
    var favoriteNotice by remember(gameId, characterId, profileKey) { mutableStateOf("") }

    LaunchedEffect(gameId, characterId, profileKey) {
        progressLoaded = false
        when (val saved = runCatching { progressStore.load(profileKey, characterId, gameId) }.getOrNull()) {
            is CompanionGameProgressState.Poetry -> poetry = saved.value
            is CompanionGameProgressState.StarMap -> starMap = saved.value
            is CompanionGameProgressState.SilkRoad -> route = saved.value
            null -> Unit
        }
        progressLoaded = true
    }

    fun persist(state: CompanionGameProgressState) {
        if (!progressLoaded || profileKey.isBlank()) return
        scope.launch {
            progressNotice = if (runCatching { progressStore.save(profileKey, characterId, state) }.isSuccess)
                "进度已保存到本机。" else "保存失败；当前进度仅在本次界面中保留。"
        }
    }

    fun clearProgress() {
        if (profileKey.isBlank()) return
        scope.launch { runCatching { progressStore.clear(profileKey, characterId, gameId) } }
    }

    pendingMission?.let { id ->
        val mission = SilkRoadMissionCatalog.find(id)
        if (mission != null) AlertDialog(
            onDismissRequest = { pendingMission = null },
            title = { Text("开始「${mission.title}」？") },
            text = { Text("当前丝路任务进度将被新任务替换。${mission.explanation}") },
            confirmButton = { TextButton(onClick = {
                route = SilkRoadRouteEngine.reduce(route, SilkRoadEvent.SelectMission(id))
                persist(CompanionGameProgressState.SilkRoad(route))
                pendingMission = null
            }, modifier = Modifier.testTag("silkroad-confirm-mission")) { Text("开始新任务") } },
            dismissButton = { TextButton(onClick = { pendingMission = null }) { Text("保留当前进度") } }
        )
    }

    fun saveFinishedSummary(summary: String) {
        val store = favoriteStore ?: run {
            favoriteNotice = "收藏入口暂不可用；本局进度仍保存在游戏存档中。"
            return
        }
        if (favoriteProfileKey.isBlank()) {
            favoriteNotice = "当前档案未就绪，未保存收藏。"
            return
        }
        val favorite = CompanionGameFavoriteFactory.fromResult(
            id = "game-summary-${System.currentTimeMillis()}",
            characterId = characterId,
            gameId = gameId,
            dateKey = readingMetadata.dateKey,
            summary = summary,
            sourceNote = "来自本机${profile.title}进度；用户主动收藏。此内容是游戏记录，不是占卜结论。"
        ) ?: run {
            favoriteNotice = "本局摘要不完整，未保存收藏。"
            return
        }
        scope.launch {
            val result = runCatching { store.upsert(favoriteProfileKey, favorite) }.getOrNull()
            favoriteNotice = if (result?.state == ReadingFavoriteDataState.Ready) {
                "本局摘要已收藏到「我的收藏」。"
            } else {
                "本局摘要未能保存；游戏进度不受影响。"
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = profile.accessibilityLabel },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .6f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(profile.title, style = MaterialTheme.typography.titleMedium)
                    Text(profile.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = onClose) { Text("收起") }
            }
            if (!progressLoaded) {
                Text("正在读取本机游戏进度…", style = MaterialTheme.typography.labelSmall)
            }
            when (gameId) {
                "poetry_chain" -> {
                    val prompt = PoetryChainEngine.prompt(poetry)
                    if (!poetry.completed) {
                        Text("第 ${poetry.promptIndex + 1} / ${poetry.levelCountAtStart} 题 · 接下一句：${prompt.line}", style = MaterialTheme.typography.bodyMedium)
                        Text("出处：${prompt.source}", style = MaterialTheme.typography.labelSmall)
                        prompt.options.forEachIndexed { index, option ->
                            OutlinedButton(
                                onClick = {
                                    poetry = PoetryChainEngine.reduce(poetry, PoetryChainEvent.Choose(option))
                                    persist(CompanionGameProgressState.Poetry(poetry))
                                },
                                enabled = progressLoaded,
                                modifier = Modifier.fillMaxWidth().testTag("poetry-option-$index")
                            ) { Text(option) }
                        }
                        Text("提示：${prompt.hint}", style = MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    poetry = PoetryChainEngine.reduce(poetry, PoetryChainEvent.Hint)
                                    persist(CompanionGameProgressState.Poetry(poetry))
                                }, enabled = progressLoaded, modifier = Modifier.testTag("poetry-hint")
                            ) { Text("再看提示") }
                            OutlinedButton(
                                onClick = {
                                    poetry = PoetryChainEngine.reduce(poetry, PoetryChainEvent.Undo)
                                    persist(CompanionGameProgressState.Poetry(poetry))
                                }, enabled = progressLoaded && poetry.undoHistory.isNotEmpty(), modifier = Modifier.testTag("poetry-undo")
                            ) { Text("撤销一步") }
                        }
                    }
                    Text("得分 ${poetry.score} · ${poetry.feedback.ifBlank { "选择一条符合韵脚的自有短句" }}", style = MaterialTheme.typography.labelSmall)
                    if (poetry.completed) {
                        val result = PoetryChainEngine.result(poetry)
                        Text(result.summary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("poetry-result-summary"))
                        OutlinedButton(onClick = { saveFinishedSummary(result.summary) }, modifier = Modifier.testTag("companion-game-favorite")) { Text("收藏本局摘要") }
                        OutlinedButton(onClick = { poetry = PoetryChainState(); clearProgress() }, enabled = progressLoaded) { Text("重新开始") }
                    }
                }
                "star_map_observation" -> {
                    val puzzle = StarMapPuzzleEngine.puzzle(starMap)
                    Text("第 ${starMap.puzzleIndex + 1} / ${StarMapPuzzleEngine.puzzleCount} 题 · ${puzzle.start} → ${puzzle.target}", style = MaterialTheme.typography.titleSmall)
                    Text("当前位置：${starMap.current}", style = MaterialTheme.typography.bodyMedium)
                    Text("路径：${starMap.path.joinToString(" → ")}", style = MaterialTheme.typography.labelSmall)
                    if (!starMap.completed) {
                        StarMapPuzzleEngine.options(starMap).forEachIndexed { index, node ->
                            OutlinedButton(
                                onClick = {
                                    starMap = StarMapPuzzleEngine.reduce(starMap, StarMapEvent.Move(node))
                                    persist(CompanionGameProgressState.StarMap(starMap))
                                },
                                enabled = progressLoaded,
                                modifier = Modifier.fillMaxWidth().testTag("starmap-option-$index")
                            ) { Text("观测 $node") }
                        }
                        Text("提示：${puzzle.hint}", style = MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    starMap = StarMapPuzzleEngine.reduce(starMap, StarMapEvent.Hint)
                                    persist(CompanionGameProgressState.StarMap(starMap))
                                }, enabled = progressLoaded, modifier = Modifier.testTag("starmap-hint")
                            ) { Text("显示提示") }
                            OutlinedButton(
                                onClick = {
                                    starMap = StarMapPuzzleEngine.reduce(starMap, StarMapEvent.Undo)
                                    persist(CompanionGameProgressState.StarMap(starMap))
                                }, enabled = progressLoaded && starMap.path.size > 1, modifier = Modifier.testTag("starmap-undo")
                            ) { Text("撤销连线") }
                        }
                    }
                    Text(starMap.feedback.ifBlank { "${puzzle.start} 起步，按连线逐步抵达目标。" }, style = MaterialTheme.typography.labelSmall)
                    if (starMap.completed) {
                        val result = StarMapPuzzleEngine.result(starMap)
                        Text(result.summary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("starmap-result-summary"))
                        OutlinedButton(
                            onClick = { saveFinishedSummary(result.summary) },
                            modifier = Modifier.testTag("companion-game-favorite")
                        ) { Text("收藏本局摘要") }
                        if (StarMapPuzzleEngine.hasNextPuzzle(starMap)) {
                            OutlinedButton(
                                onClick = {
                                    starMap = StarMapPuzzleEngine.reduce(starMap, StarMapEvent.NextPuzzle)
                                    persist(CompanionGameProgressState.StarMap(starMap))
                                }, enabled = progressLoaded, modifier = Modifier.testTag("starmap-next-level")
                            ) { Text("下一题") }
                        }
                        OutlinedButton(
                            onClick = {
                                starMap = StarMapPuzzleEngine.reduce(starMap, StarMapEvent.RestartPuzzle)
                                persist(CompanionGameProgressState.StarMap(starMap))
                            }, enabled = progressLoaded, modifier = Modifier.testTag("starmap-restart-level")
                        ) { Text("重玩本题") }
                        OutlinedButton(onClick = { starMap = StarMapPuzzleEngine.initial(); clearProgress() }, enabled = progressLoaded) { Text("从第一题重开") }
                    }
                }
                "silkroad_route" -> {
                    SilkRoadMap(route, progressLoaded) { event ->
                        route = SilkRoadRouteEngine.reduce(route, event)
                        persist(CompanionGameProgressState.SilkRoad(route))
                    }
                    val mission = SilkRoadMissionCatalog.find(route.missionId)
                    Text("任务：${mission?.title.orEmpty()} · 目标${route.target}", style = MaterialTheme.typography.titleSmall)
                    Text(mission?.explanation.orEmpty(), style = MaterialTheme.typography.labelSmall)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SilkRoadMissionCatalog.all.forEach { task ->
                            OutlinedButton(onClick = { pendingMission = task.id }, enabled = progressLoaded && task.id != route.missionId,
                                modifier = Modifier.testTag("silkroad-mission-${task.id}")) { Text(task.title) }
                        }
                    }
                    Text("${route.current} · 剩余 ${route.daysLeft} 天 / ${route.budgetLeft} 点资源", style = MaterialTheme.typography.bodyMedium)
                    if (route.path.size > 1 || route.failed) {
                        OutlinedButton(
                            onClick = {
                                route = SilkRoadRouteEngine.reduce(route, SilkRoadEvent.Undo)
                                persist(CompanionGameProgressState.SilkRoad(route))
                            }, enabled = progressLoaded && (route.path.size > 1 || route.failed),
                            modifier = Modifier.testTag("silkroad-undo")
                        ) { Text("撤销上一步") }
                    }
                    SilkRoadRouteEngine.destinations(route).forEach { city ->
                        OutlinedButton(
                            onClick = {
                                route = SilkRoadRouteEngine.reduce(route, SilkRoadEvent.Travel(city.id))
                                persist(CompanionGameProgressState.SilkRoad(route))
                            },
                            enabled = progressLoaded && !route.completed && !route.failed,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("前往${city.label}（${city.days}天·${city.cost}资源）") }
                    }
                    Text(
                        "路线：${route.path.joinToString(" → ")} · ${route.feedback}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.testTag("silkroad-route-path")
                    )
                    SilkRoadRouteEngine.replay(route).forEachIndexed { index, leg ->
                        Text(
                            "第 ${index + 1} 段 ${leg.from} → ${leg.to} · ${leg.days} 天 / ${leg.cost} 资源 · 累计 ${leg.daysSpent} 天 / ${leg.budgetSpent} 资源",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.testTag("silkroad-replay-leg-$index")
                        )
                    }
                    if (route.completed || route.failed) {
                        val result = SilkRoadRouteEngine.result(route)
                        val ending = if (result.completed) "完成" else "结束，未抵达目标"
                        Text(result.summary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("silkroad-result-summary"))
                        OutlinedButton(
                            onClick = { saveFinishedSummary(result.summary.ifBlank { "丝路路线规划$ending。${result.rulesLabel}。" }) },
                            modifier = Modifier.testTag("companion-game-favorite")
                        ) { Text("收藏本局摘要") }
                        OutlinedButton(onClick = { route = SilkRoadRouteEngine.initial(route.missionId); persist(CompanionGameProgressState.SilkRoad(route)) }, enabled = progressLoaded) { Text("重新规划") }
                    }
                }
                else -> {
                    Text(
                        if (profile.availability == GameAvailability.NotEnabled) "该游戏 provider 尚未启用，不显示假棋局。"
                        else "请从角色舞台进入对应游戏。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            if (favoriteNotice.isNotBlank()) {
                Text(favoriteNotice, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
            if (progressNotice.isNotBlank()) Text(progressNotice, style = MaterialTheme.typography.labelSmall)
        }
    }
}
