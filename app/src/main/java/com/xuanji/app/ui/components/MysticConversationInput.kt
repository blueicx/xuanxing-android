package com.xuanji.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

data class MysticInputState(val text: String, val maxLength: Int = 200) {
    fun update(value: String): MysticInputState = copy(text = value.take(maxLength))

    companion object {
        fun fromDraft(value: String, maxLength: Int = 200): MysticInputState =
            MysticInputState(value.trim().take(maxLength), maxLength)
    }
}

@Composable
fun MysticConversationInput(
    onSend: (String) -> Unit,
    busy: Boolean,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    var inputState by remember { mutableStateOf(MysticInputState("")) }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = inputState.text,
            onValueChange = { inputState = inputState.update(it) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            enabled = !busy,
            placeholder = { Text(placeholder) },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            supportingText = {
                if (inputState.text.length >= inputState.maxLength - 20) {
                    Text("${inputState.text.length}/${inputState.maxLength}")
                }
            }
        )
        Button(
            onClick = {
                val input = inputState.text.trim()
                if (input.isNotEmpty()) {
                    onSend(input)
                    inputState = MysticInputState("")
                }
            },
            enabled = inputState.text.isNotBlank() && !busy,
            modifier = Modifier.semantics { contentDescription = "发送消息" }
        ) { Text("说") }
    }
}
