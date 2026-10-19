package com.xuanji.app.domain

import com.xuanji.app.domain.action.*
import org.junit.Assert.*
import org.junit.Test

class PantryDraftBackupTest {
    @Test fun practical_food_preferences_are_selected_validated_and_roundtrip_without_affecting_old_backups() {
        val food = FoodPreference(availableEquipment = setOf(KitchenEquipment.Oven), maxCookingDifficulty = CookingDifficulty.Easy,
            lunchboxOnly = true, prioritizePantry = true)
        val json = LocalDataBackupCodec.encode("p", LocalBackupSelection(foodPreferences = true), LocalBackupPayload(foodPreference = food), 1)
        assertEquals(food, LocalDataBackupCodec.decode(json, "p").backup!!.payload.foodPreference)
        val root = com.google.gson.JsonParser.parseString(json).asJsonObject
        root.getAsJsonObject("payload").getAsJsonObject("foodPreference").addProperty("maxCookingDifficulty", "not-real")
        root.remove("checksum")
        root.addProperty("checksum", StableScore.sha256(root.toString()))
        assertNull(LocalDataBackupCodec.decode(root.toString(), "p").backup)
    }
    private val stock = PantryItem("番茄", "私人备注", "2", "个", "2026-10-23", "蔬果")
    private val purchase = ShoppingPurchase("2026-10-19", "番茄", true, "2", "个", true)
    private val draft = WeeklyMenuDraft("2026-10-19", "200", 2)

    @Test fun selected_agenda_partition_roundtrips_stock_purchase_and_draft_without_unselected_notes() {
        val payload = LocalBackupPayload(pantry = listOf(stock), shoppingPurchases = listOf(purchase), weeklyMenuDraft = draft)
        val json = LocalDataBackupCodec.encode("p", LocalBackupSelection(agenda = true), payload, 1)
        assertFalse(json.contains("私人备注"))
        val decoded = LocalDataBackupCodec.decode(json, "p").backup!!
        assertEquals(8, decoded.version)
        assertEquals(stock.copy(note = ""), decoded.payload.pantry.single())
        assertEquals(listOf(purchase), decoded.payload.shoppingPurchases)
        assertEquals(draft, decoded.payload.weeklyMenuDraft)
        val omitted = LocalDataBackupCodec.decode(LocalDataBackupCodec.encode("p", LocalBackupSelection(favorites = true), payload, 1), "p").backup!!.payload
        assertTrue(omitted.pantry.isEmpty())
        assertTrue(omitted.shoppingPurchases.isEmpty())
        assertNull(omitted.weeklyMenuDraft)
    }

    @Test fun legacy_backup_v5_without_structured_fields_remains_readable_after_checksum_verification() {
        val json = LocalDataBackupCodec.encode("p", LocalBackupSelection(agenda = true), LocalBackupPayload(pantry = listOf(stock)), 1)
        val root = com.google.gson.JsonParser.parseString(json).asJsonObject
        root.addProperty("version", 5)
        val payload = root.getAsJsonObject("payload")
        payload.remove("shoppingPurchases"); payload.remove("weeklyMenuDraft")
        val row = payload.getAsJsonArray("pantry").single().asJsonObject
        listOf("quantity", "unit", "expiresOn", "category").forEach { row.remove(it) }
        root.remove("checksum")
        root.addProperty("checksum", StableScore.sha256(root.toString()))
        val restored = LocalDataBackupCodec.decode(root.toString(), "p").backup!!.payload
        assertEquals(PantryItem("番茄"), restored.pantry.single())
        assertTrue(restored.shoppingPurchases.isEmpty())
        root.addProperty("exportedAtEpochMs", 99)
        assertEquals(LocalBackupDecodeError.ChecksumMismatch, LocalDataBackupCodec.decode(root.toString(), "p").error)
    }
}
