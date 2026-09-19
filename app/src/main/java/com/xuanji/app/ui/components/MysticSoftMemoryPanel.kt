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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.SoftMemorySource
import com.xuanji.app.domain.SoftMemoryTag

@Composable
fun MysticSoftMemoryPanel(
    tags: List<SoftMemoryTag>,
    onRevoke: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visible = tags.filterNot { it.revoked }
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
                    }
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
}
