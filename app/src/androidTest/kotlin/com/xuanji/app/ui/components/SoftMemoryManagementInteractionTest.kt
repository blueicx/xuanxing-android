package com.xuanji.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.xuanji.app.domain.SoftMemorySource
import com.xuanji.app.domain.SoftMemoryTag
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SoftMemoryManagementInteractionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun changing_text_keeps_expired_date_until_user_selects_a_new_lifetime() {
        val original = SoftMemoryTag("topic-work", "topic", "常聊", "工作", SoftMemorySource.Inferred,
            "2026-01-01", "2026-01-01", expiresAtDate = "2026-01-08")
        val tags = mutableStateOf(listOf(original))
        compose.setContent { MaterialTheme {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                MysticSoftMemoryPanel(tags.value, onRevoke = {}, onClear = {}, showExpired = true, editable = true,
                    onEdit = { tags.value = listOf(it) })
            }
        } }
        compose.onNodeWithText("编辑").performClick()
        compose.onAllNodes(hasSetTextAction())[1].performTextReplacement("学习")
        compose.onNodeWithText("保存").performClick()
        compose.waitForIdle()
        assertEquals("学习", tags.value.single().value)
        assertEquals(original.expiresAtDate, tags.value.single().expiresAtDate)
        compose.onNodeWithText("编辑").performClick()
        compose.onNodeWithText("无期限", useUnmergedTree = true).performClick()
        compose.onNodeWithText("保存").performClick()
        compose.waitForIdle()
        assertNull(tags.value.single().expiresAtDate)
    }

    @Test fun expired_tags_are_hidden_from_conversation_hints() {
        val expired = SoftMemoryTag("topic-work", "topic", "常聊", "工作", SoftMemorySource.Inferred,
            "2026-01-01", "2026-01-01", expiresAtDate = "2026-01-08")
        compose.setContent { MaterialTheme { MysticSoftMemoryPanel(listOf(expired), onRevoke = {}, onClear = {}) } }
        compose.onNodeWithText("常聊：工作").assertDoesNotExist()
    }
}
