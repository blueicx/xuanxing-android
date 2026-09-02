package com.xuanji.app.domain.divination

import com.xuanji.app.domain.MysticGuideGenerator
import kotlin.random.Random

/** 签面档次：由 POEMS 逐条声明，不再由 level 字符串的 when 猜。 */
enum class OracleTier { High, Mid, Low }

/**
 * 灵签的两位观察者。modeKey 与陪伴侧 persona 共用同一套键，称谓只在渲染时向
 * `MysticGuideGenerator.personaName` 取，因此改名不会再静默换成另一面的口吻。
 */
enum class OracleRole(val modeKey: String) {
    Scholar("scholar"),
    Half("half");

    val other: OracleRole get() = if (this == Scholar) Half else Scholar

    val label: String get() = MysticGuideGenerator.personaName(modeKey)
}

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
    ) {
        /** 只算不存：给缓存记录加字段会让旧 JSON 反序列化出 null，静默改掉当天正在显示的签。 */
        val tier: OracleTier get() = TIER_BY_LEVEL.getValue(level)
    }

    data class OracleReaction(val roleName: String, val line: String)
    data class OracleObserverChoice(val key: String, val label: String)
    data class OracleExchange(val roleName: String, val line: String, val exitLine: String)
    data class OracleRelay(val roleName: String, val line: String)

    private data class OraclePoem(
        val tier: OracleTier,
        val level: String,
        val poem: String,
        val advice: String
    )

    private val POEMS = listOf(
        OraclePoem(OracleTier.High, "上上签", "云开见月正分明，谋望求财事有成。", "诸事顺遂，宜把握良机、主动出击。"),
        OraclePoem(OracleTier.High, "上上签", "乘风破浪会有时，直挂云帆济沧海。", "运势上扬，敢想敢为必有所得。"),
        OraclePoem(OracleTier.High, "上签", "花开富贵满庭芳，喜气盈门日渐长。", "平稳向好，宜守正待时、广结善缘。"),
        OraclePoem(OracleTier.High, "上签", "青山绿水景无边，心宽自有福绵绵。", "心境开阔，贵人暗助，事多顺心。"),
        OraclePoem(OracleTier.Mid, "中平签", "风恬浪静好行舟，守得云开见月柔。", "宜稳不宜冒，循序而行可保平安。"),
        OraclePoem(OracleTier.Mid, "中平签", "半江明月半江风，得失随缘莫强求。", "随遇而安，凡事留三分余地。"),
        OraclePoem(OracleTier.Mid, "中平签", "柳暗花明又一村，困顿之中亦有门。", "转机将现，耐心可渡难关。"),
        OraclePoem(OracleTier.Low, "下签", "雾锁重山路未通，且收心神待好风。", "宜守不宜进，低调蓄力为上。"),
        OraclePoem(OracleTier.Low, "下签", "残花落尽待明春，莫为眼前一时嗔。", "运势低迷，静守本心、勿与人争。"),
        OraclePoem(OracleTier.Low, "下下签", "夜半挑灯影自单，谨防小人暗里看。", "诸事谨慎，远离口舌、莫信浮言。"),
        OraclePoem(OracleTier.High, "上签", "金风玉露一相逢，便胜却人间无数。", "情缘人际有喜，宜坦诚相待。"),
        OraclePoem(OracleTier.Mid, "中平签", "行到水穷处，坐看云起时。", "进退自如，顺其自然最相宜。")
    )

    private val TIER_BY_LEVEL: Map<String, OracleTier> = POEMS.associate { it.level to it.tier }

    private val COLORS = listOf("朱红", "明黄", "青碧", "月白", "松绿", "玄黑", "金银", "紫檀")
    private val GOODS = listOf("会友", "签约", "出行", "读书", "表白", "理财", "静思", "助人")
    private val AVOIDS = listOf("争执", "冒进", "熬夜", "借出", "迁居", "贪杯", "拖延", "轻诺")

    fun generate(seed: Long = Random.nextLong()): OracleResult {
        val rnd = Random(seed)
        val drawn = POEMS[rnd.nextInt(POEMS.size)]
        return OracleResult(
            level = drawn.level,
            poem = drawn.poem,
            luckyNumber = 1 + rnd.nextInt(9),
            luckyColor = COLORS[rnd.nextInt(COLORS.size)],
            good = GOODS[rnd.nextInt(GOODS.size)],
            avoid = AVOIDS[rnd.nextInt(AVOIDS.size)],
            advice = drawn.advice
        )
    }

    /** 从同一固定签库再抽一签 */
    fun randomDraw(): OracleResult = generate()

    fun observerChoices(): List<OracleObserverChoice> = listOf(
        OracleObserverChoice("why", "问依据"),
        OracleObserverChoice("accept", "收下"),
        OracleObserverChoice("pushback", "拦一句")
    )

    fun observerExchange(draw: OracleResult, choiceKey: String): OracleExchange? {
        if (observerChoices().none { it.key == choiceKey }) return null

        val tier = draw.tier
        val role = oracleRole(draw)

        val line = when (tier) {
            OracleTier.High -> when (role) {
                OracleRole.Scholar -> when (choiceKey) {
                    "why" -> "凭签面只说今天顺，没说永远顺；你那支「${draw.good}」还在宜里，真正的证据是你怎么做。"
                    "accept" -> "那就记这一句：亮的时候更要收着走；${draw.luckyColor}可以当个提醒色。"
                    else -> "好，我不盯着；只留一句——${draw.luckyColor}别变成逞强的旗子。"
                }
                OracleRole.Half -> when (choiceKey) {
                    "why" -> "凭什么？凭你自己抽到「${draw.good}」也怕「${draw.avoid}」；本半仙只是把这两头摆出来。"
                    "accept" -> "收好了就别飘；今日${draw.luckyColor}是提醒色，不是免检章。"
                    else -> "行行行，我退半步；可${draw.avoid}这根线，本半仙先替你拉着。"
                }
            }
            OracleTier.Mid -> when (role) {
                OracleRole.Scholar -> when (choiceKey) {
                    "why" -> "依据就是中平本身：不夸你，也不吓你；把「${draw.avoid}」绕开，路会清楚些。"
                    "accept" -> "稳着收下就好；今天不必逼自己把每一步都走成答案。"
                    else -> "我不盯，只提醒一句：中平最怕被急事推着走。"
                }
                OracleRole.Half -> when (choiceKey) {
                    "why" -> "本半仙看的是两头——「${draw.good}」能试，「${draw.avoid}」别碰；这不叫玄，叫省事。"
                    "accept" -> "肯记下也算识相；慢慢来，别拿中平当偷懒的借口。"
                    else -> "好好好，我不念了；台阶就在那儿，你自己看着踩。"
                }
            }
            OracleTier.Low -> when (role) {
                OracleRole.Scholar -> when (choiceKey) {
                    "why" -> "不是判你不行；签里让你避开「${draw.avoid}」，是把风险说在前头。"
                    "accept" -> "嗯，先把步子放小；${draw.luckyColor}不用当护身符，当成休息提示就行。"
                    else -> "好，我只陪到这里；若要继续，也别急着跟坏签硬碰。"
                }
                OracleRole.Half -> when (choiceKey) {
                    "why" -> "别皱眉，本半仙不是笑你惨；是看见你还愿意问「${draw.avoid}」怎么避。"
                    "accept" -> "行，蔫签也能翻页；今天少碰「${draw.avoid}」，先给自己留口气。"
                    else -> "退退退，不催你；可本半仙还在这儿，等你缓过劲再呛两句。"
                }
            }
        }

        val exitLine = when (role to choiceKey) {
            OracleRole.Scholar to "why" -> "把签纸抚平后离开，像把问题也折进了页边。"
            OracleRole.Scholar to "accept" -> "点头记完一笔，脚步放轻地退开。"
            OracleRole.Scholar to "pushback" -> "抬手示意不扰，转身时仍留了半步距离。"
            OracleRole.Half to "why" -> "咂了下嘴，甩着袖子走了，嘴上还嘀咕「算你有心」。"
            OracleRole.Half to "accept" -> "哼了一声，倒背着手晃出门去。"
            else -> "耸耸肩退到帘外，临走还挑了下眉。"
        }

        return OracleExchange(roleName = role.label, line = line, exitLine = exitLine)
    }

    /** 另一位玄师在离席后偶尔从旁补一句；同一支签和同一选择永远固定。 */
    fun observerRelay(draw: OracleResult, choiceKey: String): OracleRelay? {
        if (observerChoices().none { it.key == choiceKey }) return null
        if (observerRelayGate(draw, choiceKey) != 0) return null

        val firstRole = oracleRole(draw)
        val secondRole = firstRole.other
        val tier = draw.tier
        val line = when (tier) {
            OracleTier.High -> when (firstRole to choiceKey) {
                OracleRole.Scholar to "why" -> "问得倒认真；可「${draw.good}」不是保证书，别顺手把「${draw.avoid}」也招过来。"
                OracleRole.Scholar to "accept" -> "收这么快？行，${draw.luckyColor}先带着；别把顺日当成免检章。"
                OracleRole.Scholar to "pushback" -> "哟，还敢拦一句？有脾气；那就按住「${draw.avoid}」，别赢了面子丢了里子。"
                OracleRole.Half to "why" -> "刚才那句粗里有数；「${draw.good}」可以试，「${draw.avoid}」仍要绕开。"
                OracleRole.Half to "accept" -> "收得刚好。${draw.luckyColor}当提醒就够，不必把顺日演成庆典。"
                else -> "拦得住嘴，拦不住日子。亮处更要走稳，别急着加码。"
            }
            OracleTier.Mid -> when (firstRole to choiceKey) {
                OracleRole.Scholar to "why" -> "中平的依据就在眼前；避开「${draw.avoid}」，比再算十遍都实在。"
                OracleRole.Scholar to "accept" -> "肯稳着收就好；今天不用把每件事都问出个高低。"
                OracleRole.Scholar to "pushback" -> "行，你们俩都别急；中平最怕被人催成冒进。"
                OracleRole.Half to "why" -> "我刚瞄了一眼，两头都在纸上——「${draw.good}」可试，「${draw.avoid}」别碰。"
                OracleRole.Half to "accept" -> "识相。慢慢来，别拿中平当躺平的理由。"
                else -> "好好好，帘外不吵；台阶留给你自己踩。"
            }
            OracleTier.Low -> when (firstRole to choiceKey) {
                OracleRole.Scholar to "why" -> "那不是笑你；能问怎么避「${draw.avoid}」，就已经比闷着头强。"
                OracleRole.Scholar to "accept" -> "先把步子放小。${draw.luckyColor}不当护身符，当休息提示就行。"
                OracleRole.Scholar to "pushback" -> "不吵不吵；坏签也不是命令，缓口气再翻页。"
                OracleRole.Half to "why" -> "别皱眉，签沉不代表你不行；先绕开「${draw.avoid}」，剩下的交给时间。"
                OracleRole.Half to "accept" -> "行，认栽但不服输；今天少耗一口气，明天才有力气呛回来。"
                else -> "退退退，不逼你；坏签只提醒风险，不给你定罪。"
            }
        }
        return OracleRelay(roleName = secondRole.label, line = line)
    }

    fun dailyReaction(draw: OracleResult): OracleReaction {
        val tier = draw.tier
        val role = oracleRole(draw)

        return when (tier to role) {
            OracleTier.High to OracleRole.Scholar -> OracleReaction(
                roleName = role.label,
                line = "今日签面确实亮；我把要点记在旁边，别急着把它当成通行证。"
            )
            OracleTier.High to OracleRole.Half -> OracleReaction(
                roleName = role.label,
                line = "哟，签面挺会挑日子？先别飘，本半仙看看你能不能接住。"
            )
            OracleTier.Mid to OracleRole.Scholar -> OracleReaction(
                roleName = role.label,
                line = "今日签不急不缓，正好看你怎么走；稳着来就好。"
            )
            OracleTier.Mid to OracleRole.Half -> OracleReaction(
                roleName = role.label,
                line = "不上不下的签？行吧，本半仙先看看你会不会自己找台阶。"
            )
            OracleTier.Low to OracleRole.Scholar -> OracleReaction(
                roleName = role.label,
                line = "签面沉一点而已，不是终局；今天把步子放小，我在旁边看着。"
            )
            else -> OracleReaction(
                roleName = role.label,
                line = "签是有点蔫，但别急着给自己判刑；本半仙还等着看你翻页呢。"
            )
        }
    }

    fun manualReaction(draw: OracleResult): OracleReaction {
        val tier = draw.tier
        val role = oracleRole(draw)

        return when (tier to role) {
            OracleTier.High to OracleRole.Scholar -> OracleReaction(
                roleName = role.label,
                line = "彩蛋倒是亮堂；今日正签已经收好，别把这份当成加码的理由。"
            )
            OracleTier.High to OracleRole.Half -> OracleReaction(
                roleName = role.label,
                line = "哟，彩蛋也敢这么体面？正签可没答应帮你续杯，别得意。"
            )
            OracleTier.Mid to OracleRole.Scholar -> OracleReaction(
                roleName = role.label,
                line = "彩蛋平平也好，正好当对照；今天还是按正签慢慢走。"
            )
            OracleTier.Mid to OracleRole.Half -> OracleReaction(
                roleName = role.label,
                line = "中不溜的彩蛋，看看就行；本半仙可不许你拿它跟正签讨价还价。"
            )
            OracleTier.Low to OracleRole.Scholar -> OracleReaction(
                roleName = role.label,
                line = "这支只是彩蛋，不算数；先把今天的节奏放轻一点，别被它带紧张。"
            )
            else -> OracleReaction(
                roleName = role.label,
                line = "咳，彩蛋抽得有点蔫？别慌，正签才是今天的主角，本半仙盯着呢。"
            )
        }
    }

    private fun oracleRole(draw: OracleResult): OracleRole =
        if (draw.luckyNumber % 2 == 0) OracleRole.Scholar else OracleRole.Half

    private fun observerRelayGate(draw: OracleResult, choiceKey: String): Int {
        val signature = listOf(
            draw.level, draw.poem, "${draw.luckyNumber}", draw.luckyColor,
            draw.good, draw.avoid, draw.advice, choiceKey, "oracle-relay"
        ).joinToString("|")
        var hash = 5381
        for (ch in signature) hash = hash * 33 + ch.code
        return ((hash + draw.luckyNumber) % 4 + 4) % 4
    }
}
