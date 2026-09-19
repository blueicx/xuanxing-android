package com.xuanji.app.domain

import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.model.EasternDailyFortune
import com.xuanji.app.data.model.FortuneDimension
import com.xuanji.app.data.model.WesternDailyFortune
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyResponseGuardTest {
    @Test
    fun health_and_finance_verdicts_are_replaced() {
        val context = context()
        val health = SafetyResponseGuard.guard("我该吃什么药", "建议你服用某药", context)
        val finance = SafetyResponseGuard.guard("推荐我买哪只股票", "这只一定赚钱", context)
        assertTrue(health.replaced)
        assertTrue(finance.replaced)
        assertFalse(health.text.contains("建议服用"))
        assertFalse(finance.text.contains("一定赚钱"))
    }

    @Test
    fun chat_cannot_create_implicit_personality_label() {
        val guarded = SafetyResponseGuard.guard("我最近很累", "从你的聊天看你是回避型人格", context())
        assertTrue(guarded.text.contains("不会仅凭聊天"))
        assertEqualsUnknown(guarded.personalitySource)
    }

    private fun assertEqualsUnknown(value: PersonalitySource) = assertTrue(value == PersonalitySource.Unknown)

    private fun context() = DialogueContext(
        mode = "scholar",
        styleKey = "archive",
        topicKey = "health",
        fortune = CompositeDailyFortune(
            dateKey = "2026-09-19", overallScore = 70,
            dimensions = listOf(FortuneDimension("health", "健康", 70, "稳")),
            luckyNumber = 7, luckyColor = "青", luckyDirection = "东",
            cautions = "留意节奏",
            eastern = EasternDailyFortune("2026-09-19", 70, 70, 70, 70, 70, "稳", "留意", "甲子", emptyList(), "青", "东"),
            western = WesternDailyFortune("2026-09-19", "白羊座", 70, 70, 70, 70, 70, "稳", 7, "青", "东")
        )
    )
}
