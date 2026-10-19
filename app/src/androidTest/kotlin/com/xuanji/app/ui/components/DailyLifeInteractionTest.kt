package com.xuanji.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.xuanji.app.data.local.*
import com.xuanji.app.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DailyLifeInteractionTest {
    @get:Rule val compose = createComposeRule()
    private class Memory : PreferenceBridge {
        val data = mutableMapOf<String, String>()
        override suspend fun read(key: String) = data[key]
        override suspend fun write(key: String, value: String) { data[key] = value }
        override suspend fun delete(key: String) { data.remove(key) }
    }
    @Test fun actual_agenda_dialog_saves_time_note_and_ingredients_only_to_test_profile() {
        val store = DailyLifeStore(Memory())
        compose.setContent { MaterialTheme { CompositionLocalProvider(LocalDailyLifeStore provides store, LocalDailyLifeProfileKey provides "synthetic") {
            AddToAgendaAction("番茄面", "meal", "2026-10-13", listOf("番茄", "面条"), "tomato-noodle", estimatedPriceCentsPerPortion = 1200)
        } } }
        compose.onNodeWithTag("add-to-agenda").performClick()
        compose.onNodeWithTag("agenda-time").performTextReplacement("12:30")
        compose.onNodeWithTag("agenda-note").performTextInput("少放盐")
        compose.onNodeWithTag("agenda-portions").performTextReplacement("3")
        compose.onNodeWithTag("agenda-confirm").performClick()
        compose.waitForIdle()
        runBlocking {
            val item = store.load("synthetic").agenda.single()
            assertEquals("12:30", item.time)
            assertEquals("少放盐", item.note)
            assertEquals(3, item.portions)
            assertEquals(1200, item.estimatedPriceCentsPerPortion)
            assertEquals(listOf("番茄", "面条"), ShoppingList.from(listOf(item), item.dateKey).items.map { it.ingredient })
            assertTrue(store.load("other").agenda.isEmpty())
        }
    }

    @Test fun overlapping_agenda_requires_a_second_explicit_save() {
        val store = DailyLifeStore(Memory())
        runBlocking { store.upsertAgenda("synthetic", DailyAgendaItem("existing", "2026-10-16", "12:00", "已约午餐", "custom")) }
        compose.setContent { MaterialTheme { CompositionLocalProvider(LocalDailyLifeStore provides store, LocalDailyLifeProfileKey provides "synthetic") {
            AddToAgendaAction("番茄面", "meal", "2026-10-16", listOf("番茄"), "tomato-noodle")
        } } }
        compose.onNodeWithTag("add-to-agenda").performClick()
        compose.onNodeWithTag("agenda-confirm").performClick()
        compose.waitForIdle()
        runBlocking { assertEquals(1, store.load("synthetic").agenda.size) }
        compose.onNodeWithText("这个时段已有：已约午餐。可以改时间，或再次点击确认保留重叠安排。").assertIsDisplayed()
        compose.onNodeWithTag("agenda-confirm").performClick()
        compose.waitForIdle()
        runBlocking { assertEquals(2, store.load("synthetic").agenda.size) }
    }

    @Test fun actual_pantry_input_marks_saved_meal_ingredient_as_available() {
        val store = DailyLifeStore(Memory())
        runBlocking { store.upsertAgenda("synthetic", DailyAgendaItem("meal", "2026-10-16", "12:00", "番茄面", "meal", listOf("番茄", "面条"))) }
        compose.setContent { MaterialTheme {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                com.xuanji.app.ui.history.DailyLifePanel(store, "synthetic", "2026-10-16")
            }
        } }
        compose.onNodeWithTag("pantry-ingredient").performScrollTo().performTextInput("番茄")
        compose.onNodeWithText("加入库存").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("pantry-ingredient").assertIsNotFocused()
        compose.onNodeWithText("番茄 · 库存已有 · 用于 番茄面").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("面条 · 待补充 · 用于 番茄面").performScrollTo().assertIsDisplayed()
        runBlocking { assertEquals(listOf(PantryItem("番茄")), store.load("synthetic").pantry) }
    }
    @Test fun save_result_uses_original_text_and_calendar_completion_is_reversible() {
        val store = DailyLifeStore(Memory())
        runBlocking { store.upsertAgenda("synthetic", DailyAgendaItem("a", "2026-10-13", "09:00", "阅读", "custom")) }
        compose.setContent { MaterialTheme { CompositionLocalProvider(LocalDailyLifeStore provides store, LocalDailyLifeProfileKey provides "synthetic") {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                SaveReadingSnapshotAction(MysticDetailContext("综合", "day", "2026-10-13", "今日原文", "不会重算的原始结果", "原始依据", "离线", "fixture-v1"))
                com.xuanji.app.ui.history.DailyLifePanel(store, "synthetic", "2026-10-13")
            }
        } } }
        compose.onNodeWithTag("save-reading-snapshot").performClick()
        compose.onNodeWithTag("agenda-complete-a").performScrollTo().performClick()
        compose.waitForIdle()
        runBlocking { assertTrue(store.load("synthetic").agenda.single().completed) }
        compose.onNodeWithTag("agenda-complete-a").performClick()
        compose.waitForIdle()
        runBlocking {
            assertFalse(store.load("synthetic").agenda.single().completed)
            assertEquals("不会重算的原始结果", store.load("synthetic").snapshots.single().body)
        }
    }
}
