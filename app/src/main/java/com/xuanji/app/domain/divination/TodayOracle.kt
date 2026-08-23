package com.xuanji.app.domain.divination

import kotlin.random.Random

/**
 * 今日灵签：固定签库内随机抽取。
 * 仅供文化娱乐参考。
 */
object TodayOracle {

    data class OracleResult(
        val level: String,     // 上上签 / 上签 / 中平 / 下签 / 下下签
        val poem: String,
        val luckyNumber: Int,
        val luckyColor: String,
        val good: String,      // 宜
        val avoid: String,     // 忌
        val advice: String
    )

    private val POEMS = listOf(
        Triple("上上签", "云开见月正分明，谋望求财事有成。", "诸事顺遂，宜把握良机、主动出击。"),
        Triple("上上签", "乘风破浪会有时，直挂云帆济沧海。", "运势上扬，敢想敢为必有所得。"),
        Triple("上签", "花开富贵满庭芳，喜气盈门日渐长。", "平稳向好，宜守正待时、广结善缘。"),
        Triple("上签", "青山绿水景无边，心宽自有福绵绵。", "心境开阔，贵人暗助，事多顺心。"),
        Triple("中平签", "风恬浪静好行舟，守得云开见月柔。", "宜稳不宜冒，循序而行可保平安。"),
        Triple("中平签", "半江明月半江风，得失随缘莫强求。", "随遇而安，凡事留三分余地。"),
        Triple("中平签", "柳暗花明又一村，困顿之中亦有门。", "转机将现，耐心可渡难关。"),
        Triple("下签", "雾锁重山路未通，且收心神待好风。", "宜守不宜进，低调蓄力为上。"),
        Triple("下签", "残花落尽待明春，莫为眼前一时嗔。", "运势低迷，静守本心、勿与人争。"),
        Triple("下下签", "夜半挑灯影自单，谨防小人暗里看。", "诸事谨慎，远离口舌、莫信浮言。"),
        Triple("上签", "金风玉露一相逢，便胜却人间无数。", "情缘人际有喜，宜坦诚相待。"),
        Triple("中平签", "行到水穷处，坐看云起时。", "进退自如，顺其自然最相宜。")
    )

    private val COLORS = listOf("朱红", "明黄", "青碧", "月白", "松绿", "玄黑", "金银", "紫檀")
    private val GOODS = listOf("会友", "签约", "出行", "读书", "表白", "理财", "静思", "助人")
    private val AVOIDS = listOf("争执", "冒进", "熬夜", "借出", "迁居", "贪杯", "拖延", "轻诺")
    private val MANUAL_TAUNTS = listOf(
        "哟，今日签都定了还想重抽？本半仙看你是想把命运当自助餐。",
        "再抽也不会更灵，只会让本半仙多笑三声。",
        "这支只是彩蛋，别拿它跟今天正牌运势吵架。",
        "手速挺快，可惜天庭档案已经盖好章了。",
        "抽吧抽吧，反正本半仙只负责阴阳，不负责改命。",
        "你这不是求签，是想跟今天讨价还价。"
    )

    fun generate(seed: Long = Random.nextLong()): OracleResult {
        val rnd = Random(seed)
        val (level, poem, advice) = POEMS[rnd.nextInt(POEMS.size)]
        return OracleResult(
            level = level,
            poem = poem,
            luckyNumber = 1 + rnd.nextInt(9),
            luckyColor = COLORS[rnd.nextInt(COLORS.size)],
            good = GOODS[rnd.nextInt(GOODS.size)],
            avoid = AVOIDS[rnd.nextInt(AVOIDS.size)],
            advice = advice
        )
    }

    /** 从同一固定签库再抽一签 */
    fun randomDraw(): OracleResult = generate()

    /** 手动彩蛋不改当日签；半仙负责吐槽这个行为本身。 */
    fun manualTaunt(): String = MANUAL_TAUNTS[Random.nextInt(MANUAL_TAUNTS.size)]
}
