package com.xuanji.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import com.xuanji.app.data.local.*
import com.xuanji.app.domain.action.*
import com.xuanji.app.ui.history.WeeklyMenuPanel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class WeeklyMenuInteractionTest {
    @get:Rule val compose = createComposeRule()
    private class Memory : PreferenceBridge {
        val data = mutableMapOf<String, String>()
        override suspend fun read(key: String) = data[key]
        override suspend fun write(key: String, value: String) { data[key] = value }
        override suspend fun delete(key: String) { data.remove(key) }
    }
    private val pool = listOf(MealSlot.Breakfast, MealSlot.Lunch, MealSlot.Dinner).flatMap { slot -> (1..7).map { index ->
        MealSuggestion(slot, "${slot.name}餐$index", listOf("番茄"), "", emptyList(), 80, emptyList(), 800, 15, "${slot.name}-$index")
    } }

    @Test fun cold_open_loads_persisted_draft_and_ineligible_lock_prevents_saving_until_user_removes_it() {
        val memory = Memory(); val store = DailyLifeStore(memory)
        runBlocking { store.saveWeeklyDraft("synthetic", WeeklyMenuDraft("2026-10-19", "200", 1,
            listOf(WeeklyMealLock("2026-10-19", MealSlot.Lunch, "now-excluded-meal")))) }
        compose.setContent { MaterialTheme { WeeklyMenuPanel(DailyLifeStore(memory), "synthetic", LocalDate.parse("2026-10-20")) { pool } } }
        compose.onNodeWithTag("weekly-menu-open").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("weekly-menu-preview").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("weekly-menu-start").performScrollTo().assertTextContains("2026-10-19")
        compose.onNodeWithTag("weekly-menu-budget").performScrollTo().assertTextContains("200")
        compose.onNodeWithTag("weekly-menu-save").assertIsNotEnabled()
        compose.onNodeWithText("取消全部锁定（尚未保存更改）").performScrollTo().performClick()
        compose.onNodeWithTag("weekly-menu-save").assertIsEnabled()
        compose.onNodeWithTag("weekly-draft-clear").performScrollTo().performClick()
        compose.waitForIdle()
        runBlocking {
            assertNull(store.load("synthetic").weeklyMenuDraft)
            assertTrue(store.load("synthetic").agenda.isEmpty())
        }
    }

    @Test fun explicitly_saved_draft_restores_budget_and_lock_after_saved_state_recreation() {
        val memory = Memory(); val store = DailyLifeStore(memory)
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MaterialTheme { WeeklyMenuPanel(store, "synthetic", LocalDate.parse("2026-10-19")) { pool } } }
        compose.onNodeWithTag("weekly-menu-open").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("weekly-menu-preview").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("weekly-menu-budget").performScrollTo().performTextReplacement("200")
        compose.onNodeWithTag("weekly-lock-2026-10-19-Lunch").performScrollTo().performClick()
        compose.onNodeWithTag("weekly-draft-save").performScrollTo().performClick()
        compose.waitForIdle()
        runBlocking {
            val saved = DailyLifeStore(memory).load("synthetic")
            assertEquals("200", saved.weeklyMenuDraft!!.budgetYuan)
            assertEquals(1, saved.weeklyMenuDraft!!.locks.size)
            assertTrue(saved.agenda.isEmpty())
        }
        restoration.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        compose.onNodeWithTag("weekly-menu-budget").performScrollTo().assertTextContains("200")
        compose.onNodeWithTag("weekly-lock-2026-10-19-Lunch").performScrollTo().assertIsOn()
    }

    @Test fun real_preview_and_confirm_persist_twenty_one_meals_without_touching_other_profiles() {
        val store = DailyLifeStore(Memory())
        compose.setContent { MaterialTheme { WeeklyMenuPanel(store, "synthetic", LocalDate.parse("2026-10-19")) { pool } } }
        compose.onNodeWithTag("weekly-menu-open").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("weekly-menu-preview").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("weekly-menu-preview").assertTextContains("21/21 餐", substring = true)
        runBlocking { assertTrue(store.load("synthetic").agenda.isEmpty()) }
        compose.onNodeWithTag("weekly-menu-save").performClick()
        compose.waitForIdle()
        runBlocking {
            assertEquals(21, store.load("synthetic").agenda.size)
            assertTrue(store.load("synthetic").agenda.all { it.estimatedPriceCentsPerPortion == 800 })
            assertTrue(store.load("other").agenda.isEmpty())
        }
    }
}
