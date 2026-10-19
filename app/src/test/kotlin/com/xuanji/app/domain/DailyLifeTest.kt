package com.xuanji.app.domain

import com.xuanji.app.data.local.DailyLifeStore
import com.xuanji.app.data.local.PreferenceBridge
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DailyLifeTest {
    @Test fun legacy_document_migrates_to_one_portion_and_retains_unknown_budget() {
        val item = DailyAgendaItem("a", "2026-10-16", "12:00", "番茄面", "meal")
        val root = com.google.gson.JsonParser.parseString(DailyLifeCodec.encode("digest", DailyLifeDocument(agenda = listOf(item)))).asJsonObject
        root.addProperty("version", 1)
        val body = root.getAsJsonObject("document")
        body.remove("pantry")
        body.getAsJsonArray("agenda").single().asJsonObject.apply {
            remove("portions"); remove("estimatedPriceCentsPerPortion")
        }
        val decoded = DailyLifeCodec.decode(root.toString(), "digest")
        assertTrue(decoded.readable)
        assertEquals(1, decoded.agenda.single().portions)
        assertNull(decoded.agenda.single().estimatedPriceCentsPerPortion)
        assertTrue(decoded.pantry.isEmpty())
    }

    @Test fun conflicts_require_explicit_consent_and_failed_copy_does_not_partially_write() = runBlocking {
        val memory = Memory()
        val store = DailyLifeStore(memory)
        val first = DailyAgendaItem("a", "2026-10-16", "12:00", "午餐", "meal", portions = 3, estimatedPriceCentsPerPortion = 900)
        store.upsertAgenda("a", first)
        val collision = first.copy(id = "b", title = "约会")
        assertTrue(runCatching { store.upsertAgenda("a", collision) }.isFailure)
        assertEquals(listOf(first), store.load("a").agenda)
        store.upsertAgenda("a", collision, allowTimeConflict = true)
        val blocked = store.copyAgendaDate("a", "2026-10-16", "2026-10-17", onlyMeals = true)
        assertTrue(blocked.copied.isEmpty())
        assertTrue(blocked.conflicts.isNotEmpty())
        assertEquals(2, store.load("a").agenda.size)
        store.removeAgenda("a", "b")
        val copied = store.copyAgendaDate("a", "2026-10-16", "2026-10-17", onlyMeals = true).copied.single()
        assertNotEquals(first.id, copied.id)
        assertEquals(3, copied.portions)
        assertEquals(900, copied.estimatedPriceCentsPerPortion)
        assertFalse(copied.completed)
        store.moveAgenda("a", copied.id, "2026-10-18")
        assertTrue(store.load("a").agenda.none { it.dateKey == "2026-10-17" })
        assertEquals("2026-10-18", store.load("a").agenda.single { it.id == copied.id }.dateKey)
    }

    @Test fun pantry_survives_reopen_matches_shopping_and_clears_only_selected_profile() = runBlocking {
        val memory = Memory()
        val store = DailyLifeStore(memory)
        store.upsertPantryItem("a", PantryItem(" 番茄 ", "2 个"))
        store.upsertPantryItem("b", PantryItem("番茄", "留给其他档案"))
        val restored = DailyLifeStore(memory).load("a")
        val item = DailyAgendaItem("a", "2026-10-16", "", "番茄面", "meal", listOf("番茄", "面条"))
        val shopping = ShoppingList.from(listOf(item), item.dateKey, restored.pantry)
        assertTrue(shopping.items.single { it.ingredient == "番茄" }.inPantry)
        assertFalse(shopping.items.single { it.ingredient == "面条" }.inPantry)
        store.clearSections("a", notes = true)
        assertEquals("", store.load("a").pantry.single().note)
        store.clearSections("a", pantry = true)
        assertTrue(store.load("a").pantry.isEmpty())
        assertEquals("留给其他档案", store.load("b").pantry.single().note)
    }

    @Test fun invalid_portions_and_budget_cannot_be_persisted() {
        val item = DailyAgendaItem("a", "2026-10-16", "", "午餐", "meal")
        assertFalse(DailyLifeCodec.valid(DailyLifeDocument(agenda = listOf(item.copy(portions = 0)))))
        assertFalse(DailyLifeCodec.valid(DailyLifeDocument(agenda = listOf(item.copy(estimatedPriceCentsPerPortion = -1)))))
        assertFalse(DailyLifeCodec.valid(DailyLifeDocument(pantry = listOf(PantryItem("番茄"), PantryItem(" 番茄 ")))))
    }

    @Test fun selective_clear_keeps_unselected_sections_and_other_profiles() = runBlocking {
        val store = DailyLifeStore(Memory())
        val item = DailyAgendaItem("a", "2026-10-13", "12:00", "午餐", "meal", note = "备注")
        store.upsertAgenda("profile", item)
        store.upsertAgenda("other", item)
        store.saveSnapshot("profile", ReadingSnapshot("s", "2026-10-13", "day", "综合", "原文", "依据", "v1", "结果"))
        store.saveReminders("profile", ReminderPreferences(enabled = true))
        store.clearSections("profile", notes = true)
        assertEquals("", store.load("profile").agenda.single().note)
        assertEquals("备注", store.load("other").agenda.single().note)
        store.clearSections("profile", agenda = true, reminders = true)
        assertTrue(store.load("profile").agenda.isEmpty())
        assertFalse(store.load("profile").reminders.enabled)
        assertEquals(1, store.load("profile").snapshots.size)
    }
    private class Memory : PreferenceBridge {
        val values = mutableMapOf<String, String>()
        override suspend fun read(key: String) = values[key]
        override suspend fun write(key: String, value: String) { values[key] = value }
        override suspend fun delete(key: String) { values.remove(key) }
    }

    @Test fun agenda_completion_undo_and_delete_preserve_other_profiles() = runBlocking {
        val memory = Memory()
        val store = DailyLifeStore(memory)
        val item = DailyAgendaItem("item", "2026-10-13", "12:30", "番茄面", "meal", listOf("番茄", "面条"), "meal-1")
        store.upsertAgenda("a", item)
        store.upsertAgenda("b", item.copy(title = "其他档案"))
        store.setCompleted("a", "item", true)
        assertTrue(DailyLifeStore(memory).load("a").agenda.single().completed)
        store.setCompleted("a", "item", false)
        assertFalse(store.load("a").agenda.single().completed)
        store.removeAgenda("a", "item")
        assertTrue(store.load("a").agenda.isEmpty())
        assertEquals("其他档案", store.load("b").agenda.single().title)
    }

    @Test fun shopping_list_merges_ingredients_but_preserves_meal_sources() {
        val first = DailyAgendaItem("a", "2026-10-13", "", "番茄面", "meal", listOf("番茄", "面条"), "a")
        val second = first.copy(id = "b", title = "番茄汤", ingredients = listOf("番茄", "水"))
        val list = ShoppingList.from(listOf(first, second), "2026-10-13")
        assertEquals(3, list.items.size)
        assertEquals(listOf("番茄面", "番茄汤"), list.items.single { it.ingredient == "番茄" }.mealTitles)
        assertTrue(ShoppingList.from(listOf(first), "2026-10-14").items.isEmpty())
    }

    @Test fun explicit_snapshot_keeps_original_body_and_algorithm_version() = runBlocking {
        val store = DailyLifeStore(Memory())
        val snapshot = ReadingSnapshot("s", "2026-10-13", "今日", "东方", "原始结果", "原始依据", "v1", "标题")
        store.saveSnapshot("a", snapshot)
        assertEquals(snapshot, store.load("a").snapshots.single())
        assertTrue(store.load("b").snapshots.isEmpty())
    }

    @Test fun corrupt_data_never_gets_overwritten_by_normal_operations() = runBlocking {
        val memory = Memory()
        val store = DailyLifeStore(memory)
        memory.write(store.storageKey("a"), "{broken")
        assertFalse(store.load("a").readable)
        assertTrue(runCatching { store.upsertAgenda("a", DailyAgendaItem("x", "2026-10-13", "", "x", "activity")) }.isFailure)
        assertEquals("{broken", memory.read(store.storageKey("a")))
    }

    @Test fun reminders_are_disabled_initially_and_resolve_local_next_date() {
        val prefs = ReminderPreferences()
        assertFalse(prefs.enabled)
        val date = java.time.LocalDateTime.of(2026, 10, 13, 10, 0)
        assertEquals(java.time.LocalDateTime.of(2026, 10, 14, 9, 0),
            prefs.copy(enabled = true).nextAfter(date))
    }
}
