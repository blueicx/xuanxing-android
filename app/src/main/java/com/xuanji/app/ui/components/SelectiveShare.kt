package com.xuanji.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

data class ShareFieldSelection(
    val includeHeadline: Boolean = false,
    val includeSummary: Boolean = true,
    val rowIndices: Set<Int> = emptySet()
)

fun ShareCard.selectFields(selection: ShareFieldSelection): ShareCard = copy(
    badge = if (selection.includeHeadline) badge else "",
    headline = if (selection.includeHeadline) headline else "",
    headlineUnit = if (selection.includeHeadline) headlineUnit else "",
    summary = if (selection.includeSummary) summary else "",
    rows = rows.filterIndexed { index, _ -> index in selection.rowIndices }
)

@Composable
internal fun SelectiveShareDialog(card: ShareCard, onDismiss: () -> Unit, onShare: (ShareCard) -> Unit) {
    var selection by remember(card) { mutableStateOf(ShareFieldSelection()) }
    val preview = card.selectFields(selection)
    AlertDialog(onDismissRequest = onDismiss, title = { Text("选择分享内容") }, text = {
        Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
            Text("只分享下列字段，不额外附带原始档案、聊天或长期记忆；命盘卡本身可能含出生推导信息，请先核对预览。")
            Row { Checkbox(selection.includeHeadline, { selection = selection.copy(includeHeadline = it) }, Modifier.testTag("share-headline")); Text("主结果 / 分数") }
            Row { Checkbox(selection.includeSummary, { selection = selection.copy(includeSummary = it) }, Modifier.testTag("share-summary")); Text("解说摘要") }
            card.rows.forEachIndexed { index, row ->
                Row { Checkbox(index in selection.rowIndices, { checked -> selection = selection.copy(rowIndices =
                    if (checked) selection.rowIndices + index else selection.rowIndices - index) }, Modifier.testTag("share-row-$index")); Text(row.first) }
            }
            Text("将生成的图片内容", style = MaterialTheme.typography.titleSmall)
            Text(preview.title)
            if (preview.headline.isNotBlank()) Text("${preview.headline} ${preview.headlineUnit}")
            if (preview.summary.isNotBlank()) Text(preview.summary)
            preview.rows.forEach { Text("${it.first}：${it.second}") }
            Text("预览仅包含当前选中的字段；分享会打开系统选择器。", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = {
        TextButton(onClick = { onShare(preview) }, enabled = preview.summary.isNotBlank() || preview.headline.isNotBlank() || preview.rows.isNotEmpty(),
            modifier = Modifier.testTag("share-confirm")) { Text("生成图片并分享") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}
