package com.xuanji.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xuanji.app.data.local.DailyLifeStore
import com.xuanji.app.domain.DailyLifeDocument
import kotlinx.coroutines.launch

@Composable
fun DailyLifePrivacyPanel(store: DailyLifeStore, profileKey: String?) {
    if (profileKey.isNullOrBlank()) return
    val revision by store.revision.collectAsState()
    var document by remember(profileKey) { mutableStateOf(DailyLifeDocument()) }
    var clearSection by remember(profileKey) { mutableStateOf<String?>(null) }
    var status by remember(profileKey) { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    LaunchedEffect(profileKey, revision) { document = store.load(profileKey) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("安排与结果数据", style = MaterialTheme.typography.titleMedium)
        Text("当前档案：${document.agenda.size} 条安排、${document.agenda.count { it.note.isNotBlank() }} 条备注、${document.pantry.size} 种库存食材、${document.snapshots.size} 份原始快照；提醒${if (document.reminders.enabled) "开启" else "关闭"}。不包含聊天。")
        Text("另有 ${document.shoppingPurchases.size} 条采购记录、${if (document.weeklyMenuDraft == null) 0 else 1} 份餐单草稿。库存期限/数量均为手填。")
        Text("主动日记 ${document.journal.size} 天；不包含自动推断的心情。清除备注也清除日记。")
        listOf("安排", "备注", "主动日记", "库存", "采购记录", "餐单草稿", "结果快照", "提醒", "以上全部").forEach { section ->
            TextButton(onClick = { clearSection = section }) { Text("清除$section") }
        }
        if (status.isNotBlank()) Text(status)
    }
    clearSection?.let { section -> AlertDialog(
        onDismissRequest = { clearSection = null }, title = { Text("清除当前档案的$section？") },
        text = { Text("仅影响当前档案；其他档案与出生资料保留。此操作不能撤销。清除安排也清除相关采购记录和餐单草稿，库存另行清除。清除采购记录不会倒扣库存；清除提醒会取消后续通知。") },
        confirmButton = { TextButton(onClick = { scope.launch {
            status = runCatching {
                if (section == "以上全部") store.clear(profileKey)
                else store.clearSections(profileKey, agenda = section == "安排", notes = section == "备注", snapshots = section == "结果快照", reminders = section == "提醒", pantry = section == "库存",
                    purchases = section == "采购记录", weeklyDraft = section == "餐单草稿", journal = section == "主动日记")
            }.fold({ "已清除$section。" }, { "清除失败，未完成。" })
            clearSection = null
        } }) { Text("确认清除") } }, dismissButton = { TextButton(onClick = { clearSection = null }) { Text("取消") } }
    ) }
}
