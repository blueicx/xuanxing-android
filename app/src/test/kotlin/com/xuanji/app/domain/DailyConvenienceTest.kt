package com.xuanji.app.domain

import com.xuanji.app.data.local.PreferenceBridge
import com.xuanji.app.data.local.ReadingPositionStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DailyConvenienceTest {
    @Test fun agendaWidgetUsesClickDateWhileNotificationsKeepTheirOriginalDate() {
        val today = java.time.LocalDate.of(2026, 10, 20)
        assertEquals("2026-10-20", DailyQuickEntry.resolveDate("com.xuanji.app.widget.Agenda", "2026-10-19", today, "com.xuanji.app"))
        assertEquals("2026-10-19", DailyQuickEntry.resolveDate(null, "2026-10-19", today, "com.xuanji.app"))
        assertNull(DailyQuickEntry.resolveDate("com.xuanji.app.widget.Resume", null, today, "com.xuanji.app"))
    }
    @Test fun detailPositionIsBoundToOwnerAndReadingAndCanBeCleared() = runBlocking {
        val data = mutableMapOf<String, String>()
        val store = ReadingPositionStore(object : PreferenceBridge {
            override suspend fun read(key: String) = data[key]
            override suspend fun write(key: String, value: String) { data[key] = value }
            override suspend fun delete(key: String) { data.remove(key) }
        })
        store.save("one", "reading", 350)
        assertEquals(350, store.load("one", "reading"))
        assertEquals(0, store.load("two", "reading"))
        assertEquals(0, store.load("one", "other"))
        assertTrue(runCatching { store.save("one", "reading", -1) }.isFailure)
        store.clear("one", "reading")
        assertEquals(0, store.load("one", "reading"))
    }
    @Test fun widgetRoutesAreWhitelistedAndNeverCarryPersonalPayloads() {
        assertEquals("history", DailyQuickEntry.Agenda.tab)
        assertEquals("companion", DailyQuickEntry.Resume.tab)
        assertEquals(DailyQuickEntry.Resume, DailyQuickEntry.from("companion"))
        assertNull(DailyQuickEntry.from("file:///private"))
    }
    @Test fun homepageReorderKeepsAllApprovedSectionsAndSupportsPinning() {
        val layouts = com.xuanji.app.ui.components.CardLayouts
        var state = layouts.default("today-sections")
        assertEquals(listOf("hero", "summary", "dimensions", "actions", "extras"), state.order.keys.toList())
        repeat(4) { state = layouts.move(state, "today-sections", "actions", -1) }
        assertEquals("actions", state.order.keys.sortedBy { state.order[it] }.first())
        assertEquals(5, state.order.size)
        assertTrue(state.hidden.isEmpty())
    }
}
