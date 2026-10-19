package com.xuanji.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.xuanji.app.data.local.DailyLifeStore
import com.xuanji.app.data.local.PreferenceBridge
import com.xuanji.app.domain.DailyAgendaItem
import com.xuanji.app.ui.history.DailyLifePanel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AgendaPlanningInteractionTest {
    @get:Rule val compose = createComposeRule()
    private class Memory : PreferenceBridge {
        val data = mutableMapOf<String, String>()
        override suspend fun read(key: String) = data[key]
        override suspend fun write(key: String, value: String) { data[key] = value }
        override suspend fun delete(key: String) { data.remove(key) }
    }
    @Test fun editing_duration_and_confirming_weekday_repeat_saves_real_occurrences() {
        val store = DailyLifeStore(Memory())
        runBlocking { store.upsertAgenda("synthetic", DailyAgendaItem("a", "2026-10-19", "09:00", "阅读", "custom")) }
        compose.setContent { MaterialTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            DailyLifePanel(store, "synthetic", "2026-10-19")
        } } }
        compose.onNodeWithText("编辑").performScrollTo().performClick()
        compose.onNodeWithTag("agenda-edit-duration").performTextReplacement("45")
        compose.onNodeWithText("保存", useUnmergedTree = true).performClick()
        compose.waitForIdle()
        runBlocking { assertEquals(45, store.load("synthetic").agenda.single().durationMinutes) }
        compose.onNodeWithTag("agenda-repeat-a").performScrollTo().performClick()
        compose.onNodeWithTag("repeat-weeks").performTextReplacement("1")
        compose.onNodeWithTag("repeat-preview").assertExists()
        runBlocking { assertEquals(1, store.load("synthetic").agenda.size) }
        compose.onNodeWithTag("repeat-confirm").performClick()
        compose.waitForIdle()
        runBlocking {
            val saved = store.load("synthetic").agenda
            assertEquals(5, saved.size)
            assertTrue(saved.all { it.durationMinutes == 45 })
            assertTrue(store.load("other").agenda.isEmpty())
        }
    }

    @Test fun repeat_with_one_overlap_does_not_save_partial_days() {
        val store = DailyLifeStore(Memory())
        runBlocking {
            store.upsertAgenda("synthetic", DailyAgendaItem("a", "2026-10-19", "09:00", "阅读", "custom", durationMinutes = 45))
            store.upsertAgenda("synthetic", DailyAgendaItem("busy", "2026-10-20", "09:15", "已有安排", "custom", durationMinutes = 60))
        }
        compose.setContent { MaterialTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            DailyLifePanel(store, "synthetic", "2026-10-19")
        } } }
        compose.onNodeWithTag("agenda-repeat-a").performScrollTo().performClick()
        compose.onNodeWithTag("repeat-weeks").performTextReplacement("1")
        compose.onNodeWithTag("repeat-confirm").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("时段冲突：已有安排；未加入任何重复安排。").assertExists()
        runBlocking { assertEquals(2, store.load("synthetic").agenda.size) }
    }
}
