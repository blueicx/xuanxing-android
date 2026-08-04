package com.xuanji.app.domain.divination

import java.time.LocalDate
import java.lang.Math.floorMod

/**
 * 泰国那迦占雨（Naga Offering Water）确定性计算。
 *
 * 基于用户的 Python 基础实现移植并完善：
 *  - 佛历年份（BE）= 公历 + 543；
 *  - 生肖以佛历年份计算（基准：佛历 2500 年 = 1957 年 = 鸡年 index 9）；
 *  - 每个生肖年对应「那迦数量」与平地/森林/山区/空中降雨量及年度农业解读；
 *  - 那迦数量 2-7 条对应年度雨水寓意与农事建议。
 * 全部离线、确定性，无随机，仅供文化娱乐参考。
 */

// ======================== 数据 ========================

/** 泰国十二生肖（以那迦 Naga 取代龙） */
val THAI_ZODIAC: List<String> = listOf(
    "鼠 (Chuat/ชวด)", "牛 (Chalu/ฉลู)", "虎 (Khan/ขาล)", "兔 (Tho/เถาะ)",
    "那迦 (Marong/มะโรง)", "蛇 (Maseng/มะเส็ง)", "马 (Mamia/มะเมีย)", "羊 (Mamae/มะแม)",
    "猴 (Wok/วอก)", "鸡 (Raka/ระกา)", "狗 (Cho/จอ)", "猪 (Kun/กุน)"
)

/** 生肖年 → 那迦占雨数据 */
private data class NagaRainData(
    val nagaCount: Int,
    val rainEarth: Int? = null,     // 平地降雨量
    val rainForest: Int? = null,    // 森林降雨量
    val rainMountain: Int? = null,  // 山区降雨量
    val rainAir: Int? = null,       // 空中降雨量
    val interpretation: String
)

private val NAGA_RAIN_DATA: Map<Int, NagaRainData> = mapOf(
    0 to NagaRainData(3, rainEarth = 200, rainForest = 100, interpretation = "雨水适中，稻米丰饶。低洼稻田在7、8、9月水量充足，适合年初耕作。"),
    1 to NagaRainData(6, rainEarth = 200, rainMountain = 100, interpretation = "晚稻收成好，6、8月水量充足。宜在8月月亏后的第8天插秧。"),
    2 to NagaRainData(7, rainEarth = 900, rainForest = 100, interpretation = "早稻好而晚稻差。宜在7月播种，适合年初耕作。"),
    3 to NagaRainData(2, rainEarth = 100, rainForest = 200, interpretation = "早稻收成好。宜在7、8月播种。"),
    4 to NagaRainData(3, rainAir = 100, rainEarth = 500, interpretation = "晚稻丰饶。10月有小雨，12月有大雨。"),
    5 to NagaRainData(5, rainEarth = 500, interpretation = "稻谷长势良好但产量可能较低。5、6、7、8月有雨。"),
    6 to NagaRainData(4, rainEarth = 500, rainForest = 100, interpretation = "年中稻谷长势良好，晚稻较差，早稻尚可。"),
    7 to NagaRainData(4, rainEarth = 500, rainMountain = 200, interpretation = "晚稻可能歉收。5、6月开始降雨，9、10月有大雨。"),
    8 to NagaRainData(5, rainEarth = 400, rainForest = 300, interpretation = "雨水分布均匀，稻谷收成中等。"),
    9 to NagaRainData(6, rainEarth = 300, rainForest = 200, interpretation = "早稻较好，晚稻一般。注意7、8月的降雨。"),
    10 to NagaRainData(7, rainEarth = 600, rainForest = 200, interpretation = "雨水充沛，稻谷收成好。但需防涝。"),
    11 to NagaRainData(2, rainEarth = 100, rainForest = 100, interpretation = "雨水较少，可能干旱。需节约用水，种植耐旱作物。")
)

/** 那迦数量 → 年度寓意 */
private val NAGA_COUNT_MEANING = mapOf(
    2 to "雨水稀少，需警惕旱情",
    3 to "雨水适中，风调雨顺",
    4 to "雨水尚可，但分布不均",
    5 to "雨水较多，需防涝",
    6 to "雨水充沛，利于农业",
    7 to "雨水极多，警惕水灾"
)

/** 六维解读标签（总评/事业/财运/感情/健康/建议） */
private val DIM_LABELS = listOf("总评", "事业", "财运", "感情", "健康", "建议")

/** 将六个维度的文本拼成带「」标签的多行解读 */
private fun joinReading(lines: List<String>): String =
    DIM_LABELS.mapIndexed { i, label -> "「$label」${lines[i]}" }.joinToString("\n")

/** 依据那迦数量与降雨分布生成六维年度解读（贴合占雨农事主题） */
private fun buildReading(data: NagaRainData, count: Int): String {
    val overall = when {
        count <= 2 -> "雨水稀缺之年，旱情风险偏高，全年须以「节水」二字为纲。"
        count == 3 -> "雨水适中之年，风调雨顺的概率最大，五谷有望丰登。"
        count <= 5 -> "雨水偏多年份，涝忧渐显，喜忧参半，须防降雨分布不均。"
        else -> "雨水极为充沛之年，丰收可期，但水患之患须臾不可放松。"
    }
    val career = when {
        (data.rainEarth != null && data.rainEarth >= 500) -> "水足土润，农事与经营皆可大展拳脚，宜趁墒情抢种、积极开工。"
        (data.rainEarth != null && data.rainEarth <= 200) -> "墒情偏旱，耕作与创业宜保守推进，此年蓄力大于发力。"
        else -> "雨水尚可，耕耘按部就班，稳中求进方为上策。"
    }
    val wealth = when {
        (data.rainForest != null && data.rainForest >= 200) -> "山林雨沛，林产与副业财源看好，多元经营反有意外之喜。"
        count >= 5 -> "丰水年份收成厚，财运随之走高，但须防洪损与贱卖之失。"
        else -> "收成平平之年，理财宜守，忌囤积居奇与冒进投资。"
    }
    val love = when {
        (data.rainMountain != null && data.rainMountain >= 200) -> "山雨润泽，情感如溪流绵长，宜细水长流地经营关系。"
        count <= 3 -> "雨水稀薄，情感易现干涸，须主动浇灌、多些陪伴与倾诉。"
        else -> "雨水丰沛，人缘与情感皆和顺，宜趁势加深情谊、广结善缘。"
    }
    val health = when {
        count >= 6 -> "雨多湿重，注意风湿、湿疹与霉变侵扰，居处宜防潮除湿。"
        count <= 3 -> "天干物燥，注意呼吸道与皮肤干燥，宜多补水润养。"
        else -> "气候居中，身心平稳，惟农忙之际须防过劳伤身。"
    }
    val advice = "结合${data.interpretation} 此年" + when {
        count <= 3 -> "宜提前布局灌溉设施、改种耐旱作物，未雨绸缪方能稳收。"
        count <= 5 -> "宜按计划耕作并留意雨讯，适期播种、及时排水，防灾于未然。"
        else -> "宜疏通沟渠、加固田埂，抢晴抢收，防洪排涝一刻不可懈怠。"
    }
    return joinReading(listOf(overall, career, wealth, love, health, advice))
}

// ======================== 结果模型 ========================

data class NagaRainResult(
    val yearBe: Int,        // 佛历年份
    val yearCe: Int,        // 公历年份
    val zodiacIndex: Int,
    val zodiac: String,     // 生肖（含泰文）
    val nagaCount: Int,
    val nagaMeaning: String,
    val rainEarth: Int?,    // 平地降雨量（Ha）
    val rainForest: Int?,
    val rainMountain: Int?,
    val rainAir: Int?,
    val interpretation: String,
    val farmingAdvice: String
)

// ======================== 核心计算 ========================

object NagaRain {

    /** 佛历年份 → 生肖索引（基准：佛历 2500 年 = 鸡年 index 9；floorMod 防负） */
    fun zodiacIndex(yearBe: Int): Int = floorMod(yearBe - 2500 + 9, 12)

    /** 指定公历年份的降雨预测（未给年份则取今年） */
    fun predict(yearCe: Int = LocalDate.now().year): NagaRainResult {
        val yearBe = yearCe + 543
        val zi = zodiacIndex(yearBe)
        val data = NAGA_RAIN_DATA[zi]
        val count = data?.nagaCount ?: 0
        return NagaRainResult(
            yearBe = yearBe,
            yearCe = yearCe,
            zodiacIndex = zi,
            zodiac = THAI_ZODIAC[zi],
            nagaCount = count,
            nagaMeaning = NAGA_COUNT_MEANING[count] ?: "未知",
            rainEarth = data?.rainEarth,
            rainForest = data?.rainForest,
            rainMountain = data?.rainMountain,
            rainAir = data?.rainAir,
            interpretation = data?.let { buildReading(it, count) } ?: "暂无详细解读",
            farmingAdvice = when {
                count <= 3 -> "需注意节水与灌溉，种植耐旱作物。"
                count <= 5 -> "可按正常计划耕作，留意降雨分布。"
                else -> "需防范洪涝灾害，注意排涝与加固田埂。"
            }
        )
    }

    /** 十二生肖年那迦数量一览（供展示） */
    fun countTable(): List<Triple<String, Int, String>> =
        THAI_ZODIAC.mapIndexed { i, z ->
            val c = NAGA_RAIN_DATA[i]?.nagaCount ?: 0
            Triple(z, c, NAGA_COUNT_MEANING[c] ?: "未知")
        }
}
