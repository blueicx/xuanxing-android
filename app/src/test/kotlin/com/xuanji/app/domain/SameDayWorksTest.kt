package com.xuanji.app.domain

import java.time.LocalDate
import com.xuanji.app.ui.history.HistoryCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SameDayWorksTest {

    @Test
    fun works_are_deterministic_and_include_public_domain_music_and_poetry() {
        val first = SameDayWorks.forDate(LocalDate.of(2026, 8, 31))

        assertEquals(first, SameDayWorks.forDate(LocalDate.of(2026, 8, 31)))
        assertTrue(first.any { it.kind == WorkKind.MUSIC })
        assertTrue(first.any { it.kind == WorkKind.POETRY && it.publicDomain })
    }

    @Test
    fun copyrighted_work_never_exposes_excerpt() {
        SameDayWorks.CATALOG.filterNot { it.publicDomain }.forEach {
            assertNull(it.excerpt)
        }
    }

    @Test
    fun summary_is_shorter_than_full_note() {
        val text = "第一句摘要。第二句补充。第三句展开细节。"

        assertEquals("第一句摘要。", HistoryCopy.summary(text))
    }
}
