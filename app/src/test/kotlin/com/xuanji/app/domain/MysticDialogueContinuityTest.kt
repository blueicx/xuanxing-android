package com.xuanji.app.domain

import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.model.Element
import com.xuanji.app.data.model.EasternDailyFortune
import com.xuanji.app.data.model.FortuneDimension
import com.xuanji.app.data.model.WesternDailyFortune
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MysticDialogueContinuityTest {
    private val fortune = CompositeDailyFortune(
        dateKey = "2026-08-31", overallScore = 72,
        dimensions = listOf(FortuneDimension("career", "事业", 70, "稳步推进")),
        luckyNumber = 6, luckyColor = "青", luckyDirection = "东南", cautions = "别硬顶",
        eastern = EasternDailyFortune("2026-08-31", 68, 70, 65, 66, 69, "东方盘平稳", "稳步推进", "甲子", emptyList<Element>(), "青", "东南"),
        western = WesternDailyFortune("2026-08-31", "处女座", 74, 73, 72, 71, 75, "西方盘平稳", 6, "青", "东南", dimensionBasis = emptyMap()),
        period = "day", periodSummary = "平稳推进", insights = emptyList()
    )

    @Test
    fun elliptical_follow_up_inherits_last_topic() {
        val context = DialogueContext(
            mode = "scholar", styleKey = "archive", topicKey = "composite", fortune = fortune,
            recentTurns = listOf(MysticTurn("我今天运势怎么样", "综合 72 分", MysticIntent.Fortune.value))
        )

        val reply = DefaultMysticDialogueEngine().reply(context, "继续")

        assertEquals(MysticIntent.Fortune, reply.intent)
        assertTrue(reply.text.contains("综合 72 分"))
    }

    @Test
    fun explicit_new_topic_overrides_last_topic() {
        val context = DialogueContext(
            mode = "scholar", styleKey = "archive", topicKey = "composite", fortune = fortune,
            recentTurns = listOf(MysticTurn("我今天运势怎么样", "综合 72 分", MysticIntent.Fortune.value))
        )

        val reply = DefaultMysticDialogueEngine().reply(context, "那工作怎么办")

        assertEquals(MysticIntent.Career, reply.intent)
    }
}
