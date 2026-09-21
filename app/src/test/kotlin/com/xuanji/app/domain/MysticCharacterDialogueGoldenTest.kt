package com.xuanji.app.domain

import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.model.EasternDailyFortune
import com.xuanji.app.data.model.Element
import com.xuanji.app.data.model.FortuneDimension
import com.xuanji.app.data.model.WesternDailyFortune
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 20 inputs × 4 guests = 80 deterministic relevance checks. */
class MysticCharacterDialogueGoldenTest {
    private val cases = listOf(
        "你好" to MysticIntent.Greeting,
        "早安" to MysticIntent.Greeting,
        "晚安" to MysticIntent.Greeting,
        "谢谢" to MysticIntent.Thanks,
        "再见" to MysticIntent.Farewell,
        "你是谁" to MysticIntent.Identity,
        "无聊" to MysticIntent.Smalltalk,
        "今天运势怎么样" to MysticIntent.Fortune,
        "我最近很焦虑" to MysticIntent.Mood,
        "感情怎么办" to MysticIntent.Love,
        "工作和事业" to MysticIntent.Career,
        "最近睡眠不好" to MysticIntent.Health,
        "投资要注意什么" to MysticIntent.Wealth,
        "今天吃什么" to MysticIntent.TodayMeal,
        "今天适合做什么" to MysticIntent.TodayActivity,
        "去哪玩" to MysticIntent.TodayOuting,
        "天气预报" to MysticIntent.Weather,
        "塔罗牌" to MysticIntent.Tarot,
        "生命数字" to MysticIntent.Numerology,
        "来一盘象棋" to MysticIntent.Game
    )

    @Test
    fun all_golden_intents_stay_relevant_for_all_four_characters() {
        val engine = DefaultMysticDialogueEngine()
        MysticCharacterId.entries.forEach { id ->
            val name = MysticCharacterCatalog.byId(id).displayName
            cases.forEach { (input, expected) ->
                val context = DialogueContext(
                    mode = "scholar",
                    styleKey = "archive",
                    topicKey = "composite",
                    fortune = fortune,
                    question = input,
                    characterId = id,
                    characterName = name
                )
                val first = engine.reply(context, input)
                val second = engine.reply(context, input)
                assertEquals("$id / $input", expected, first.intent)
                assertTrue("empty reply for $id / $input", first.text.isNotBlank())
                val marker = when (id) {
                    MysticCharacterId.ShenYanzhou -> listOf("八字", "沈砚舟", "诗歌")
                    MysticCharacterId.MoHeng -> listOf("墨衡", "批注", "棋盘")
                    MysticCharacterId.EvelynNova -> listOf("伊芙琳", "观测", "牌阵")
                    MysticCharacterId.NadirRashid -> listOf("纳迪尔", "路线", "文化参考")
                }
                assertTrue("missing role voice for $id / $input: ${first.text}", marker.any(first.text::contains))
                assertTrue(first.specialtyKey?.isNotBlank() == true)
                assertEquals("non-deterministic $id / $input", first, second)
                assertFalse(first.text.contains("保证收益"))
                assertFalse(first.text.contains("确诊"))
                if (expected in setOf(MysticIntent.Greeting, MysticIntent.Thanks, MysticIntent.Farewell, MysticIntent.Smalltalk)) {
                    assertFalse(first.text.contains("你问：「$input」"))
                }
            }
        }
    }

    private companion object {
        val fortune = CompositeDailyFortune(
            dateKey = "2026-09-22", overallScore = 72,
            dimensions = listOf(
                FortuneDimension("career", "事业", 70, "稳步推进"),
                FortuneDimension("wealth", "财富", 66, "先守后动"),
                FortuneDimension("emotion", "情感", 68, "说清楚")
            ),
            luckyNumber = 6, luckyColor = "青", luckyDirection = "东南", cautions = "别硬顶",
            eastern = EasternDailyFortune(
                dateKey = "2026-09-22", overallScore = 68, careerScore = 70, wealthScore = 65,
                loveScore = 66, healthScore = 69, summary = "东方盘平稳", advice = "稳步推进",
                dayPillarText = "甲子", favorableToday = emptyList<Element>(), luckyColor = "青", luckyDirection = "东南"
            ),
            western = WesternDailyFortune(
                dateKey = "2026-09-22", sign = "处女座", overallScore = 74, careerScore = 73,
                wealthScore = 72, loveScore = 71, healthScore = 75, summary = "西方盘平稳",
                luckyNumber = 6, luckyColor = "青", luckyDirection = "东南", dimensionBasis = emptyMap()
            )
        )
    }
}
