package com.xuanji.app.domain

import com.xuanji.app.data.local.DailyLifeStore
import com.xuanji.app.data.local.PreferenceBridge
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PantryShoppingTest {
    private class Memory : PreferenceBridge {
        val data = mutableMapOf<String, String>()
        override suspend fun read(key: String) = data[key]
        override suspend fun write(key: String, value: String) { data[key] = value }
        override suspend fun delete(key: String) { data.remove(key) }
    }
    private val date = "2026-10-19"
    private val meal = DailyAgendaItem("meal", date, "", "番茄面", "meal", listOf("番茄", "面条"))

    @Test fun purchased_checkbox_does_not_change_stock_and_confirmed_transfer_is_idempotent() = runBlocking {
        val memory = Memory(); val store = DailyLifeStore(memory)
        store.savePurchase("a", ShoppingPurchase(date, " 番茄 ", purchased = true, quantity = "2", unit = "个"))
        assertTrue(store.load("a").pantry.isEmpty())
        assertTrue(store.transferPurchaseToPantry("a", date, "番茄", "2026-10-23", "蔬果"))
        assertFalse(store.transferPurchaseToPantry("a", date, "番茄", "2026-10-23", "蔬果"))
        val restored = DailyLifeStore(memory).load("a")
        assertEquals(PantryItem("番茄", quantity = "2", unit = "个", expiresOn = "2026-10-23", category = "蔬果"), restored.pantry.single())
        assertTrue(restored.shoppingPurchases.single().transferredToPantry)
        assertTrue(store.load("b").pantry.isEmpty())
    }

    @Test fun existing_stock_is_neither_summed_nor_overwritten_without_explicit_confirmation() = runBlocking {
        val memory = Memory(); val store = DailyLifeStore(memory)
        store.upsertPantryItem("a", PantryItem("番茄", "原备注", "500", "克"))
        store.savePurchase("a", ShoppingPurchase(date, "番茄", true, "2", "个"))
        val before = memory.data.toMap()
        assertTrue(runCatching { store.transferPurchaseToPantry("a", date, "番茄") }.isFailure)
        assertEquals(before, memory.data)
        store.transferPurchaseToPantry("a", date, "番茄", replaceExisting = true)
        assertEquals("2", store.load("a").pantry.single().quantity)
        assertEquals("个", store.load("a").pantry.single().unit)
        assertEquals("原备注", store.load("a").pantry.single().note)
    }

    @Test fun unpurchased_or_invalid_input_cannot_transfer_or_overwrite_a_document() = runBlocking {
        val memory = Memory(); val store = DailyLifeStore(memory)
        store.savePurchase("a", ShoppingPurchase(date, "番茄"))
        val before = memory.data.toMap()
        assertTrue(runCatching { store.transferPurchaseToPantry("a", date, "番茄") }.isFailure)
        assertTrue(runCatching { store.upsertPantryItem("a", PantryItem("番茄", quantity = "-2", unit = "克")) }.isFailure)
        assertTrue(runCatching { store.upsertPantryItem("a", PantryItem("番茄", expiresOn = "2026-02-30")) }.isFailure)
        assertEquals(before, memory.data)
    }

    @Test fun expired_stock_is_not_marked_available_but_unknown_quantity_is_not_invented() {
        val stock = listOf(PantryItem("番茄", expiresOn = "2026-10-18"), PantryItem("面条"))
        val shopping = ShoppingList.from(listOf(meal), date, stock)
        assertFalse(shopping.items.single { it.ingredient == "番茄" }.inPantry)
        assertTrue(shopping.items.single { it.ingredient == "番茄" }.expiredStock)
        assertTrue(shopping.items.single { it.ingredient == "面条" }.inPantry)
        assertEquals("", stock.last().quantity)
    }

    @Test fun legacy_v4_stock_migrates_without_interpreting_notes_as_quantities_or_expiry() {
        val root = com.google.gson.JsonParser.parseString(DailyLifeCodec.encode("p", DailyLifeDocument(pantry = listOf(PantryItem("番茄", "2个，周末吃"))))).asJsonObject
        root.addProperty("version", 4)
        val body = root.getAsJsonObject("document")
        body.remove("shoppingPurchases")
        val entry = body.getAsJsonArray("pantry").single().asJsonObject
        listOf("quantity", "unit", "expiresOn", "category").forEach { entry.remove(it) }
        val restored = DailyLifeCodec.decode(root.toString(), "p")
        assertTrue(restored.readable)
        assertEquals(PantryItem("番茄", "2个，周末吃"), restored.pantry.single())
        assertTrue(restored.shoppingPurchases.isEmpty())
    }

    @Test fun clear_sections_keep_unselected_records_and_other_profiles() = runBlocking {
        val store = DailyLifeStore(Memory())
        store.savePurchase("a", ShoppingPurchase(date, "番茄", true))
        store.upsertPantryItem("a", PantryItem("番茄", "备注", "2", "个", "2026-10-23", "蔬果"))
        store.savePurchase("b", ShoppingPurchase(date, "番茄", true))
        store.clearSections("a", notes = true)
        assertEquals("2", store.load("a").pantry.single().quantity)
        store.clearSections("a", purchases = true)
        assertTrue(store.load("a").shoppingPurchases.isEmpty())
        assertEquals(1, store.load("a").pantry.size)
        assertEquals(1, store.load("b").shoppingPurchases.size)
    }

    @Test fun expiry_reminders_are_opt_in_and_use_only_explicit_dates_not_unknown_or_empty_stock() {
        val stock = listOf(PantryItem("番茄", expiresOn = "2026-10-21"), PantryItem("面条"),
            PantryItem("鸡蛋", quantity = "0", unit = "个", expiresOn = "2026-10-18"),
            PantryItem("蘑菇", expiresOn = "2026-10-30"))
        val base = DailyLifeDocument(pantry = stock)
        val today = java.time.LocalDate.parse(date)
        assertTrue(PantryRules.reminderItems(base, today).isEmpty())
        val opted = base.copy(reminders = ReminderPreferences(enabled = true, agenda = false, savedResults = false, pantry = true))
        assertEquals(listOf("番茄"), PantryRules.reminderItems(opted, today).map { it.ingredient })
        assertNotNull(opted.reminders.nextAfter(today.atTime(10, 0)))
        assertTrue(PantryRules.reminderItems(opted.copy(readable = false), today).isEmpty())
        assertTrue(PantryRules.reminderItems(opted.copy(reminders = opted.reminders.copy(weekdays = emptySet())), today).isEmpty())
    }
}
