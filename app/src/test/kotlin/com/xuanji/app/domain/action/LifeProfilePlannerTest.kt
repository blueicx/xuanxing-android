package com.xuanji.app.domain.action

import com.xuanji.app.data.model.BaziChart
import com.xuanji.app.data.model.Branch
import com.xuanji.app.data.model.Element
import com.xuanji.app.data.model.Pillar
import com.xuanji.app.data.model.Stem
import com.xuanji.app.data.model.TestRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LifeProfilePlannerTest {
    private val planner = LifeProfilePlanner()

    @Test
    fun no_test_records_produce_low_confidence_and_explicit_missing_inputs() {
        val result = planner.plan(input(testRecords = emptyList(), preferredCityKey = null))
        assertEquals(ConfidenceLevel.Low, result.confidence)
        assertTrue(result.missingInputs.contains("职业/性格测试"))
        assertTrue(result.regionCandidates.size <= 3)
    }

    @Test
    fun test_records_change_reasons_but_do_not_replace_chart_and_zodiac_evidence() {
        val result = planner.plan(input(testRecords = listOf(
            TestRecord("霍兰德", "职业", "IAS", "艺术社会研究", "2026-09-19 09:00")
        )))
        assertTrue(result.careerClusters.flatMap { it.reasons }.any { it.contains("霍兰德") })
        assertTrue(result.evidence.any { it.source == ActionSource.FiveElements })
        assertTrue(result.evidence.any { it.source == ActionSource.Zodiac })
    }

    @Test
    fun region_cards_have_matched_and_unmatched_factors_and_stable_order() {
        val first = planner.plan(input(emptyList(), "shanghai"))
        val second = planner.plan(input(emptyList(), "shanghai"))
        assertEquals(first.regionCandidates, second.regionCandidates)
        assertTrue(first.regionCandidates.all { it.matchedTags.isNotEmpty() || it.unmatchedFactors.isNotEmpty() })
        assertFalse(first.disclaimer.contains("最适合"))
    }

    private fun input(testRecords: List<TestRecord>, preferredCityKey: String? = null) = LifeProfileInput(
        profileKey = "profile-a",
        chart = BaziChart(
            yearPillar = Pillar(Stem.甲, Branch.子),
            monthPillar = Pillar(Stem.丙, Branch.寅),
            dayPillar = Pillar(Stem.戊, Branch.辰),
            hourPillar = Pillar(Stem.壬, Branch.申),
            dayMaster = Stem.戊,
            zodiac = "鼠",
            elementCounts = mapOf(Element.WOOD to 2, Element.FIRE to 1, Element.EARTH to 3, Element.METAL to 1, Element.WATER to 1),
            favorableElements = listOf(Element.WATER, Element.WOOD),
            unfavorableElements = listOf(Element.FIRE)
        ),
        zodiacKey = "双鱼座",
        zodiacElement = Element.WATER,
        tests = testRecords,
        preferredCityKey = preferredCityKey
    )
}
