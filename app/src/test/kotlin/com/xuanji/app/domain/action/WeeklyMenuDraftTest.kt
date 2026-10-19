package com.xuanji.app.domain.action

import com.xuanji.app.data.local.DailyLifeStore
import com.xuanji.app.data.local.PreferenceBridge
import com.xuanji.app.domain.DailyLifeCodec
import com.xuanji.app.domain.DailyLifeDocument
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class WeeklyMenuDraftTest {
    private class Memory : PreferenceBridge {
        val data = mutableMapOf<String, String>()
        override suspend fun read(key: String) = data[key]
        override suspend fun write(key: String, value: String) { data[key] = value }
        override suspend fun delete(key: String) { data.remove(key) }
    }
    private val draft = WeeklyMenuDraft("2026-10-19", "200", 3,
        listOf(WeeklyMealLock("2026-10-19", MealSlot.Lunch, "tomato-egg-noodle")))

    @Test fun draft_survives_new_store_instance_and_is_explicitly_profile_scoped_and_clearable() = runBlocking {
        val memory = Memory(); val store = DailyLifeStore(memory)
        store.saveWeeklyDraft("a", draft)
        assertEquals(draft, DailyLifeStore(memory).load("a").weeklyMenuDraft)
        assertNull(store.load("b").weeklyMenuDraft)
        assertTrue(store.load("a").agenda.isEmpty())
        store.clearSections("a", weeklyDraft = true)
        assertNull(store.load("a").weeklyMenuDraft)
    }

    @Test fun malformed_or_duplicate_locks_are_rejected_without_replacing_existing_draft() = runBlocking {
        val memory = Memory(); val store = DailyLifeStore(memory)
        store.saveWeeklyDraft("a", draft)
        val before = memory.data.toMap()
        assertTrue(runCatching { store.saveWeeklyDraft("a", draft.copy(locks = draft.locks + draft.locks)) }.isFailure)
        assertTrue(runCatching { store.saveWeeklyDraft("a", draft.copy(budgetYuan = "99999999")) }.isFailure)
        assertFalse(DailyLifeCodec.valid(DailyLifeDocument(weeklyMenuDraft = draft.copy(startDate = "2026-02-30"))))
        assertEquals(before, memory.data)
    }

    @Test fun restored_locks_are_revalidated_against_current_filtered_pool_and_never_auto_saved() {
        val plan = WeeklyMealPlanner.plan("a", LocalDate.parse(draft.startDate), emptyList(), draft.locks)
        assertTrue(plan.meals.isEmpty())
        assertTrue(plan.problems.any { it.contains("锁定") })
    }
}
