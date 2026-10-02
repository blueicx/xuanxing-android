package com.xuanji.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import com.xuanji.app.domain.game.StarMapEvent
import com.xuanji.app.domain.game.StarMapPuzzleEngine
import com.xuanji.app.domain.game.StarMapState
import kotlinx.coroutines.launch

/** Small local-game shell; the Chinese chess board keeps using its existing verified UI. */
@Composable
fun CompanionGameCard(
    gameId: String,
    characterId: MysticCharacterId,
    profileKey: String = "",
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = CompanionGameCatalog.byId(gameId) ?: return
    if (profile.characterId != characterId) return
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val favoriteStore = LocalReadingFavoriteStore.current
    val favoriteProfileKey = LocalReadingFavoriteProfileKey.current ?: profileKey
    val readingMetadata = LocalMysticReadingMetadata.current
    val progressStore = remember(context) { CompanionGameProgressStore(DataStorePreferenceBridge(context)) }
    var poetry by remember(gameId, characterId) { mutableStateOf(PoetryChainState()) }
    var starMap by remember(gameId, characterId) { mutableStateOf(StarMapPuzzleEngine.initial()) }
    var route by remember(gameId, characterId) { mutableStateOf(SilkRoadState()) }
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
        scope.launch { runCatching { progressStore.save(profileKey, characterId, state) } }
    }

    fun clearProgress() {
        if (profileKey.isBlank()) return
        scope.launch { runCatching { progressStore.clear(profileKey, characterId, gameId) } }
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
                    Text("接下一句：${prompt.line}", style = MaterialTheme.typography.bodyMedium)
                    prompt.options.forEach { option ->
                        OutlinedButton(
                            onClick = {
                                poetry = PoetryChainEngine.reduce(poetry, PoetryChainEvent.Choose(option))
                                persist(CompanionGameProgressState.Poetry(poetry))
                            },
                            enabled = progressLoaded && !poetry.completed,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(option) }
                    }
                    Text("得分 ${poetry.score} · ${poetry.feedback.ifBlank { "选择一条符合韵脚的自有短句" }}", style = MaterialTheme.typography.labelSmall)
                    if (poetry.completed) {
                        OutlinedButton(onClick = {
                            val result = PoetryChainEngine.result(poetry)
                            saveFinishedSummary("诗句接龙完成：${poetry.promptIndex} 题，得分 ${result.score}；来源为自有短句库。")
                        }, modifier = Modifier.testTag("companion-game-favorite")) { Text("收藏本局摘要") }
                        OutlinedButton(onClick = { poetry = PoetryChainState(); clearProgress() }, enabled = progressLoaded) { Text("重新开始") }
                    }
                }
                "star_map_observation" -> {
                    Text("当前位置：${starMap.current}", style = MaterialTheme.typography.bodyMedium)
                    StarMapPuzzleEngine.options(starMap).forEach { node ->
                        OutlinedButton(
                            onClick = {
                                starMap = StarMapPuzzleEngine.reduce(starMap, StarMapEvent.Move(node))
                                persist(CompanionGameProgressState.StarMap(starMap))
                            },
                            enabled = progressLoaded && !starMap.completed,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("观测 $node") }
                    }
                    Text("路径：${starMap.path.joinToString(" → ")} · ${starMap.feedback}", style = MaterialTheme.typography.labelSmall)
                    if (starMap.completed) {
                        val result = StarMapPuzzleEngine.result(starMap)
                        OutlinedButton(
                            onClick = { saveFinishedSummary("星图逻辑题完成：${result.path.joinToString(" → ")}；${result.rulesLabel}。") },
                            modifier = Modifier.testTag("companion-game-favorite")
                        ) { Text("收藏本局摘要") }
                        OutlinedButton(onClick = { starMap = StarMapPuzzleEngine.initial(); clearProgress() }, enabled = progressLoaded) { Text("重新观测") }
                    }
                }
                "silkroad_route" -> {
                    Text("${route.current} · 剩余 ${route.daysLeft} 天 / ${route.budgetLeft} 点资源", style = MaterialTheme.typography.bodyMedium)
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
                    Text("路线：${route.path.joinToString(" → ")} · ${route.feedback}", style = MaterialTheme.typography.labelSmall)
                    if (route.completed || route.failed) {
                        val result = SilkRoadRouteEngine.result(route)
                        val ending = if (result.completed) "完成" else "结束，未抵达目标"
                        OutlinedButton(
                            onClick = {
                                saveFinishedSummary(
                                    "丝路路线规划$ending：${result.path.joinToString(" → ")}；剩余 ${result.daysLeft} 天、${result.budgetLeft} 点资源。${result.rulesLabel}。"
                                )
                            },
                            modifier = Modifier.testTag("companion-game-favorite")
                        ) { Text("收藏本局摘要") }
                        OutlinedButton(onClick = { route = SilkRoadState(); clearProgress() }, enabled = progressLoaded) { Text("重新规划") }
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
        }
    }
}
