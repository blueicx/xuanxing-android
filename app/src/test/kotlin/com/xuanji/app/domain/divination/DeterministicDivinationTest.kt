package com.xuanji.app.domain.divination

import com.xuanji.app.data.model.TarotCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeterministicDivinationTest {
    @Test fun reduction_steps_reproduce_result_and_preserve_existing_master_values() {
        assertEquals(listOf(1984, 22), NumerologyReduction.steps(1984))
        listOf(11, 22, 33).forEach { assertEquals(listOf(it), NumerologyReduction.steps(it)) }
        assertEquals(listOf(9999, 36, 9), NumerologyReduction.steps(9999))
        assertEquals("1984 → 1+9+8+4=22（保留大师数）", NumerologyReduction.explain(1984))
        assertEquals(0, NumerologyReduction.reduce(0))
    }
    private val deck = (1..12).map { index ->
        TarotCard("牌$index", "Card $index", "minor", "权杖", "正位$index", "逆位$index")
    }

    @Test
    fun same_query_has_same_tarot_order_and_orientation() {
        val query = DivinationQuery("tarot", "profile-a", "2026-09-19", "今天的工作", "three")
        val first = DeterministicTarot.read(deck, query)
        val second = DeterministicTarot.read(deck, query)
        assertEquals(first, second)
        assertEquals(3, first.cards.size)
        assertEquals(3, first.cards.map { it.card.nameCn }.toSet().size)
    }

    @Test
    fun changing_question_changes_seed_without_randomness() {
        val base = DivinationQuery("tarot", "profile-a", "2026-09-19", "工作", "single")
        val changed = base.copy(question = "感情")
        assertNotEquals(DeterministicDraw.seed(base), DeterministicDraw.seed(changed))
    }

    @Test
    fun rune_three_spread_is_stable_and_unique() {
        val query = DivinationQuery("rune", "p", "2026-09-19", "要不要换工作", "three")
        val reading = Rune.read(query)
        assertEquals(listOf("根因", "当下", "方向"), reading.draws.map { it.position })
        assertEquals(3, reading.draws.map { it.result.rune.name }.toSet().size)
        assertEquals(reading, Rune.read(query))
    }

    @Test
    fun numerology_cycles_keep_master_numbers() {
        val profile = com.xuanji.app.data.model.UserProfile(1984, 11, 22, 9, 0, "中国/上海")
        val cycles = NumerologyCycles.calculate(profile, java.time.LocalDate.of(2026, 11, 22))
        assertEquals(4, cycles.size)
        assertTrue(cycles.all { it.calculation.isNotBlank() && it.interpretation.isNotBlank() })
        val masters = NumerologyCycles.calculate(profile.copy(birthDay = 11), java.time.LocalDate.of(2009, 11, 11))
        assertEquals(33, masters.first().number)
        assertTrue(masters.first().calculation.contains("11+11+11"))
    }
}
