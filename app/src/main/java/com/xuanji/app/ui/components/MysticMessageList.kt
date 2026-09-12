package com.xuanji.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.MysticMessage
import com.xuanji.app.domain.MysticMessageRole

object MysticMessageListModel {
    const val MAX_VISIBLE_MESSAGES = 12

    fun visibleMessages(messages: List<MysticMessage>): List<MysticMessage> =
        messages.takeLast(MAX_VISIBLE_MESSAGES)
}

@Composable
fun MysticMessageList(
    messages: List<MysticMessage>,
    accent: Color,
    busy: Boolean,
    onClarifierSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        MysticMessageListModel.visibleMessages(messages).forEach { message ->
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
            if (message.role == MysticMessageRole.Mystic && message.clarifiers.isNotEmpty()) {
                MysticClarifierRow(
                    options = message.clarifiers,
                    onSelected = onClarifierSelected,
                    enabled = !busy
                )
            }
        }
    }
}
