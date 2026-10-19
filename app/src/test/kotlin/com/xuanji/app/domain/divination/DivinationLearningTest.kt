package com.xuanji.app.domain.divination

import com.xuanji.app.data.model.*
import org.junit.Assert.*
import org.junit.Test

class DivinationLearningTest {
    private val profile = UserProfile(1984, 11, 22, 9, 0, "测试")
    @Test fun basic_date_and_name_rules_show_distinct_reproducible_reduction_paths() {
        val items = Numerology.calculate(profile)
        assertTrue(items.all { it.calculation.isNotBlank() })
        assertTrue(items.single { it.label == "生命数字" }.calculation.contains("1+9+8+4+1+1+2+2"))
        assertTrue(items.single { it.label == "生日数字" }.calculation.contains("22（保留大师数）"))
    }
    @Test fun full_name_trace_matches_real_results_and_rejects_unmapped_scripts() {
        val trace = Numerology.fullCalculationSteps(profile, "john smith")
        assertEquals(Numerology.calculateFull(profile, "john smith").map { it.label to it.number }, trace.map { it.label to it.number })
        assertTrue(trace.single { it.label == "命运数" }.calculation.contains("J=10"))
        assertTrue(trace.single { it.label == "灵魂渴望数" }.calculation.contains("O=15"))
        assertTrue(Numerology.calculateFull(profile, "张三").isEmpty())
        assertTrue(Numerology.calculateFull(profile, "JOHN张").isEmpty())
        assertTrue(Numerology.calculateFull(profile, "straße").isEmpty())
        assertTrue(Numerology.calculateFull(profile, "J".repeat(121)).isEmpty())
        assertTrue(Numerology.calculateFull(profile, "123").isEmpty())
        assertEquals(0, Numerology.fullCalculationSteps(profile, "BCDF").single { it.label == "灵魂渴望数" }.number)
        assertEquals(trace, Numerology.fullCalculationSteps(profile, " JOHN SMITH "))
    }
    @Test fun tarot_relationships_use_only_actual_cards_positions_and_observed_suits() {
        fun card(name: String, suit: String, position: String, reversed: Boolean = false) =
            DrawnTarot(TarotCard(name, name, "minor", suit, "当前正位文案", "当前逆位文案"), reversed, position)
        val cards = listOf(card("甲牌", "圣杯", "过去"), card("乙牌", "圣杯", "现在", true), card("丙牌", "宝剑", "未来"))
        val result = DivinationLearning.tarotRelations(cards)
        assertTrue(result.any { it.contains("过去 · 甲牌") && it.contains("现在 · 乙牌") })
        assertTrue(result.any { it.contains("圣杯") && it.contains("2") })
        assertTrue(result.any { it.contains("逆位 1/3") })
        assertTrue(result.none { it.contains("命中注定") || it.contains("健康诊断") })
        assertTrue(DivinationLearning.tarotRelations(emptyList()).isEmpty())
        val snapshot = DivinationLearning.tarotDetail(cards, "2026-10-19", "seed-example")
        assertTrue(snapshot.result.contains("乙牌（逆位）") && snapshot.result.contains("当前逆位文案"))
        assertTrue(snapshot.evidence.contains("seed-example") && snapshot.evidence.contains("项目自编"))
        assertEquals("2026-10-19", snapshot.dateKey)
    }
    @Test fun rune_learning_is_complete_catalog_with_project_scope_and_no_fake_originals() {
        assertEquals(24, Rune.learningCatalog().size)
        assertEquals(24, Rune.learningCatalog().map { it.name }.distinct().size)
        val reading = Rune.read(DivinationQuery("rune", "p", "2026-10-19", "学习", "three"))
        val lines = DivinationLearning.runePositions(reading)
        assertEquals(3, lines.size)
        reading.draws.zip(lines).forEach { (draw, line) ->
            assertTrue(line.contains(draw.position) && line.contains(draw.result.rune.name))
        }
        assertTrue(DivinationLearning.SCOPE.contains("项目自编"))
    }
}
