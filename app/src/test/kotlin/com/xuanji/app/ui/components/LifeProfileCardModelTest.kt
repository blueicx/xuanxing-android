package com.xuanji.app.ui.components

import com.xuanji.app.domain.action.ConfidenceLevel
import com.xuanji.app.domain.action.EnvironmentProfile
import com.xuanji.app.domain.action.LifeProfile
import com.xuanji.app.domain.action.RegionCandidate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LifeProfileCardModelTest {
    @Test
    fun region_cards_use_match_language_and_show_unmatched_factors() {
        val card = LifeProfileCardModel.from(
            LifeProfile("p", emptyList(), emptyList(), EnvironmentProfile(listOf("green"), "慢", emptyList()), listOf(RegionCandidate("葡萄牙", "里斯本", listOf("waterfront"), listOf("签证"), 70)), emptyList(), ConfidenceLevel.Low, listOf("测试"), "边界")
        ).regions.single()
        assertTrue(card.title.contains("匹配示例"))
        assertTrue(card.body.contains("未考虑因素"))
        assertFalse(card.body.contains("最适合你"))
    }
}
