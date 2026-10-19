package com.xuanji.app.domain

import com.google.gson.JsonParser
import com.xuanji.app.domain.action.*
import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest

class LocalDataBackupV2Test {
    @Test fun selected_new_sections_roundtrip_with_notes_and_historical_text() {
        val selection = LocalBackupSelection(foodPreferences = true, activityPreferences = true, agenda = true, notes = true, snapshots = true)
        val payload = LocalBackupPayload(
            foodPreference = FoodPreference(excludedIngredients = setOf("豆腐"), maxMealBudgetCents = 1500),
            activityPreference = ActivityPreference(indoorOnly = true, maxDurationMinutes = 15),
            agenda = listOf(DailyAgendaItem("a", "2026-10-13", "12:00", "午餐", "meal", note = "自己写的备注")),
            snapshots = listOf(ReadingSnapshot("s", "2026-10-12", "day", "composite", "昨天原文", "原依据", "old-v1", "昨天"))
        )
        val result = LocalDataBackupCodec.decode(LocalDataBackupCodec.encode("profile", selection, payload, 1), "profile").backup!!
        assertEquals(LocalDataBackupCodec.VERSION, result.version)
        assertEquals(payload, result.payload)
        val withoutNotes = LocalDataBackupCodec.decode(LocalDataBackupCodec.encode("profile", selection.copy(notes = false), payload, 1), "profile").backup!!
        assertEquals("", withoutNotes.payload.agenda.single().note)
    }
    @Test fun too_many_selected_records_are_rejected_not_truncated() {
        val payload = LocalBackupPayload(favorites = (0..LocalDataBackupCodec.MAX_FAVORITES).map {
            ReadingFavorite("f-$it", "fortune", "标题", body = "正文", createdAt = 1)
        })
        assertTrue(runCatching { LocalDataBackupCodec.encode("profile", LocalBackupSelection(favorites = true), payload, 1) }.isFailure)
    }
    @Test fun legacy_v1_is_verified_before_migration_and_remains_importable() {
        val root = JsonParser.parseString(LocalDataBackupCodec.encode("profile", LocalBackupSelection(favorites = true), LocalBackupPayload(), 1)).asJsonObject
        root.addProperty("version", 1)
        listOf("foodPreferences", "activityPreferences", "agenda", "notes", "snapshots").forEach { root.getAsJsonObject("selection").remove(it) }
        listOf("foodPreference", "activityPreference", "agenda", "snapshots").forEach { root.getAsJsonObject("payload").remove(it) }
        root.remove("checksum")
        val sum = MessageDigest.getInstance("SHA-256").digest(root.toString().toByteArray()).joinToString("") { "%02x".format(it) }
        root.addProperty("checksum", sum)
        assertNotNull(LocalDataBackupCodec.decode(root.toString(), "profile").backup)
        root.addProperty("exportedAtEpochMs", 99)
        assertEquals(LocalBackupDecodeError.ChecksumMismatch, LocalDataBackupCodec.decode(root.toString(), "profile").error)
    }
}
