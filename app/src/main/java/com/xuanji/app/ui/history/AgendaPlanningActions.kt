package com.xuanji.app.ui.history

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.xuanji.app.data.local.DailyLifeStore
import com.xuanji.app.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate

@Composable
internal fun AgendaBatchMoveDialog(
    store: DailyLifeStore, profileKey: String, document: DailyLifeDocument, sourceDate: String,
    onDismiss: () -> Unit, onResult: (String) -> Unit
) {
    var target by rememberSaveable(sourceDate) { mutableStateOf(LocalDate.parse(sourceDate).plusDays(1).toString()) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val source = document.agenda.filter { it.dateKey == sourceDate && !it.completed }
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() }, title = { Text("当天待办整体延期") }, text = {
        Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("移动 $sourceDate 的 ${source.size} 项未完成安排；保留 ID、时间、时长、份数与备注，原日期不留副本。已完成项不移动。任一目标时段冲突则整批不写入。")
            OutlinedTextField(target, { target = it.take(10) }, label = { Text("目标日期 YYYY-MM-DD") })
            source.forEach { Text("${it.time.ifBlank { "时间未定" }} · ${it.title}") }
        }
    }, confirmButton = { TextButton(enabled = !saving && source.isNotEmpty() && target != sourceDate && runCatching { LocalDate.parse(target) }.isSuccess, onClick = {
        saving = true
        scope.launch {
            onResult(runCatching { store.moveAgendaDate(profileKey, sourceDate, target) }.fold({
                if (it.conflicts.isNotEmpty()) "目标时段冲突：${it.conflicts.joinToString("、") { item -> item.title }}；未移动任何安排。"
                else "已移动 ${it.copied.size} 项到 $target；原已完成项保留。"
            }, { "延期失败，原安排保留。" }))
        }
    }) { Text(if (saving) "移动中…" else "确认整体延期") } }, dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("取消") } })
}

@Composable
internal fun AgendaRepeatDialog(
    source: DailyAgendaItem, store: DailyLifeStore, profileKey: String,
    onDismiss: () -> Unit, onResult: (String) -> Unit
) {
    var weeks by rememberSaveable(source.id) { mutableStateOf("2") }
    var start by rememberSaveable(source.id) { mutableStateOf(source.dateKey) }
    var days by rememberSaveable(source.id) { mutableStateOf("1,2,3,4,5") }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val weekdays = days.split(',').mapNotNull { it.toIntOrNull() }.toSet()
    val preview = runCatching { AgendaPlanning.repeat(source, LocalDate.parse(start), weeks.toInt(), weekdays) }.getOrNull()
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() }, title = { Text("重复 · ${source.title}") }, text = {
        Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("仅在确认后批量生成独立安排；修改或删除其中一天不会影响其他日期。不会后台无限续排。")
            OutlinedTextField(start, { start = it.take(10) }, label = { Text("开始日期 YYYY-MM-DD") })
            OutlinedTextField(weeks, { weeks = it.filter(Char::isDigit).take(2) }, label = { Text("周数（1–12）") }, modifier = Modifier.testTag("repeat-weeks"))
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                (1..7).forEach { day -> FilterChip(day in weekdays, onClick = {
                    days = (if (day in weekdays) weekdays - day else weekdays + day).sorted().joinToString(",")
                }, label = { Text(listOf("一", "二", "三", "四", "五", "六", "日")[day - 1]) }) }
            }
            Text("预览：${preview?.size ?: 0} 项（不复制原日期；已有重复项不会覆盖）", modifier = Modifier.testTag("repeat-preview"))
            preview?.forEach { Text("${it.dateKey} · ${it.time.ifBlank { "时间未定" }} · ${it.title}") }
            if (preview == null) Text("请选择有效日期、周数及至少一个星期。")
        }
    }, confirmButton = {
        TextButton(enabled = !saving && !preview.isNullOrEmpty(), modifier = Modifier.testTag("repeat-confirm"), onClick = {
            val batch = preview ?: return@TextButton
            saving = true
            scope.launch {
                val result = runCatching { store.addAgendaBatch(profileKey, batch) }
                onResult(result.fold({
                    if (it.conflicts.isNotEmpty()) "时段冲突：${it.conflicts.joinToString("、") { item -> item.title }}；未加入任何重复安排。"
                    else "已加入 ${it.copied.size} 项安排；已有日期记录未覆盖。"
                }, { "保存失败，原安排保留；请检查容量或本地数据。" }))
            }
        }) { Text(if (saving) "保存中…" else "确认批量加入") }
    }, dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("取消") } })
}

@Composable
internal fun AgendaCalendarExportAction(document: DailyLifeDocument, date: LocalDate) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var show by rememberSaveable { mutableStateOf(false) }
    var from by rememberSaveable(date) { mutableStateOf(date.toString()) }
    var through by rememberSaveable(date) { mutableStateOf(date.plusDays(6).toString()) }
    var notes by rememberSaveable { mutableStateOf(false) }
    var pending by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/calendar")) { uri ->
        val content = pending
        pending = null
        if (uri == null) status = "已取消导出。"
        else if (content == null) status = "导出内容已失效，请重新选择；未写入文件。"
        else scope.launch {
            status = runCatching { withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(content) }
                    ?: error("cannot_open_document")
            } }.fold({ "已导出所选安排。导入日历后需在日历应用管理该副本。" }, { "导出失败，请检查目标文件。" })
        }
    }
    OutlinedButton(enabled = document.readable && document.agenda.isNotEmpty(), onClick = { show = true }, modifier = Modifier.testTag("agenda-export")) { Text("导出安排到日历文件") }
    status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    if (show) {
        val first = runCatching { LocalDate.parse(from) }.getOrNull()
        val last = runCatching { LocalDate.parse(through) }.getOrNull()
        val valid = first != null && last != null && last >= first
        val count = if (valid) document.agenda.count { LocalDate.parse(it.dateKey) in first!!..last!! } else 0
        AlertDialog(onDismissRequest = { show = false }, title = { Text("导出安排（.ics）") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("仅导出下列日期范围的 $count 项安排。标题会写入文件；不包含出生资料、聊天、库存和运势快照。时间按本地钟面保存，跨时区导入请自行核对。")
                OutlinedTextField(from, { from = it.take(10) }, label = { Text("开始 YYYY-MM-DD") })
                OutlinedTextField(through, { through = it.take(10) }, label = { Text("结束 YYYY-MM-DD") })
                Row { Checkbox(notes, { notes = it }); Text("包含私人备注（默认不导出）") }
                Text("导出是单向副本，不同步修改、不自动设置日历提醒。")
            }
        }, confirmButton = { TextButton(enabled = valid && count > 0, onClick = {
            val content = runCatching { AgendaCalendarExport.encode(document.agenda, first!!, last!!, notes, Instant.now()) }
            content.onSuccess {
                pending = it; show = false
                picker.launch("xuanji-agenda-$from.ics")
            }.onFailure { status = "记录无法导出，原数据保留。" }
        }) { Text("选择保存位置") } }, dismissButton = { TextButton(onClick = { show = false }) { Text("取消") } })
    }
}
