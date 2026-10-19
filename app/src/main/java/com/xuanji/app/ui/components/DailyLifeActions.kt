package com.xuanji.app.ui.components

import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.xuanji.app.data.local.DailyLifeStore
import com.xuanji.app.domain.*
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.util.UUID

val LocalDailyLifeStore = staticCompositionLocalOf<DailyLifeStore?> { null }
val LocalDailyLifeProfileKey = staticCompositionLocalOf<String?> { null }

@Composable
internal fun AddToAgendaAction(
    title: String,
    category: String,
    dateKey: String,
    ingredients: List<String>,
    candidateKey: String,
    estimatedPriceCentsPerPortion: Int? = null
) {
    val store = LocalDailyLifeStore.current ?: return
    val key = LocalDailyLifeProfileKey.current ?: return
    val scope = rememberCoroutineScope()
    var editing by rememberSaveable(key, dateKey, title, candidateKey) { mutableStateOf(false) }
    var time by rememberSaveable(key, dateKey, title, candidateKey) { mutableStateOf("12:00") }
    var note by rememberSaveable(key, dateKey, title, candidateKey) { mutableStateOf("") }
    var portions by rememberSaveable(key, dateKey, title, candidateKey) { mutableStateOf("1") }
    var duration by rememberSaveable(key, dateKey, title, candidateKey) { mutableStateOf("") }
    var conflicts by remember { mutableStateOf(emptyList<DailyAgendaItem>()) }
    var status by remember { mutableStateOf<String?>(null) }
    OutlinedButton(onClick = { editing = true }, modifier = Modifier.testTag("add-to-agenda")) { Text("加入今日安排") }
    status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    if (editing) AlertDialog(
        onDismissRequest = { editing = false }, title = { Text("加入安排 · $title") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(time, { time = it.take(5); conflicts = emptyList() }, label = { Text("时间 HH:mm，可留空") }, modifier = Modifier.testTag("agenda-time"))
            OutlinedTextField(duration, { duration = it.filter(Char::isDigit).take(4); conflicts = emptyList() },
                label = { Text("持续分钟（留空为未知，最多 1440）") }, modifier = Modifier.testTag("agenda-duration"))
            OutlinedTextField(note, { note = it.take(2000) }, label = { Text("备注") }, modifier = Modifier.testTag("agenda-note"))
            if (category == "meal") OutlinedTextField(
                portions, { portions = it.filter(Char::isDigit).take(2) }, label = { Text("用餐份数（1–20）") }, modifier = Modifier.testTag("agenda-portions")
            )
            if (conflicts.isNotEmpty()) Text(
                "这个时段已有：${conflicts.joinToString("、") { it.title }}。可以改时间，或再次点击确认保留重叠安排。",
                color = MaterialTheme.colorScheme.error
            )
        } },
        confirmButton = { TextButton(
            enabled = (time.isBlank() || runCatching { LocalTime.parse(time) }.isSuccess) &&
                (duration.isBlank() || duration.toIntOrNull()?.let { it in 1..1440 } == true) &&
                (category != "meal" || portions.toIntOrNull()?.let { it in 1..20 } == true),
            modifier = Modifier.testTag("agenda-confirm"), onClick = {
                scope.launch {
                    val item = DailyAgendaItem(
                        UUID.randomUUID().toString(), dateKey, time, title, category, ingredients, candidateKey, note,
                        portions = portions.toIntOrNull()?.coerceIn(1, 20) ?: 1,
                        estimatedPriceCentsPerPortion = estimatedPriceCentsPerPortion,
                        durationMinutes = duration.toIntOrNull() ?: 0
                    )
                    val found = runCatching { store.agendaConflicts(key, item) }.getOrElse { emptyList() }
                    if (found.isNotEmpty() && conflicts.isEmpty()) conflicts = found
                    else status = runCatching { store.upsertAgenda(key, item, allowTimeConflict = conflicts.isNotEmpty()) }
                        .fold({ editing = false; conflicts = emptyList(); "已加入 $dateKey 的安排" }, { "保存失败，原安排保留。" })
                }
            }) { Text(if (conflicts.isEmpty()) "保存安排" else "仍然保存") } }, dismissButton = { TextButton(onClick = { editing = false; conflicts = emptyList() }) { Text("取消") } }
    )
}

@Composable
internal fun SaveReadingSnapshotAction(detail: MysticDetailContext) {
    if (runCatching { java.time.LocalDate.parse(detail.dateKey) }.isFailure) return
    val store = LocalDailyLifeStore.current ?: return
    val key = LocalDailyLifeProfileKey.current ?: return
    val scope = rememberCoroutineScope()
    var status by remember(detail) { mutableStateOf<String?>(null) }
    OutlinedButton(modifier = Modifier.testTag("save-reading-snapshot"), onClick = {
        scope.launch {
            status = runCatching { store.saveSnapshot(key, ReadingSnapshot(
                ReadingFavoriteCodec.stableId("snapshot", detail), detail.dateKey, detail.period, detail.mode,
                detail.result, detail.evidence, detail.algorithmVersion, detail.title
            )) }.fold({ "已保存原始结果到记录日历" }, { "保存失败，请检查日期与本地空间。" })
        }
    }) { Text("保存当前结果") }
    status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
}
