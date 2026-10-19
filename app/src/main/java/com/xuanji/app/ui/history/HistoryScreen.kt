package com.xuanji.app.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.data.model.HistoryEvent
import com.xuanji.app.data.local.ReadingFavoriteStore
import com.xuanji.app.data.local.ActionFeedbackDataStore
import com.xuanji.app.data.local.DataStorePreferenceBridge
import com.xuanji.app.data.local.LocalReviewTimelineRepository
import com.xuanji.app.domain.LocalReviewFilter
import com.xuanji.app.domain.LocalReviewItem
import com.xuanji.app.domain.LocalReviewPeriod
import com.xuanji.app.domain.LocalReviewSource
import com.xuanji.app.domain.LocalReviewSnapshot
import com.xuanji.app.domain.LocalReviewTimeline
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.ReadingFavorite
import com.xuanji.app.domain.ReadingFavoriteDataState
import com.xuanji.app.domain.ReadingFavoriteSnapshot
import com.xuanji.app.domain.SameDayBirth
import com.xuanji.app.domain.SameDayWork
import com.xuanji.app.domain.SameDayWorks
import com.xuanji.app.domain.WorkKind
import com.xuanji.app.ui.viewmodel.HistoryViewModel
import com.xuanji.app.ui.components.FortuneDetailPage
import com.xuanji.app.ui.xuanjiViewModel
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(initialRecordDate: String? = null) {
    val viewModel = xuanjiViewModel { HistoryViewModel(AppModule.historyRepository) }
    val events by viewModel.events.collectAsStateWithLifecycle()
    val profile by AppModule.repository.userProfileFlow.collectAsStateWithLifecycle(initialValue = null)
    val baziFull by AppModule.repository.baziFullFlow.collectAsStateWithLifecycle(initialValue = null)
    val favoriteProfileKey = remember(baziFull) { baziFull?.let { "bazi|${it.chart.display}" } }
    val actionProfileKey = remember(profile) { profile?.let(AppModule.actionRepository::profileKey) }
    val localContext = LocalContext.current
    val reviewTimelineRepository = remember(localContext) {
        LocalReviewTimelineRepository(
            AppModule.readingFavoriteStore,
            ActionFeedbackDataStore(DataStorePreferenceBridge(localContext))
        )
    }
    val today = LocalDate.now()
    val todayStr = today.format(DateTimeFormatter.ofPattern("M月d日"))

    // 同月同日生：以「用户出生日期」为准（若已设置），否则退化为当天日期并提示
    val birthDate = profile?.let {
        try {
            LocalDate.of(it.birthYear, it.birthMonth, it.birthDay)
        } catch (_: Exception) { null }
    }
    val sameDayDate = birthDate ?: today
    val sameDayStr = sameDayDate.format(DateTimeFormatter.ofPattern("M月d日"))

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "历史上的今天",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "$todayStr · 岁月长河中的玄学与人文印记",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (events.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "📭",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        "今天暂无收录的历史事件",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "$todayStr 这一天尚未收录专属的玄学与人文印记，日后会继续补充。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            events.forEach { EventCard(it) }
        }

        // 与你同月同日生（以你的出生日期为准）
        Spacer(Modifier.height(4.dp))
        Text(
            "同月同日生",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            if (birthDate != null)
                "$sameDayStr 出生的名人 · 与你（出生日）同月同日生"
            else
                "$sameDayStr 出生的名人 · 暂未设置出生日期，暂按今日展示（请到「我的」填写）",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SameDayBirth.forToday(sameDayDate).forEach { SameDayBirthCard(it) }
        SameDayWorks.forDate(sameDayDate).forEach { SameDayWorkCard(it) }

        Spacer(Modifier.height(8.dp))
        AppModule.dailyLifeStore?.let { store -> DailyLifePanel(store, actionProfileKey, initialRecordDate,
            loadWeeklyMealPool = { actionProfileKey?.let { AppModule.actionRepository.loadMealPool(it) } }) }
        LocalReviewTimelineSection(reviewTimelineRepository, favoriteProfileKey, actionProfileKey)
        ReadingFavoritesSection(AppModule.readingFavoriteStore, favoriteProfileKey)
    }
}

@Composable
fun LocalReviewTimelineSection(
    repository: LocalReviewTimelineRepository,
    favoriteProfileKey: String?,
    actionProfileKey: String?
) {
    var filter by rememberSaveable { mutableStateOf(LocalReviewFilter.All) }
    var period by rememberSaveable { mutableStateOf(LocalReviewPeriod.Week) }
    var snapshot by remember { mutableStateOf(LocalReviewSnapshot(emptyList(), ReadingFavoriteDataState.Missing, true)) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(repository, favoriteProfileKey, actionProfileKey) {
        loaded = false
        snapshot = runCatching { repository.load(favoriteProfileKey, actionProfileKey, LocalReviewFilter.All) }
            .getOrElse { LocalReviewSnapshot(emptyList(), ReadingFavoriteDataState.Corrupt, false) }
        loaded = true
    }
    val summaryDate = LocalDate.now()
    val summary = remember(snapshot.items, period, summaryDate) {
        LocalReviewTimeline.summarize(snapshot.items, period, summaryDate)
    }
    val visibleItems = remember(snapshot.items, filter) {
        snapshot.items.filter { item ->
            when (filter) {
                LocalReviewFilter.All -> true
                LocalReviewFilter.Favorites -> item.source == LocalReviewSource.Favorite
                LocalReviewFilter.ActionFeedback -> item.source != LocalReviewSource.Favorite
            }
        }
    }

    Column(
        Modifier.fillMaxWidth().testTag("local-review-timeline"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("近期选择", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "只回顾你主动收藏的快照与行动反馈；不读取聊天内容，也不推断性格。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(LocalReviewPeriod.Week to "本周", LocalReviewPeriod.Month to "本月").forEach { (choice, label) ->
                FilterChip(
                    selected = period == choice,
                    onClick = { period = choice },
                    modifier = Modifier.testTag("local-review-period-${choice.name.lowercase()}"),
                    label = { Text(label) }
                )
            }
        }
        Card(Modifier.fillMaxWidth().testTag("local-review-period-summary")) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(summary.label, style = MaterialTheme.typography.titleSmall)
                Text(
                    "收藏 ${summary.favorites} · 采纳 ${summary.acceptedActions} · 不合适 ${summary.notSuitableActions} · 换掉 ${summary.replacedActions}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (summary.total == 0) Text("这个周期还没有主动记录。", style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                LocalReviewFilter.All to "全部",
                LocalReviewFilter.Favorites to "收藏",
                LocalReviewFilter.ActionFeedback to "行动反馈"
            ).forEach { (choice, label) ->
                FilterChip(
                    selected = filter == choice,
                    onClick = { filter = choice },
                    modifier = Modifier.testTag("local-review-filter-${choice.name.lowercase()}"),
                    label = { Text(label) }
                )
            }
        }
        when {
            !loaded -> Text("正在读取本地回顾…", style = MaterialTheme.typography.bodyMedium)
            visibleItems.isEmpty() -> Text(
                "还没有主动收藏或行动反馈。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("local-review-empty")
            )
            else -> visibleItems.forEach { item -> LocalReviewItemCard(item) }
        }
        if (!snapshot.feedbackAvailable) {
            Text("行动反馈暂时无法读取；其他可用记录仍保留。", style = MaterialTheme.typography.labelSmall)
        }
        if (snapshot.favoriteState in setOf(ReadingFavoriteDataState.Corrupt, ReadingFavoriteDataState.UnsupportedVersion, ReadingFavoriteDataState.ProfileMismatch)) {
            Text("收藏快照无法加入回顾；原始数据保持不变。", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun LocalReviewItemCard(item: LocalReviewItem) {
    val sourceLabel = when (item.source) {
        LocalReviewSource.Favorite -> "主动收藏"
        LocalReviewSource.AcceptedAction -> "已采纳"
        LocalReviewSource.NotSuitableAction -> "标记不合适"
        LocalReviewSource.ReplacedAction -> "换过一项"
    }
    val timestamp = runCatching {
        Instant.ofEpochMilli(item.createdAt).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("M月d日 HH:mm"))
    }.getOrDefault("时间未知")
    Card(Modifier.fillMaxWidth().testTag("local-review-item-${item.id}")) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleSmall)
            Text("$sourceLabel · $timestamp", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(item.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ReadingFavoritesSection(store: ReadingFavoriteStore, profileKey: String?) {
    var favoriteLoaded by remember(profileKey) { mutableStateOf(false) }
    var favoriteSnapshot by remember(profileKey) {
        mutableStateOf(ReadingFavoriteSnapshot(ReadingFavoriteDataState.Missing))
    }
    var favoriteFilter by rememberSaveable { mutableStateOf("all") }
    var favoriteSearch by rememberSaveable { mutableStateOf("") }
    var favoriteToRead by remember { mutableStateOf<ReadingFavorite?>(null) }
    var showFavoriteClearConfirmation by rememberSaveable { mutableStateOf(false) }
    val favoriteScope = rememberCoroutineScope()
    val favoriteSnackbar = remember { SnackbarHostState() }
    LaunchedEffect(profileKey, store) {
        favoriteSnapshot = if (profileKey.isNullOrBlank()) {
            ReadingFavoriteSnapshot(ReadingFavoriteDataState.InvalidProfile)
        } else {
            runCatching { store.list(profileKey) }
                .getOrElse { ReadingFavoriteSnapshot(ReadingFavoriteDataState.Corrupt) }
        }
        favoriteLoaded = true
    }

    Column(
        Modifier.fillMaxWidth().testTag("reading-favorites-section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("我的收藏", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "仅显示你主动收藏的快照；查看时不重新排盘、不生成新结果。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!profileKey.isNullOrBlank()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("all" to "全部", "fortune" to "运势", "action" to "行动", "mystic" to "玄师", "game" to "游戏")
                    .forEach { (key, label) ->
                        FilterChip(
                            selected = favoriteFilter == key,
                            onClick = { favoriteFilter = key },
                            modifier = Modifier.testTag("reading-favorite-filter-$key"),
                            label = { Text(label) }
                        )
                    }
            }
            OutlinedTextField(
                value = favoriteSearch,
                onValueChange = { favoriteSearch = it },
                modifier = Modifier.fillMaxWidth().testTag("reading-favorite-search"),
                label = { Text("搜索收藏") },
                singleLine = true
            )
            when {
                !favoriteLoaded -> Text("正在读取收藏…", style = MaterialTheme.typography.bodyMedium)
                favoriteSnapshot.state in setOf(
                    ReadingFavoriteDataState.Corrupt,
                    ReadingFavoriteDataState.UnsupportedVersion,
                    ReadingFavoriteDataState.ProfileMismatch
                ) -> Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("收藏数据无法读取，原始内容未被覆盖。")
                        Text("你可以取消并保留数据，或确认清除这份损坏的收藏。", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = { showFavoriteClearConfirmation = true }) {
                            Text("清除损坏收藏")
                        }
                    }
                }
                favoriteSnapshot.state == ReadingFavoriteDataState.InvalidProfile -> Text("当前档案尚未准备好。")
                else -> {
                    val visibleFavorites = favoriteSnapshot.entries.filter { favorite ->
                        (favoriteFilter == "all" || favorite.type == favoriteFilter) &&
                            (favoriteSearch.isBlank() || listOf(
                                favorite.title,
                                favorite.body,
                                favorite.evidence,
                                favorite.mode,
                                favorite.characterId.orEmpty(),
                                favorite.gameId.orEmpty(),
                                favorite.period,
                                favorite.dateKey
                            ).any { it.contains(favoriteSearch, ignoreCase = true) })
                    }
                    if (visibleFavorites.isEmpty()) {
                        Text(
                            if (favoriteSnapshot.entries.isEmpty()) "还没有收藏。打开详情后可点「收藏」，只保存你选中的这一项。"
                            else "没有符合筛选条件的收藏。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        visibleFavorites.forEach { favorite ->
                            ReadingFavoriteCard(
                                favorite = favorite,
                                modifier = Modifier.testTag("reading-favorite-card-${favorite.id}"),
                                onRead = { favoriteToRead = favorite },
                                onRemove = {
                                    val key = profileKey.orEmpty()
                                    favoriteScope.launch {
                                        val updated = runCatching { store.remove(key, favorite.id) }.getOrNull()
                                        if (updated == null) {
                                            favoriteSnapshot = ReadingFavoriteSnapshot(ReadingFavoriteDataState.Corrupt)
                                            return@launch
                                        }
                                        favoriteSnapshot = updated
                                        val result = favoriteSnackbar.showSnackbar(
                                            message = "已取消收藏",
                                            actionLabel = "撤销",
                                            withDismissAction = true
                                        )
                                        if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                            favoriteSnapshot = runCatching { store.upsert(key, favorite) }
                                                .getOrElse { ReadingFavoriteSnapshot(ReadingFavoriteDataState.Corrupt) }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
            if (favoriteSnapshot.entries.isNotEmpty() && favoriteSnapshot.state in setOf(
                    ReadingFavoriteDataState.Ready,
                    ReadingFavoriteDataState.Migrated
                )) {
                OutlinedButton(onClick = { showFavoriteClearConfirmation = true }) { Text("清空本档案收藏") }
            }
        } else {
            Text("设置出生档案后，收藏会自动按档案隔离。", style = MaterialTheme.typography.bodyMedium)
        }
        SnackbarHost(favoriteSnackbar)
    }

    if (showFavoriteClearConfirmation && !profileKey.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { showFavoriteClearConfirmation = false },
            title = { Text("清除本档案收藏？") },
            text = { Text("仅清除当前档案的收藏快照，不影响历史记录、角色对话或游戏存档。此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    val key = profileKey.orEmpty()
                    showFavoriteClearConfirmation = false
                    favoriteScope.launch {
                        favoriteSnapshot = runCatching { store.clear(key) }
                            .getOrElse { ReadingFavoriteSnapshot(ReadingFavoriteDataState.Corrupt) }
                    }
                }, modifier = Modifier.testTag("reading-favorite-confirm-clear")) { Text("清除收藏") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showFavoriteClearConfirmation = false },
                    modifier = Modifier.testTag("reading-favorite-cancel-clear")
                ) { Text("取消") }
            }
        )
    }

    favoriteToRead?.let { favorite ->
        FortuneDetailPage(
            title = favorite.title,
            onDismiss = { favoriteToRead = null }
        ) {
            Text("只读快照 · ${favoriteTypeLabel(favorite.type)}")
            Text(listOf(favorite.mode, favorite.period, favorite.dateKey).filter(String::isNotBlank).joinToString(" · "))
            Text(
                favorite.body,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag("reading-favorite-snapshot-body")
            )
            if (favorite.evidence.isNotBlank()) {
                Text("保存时的依据", style = MaterialTheme.typography.titleSmall)
                Text(favorite.evidence, style = MaterialTheme.typography.bodyMedium)
            }
            Text("算法版本：${favorite.algorithmVersion.ifBlank { "未提供" }}", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ReadingFavoriteCard(
    favorite: ReadingFavorite,
    modifier: Modifier = Modifier,
    onRead: () -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(favorite.title, style = MaterialTheme.typography.titleMedium)
            Text(
                "${favoriteTypeLabel(favorite.type)} · ${listOf(favorite.mode, favorite.period, favorite.dateKey).filter(String::isNotBlank).joinToString(" · ")}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(favorite.body.take(180), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onRead, modifier = Modifier.testTag("reading-favorite-read-${favorite.id}")) { Text("查看快照") }
                TextButton(onClick = onRemove, modifier = Modifier.testTag("reading-favorite-remove-${favorite.id}")) { Text("取消收藏") }
            }
        }
    }
}

private fun favoriteTypeLabel(type: String): String = when (type) {
    "fortune" -> "运势"
    "action" -> "今日行动"
    "mystic" -> "玄师对话"
    "game" -> "游戏摘要"
    else -> "收藏"
}

@Composable
private fun SameDayBirthCard(fig: SameDayBirth.Figure) {
    var expanded by rememberSaveable(fig.name, fig.date) { mutableStateOf(false) }
    val summary = HistoryCopy.summary(fig.note)

    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "${fig.name} · ${fig.title}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AnimatedVisibility(visible = expanded && summary != fig.note) {
                Text(
                    fig.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (summary != fig.note) {
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.semantics {
                        contentDescription = if (expanded) "收起评语" else "展开评语"
                    }
                ) {
                    Text(if (expanded) "收起评语" else "展开评语")
                }
            }
            Text(
                "出生 · ${fig.date}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SameDayWorkCard(work: SameDayWork) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "${if (work.kind == WorkKind.MUSIC) "🎵" else "📝"} ${work.title}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                buildString {
                    append(work.creator)
                    work.year?.takeIf { it > 0 }?.let { append(" · $it") }
                    append(" · ${work.style}")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (work.publicDomain && !work.excerpt.isNullOrBlank()) {
                Text(
                    "“${work.excerpt}”",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                work.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EventCard(event: HistoryEvent) {
    // 事件自身的真实月日（"MM-dd" → "M月d日"），避免旧版「日期被写死」的 Bug 复发
    val dateStr = run {
        val parts = event.date.split("-")
        if (parts.size == 2) "${parts[0].toInt()}月${parts[1].toInt()}日" else event.date
    }
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                dateStr,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                event.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                event.desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                buildString {
                    event.year?.let { append("${it} · ") }
                    append(event.tag)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
