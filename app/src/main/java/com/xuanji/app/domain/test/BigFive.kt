package com.xuanji.app.domain.test

/**
 * 大五人格（Big Five / OCEAN）测试
 * 50 题，五维度（开放性O/尽责性C/外向性E/宜人性A/神经质N）各 10 题（含反向计分）。
 * 这是自编的 Big Five 风格自我探索问卷，不是经过常模验证的临床量表。
 * 全离线、确定性计分。
 */

data class BigFiveQuestion(val text: String, val dimension: String, val reverse: Boolean)
data class BigFiveInterpretation(
    val title: String,
    val traits: String,
    val strengths: String,
    val weaknesses: String,
    val work: String,
    val relationship: String,
    val growth: String,
    val famous: String
)
data class BigFiveDimensionResult(
    val code: String,
    val name: String,
    val description: String,
    val score: Int,          // 0-50
    val percent: Int,        // 0-100
    val level: String,       // 低 / 中 / 高
    val interpretation: BigFiveInterpretation
)
data class BigFiveResult(
    val dimensions: List<BigFiveDimensionResult>,
    val portrait: String
)

object BigFive {
    val QUESTIONS: List<BigFiveQuestion> = listOf(
        // O 开放 1-10
        BigFiveQuestion("我对艺术和美学有强烈的感受。", "O", false),
        BigFiveQuestion("我喜欢探索新的文化或国外旅行。", "O", false),
        BigFiveQuestion("我容易被诗歌、音乐或绘画打动。", "O", false),
        BigFiveQuestion("我经常沉浸在幻想或白日梦中。", "O", false),
        BigFiveQuestion("我能够欣赏抽象的艺术或概念。", "O", false),
        BigFiveQuestion("我对哲学或深奥的问题不感兴趣。", "O", true),
        BigFiveQuestion("我不太喜欢尝试新的食物。", "O", true),
        BigFiveQuestion("我更倾向于遵循传统，而不是挑战它。", "O", true),
        BigFiveQuestion("我的想象力不太丰富。", "O", true),
        BigFiveQuestion("我对抽象的理论感到厌烦。", "O", true),
        // C 尽责 11-20
        BigFiveQuestion("我保持我的物品井然有序。", "C", false),
        BigFiveQuestion("我按时完成工作任务。", "C", false),
        BigFiveQuestion("我注重细节和准确性。", "C", false),
        BigFiveQuestion("我喜欢制定计划并按计划行事。", "C", false),
        BigFiveQuestion("我做事有条理，有组织。", "C", false),
        BigFiveQuestion("我经常拖延事情。", "C", true),
        BigFiveQuestion("我很难让自己开始工作。", "C", true),
        BigFiveQuestion("我不太注意细节。", "C", true),
        BigFiveQuestion("我经常忘记自己的承诺。", "C", true),
        BigFiveQuestion("我习惯把东西乱放，不太整洁。", "C", true),
        // E 外向 21-30
        BigFiveQuestion("我喜欢成为聚会的焦点。", "E", false),
        BigFiveQuestion("我善于与人交谈。", "E", false),
        BigFiveQuestion("我容易与陌生人熟络。", "E", false),
        BigFiveQuestion("我在团队中喜欢多发言。", "E", false),
        BigFiveQuestion("我精力充沛，喜欢热闹。", "E", false),
        BigFiveQuestion("我更享受独处而非社交。", "E", true),
        BigFiveQuestion("我不太喜欢主动与人互动。", "E", true),
        BigFiveQuestion("我倾向于保留自己的意见，不太表达。", "E", true),
        BigFiveQuestion("在人群中我感到不自在。", "E", true),
        BigFiveQuestion("我更喜欢安静、低调的场合。", "E", true),
        // A 宜人 31-40
        BigFiveQuestion("我关心他人的感受。", "A", false),
        BigFiveQuestion("我乐于帮助他人。", "A", false),
        BigFiveQuestion("我很容易与他人共情。", "A", false),
        BigFiveQuestion("我相信人性本善。", "A", false),
        BigFiveQuestion("我容易被别人的情绪感染。", "A", false),
        BigFiveQuestion("我有时会冷落他人的感受。", "A", true),
        BigFiveQuestion("我更关心自己的利益而非他人的。", "A", true),
        BigFiveQuestion("我对别人的困难不太在意。", "A", true),
        BigFiveQuestion("我倾向于怀疑他人的动机。", "A", true),
        BigFiveQuestion("我不太容易被别人的悲伤打动。", "A", true),
        // N 神经质 41-50
        BigFiveQuestion("我经常感到焦虑。", "N", false),
        BigFiveQuestion("我容易情绪波动。", "N", false),
        BigFiveQuestion("我很容易紧张。", "N", false),
        BigFiveQuestion("我容易感到沮丧。", "N", false),
        BigFiveQuestion("我经常担心事情会出错。", "N", false),
        BigFiveQuestion("我能很好地应对压力。", "N", true),
        BigFiveQuestion("我很少感到情绪低落。", "N", true),
        BigFiveQuestion("我通常情绪稳定。", "N", true),
        BigFiveQuestion("我很少被小事激怒。", "N", true),
        BigFiveQuestion("我能够保持冷静。", "N", true)
    )

    val DIMENSION_NAMES = mapOf(
        "O" to "开放性", "C" to "尽责性", "E" to "外向性", "A" to "宜人性", "N" to "神经质"
    )
    val DIMENSION_DESC = mapOf(
        "O" to "反映个体对新思想、新体验、审美和想象力的好奇与接受程度。",
        "C" to "反映个体的组织性、条理性、责任感和成就动机，是预测职业成功的重要维度。",
        "E" to "反映个体在人际互动中的活跃程度、热情和积极情绪的强度。",
        "A" to "反映个体在人际关系中的合作性、同情心、信任和关心他人的倾向。",
        "N" to "反映个体体验负面情绪（如焦虑、愤怒、抑郁）的倾向，情绪稳定性是其相反端。"
    )

    private val INTERP = mapOf(
        "O" to mapOf(
            "low" to BigFiveInterpretation("低开放性",
                "你偏向传统、务实，注重现实和经验，喜欢熟悉的环境和常规。你不容易被抽象概念吸引，更关注眼前的事务。",
                "实际、踏实、可靠、专注、不易被干扰",
                "可能抗拒新事物，灵活性较差，想象力受限，适应变化较慢",
                "适合从事需要稳定、规律和细致入微的工作，如会计、行政、质量控制、传统行业。",
                "在关系中你提供稳定和安全感，但可能因缺乏尝试新事物的热情而让伴侣感到乏味。",
                "尝试定期接触新体验（如新餐馆、新路线、新兴趣），练习创造性思考，阅读不同类型的书籍。",
                "约翰·洛克等注重实践与逻辑的人物"),
            "moderate" to BigFiveInterpretation("中等开放性",
                "你在接受新经验和坚持传统之间保持平衡。你能欣赏艺术和抽象概念，但也务实关注实际。",
                "兼具灵活性和稳定性，既能创新也能执行，适应大部分环境",
                "可能在两者间摇摆，遇到极端情况可能不适",
                "适合需要适度创新的职业，如市场营销、项目管理、教育。",
                "你既能享受稳定的日常，也能偶尔创造惊喜，关系较为和谐。",
                "保持好奇，但不必勉强改变，持续培养审美和反思习惯。",
                "许多成功的中层管理者"),
            "high" to BigFiveInterpretation("高开放性",
                "你充满好奇，喜欢探索新事物，对艺术、美学、抽象概念有浓厚兴趣。你乐于接受变化，思维灵活，富有想象力。",
                "创造力强、适应力强、思想开放、善于学习、审美品味高",
                "可能不切实际，过于追求新奇而忽视常规，容易分心，计划执行能力弱",
                "适合创意、研究、艺术、咨询、科技等需要创新和探索的领域。",
                "你带来新鲜感和智识刺激，但可能被认为不稳定或难以满足于平淡。",
                "学习结合创意与实践，培养执行力和专注力，同时享受探索的乐趣。",
                "爱因斯坦、达芬奇、乔布斯")
        ),
        "C" to mapOf(
            "low" to BigFiveInterpretation("低尽责性",
                "你更随性、灵活，不喜欢被严格的计划和规则束缚。你享受即兴，但可能缺乏持久性和条理性。",
                "适应性强、轻松愉快、善于应对变化、有创造力",
                "可能拖延、杂乱无章、不可靠、缺乏坚持",
                "适合灵活、自由的工作，如自由职业、创意行业、紧急救援。",
                "你带来轻松和乐趣，但可能因不守承诺或缺乏规划让伴侣困扰。",
                "建立简单的日常习惯（如每日待办清单），设定小目标并坚持，提高自律。",
                "许多艺术家和冒险家"),
            "moderate" to BigFiveInterpretation("中等尽责性",
                "你能在条理和灵活之间取得平衡，根据情况设定计划和调整，既有责任心也不过分僵化。",
                "可靠且灵活，能适应不同环境，效率与创意并重",
                "可能时好时坏，缺乏一贯的坚持",
                "适合需要一定结构但又需要灵活性的职业，如管理、销售、教育培训。",
                "你是可靠又灵活的伙伴，关系处理得宜。",
                "提升自我管理，保持稳定性，同时保留弹性空间。",
                "多数成功的专业人士"),
            "high" to BigFiveInterpretation("高尽责性",
                "你极其有条理、自律、负责，善于规划并坚持完成任务。你注重细节和成就，可靠且值得信赖。",
                "自律、高效、可靠、有条理、执行力强",
                "可能过于刻板、完美主义、压力大、难以放松",
                "适合高级管理、工程、法律、财务、医疗等需要高度负责和精确的领域。",
                "你是可靠的伴侣，但有时因过于注重规则而显得不够灵活。",
                "学会放松，接受适度的不完美，给自己安排休息时间。",
                "比尔·盖茨、沃伦·巴菲特、奥巴马")
        ),
        "E" to mapOf(
            "low" to BigFiveInterpretation("低外向性（内向）",
                "你倾向于安静、独立，喜欢独处和深度思考，社交活动会消耗你的能量。你更关注内部世界。",
                "专注、善于倾听、深度思考、独立自主、忠诚",
                "可能显得疏远、避免社交、表达不足、易被忽视",
                "适合需要独立思考和分析的工作，如编程、写作、研究、设计。",
                "你提供稳定和深度，但需主动表达情感，让伴侣了解你的内心。",
                "有意识地参与少量社交，培养表达技巧，同时珍惜独处时间。",
                "爱因斯坦、村上春树、比尔·盖茨（也是内向者）"),
            "moderate" to BigFiveInterpretation("中外向性（平衡）",
                "你能平衡社交与独处，既喜欢与人互动也享受安静时光。适应大多数社交环境。",
                "灵活，能适应不同社交场合，沟通良好，自我调节能力强",
                "可能在极端情况下不够突出",
                "适合需要合作但也可独立工作的职业，如项目管理、咨询、教育。",
                "你能在社交与独处间自如切换，关系较为平衡。",
                "根据情境调整行为，保持舒适区的同时拓展人际圈。",
                "多数中层管理者"),
            "high" to BigFiveInterpretation("高外向性（外向）",
                "你精力充沛、热情、善于社交，喜欢成为注意中心，从外部世界获取能量。",
                "社交能力强、乐观、主动、善于说服、团队合作佳",
                "可能过于依赖外部刺激，难以独处，有时不够细致，忽略深度",
                "适合销售、公关、演艺、管理、培训等需要人际互动的职业。",
                "你带来活力和趣味，但需注意倾听对方，平衡输出与接纳。",
                "培养耐心聆听，练习独处内省，建立深层关系。",
                "比尔·克林顿、奥普拉、汤姆·汉克斯")
        ),
        "A" to mapOf(
            "low" to BigFiveInterpretation("低宜人性",
                "你更注重自身利益和原则，对他人可能持怀疑态度，竞争性强，不易妥协。",
                "独立、果断、坚持己见、批判性强、不易被操纵",
                "可能缺乏同理心、人际关系紧张、过于好斗、不合作",
                "适合需要独立判断、评估和竞争的工作，如法律、金融分析、工程。",
                "你可能因过于直接而伤害对方，但真诚坦率也是优点，需要学会温和表达。",
                "练习换位思考，学习倾听和协商，培养温和的沟通方式。",
                "史蒂夫·乔布斯（曾被认为宜人性较低）"),
            "moderate" to BigFiveInterpretation("中宜人性",
                "你能够在关心他人和维护自身利益之间取得平衡，既合作也不失独立性。",
                "良好的合作能力，也能坚持原则，人际关系和谐",
                "可能优柔寡断，在冲突中摇摆",
                "适合需要合作但也要坚持立场的职业，如管理咨询、客户经理。",
                "你能兼顾自我与他人利益，关系处理得当。",
                "培养在合作中保持自我的能力，学习妥善处理分歧。",
                "大多数适应良好的职场人士"),
            "high" to BigFiveInterpretation("高宜人性",
                "你温暖、同情、信任他人，乐于合作，避免冲突，关心他人的福祉。",
                "善良、乐于助人、团队精神、忠诚、易于相处",
                "可能过于顺从、容易被利用、不敢表达立场、忽视自身需求",
                "适合教育、社会服务、医疗、人力资源、客户服务。",
                "你是理想的伴侣和朋友，但需学会设立界限，保护自己。",
                "学习说'不'，关注自我需求，在必要时要坚定立场。",
                "特蕾莎修女、马丁·路德·金")
        ),
        "N" to mapOf(
            "low" to BigFiveInterpretation("低神经质（情绪稳定）",
                "你通常情绪平稳，不易焦虑或紧张，能冷静应对压力，较少体验负面情绪。",
                "情绪稳定、抗压力强、乐观、理性、不易崩溃",
                "可能显得冷漠或缺乏激情，对他人情绪感知不敏感",
                "适合需要高压力承受能力的职业，如急诊医生、飞行员、执行官。",
                "你提供稳定和安全，但需注意体谅伴侣的情感需求。",
                "练习感知和表达情感，增加对他人情绪的敏感度。",
                "众多领袖和稳定型人物"),
            "moderate" to BigFiveInterpretation("中神经质",
                "你有适度的情绪反应，能够觉察压力并采取应对措施，但不会过度焦虑。",
                "能够体察情绪，适时调整，有良好自我调节能力",
                "偶尔会过度担忧，但能较快平复",
                "适合大部分职业，需注意压力管理。",
                "你情绪适度，能有效调节，关系平稳。",
                "加强放松技巧，保持健康生活方式，维持情绪平衡。",
                "大多数正常人"),
            "high" to BigFiveInterpretation("高神经质（情绪不稳定）",
                "你容易感到焦虑、抑郁、愤怒或紧张，对压力敏感，情绪波动较大。",
                "有深度情感，能够体察细腻的情绪，富有同理心",
                "易受负面情绪困扰，可能影响工作和人际关系，健康风险较高",
                "需要避免高压、高冲突的工作环境，适合支持性、人文关怀的职业。",
                "你情感丰富，但需要伴侣的理解和支持，学习管理情绪。",
                "练习正念、认知重塑、规律运动，寻求专业支持如心理咨询。",
                "许多艺术家和敏感者")
        )
    )

    private fun levelOf(percent: Int): String = when { percent <= 40 -> "低"; percent <= 60 -> "中"; else -> "高" }
    private fun levelKey(percent: Int): String = when (levelOf(percent)) { "低" -> "low"; "中" -> "moderate"; else -> "high" }

    /** 计分：answers 为每题 1-5 的自评，返回结果 */
    fun calculate(answers: List<Int>): BigFiveResult {
        require(answers.size == QUESTIONS.size) {
            "Big Five 需要 ${QUESTIONS.size} 个答案，实际收到 ${answers.size} 个"
        }
        require(answers.all { it in 1..5 }) {
            "Big Five 每个答案必须是 1 到 5"
        }
        val raw = mutableMapOf("O" to 0, "C" to 0, "E" to 0, "A" to 0, "N" to 0)
        answers.forEachIndexed { i, ans ->
            val q = QUESTIONS[i]
            val score = if (q.reverse) 6 - ans else ans
            raw[q.dimension] = raw.getOrDefault(q.dimension, 0) + score
        }
        val dims = listOf("O", "C", "E", "A", "N").map { code ->
            val score = raw[code]!!              // 0-50
            val percent = score * 2               // 0-100
            BigFiveDimensionResult(
                code = code,
                name = DIMENSION_NAMES[code]!!,
                description = DIMENSION_DESC[code]!!,
                score = score,
                percent = percent,
                level = levelOf(percent),
                interpretation = INTERP[code]!![levelKey(percent)]!!
            )
        }
        return BigFiveResult(dims, buildPortrait(dims))
    }

    private fun buildPortrait(dims: List<BigFiveDimensionResult>): String {
        val p = dims.associate { it.code to it.percent }
        val sb = StringBuilder()
        fun pick(code: String, hi: String, lo: String, mid: String) {
            sb.append(if (p[code]!! > 60) hi else if (p[code]!! < 40) lo else mid).append(" ")
        }
        pick("O", "你思想开放，乐于探索新事物。", "你务实传统，偏好稳定和熟悉。", "你能在传统与创新间保持平衡。")
        pick("C", "你高度自律、有条理，是可靠的执行者。", "你灵活随性，不喜欢被束缚。", "你既注重计划，也保留灵活性。")
        pick("E", "你热情外向，善于社交。", "你安静内敛，享受独处。", "你能在社交与独处中自如切换。")
        pick("A", "你温暖友善，乐于合作。", "你独立果断，但可能不够圆融。", "你能兼顾自我与他人利益。")
        pick("N", "你情绪敏感，易受压力影响。", "你情绪稳定，能冷静应对压力。", "你情绪适度，能有效调节。")
        sb.append("总的来说，你的人格特质组合形成了独特的行为模式和适应方式。大五人格描述的是倾向性，并非固定标签，你可以通过自我觉察和成长来调整。")
        return sb.toString()
    }
}
