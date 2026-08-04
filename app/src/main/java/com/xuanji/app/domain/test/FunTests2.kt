package com.xuanji.app.domain.test

/**
 * 趣味人格测试合集 2
 * 4 个子测试：菲尔人格、牛马浓度、恋爱16型、恋爱说明书。
 * 菲尔人格/牛马浓度按总分分档；恋爱16型按 E/I、S/N、T/F、J/P 四维度组合；
 * 恋爱说明书按最高动物类型判定。
 * 并列时按该子测试的固定顺序取先出现的类型（确定性，不使用 random）。
 * 全离线、确定性计分，结果仅供娱乐参考。
 */

data class Fun2Question(
    val text: String,
    val options: List<Pair<String, String>>,  // 字母 A/B/C/D 到选项文本
    val weights: Map<String, String>          // 字母到类型/得分代码
)

data class Fun2Result(
    val subTest: String,
    val scores: Map<String, Int>,
    val code: String,
    val name: String,
    val interpretation: String
)

data class Fun2SubtestInfo(
    val id: String,
    val title: String,
    val badge: String,
    val desc: String
)

object FunTests2 {

    /** 由四个（文本 + 类型/得分代码）构建一道题，保证 options 与 weights 一一对应 */
    private fun fun2Question(
        text: String,
        a: String, aCode: String,
        b: String, bCode: String,
        c: String, cCode: String,
        d: String, dCode: String
    ): Fun2Question = Fun2Question(
        text = text,
        options = listOf("A" to a, "B" to b, "C" to c, "D" to d),
        weights = mapOf("A" to aCode, "B" to bCode, "C" to cCode, "D" to dCode)
    )

    /** 双选项题目：恋爱16型专用（A/B 二选一，映射两个维度字母） */
    private fun fun2Question2(
        text: String,
        a: String, aCode: String,
        b: String, bCode: String
    ): Fun2Question = Fun2Question(
        text = text,
        options = listOf("A" to a, "B" to b),
        weights = mapOf("A" to aCode, "B" to bCode)
    )

    // ============ 菲尔人格（A/B/C/D 各记 1/2/3/4 分，总分分档） ============
    val PHIL_QUESTIONS: List<Fun2Question> = listOf(
        fun2Question(
            "早上闹钟响起时你通常……",
            a = "随手一按，睡到自然醒再说。", aCode = "1",
            b = "赖床十分钟，恋恋不舍地爬起来。", bCode = "2",
            c = "闹钟一响就起，绝不赖床。", cCode = "3",
            d = "根本不需要闹钟，我比它醒得早。", dCode = "4"
        ),
        fun2Question(
            "在热闹的人群里你通常是……",
            a = "安安静静待在角落的旁观者。", aCode = "1",
            b = "偶尔插一句话的参与者。", bCode = "2",
            c = "带节奏聊天的核心人物。", cCode = "3",
            d = "负责组织全场的主持人。", dCode = "4"
        ),
        fun2Question(
            "面对「谁来拍板」这个问题你……",
            a = "都行，你们定就好，我没意见。", aCode = "1",
            b = "大家一起商量着来，集思广益。", bCode = "2",
            c = "我可以先提个方案供大家参考。", cCode = "3",
            d = "听我的，出了事我负责。", dCode = "4"
        ),
        fun2Question(
            "你的日常节奏更接近……",
            a = "慢悠悠的，做多少算多少。", aCode = "1",
            b = "差不多就行，别把自己累着。", bCode = "2",
            c = "有条不紊，按时保质完成。", cCode = "3",
            d = "火力全开，进度必须攥在我手里。", dCode = "4"
        ),
        fun2Question(
            "朋友之间起了争执你……",
            a = "赶紧躲远点，别殃及到我。", aCode = "1",
            b = "等他们吵完再上去劝两句。", bCode = "2",
            c = "主动出面调解，各打五十大板。", cCode = "3",
            d = "直接拍板「都听我的，这事翻篇」。", dCode = "4"
        ),
        fun2Question(
            "你的口头禅更接近……",
            a = "「随便」「都行」「你决定」。", aCode = "1",
            b = "「再看看」「也不着急」。", bCode = "2",
            c = "「我觉得可以这样试试」。", cCode = "3",
            d = "「就这么定了，不纠结」。", dCode = "4"
        ),
        fun2Question(
            "一周里你最喜欢的时间段是……",
            a = "睡懒觉的悠闲早晨。", aCode = "1",
            b = "和朋友小聚的傍晚。", bCode = "2",
            c = "高效完成任务的下午。", cCode = "3",
            d = "掌控全场的关键会议。", dCode = "4"
        ),
        fun2Question(
            "被突然点名发言时你……",
            a = "心里一紧，能省则省说两句。", aCode = "1",
            b = "简单说两句，不抢风头。", bCode = "2",
            c = "有条理地把观点讲清楚。", cCode = "3",
            d = "侃侃而谈，顺手把气氛拿捏住。", dCode = "4"
        ),
        fun2Question(
            "你对「做决定」这件事的态度是……",
            a = "能不做就不做，别人决定最好。", aCode = "1",
            b = "做也行，不做也行，随缘。", bCode = "2",
            c = "该做就做，不拖泥带水。", cCode = "3",
            d = "必须由我来定，而且要立刻定。", dCode = "4"
        ),
        fun2Question(
            "你理想中的自己更像……",
            a = "归隐山林的世外高人。", aCode = "1",
            b = "温和从容的老友。", bCode = "2",
            c = "干练靠谱的中坚力量。", cCode = "3",
            d = "号令一方的王者。", dCode = "4"
        )
    )

    // ============ 牛马浓度（A/B/C/D 各记 4/3/2/1 分，总分分档） ============
    val COWHORSE_QUESTIONS: List<Fun2Question> = listOf(
        fun2Question(
            "周一早上醒来你……",
            a = "闹钟没响就醒了，脑子里全是今天的工作。", aCode = "4",
            b = "听到闹钟一秒弹起，边洗漱边翻工作群。", bCode = "3",
            c = "赖床五分钟，磨磨蹭蹭爬起来。", cCode = "2",
            d = "心情不好就请假，工作先放一放。", dCode = "1"
        ),
        fun2Question(
            "领导晚上十点发消息「在吗」你……",
            a = "秒回「在的，您说」，哪怕已经躺下。", aCode = "4",
            b = "假装没看到，过十分钟再回。", bCode = "3",
            c = "明早再回，有事明天说。", cCode = "2",
            d = "直接回「下班了，明天上班处理」。", dCode = "1"
        ),
        fun2Question(
            "加班这件事你……",
            a = "主动留下，做完才安心，这就是命。", aCode = "4",
            b = "同事都加我就加，不然显得我不努力。", bCode = "3",
            c = "能不加就不加，磨到点准时走。", cCode = "2",
            d = "从不加班，到点准时消失。", dCode = "1"
        ),
        fun2Question(
            "面对年终考核的 KPI 你……",
            a = "逐条拉满，超额完成才踏实。", aCode = "4",
            b = "完成得差不多，别垫底就行。", bCode = "3",
            c = "及格万岁，多一分都算我输。", cCode = "2",
            d = "KPI 关我什么事，我只关心我学到什么。", dCode = "1"
        ),
        fun2Question(
            "群里艾特你处理杂活你……",
            a = "马上接手，还追问「还有别的吗」。", aCode = "4",
            b = "接下来做，但心里默默吐槽两句。", bCode = "3",
            c = "装没看见，等别人接。", cCode = "2",
            d = "直接说「这不是我的活儿」。", dCode = "1"
        ),
        fun2Question(
            "你的通勤状态是……",
            a = "地铁上还在改方案，站着也能办公。", aCode = "4",
            b = "通勤路上回复消息，见缝插针处理工作。", bCode = "3",
            c = "戴上耳机闭目养神，谁也别打扰。", cCode = "2",
            d = "在家远程办公，通勤是什么？", dCode = "1"
        ),
        fun2Question(
            "领导问「这块谁顶上」你……",
            a = "主动举手，责任越大越光荣。", aCode = "4",
            b = "犹豫一下，最后还是接了。", bCode = "3",
            c = "低头假装看手机，千万别点我。", cCode = "2",
            d = "笑着反问「顶什么？我档期很满」。", dCode = "1"
        ),
        fun2Question(
            "公司组织团建你……",
            a = "积极参加，还主动张罗破冰游戏。", aCode = "4",
            b = "会去，但坐在角落吃吃喝喝。", bCode = "3",
            c = "找借口不去，在家躺着不香吗。", cCode = "2",
            d = "直接拒绝，谁也别想占用我的休息时间。", dCode = "1"
        ),
        fun2Question(
            "工资到账那一刻你……",
            a = "先看扣了多少税，再算算自己值多少钱。", aCode = "4",
            b = "默默感叹一声「牛马费到手了」。", bCode = "3",
            c = "开心一下，然后继续回去上班。", cCode = "2",
            d = "瞄一眼就关掉，反正也不指望这点。", dCode = "1"
        ),
        fun2Question(
            "午夜十二点你大概率在……",
            a = "改第 N 版 PPT，明早还要交。", aCode = "4",
            b = "回复工作消息，边回边叹气。", bCode = "3",
            c = "刷手机放松，但脑子里还转着明天的事。", cCode = "2",
            d = "已经睡了三小时，明天休一天。", dCode = "1"
        )
    )

    // ============ 恋爱16型（每维度 3 题，A/B 二选一） ============
    val LOVE16_QUESTIONS: List<Fun2Question> = listOf(
        // E/I 维度
        fun2Question2(
            "恋爱初期你更享受哪种见面方式？",
            a = "约上朋友一起热闹聚会，人多更有意思。", aCode = "E",
            b = "两个人找个安静的地方独处聊天。", bCode = "I"
        ),
        fun2Question2(
            "约会时你的「电量」主要来自……",
            a = "人群、热闹、说个不停。", aCode = "E",
            b = "独处、慢聊、深度交流。", bCode = "I"
        ),
        fun2Question2(
            "恋爱中遇到烦心事你……",
            a = "立刻说出来，拉着 TA 和朋友们倾诉。", aCode = "E",
            b = "先自己消化，想清楚了再说。", bCode = "I"
        ),
        // S/N 维度
        fun2Question2(
            "你更在意恋爱里的……",
            a = "日常细节：记住对方爱吃什么、几点睡。", aCode = "S",
            b = "未来想象：想象我们十年后的生活画面。", bCode = "N"
        ),
        fun2Question2(
            "挑选约会地点你倾向……",
            a = "熟悉的老地方，稳妥不出错。", aCode = "S",
            b = "没去过的新奇去处，制造惊喜。", bCode = "N"
        ),
        fun2Question2(
            "回忆心动时刻，你想起的更多是……",
            a = "具体画面：那天 TA 穿的衣服、说的话。", aCode = "S",
            b = "一种感觉：那种怦然心动的氛围。", bCode = "N"
        ),
        // T/F 维度
        fun2Question2(
            "恋人生气时你首先会……",
            a = "先讲道理，把对错掰扯清楚。", aCode = "T",
            b = "先哄情绪，道理之后再说。", bCode = "F"
        ),
        fun2Question2(
            "面对「晚上吃什么」你……",
            a = "理性分析：哪家评分高、哪家划算。", aCode = "T",
            b = "感性决定：今天突然就好想吃火锅。", bCode = "F"
        ),
        fun2Question2(
            "你表达爱意的方式更接近……",
            a = "行动派：帮你解决问题、安排好一切。", aCode = "T",
            b = "情感派：甜言蜜语、小惊喜不断。", bCode = "F"
        ),
        // J/P 维度
        fun2Question2(
            "你的约会计划通常是……",
            a = "提前三天规划好，按清单执行。", aCode = "J",
            b = "临时起意，说走就走。", bCode = "P"
        ),
        fun2Question2(
            "关于房间和生活状态你……",
            a = "东西各归其位，计划本写得满满当当。", aCode = "J",
            b = "随手放，想起来再收拾，主打随缘。", bCode = "P"
        ),
        fun2Question2(
            "面对恋爱中的临时变动你……",
            a = "一切照计划来，别打乱我的节奏。", aCode = "J",
            b = "随时调整，计划赶不上变化也挺好。", bCode = "P"
        )
    )

    // ============ 恋爱说明书（A/B/C/D 映射 犬/猫/狐/鹿） ============
    val LOVEMANUAL_QUESTIONS: List<Fun2Question> = listOf(
        fun2Question(
            "恋爱中你的状态更接近……",
            a = "时刻想黏着对方，恨不得天天见面。", aCode = "犬",
            b = "需要大量独处时间，想你了自然会来。", bCode = "猫",
            c = "喜欢若即若离，保持一点神秘感。", cCode = "狐",
            d = "温柔安静，安安静静陪在对方身边。", dCode = "鹿"
        ),
        fun2Question(
            "对方迟迟不回消息你……",
            a = "连发三条「在吗」「去哪了」「理理我」。", aCode = "犬",
            b = "无所谓，该回的时候自然会回。", bCode = "猫",
            c = "故意也不回，让对方猜猜我在干嘛。", cCode = "狐",
            d = "有点失落，但选择安安静静等。", dCode = "鹿"
        ),
        fun2Question(
            "你的心动方式更接近……",
            a = "明明白白示爱，全世界都知道我喜欢你。", aCode = "犬",
            b = "暗戳戳关注对方，等 TA 先开口。", bCode = "猫",
            c = "制造偶遇和巧合，等 TA 主动上钩。", cCode = "狐",
            d = "脸红心跳，话到嘴边又咽回去。", dCode = "鹿"
        ),
        fun2Question(
            "吵架之后你通常会……",
            a = "不到半小时就憋不住，跑去哄对方。", aCode = "犬",
            b = "先冷静几天，情绪过去了再谈。", bCode = "猫",
            c = "不吵不闹，用别的话题把这事绕过去。", cCode = "狐",
            d = "心里难过，但嘴上说不出重话。", dCode = "鹿"
        ),
        fun2Question(
            "朋友眼里的你恋爱时……",
            a = "眼里只有对象，重色轻友第一名。", aCode = "犬",
            b = "清高独立，让人有点捉摸不透。", bCode = "猫",
            c = "暧昧高手，走到哪都有故事。", cCode = "狐",
            d = "温温柔柔，让人忍不住想保护。", dCode = "鹿"
        ),
        fun2Question(
            "你给对方的安全感主要来自……",
            a = "随叫随到，有求必应。", aCode = "犬",
            b = "长期稳定的陪伴，不爱说但一直在做。", bCode = "猫",
            c = "会说话，每句话都说到对方心坎里。", cCode = "狐",
            d = "从不伤害对方，永远柔软真诚。", dCode = "鹿"
        ),
        fun2Question(
            "你最喜欢的情侣相处模式是……",
            a = "一起腻着，吃饭睡觉都要在一起。", aCode = "犬",
            b = "各自忙各自的，忙完再碰头。", bCode = "猫",
            c = "互相试探推拉，永远有新鲜感。", cCode = "狐",
            d = "平平淡淡细水长流，一起看日落。", dCode = "鹿"
        ),
        fun2Question(
            "收到对方礼物时你……",
            a = "当场欢呼，恨不得原地转圈。", aCode = "犬",
            b = "表面淡定，心里已经甜翻了。", bCode = "猫",
            c = "笑着收下，顺便夸得对方心花怒放。", cCode = "狐",
            d = "小声说谢谢，然后悄悄珍藏起来。", dCode = "鹿"
        ),
        fun2Question(
            "恋爱中你最容易吃醋的对象是……",
            a = "对方的任何朋友，谁靠近 TA 我都不乐意。", aCode = "犬",
            b = "对方的前任，偶尔会想起但不说。", bCode = "猫",
            c = "我自己，我要做 TA 永远猜不透的人。", cCode = "狐",
            d = "没有人，我信任对方也信任这段感情。", dCode = "鹿"
        ),
        fun2Question(
            "你眼中的「最好爱情」是……",
            a = "我为你赴汤蹈火，你眼里只有我。", aCode = "犬",
            b = "你懂我的独立，我懂你的需要。", bCode = "猫",
            c = "永远推拉，永远心动，永远新鲜。", cCode = "狐",
            d = "清晨一起醒来，黄昏一起散步。", dCode = "鹿"
        )
    )

    // ============ 子测试元信息 ============
    val SUBTESTS: List<Fun2SubtestInfo> = listOf(
        Fun2SubtestInfo("Phil", "菲尔人格", "10 题 · 4 种类型", "经典菲尔人格测试，按总分分档评估你的性格底色与处事基调。"),
        Fun2SubtestInfo("CowHorse", "牛马浓度", "10 题 · 4 种浓度", "以职场状态视角，趣味评估你的工作节奏与压力应对倾向。"),
        Fun2SubtestInfo("Love16", "恋爱16型", "12 题 · 16 种类型", "从四个维度分析你在亲密关系中的相处模式与情感倾向。"),
        Fun2SubtestInfo("LoveManual", "恋爱说明书", "10 题 · 4 种动物系", "通过意象对照，呈现你在亲密关系中的互动方式与需求表达。")
    )

    // ============ 类型固定顺序 ============
    private val LOVE16_DIMS = listOf("E" to "I", "S" to "N", "T" to "F", "J" to "P")
    private val LOVEMANUAL_ORDER = listOf("犬", "猫", "狐", "鹿")

    /** 恋爱16型四个维度的固定顺序（并列时取每维先出现者：E、S、T、J） */
    fun love16Dims(): List<Pair<String, String>> = LOVE16_DIMS

    /** 恋爱说明书动物类型的固定顺序（并列时取先出现者：犬、猫、狐、鹿） */
    fun loveManualOrder(): List<String> = LOVEMANUAL_ORDER

    // ============ 类型名 ============
    private val PHIL_NAMES = mapOf(
        "FLeader" to "霸气领袖",
        "FBalanced" to "平衡稳健",
        "FGentle" to "温和随性",
        "FUnique" to "特立独行"
    )

    private val COWHORSE_NAMES = mapOf(
        "PureHorse" to "纯血牛马",
        "HalfHorse" to "半血牛马",
        "HumanLike" to "人模狗样",
        "Capitalist" to "资本家自由人"
    )

    private val LOVE16_NAMES = mapOf(
        "ISTJ" to "ISTJ 恋爱型 · 稳重可靠的守护者",
        "ISFJ" to "ISFJ 恋爱型 · 细心体贴的暖窝",
        "INFJ" to "INFJ 恋爱型 · 温柔深邃的灵魂",
        "INTJ" to "INTJ 恋爱型 · 理性笃定的规划师",
        "ISTP" to "ISTP 恋爱型 · 低调神秘的工匠",
        "ISFP" to "ISFP 恋爱型 · 浪漫随性的艺术家",
        "INFP" to "INFP 恋爱型 · 天真深情的诗人",
        "INTP" to "INTP 恋爱型 · 有趣好奇的学者",
        "ESTP" to "ESTP 恋爱型 · 热烈直接的冒险家",
        "ESFP" to "ESFP 恋爱型 · 活力四射的开心果",
        "ENFP" to "ENFP 恋爱型 · 热情洋溢的太阳",
        "ENTP" to "ENTP 恋爱型 · 机灵爱闹的辩手",
        "ESTJ" to "ESTJ 恋爱型 · 务实可靠的大管家",
        "ESFJ" to "ESFJ 恋爱型 · 热心周全的暖风机",
        "ENFJ" to "ENFJ 恋爱型 · 温暖有力的灯塔",
        "ENTJ" to "ENTJ 恋爱型 · 掌控全局的王者"
    )

    private val LOVEMANUAL_NAMES = mapOf(
        "犬" to "犬系恋人",
        "猫" to "猫系恋人",
        "狐" to "狐系恋人",
        "鹿" to "鹿系恋人"
    )

    // ============ 解读库 ============
    private val PHIL_INTERP = mapOf(
        "FLeader" to "类型名：霸气领袖\n特质：你气场全开、目标感极强，习惯掌控局面，是人群里天然的主心骨，说话做事自带一种「这事我来定」的笃定。\n优势：决策快、执行力强、敢担责，别人还在犹豫你已拍板，天生适合扛大旗。\n提醒：强势之外记得留一点温柔，偶尔也听听别人怎么说，领袖的魅力不止于发号施令。",
        "FBalanced" to "类型名：平衡稳健\n特质：你不卑不亢、张弛有度，既能带头冲锋，也能安静倾听，是那种「大事扛得住、小事拎得清」的人。\n优势：分寸感极佳，在人群中既不抢眼也不掉队，是团队里最让人安心的存在。\n提醒：别总想着兼顾所有人，偶尔也可以任性一把，按自己的节奏来。",
        "FGentle" to "类型名：温和随性\n特质：你随和包容、不争不抢，讲究顺其自然，习惯把舒服和快乐放在第一位，是身边人的「情绪松弛剂」。\n优势：好相处、零压力、有亲和力，跟你在一起从来不需要端着，简单自在。\n提醒：随性不等于将就，想要的东西偶尔也要主动争取一下，温和也有力量。",
        "FUnique" to "类型名：特立独行\n特质：你不按常理出牌，自带一套独立于世界之外的运行逻辑，很少被外界标准绑架，活得清醒又自由。\n优势：思维独特、不易被裹挟，能坚持做自己，是那种「任他潮起潮落，我自岿然不动」的人。\n提醒：特立独行很酷，也别忘了和世界保持一点点连接，独处之外也需要同行者。"
    )

    private val COWHORSE_INTERP = mapOf(
        "PureHorse" to "类型名：纯血牛马\n浓度诊断：你的牛马浓度拉满，是工位上最可靠的引擎，领导放心、同事依赖，连你手里的方案都在替公司扛 KPI。\n灵魂建议：学会对不合理的需求说不，身体是革命的本钱，牛马也要按时吃饭睡觉。\n金句：牛马可以低头吃草，但别忘了抬头看看天。",
        "HalfHorse" to "类型名：半血牛马\n浓度诊断：你一半是牛马，一半还留着当人的尊严，会加班但不卷命，会抱怨但也会干活，在躺与卷之间反复横跳。\n灵魂建议：把省下的力气投资自己，技能才是你摆脱牛马命运的唯一车票。\n金句：半血的牛马最清醒，知道草要抢，也知道命是自己的。",
        "HumanLike" to "类型名：人模狗样\n浓度诊断：你牛马浓度偏低，活得像个人样——到点下班、拒绝内耗、工作是工作，生活是生活，边界感比工资条还清晰。\n灵魂建议：继续保持，别被内卷的气氛带偏，你已经跑赢了大多数人。\n金句：工作是老板的，身体是自己的，这场交易你只出八小时。",
        "Capitalist" to "类型名：资本家自由人\n浓度诊断：你的牛马浓度趋近于零，活得像是来公司「考察行情」的——班照上，但想让你无条件奉献，门都没有。\n灵魂建议：把这份通透贯彻到底，早做规划、拥抱变化，你的人生方向盘始终在自己手里。\n金句：做不了资本家，也要做时间的主人，打工而已，别入戏太深。"
    )

    private val LOVE16_INTERP = mapOf(
        "ISTJ" to "类型定位：稳重可靠的守护者\n恋爱风格：不爱花哨的浪漫，但记得你所有生活细节，用行动把安全感拉满，说到做到从不画饼。\n甜蜜提醒：别只做事不说爱，偶尔把心里的在乎讲出来，对方会更幸福。",
        "ISFJ" to "类型定位：细心体贴的暖窝\n恋爱风格：默默记住你的一切偏好，把温柔藏在一粥一饭里，是对方身后最踏实的港湾。\n甜蜜提醒：温柔有余也要学会表达需求，你的感受同样值得被照顾。",
        "INFJ" to "类型定位：温柔深邃的灵魂\n恋爱风格：爱得深沉而克制，渴望灵魂共鸣，愿意为一段值得的感情付出惊人的耐心和忠诚。\n甜蜜提醒：别把期待都藏进心里，及时沟通，对方才跟得上你的心。",
        "INTJ" to "类型定位：理性笃定的规划师\n恋爱风格：爱一个人会认真规划未来，用靠谱的行动证明承诺，不轻易动心但动了心就非常认真。\n甜蜜提醒：爱情不是项目计划，偶尔的浪漫和情感表达能让关系更暖。",
        "ISTP" to "类型定位：低调神秘的工匠\n恋爱风格：话不多但手很巧，擅长用行动解决问题，喜欢自由也需要空间，爱得冷静而实在。\n甜蜜提醒：适当的甜言蜜语不是虚伪，让对方知道你的在意会更安心。",
        "ISFP" to "类型定位：浪漫随性的艺术家\n恋爱风格：用细腻的观察和温柔的关怀表达爱意，擅长制造小而美的浪漫，不喜欢被约束。\n甜蜜提醒：随性之余记得回应对方的期待，双向奔赴的感情才长久。",
        "INFP" to "类型定位：天真深情的诗人\n恋爱风格：把爱情想象得无比美好，一旦认定便深情投入，愿意为对方付出全部真心。\n甜蜜提醒：理想的爱情也需要落地，接受对方的不完美，感情会更稳固。",
        "INTP" to "类型定位：有趣好奇的学者\n恋爱风格：用奇思妙想和知识储备让对方眼前一亮，爱得理性克制，但专注起来会特别认真。\n甜蜜提醒：理性之外多给对方一点情绪反馈，感情不是辩论赛。",
        "ESTP" to "类型定位：热烈直接的冒险家\n恋爱风格：敢爱敢追，行动力满格，约会永远新鲜刺激，喜欢谁就直接表达，从不拖泥带水。\n甜蜜提醒：热情来得快去得也快，学会沉淀，让对方感受到长久的诚意。",
        "ESFP" to "类型定位：活力四射的开心果\n恋爱风格：把恋爱过成一场热闹的派对，甜话不断、惊喜不断，和 TA 在一起永远不无聊。\n甜蜜提醒：热闹之外也要学会倾听和陪伴，感情需要深度的连接。",
        "ENFP" to "类型定位：热情洋溢的太阳\n恋爱风格：爱得坦诚又热烈，满脑子都是对方，愿意为喜欢的人奔赴山海，真诚又治愈。\n甜蜜提醒：热情上头时也留一点清醒，爱别人的同时别忘了爱自己。",
        "ENTP" to "类型定位：机灵爱闹的辩手\n恋爱风格：脑洞大开、嘴甜又会哄，喜欢和对方斗嘴打趣，把平淡日子过得火花四溅。\n甜蜜提醒：逗趣归逗趣，关键时刻要能收起玩笑，认真回应对方的情感需求。",
        "ESTJ" to "类型定位：务实可靠的大管家\n恋爱风格：用行动承包对方的生活，靠谱、守时、有担当，是那种「跟我在一起就不用操心」的恋人。\n甜蜜提醒：安排一切的时候，也问问对方的想法，别把照顾变成控制。",
        "ESFJ" to "类型定位：热心周全的暖风机\n恋爱风格：把恋人照顾得无微不至，记得每个纪念日，也记得对方随口提的小愿望，付出型选手。\n甜蜜提醒：付出值得被看见，也要学会接受对方的爱，别把自己累着。",
        "ENFJ" to "类型定位：温暖有力的灯塔\n恋爱风格：洞察力一流，总能读懂对方没说出口的心思，用温暖和鼓励把对方推向更好的自己。\n甜蜜提醒：别总当照顾别人情绪的那个人，你的软肋也需要一个港湾。",
        "ENTJ" to "类型定位：掌控全局的王者\n恋爱风格：目标感极强，爱了就认真经营，把这段感情当成最重要的项目，护短又专一。\n甜蜜提醒：感情里没有绝对的正确方案，偶尔放下强势，听听对方的心声。"
    )

    private val LOVEMANUAL_INTERP = mapOf(
        "犬" to "说明书名称：犬系恋人\ndesc：忠诚热烈的头号粉丝，爱了就掏心掏肺，恨不得把整个世界的糖都搬到对方面前。\ntraits：黏人、忠诚、热情外放、随叫随到，安全感给得明明白白，吃醋也吃得光明正大。\nlove_style：热烈直接，爱意写在脸上，也写在每一次秒回和每一场风雨无阻的奔赴里。\nsuitable：适合愿意回应热烈、也享受被依赖的恋人，双方都能接住这份滚烫。\nwarning：别爱到失去自己，过度依附会让你疲惫，也会让对方喘不过气。\nslogan：别人都说我像只狗，只有你知道，我的尾巴只对你摇。",
        "猫" to "说明书名称：猫系恋人\ndesc：高冷独立的神秘艺术家，爱有分寸，静水流深，不黏人但心里一直有对方。\ntraits：独立、慢热、口是心非、需要空间，表面云淡风轻，心里记着对方的所有小事。\nlove_style：爱得很安静，不常挂在嘴边，但会在细节里悄悄流露，想要了会自己蹭过来。\nsuitable：适合尊重边界、给足空间、也能读懂潜台词的恋人，慢慢融化这座冰山。\nwarning：太傲娇容易让对方误会，偶尔直白一点，爱要说出来才有效。\nslogan：我懒得撒娇，但我的尾巴尖一直在朝你。",
        "狐" to "说明书名称：狐系恋人\ndesc：聪明迷人的氛围高手，懂人心、会来事，把恋爱的分寸和新鲜感拿捏得恰到好处。\ntraits：机敏、会说话、神秘感十足，情商在线，总能让对方既安心又忍不住好奇。\nlove_style：若即若离的张弛艺术，既有热烈的告白，也有恰到好处的距离，永远让人心动。\nsuitable：适合同样有趣、享受推拉、内心强大的恋人，太直白的人可能跟不上你的节奏。\nwarning：套路用多了会变成隔阂，偶尔卸下聪明，用真心换真心才长久。\nslogan：我是猎人，也是猎物，但这场游戏里，我只想被你驯服。",
        "鹿" to "说明书名称：鹿系恋人\ndesc：温柔纯净的治愈系，眼神干净、心地柔软，像山间小鹿一样让人忍不住想要保护。\ntraits：温柔、善良、慢热、重感情，不擅长甜言蜜语，但真心实意，爱得很纯粹。\nlove_style：细水长流的陪伴，不争不抢，用笨拙却真诚的方式把对方放在心上。\nsuitable：适合温柔有耐心、愿意慢慢来、重视真诚的恋人，太急的人会吓到小鹿。\nwarning：善良要带点锋芒，太容易退让会委屈自己，学会说出自己的感受。\nslogan：世界纷纷扰扰，我只想在你身边，安安静静地过完这一生。"
    )

    private fun interpretationOf(subTest: String, code: String): String = when (subTest) {
        "Phil" -> PHIL_INTERP[code] ?: ""
        "CowHorse" -> COWHORSE_INTERP[code] ?: ""
        "Love16" -> LOVE16_INTERP[code] ?: ""
        else -> LOVEMANUAL_INTERP[code] ?: ""
    }

    fun questionsOf(subTest: String): List<Fun2Question> = when (subTest) {
        "Phil" -> PHIL_QUESTIONS
        "CowHorse" -> COWHORSE_QUESTIONS
        "Love16" -> LOVE16_QUESTIONS
        else -> LOVEMANUAL_QUESTIONS
    }

    /** 计分入口：answers 为每题所选字母（A/B/C/D），按子测试各自规则确定性计分 */
    fun calculate(subTest: String, answers: List<String>): Fun2Result = when (subTest) {
        "Phil" -> philResult(answers)
        "CowHorse" -> cowHorseResult(answers)
        "Love16" -> love16Result(answers)
        else -> loveManualResult(answers)
    }

    /** 菲尔人格：A/B/C/D 各记 1/2/3/4 分，累计总分按档位定类型 */
    private fun philResult(answers: List<String>): Fun2Result {
        var total = 0
        PHIL_QUESTIONS.forEachIndexed { i, q ->
            val letter = answers.getOrNull(i) ?: return@forEachIndexed
            total += q.weights[letter]?.toIntOrNull() ?: 0
        }
        val code = when {
            total in 30..40 -> "FLeader"
            total in 20..29 -> "FBalanced"
            total in 10..19 -> "FGentle"
            else -> "FUnique"
        }
        return Fun2Result(
            subTest = "Phil",
            scores = mapOf("总分" to total),
            code = code,
            name = PHIL_NAMES[code] ?: code,
            interpretation = interpretationOf("Phil", code)
        )
    }

    /** 牛马浓度：A/B/C/D 各记 4/3/2/1 分，累计总分按档位定浓度 */
    private fun cowHorseResult(answers: List<String>): Fun2Result {
        var total = 0
        COWHORSE_QUESTIONS.forEachIndexed { i, q ->
            val letter = answers.getOrNull(i) ?: return@forEachIndexed
            total += q.weights[letter]?.toIntOrNull() ?: 0
        }
        val code = when {
            total in 32..40 -> "PureHorse"
            total in 24..31 -> "HalfHorse"
            total in 16..23 -> "HumanLike"
            else -> "Capitalist"
        }
        return Fun2Result(
            subTest = "CowHorse",
            scores = mapOf("总分" to total),
            code = code,
            name = COWHORSE_NAMES[code] ?: code,
            interpretation = interpretationOf("CowHorse", code)
        )
    }

    /** 恋爱16型：每个维度计 A/B 两字母票数，取高者拼成四字母组合（并列取先出现者） */
    private fun love16Result(answers: List<String>): Fun2Result {
        val scores = mutableMapOf(
            "E" to 0, "I" to 0, "S" to 0, "N" to 0,
            "T" to 0, "F" to 0, "J" to 0, "P" to 0
        )
        LOVE16_QUESTIONS.forEachIndexed { i, q ->
            val letter = answers.getOrNull(i) ?: return@forEachIndexed
            val code = q.weights[letter] ?: return@forEachIndexed
            scores[code] = (scores[code] ?: 0) + 1
        }
        val code = LOVE16_DIMS.joinToString("") { (a, b) ->
            if ((scores[a] ?: 0) >= (scores[b] ?: 0)) a else b
        }
        return Fun2Result(
            subTest = "Love16",
            scores = scores.toMap(),
            code = code,
            name = LOVE16_NAMES[code] ?: code,
            interpretation = interpretationOf("Love16", code)
        )
    }

    /** 恋爱说明书：累计四种动物票数，取最高者（并列按 犬、猫、狐、鹿 顺序取先出现者） */
    private fun loveManualResult(answers: List<String>): Fun2Result {
        val scores = mutableMapOf<String, Int>()
        LOVEMANUAL_ORDER.forEach { scores[it] = 0 }
        LOVEMANUAL_QUESTIONS.forEachIndexed { i, q ->
            val letter = answers.getOrNull(i) ?: return@forEachIndexed
            val code = q.weights[letter] ?: return@forEachIndexed
            scores[code] = (scores[code] ?: 0) + 1
        }
        var best = LOVEMANUAL_ORDER.first()
        LOVEMANUAL_ORDER.drop(1).forEach { animal ->
            if ((scores[animal] ?: 0) > (scores[best] ?: 0)) best = animal
        }
        return Fun2Result(
            subTest = "LoveManual",
            scores = scores.toMap(),
            code = best,
            name = LOVEMANUAL_NAMES[best] ?: best,
            interpretation = interpretationOf("LoveManual", best)
        )
    }
}
