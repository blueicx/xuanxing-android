package com.xuanji.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.SoftMemorySource
import com.xuanji.app.domain.SoftMemoryTag
import java.time.LocalDate

@Composable
fun MysticSoftMemoryPanel(
    tags: List<SoftMemoryTag>,
    onRevoke: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    showExpired: Boolean = false,
    editable: Boolean = false,
    onEdit: (SoftMemoryTag) -> Unit = {}
) {
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    val visible = tags.filterNot { it.revoked }.filter { showExpired || !it.isExpiredOn() }
    if (visible.isEmpty()) return
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("本地软记忆", style = MaterialTheme.typography.labelLarge)
            visible.forEach { tag ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("${tag.label}：${tag.value}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            if (tag.source == SoftMemorySource.Inferred) "由对话推断" else "你已确认",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            when {
                                tag.isExpiredOn() -> "已过期 · 不再作为有效提示"
                                tag.expiresAtDate == null -> "无期限 · 可随时撤回"
                                else -> "有效至 ${tag.expiresAtDate}"
                            }, style = MaterialTheme.typography.labelSmall,
                            color = if (tag.isExpiredOn()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (editable) TextButton(onClick = { editingId = tag.id }) { Text("编辑") }
                    OutlinedButton(
                        onClick = { onRevoke(tag.id) },
                        modifier = Modifier.semantics { contentDescription = "撤回软标签：${tag.value}" }
                    ) { Text("撤回") }
                }
            }
            OutlinedButton(
                onClick = onClear,
                modifier = Modifier.semantics { contentDescription = "清除全部软标签" }
            ) { Text("清除全部软标签") }
        }
    }
    visible.firstOrNull { it.id == editingId }?.let { tag ->
        var label by rememberSaveable(tag) { mutableStateOf(tag.label) }
        var value by rememberSaveable(tag) { mutableStateOf(tag.value) }
        val today = LocalDate.now()
        var lifetime by rememberSaveable(tag) { mutableStateOf("keep") }
        AlertDialog(
            onDismissRequest = { editingId = null },
            title = { Text("编辑本地记忆") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(label, { label = it.take(80) }, label = { Text("显示名称") }, singleLine = true)
                OutlinedTextField(value, { value = it.take(80) }, label = { Text("内容") }, singleLine = true)
                Text("有效期", style = MaterialTheme.typography.labelLarge)
                FilterChip(selected = lifetime == "keep", onClick = { lifetime = "keep" }, label = {
                    Text("保留原有效期：${tag.expiresAtDate ?: "无期限"}")
                })
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("7" to "7 天", "30" to "30 天", "90" to "90 天", "never" to "无期限").forEach { (key, title) ->
                        FilterChip(selected = lifetime == key, onClick = { lifetime = key }, label = { Text(title) })
                    }
                }
            } },
            confirmButton = { TextButton(enabled = label.isNotBlank() && value.isNotBlank(), onClick = {
                val days = lifetime.toLongOrNull()
                val edited = tag.copy(label = label.trim(), value = value.trim())
                onEdit(if (lifetime == "keep") edited else edited.withLifetime(days, today))
                editingId = null
            }) { Text("保存") } },
            dismissButton = { TextButton(onClick = { editingId = null }) { Text("取消") } }
        )
    }
}
