package com.xuanji.app.data.local

import com.xuanji.app.domain.*
import com.xuanji.app.domain.action.*
import com.xuanji.app.domain.game.GameSave
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LocalBackupV2RepositoryTest {
    @Test fun failed_restore_rolls_back_structured_stock_purchase_and_draft_together() = runBlocking {
        val bridge = Memory(); val life = DailyLifeStore(bridge)
        val keys = LocalBackupProfileKeys("fav", "profile")
        val original = DailyLifeDocument(pantry = listOf(PantryItem("番茄", quantity = "1", unit = "个")),
            shoppingPurchases = listOf(ShoppingPurchase("2026-10-19", "番茄", true)), weeklyMenuDraft = WeeklyMenuDraft("2026-10-19", "100"),
            journal = listOf(DailyJournalEntry("2026-10-19", "原始主动记录")))
        life.replace("profile", original)
        val selection = LocalBackupSelection(agenda = true, notes = true)
        val json = LocalDataBackupCodec.encode(keys.fingerprintKey, selection, LocalBackupPayload(
            pantry = listOf(PantryItem("番茄", quantity = "2", unit = "个", expiresOn = "2026-10-23")),
            shoppingPurchases = listOf(ShoppingPurchase("2026-10-19", "番茄", true, "2", "个", true)),
            weeklyMenuDraft = WeeklyMenuDraft("2026-10-19", "200"), journal = listOf(DailyJournalEntry("2026-10-19", "替换记录"))), 1)
        val repo = repository(bridge); val preview = repo.preview(json, keys)
        bridge.failOnceOnKey = LocalRestoreTransaction.LOG_KEY
        assertEquals(LocalBackupRestoreStatus.Failed, repo.restore(json, keys, selection, preview.conflicts).status)
        assertEquals(original, life.load("profile"))
        assertNull(bridge.values[LocalRestoreTransaction.JOURNAL_KEY])
        assertTrue(bridge.values[LocalRestoreTransaction.LOG_KEY]!!.contains("rolled_back"))
    }
    @Test fun structured_purchase_and_draft_restore_preview_conflicts_before_atomic_write() = runBlocking {
        val bridge = Memory(); val life = DailyLifeStore(bridge)
        val keys = LocalBackupProfileKeys("fav", "profile")
        val selection = LocalBackupSelection(agenda = true)
        val purchase = ShoppingPurchase("2026-10-19", "番茄", true, "2", "个")
        val draft = WeeklyMenuDraft("2026-10-19", "200")
        life.savePurchase("profile", purchase.copy(quantity = "1"))
        life.saveWeeklyDraft("profile", draft.copy(budgetYuan = "100"))
        val json = LocalDataBackupCodec.encode(keys.fingerprintKey, selection,
            LocalBackupPayload(pantry = listOf(PantryItem("番茄", quantity = "2", unit = "个", expiresOn = "2026-10-23", category = "蔬果")),
                shoppingPurchases = listOf(purchase), weeklyMenuDraft = draft), 1)
        val repo = repository(bridge); val before = bridge.values.toMap()
        val preview = repo.preview(json, keys)
        assertEquals(setOf("purchase:${purchase.key}", "weeklyMenuDraft"), preview.selectedConflicts(selection))
        assertEquals(LocalBackupRestoreStatus.ConfirmationRequired, repo.restore(json, keys, selection, emptySet()).status)
        assertEquals(before, bridge.values)
        assertEquals(LocalBackupRestoreStatus.Completed, repo.restore(json, keys, selection, preview.selectedConflicts(selection)).status)
        assertEquals(listOf(purchase), life.load("profile").shoppingPurchases)
        assertEquals(draft, life.load("profile").weeklyMenuDraft)
        assertEquals("2", life.load("profile").pantry.single().quantity)
        assertTrue(life.load("other").shoppingPurchases.isEmpty())
    }

    @Test fun old_v5_import_does_not_erase_new_stock_metadata_purchase_or_weekly_draft() = runBlocking {
        val bridge = Memory(); val life = DailyLifeStore(bridge)
        val keys = LocalBackupProfileKeys("fav", "profile"); val selection = LocalBackupSelection(agenda = true)
        val stock = PantryItem("番茄", "本地备注", "500", "克", "2026-10-23", "蔬果")
        val draft = WeeklyMenuDraft("2026-10-19", "200")
        val purchase = ShoppingPurchase("2026-10-19", "番茄", true)
        life.upsertPantryItem("profile", stock); life.savePurchase("profile", purchase); life.saveWeeklyDraft("profile", draft)
        val root = com.google.gson.JsonParser.parseString(LocalDataBackupCodec.encode(keys.fingerprintKey, selection,
            LocalBackupPayload(pantry = listOf(PantryItem("番茄"))), 1)).asJsonObject
        root.addProperty("version", 5)
        val payload = root.getAsJsonObject("payload"); payload.remove("shoppingPurchases"); payload.remove("weeklyMenuDraft")
        val row = payload.getAsJsonArray("pantry").single().asJsonObject
        listOf("quantity", "unit", "expiresOn", "category").forEach { row.remove(it) }
        root.remove("checksum"); root.addProperty("checksum", StableScore.sha256(root.toString()))
        val repo = repository(bridge); val preview = repo.preview(root.toString(), keys)
        assertTrue(preview.conflicts.isEmpty())
        assertEquals(LocalBackupRestoreStatus.Completed, repo.restore(root.toString(), keys, selection, emptySet()).status)
        assertEquals(stock, life.load("profile").pantry.single())
        assertEquals(listOf(purchase), life.load("profile").shoppingPurchases)
        assertEquals(draft, life.load("profile").weeklyMenuDraft)
    }

    @Test fun pantry_notes_are_opt_in_and_restore_preserves_excluded_local_notes() = runBlocking {
        val bridge = Memory()
        val life = DailyLifeStore(bridge)
        val keys = LocalBackupProfileKeys("fav", "profile")
        life.upsertPantryItem("profile", PantryItem("番茄", "本机数量"))
        val selection = LocalBackupSelection(agenda = true)
        val payload = LocalBackupPayload(
            pantry = listOf(PantryItem("番茄", "备份数量"), PantryItem("鸡蛋", "备份备注")),
            agenda = listOf(DailyAgendaItem("meal", "2026-10-16", "12:00", "午餐", "meal", portions = 2, estimatedPriceCentsPerPortion = 1200))
        )
        val json = LocalDataBackupCodec.encode(keys.fingerprintKey, selection, payload, 1)
        assertFalse(json.contains("备份数量"))
        assertFalse(json.contains("备份备注"))
        val repo = repository(bridge)
        assertTrue(repo.preview(json, keys).conflicts.isEmpty())
        assertEquals(LocalBackupRestoreStatus.Completed, repo.restore(json, keys, selection, emptySet()).status)
        val restored = life.load("profile")
        assertEquals("本机数量", restored.pantry.single { it.ingredient == "番茄" }.note)
        assertEquals("", restored.pantry.single { it.ingredient == "鸡蛋" }.note)
        assertEquals(2, restored.agenda.single().portions)
        assertEquals(1200, restored.agenda.single().estimatedPriceCentsPerPortion)
        val noted = LocalDataBackupCodec.encode(keys.fingerprintKey, selection.copy(notes = true), payload, 1)
        val notedPreview = repo.preview(noted, keys)
        assertTrue("pantry-note:番茄" in notedPreview.selectedConflicts(selection.copy(notes = true)))
        assertTrue(notedPreview.selectedConflicts(selection).isEmpty())
    }

    @Test fun old_v2_backup_does_not_remove_newer_inventory() = runBlocking {
        val bridge = Memory()
        val life = DailyLifeStore(bridge)
        val keys = LocalBackupProfileKeys("fav", "profile")
        life.upsertPantryItem("profile", PantryItem("番茄", "保留"))
        val selection = LocalBackupSelection(agenda = true)
        val item = DailyAgendaItem("legacy", "2026-10-16", "", "旧午餐", "meal")
        val root = com.google.gson.JsonParser.parseString(LocalDataBackupCodec.encode(keys.fingerprintKey, selection,
            LocalBackupPayload(agenda = listOf(item)), 1)).asJsonObject
        root.addProperty("version", 2)
        root.getAsJsonObject("payload").remove("pantry")
        root.getAsJsonObject("payload").getAsJsonArray("agenda").single().asJsonObject.apply {
            remove("portions"); remove("estimatedPriceCentsPerPortion")
        }
        root.remove("checksum")
        val checksum = java.security.MessageDigest.getInstance("SHA-256").digest(root.toString().toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        root.addProperty("checksum", checksum)
        assertEquals(LocalBackupRestoreStatus.Completed, repository(bridge).restore(root.toString(), keys, selection, emptySet()).status)
        assertEquals(listOf(PantryItem("番茄", "保留")), life.load("profile").pantry)
        assertEquals(1, life.load("profile").agenda.single().portions)
        assertNull(life.load("profile").agenda.single().estimatedPriceCentsPerPortion)
    }

    @Test fun identical_game_progress_is_merged_without_an_overwrite_prompt() = runBlocking {
        val bridge = Memory()
        val keys = LocalBackupProfileKeys("fav", "profile")
        val games = CompanionGameProgressStore(bridge)
        games.save("profile", MysticCharacterId.ShenYanzhou,
            com.xuanji.app.domain.game.CompanionGameProgressState.Poetry(com.xuanji.app.domain.game.PoetryChainState()), 1)
        val selection = LocalBackupSelection(companionGames = true)
        val repo = repository(bridge)
        val json = requireNotNull(repo.export(keys, selection).json)
        val before = bridge.values.toMap()
        val preview = repo.preview(json, keys)
        assertEquals(1, preview.statistics(selection).merged)
        assertTrue(preview.conflicts.isEmpty())
        assertEquals(before, bridge.values)
    }

    @Test fun note_only_conflicts_disappear_when_notes_are_deselected() = runBlocking {
        val bridge = Memory()
        val keys = LocalBackupProfileKeys("fav", "profile")
        val item = DailyAgendaItem("same", "2026-10-13", "09:00", "安排", "custom", note = "保留的备注", createdAt = 1)
        DailyLifeStore(bridge).upsertAgenda("profile", item)
        val selection = LocalBackupSelection(agenda = true, notes = true)
        val json = LocalDataBackupCodec.encode(keys.fingerprintKey, selection, LocalBackupPayload(agenda = listOf(item.copy(note = "导入备注"))), 1)
        val preview = repository(bridge).preview(json, keys)
        assertEquals(setOf("notes:same"), preview.selectedConflicts(selection))
        assertTrue(preview.selectedConflicts(selection.copy(notes = false)).isEmpty())
        assertEquals(LocalBackupRestoreStatus.Completed, repository(bridge).restore(json, keys, selection.copy(notes = false), emptySet()).status)
        assertEquals(item.note, DailyLifeStore(bridge).load("profile").agenda.single().note)
    }
    @Test fun preview_counts_follow_final_partition_selection_and_do_not_write() = runBlocking {
        val bridge = Memory()
        val keys = LocalBackupProfileKeys("fav", "profile")
        val life = DailyLifeStore(bridge)
        val same = DailyAgendaItem("same", "2026-10-13", "09:00", "不变", "custom", createdAt = 1)
        val changed = same.copy(id = "changed", title = "旧安排")
        life.upsertAgenda("profile", same)
        life.upsertAgenda("profile", changed, allowTimeConflict = true)
        val selection = LocalBackupSelection(agenda = true, snapshots = true, foodPreferences = true)
        val json = LocalDataBackupCodec.encode(keys.fingerprintKey, selection,
            LocalBackupPayload(agenda = listOf(same, changed.copy(title = "新安排"), same.copy(id = "new")),
                foodPreference = FoodPreference(vegetarian = true),
                snapshots = listOf(ReadingSnapshot("snapshot", "2026-10-13", "今日", "综合", "保存的原文", "依据", "v1", "结果", 1))), 1)
        val before = bridge.values.toMap()
        val preview = repository(bridge).preview(json, keys)
        val all = preview.statistics(selection)
        assertEquals(3, all.added)
        assertEquals(1, all.merged)
        assertEquals(1, all.overwritten)
        assertEquals(0, all.omitted)
        val onlyAgenda = preview.statistics(LocalBackupSelection(agenda = true))
        assertEquals(1, onlyAgenda.added)
        assertEquals(1, onlyAgenda.merged)
        assertEquals(1, onlyAgenda.overwritten)
        assertEquals(2, onlyAgenda.omitted)
        assertEquals(before, bridge.values)
    }

    @Test fun unselected_notes_do_not_create_spurious_agenda_overwrite_conflicts() = runBlocking {
        val bridge = Memory()
        val keys = LocalBackupProfileKeys("fav", "profile")
        val item = DailyAgendaItem("same", "2026-10-13", "09:00", "安排", "custom", note = "本机私人备注", createdAt = 1)
        DailyLifeStore(bridge).upsertAgenda("profile", item)
        val selection = LocalBackupSelection(agenda = true)
        val json = LocalDataBackupCodec.encode(keys.fingerprintKey, selection, LocalBackupPayload(agenda = listOf(item)), 1)
        val preview = repository(bridge).preview(json, keys)
        assertTrue(preview.conflicts.isEmpty())
        assertEquals(1, preview.statistics(selection).merged)
        assertEquals(LocalBackupRestoreStatus.Completed, repository(bridge).restore(json, keys, selection, emptySet()).status)
        assertEquals(item.note, DailyLifeStore(bridge).load("profile").agenda.single().note)
    }
    private class Memory : PreferenceBridge {
        val values = linkedMapOf<String, String>()
        var failOnceOnKey: String? = null
        override suspend fun read(key: String) = values[key]
        override suspend fun write(key: String, value: String) {
            if (key == failOnceOnKey) { failOnceOnKey = null; error("synthetic_write_failure") }
            values[key] = value
        }
        override suspend fun delete(key: String) { values.remove(key) }
    }
    private fun repository(bridge: Memory) = LocalDataBackupRepository(
        ReadingFavoriteStore(bridge), ActionFeedbackDataStore(bridge), CompanionGameProgressStore(bridge),
        object : XiangqiSaveBackupStore {
            override suspend fun load(profileKey: String): GameSave? = null
            override suspend fun save(profileKey: String, save: GameSave) = Unit
        }, atomicBridge = bridge, dailyLifeStore = DailyLifeStore(bridge),
        foodPreferenceStore = FoodPreferenceStore(bridge), activityPreferenceStore = ActionConstraintStore(bridge)
    )
    @Test fun new_sections_restore_atomically_and_do_not_enable_reminders_or_other_profiles() = runBlocking {
        val bridge = Memory()
        val keys = LocalBackupProfileKeys("fav", "profile")
        val life = DailyLifeStore(bridge)
        life.upsertAgenda("profile", DailyAgendaItem("old", "2026-10-13", "09:00", "旧安排", "custom"))
        life.upsertAgenda("other", DailyAgendaItem("private", "2026-10-13", "09:00", "另一个档案", "custom"))
        val json = LocalDataBackupCodec.encode(keys.fingerprintKey,
            LocalBackupSelection(foodPreferences = true, agenda = true, notes = true, snapshots = true),
            LocalBackupPayload(foodPreference = FoodPreference(vegetarian = true),
                agenda = listOf(DailyAgendaItem("new", "2026-10-13", "12:00", "新安排", "meal", note = "原备注"))), 1)
        val result = repository(bridge).restore(json, keys,
            LocalBackupSelection(foodPreferences = true, agenda = true, notes = true, snapshots = true), emptySet())
        assertEquals(LocalBackupRestoreStatus.Completed, result.status)
        assertEquals(setOf("old", "new"), life.load("profile").agenda.map { it.id }.toSet())
        assertEquals("原备注", life.load("profile").agenda.single { it.id == "new" }.note)
        assertFalse(life.load("profile").reminders.enabled)
        assertEquals(listOf("private"), life.load("other").agenda.map { it.id })
        assertTrue(FoodPreferenceStore(bridge).read("profile").vegetarian)
        assertNotNull(bridge.values[LocalRestoreTransaction.LOG_KEY])
    }
    @Test fun same_id_conflicts_are_previewed_and_confirmation_prevents_any_write() = runBlocking {
        val bridge = Memory()
        val keys = LocalBackupProfileKeys("fav", "profile")
        DailyLifeStore(bridge).upsertAgenda("profile", DailyAgendaItem("same", "2026-10-13", "09:00", "旧安排", "custom"))
        val selection = LocalBackupSelection(agenda = true)
        val json = LocalDataBackupCodec.encode(keys.fingerprintKey, selection,
            LocalBackupPayload(agenda = listOf(DailyAgendaItem("same", "2026-10-13", "12:00", "新安排", "custom"))), 1)
        val before = bridge.values.toMap()
        val repo = repository(bridge)
        assertTrue(repo.preview(json, keys).conflicts.contains("agenda:same"))
        assertEquals(LocalBackupRestoreStatus.ConfirmationRequired, repo.restore(json, keys, selection, emptySet()).status)
        assertEquals(before, bridge.values)
    }
}
