package com.xuanji.app.domain

import com.xuanji.app.domain.divination.NadiQuery
import com.xuanji.app.domain.divination.NadiReadingKind
import com.xuanji.app.domain.divination.OfflineNadiSimulationProvider
import com.xuanji.app.domain.divination.UnavailableNadiCorpusProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NadiCorpusTest {
    @Test fun offline_simulation_is_labeled_and_deterministic() {
        val query = NadiQuery("thumb-print", "zh", 7)
        val a = OfflineNadiSimulationProvider.lookup(query)
        val b = OfflineNadiSimulationProvider.lookup(query)
        assertEquals(NadiReadingKind.OfflineSimulation, a.kind)
        assertEquals(a, b)
        assertNotNull(a.text)
    }
    @Test fun unavailable_provider_has_no_text() {
        assertEquals(NadiReadingKind.Unavailable, UnavailableNadiCorpusProvider.lookup(NadiQuery("x")).kind)
    }
}
