package com.xuanji.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 医疗/财务红线的守卫层。
 *
 * 这里只断言守卫自己负责的事：哪些问法算要结论、拒答句里不得出现什么、免责句怎么补。
 * customAnswer 是否真的走了这道门，由 MysticDialogueEngineTest 的那条用例负责。
 */
class MysticSafetyGuardTest {

    private val modes = listOf("scholar", "half", "")
    private val domains = listOf(SafetyDomain.Health, SafetyDomain.Finance)

    // ---- 拒答句本身 -----------------------------------------------------------------

    @Test
    fun refusals_never_state_a_diagnosis_or_a_profit_guarantee() {
        modes.forEach { mode ->
            domains.forEach { domain ->
                (0..7).forEach { variant ->
                    val line = MysticSafetyGuard.refusal(mode, domain, variant)
                    assertTrue(line.isNotEmpty())
                    MysticSafetyGuard.FORBIDDEN.forEach { token ->
                        assertEquals("$mode/$domain/$variant 泄漏了 $token", false, line.contains(token))
                    }
                }
            }
        }
    }

    @Test
    fun each_refusal_names_only_its_own_domain() {
        modes.forEach { mode ->
            domains.forEach { domain ->
                (0..1).forEach { variant ->
                    val line = MysticSafetyGuard.refusal(mode, domain, variant)
                    val own = MysticSafetyGuard.disclaimerFor(domain)
                    val other = if (domain == SafetyDomain.Health)
                        MysticSafetyGuard.FINANCE_DISCLAIMER else MysticSafetyGuard.HEALTH_DISCLAIMER
                    assertEquals(1, occurrences(line, own))
                    assertEquals(0, occurrences(line, other))
                }
            }
        }
    }

    @Test
    fun the_two_personas_refuse_in_their_own_words() {
        val lines = mutableSetOf<String>()
        modes.forEach { mode ->
            domains.forEach { domain ->
                (0..1).forEach { variant -> lines += MysticSafetyGuard.refusal(mode, domain, variant) }
            }
        }
        // 玄学家/半仙 × 健康/财务 × 两句一组 = 8 句，各不相同才是两套嘴而不是一个模板。
        assertEquals(8, lines.size)
        domains.forEach { domain ->
            (0..1).forEach { variant ->
                assertNotEquals(
                    MysticSafetyGuard.refusal("scholar", domain, variant),
                    MysticSafetyGuard.refusal("half", domain, variant)
                )
            }
        }
    }

    @Test
    fun the_disclaimers_are_the_ones_the_app_already_shows() {
        assertEquals("不构成医疗建议，如有不适请咨询专业人士。", MysticSafetyGuard.HEALTH_DISCLAIMER)
        assertEquals("不构成投资建议，请量力而行。", MysticSafetyGuard.FINANCE_DISCLAIMER)
        assertEquals("", MysticSafetyGuard.disclaimerFor(SafetyDomain.None))
    }

    // ---- 什么算「要结论」 -------------------------------------------------------------

    @Test
    fun the_guard_catches_what_the_topic_router_misses() {
        // 分类器按主题分流，这句落在 Daily，根本不会走健康分支；守卫必须自己认出来。
        assertEquals(MysticIntent.Daily, MysticIntentClassifier.classify("我该吃什么药"))
        assertEquals(SafetyDomain.Health, MysticSafetyGuard.verdictDomainOf("我该吃什么药"))
        assertEquals(
            MysticSafetyGuard.refusal("scholar", SafetyDomain.Health, 0),
            MysticSafetyGuard.enforce("scholar", "我该吃什么药", 0, "走 Daily 分支的草稿")
        )

        assertEquals(MysticIntent.Wealth, MysticIntentClassifier.classify("告诉我买哪只股票一定赚钱"))
        assertEquals(SafetyDomain.Finance, MysticSafetyGuard.verdictDomainOf("告诉我买哪只股票一定赚钱"))
    }

    @Test
    fun everyday_hesitation_is_not_mistaken_for_a_medical_ask() {
        assertEquals(SafetyDomain.None, MysticSafetyGuard.verdictDomainOf("我是不是该辞职"))
        assertEquals(SafetyDomain.None, MysticSafetyGuard.domainOf("我是不是该辞职"))
        assertEquals("原样返回", MysticSafetyGuard.enforce("scholar", "我是不是该辞职", 0, "原样返回"))
    }

    @Test
    fun verdict_asks_replace_the_whole_reply_instead_of_topping_it_up() {
        val draft = "「健康」 69 分。优先睡眠、饮食和活动量。"
        val out = MysticSafetyGuard.enforce("scholar", "我是不是抑郁症", 0, draft)
        assertEquals(MysticSafetyGuard.refusal("scholar", SafetyDomain.Health, 0), out)
        assertFalse(out.contains(draft))
    }

    @Test
    fun advisory_replies_keep_their_own_words_and_gain_the_disclaimer_once() {
        val out = MysticSafetyGuard.enforce("scholar", "最近体检要注意什么", 0, "先把睡眠排第一。")
        assertEquals("先把睡眠排第一。" + MysticSafetyGuard.HEALTH_DISCLAIMER, out)

        val alreadyCarrying = "先把睡眠排第一。" + MysticSafetyGuard.HEALTH_DISCLAIMER
        assertEquals(alreadyCarrying, MysticSafetyGuard.enforce("scholar", "最近体检要注意什么", 0, alreadyCarrying))
    }

    @Test
    fun plain_questions_are_left_entirely_alone() {
        listOf("", "   ", "今天运势怎么样", "陪我聊聊").forEach { question ->
            assertEquals(SafetyDomain.None, MysticSafetyGuard.domainOf(question))
            assertEquals(SafetyDomain.None, MysticSafetyGuard.verdictDomainOf(question))
            assertEquals("原句", MysticSafetyGuard.enforce("half", question, 1, "原句"))
        }
    }

    @Test
    fun the_same_question_is_always_judged_the_same_way() {
        val cases = listOf(
            "投资一套房是不是病" to SafetyDomain.Health,
            "股票能不能买，是不是病" to SafetyDomain.Finance,
            "是不是病，股票能不能买" to SafetyDomain.Health,
            "血压高怎么办" to SafetyDomain.None,
            // 拿着别人的确诊结果来问「怎么办」同样拒：解读报告比下判断更容易出错。
            "我拿到确诊结果了怎么办" to SafetyDomain.Health
        )
        cases.forEach { (question, expected) ->
            assertEquals(expected, MysticSafetyGuard.verdictDomainOf(question))
            assertEquals(expected, MysticSafetyGuard.verdictDomainOf(question))
            assertEquals(MysticSafetyGuard.domainOf(question), MysticSafetyGuard.domainOf(question))
        }
    }

    @Test
    fun overlong_questions_are_judged_on_the_same_words_as_short_ones() {
        // 截断是引擎的边界（take(200)），守卫不自己削句子，否则两端判定会不一致。
        val long = "我该吃什么药" + "呀".repeat(500)
        assertEquals(SafetyDomain.Health, MysticSafetyGuard.verdictDomainOf(long))
        assertTrue(MysticSafetyGuard.refusal("scholar", SafetyDomain.Health, 0).length < long.length)
    }

    private fun occurrences(text: String, needle: String): Int {
        var index = text.indexOf(needle)
        var count = 0
        while (index >= 0) {
            count++
            index = text.indexOf(needle, index + needle.length)
        }
        return count
    }
}
