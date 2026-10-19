package com.xuanji.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.xuanji.app.data.local.MysticCharacterSessionStore
import com.xuanji.app.data.local.PreferenceBridge
import com.xuanji.app.domain.*
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test

class MysticInteractionRestoreTest {
    @get:Rule val compose = createComposeRule()

    @Test fun explicit_conditions_and_evidence_restore_through_real_reducer_codec_and_store() {
        val values = mutableMapOf<String, String>()
        val bridge = object : PreferenceBridge {
            override suspend fun read(key: String) = values[key]
            override suspend fun write(key: String, value: String) { values[key] = value }
            override suspend fun delete(key: String) { values.remove(key) }
        }
        compose.setContent {
            var state by remember { mutableStateOf(MysticSessionState(sessionToken = 9)) }
            var restored by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
            fun current() = state.interaction.forDate("2026-10-13")
            fun update(value: MysticInteractionState) {
                state = reduce(state, MysticEvent.UpdateInteraction(state.sessionToken, value))
            }
            var conditions by MysticInteractionProperty(read = { current().conditions }, write = { update(current().copy(conditions = it)) })
            var evidence by MysticInteractionProperty(read = { current().evidenceOpen }, write = { update(current().copy(evidenceOpen = it)) })
            Column {
                Button(onClick = { conditions = TemporaryActionConditions.parse("只有十五分钟，不要豆腐") }) { Text("设置本次条件") }
                Button(onClick = { evidence = !evidence }) { Text("展开依据") }
                Text("时限 ${conditions.maxMinutes ?: 0} · 排除 ${conditions.excludedIngredients.joinToString()} · 依据 $evidence")
                Button(onClick = { scope.launch {
                    val snapshot = MysticCharacterSessionState.initial(MysticCharacterId.ShenYanzhou)
                        .copy(sessions = mapOf(MysticCharacterId.ShenYanzhou to state))
                    MysticCharacterSessionStore(bridge).save("isolated-synthetic-profile", snapshot)
                    state = MysticSessionState()
                    state = requireNotNull(MysticCharacterSessionStore(bridge).load("isolated-synthetic-profile")).session(MysticCharacterId.ShenYanzhou)
                    restored = true
                } }) { Text("关闭并恢复会话") }
                if (restored) Text("已从本机测试存储恢复")
            }
        }
        compose.onNodeWithText("设置本次条件").performClick()
        compose.onNodeWithText("展开依据").performClick()
        compose.onNodeWithText("时限 15 · 排除 豆腐 · 依据 true").assertExists()
        compose.onNodeWithText("关闭并恢复会话").performClick()
        compose.onNodeWithText("已从本机测试存储恢复").assertExists()
        compose.onNodeWithText("时限 15 · 排除 豆腐 · 依据 true").assertExists()
    }
}
