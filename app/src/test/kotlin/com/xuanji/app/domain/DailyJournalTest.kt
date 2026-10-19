package com.xuanji.app.domain

import com.xuanji.app.data.local.*
import kotlinx.coroutines.test.runTest
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class DailyJournalTest {
    @Test fun journal_roundtrips_isolated_profiles_and_notes_clear_without_inferred_mood() = runTest {
        val data = mutableMapOf<String, String>()
        val store = DailyLifeStore(object : PreferenceBridge {
            override suspend fun read(key: String) = data[key]
            override suspend fun write(key: String, value: String) { data[key] = value }
            override suspend fun delete(key: String) { data.remove(key) }
        })
        val entry = DailyJournalEntry("2026-10-19", "用户写下的一天", "平静", "散步十分钟", 123)
        store.saveJournal("p", entry)
        assertEquals(listOf(entry), store.load("p").journal)
        assertTrue(store.load("other").journal.isEmpty())
        store.saveJournal("p", entry.copy(mood = "", text = "改过"))
        assertEquals("", store.load("p").journal.single().mood)
        store.clearSections("p", notes = true)
        assertTrue(store.load("p").journal.isEmpty())
    }

    @Test fun descriptive_review_counts_only_selected_range_and_explicit_values() {
        val date = LocalDate.of(2026, 10, 19)
        val document = DailyLifeDocument(journal = listOf(
            DailyJournalEntry("2026-10-18", "不在本周", "低落"),
            DailyJournalEntry("2026-10-19", "今天", "平静"),
            DailyJournalEntry("2026-10-20", "无心情选择", "")
        ), agenda = listOf(DailyAgendaItem("a", "2026-10-19", "", "散步", "activity", completed = true)))
        val review = DailyJournalRules.review(document, date, date.plusDays(6))
        assertEquals(2, review.recordedDays)
        assertEquals(mapOf("平静" to 1), review.moods)
        assertEquals(1, review.completedAgenda)
        assertEquals(1, review.totalAgenda)
        assertFalse(DailyJournalRules.valid(DailyJournalEntry("2026-02-30", "bad")))
        assertFalse(DailyJournalRules.valid(DailyJournalEntry("2026-10-19", "bad", "自动人格")))
    }

    @Test fun journal_backup_is_opt_in_notes_partition_and_legacy_life_document_is_empty() {
        val entry = DailyJournalEntry("2026-10-19", "私人日记", "平静", "主动记录")
        val payload = LocalBackupPayload(journal = listOf(entry))
        val excluded = LocalDataBackupCodec.encode("p", LocalBackupSelection(agenda = true), payload, 1)
        assertFalse(excluded.contains("私人日记"))
        val included = LocalDataBackupCodec.encode("p", LocalBackupSelection(agenda = true, notes = true), payload, 1)
        assertEquals(listOf(entry), LocalDataBackupCodec.decode(included, "p").backup!!.payload.journal)
        val raw = """{"version":5,"profileDigest":"p","document":{"agenda":[],"snapshots":[],"pantry":[],"shoppingPurchases":[]}}"""
        val legacy = DailyLifeCodec.decode(raw, "p")
        assertTrue(legacy.readable && legacy.journal.isEmpty())
    }
}
