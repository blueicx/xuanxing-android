package com.xuanji.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.MysticMessage
import com.xuanji.app.domain.MysticMessageRole
import com.xuanji.app.domain.MysticRequestState
import com.xuanji.app.domain.MysticSessionState

private val QUICK_PROMPTS = listOf(
    "今日运势", "继续说", "换个话题", "解释刚才", "我只是想聊聊"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MysticConversationPanel(
    state: MysticSessionState,
    onSend: (String) -> Unit,
    onQuickPrompt: (String) -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    accent: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    placeholder: String = "说点什么，我接得住",
    showMessages: Boolean = true
) {
    var draft by remember { mutableStateOf("") }
    val pending = state.requestState as? MysticRequestState.Pending
    val failed = state.requestState as? MysticRequestState.Failed
    val busy = pending != null
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!showMessages) {
            // The host may render a richer transcript and use this component for controls/status only.
        } else if (state.messages.isEmpty()) {
            Text("想从哪里开始？", style = MaterialTheme.typography.labelMedium, color = accent)
        } else {
            state.messages.takeLast(12).forEach { message ->
                val roleLabel = when (message.role) {
                    MysticMessageRole.User -> "我"
                    MysticMessageRole.Mystic -> "玄师"
                    MysticMessageRole.System -> "系统"
                }
                val color = when (message.role) {
                    MysticMessageRole.User -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
                    MysticMessageRole.Mystic -> accent.copy(alpha = 0.15f)
                    MysticMessageRole.System -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f)
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = color,
                    modifier = Modifier.semantics { contentDescription = "$roleLabel：${message.text}" }
                ) {
                    Text(
                        "$roleLabel：${message.text}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            QUICK_PROMPTS.forEach { prompt ->
                OutlinedButton(
                    onClick = { onQuickPrompt(prompt) },
                    enabled = !busy,
                    modifier = Modifier.semantics { contentDescription = "快捷提问：$prompt" },
                    shape = RoundedCornerShape(999.dp)
                ) { Text(prompt, style = MaterialTheme.typography.labelSmall) }
            }
        }

        if (pending != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("玄师正在接话…", style = MaterialTheme.typography.labelSmall, color = accent,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                OutlinedButton(onClick = onCancel, modifier = Modifier.semantics { contentDescription = "取消回复" }) { Text("取消") }
            }
        }
        if (failed != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("这次没有接上：${failed.message}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error)
                Button(onClick = onRetry, modifier = Modifier.semantics { contentDescription = "重试回复" }) { Text("重试") }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.take(200) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                enabled = !busy,
                placeholder = { Text(placeholder) },
                shape = RoundedCornerShape(14.dp),
                supportingText = { if (draft.length >= 180) Text("${draft.length}/200") }
            )
            Button(
                onClick = { val input = draft.trim(); if (input.isNotEmpty()) { onSend(input); draft = "" } },
                enabled = draft.isNotBlank() && !busy,
                modifier = Modifier.semantics { contentDescription = "发送消息" }
            ) { Text("说") }
        }
    }
}
