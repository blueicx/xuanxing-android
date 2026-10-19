package com.xuanji.app.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.xuanji.app.data.local.DailyLifeStore
import com.xuanji.app.domain.*
import com.xuanji.app.ui.components.FortuneDetailPage
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

@Composable
fun DailyLifePanel(
    store: DailyLifeStore, profileKey: String?, initialDate: String? = null,
    loadWeeklyMealPool: (suspend () -> List<com.xuanji.app.domain.action.MealSuggestion>?)? = null
) {
    if (profileKey.isNullOrBlank()) { Text("保存出生档案后可使用安排与记录日历。"); return }
    val scope = rememberCoroutineScope()
    val revision by store.revision.collectAsState()
    var document by remember(profileKey) { mutableStateOf(DailyLifeDocument()) }
    var dateKey by rememberSaveable(profileKey, initialDate) { mutableStateOf(runCatching { LocalDate.parse(initialDate).toString() }.getOrDefault(LocalDate.now().toString())) }
    var snapshotId by rememberSaveable(profileKey) { mutableStateOf<String?>(null) }
    var editId by rememberSaveable(profileKey) { mutableStateOf<String?>(null) }
    var moveId by rememberSaveable(profileKey) { mutableStateOf<String?>(null) }
    var copyMealsTarget by rememberSaveable(profileKey) { mutableStateOf<String?>(null) }
    var repeatId by rememberSaveable(profileKey) { mutableStateOf<String?>(null) }
    var moveDay by rememberSaveable(profileKey) { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(profileKey, revision) { document = store.load(profileKey) }
    val date = runCatching { LocalDate.parse(dateKey) }.getOrDefault(LocalDate.now())
    val month = YearMonth.from(date)
    fun operate(action: suspend () -> Unit) { scope.launch { message = runCatching { action() }.fold({ null }, { "操作失败，现有记录保留。" }) } }
    Column(Modifier.fillMaxWidth().testTag("daily-life-panel"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("安排与记录日历", style = MaterialTheme.typography.headlineSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { dateKey = date.minusMonths(1).toString() }) { Text("‹ 上月") }
            Text("${month.year} 年 ${month.monthValue} 月")
            TextButton(onClick = { dateKey = date.plusMonths(1).toString() }) { Text("下月 ›") }
        }
        val cells = List(month.atDay(1).dayOfWeek.value - 1) { 0 } + (1..month.lengthOfMonth()).toList()
        cells.chunked(7).forEach { week -> Row(Modifier.fillMaxWidth()) {
            week.forEach { day -> Box(Modifier.weight(1f)) {
                if (day != 0) {
                    val key = month.atDay(day).toString()
                    val marked = document.agenda.any { it.dateKey == key } || document.snapshots.any { it.dateKey == key }
                    TextButton(onClick = { dateKey = key }, modifier = Modifier.testTag("calendar-day-$key")) {
                        Text("$day${if (marked) "·" else ""}", color = if (key == dateKey) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            } }
            repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
        } }
        Text(dateKey, style = MaterialTheme.typography.titleMedium)
        if (!document.readable) Text("安排数据暂不可读；未覆盖原数据。")
        if (document.readable) DailyJournalPanel(store, profileKey, date, document)
        val agenda = document.agenda.filter { it.dateKey == dateKey }
        if (agenda.isEmpty()) Text("当天没有主动加入的安排。")
        if (agenda.any { !it.completed }) OutlinedButton(onClick = { moveDay = dateKey }, modifier = Modifier.testTag("agenda-move-day")) {
            Text("当天待办整体延期")
        }
        agenda.forEach { item -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
            Text("${item.time.ifBlank { "时间未定" }} · ${item.title}")
            Text(if (item.durationMinutes == 0) "持续时长未记录" else "持续 ${item.durationMinutes} 分钟", style = MaterialTheme.typography.bodySmall)
            if (item.category == "meal") {
                Text("计划份数：${item.portions} 份 · 食材数量按实际人数调整")
                Text(item.estimatedPriceCentsPerPortion?.let { perPortion ->
                    val total = perPortion.toLong() * item.portions
                    "目录参考预算：约 ¥${"%.2f".format(java.util.Locale.ROOT, total / 100.0)}（每份约 ¥${"%.2f".format(java.util.Locale.ROOT, perPortion / 100.0)}）"
                } ?: "目录未提供预算估算；不推测实际花费。", style = MaterialTheme.typography.bodySmall)
            }
            if (item.note.isNotBlank()) Text(item.note)
            Row {
                TextButton(onClick = { operate { store.setCompleted(profileKey, item.id, !item.completed) } }, modifier = Modifier.testTag("agenda-complete-${item.id}")) { Text(if (item.completed) "撤销完成" else "标记完成") }
                TextButton(onClick = { editId = item.id }) { Text("编辑") }
                if (!item.completed) TextButton(onClick = { moveId = item.id }) { Text("延期") }
                TextButton(onClick = { operate { store.removeAgenda(profileKey, item.id) } }) { Text("删除") }
            }
            if (!item.completed) TextButton(onClick = { repeatId = item.id }, modifier = Modifier.testTag("agenda-repeat-${item.id}")) {
                Text("批量重复安排")
            }
        } } }
        AgendaCalendarExportAction(document, date)
        val weekStart = date.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        Text("本周餐单", style = MaterialTheme.typography.titleMedium)
        if (loadWeeklyMealPool != null) WeeklyMenuPanel(store, profileKey, date, loadWeeklyMealPool)
        Text("仅整理你主动保存的菜单；不代表未来运势预测。可先选一天加入建议，再复制到其他日期。", style = MaterialTheme.typography.bodySmall)
        (0L..6L).forEach { offset ->
            val weekDate = weekStart.plusDays(offset).toString()
            val meals = document.agenda.filter { it.dateKey == weekDate && it.category == "meal" }
            Text("${weekStart.plusDays(offset).dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.SIMPLIFIED_CHINESE)} ${weekDate.substring(5)}：" +
                (meals.joinToString("、") { "${it.title}（${it.portions}份）" }.ifBlank { "未安排" }))
        }
        if (agenda.any { it.category == "meal" }) OutlinedButton(onClick = { copyMealsTarget = date.plusDays(1).toString() }) {
            Text("把当天餐食排到另一日")
        }
        PantryShoppingPanel(store, profileKey, document, dateKey)
        Text("保存的原始结果", style = MaterialTheme.typography.titleMedium)
        document.snapshots.filter { it.dateKey == dateKey }.forEach { item ->
            Row { TextButton(onClick = { snapshotId = item.id }) { Text("${item.mode} · ${item.title} · ${item.period}") }
                TextButton(onClick = { operate { store.removeSnapshot(profileKey, item.id) } }) { Text("删除") }
            }
        }
        ReminderPreferencesEditor(document.reminders) { preferences -> operate { store.saveReminders(profileKey, preferences) } }
        message?.let { Text(it) }
    }
    document.snapshots.firstOrNull { it.id == snapshotId }?.let { item ->
        FortuneDetailPage(item.title, { snapshotId = null }) {
            Text(item.body); Text("依据", style = MaterialTheme.typography.titleMedium); Text(item.evidence)
            Text("${item.dateKey} · ${item.period} · 算法 ${item.algorithmVersion}")
        }
    }
    document.agenda.firstOrNull { it.id == editId }?.let { item ->
        var time by remember(item) { mutableStateOf(item.time) }; var note by remember(item) { mutableStateOf(item.note) }
        var portions by remember(item) { mutableStateOf(item.portions.toString()) }
        var duration by remember(item) { mutableStateOf(item.durationMinutes.takeIf { it > 0 }?.toString().orEmpty()) }
        var conflicts by remember(item) { mutableStateOf(emptyList<DailyAgendaItem>()) }
        AlertDialog(onDismissRequest = { editId = null }, title = { Text(item.title) }, text = { Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
            OutlinedTextField(time, { time = it.take(5); conflicts = emptyList() }, label = { Text("时间 HH:mm") })
            OutlinedTextField(duration, { duration = it.filter(Char::isDigit).take(4); conflicts = emptyList() },
                label = { Text("持续分钟（留空为未知，最多 1440）") }, modifier = Modifier.testTag("agenda-edit-duration"))
            val slotDuration = duration.toIntOrNull()
            if (slotDuration != null && slotDuration in 1..1440) {
                val free = AgendaPlanning.freeSlots(document.agenda.filterNot { it.id == item.id }, LocalDate.parse(item.dateKey), slotDuration)
                Text("可用时段（08:00–20:00，每 15 分钟检查）", style = MaterialTheme.typography.bodySmall)
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    free.take(4).forEach { slot -> TextButton(onClick = { time = slot; conflicts = emptyList() }) { Text(slot) } }
                }
                if (free.isEmpty()) Text("当前范围内没有容得下该时长的空闲时段。")
            }
            OutlinedTextField(note, { note = it.take(2000) }, label = { Text("备注") })
            if (item.category == "meal") OutlinedTextField(
                portions, { portions = it.filter(Char::isDigit).take(2) }, label = { Text("份数（1–20）") }
            )
            if (conflicts.isNotEmpty()) Text("该时段已有：${conflicts.joinToString("、") { it.title }}。再次保存可保留重叠安排。", color = MaterialTheme.colorScheme.error)
        } }, confirmButton = { TextButton(enabled = (time.isBlank() || runCatching { LocalTime.parse(time) }.isSuccess) &&
            (duration.isBlank() || duration.toIntOrNull()?.let { it in 1..1440 } == true) &&
            (item.category != "meal" || portions.toIntOrNull()?.let { it in 1..20 } == true), onClick = {
            scope.launch {
                val changed = item.copy(time = time, note = note, portions = portions.toIntOrNull()?.coerceIn(1, 20) ?: item.portions,
                    durationMinutes = duration.toIntOrNull() ?: 0)
                val found = runCatching { store.agendaConflicts(profileKey, changed) }.getOrDefault(emptyList())
                if (found.isNotEmpty() && conflicts.isEmpty()) conflicts = found
                else {
                    val saved = runCatching { store.upsertAgenda(profileKey, changed, allowTimeConflict = conflicts.isNotEmpty()) }
                    message = saved.fold({ editId = null; "安排已更新。" }, { "保存失败，原安排保留。" })
                }
            }
        }) { Text(if (conflicts.isEmpty()) "保存" else "仍然保存") } }, dismissButton = { TextButton(onClick = { editId = null }) { Text("取消") } })
    }
    document.agenda.firstOrNull { it.id == moveId }?.let { item ->
        var target by remember(item) { mutableStateOf(date.plusDays(1).toString()) }
        var conflicts by remember(item) { mutableStateOf(emptyList<DailyAgendaItem>()) }
        AlertDialog(
            onDismissRequest = { moveId = null }, title = { Text("延期安排") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("原安排将移到目标日期并保留原份数、备注；原日期不留副本。")
                OutlinedTextField(target, { target = it.take(10); conflicts = emptyList() }, label = { Text("目标日期 YYYY-MM-DD") })
                if (conflicts.isNotEmpty()) Text("目标时段已有：${conflicts.joinToString("、") { it.title }}。再次确认可保留重叠安排。", color = MaterialTheme.colorScheme.error)
            } },
            confirmButton = { TextButton(enabled = runCatching { LocalDate.parse(target) }.isSuccess, onClick = {
                scope.launch {
                    val candidate = item.copy(dateKey = target)
                    val found = runCatching { store.agendaConflicts(profileKey, candidate) }.getOrDefault(emptyList())
                    if (found.isNotEmpty() && conflicts.isEmpty()) conflicts = found
                    else {
                        val moved = runCatching { store.moveAgenda(profileKey, item.id, target, allowTimeConflict = conflicts.isNotEmpty()) }
                        message = moved.fold({ moveId = null; "已延期到 $target。" }, { "延期失败，原安排保留。" })
                    }
                }
            }) { Text(if (conflicts.isEmpty()) "延期" else "仍然延期") } },
            dismissButton = { TextButton(onClick = { moveId = null }) { Text("取消") } }
        )
    }
    copyMealsTarget?.let { initialTarget ->
        var target by remember(initialTarget) { mutableStateOf(initialTarget) }
        AlertDialog(
            onDismissRequest = { copyMealsTarget = null },
            title = { Text("复制餐单到其他日期") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("只复制当天已保存且未完成的餐食；不会覆盖目标日既有安排。")
                OutlinedTextField(target, { target = it.take(10) }, label = { Text("目标日期 YYYY-MM-DD") })
            } },
            confirmButton = { TextButton(enabled = runCatching { LocalDate.parse(target) }.isSuccess, onClick = {
                scope.launch {
                    val result = runCatching { store.copyAgendaDate(profileKey, dateKey, target, onlyMeals = true) }
                    message = result.fold(
                        { copied -> if (copied.conflicts.isNotEmpty()) "目标日时段冲突：${copied.conflicts.joinToString("、") { it.title }}；未复制任何餐食。"
                            else "已复制 ${copied.copied.size} 项餐食，份数和备注已保留。" },
                        { "复制失败，原安排保留。" }
                    )
                    copyMealsTarget = null
                }
            }) { Text("加入目标日") } },
            dismissButton = { TextButton(onClick = { copyMealsTarget = null }) { Text("取消") } }
        )
    }
    document.agenda.firstOrNull { it.id == repeatId }?.let { item ->
        AgendaRepeatDialog(item, store, profileKey, onDismiss = { repeatId = null }, onResult = {
            message = it; repeatId = null
        })
    }
    moveDay?.let { source ->
        AgendaBatchMoveDialog(store, profileKey, document, source, onDismiss = { moveDay = null }, onResult = {
            message = it; moveDay = null
        })
    }
}

@Composable
internal fun ReminderPreferencesEditor(preferences: ReminderPreferences, onSave: (ReminderPreferences) -> Unit) {
    var value by remember(preferences) { mutableStateOf(preferences) }
    var time by remember(preferences) { mutableStateOf("%02d:%02d".format(preferences.hour, preferences.minute)) }
    Text("安排与结果提醒", style = MaterialTheme.typography.titleMedium)
    Row { Checkbox(value.enabled, { value = value.copy(enabled = it) }); Text("开启提醒") }
    OutlinedTextField(time, { time = it.take(5) }, label = { Text("提醒时间 HH:mm") })
    Row(Modifier.horizontalScroll(rememberScrollState())) { (1..7).forEach { day -> FilterChip(day in value.weekdays, onClick = {
        value = value.copy(weekdays = if (day in value.weekdays) value.weekdays - day else value.weekdays + day)
    }, label = { Text(listOf("一", "二", "三", "四", "五", "六", "日")[day - 1]) }) } }
    Row { Checkbox(value.agenda, { value = value.copy(agenda = it) }); Text("今日安排")
        Checkbox(value.savedResults, { value = value.copy(savedResults = it) }); Text("已保存结果") }
    Row { Checkbox(value.pantry, { value = value.copy(pantry = it) }, modifier = Modifier.testTag("reminder-pantry")); Text("库存手填期限（3 天内或已超过，默认关闭）") }
    Text("提醒可能因系统省电与调度延迟，不保证精确到分钟；安排通知可直接标记完成或约 1 小时后再提醒。", style = MaterialTheme.typography.bodySmall)
    val parsed = runCatching { LocalTime.parse(time) }.getOrNull()
    OutlinedButton(enabled = parsed != null && (!value.enabled || (value.weekdays.isNotEmpty() && (value.agenda || value.savedResults || value.pantry))), onClick = {
        parsed?.let { onSave(value.copy(hour = it.hour, minute = it.minute)) }
    }) { Text("保存提醒设置") }
}
