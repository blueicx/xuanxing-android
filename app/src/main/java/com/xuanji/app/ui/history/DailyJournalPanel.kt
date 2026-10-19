package com.xuanji.app.ui.history

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.xuanji.app.data.local.DailyLifeStore
import com.xuanji.app.domain.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.launch

@Composable
fun DailyJournalPanel(store: DailyLifeStore, profileKey: String, date: LocalDate, document: DailyLifeDocument) {
    val entry = document.journal.firstOrNull { it.dateKey == date.toString() }
    var editing by rememberSaveable(profileKey, date.toString()) { mutableStateOf(false) }
    var text by rememberSaveable(profileKey, date.toString(), entry) { mutableStateOf(entry?.text.orEmpty()) }
    var mood by rememberSaveable(profileKey, date.toString(), entry) { mutableStateOf(entry?.mood.orEmpty()) }
    var execution by rememberSaveable(profileKey, date.toString(), entry) { mutableStateOf(entry?.actualExecution.orEmpty()) }
    var monthReview by rememberSaveable(profileKey) { mutableStateOf(false) }
    var message by remember(profileKey, date) { mutableStateOf("") }
    var deleting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Card(Modifier.fillMaxWidth().testTag("daily-journal")) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("主动记录与回顾", style = MaterialTheme.typography.titleMedium)
            Text("只保存你主动填写的内容，不从运势或聊天推断心情，不分析人格或因果。", style = MaterialTheme.typography.bodySmall)
            entry?.let {
                if (it.mood.isNotBlank()) Text("所选心情：${it.mood}")
                if (it.text.isNotBlank()) Text(it.text)
                if (it.actualExecution.isNotBlank()) Text("实际执行：${it.actualExecution}")
            }
            TextButton(onClick = { editing = !editing }, modifier = Modifier.testTag("journal-edit")) { Text(if (editing) "收起记录编辑" else "记录/编辑这一天") }
            if (editing && document.readable) {
                OutlinedTextField(text, { text = it.take(4000) }, Modifier.fillMaxWidth().testTag("journal-text"), label = { Text("日记（最多 4000 字）") })
                Text("心情可不选，留空不作推断。")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (listOf("") + DailyJournalRules.moods).forEach { choice ->
                        FilterChip(selected = mood == choice, onClick = { mood = choice }, label = { Text(choice.ifBlank { "未选择" }) })
                    }
                }
                OutlinedTextField(execution, { execution = it.take(1000) }, Modifier.fillMaxWidth().testTag("journal-execution"), label = { Text("实际做了什么（不自动勾选安排）") })
                Button(onClick = { scope.launch {
                    message = runCatching { store.saveJournal(profileKey, DailyJournalEntry(date.toString(), text, mood, execution, System.currentTimeMillis())) }
                        .fold({ editing = false; "已保存主动记录。" }, { "保存失败或已达到 366 天上限；原记录保留。" })
                } }, modifier = Modifier.testTag("journal-save")) { Text("保存主动记录") }
            }
            if (entry != null) TextButton(onClick = { deleting = true }, modifier = Modifier.testTag("journal-delete")) { Text("删除当天记录") }
            Row {
                FilterChip(!monthReview, { monthReview = false }, { Text("本周回顾") })
                Spacer(Modifier.width(8.dp))
                FilterChip(monthReview, { monthReview = true }, { Text("本月回顾") })
            }
            val start = if (monthReview) date.withDayOfMonth(1) else date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val end = if (monthReview) start.plusMonths(1).minusDays(1) else start.plusDays(6)
            if (document.readable) {
                val review = DailyJournalRules.review(document, start, end)
                Text("$start 至 $end：记录 ${review.recordedDays} 天；安排 ${review.completedAgenda}/${review.totalAgenda} 项已勾选完成。", Modifier.testTag("journal-review"))
                Text("主动选择的心情次数：${review.moods.entries.joinToString("、") { "${it.key} ${it.value}" }.ifBlank { "尚无选择" }}。未记录不等于未行动；不对心情求平均或诊断。")
            }
            if (message.isNotBlank()) Text(message)
        }
    }
    if (deleting) AlertDialog(onDismissRequest = { deleting = false }, title = { Text("删除当天主动记录？") },
        text = { Text("只删除当前档案这一天的日记、心情和实际执行，不影响安排和其他日期。无法撤销。") },
        confirmButton = { TextButton(onClick = { scope.launch {
            message = runCatching { store.deleteJournal(profileKey, date.toString()) }.fold({ "已删除。" }, { "删除失败。" })
            deleting = false
        } }) { Text("确认删除") } }, dismissButton = { TextButton(onClick = { deleting = false }) { Text("取消") } })
}
