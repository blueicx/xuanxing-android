package com.xuanji.app.ui.components

import com.xuanji.app.domain.MysticMessage
import com.xuanji.app.domain.MysticMessageRole
import kotlin.test.Test
import kotlin.test.assertEquals

class MysticConversationUiModelTest {
    @Test
    fun visibleMessagesKeepsOnlyRecentTranscript() {
        val messages = (0..14).map { index ->
            MysticMessage(index.toLong(), 1L, MysticMessageRole.User, "消息$index")
        }

        val visible = MysticMessageListModel.visibleMessages(messages)

        assertEquals(12, visible.size)
        assertEquals("消息3", visible.first().text)
        assertEquals("消息14", visible.last().text)
    }

    @Test
    fun inputStateTrimsAndCapsDraft() {
        val state = MysticInputState.fromDraft("  ${"a".repeat(240)}  ")

        assertEquals(200, state.text.length)
        assertEquals(200, state.text.length)
        assertEquals(200, state.maxLength)
    }
}
