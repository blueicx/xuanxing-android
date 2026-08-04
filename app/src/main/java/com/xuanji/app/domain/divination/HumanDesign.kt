package com.xuanji.app.domain.divination

import com.xuanji.app.data.model.UserProfile
import com.xuanji.app.domain.ZodiacCalculator
import kotlin.math.floor

/**
 * 人类图（Human Design，近似演示）：
 * 复用本命星盘把日月等行星的黄经映射到《易经》64 卦之门，再按中心分组判定「有定义/开放」，
 * 据此推演类型（Type）与权威（Authority）。此为离线简化近似，非完整人类图计算，仅供娱乐参考。
 */
object HumanDesign {

    data class Center(
        val nameCn: String,
        val nameEn: String,
        val defined: Boolean,
        val desc: String
    )

    data class HumanDesignResult(
        val type: String,
        val typeDesc: String,
        val strategy: String,
        val notSelf: String,
        val authority: String,
        val authorityDesc: String,
        val profile: String,
        val profileDesc: String,
        val definition: String,
        val definitionDesc: String,
        val centers: List<Center>,
        val gates: List<Int>,
        val channels: List<String>,
        val verdict: String,
        val note: String
    )

    // 9 大能量中心（名称 / 简释）
    private val CENTERS = listOf(
        Center("头顶中心", "Crown", false, "灵思、压力与意义"),
        Center("眉心中心", "Ajna", false, "心智、分析与笃定"),
        Center("喉部中心", "Throat", false, "表达与行动之门"),
        Center("G 中心", "Self", false, "爱、方向与身份"),
        Center("情绪中心", "Solar Plexus", false, "情绪波浪"),
        Center("荐骨中心", "Sacral", false, "生命力与劳作"),
        Center("脾中心", "Spleen", false, "直觉、免疫与恐惧"),
        Center("心力中心", "Heart/Ego", false, "意志与承诺"),
        Center("根部中心", "Root", false, "压力与驱动力")
    )

    private val TYPE_DESC = mapOf(
        "显示者" to "发起行动、打破僵局，宜先告知再行动。",
        "生产者" to "以荐骨回应世界，宜等待被邀、顺应能量。",
        "显示生产者" to "兼具发起与回应，效率高但宜先回应再行动。",
        "投射者" to "识人善任的引导者，宜等待被邀请再付出。",
        "反映者" to "如月光映照群体，宜以 Lunar 周期（约 28 天）做决定。"
    )
    // 类型 → 人生策略 / 非自己主题
    private val TYPE_STRATEGY = mapOf(
        "显示者" to ("告知（Inform）" to "愤怒（Anger）"),
        "生产者" to ("回应（Respond）" to "挫败（Frustration）"),
        "显示生产者" to ("告知与回应（Inform & Respond）" to "挫败（Frustration）"),
        "投射者" to ("等待邀请（Wait for Invitation）" to "苦涩（Bitterness）"),
        "反映者" to ("等待 28 天周期（Wait 28 Days）" to "失望（Disappointment）")
    )
    private val AUTHORITY_DESC = mapOf(
        "情绪权威" to "重大决定须等情绪波浪落定、清晰后再定。",
        "荐骨权威" to "用身体「嗯/啊」的直觉回应来做决定。",
        "脾权威" to "瞬间的觉察与恐惧管理，当下即知。",
        "自我权威" to "以 G 中心的真实方向感做决定。",
        "反射权威" to "借 Lunar 周期（约 28 天）沉淀后回应环境。"
    )
    // 十二种人生角色
    private val PROFILE_DESC = mapOf(
        "1/3" to "研究者/殉道者——探索与实验",
        "1/4" to "研究者/机会主义者——探索与连接",
        "2/4" to "隐士/机会主义者——独处与连接",
        "2/5" to "隐士/异端者——独处与影响",
        "3/5" to "殉道者/异端者——实验与影响",
        "3/6" to "殉道者/人生典范——实验与榜样",
        "4/6" to "机会主义者/人生典范——连接与榜样",
        "4/1" to "机会主义者/研究者——连接与探索",
        "5/1" to "异端者/研究者——影响与探索",
        "5/2" to "异端者/隐士——影响与独处",
        "6/2" to "人生典范/隐士——榜样与独处",
        "6/3" to "人生典范/殉道者——榜样与实验"
    )
    // 定义类型
    private val DEFINITION_DESC = mapOf(
        "单一定义" to "所有定义的能量中心相互连接，能量流畅。",
        "分裂定义" to "能量中心分为两个独立集群，需他人桥接。",
        "三重分裂定义" to "能量中心分为三个独立集群，需多人桥接。",
        "四重分裂定义" to "能量中心分为四个独立集群，需群体桥接。",
        "无定义" to "所有能量中心均为开放（反映者）。"
    )
    // 模拟通道（按出生日奇偶激活）
    private val ALL_CHANNELS = listOf(
        "通道 1：头顶→逻辑（个体电路）",
        "通道 2：G 中心→喉咙（社会电路）",
        "通道 3：荐骨→情绪（部落电路）",
        "通道 4：荐骨→喉咙（生成电路）",
        "通道 5：根部→情绪（压力电路）",
        "通道 6：脾→喉咙（直觉电路）"
    )

    fun cast(profile: UserProfile): HumanDesignResult {
        val chart = ZodiacCalculator.calculateNatalChart(
            profile.birthYear, profile.birthMonth, profile.birthDay,
            profile.birthHour, profile.birthMinute, profile.locationName
        )
        val map = chart.planets.associateBy { it.name }
        val planets = listOf("太阳", "月亮", "水星", "金星", "火星", "木星", "土星", "北交")
        val gates = planets.mapNotNull { map[it]?.let { p -> longitudeToGate(p.longitude) } }.distinct()

        val definedFlags = BooleanArray(9) { false }
        gates.forEach { g ->
            definedFlags[(g - 1) % 9] = true
        }

        val centers = CENTERS.mapIndexed { i, c -> c.copy(defined = definedFlags[i]) }
        val sacral = definedFlags[5]
        val throat = definedFlags[2]
        val root = definedFlags[8]
        val heart = definedFlags[7]
        val solar = definedFlags[4]
        val spleen = definedFlags[6]
        val self = definedFlags[3]
        val anyDefined = definedFlags.any { it }

        val (type, authority) = when {
            sacral && throat && (root || heart || solar) -> "显示生产者" to authorityOf(solar, sacral, spleen, self)
            sacral && throat -> "生产者" to authorityOf(solar, sacral, spleen, self)
            sacral -> "生产者" to authorityOf(solar, sacral, spleen, self)
            throat && (self || heart) -> "显示者" to authorityOf(solar, sacral, spleen, self)
            anyDefined -> "投射者" to authorityOf(solar, sacral, spleen, self)
            else -> "反映者" to "反射权威"
        }

        // 人生角色（按出生日确定性，用户算法：day_of_year % 6 + 1 / hour % 3 + 1）
        val dayOfYear = java.time.LocalDate.of(
            profile.birthYear, profile.birthMonth, profile.birthDay
        ).dayOfYear
        val hour = profile.birthHour
        val p1 = dayOfYear % 6 + 1
        val p2 = hour % 3 + 1
        val profileKey = "$p1/$p2"
        val profileDesc = PROFILE_DESC[profileKey] ?: PROFILE_DESC.getValue("1/3")

        // 定义类型（按已定义中心数，用户算法）
        val definedCount = definedFlags.count { it }
        val definition = when {
            definedCount == 0 -> "无定义"
            definedCount <= 4 -> "单一定义"
            definedCount <= 7 -> "分裂定义"
            definedCount <= 9 -> "三重分裂定义"
            else -> "四重分裂定义"
        }

        // 模拟通道（按出生日奇偶激活）
        val channels = ALL_CHANNELS.filterIndexed { i, _ ->
            (profile.birthDay + i) % 2 == 0
        }

        val strategy = TYPE_STRATEGY.getValue(type).first
        val notSelf = TYPE_STRATEGY.getValue(type).second
        val authorityDesc = AUTHORITY_DESC.getValue(authority)
        val definitionDesc = DEFINITION_DESC.getValue(definition)

        val verdict = buildVerdict(
            type, strategy, notSelf, authority, authorityDesc,
            profileKey, profileDesc, definition, definitionDesc, centers, gates, channels
        )

        return HumanDesignResult(
            type = type,
            typeDesc = TYPE_DESC.getValue(type),
            strategy = strategy,
            notSelf = notSelf,
            authority = authority,
            authorityDesc = authorityDesc,
            profile = profileKey,
            profileDesc = profileDesc,
            definition = definition,
            definitionDesc = definitionDesc,
            centers = centers,
            gates = gates,
            channels = channels,
            verdict = verdict,
            note = "本结果为离线简化近似：将行星黄经映射为 64 卦之门并按中心分组判定「有定义」，类型/策略/权威由此推演；人生角色、定义类型与通道按出生信息确定性生成。非完整人类图计算（真实需精确星历与闸门边界），仅供娱乐参考。"
        )
    }

    /** 按类型给出事业一句话 */
    private fun careerLine(type: String): String = when (type) {
        "显示者" -> "你天生适合发起与破局，适合开创性、前导性的位置"
        "生产者" -> "荐骨能量充沛，适合顺应热情稳步深耕，把「回应」当雷达"
        "显示生产者" -> "兼具发起与执行效率，宜先回应再加速推进，可同时兼顾开创与落地"
        "投射者" -> "你是军师型人才，识人善任、善作指导，宜等待被邀请再全力投入"
        else -> "你是群体的镜子，适合以观察与调和的姿态参与协作"
    }

    /** 六维解读：总评 + 事业/财运/感情/健康/建议，贴合类型、策略与能量中心主题 */
    private fun buildVerdict(
        type: String,
        strategy: String,
        notSelf: String,
        authority: String,
        authorityDesc: String,
        profile: String,
        profileDesc: String,
        definition: String,
        definitionDesc: String,
        centers: List<Center>,
        gates: List<Int>,
        channels: List<String>
    ): String {
        val definedNames = centers.filter { it.defined }.map { it.nameCn }
        val spleenDefined = centers.first { it.nameCn == "脾中心" }.defined
        val energyDesc = when {
            definedNames.isEmpty() -> "九大能量中心全部开放，你像一面镜子般映照环境"
            definedNames.size >= 6 -> "多数能量中心有定义，内在引擎强劲而自足"
            else -> "部分能量中心开放，易受周围气场影响，但也因此更富弹性"
        }
        val healthDesc = if (spleenDefined) "脾中心有定义，直觉敏锐，可多倾听身体的第一信号"
        else "脾中心开放，身体预感偏弱，宜定期体检并留意作息信号"
        val channelDesc = if (channels.isEmpty()) "尚无稳定通道，宜先夯实单点能力"
        else "并点亮 ${channels.size} 条通道，能量路线清晰可循"
        val selfCare = if (definedNames.isEmpty()) "为环境所累时及时抽离独处" else "别因能量自足而长期紧绷，适时休息"
        val sb = StringBuilder()
        sb.append("总评：你是「$type」类型，以「$strategy」为人生策略，${TYPE_DESC.getValue(type)}")
        sb.append("\n事业：${careerLine(type)}；本盘激活 ${gates.size} 个能量闸门，$channelDesc")
        sb.append("\n财运：$energyDesc，财运随能量的聚散而起伏；重大财务决策宜交由「$authority」把关，待状态清晰再作承诺")
        sb.append("\n感情：人生角色「$profile」赋予你「$profileDesc」的关系质地，亲密相处宜留出空间又保持真诚；情感上的抉择让「$authority」替你定调")
        sb.append("\n健康：$healthDesc；同时注意$selfCare，张弛有度方得安康")
        sb.append("\n建议：当「$notSelf」的非自己主题浮现时，提醒你已偏离本真；回到「$strategy」，${authorityDesc}顺流而行，能量自然归位")
        return sb.toString()
    }

    private fun authorityOf(solar: Boolean, sacral: Boolean, spleen: Boolean, self: Boolean): String = when {
        solar -> "情绪权威"
        sacral -> "荐骨权威"
        spleen -> "脾权威"
        self -> "自我权威"
        else -> "反射权威"
    }

    private fun longitudeToGate(lon: Double): Int {
        val g = floor(lon / 5.625).toInt() // 0-63
        return ((g % 64) + 64) % 64 + 1
    }
}
