package com.xuanji.app.domain.test

/**
 * 趣味人格测试合集
 * 4 个子测试：SBTI（傻乎乎大）、九型人格、DISC、性格色彩（FPA）。
 * 每套 10 题，每题 A/B/C/D 四选项，选项字母经 weights 映射到类型代码，计分后取最高分类型。
 * 并列时按该子测试的固定顺序取先出现的类型（确定性，不使用 random）。
 * 全离线、确定性计分，结果仅供娱乐参考。
 */

data class FunQuestion(
    val text: String,
    val options: List<Pair<String, String>>,  // 字母 A/B/C/D 到选项文本
    val weights: Map<String, String>          // 字母到类型/维度代码
)

data class FunResult(
    val subTest: String,
    val scores: Map<String, Int>,
    val code: String,
    val name: String,
    val interpretation: String
)

data class FunSubtestInfo(
    val id: String,
    val title: String,
    val badge: String,
    val desc: String
)

object FunTests {

    /** 由四个（文本 + 类型代码）构建一道题，保证 options 与 weights 一一对应 */
    private fun funQuestion(
        text: String,
        a: String, aCode: String,
        b: String, bCode: String,
        c: String, cCode: String,
        d: String, dCode: String
    ): FunQuestion = FunQuestion(
        text = text,
        options = listOf("A" to a, "B" to b, "C" to c, "D" to d),
        weights = mapOf("A" to aCode, "B" to bCode, "C" to cCode, "D" to dCode)
    )

    // ============ SBTI 傻乎乎大（S躺平 / B卷王 / T嘴炮 / I纠结） ============
    val SBTI_QUESTIONS: List<FunQuestion> = listOf(
        funQuestion(
            "周末早上你通常……",
            a = "睡到自然醒，被子是全世界最好的朋友。", aCode = "S",
            b = "六点起床晨跑，时间管理大师的一天开始了。", bCode = "B",
            c = "躺在床上刷手机，顺便点评一下全世界。", cCode = "T",
            d = "想睡又想起，躺着纠结半天到底起不起。", dCode = "I"
        ),
        funQuestion(
            "面对一堆任务你……",
            a = "先躺一会儿，ddl 才是第一生产力。", aCode = "S",
            b = "列好清单逐项打勾，今天必须卷完。", bCode = "B",
            c = "嘴上说「包在我身上」，然后继续跟人聊天。", cCode = "T",
            d = "反复盘算先做哪个，最后什么都没做。", dCode = "I"
        ),
        funQuestion(
            "看到别人深夜发朋友圈晒加班你……",
            a = "内心毫无波澜，甚至有点想睡。", aCode = "S",
            b = "暗暗较劲，明天比 TA 走得更晚。", bCode = "B",
            c = "评论一句「卷不动了」，然后接着刷。", cCode = "T",
            d = "开始焦虑自己是不是不够努力。", dCode = "I"
        ),
        funQuestion(
            "朋友找你吐槽领导你……",
            a = "边听边点头，内心已经神游天外。", aCode = "S",
            b = "立刻给出解决方案和分步行动计划。", bCode = "B",
            c = "帮朋友把领导从头到脚声情并茂地骂一遍。", cCode = "T",
            d = "陪着一起纠结，最后两个人一起 emo。", dCode = "I"
        ),
        funQuestion(
            "群里有人喊「一起拼奶茶」你……",
            a = "看到消息已经过了一小时，算了不喝了。", aCode = "S",
            b = "立刻算出满减规则，人均省三块五。", bCode = "B",
            c = "第一个回复「我，大杯，加料」，然后发现报错名字。", cCode = "T",
            d = "想喝又怕长胖，纠结十分钟后回了个「行吧」。", dCode = "I"
        ),
        funQuestion(
            "面对「躺平还是内卷」的灵魂拷问你……",
            a = "躺平，不解释。", aCode = "S",
            b = "内卷，不停。", bCode = "B",
            c = "嘴上躺平背地里卷，这就是艺术。", cCode = "T",
            d = "想躺又想卷，最后决定先纠结一下。", dCode = "I"
        ),
        funQuestion(
            "上班摸鱼时你……",
            a = "光明正大地趴在桌上，领导来了再说。", aCode = "S",
            b = "假装在工作，其实在偷偷学新技能。", bCode = "B",
            c = "和同事激情讨论八卦，声音盖过全办公室。", cCode = "T",
            d = "一边摸鱼一边心跳加速，担心被领导发现。", dCode = "I"
        ),
        funQuestion(
            "别人问你「周末打算去哪玩」你……",
            a = "在家，床即宇宙。", aCode = "S",
            b = "报了三个课，顺便约了两个客户。", bCode = "B",
            c = "说了一堆计划，最后哪个都没去成。", cCode = "T",
            d = "还没想好，让我先纠结一下再回答。", dCode = "I"
        ),
        funQuestion(
            "你的桌面状态是……",
            a = "除了外卖盒什么都找不到，但我无所谓。", aCode = "S",
            b = "分类归档、标签齐整，谁碰一下我都不乐意。", bCode = "B",
            c = "键盘边堆满零食袋，那是灵感的源泉。", cCode = "T",
            d = "时而整洁时而混乱，取决于今天是哪种人格。", dCode = "I"
        ),
        funQuestion(
            "你的人生终极追求是……",
            a = "吃饱、睡好、别加班。", aCode = "S",
            b = "站上顶峰，让所有人都记住我。", bCode = "B",
            c = "活得精彩，主要是让朋友圈活得精彩。", cCode = "T",
            d = "找到正确答案，然后再纠结要不要选它。", dCode = "I"
        )
    )

    // ============ 九型人格（1 完美 / 2 助人 / 3 成就 / 4 浪漫 / 5 观察 / 6 忠诚 / 7 享乐 / 8 挑战 / 9 和平） ============
    val ENNEAGRAM_QUESTIONS: List<FunQuestion> = listOf(
        funQuestion(
            "面对冲突时你更倾向于……",
            a = "把对错掰扯清楚，规则不能乱。", aCode = "1",
            b = "先照顾大家情绪，别伤了和气。", bCode = "2",
            c = "直接摊牌，速战速决。", cCode = "8",
            d = "找个安静角落，等风波自己过去。", dCode = "9"
        ),
        funQuestion(
            "团队合作中你最在意……",
            a = "效率第一，先干出成绩。", aCode = "3",
            b = "气氛和谐，谁也别掉队。", bCode = "9",
            c = "方案经得起推敲，逻辑要严谨。", cCode = "5",
            d = "过程好玩，别搞得像开追悼会。", dCode = "7"
        ),
        funQuestion(
            "被误解时你通常会……",
            a = "反复解释，直到对方明白为止。", aCode = "1",
            b = "表面说「没事」，心里已经演完一部电影。", bCode = "4",
            c = "懒得解释，清者自清。", cCode = "5",
            d = "直接对线，把话说开。", dCode = "8"
        ),
        funQuestion(
            "朋友深夜 emo 找你倾诉你……",
            a = "先分析原因，再给出 1234 条建议。", aCode = "5",
            b = "放下手头的事，陪 TA 熬到天亮。", bCode = "2",
            c = "讲两个段子，先逗笑了再说。", cCode = "7",
            d = "告诉 TA「别怕，我永远站你这边」。", dCode = "6"
        ),
        funQuestion(
            "面对未知的风险你……",
            a = "预演所有坏情况，plan B、C、D 都备好。", aCode = "6",
            b = "相信自己，冲就完了。", bCode = "3",
            c = "先评估值不值，不行果断撤。", cCode = "8",
            d = "想太多太累，走一步看一步。", dCode = "9"
        ),
        funQuestion(
            "你最怕别人说你……",
            a = "不负责任。", aCode = "1",
            b = "冷漠自私。", bCode = "2",
            c = "平庸没用。", cCode = "3",
            d = "肤浅俗气。", dCode = "4"
        ),
        funQuestion(
            "难得的周末你想怎么过……",
            a = "宅家充电，独处回血。", aCode = "5",
            b = "组局聚会，热闹至上。", bCode = "7",
            c = "陪家人朋友，谁需要我我就去哪。", cCode = "2",
            d = "睡到自然醒，谁也别安排我。", dCode = "9"
        ),
        funQuestion(
            "遇到不公平的事你……",
            a = "据理力争，绝不让步。", aCode = "8",
            b = "先记小本本，时机成熟再出手。", bCode = "6",
            c = "当面指出，原则问题不能含糊。", cCode = "1",
            d = "不吭声，但心里跟明镜似的。", dCode = "9"
        ),
        funQuestion(
            "给朋友挑生日礼物你……",
            a = "追求「最懂 TA」的惊喜效果。", aCode = "4",
            b = "实用优先，性价比算得明明白白。", bCode = "5",
            c = "贵就完事，面子要给足。", cCode = "3",
            d = "看缘分，碰到什么买什么。", dCode = "7"
        ),
        funQuestion(
            "你觉得自己更像团队里的……",
            a = "定海神针，靠谱担当。", aCode = "6",
            b = "气氛组组长，走到哪热闹到哪。", bCode = "7",
            c = "干活最多的老黄牛。", cCode = "2",
            d = "文艺担当，氛围感艺术家。", dCode = "4"
        )
    )

    // ============ DISC（D 支配 / I 影响 / S 稳健 / C 谨慎） ============
    val DISC_QUESTIONS: List<FunQuestion> = listOf(
        funQuestion(
            "接手一个新项目，你首先会……",
            a = "直接开干，边做边调整。", aCode = "D",
            b = "召集大家头脑风暴，先热闹起来。", bCode = "I",
            c = "按部就班，一步步推进。", cCode = "S",
            d = "先研究资料，把方案想清楚再说。", dCode = "C"
        ),
        funQuestion(
            "会议上有人反对你的方案，你……",
            a = "强势回击，用气势压住对方。", aCode = "D",
            b = "换个说法继续推销，把气氛搞起来。", bCode = "I",
            c = "耐心听完，尽量不伤和气。", cCode = "S",
            d = "逐条列数据反驳，用逻辑碾压。", dCode = "C"
        ),
        funQuestion(
            "你的工作风格更接近……",
            a = "目标导向，结果说话。", aCode = "D",
            b = "人脉导向，关系先行。", bCode = "I",
            c = "节奏稳定，保质保量。", cCode = "S",
            d = "细节控，事事求完美。", dCode = "C"
        ),
        funQuestion(
            "面对突发状况你……",
            a = "当机立断，先拍板再说。", aCode = "D",
            b = "号召大家一起想办法，气氛不能垮。", bCode = "I",
            c = "稳住大家，按应急预案来。", cCode = "S",
            d = "先收集信息、分析原因，再谨慎处理。", dCode = "C"
        ),
        funQuestion(
            "朋友说你最大的缺点是……",
            a = "太强势，不给别人说话的机会。", aCode = "D",
            b = "太爱说，一开口就停不下来。", bCode = "I",
            c = "太慢热，从来不会主动。", cCode = "S",
            d = "太较真，老纠结细节。", dCode = "C"
        ),
        funQuestion(
            "你最喜欢的工作环境是……",
            a = "我说了算的那种。", aCode = "D",
            b = "热闹有活力，天天像开派对。", bCode = "I",
            c = "稳定和谐，大家互相照应。", cCode = "S",
            d = "规范清晰，流程明明白白。", dCode = "C"
        ),
        funQuestion(
            "团队聚餐时你通常……",
            a = "安排座位，主持大局。", aCode = "D",
            b = "负责讲段子，全场笑点担当。", bCode = "I",
            c = "默默帮大家倒茶夹菜。", cCode = "S",
            d = "安静吃饭，暗中观察每个人。", dCode = "C"
        ),
        funQuestion(
            "你做事最看重……",
            a = "效率与结果。", aCode = "D",
            b = "过程开心，氛围融洽。", bCode = "I",
            c = "稳步前进，不出差错。", cCode = "S",
            d = "数据准确，逻辑自洽。", dCode = "C"
        ),
        funQuestion(
            "被批评的时候你……",
            a = "立刻反驳，别想让我认输。", aCode = "D",
            b = "笑着打哈哈，把话题岔开。", bCode = "I",
            c = "默默记下，心里有点难受。", cCode = "S",
            d = "认真分析，说得对就改。", dCode = "C"
        ),
        funQuestion(
            "你觉得自己是……",
            a = "天生的领导者。", aCode = "D",
            b = "天生的开心果。", bCode = "I",
            c = "天生的倾听者。", cCode = "S",
            d = "天生的分析家。", dCode = "C"
        )
    )

    // ============ 性格色彩 FPA（黄力量 / 红热情 / 绿平和 / 蓝理性） ============
    val COLOR_QUESTIONS: List<FunQuestion> = listOf(
        funQuestion(
            "买东西时你更看重……",
            a = "质量耐用，一分钱一分货。", aCode = "蓝",
            b = "眼缘，喜欢就买。", bCode = "红",
            c = "实惠，够用就行。", cCode = "绿",
            d = "档次，要买就买好的。", dCode = "黄"
        ),
        funQuestion(
            "情绪激动的时候你会……",
            a = "先让自己冷静，理性分析一下。", aCode = "蓝",
            b = "直接爆发，情绪全写在脸上。", bCode = "红",
            c = "自己慢慢消化，不想让人担心。", cCode = "绿",
            d = "据理力争，必须讨个说法。", dCode = "黄"
        ),
        funQuestion(
            "朋友眼中的你更接近……",
            a = "热心肠，风风火火。", aCode = "红",
            b = "老好人，随和温吞。", bCode = "绿",
            c = "冷静理智，话不多但句句在理。", cCode = "蓝",
            d = "说一不二，行动力极强。", dCode = "黄"
        ),
        funQuestion(
            "面对「要不要改变」你……",
            a = "变，而且要快，先动起来。", aCode = "黄",
            b = "变不变都行，随缘。", bCode = "绿",
            c = "先想清楚利弊，再决定。", cCode = "蓝",
            d = "想变就变，全凭心情。", dCode = "红"
        ),
        funQuestion(
            "你最喜欢别人夸你……",
            a = "有魄力。", aCode = "黄",
            b = "有感染力。", bCode = "红",
            c = "好相处。", cCode = "绿",
            d = "有头脑。", dCode = "蓝"
        ),
        funQuestion(
            "旅行时你更喜欢……",
            a = "提前做好攻略，精确到小时。", aCode = "蓝",
            b = "走到哪玩到哪，惊喜才最重要。", bCode = "红",
            c = "找个慢节奏的小城住下。", cCode = "绿",
            d = "挑战极限，特种兵式打卡。", dCode = "黄"
        ),
        funQuestion(
            "工作遇到难题你……",
            a = "迎难而上，死磕到底。", aCode = "黄",
            b = "换个思路，先给自己找点乐子。", bCode = "红",
            c = "稳住心态，一步一步来。", cCode = "绿",
            d = "抽丝剥茧，分析到根上。", dCode = "蓝"
        ),
        funQuestion(
            "你更适合做……",
            a = "团队里的指挥官。", aCode = "黄",
            b = "团队里的气氛担当。", bCode = "红",
            c = "团队里的协调者。", cCode = "绿",
            d = "团队里的军师。", dCode = "蓝"
        ),
        funQuestion(
            "你给人的第一印象是……",
            a = "热情主动，自来熟。", aCode = "红",
            b = "温柔安静，让人舒服。", bCode = "绿",
            c = "沉稳专业，有点距离感。", cCode = "蓝",
            d = "强势干练，气场两米八。", dCode = "黄"
        ),
        funQuestion(
            "你的座右铭是……",
            a = "行动胜于空谈。", aCode = "黄",
            b = "及时行乐，开心最重要。", bCode = "红",
            c = "平和处世，随遇而安。", cCode = "绿",
            d = "谋定而后动。", dCode = "蓝"
        )
    )

    // ============ 子测试元信息与类型定义 ============
    val SUBTESTS: List<FunSubtestInfo> = listOf(
        FunSubtestInfo("SBTI", "SBTI 处事风格", "10 题 · 4 种类型", "以躺平、进取、表达、纠结四类处事风格，反映你的生活与工作姿态。"),
        FunSubtestInfo("Enneagram", "九型人格", "10 题 · 9 种类型", "经典九型人格测评，识别驱动你行为的核心动机与性格模式。"),
        FunSubtestInfo("DISC", "DISC 行为风格", "10 题 · 4 种风格", "DISC 行为风格测评，分析你在职场与社交中的行为倾向。"),
        FunSubtestInfo("Color", "性格色彩", "10 题 · 4 种颜色", "FPA 性格色彩测评，从力量、热情、平和、理性四个维度刻画性格。")
    )

    private val SBTI_ORDER = listOf("S", "B", "T", "I")
    private val ENNEAGRAM_ORDER = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9")
    private val DISC_ORDER = listOf("D", "I", "S", "C")
    private val COLOR_ORDER = listOf("黄", "红", "绿", "蓝")

    private val SBTI_NAMES = mapOf(
        "S" to "S躺平", "B" to "B卷王", "T" to "T嘴炮", "I" to "I纠结"
    )
    private val ENNEAGRAM_NAMES = mapOf(
        "1" to "完美型", "2" to "助人型", "3" to "成就型", "4" to "浪漫型",
        "5" to "观察型", "6" to "忠诚型", "7" to "享乐型", "8" to "挑战型", "9" to "和平型"
    )
    private val DISC_NAMES = mapOf(
        "D" to "支配型", "I" to "影响型", "S" to "稳健型", "C" to "谨慎型"
    )
    private val COLOR_NAMES = mapOf(
        "黄" to "黄色 · 力量型", "红" to "红色 · 热情型", "绿" to "绿色 · 平和型", "蓝" to "蓝色 · 理性型"
    )

    fun questionsOf(subTest: String): List<FunQuestion> = when (subTest) {
        "SBTI" -> SBTI_QUESTIONS
        "Enneagram" -> ENNEAGRAM_QUESTIONS
        "DISC" -> DISC_QUESTIONS
        else -> COLOR_QUESTIONS
    }

    /** 各子测试的类型固定顺序，用于并列时取先出现的类型 */
    fun typeOrder(subTest: String): List<String> = when (subTest) {
        "SBTI" -> SBTI_ORDER
        "Enneagram" -> ENNEAGRAM_ORDER
        "DISC" -> DISC_ORDER
        else -> COLOR_ORDER
    }

    fun typeNames(subTest: String): Map<String, String> = when (subTest) {
        "SBTI" -> SBTI_NAMES
        "Enneagram" -> ENNEAGRAM_NAMES
        "DISC" -> DISC_NAMES
        else -> COLOR_NAMES
    }

    // ============ 解读 ============
    private val SBTI_INTERP = mapOf(
        "S" to "类型名：S躺平\n幽默描述：躺平是态度，舒服是天赋。你能躺着绝不坐着，能坐着绝不站着，把「少做少错」奉为人生第一法则，看似慵懒，实则是看透了生活的本质。\n灵魂心声：只要我躺得够平，内卷就追不上我。",
        "B" to "类型名：B卷王\n幽默描述：你是卷王之王，自带红眼特效。别人还在做梦你已经在跑，别人开始跑你已经开始冲刺，不是在努力，就是在努力的路上，卷得自己都心疼自己。\n灵魂心声：我卷的不是别人，是那个不够努力的自己。",
        "T" to "类型名：T嘴炮\n幽默描述：你是嘴上功夫十级选手，上能点评国计民生，下能八卦同事午饭。行动？那是什么，能吃吗？你的世界里没有解决不了的问题，只有懒得去解决的问题。\n灵魂心声：道理我都懂，就是做不动。",
        "I" to "类型名：I纠结\n幽默描述：你是选择困难症晚期患者，从早上吃什么到人生方向，每一个决定都是一场内心大戏，A 和 B 之间永远差一个 C，而 C 永远慢一步出现。\n灵魂心声：再给我一分钟，我马上就好。"
    )

    private val ENNEAGRAM_INTERP = mapOf(
        "1" to "类型核心：你是行走的规则校准器，眼里容不下瑕疵，心中自带一杆秤，凡事追求正确与完美。\n特质：自律、严谨、有原则、注重细节，也容易对自己和别人要求过高。\n成长方向：试着对世界说「差不多得了」，给自己和别人都留一点余地。\n名人代表：包拯。",
        "2" to "类型核心：你是行走的暖宝宝，总在别人开口前就发现需求，把「被需要」当作最高奖赏。\n特质：热情、体贴、慷慨、善解人意，但容易忽略自己的需求。\n成长方向：学会先照顾自己，被爱不一定要靠付出换来。\n名人代表：特蕾莎修女。",
        "3" to "类型核心：你是舞台中央的追光灯，渴望被看见、被认可，用成绩单证明自己的价值。\n特质：目标感强、执行力高、形象管理一流，偶尔累到忘了自己是谁。\n成长方向：停下来问问自己：不优秀的时候，你还喜欢我吗？\n名人代表：乔布斯。",
        "4" to "类型核心：你是灵魂深处的诗人，对美和意义有着近乎偏执的追求，情绪是你的调色盘。\n特质：敏感、独特、有创造力，容易陷入「我好像和他们不一样」的孤独。\n成长方向：拥抱平凡，你本身就是独一无二的风景。\n名人代表：林黛玉。",
        "5" to "类型核心：你是人形图书馆，喜欢躲在安静角落观察世界，知识是铠甲，边界是安全区。\n特质：理性、专注、博学、独立，社交电量总是很快耗尽。\n成长方向：走出书页，世界需要你的参与而不是旁观。\n名人代表：爱因斯坦。",
        "6" to "类型核心：你是团队的定海神针，时刻预判风险，用警惕换安全感，认定了谁就掏心掏肺。\n特质：忠诚、可靠、谨慎、有责任心，但容易过度焦虑。\n成长方向：世界没那么危险，你可以试着相信一点点。\n名人代表：诸葛亮。",
        "7" to "类型核心：你是行走的快乐喷泉，见不得冷场，受不了无聊，把「开心」当成人生第一要务。\n特质：乐观、机灵、点子多、人缘好，只是不太擅长面对痛苦。\n成长方向：允许自己慢下来，拥抱平凡和偶尔的无聊。\n名人代表：周星驰。",
        "8" to "类型核心：你是天生的老大，气场全开，喜欢掌控局面，罩着身边的人也要求绝对的忠诚。\n特质：果断、勇敢、有担当、护短，但偶尔控制欲上头。\n成长方向：放下铠甲，柔软不是软弱。\n名人代表：拿破仑。",
        "9" to "类型核心：你是团队里的和事佬，自动屏蔽冲突，习惯把所有人的感受都排在前面。\n特质：随和、包容、好相处，就是常常忘了自己也该被照顾。\n成长方向：你的感受同样重要，学会为自己站出来。\n名人代表：李安。"
    )

    private val DISC_INTERP = mapOf(
        "D" to "类型名：D 支配型\n特质：目标明确、果断强势、行动力爆表，天生自带决策引擎。\n优势：敢闯敢拼、不怕冲突、能扛事，危机时刻是团队的定心丸。\n盲区：容易急躁，听不进反对意见，对慢节奏的人缺乏耐心。\n工作风格：适合需要决策和开拓的岗位，比如管理、销售、创业，最怕被人管着做无意义的事。",
        "I" to "类型名：I 影响型\n特质：热情开朗、口才一流、感染力强，走到哪都是气氛担当。\n优势：人脉广、能调动情绪、善于说服，让团队气氛轻松活跃。\n盲区：容易三分钟热度、讨厌细节，承诺时爽快、兑现时看心情。\n工作风格：适合公关、主持、市场、培训等需要「刷脸」的岗位，最怕封闭无趣的重复劳动。",
        "S" to "类型名：S 稳健型\n特质：温和可靠、耐心十足、以和为贵，是团队里的情绪稳定器。\n优势：执行力稳定、乐于助人、从不掉链子，是大家最愿意托付的人。\n盲区：害怕冲突、不愿改变，遇到急事容易犹豫，委屈爱往肚里咽。\n工作风格：适合稳定协作的环境，比如客服、行政、后勤、项目管理，最怕突然的大变动。",
        "C" to "类型名：C 谨慎型\n特质：逻辑严密、细节控、条理分明，凡事讲究依据和标准。\n优势：精准可靠，是 bug 终结者，方案经得起推敲，质量有保障。\n盲区：过度较真、完美主义、决策偏慢，容易给人「不好搞」的印象。\n工作风格：适合财务、研发、质检、数据分析等精细岗位，最怕拍脑袋的糊涂决策。"
    )

    private val COLOR_INTERP = mapOf(
        "黄" to "颜色：黄色 · 力量型\n描述：黄色是行动派的力量色，你目标感极强，天生就是要掌控局面的人。\n特质：果断、自信、行动力爆棚，说话直接，讨厌磨叽。\n适合：当领导、开疆拓土，越有挑战你越兴奋。\n成长：学会慢下来听别人说话，强势之外多一点温度。",
        "红" to "颜色：红色 · 热情型\n描述：红色是热情奔放的火焰，你活力四射，走到哪里都是焦点。\n特质：乐观、开朗、自来熟，情绪来得快去得也快。\n适合：社交、创意、舞台类的角色，天生聚光灯体质。\n成长：情绪上头时先深呼吸，热情也要学会收放。",
        "绿" to "颜色：绿色 · 平和型\n描述：绿色是安静平和的草原，你与世无争，是身边人的情绪避风港。\n特质：温和、包容、有耐心，习惯把别人的需求放在前面。\n适合：协调、陪伴、服务类的角色，是团队里的粘合剂。\n成长：你的需求也值得被满足，学会说「我想要」。",
        "蓝" to "颜色：蓝色 · 理性型\n描述：蓝色是冷静深邃的海洋，你逻辑清晰，遇事第一反应是分析。\n特质：理性、严谨、克制，感情不外露，说话讲究依据。\n适合：研究、分析、策划类的角色，天生军师命。\n成长：适当释放情感，理性之外也需要温度。"
    )

    private fun interpretationOf(subTest: String, code: String): String = when (subTest) {
        "SBTI" -> SBTI_INTERP[code] ?: ""
        "Enneagram" -> ENNEAGRAM_INTERP[code] ?: ""
        "DISC" -> DISC_INTERP[code] ?: ""
        else -> COLOR_INTERP[code] ?: ""
    }

    /** 计分：answers 为每题所选字母（A/B/C/D），映射代码 +1，取最高分类型（并列按固定顺序取先出现者） */
    fun calculate(subTest: String, answers: List<String>): FunResult {
        val questions = questionsOf(subTest)
        val order = typeOrder(subTest)
        val names = typeNames(subTest)
        val scores = mutableMapOf<String, Int>()
        order.forEach { scores[it] = 0 }
        questions.forEachIndexed { i, q ->
            val letter = answers.getOrNull(i) ?: return@forEachIndexed
            val code = q.weights[letter] ?: return@forEachIndexed
            scores[code] = scores.getOrDefault(code, 0) + 1
        }
        var best = order.first()
        order.drop(1).forEach { code ->
            if ((scores[code] ?: 0) > (scores[best] ?: 0)) best = code
        }
        return FunResult(
            subTest = subTest,
            scores = scores.toMap(),
            code = best,
            name = names[best] ?: best,
            interpretation = interpretationOf(subTest, best)
        )
    }
}
