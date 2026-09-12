package com.xuanji.app.domain.divination

import com.google.gson.Gson
import com.xuanji.app.domain.MysticGuideGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

/**
 * 今日灵签的分支键测试：重构把「玄学家 / 半仙」和 "high"/"mid"/"low" 从查询键换成了枚举，
 * 因此这里钉的全是**换键之后必须仍然逐字成立**的东西——口吻归属、12 条反应句、6 条离席句
 * 与缓存字段形状，而不是实现细节。
 */
class TodayOracleTest {

    private fun aDraw(level: String, luckyNumber: Int) = TodayOracle.OracleResult(
        level = level,
        poem = "云开见月正分明，谋望求财事有成。",
        luckyNumber = luckyNumber,
        luckyColor = "朱红",
        good = "会友",
        avoid = "争执",
        advice = "诸事顺遂，宜把握良机、主动出击。"
    )

    private val choices = listOf("why", "accept", "pushback")

    /** 偶数是学者、奇数是半仙：这两个号码就是两张表的全部键。 */
    private fun scholar(level: String) = aDraw(level, 2)
    private fun half(level: String) = aDraw(level, 3)

    @Test
    fun the_two_oracle_personas_share_the_companion_labels() {
        assertEquals("玄学家", OracleRole.Scholar.label)
        assertEquals("半仙", OracleRole.Half.label)
        listOf(OracleRole.Scholar, OracleRole.Half).forEach { role ->
            assertEquals(
                "the oracle must read its label from the companion's single source",
                MysticGuideGenerator.personaName(role.modeKey),
                role.label
            )
        }
        assertEquals(OracleRole.Half, OracleRole.Scholar.other)
        assertEquals(OracleRole.Scholar, OracleRole.Half.other)
    }

    @Test
    fun every_level_maps_to_the_tier_the_copy_assumes() {
        assertEquals(OracleTier.High, aDraw("上上签", 2).tier)
        assertEquals(OracleTier.High, aDraw("上签", 2).tier)
        assertEquals(OracleTier.Mid, aDraw("中平签", 2).tier)
        assertEquals(OracleTier.Low, aDraw("下签", 2).tier)
        assertEquals(OracleTier.Low, aDraw("下下签", 2).tier)
    }

    @Test
    fun rekeying_preserves_every_reaction_and_exit_line_verbatim() {
        val daily = listOf(
            scholar("上签") to "今日签面确实亮；我把要点记在旁边，别急着把它当成通行证。",
            half("上签") to "哟，签面挺会挑日子？先别飘，本半仙看看你能不能接住。",
            scholar("中平签") to "今日签不急不缓，正好看你怎么走；稳着来就好。",
            half("中平签") to "不上不下的签？行吧，本半仙先看看你会不会自己找台阶。",
            scholar("下签") to "签面沉一点而已，不是终局；今天把步子放小，我在旁边看着。",
            half("下签") to "签是有点蔫，但别急着给自己判刑；本半仙还等着看你翻页呢。"
        )
        daily.forEach { (draw, expected) ->
            val reaction = TodayOracle.dailyReaction(draw)
            assertEquals(expected, reaction.line)
            assertEquals(roleNameOf(draw), reaction.roleName)
        }

        val manual = listOf(
            scholar("上签") to "彩蛋倒是亮堂；今日正签已经收好，别把这份当成加码的理由。",
            half("上签") to "哟，彩蛋也敢这么体面？正签可没答应帮你续杯，别得意。",
            scholar("中平签") to "彩蛋平平也好，正好当对照；今天还是按正签慢慢走。",
            half("中平签") to "中不溜的彩蛋，看看就行；本半仙可不许你拿它跟正签讨价还价。",
            scholar("下签") to "这支只是彩蛋，不算数；先把今天的节奏放轻一点，别被它带紧张。",
            half("下签") to "咳，彩蛋抽得有点蔫？别慌，正签才是今天的主角，本半仙盯着呢。"
        )
        manual.forEach { (draw, expected) ->
            val reaction = TodayOracle.manualReaction(draw)
            assertEquals(expected, reaction.line)
            assertEquals(roleNameOf(draw), reaction.roleName)
        }

        val exits = listOf(
            scholar("上签") to "why" to "把签纸抚平后离开，像把问题也折进了页边。",
            scholar("上签") to "accept" to "点头记完一笔，脚步放轻地退开。",
            scholar("上签") to "pushback" to "抬手示意不扰，转身时仍留了半步距离。",
            half("上签") to "why" to "咂了下嘴，甩着袖子走了，嘴上还嘀咕「算你有心」。",
            half("上签") to "accept" to "哼了一声，倒背着手晃出门去。",
            half("上签") to "pushback" to "耸耸肩退到帘外，临走还挑了下眉。"
        )
        exits.forEach { (key, expected) ->
            val (draw, choiceKey) = key
            val exchange = TodayOracle.observerExchange(draw, choiceKey)
            assertNotNull(exchange)
            assertEquals(expected, exchange!!.exitLine)
        }
    }

    @Test
    fun the_cached_oracle_shape_is_unchanged() {
        // 按 Gson 自己的口径数：非 static、非 transient 的实例字段。
        // declaredFields 全量会多出 Compose @StabilityInferred 生成的 `public static final int $stable`。
        val persisted = TodayOracle.OracleResult::class.java.declaredFields
            .filter { !Modifier.isStatic(it.modifiers) && !Modifier.isTransient(it.modifiers) }
            .map { it.name }
            .sorted()
        assertEquals(
            "a new persisted field would turn yesterday's cached draw into nulls",
            listOf("advice", "avoid", "good", "level", "luckyColor", "luckyNumber", "poem"),
            persisted
        )

        val draw = scholar("上签")
        val json = Gson().toJson(draw)
        assertTrue(json.contains("\"level\""))
        assertTrue("tier must stay a computed read, never a wire field", !json.contains("tier"))
        assertEquals(draw, Gson().fromJson(json, TodayOracle.OracleResult::class.java))
        assertEquals(draw.tier, Gson().fromJson(json, TodayOracle.OracleResult::class.java).tier)
    }

    @Test
    fun relay_and_replies_are_deterministic_per_draw_and_choice() {
        listOf("上上签", "上签", "中平签", "下签", "下下签").forEach { level ->
            choices.forEach { choice ->
                val asScholar = TodayOracle.observerExchange(scholar(level), choice)!!
                val asHalf = TodayOracle.observerExchange(half(level), choice)!!
                assertEquals("玄学家", asScholar.roleName)
                assertEquals("半仙", asHalf.roleName)
                assertNotEquals("the two personas must never share a line", asScholar.line, asHalf.line)
                assertNotEquals(asScholar.exitLine, asHalf.exitLine)
                assertEquals(asScholar.line, TodayOracle.observerExchange(scholar(level), choice)!!.line)

                listOf(scholar(level), half(level)).forEach { draw ->
                    val relay = TodayOracle.observerRelay(draw, choice)
                    if (relay != null) {
                        assertEquals(roleNameOf(draw.other()), relay.roleName)
                        assertNotEquals(asScholar.line, relay.line)
                        assertEquals(relay.line, TodayOracle.observerRelay(draw, choice)!!.line)
                    }
                }
            }
        }

        assertNull(TodayOracle.observerExchange(scholar("上签"), "未知选项"))
        assertNull(TodayOracle.observerRelay(scholar("上签"), "未知选项"))
    }

    @Test
    fun a_seeded_draw_still_walks_the_unchanged_poem_table() {
        val seen = mutableSetOf<String>()
        (1L..400L).forEach { seed ->
            val first = TodayOracle.generate(seed)
            assertEquals(first, TodayOracle.generate(seed))
            assertTrue(first.level in listOf("上上签", "上签", "中平签", "下签", "下下签"))
            assertTrue(first.poem.isNotBlank() && first.advice.isNotBlank())
            seen += first.poem
        }
        assertEquals("every poem in the fixed table must still be reachable", 12, seen.size)
    }

    private fun roleNameOf(draw: TodayOracle.OracleResult): String =
        MysticGuideGenerator.personaName(if (draw.luckyNumber % 2 == 0) "scholar" else "half")

    /** The relay always speaks as the persona that did *not* read the sign first. */
    private fun TodayOracle.OracleResult.other(): TodayOracle.OracleResult =
        aDraw(level, if (luckyNumber % 2 == 0) 3 else 2)
}
