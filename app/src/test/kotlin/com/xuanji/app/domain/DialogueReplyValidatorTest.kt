package com.xuanji.app.domain

import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.model.EasternDailyFortune
import com.xuanji.app.data.model.Element
import com.xuanji.app.data.model.WesternDailyFortune
import org.junit.Assert.assertEquals
import org.junit.Test

class DialogueReplyValidatorTest {
    @Test
    fun preparation_minutes_are_not_fortune_scores() {
        listOf("准备约 15 分钟", "准备约15分钟", "活动 10–15 分钟", "只有十五分钟").forEach {
            assertEquals(it, ValidationResult.Accept, DialogueReplyValidator.validate(it, testDialogueContext()))
        }
    }

    @Test
    fun minutes_do_not_allow_an_invented_score_in_the_same_reply() {
        assertEquals(ValidationResult.Reject("ungrounded_fact"),
            DialogueReplyValidator.validate("准备15分钟，综合99分。", testDialogueContext()))
    }
    @Test
    fun provider_text_that_invents_a_score_is_rejected_to_offline_fallback() {
        val result = DialogueReplyValidator.validate(
            "你的综合分是 99 分，保证升职。",
            testDialogueContext(fortune = fortune(overallScore = 72))
        )

        assertEquals(ValidationResult.Reject("ungrounded_fact"), result)
    }

    @Test
    fun provider_text_with_a_medical_or_finance_conclusion_is_rejected() {
        assertEquals(
            ValidationResult.Reject("safety_boundary"),
            DialogueReplyValidator.validate("你已经确诊，建议服用这个药。", testDialogueContext())
        )
        assertEquals(
            ValidationResult.Reject("safety_boundary"),
            DialogueReplyValidator.validate("这只股票保证收益。", testDialogueContext())
        )
    }

    private fun testDialogueContext(fortune: CompositeDailyFortune = fortune()) = DialogueContext(
        mode = "scholar",
        styleKey = "archive",
        topicKey = "composite",
        fortune = fortune
    )

    private fun fortune(overallScore: Int = 72) = CompositeDailyFortune(
        dateKey = "2026-09-12", overallScore = overallScore, dimensions = emptyList(), luckyNumber = 6,
        luckyColor = "青", luckyDirection = "东南", cautions = "慢一点",
        eastern = EasternDailyFortune("2026-09-12", 68, 70, 65, 66, 69, "平稳", "慢一点", "甲子", emptyList<Element>(), "青", "东南"),
        western = WesternDailyFortune("2026-09-12", "处女座", 74, 73, 72, 71, 75, "平稳", 6, "青", "东南")
    )
}
