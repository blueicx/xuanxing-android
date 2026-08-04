package com.xuanji.app.domain.test

/**
 * 卡特尔 16PF 人格因素问卷（通识简化版）
 * 16 个因素（A 乐群性 ~ Q4 紧张性），共 34 题（含 2 道开场不计分题）。
 * 每题三选项 A=0 / B=1 / C=2，每因素 2 题（部分反向计分）。
 * 原始分换算 1-10 标准分：0→1、1→3、2→5、3→8、4→10。
 * 全离线、确定性计分，结果仅供自我探索参考。
 */

data class Cattell16Question(val text: String, val factor: String?, val reverse: Boolean)

/** 六维解读：总评 / 事业 / 财运 / 感情 / 健康 / 建议 */
data class FactorInterpretation(
    val summary: String,
    val career: String,
    val wealth: String,
    val love: String,
    val health: String,
    val advice: String
)

data class FactorResult(
    val code: String,
    val name: String,
    val score: Int,          // 1-10 标准分
    val level: String,       // 低 / 中 / 高
    val interpretation: String
)

data class Cattell16Result(
    val factors: List<FactorResult>,
    val portrait: String
)

object Cattell16PF {
    val QUESTIONS: List<Cattell16Question> = listOf(
        // 开场不计分 1-2
        Cattell16Question("我对本次测评持认真配合的态度。", null, false),
        Cattell16Question("我会如实地回答以下每一个问题。", null, false),
        // A 乐群性 3-4
        Cattell16Question("在陌生人的聚会中，我通常会主动与人交谈。", "A", false),
        Cattell16Question("比起热闹的人群，我更喜欢一个人安静地待着。", "A", true),
        // B 聪慧性 5-6
        Cattell16Question("我能够迅速抓住问题的核心并找到解决办法。", "B", false),
        Cattell16Question("面对抽象的概念或理论时，我常常感到吃力。", "B", true),
        // C 稳定性 7-8
        Cattell16Question("遇到突发变故时，我通常能保持冷静和镇定。", "C", false),
        Cattell16Question("我很容易因为一点小事而情绪激动。", "C", true),
        // E 恃强性 9-10
        Cattell16Question("在团队讨论中，我倾向于坚持自己的意见。", "E", false),
        Cattell16Question("即使意见不同，我也常会顺从多数人的看法。", "E", true),
        // F 活泼性 11-12
        Cattell16Question("我经常开怀大笑，享受轻松欢乐的气氛。", "F", false),
        Cattell16Question("我的性格偏严肃，玩笑话很少。", "F", true),
        // G 有恒性 13-14
        Cattell16Question("凡是答应别人的事情，我都会尽力做到。", "G", false),
        Cattell16Question("做事时我常凭一时兴趣，难以坚持到底。", "G", true),
        // H 敢为性 15-16
        Cattell16Question("面对挑战和风险，我敢于大胆尝试。", "H", false),
        Cattell16Question("在陌生的环境中，我常常感到紧张和退缩。", "H", true),
        // I 敏感性 17-18
        Cattell16Question("我容易被艺术作品或电影情节深深打动。", "I", false),
        Cattell16Question("我做事注重理性分析，很少感情用事。", "I", true),
        // L 怀疑性 19-20
        Cattell16Question("我常常怀疑别人说话的真实意图。", "L", false),
        Cattell16Question("我相信大多数人都是善意和真诚的。", "L", true),
        // M 幻想性 21-22
        Cattell16Question("我经常沉浸在自己的幻想和白日梦中。", "M", false),
        Cattell16Question("我更关注现实中的具体问题，不爱空想。", "M", true),
        // N 世故性 23-24
        Cattell16Question("与人相处时，我懂得察言观色、话留三分。", "N", false),
        Cattell16Question("我心里想什么就说什么，不会拐弯抹角。", "N", true),
        // O 忧虑性 25-26
        Cattell16Question("我常常为未来可能发生的不测而担忧。", "O", false),
        Cattell16Question("即使事情出了差错，我也很少自责或焦虑。", "O", true),
        // Q1 实验性 27-28
        Cattell16Question("我愿意接受新思想，尝试打破常规。", "Q1", false),
        Cattell16Question("我更倾向于遵循传统和既有的规矩。", "Q1", true),
        // Q2 独立性 29-30
        Cattell16Question("需要做决定时，我更相信自己而不愿依赖他人。", "Q2", false),
        Cattell16Question("做出重大选择前，我常常需要征求别人的意见。", "Q2", true),
        // Q3 自律性 31-32
        Cattell16Question("我能够严格要求自己，按计划完成既定目标。", "Q3", false),
        Cattell16Question("我做事随心所欲，很少给自己立规矩。", "Q3", true),
        // Q4 紧张性 33-34
        Cattell16Question("即使没有大事发生，我也常常感到心神不宁。", "Q4", false),
        Cattell16Question("我大多数时候内心平静，很少有紧张不安的感觉。", "Q4", true)
    )

    val FACTORS: List<Pair<String, String>> = listOf(
        "A" to "乐群性", "B" to "聪慧性", "C" to "稳定性", "E" to "恃强性",
        "F" to "活泼性", "G" to "有恒性", "H" to "敢为性", "I" to "敏感性",
        "L" to "怀疑性", "M" to "幻想性", "N" to "世故性", "O" to "忧虑性",
        "Q1" to "实验性", "Q2" to "独立性", "Q3" to "自律性", "Q4" to "紧张性"
    )

    private fun standardScore(raw: Int): Int = when (raw) {
        0 -> 1; 1 -> 3; 2 -> 5; 3 -> 8; else -> 10
    }

    private fun levelOf(score: Int): String = when {
        score <= 3 -> "低"
        score >= 8 -> "高"
        else -> "中"
    }

    private val MID: Map<String, String> = mapOf(
        "A" to "你的乐群性处于中等水平，能依据场合在社交与独处间自如切换。",
        "B" to "你的聪慧性处于中等水平，处理日常事务与一般学习游刃有余。",
        "C" to "你的稳定性处于中等水平，情绪大体平稳，偶有小幅起伏。",
        "E" to "你的恃强性处于中等水平，既能坚持己见，也懂得适当妥协。",
        "F" to "你的活泼性处于中等水平，严肃与活泼兼而有之。",
        "G" to "你的有恒性处于中等水平，能坚持重要事情，也保留灵活空间。",
        "H" to "你的敢为性处于中等水平，敢于尝试新事物，但会先评估风险。",
        "I" to "你的敏感性处于中等水平，理性与感性运用得当。",
        "L" to "你的怀疑性处于中等水平，对人基本信任，也不失警惕。",
        "M" to "你的幻想性处于中等水平，务实之中保留想象力。",
        "N" to "你的世故性处于中等水平，坦诚而不失分寸。",
        "O" to "你的忧虑性处于中等水平，能觉察压力而不被其压垮。",
        "Q1" to "你的实验性处于中等水平，尊重传统也愿意接受合理新事物。",
        "Q2" to "你的独立性处于中等水平，既能自主决策，也乐于听取建议。",
        "Q3" to "你的自律性处于中等水平，张弛有度。",
        "Q4" to "你的紧张性处于中等水平，多数时候心态平稳。"
    )

    private val INTERP: Map<String, Map<String, FactorInterpretation>> = mapOf(
        "A" to mapOf(
            "low" to FactorInterpretation(
                "你缄默、审慎，习惯独处，在人群中话不多。",
                "适合独立钻研型岗位，如技术、写作、研发。",
                "消费理性，花钱有计划，能稳步积蓄。",
                "慢热内敛，需要对方耐心走进你的内心。",
                "情绪不外露，建议通过运动与书写释放压力。",
                "适当参加小型社交，练习主动表达感受。"),
            "high" to FactorInterpretation(
                "你外向热情，喜欢与人交往，乐于参加集体活动。",
                "适合公关、销售、管理、教育培训等人际密集型工作。",
                "人脉广、信息渠道多，但易冲动消费。",
                "主动热情，善于营造气氛，需留意对方节奏。",
                "社交活跃有利于情绪，但注意留出独处时间。",
                "在享受热闹的同时，学会倾听与沉淀。")
        ),
        "B" to mapOf(
            "low" to FactorInterpretation(
                "你更习惯具体、实际的事务，抽象思维相对吃力。",
                "适合动手实践、流程明确的工作，如工艺、物流、一线操作。",
                "对数字与风险较为敏感，投资偏保守稳健。",
                "表达直白朴实，注重日常的实际付出。",
                "思虑较少反而少忧少虑，注意饮食规律。",
                "多阅读、多训练逻辑，勤能补拙。"),
            "high" to FactorInterpretation(
                "你思维敏捷，善于抽象思考，学习新事物快。",
                "适合需要分析、策划与创新的岗位，如研究、编程、咨询。",
                "善于发现机会，但需防聪明反被聪明误。",
                "谈吐有内容，易吸引欣赏智慧的伴侣。",
                "用脑多，注意劳逸结合与充足睡眠。",
                "把聪明用在长期价值上，避免恃才傲物。")
        ),
        "C" to mapOf(
            "low" to FactorInterpretation(
                "你情绪起伏较大，易被小事触动，遇事容易激动。",
                "高压岗位会加重你的负担，宜选节奏平稳的工作。",
                "情绪化决策易致财务起伏，理财宜设固定纪律。",
                "情绪波动会消耗亲密关系，学习先冷静再沟通。",
                "情绪压力易转化为身体不适，注意睡眠与放松。",
                "练习深呼吸与正念，记录自己的情绪触发点。"),
            "high" to FactorInterpretation(
                "你情绪成熟稳定，遇事从容，抗压能力强。",
                "能承担高压与突发任务，是团队中的定海神针。",
                "决策理性，不急不躁，财富积累稳健。",
                "情绪稳定给伴侣安全感，关系和谐持久。",
                "身心平衡，抗病能力强。",
                "保持运动习惯，注意倾听他人的情绪。")
        ),
        "E" to mapOf(
            "low" to FactorInterpretation(
                "你谦逊随和，乐于配合他人，不喜争执。",
                "适合执行、协调、服务型岗位，团队合作佳。",
                "较少争抢利益，需学会主动争取应得回报。",
                "体贴包容，但易压抑自己的需求。",
                "少与人冲突，身心平和。",
                "在必要时敢于表达立场，维护自己的边界。"),
            "high" to FactorInterpretation(
                "你自信好强，坚持己见，喜欢掌控局面。",
                "适合领导、开拓型岗位，执行力强。",
                "敢闯敢拼，收益与风险并存。",
                "主导性强，需避免把伴侣当成下属。",
                "好胜心强易积累压力，注意放松。",
                "学会倾听与授权，赢得人心比赢得争论更重要。")
        ),
        "F" to mapOf(
            "low" to FactorInterpretation(
                "你严肃审慎，思虑周全，不轻易表露情绪。",
                "适合需要严谨与责任感的岗位，如财务、法务、质检。",
                "稳健保守，不冒进。",
                "深情但内敛，需要对方读懂你的细腻。",
                "压力内隐，注意给情绪找出口。",
                "适度放松，允许自己享受轻松时刻。"),
            "high" to FactorInterpretation(
                "你轻松活泼，爱说爱笑，充满感染力。",
                "适合气氛活跃的岗位，如主持、销售、公关。",
                "花钱随性，易透支，建议设定预算。",
                "幽默风趣，能给关系带来活力。",
                "情绪外放有利于释放压力，注意劳逸结合。",
                "在轻松之外培养专注与条理。")
        ),
        "G" to mapOf(
            "low" to FactorInterpretation(
                "你随性灵活，不喜被规则束缚，做事凭兴趣。",
                "适合自由、创意、变化快的领域，反感刻板管理。",
                "收入波动大，宜培养储蓄习惯。",
                "随性浪漫，但承诺力需加强。",
                "作息不规律易影响状态。",
                "为重要目标设立小步计划并坚持。"),
            "high" to FactorInterpretation(
                "你持之以恒，责任心强，做事有始有终。",
                "适合需要长期投入与信任的岗位，如会计、工程、管理。",
                "自律储蓄，财务稳健。",
                "言而有信，是可靠的伴侣。",
                "生活规律，身体状态稳定。",
                "避免过度苛责自己，偶尔允许灵活变通。")
        ),
        "H" to mapOf(
            "low" to FactorInterpretation(
                "你谨慎小心，面对陌生环境容易退缩。",
                "宜选稳定熟悉的环境，循序渐进地拓展。",
                "保守理财，不轻易冒险。",
                "被动害羞，需要对方主动靠近。",
                "紧张感易影响消化与睡眠。",
                "从小挑战开始练习勇气，积少成多。"),
            "high" to FactorInterpretation(
                "你大胆敢为，敢于冒险，善于把握机会。",
                "适合开拓、销售、创业等挑战型岗位。",
                "敢于投资，也需设好止损线。",
                "主动热烈，追求心仪对象时有勇气。",
                "冒险活动注意安全防护。",
                "胆大之外加一分心细，评估风险后再行动。")
        ),
        "I" to mapOf(
            "low" to FactorInterpretation(
                "你理智务实，就事论事，不轻易感情用事。",
                "适合需要客观判断的岗位，如工程、法律、技术。",
                "理性消费，决策冷静。",
                "表达务实，少甜言蜜语但行动可靠。",
                "情绪稳定，少内耗。",
                "适度关注他人感受，增加人情温度。"),
            "high" to FactorInterpretation(
                "你敏感细腻，共情力强，容易被打动。",
                "适合艺术、心理、教育、人文类工作。",
                "易因情绪消费，注意理性预算。",
                "浪漫体贴，善解人意，易与伴侣共情。",
                "情绪敏感易失眠，注意情绪管理。",
                "练习区分他人情绪与自身情绪，保护内心边界。")
        ),
        "L" to mapOf(
            "low" to FactorInterpretation(
                "你信赖随和，容易相信别人，待人真诚。",
                "团队合作顺畅，但需防范被利用。",
                "轻信他人易吃亏，重要决定多核实。",
                "信任伴侣，关系简单轻松。",
                "心态放松，少焦虑。",
                "保留基本警惕心，学会保护自己。"),
            "high" to FactorInterpretation(
                "你多疑警惕，习惯观察他人的动机。",
                "善于识人辨事，适合调查、风控、审计。",
                "精于算计，不轻信高回报，但也防过度猜疑错失良机。",
                "猜疑易伤感情，需给伴侣信任空间。",
                "长期戒备耗神，注意放松。",
                "有证据地信任，把猜疑转为建设性沟通。")
        ),
        "M" to mapOf(
            "low" to FactorInterpretation(
                "你脚踏实地，注重现实，不爱空想。",
                "适合执行、操作、运营等务实岗位。",
                "消费实际，储蓄稳健。",
                "务实安稳，少浪漫但可靠。",
                "心态踏实，身体状态平稳。",
                "偶尔放飞想象力，让生活多一点色彩。"),
            "high" to FactorInterpretation(
                "你天马行空，想象力丰富，常有奇思妙想。",
                "适合创意、设计、文学、策划等岗位。",
                "想法多但落地难，宜聚焦执行。",
                "浪漫梦幻，需兼顾现实需求。",
                "思虑过重易失眠，注意劳逸结合。",
                "把灵感记录并分解成可执行的小步骤。")
        ),
        "N" to mapOf(
            "low" to FactorInterpretation(
                "你坦白直率，表里如一，不善伪装。",
                "适合技术、研究等凭实力说话的领域。",
                "不善讨价还价，理财宜借助工具。",
                "真诚坦荡，伴侣容易信任你。",
                "情绪外露，不易积压。",
                "场合需要时，学习委婉的表达方式。"),
            "high" to FactorInterpretation(
                "你世故练达，洞察人心，擅长处理复杂关系。",
                "适合谈判、公关、管理、外交型岗位。",
                "精于算计，善于把握机会。",
                "懂进退，但也可能让人觉得不够交心。",
                "思虑深，注意放松神经。",
                "城府之外保留真诚，真诚是最高的情商。")
        ),
        "O" to mapOf(
            "low" to FactorInterpretation(
                "你安详沉着，少忧少虑，心态豁达。",
                "抗压能力强，能轻松应对繁忙事务。",
                "心态稳，不因涨跌焦虑。",
                "给伴侣带来松弛感，少纠结小事。",
                "心理负担轻，睡眠良好。",
                "保持从容的同时，适当保留对未来的规划意识。"),
            "high" to FactorInterpretation(
                "你忧思较重，容易自责，常为未来担忧。",
                "责任感强，但易把压力扛在肩上。",
                "患得患失，决策易被焦虑影响。",
                "多思多虑，需要伴侣更多安抚。",
                "焦虑易影响睡眠与消化。",
                "练习接纳不完美，用行动代替空想。")
        ),
        "Q1" to mapOf(
            "low" to FactorInterpretation(
                "你尊重传统，倾向守成，喜欢熟悉的方式。",
                "适合规范成熟的行业，如行政、财会、传统制造。",
                "投资偏保守，收益稳定。",
                "恋旧专一，重视稳定的关系。",
                "生活规律，身体适应力稳定。",
                "在必要时拥抱变化，避免被时代抛下。"),
            "high" to FactorInterpretation(
                "你思想开放，乐于实验，敢于打破常规。",
                "适合创新、改革、前沿科技领域。",
                "愿意尝试新理财方式，注意风险控制。",
                "关系观念开放，需与伴侣对齐期待。",
                "生活方式多变，注意维持基础规律。",
                "把创新精神聚焦到事业，家庭生活保留稳定性。")
        ),
        "Q2" to mapOf(
            "low" to FactorInterpretation(
                "你重视依赖与合作，习惯听取他人意见。",
                "适合团队协作、分工明确的环境。",
                "投资易跟风，宜独立思考。",
                "依赖亲密关系，需保持适度独立。",
                "情绪受他人影响大，注意自我调节。",
                "培养独立思考的习惯，重要决定自己做主。"),
            "high" to FactorInterpretation(
                "你独立自主，当机立断，不依赖他人。",
                "适合独立决策、自主创业或专业研究。",
                "投资决策独立，自负盈亏。",
                "需要个人空间，给伴侣留出距离。",
                "独处能力好，但别过度自我封闭。",
                "必要时寻求支持，独立与合作并不矛盾。")
        ),
        "Q3" to mapOf(
            "low" to FactorInterpretation(
                "你随性洒脱，不拘小节，不喜约束。",
                "灵活机动，但不适合强纪律环境。",
                "消费随性，宜设自动储蓄。",
                "自由随性，需与伴侣沟通边界。",
                "作息不规律，注意自我管理。",
                "建立简单可行的日常惯例，自律带来自由。"),
            "high" to FactorInterpretation(
                "你自律严谨，克己守规，计划性强。",
                "执行力强，适合高标准、高要求的岗位。",
                "理财有计划，能稳步积累财富。",
                "责任感强，让伴侣安心。",
                "生活习惯好，状态稳定。",
                "偶尔放松标准，允许自己不完美。")
        ),
        "Q4" to mapOf(
            "low" to FactorInterpretation(
                "你心平气和，从容淡定，很少紧张不安。",
                "临场发挥稳定，能淡定处理突发状况。",
                "心态稳，不轻易被行情左右。",
                "松弛有度，能给关系带来平和。",
                "身心放松，状态良好。",
                "保持从容，同时用适度紧迫感推动进步。"),
            "high" to FactorInterpretation(
                "你易感紧张，常有莫名的焦虑与不安。",
                "压力环境下表现打折，宜创造松弛的节奏。",
                "紧张决策易失误，重大决定缓一缓。",
                "紧张情绪易传导给伴侣，注意调节。",
                "紧张影响肌肉与睡眠，建议运动放松。",
                "练习腹式呼吸与正念，保证充足睡眠。")
        )
    )

    private fun interpretation(code: String, level: String): String {
        if (level == "中") return MID[code]!!
        val i = INTERP[code]!![if (level == "低") "low" else "high"]!!
        return "总评：${i.summary}\n" +
            "事业：${i.career}\n" +
            "财运：${i.wealth}\n" +
            "感情：${i.love}\n" +
            "健康：${i.health}\n" +
            "建议：${i.advice}"
    }

    /** 计分：answers 为每题 A/B/C 的 0/1/2 选择 */
    fun calculate(answers: List<Int>): Cattell16Result {
        val raw = mutableMapOf<String, Int>()
        QUESTIONS.forEachIndexed { i, q ->
            val f = q.factor ?: return@forEachIndexed
            val ans = answers.getOrNull(i) ?: return@forEachIndexed
            val score = if (q.reverse) 2 - ans else ans
            raw[f] = raw.getOrDefault(f, 0) + score
        }
        val factors = FACTORS.map { (code, name) ->
            val r = raw[code] ?: 0
            val score = standardScore(r)
            val level = levelOf(score)
            FactorResult(code, name, score, level, interpretation(code, level))
        }
        return Cattell16Result(factors, buildPortrait(factors))
    }

    private fun buildPortrait(factors: List<FactorResult>): String {
        val byCode = factors.associate { it.code to it.level }
        val sb = StringBuilder()
        fun phrase(code: String, hi: String, lo: String, mid: String) {
            sb.append(
                if (byCode[code] == "高") hi
                else if (byCode[code] == "低") lo
                else mid
            ).append(" ")
        }
        phrase("A", "你热情外向，乐群合群。", "你缄默孤独，沉静内敛。", "你的乐群性适中，社交节奏收放自如。")
        phrase("B", "你聪慧敏捷，善于抽象思维。", "你注重实际，抽象思维略显吃力。", "你的聪慧性中等，日常思考游刃有余。")
        phrase("C", "你情绪稳定成熟，抗压能力强。", "你情绪易波动，对压力较敏感。", "你的情绪稳定性适中，总体平稳。")
        phrase("E", "你自信好强，勇于坚持己见。", "你谦逊随和，善于配合他人。", "你的恃强性适中，坚持与妥协兼顾。")
        phrase("F", "你轻松活泼，充满活力。", "你严肃审慎，思虑周全。", "你的活泼性适中，张弛有度。")
        phrase("G", "你持之以恒，责任心强。", "你随性变通，不喜约束。", "你的有恒性适中，能坚持也能变通。")
        phrase("H", "你大胆敢为，乐于冒险。", "你谨慎畏缩，回避风险。", "你的敢为性适中，敢试但留分寸。")
        phrase("I", "你敏感细腻，重情重义。", "你理智务实，就事论事。", "你的敏感性适中，情理并重。")
        phrase("L", "你多疑警惕，保护意识强。", "你信赖随和，真诚待人。", "你的怀疑性适中，信任但不盲从。")
        phrase("M", "你天马行空，想象力丰富。", "你脚踏实地，注重现实。", "你的幻想性适中，务实不失想象。")
        phrase("N", "你世故练达，善于周旋。", "你坦白直率，表里如一。", "你的世故性适中，坦诚不失分寸。")
        phrase("O", "你忧思较重，常感不安。", "你安详沉着，少忧少虑。", "你的忧虑性适中，能正视压力。")
        phrase("Q1", "你思想开放，乐于革新。", "你尊重传统，倾向守成。", "你的实验性适中，兼容新旧。")
        phrase("Q2", "你独立自主，当机立断。", "你重视依赖，乐于合作。", "你的独立性适中，独立也善借力。")
        phrase("Q3", "你自律严谨，克己守规。", "你随性洒脱，不拘小节。", "你的自律性适中，松弛有度。")
        phrase("Q4", "你心境平和，从容淡定。", "你易感紧张，常陷焦虑。", "你的紧张性适中，压力下能自我调节。")
        sb.append("你的 16 种人格因素组合，反映了你独特的处世方式。标准分 1-10 分：1-3 为低分、4-7 为中等、8-10 为高分。本结果仅供参考，不构成专业心理诊断。")
        return sb.toString()
    }
}
