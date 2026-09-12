package com.xuanji.app.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xuanji.app.domain.MysticIntent
import com.xuanji.app.domain.MysticMessage
import com.xuanji.app.domain.MysticMessageRole
import com.xuanji.app.domain.MysticSessionState
import com.xuanji.app.domain.SoftMemorySource
import com.xuanji.app.domain.SoftMemoryTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MysticConversationPanelTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun clarifier_and_soft_memory_controls_have_accessible_labels() {
        compose.setContent {
            MysticConversationPanel(
                state = MysticSessionState(
                    messages = listOf(
                        MysticMessage(
                            turnId = 1,
                            sessionToken = 0,
                            role = MysticMessageRole.Mystic,
                            text = "先看工作",
                            intent = MysticIntent.Career,
                            clarifiers = listOf("先看工作", "再看感情")
                        )
                    )
                ),
                onSend = {},
                onQuickPrompt = {},
                onCancel = {},
                onRetry = {},
                softMemoryTags = listOf(
                    SoftMemoryTag(
                        id = "work",
                        key = "topic",
                        label = "最近常聊",
                        value = "工作",
                        source = SoftMemorySource.Inferred,
                        createdAt = "2026-09-12",
                        lastSeenAt = "2026-09-12"
                    )
                )
            )
        }
        compose.onNodeWithText("由对话推断").assertExists()
        compose.onNodeWithContentDescription("撤回软标签：工作").assertExists()
        compose.onNodeWithContentDescription("继续追问：再看感情").assertExists()
    }
}
