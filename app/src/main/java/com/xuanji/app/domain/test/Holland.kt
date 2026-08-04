package com.xuanji.app.domain.test

/**
 * 霍兰德职业兴趣测试（RIASEC，通识简化版）
 * 六种职业兴趣类型：R 现实型 / I 研究型 / A 艺术型 / S 社会型 / E 企业型 / C 常规型。
 * 共 60 题，每类型 10 题。每题作答「是」给对应类型 +1，作答「否」+0。
 * 取分最高的三个类型组成三码，并列时按 R、I、A、S、E、C 顺序取先出现的（确定性，不使用随机）。
 * 全离线、确定性计分，结果仅供自我探索参考。
 */

data class HollandQuestion(val text: String, val type: String)

data class HollandTypeResult(
    val code: String,
    val name: String,
    val score: Int,          // 0-10
    val level: String,       // 低 / 中 / 高
    val interpretation: String
)

data class HollandResult(
    val types: List<HollandTypeResult>,
    val code: String,
    val codeAdvice: String,
    val portrait: String
)

object Holland {
    /** 固定顺序：R、I、A、S、E、C，用于并列时的确定性排序 */
    val TYPE_ORDER: List<String> = listOf("R", "I", "A", "S", "E", "C")

    val TYPE_NAMES: Map<String, String> = mapOf(
        "R" to "现实型", "I" to "研究型", "A" to "艺术型",
        "S" to "社会型", "E" to "企业型", "C" to "常规型"
    )

    val QUESTIONS: List<HollandQuestion> = listOf(
        // ---- R 现实型 1-10 ----
        HollandQuestion("我喜欢动手修理电器、家具或自行车。", "R"),
        HollandQuestion("我热衷于种植花草，或照料家中的小动物。", "R"),
        HollandQuestion("比起整天坐在办公室，我更喜欢在户外活动。", "R"),
        HollandQuestion("我乐于使用工具亲手完成一件小制作。", "R"),
        HollandQuestion("我享受驾驶、骑行等亲手操控带来的乐趣。", "R"),
        HollandQuestion("我愿意从事体力劳动，觉得踏实又有成就感。", "R"),
        HollandQuestion("我对机械、电子设备的结构很感兴趣，喜欢拆解研究。", "R"),
        HollandQuestion("我喜欢登山、露营、徒步等户外运动。", "R"),
        HollandQuestion("我擅长操作各类机器，并乐于摸索它们的使用方法。", "R"),
        HollandQuestion("比起空谈理论，我更愿意亲手做出看得见的成果。", "R"),
        // ---- I 研究型 11-20 ----
        HollandQuestion("我热衷于探索事物的原理与规律。", "I"),
        HollandQuestion("我喜欢通过阅读科学书籍来满足好奇心。", "I"),
        HollandQuestion("面对难题，我愿意长时间独立钻研。", "I"),
        HollandQuestion("我常对自然现象背后的原因感到好奇。", "I"),
        HollandQuestion("我享受在实验或尝试中验证自己猜想的乐趣。", "I"),
        HollandQuestion("我习惯用数据或逻辑来分析问题。", "I"),
        HollandQuestion("我喜欢研究代码、数学谜题或烧脑的游戏。", "I"),
        HollandQuestion("我常问「为什么」，而不是满足于「怎么做」。", "I"),
        HollandQuestion("我乐意收集整理资料，形成自己的独到见解。", "I"),
        HollandQuestion("我喜欢挑战需要严密推理的智力问题。", "I"),
        // ---- A 艺术型 21-30 ----
        HollandQuestion("我热爱绘画、书法、音乐或舞蹈等艺术活动。", "A"),
        HollandQuestion("我常被电影、文学作品中的构思深深打动。", "A"),
        HollandQuestion("我喜欢把自己的想法通过作品表达出来。", "A"),
        HollandQuestion("我注重生活的美感，喜欢布置自己的小天地。", "A"),
        HollandQuestion("我乐于尝试新的创作形式和风格。", "A"),
        HollandQuestion("我宁愿发挥想象，也不愿按固定套路做事。", "A"),
        HollandQuestion("我习惯用照片、文字或画笔记录生活点滴。", "A"),
        HollandQuestion("我对时尚、设计、配色等审美话题很感兴趣。", "A"),
        HollandQuestion("我享受独自沉浸于创作带来的忘我状态。", "A"),
        HollandQuestion("我向往自由创作，不愿被规则和流程束缚。", "A"),
        // ---- S 社会型 31-40 ----
        HollandQuestion("我乐意帮助他人解决生活或学习中的难题。", "S"),
        HollandQuestion("我享受向别人讲解知识、分享经验的过程。", "S"),
        HollandQuestion("我关心身边人的情绪变化，愿意耐心倾听。", "S"),
        HollandQuestion("我热衷于参加志愿活动或公益服务。", "S"),
        HollandQuestion("我擅长调解同学或朋友之间的矛盾。", "S"),
        HollandQuestion("我愿意花时间陪伴和照顾家人朋友。", "S"),
        HollandQuestion("在集体中，我乐于关心和鼓励身边的人。", "S"),
        HollandQuestion("我喜欢组织大家互相帮助、共同成长。", "S"),
        HollandQuestion("我更能从别人的感谢中获得成就感。", "S"),
        HollandQuestion("我认为帮助他人成长比个人成就更有意义。", "S"),
        // ---- E 企业型 41-50 ----
        HollandQuestion("我乐于组织活动，并喜欢担任带头人。", "E"),
        HollandQuestion("我享受说服别人接受我的观点。", "E"),
        HollandQuestion("我喜欢竞争，渴望成为团队里的赢家。", "E"),
        HollandQuestion("我愿意承担一定风险去争取更大的回报。", "E"),
        HollandQuestion("我对经营生意或管理团队很感兴趣。", "E"),
        HollandQuestion("我敢于在公开场合表达自己的主张。", "E"),
        HollandQuestion("我习惯主动争取机会，而不是被动等待。", "E"),
        HollandQuestion("我喜欢制定目标，并带领大家去达成。", "E"),
        HollandQuestion("我欣赏果断决策、雷厉风行的人。", "E"),
        HollandQuestion("我向往有挑战、高回报的成长空间。", "E"),
        // ---- C 常规型 51-60 ----
        HollandQuestion("我喜欢把资料按一定规则整理得井井有条。", "C"),
        HollandQuestion("我享受记账、填报表这类细致的工作。", "C"),
        HollandQuestion("我习惯按流程办事，做事讲求规范。", "C"),
        HollandQuestion("我偏爱稳定、规律、可预期的工作节奏。", "C"),
        HollandQuestion("我对数据录入、档案整理等细致活有耐心。", "C"),
        HollandQuestion("我乐于核对细节，发现错误会很有成就感。", "C"),
        HollandQuestion("我做事讲究条理，东西总是摆放整齐。", "C"),
        HollandQuestion("我喜欢明确的规章制度，不喜欢含糊其辞。", "C"),
        HollandQuestion("我擅长把繁琐事务安排得有条不紊。", "C"),
        HollandQuestion("我倾向于选择稳妥、变化少的工作。", "C")
    )

    private fun levelOf(score: Int): String = when {
        score >= 7 -> "高"
        score >= 4 -> "中"
        else -> "低"
    }

    /** 高分解读的八个维度 */
    private data class HollandInterp(
        val desc: String,
        val trait: String,
        val career: String,
        val env: String,
        val major: String,
        val social: String,
        val weakness: String,
        val growth: String
    )

    private val INTERP: Map<String, HollandInterp> = mapOf(
        "R" to HollandInterp(
            "你偏好与物打交道，动手能力强，做事踏实可靠。",
            "务实、稳健、坚毅、独立，注重实际操作与看得见的成果。",
            "机械师、工程师、电工、农林畜牧、运动员、军警、外科医生等。",
            "户外或车间等具体可感的环境，喜欢与工具、机器、材料为伴。",
            "机械工程、电气自动化、建筑土木、农林园艺、体育等。",
            "话不多但真诚，靠行动赢得信任，不喜欢虚与委蛇。",
            "可能忽略抽象思考与人际沟通，遇到复杂人情事务容易不耐烦。",
            "适当补充抽象知识和社交训练，提升协调与表达能力。"),
        "I" to HollandInterp(
            "你头脑缜密，热衷钻研，享受用知识解开未知。",
            "理性、好奇、专注、独立，善于分析归纳与解决复杂问题。",
            "科学家、研究员、程序员、数据分析师、医生、大学教授等。",
            "实验室、图书馆、安静的办公环境，允许长时间独立专注。",
            "理工科、医学、计算机、心理学、经济学研究等。",
            "理性克制，习惯讲道理而不是讲感情，独处时精力更足。",
            "容易沉浸在自己的世界里，忽视人际互动与实际落地。",
            "多参与团队项目，学会把研究成果转化为现实价值。"),
        "A" to HollandInterp(
            "你感性丰富，富有创造力，习惯用艺术表达自我。",
            "敏感、浪漫、自由、想象，崇尚个性与美感。",
            "设计师、作家、音乐人、画家、摄影师、广告创意等。",
            "自由宽松、鼓励创造的工作氛围，反感僵硬流程。",
            "设计、文学、音乐、影视、戏剧、艺术教育等。",
            "感性细腻，情绪丰富，渴望被理解与欣赏。",
            "情绪波动大，容易理想化，拖延与随性可能影响效率。",
            "在保护灵感的同时建立创作纪律，学会按计划交付。"),
        "S" to HollandInterp(
            "你热心温暖，乐于助人，在与人相处中收获力量。",
            "友善、共情、利他、善于倾听，重视他人感受。",
            "教师、心理咨询师、社工、护士、培训师、人力资源等。",
            "以人为本的机构或团队，氛围和谐、互相支持。",
            "教育学、心理学、社会工作、护理、语言学等。",
            "真诚热情，善于倾听与鼓励，是团队里的暖心角色。",
            "过度付出容易透支自己，可能忽视自身的需要。",
            "学会设定边界，先照顾好自己的能量再帮助他人。"),
        "E" to HollandInterp(
            "你目标感强，敢于竞争，乐于带领他人开拓局面。",
            "自信、果断、有魄力、结果导向，善于说服与影响。",
            "创业者、销售、管理者、市场推广、经纪人、政界等。",
            "节奏快、讲业绩、充满机会与挑战的舞台。",
            "工商管理、市场营销、金融、国际贸易、公共管理等。",
            "主动热情，善于调动气氛，天然的领导气场。",
            "好胜心强，可能忽略他人感受，急于求成易冒进。",
            "学会倾听与授权，把说服力用在共赢而非支配上。"),
        "C" to HollandInterp(
            "你细致严谨，讲究秩序，是让一切井井有条的可靠角色。",
            "细心、守时、自律、有条理，重视规则与准确。",
            "会计、审计、行政、文秘、档案管理、数据分析、质检等。",
            "制度清晰、流程规范、相对稳定的办公环境。",
            "财会、金融、信息管理、行政管理、法律事务等。",
            "温和守规矩，低调可靠，是值得托付的伙伴。",
            "对变化适应慢，容易因循守旧，缺少冒险精神。",
            "在规范之外保留弹性，适当尝试新方法与新挑战。")
    )

    /** 低分类型只给一句建议 */
    private val LOW_ADVICE: Map<String, String> = mapOf(
        "R" to "你的现实型兴趣偏弱，动手与户外事务对你的吸引力不大，注意避免过分忽视实操能力。",
        "I" to "你的研究型兴趣偏弱，抽象钻研的吸引力有限，注意培养基本的分析与求证能力。",
        "A" to "你的艺术型兴趣偏弱，创作与审美可能不是你的主舞台，也不妨偶尔让生活多点色彩。",
        "S" to "你的社会型兴趣偏弱，助人与教学场景吸引力有限，注意保持基本的人际温度。",
        "E" to "你的企业型兴趣偏弱，竞争与领导对你吸引力不大，注意别错过该主动争取的机会。",
        "C" to "你的常规型兴趣偏弱，规则与细节可能让你觉得束缚，注意用基本条理支撑高效生活。"
    )

    /** 中高分段（>=4 分）给高分解读，低分段（<=3 分）给一句低分建议 */
    private fun interpretation(code: String, score: Int): String {
        if (score < 4) return LOW_ADVICE[code]!!
        val i = INTERP[code]!!
        return "描述：${i.desc}\n" +
            "特质：${i.trait}\n" +
            "适合职业：${i.career}\n" +
            "工作环境：${i.env}\n" +
            "专业方向：${i.major}\n" +
            "人际风格：${i.social}\n" +
            "潜在弱点：${i.weakness}\n" +
            "成长建议：${i.growth}"
    }

    /** 预置常见三码组合的职业建议 */
    private val CODE_ADVICE: Map<String, String> = mapOf(
        "RIA" to "动手+钻研+创意：你是典型的「全能实干家」，能把想法亲手做出来。适合工程设计、产品开发、工业设计、建筑等领域，注意在专注钻研与表达创意之间保持平衡。",
        "RIS" to "现实+研究+社会：你兼具技术与人文关怀，适合工程技术教育、医疗康复、环境保护、社区规划等「技术造福于人」的职业，把动手能力用在帮助别人上会很有成就感。",
        "REC" to "现实+企业+常规：你务实又有闯劲，擅长把资源组织起来落地执行，适合工程项目管理、生产运营、设备销售、自主创办实业等，注意守住流程规范避免盲目冒进。",
        "RSE" to "现实+社会+企业：你行动力强又善于带动他人，适合户外拓展教练、体育管理、农林业经营、军人警察等实践型领导岗位，人际与行动结合是你的独特优势。",
        "RSC" to "现实+社会+常规：你踏实可靠又乐于助人，适合技术培训、职教教师、康复治疗、生产管理、质检监督等稳定助人型工作，是团队里值得信赖的基石。",
        "RIC" to "现实+研究+常规：你动手与逻辑兼备，做事严谨，适合精密制造、机械设计、数据分析、测绘、品质工程等讲究准确的技术岗位。",
        "IAR" to "研究+艺术+现实：你既有创造力又讲实证，适合科学可视化、设计研发、动画技术、建筑设计与研究等跨界领域，注意协调感性与理性。",
        "IAS" to "研究+艺术+社会：你热爱知识又关心人，适合科学传播、科普教育、心理辅导、教育产品研发等，把深邃的思考讲给更多人听。",
        "IRE" to "研究+现实+企业：你思维敏锐又敢于行动，适合科技创业、产品经理、技术管理、专利顾问、医药代表等「用专业驱动业务」的角色。",
        "IEC" to "研究+企业+常规：你理性且有经营头脑，适合管理咨询、金融分析、战略策划、数据驱动的运营管理等岗位，注意保持对细节的耐心。",
        "AIS" to "艺术+研究+社会：你是「思想与美的传播者」，适合教育设计、艺术治疗、文学研究、内容策划、文化传媒等，把创造与服务融为一体。",
        "AER" to "艺术+企业+现实：你创意大胆又行动力强，适合广告创意、产品设计、时尚产业、演出经纪、创意工作室经营等，注意把灵感落地成作品。",
        "AEC" to "艺术+企业+常规：你兼顾审美与条理，适合文创产品运营、媒体制作管理、品牌视觉管理、设计项目管理等，是创意团队里把关落地的角色。",
        "SEC" to "社会+企业+常规：你是「组织与人情的桥梁」，适合教育管理、人力资源、行政主管、公益机构运营、客户服务管理等，善于用规则成就他人。",
        "SIC" to "社会+研究+常规：你细心且有助人热忱，适合图书情报、档案教育、公共卫生管理、职业辅导、知识管理等服务型技术岗位。",
        "ECR" to "企业+常规+现实：你经营意识强又讲究落地，适合连锁经营、物流管理、供应链、工程承包、贸易实务等，注意照顾团队里的人际温度。",
        "EIC" to "企业+研究+常规：你长于商业分析，适合投行分析、战略咨询、市场研究、经营分析、风险管理等用数据支撑决策的岗位。",
        "CRI" to "常规+现实+研究：你细致严谨又有技术功底，适合审计、成本核算、档案与数据管理、工程资料管理等需要精确到位的岗位。",
        "CSE" to "常规+社会+企业：你温和有条理又善协作，适合行政主管、教务管理、银行柜面管理、保险服务等，把秩序与服务结合得很好。"
    )

    private val CODE_ADVICE_DEFAULT: String =
        "你的高分类型组合较为少见或分布均衡。建议结合各类型解读，从「你最享受、也做得最好」的活动中寻找职业线索，不必被单一类型框定。多尝试跨领域的实践，兴趣会指引你找到真正适合的方向。"

    private fun buildPortrait(code: String): String {
        val topNames = code.map { c -> "${TYPE_NAMES[c.toString()]!!}（${c}）" }.joinToString("、")
        return "你的职业兴趣三码为 ${code}，即 ${topNames}。" +
            "兴趣反映偏好而非能力，职业选择还需要结合能力、性格与现实条件。" +
            "建议你在高分类型对应的领域多多尝试实践，把「想做的」与「能做的」慢慢对齐，你的方向会在探索中逐渐清晰。"
    }

    /** 计分：answers 为每题「是/否」的选择（true=是，false=否） */
    fun calculate(answers: List<Boolean>): HollandResult {
        val raw = mutableMapOf<String, Int>()
        QUESTIONS.forEachIndexed { i, q ->
            val ans = answers.getOrNull(i) ?: return@forEachIndexed
            if (ans) raw[q.type] = raw.getOrDefault(q.type, 0) + 1
        }
        val types = TYPE_ORDER.map { code ->
            val score = raw[code] ?: 0
            HollandTypeResult(code, TYPE_NAMES[code]!!, score, levelOf(score), interpretation(code, score))
        }
        // 取分最高前三码：分数降序，并列时按 TYPE_ORDER（R/I/A/S/E/C）顺序取先出现的，确定性
        val top3 = TYPE_ORDER
            .map { it to raw.getOrDefault(it, 0) }
            .sortedWith(
                compareByDescending<Pair<String, Int>> { it.second }
                    .thenBy { TYPE_ORDER.indexOf(it.first) }
            )
            .take(3)
            .map { it.first }
        val code = top3.joinToString("")
        val advice = CODE_ADVICE[code] ?: CODE_ADVICE_DEFAULT
        return HollandResult(types, code, advice, buildPortrait(code))
    }
}
