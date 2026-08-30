package com.xuanji.app.domain

import com.xuanji.app.domain.divination.Ifa
import com.xuanji.app.domain.divination.IfaCatalogue
import com.xuanji.app.domain.divination.UnavailableEseCorpusProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class IfaCorpusTest {
    @Test fun catalogue_contains_256_unique_combinations() {
        val all = Ifa.combinations()
        assertEquals(256, all.size)
        assertEquals(256, all.map { it.canonicalName }.distinct().size)
        assertEquals(IfaCatalogue.combination(15, 15), all.last())
    }
    @Test fun missing_ese_corpus_never_fabricates_text() {
        val result = Ifa.divine(LocalDate.of(2026, 8, 30), "career")
        assertTrue(result.combination.index in 0..255)
        assertNull(UnavailableEseCorpusProvider.find(result.combination))
    }
}
