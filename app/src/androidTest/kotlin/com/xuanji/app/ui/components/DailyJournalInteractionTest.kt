package com.xuanji.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import com.xuanji.app.data.local.*
import com.xuanji.app.domain.*
import com.xuanji.app.ui.history.DailyJournalPanel
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DailyJournalInteractionTest {
    @get:Rule val compose = createComposeRule()
    @Test fun write_restore_edit_save_review_and_confirm_delete_use_synthetic_records() {
        val values = mutableMapOf<String, String>()
        val store = DailyLifeStore(object : PreferenceBridge {
            override suspend fun read(key: String) = values[key]
            override suspend fun write(key: String, value: String) { values[key] = value }
            override suspend fun delete(key: String) { values.remove(key) }
        })
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MaterialTheme {
            val revision by store.revision.collectAsState()
            var doc by remember { mutableStateOf(DailyLifeDocument()) }
            LaunchedEffect(revision) { doc = store.load("synthetic") }
            Column(Modifier.verticalScroll(rememberScrollState())) { DailyJournalPanel(store, "synthetic", LocalDate.of(2026, 10, 19), doc) }
        } }
        compose.onNodeWithTag("journal-edit").performScrollTo().performClick()
        compose.onNodeWithTag("journal-text").performTextInput("主动写下一天")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("journal-text").assertTextContains("主动写下一天")
        compose.onNodeWithText("平静", useUnmergedTree = true).performScrollTo().performClick()
        compose.onNodeWithTag("journal-execution").performScrollTo().performTextInput("实际散步十分钟")
        compose.onNodeWithTag("journal-save").performScrollTo().performClick()
        compose.waitForIdle()
        runBlocking {
            assertEquals("主动写下一天", store.load("synthetic").journal.single().text)
            assertEquals("平静", store.load("synthetic").journal.single().mood)
            assertEquals("实际散步十分钟", store.load("synthetic").journal.single().actualExecution)
            assertTrue(store.load("other").journal.isEmpty())
        }
        compose.onNodeWithTag("journal-review").performScrollTo().assertTextContains("2026-10-19 至 2026-10-25：记录 1 天；安排 0/0 项已勾选完成。")
        compose.onNodeWithTag("journal-delete").performScrollTo().performClick()
        compose.onNodeWithText("确认删除").performClick()
        compose.waitForIdle()
        runBlocking { assertTrue(store.load("synthetic").journal.isEmpty()) }
    }
}
