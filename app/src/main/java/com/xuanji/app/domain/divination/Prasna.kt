package com.xuanji.app.domain.divination

import java.time.LocalDateTime

/**
 * 普拉萨那（Prasna，印度卜卦占星简化版）：
 * 基于提问时刻起盘——分钟数定上升宫位，行星按固定间隔分布，
 * 按提问内容（感情/事业/其他）给出针对性解读。确定性算法，离线可用。
 */
object Prasna {

    private val PLANETS = listOf("太阳", "月亮", "火星", "水星", "木星", "金星", "土星")
    private val HOUSES = listOf(
        "自我", "财富", "沟通", "家庭", "创造", "健康",
        "关系", "转变", "哲学", "事业", "社群", "灵性"
    )

    private val HOUSE_MEANING = mapOf(
        "自我" to "关于你的个人形象和方向。",
        "财富" to "关于金钱、资源和价值观。",
        "沟通" to "关于学习、写作和短途旅行。",
        "家庭" to "关于家庭、根基和内在安全感。",
        "创造" to "关于子女、娱乐和创造力。",
        "健康" to "关于身体、工作和日常事务。",
        "关系" to "关于伴侣、合作伙伴和公开敌人。",
        "转变" to "关于生死、遗产和深度心理。",
        "哲学" to "关于旅行、高等教育和信仰。",
        "事业" to "关于社会地位、职业和成就。",
        "社群" to "关于朋友、团体和理想。",
        "灵性" to "关于潜意识、秘密和灵性解脱。"
    )

    data class PlanetPos(val planet: String, val degree: Double, val house: String)

    data class PrasnaResult(
        val askTime: LocalDateTime,
        val question: String,
        val ascHouse: String,
        val ascHouseIndex: Int,
        val planets: List<PlanetPos>,
        val strongPlanets: List<String>,
        val detail: String
    )

    fun calculate(question: String, askTime: LocalDateTime): PrasnaResult {
        val totalMin = askTime.hour * 60 + askTime.minute
        val ascIndex = (totalMin / 30) % 12
        val ascHouse = HOUSES[ascIndex]

        val planets = PLANETS.mapIndexed { i, p ->
            val pos = (i * 30 + totalMin / 10) % 360
            PlanetPos(p, pos.toDouble(), HOUSES[(pos / 30).toInt() % 12])
        }

        val strong = planets.filter { HOUSES.indexOf(it.house) in setOf(0, 3, 6, 9) }.map { it.planet }

        val detail = buildDetail(question, ascHouse, planets, strong)

        return PrasnaResult(
            askTime = askTime,
            question = question,
            ascHouse = ascHouse,
            ascHouseIndex = ascIndex,
            planets = planets,
            strongPlanets = strong,
            detail = detail
        )
    }

    /** 六维解读：总评/事业/财运/感情/健康/建议（贴合卜卦主题，确定性拼接） */
    private fun buildDetail(
        question: String,
        ascHouse: String,
        planets: List<PlanetPos>,
        strong: List<String>
    ): String {
        fun houseOf(name: String): String {
            val p = planets.first { it.planet == name }
            return p.house
        }
        val sunH = houseOf("太阳")
        val moonH = houseOf("月亮")
        val marsH = houseOf("火星")
        val jupH = houseOf("木星")
        val venH = houseOf("金星")

        val overall = "此问起于「$question」，上升点落「$ascHouse」宫——${HOUSE_MEANING.getValue(ascHouse)} 大方向可循，宜顺此势而行。"
        val career = when {
            sunH == "事业" -> "太阳坐守事业宫，自我实现欲强，有望升迁上位，宜主动担当、谋定后动。"
            marsH == "事业" -> "火星催动事业宫，行动力爆棚，宜趁热打铁推进大计划，忌半途而废。"
            sunH == "财富" -> "太阳落财帛宫，事业与财路相连，实干即生财，宜以业绩说话。"
            sunH == "自我" -> "太阳落自我宫，自我意志强大，宜先立人设再立事业，切忌刚愎自用。"
            else -> "事业星落「$sunH」宫，所谋之事宜围绕此宫领域展开，顺势则顺。"
        }
        val wealth = when {
            jupH == "财富" -> "木星坐守财帛宫，正财亨通、或有意外之喜，宜把握扩张良机。"
            venH == "财富" -> "金星临财帛宫，人脉生财，审美与社交皆可转化为收益。"
            jupH == "事业" -> "财富来自事业成就，升职加薪之象，宜深耕本职、以业求财。"
            else -> "财星落「$jupH」宫，财运起伏与「$jupH」领域密切相关，宜稳健理财。"
        }
        val love = when {
            venH == "关系" -> "金星落关系宫，情感交流顺畅，关系可望升温，惟忌口角。"
            venH == "创造" -> "金星落创造宫，浪漫氛围浓厚，宜以惊喜与陪伴滋养感情。"
            venH == "家庭" -> "金星落家庭宫，感情与家人牵绊深，宜先安内再谈情。"
            venH == "事业" -> "情缘或与职场相关，若涉同僚之谊，须谨慎权衡公私。"
            else -> "金星落「$venH」宫，感情走向受「$venH」领域能量影响，宜多用心经营。"
        }
        val health = when {
            moonH == "健康" -> "月亮落健康宫，情绪与身体互相牵动，宜注意作息与情绪管理。"
            marsH == "健康" -> "火星落健康宫，精力旺盛但易透支，防炎症与意外磕碰。"
            moonH == "家庭" -> "月亮落家庭宫，宜以家庭为养生之所，居家静养最宜。"
            else -> "健康星落「$moonH」宫，宜关照「$moonH」对应的身体部位，防微杜渐。"
        }
        val advice = if (strong.isEmpty()) {
            "本盘无明显强势行星，宜平心静气、守时待变，勿急勿躁。"
        } else {
            "本盘强势行星为${strong.joinToString("、")}，宜借其力顺势而为，择吉时行动，事半功倍。"
        }
        return "「总评」$overall\n「事业」$career\n「财运」$wealth\n「感情」$love\n「健康」$health\n「建议」$advice"
    }
}
