package com.xuanji.app.domain.test

/**
 * 趣味测试大合集 Pack 3
 * 6 个子测试:动物人格、美食水果人格、影视动漫角色、颜色心理、FBTI 美食 MBTI、瑞文智力挑战。
 * 前四个为「逐题选型」:每题 A/B/C/D 映射到类型,计分后取最高分类型(并列按固定顺序取先出现者)。
 * FBTI 为「维度计分」:A/B 映射到 E/I、S/N、T/F、J/P 四维度,各维度取高分拼成四字母代码。
 * 瑞文为「对题计分」:每题带正确答案,按对题数分档评定。
 * 全离线、确定性计分,不使用 random,结果仅供娱乐参考。
 */

data class Fun3Question(
    val text: String,
    val options: List<Pair<String, String>>,  // 字母 A/B/C/D(或 A-E)到选项文本
    val weights: Map<String, String>          // 字母到类型/维度代码;瑞文用「correct」键存正确答案
)

data class Fun3Result(
    val subTest: String,
    val scores: Map<String, Int>,
    val code: String,
    val name: String,
    val interpretation: String
)

data class Fun3SubtestInfo(
    val id: String,
    val title: String,
    val badge: String,
    val desc: String
)

object FunTests3 {

    /** 四选项题:文本 + 四组(选项文本, 类型代码),保证 options 与 weights 一一对应 */
    private fun fun3Question4(
        text: String,
        a: String, aCode: String,
        b: String, bCode: String,
        c: String, cCode: String,
        d: String, dCode: String
    ): Fun3Question = Fun3Question(
        text = text,
        options = listOf("A" to a, "B" to b, "C" to c, "D" to d),
        weights = mapOf("A" to aCode, "B" to bCode, "C" to cCode, "D" to dCode)
    )

    /** 二选项题(FBTI):A/B 映射到维度代码 */
    private fun fun3Question2(
        text: String,
        a: String, aCode: String,
        b: String, bCode: String
    ): Fun3Question = Fun3Question(
        text = text,
        options = listOf("A" to a, "B" to b),
        weights = mapOf("A" to aCode, "B" to bCode)
    )

    /** 五选项推理题(瑞文):正确答案存 weights 的「correct」键 */
    private fun fun3Raven(
        text: String,
        correct: String,
        a: String, b: String, c: String, d: String, e: String
    ): Fun3Question = Fun3Question(
        text = text,
        options = listOf("A" to a, "B" to b, "C" to c, "D" to d, "E" to e),
        weights = mapOf("correct" to correct)
    )

    // ============ 1. 动物人格(7 种动物) ============
    val ANIMAL_QUESTIONS: List<Fun3Question> = listOf(
        fun3Question4(
            "在人群里你更希望自己是……",
            a = "发号施令、镇住全场的那个。", aCode = "狮子",
            b = "说走就走、带队冲锋的那个。", bCode = "狼",
            c = "安静观察、看穿一切的那个。", cCode = "猫头鹰",
            d = "带动气氛、人见人爱的那个。", dCode = "海豚"
        ),
        fun3Question4(
            "面对难题你通常……",
            a = "绕个弯子,换个思路巧解。", aCode = "狐狸",
            b = "不急不躁,稳扎稳打硬扛。", bCode = "熊",
            c = "寸步不离,陪到最后。", cCode = "狗",
            d = "正面硬刚,谁的场子都不虚。", dCode = "狮子"
        ),
        fun3Question4(
            "朋友眼中的你更像……",
            a = "高冷神秘,深不可测。", aCode = "猫头鹰",
            b = "话痨担当,永远热场。", bCode = "海豚",
            c = "憨厚可靠,安全感拉满。", cCode = "熊",
            d = "行动力强,说干就干。", dCode = "狼"
        ),
        fun3Question4(
            "你做事的方式是……",
            a = "见招拆招,灵活变通。", aCode = "狐狸",
            b = "直来直往,单刀直入。", bCode = "狮子",
            c = "按部就班,踏实推进。", cCode = "狗",
            d = "谋定后动,先观察再出手。", dCode = "猫头鹰"
        ),
        fun3Question4(
            "深夜的你打开手机……",
            a = "越夜越清醒,享受独自思考。", aCode = "狼",
            b = "还在和朋友东聊西聊。", bCode = "海豚",
            c = "早就睡了,养生第一。", cCode = "熊",
            d = "偷偷盘算明天的计划。", dCode = "狐狸"
        ),
        fun3Question4(
            "你更向往的生活是……",
            a = "安稳温暖,有人陪伴有人依靠。", aCode = "狗",
            b = "成就霸业,站上自己的王座。", bCode = "狮子",
            c = "自由自在,回归山野自然。", cCode = "熊",
            d = "宁静专注,在自己热爱里深耕。", dCode = "猫头鹰"
        )
    )

    private val ANIMAL_ORDER = listOf("狮子", "狼", "猫头鹰", "海豚", "狐狸", "熊", "狗")

    private val ANIMAL_NAMES = mapOf(
        "狮子" to "狮子 · 王者型", "狼" to "狼 · 协作型", "猫头鹰" to "猫头鹰 · 智者型",
        "海豚" to "海豚 · 乐天型", "狐狸" to "狐狸 · 机敏型", "熊" to "熊 · 稳健型", "狗" to "狗 · 忠诚型"
    )

    private val ANIMAL_INTERP = mapOf(
        "狮子" to "描述:你体内住着一头草原之王,天生自带气场,走到哪里都像在巡视自己的领地。\n特质:自信、果断、有领导力,敢于承担,渴望被看见和尊重。\n优势:关键时刻能镇场,敢拍板、敢负责,是天然的主心骨。\n弱点:有时过于强势,听不进反对意见,面子比天大。\n职业:管理、创业、演讲、竞技类岗位,凡是需要站上 C 位的都适合你。\nslogan:我即王座,无需加冕。",
        "狼" to "描述:你是荒野里的独行者,更是团队里的灵魂猎手,懂得协作,也享受独处。\n特质:坚韧、有纪律、行动力强,认定目标便死磕到底。\n优势:执行力与耐力兼备,既能在团队中冲锋,也能独自扛住压力。\n弱点:容易把自己绷得太紧,偶尔显得固执甚至孤傲。\n职业:军事、工程、竞技、野外探索类,越是硬仗越能激发你的狼性。\nslogan:目标既定,虽远必达。",
        "猫头鹰" to "描述:你是夜色中的智者,习惯在安静处观察世界,想清楚了才出手。\n特质:冷静、睿智、专注,洞察力一流,不爱无效社交。\n优势:看问题直击本质,极少冲动决策,是团队的军师型人才。\n弱点:话少容易让人觉得高冷,过度思虑有时会错失时机。\n职业:研究、分析、写作、战略规划类,安静的思考环境最能成就你。\nslogan:先看清棋局,再落下棋子。",
        "海豚" to "描述:你是海面上最欢快的那道弧线,自带社交天赋,是人群里的开心果。\n特质:乐观、聪明、热心肠,情绪来得快去得也快,人缘极好。\n优势:亲和力与应变力兼备,善于化解尴尬,让气氛永远在线。\n弱点:有时三分钟热度,过分在意他人评价,容易累到自己。\n职业:公关、市场、教育、服务类,凡是需要与人打交道的领域都是你的主场。\nslogan:开心会传染,我负责当传染源。",
        "狐狸" to "描述:你是森林里最灵动的谋略家,遇到难题从不硬碰,总能绕出第三条路。\n特质:机敏、圆融、点子多,嗅觉敏锐,天生懂得审时度势。\n优势:应变力超强,谈判与斡旋是你的拿手好戏,几乎不会被难倒。\n弱点:想得太多容易让人看不透,有时机灵过头反而少了点真诚。\n职业:销售、公关、策划、创业类,越是复杂多变的局面越能发挥你的聪明。\nslogan:正面过不去,那就换个角度过去。",
        "熊" to "描述:你是山林里的定海神针,平时慢悠悠,关键时一出手就是雷霆万钧。\n特质:沉稳、宽厚、可靠,不爱张扬,但一诺千金。\n优势:耐性与爆发力兼备,能扛大事,是大家最愿意托付后背的人。\n弱点:启动慢热,固执起来九头牛都拉不回,容易错过风口。\n职业:技术、制造、医疗、体能类,需要深耕与厚积的岗位为你而生。\nslogan:慢一点没关系,稳才能赢。",
        "狗" to "描述:你是人间最温柔的守护者,忠诚刻在骨子里,认定了就绝不放手。\n特质:真诚、可靠、重感情,感知力强,永远把在乎的人放在心上。\n优势:信赖度满分,是朋友情绪的避风港,也是团队里最稳的后方。\n弱点:容易过度付出,习惯性把别人的需求排在前面,委屈了自己。\n职业:医护、教育、公益、服务类,用真心换真心的事业最适合你。\nslogan:你在,我就在。"
    )

    // ============ 2. 美食水果人格(15 种美食) ============
    val FOOD_QUESTIONS: List<Fun3Question> = listOf(
        fun3Question4(
            "别人对你的第一印象是……",
            a = "气场十足,像一道硬菜压轴登场。", aCode = "牛排",
            b = "精致讲究,细节经得起推敲。", bCode = "寿司",
            c = "热情直爽,自带分享属性。", cCode = "披萨",
            d = "爱恨分明,喜欢的人超喜欢。", dCode = "榴莲"
        ),
        fun3Question4(
            "你在团队里的角色是……",
            a = "稳重大众,谁跟你合作都舒服。", aCode = "苹果",
            b = "大方解渴,缺你气氛就差了。", bCode = "西瓜",
            c = "犀利清醒,一句话点醒全桌。", cCode = "柠檬",
            d = "甜度超标,生日气氛你负责。", dCode = "蛋糕"
        ),
        fun3Question4(
            "心情不好的时候你会……",
            a = "拉人吃顿火锅,边吃边吐槽。", aCode = "火锅",
            b = "点杯奶茶,甜一点就满血了。", bCode = "奶茶",
            c = "打开一包薯片,咔滋咔滋。", cCode = "薯片",
            d = "买颗糖,让甜味冲淡烦恼。", dCode = "糖果"
        ),
        fun3Question4(
            "你的工作风格接近……",
            a = "丝滑高效,苦中带甜不掉线。", aCode = "巧克力",
            b = "冷静细致,条理清晰不慌张。", bCode = "抹茶",
            c = "慢工细活,越打磨越出味。", cCode = "炖汤",
            d = "火力全开,直击目标不废话。", dCode = "牛排"
        ),
        fun3Question4(
            "朋友临时约你出门,你……",
            a = "好啊,顺便整杯奶茶。", aCode = "奶茶",
            b = "宅家吧,零食和剧都准备好了。", bCode = "薯片",
            c = "都行,你们定,我随意。", cCode = "苹果",
            d = "挑家精致点的,好好吃一顿。", dCode = "寿司"
        ),
        fun3Question4(
            "你的隐藏属性是……",
            a = "外表带刺,内里其实很软糯。", aCode = "榴莲",
            b = "日常吐槽,酸里带点幽默。", bCode = "柠檬",
            c = "越热闹越开心,人多就亢奋。", cCode = "火锅",
            d = "兼容万物,和谁都处得来。", dCode = "披萨"
        )
    )

    private val FOOD_ORDER = listOf("牛排", "寿司", "披萨", "榴莲", "苹果", "西瓜", "柠檬", "蛋糕", "火锅", "奶茶", "薯片", "糖果", "巧克力", "抹茶", "炖汤")

    private val FOOD_NAMES = mapOf(
        "牛排" to "牛排 · 王者派", "寿司" to "寿司 · 精致派", "披萨" to "披萨 · 分享派", "榴莲" to "榴莲 · 反差派",
        "苹果" to "苹果 · 稳当派", "西瓜" to "西瓜 · 大方派", "柠檬" to "柠檬 · 清醒派", "蛋糕" to "蛋糕 · 甜蜜派",
        "火锅" to "火锅 · 热辣派", "奶茶" to "奶茶 · 续命派", "薯片" to "薯片 · 解压派", "糖果" to "糖果 · 直接派",
        "巧克力" to "巧克力 · 层次派", "抹茶" to "抹茶 · 清冷派", "炖汤" to "炖汤 · 滋补派"
    )

    private val FOOD_INTERP = mapOf(
        "牛排" to "描述:你是铁板上的主角,一出场就是全场焦点,讲究品质与火候,从不将就。\n特质:自信、直接、目标感强,认准的事就全力以赴,追求有分量的人生。\n建议:偶尔接受七分熟之外的可能,人生不必每次都煎到全熟才算成功。",
        "寿司" to "描述:你是精雕细琢的匠人型人格,追求细节与仪式感,小事也做到极致。\n特质:克制、讲究、有品位,分寸感极好,安静却让人挪不开眼。\n建议:别让完美主义累坏自己,有些粗糙和随意,反而是生活的鲜味。",
        "披萨" to "描述:你是什么都兼容的分享型人格,热情直爽,快乐大方,身边的快乐都想分你一半。\n特质:慷慨、随和、社交能力强,朋友多到数不清,天生乐天派。\n建议:分得出去快乐,也要学会留一片给自己,别总当最后擦盘子的人。",
        "榴莲" to "描述:你是极具争议的反差人格,第一面让人犹豫,熟悉了让人上瘾。\n特质:爱憎分明、个性强烈,绝不迎合,懂你的人会爱死你。\n建议:不必强求所有人都喜欢你,找到同频的人,就是最大的幸福。",
        "苹果" to "描述:你是最日常却也最难得的稳当派,像每天一苹果,低调但谁都离不开。\n特质:健康、务实、性格温和,不争不抢,却是人群里的定心丸。\n建议:别把自己活成背景板,你其实自带光芒,偶尔也要站到台前。",
        "西瓜" to "描述:你是盛夏的快乐源泉,大方爽快,见面就给人降温,自带解暑体质。\n特质:实在、大方、重情义,心里有什么都写在脸上,相处零负担。\n建议:大方是美德,但记得给信任设边界,真心要留给值得的人。",
        "柠檬" to "描述:你是清醒犀利的吐槽担当,酸只是表象,其实句句都在点醒别人。\n特质:敏锐、直率、有锋芒,看问题一针见血,关键时刻从不掉链子。\n建议:酸可以点缀,但别让它盖过甜,偶尔也要允许自己软一点。",
        "蛋糕" to "描述:你是全场最甜的社交担当,生日愿望都许给你,气氛靠你撑起。\n特质:热情、细腻、注重仪式感,最会照顾别人的情绪与面子。\n建议:甜是天赋,也要记得补充自己的糖分,别把能量全给出去。",
        "火锅" to "描述:你是越煮越热闹的团魂型人格,一顿火锅就是一场小型社交。\n特质:热烈、豪爽、重感情,喜欢分享,讨厌冷场,朋友遍布各地。\n建议:热闹是解药,独处也是,偶尔安静一下,锅底也需要休息。",
        "奶茶" to "描述:你是现代人的精神续命剂,甜中带一丝苦,明明知道会上瘾还是戒不掉。\n特质:温柔、细腻、恋旧,习惯用一点甜来对冲生活的苦。\n建议:少糖去冰不是妥协,是更高级的自爱,你值得无糖的快乐。",
        "薯片" to "描述:你是解压界的顶流,咔滋一声,烦恼散一半,快乐来得干脆。\n特质:随性、幽默、心态好,小事不计较,天塌下来先吃口零食。\n建议:快乐很简单,但也要记得按时吃饭,别让零食当了正餐。",
        "糖果" to "描述:你是最直接的正能量选手,情绪从不拐弯,甜就甜得明明白白。\n特质:单纯、乐观、真诚,笑容有感染力,是朋友群里的治愈担当。\n建议:世界有时很苦,你的甜很珍贵,也别忘了保护好这份天真。",
        "巧克力" to "描述:你是层次丰富的宝藏人格,先苦后甜,越品越有味道。\n特质:有深度、有韧性,表面冷静,内里藏着滚烫的热情。\n建议:别急着剥开自己的包装,懂你的人,自然愿意陪你慢慢融化。",
        "抹茶" to "描述:你是清冷高级的治愈系人格,不喧哗,却自带让人安心的气场。\n特质:细腻、冷静、自律,情绪稳定,是朋友圈里的定海神针。\n建议:清冷之外,偶尔也要释放一下烟火气,苦味回甘才更珍贵。",
        "炖汤" to "描述:你是小火慢熬的养生型人格,不急着出彩,但越相处越有滋味。\n特质:耐心、包容、有后劲,对朋友是长情,对事是从容。\n建议:慢工出细活是你的天赋,但也别错过趁热喝汤的当下。"
    )

    // ============ 3. 影视动漫角色(6 位角色) ============
    val CHARACTER_QUESTIONS: List<Fun3Question> = listOf(
        fun3Question4(
            "面对强加于你的命运,你会……",
            a = "反抗到底,命运由我自己写。", aCode = "哈利·波特",
            b = "用天才的头脑,直接拆解问题。", bCode = "钢铁侠",
            c = "笑着冲上去,管它是什么来头。", cCode = "路飞",
            d = "先立个誓,然后一步步变强。", dCode = "索隆"
        ),
        fun3Question4(
            "你在团队里更像……",
            a = "永不放弃的热血核心。", aCode = "鸣人",
            b = "暗中布局的战术大师。", bCode = "蝙蝠侠",
            c = "沉默靠谱的武力担当。", cCode = "索隆",
            d = "关键时刻的救场者。", dCode = "哈利·波特"
        ),
        fun3Question4(
            "面对强大的对手,你会……",
            a = "喊上伙伴一起上,怕什么。", aCode = "钢铁侠",
            b = "笑着说一句「有意思」。", bCode = "路飞",
            c = "绝不逃,相信伙伴也相信自己。", cCode = "鸣人",
            d = "先做一百个预案,再逐一击破。", dCode = "蝙蝠侠"
        ),
        fun3Question4(
            "你的行事风格是……",
            a = "跟着直觉走,关键时灵光一现。", aCode = "哈利·波特",
            b = "升级装备,科技就是底气。", bCode = "钢铁侠",
            c = "说到做到,一刀解决不拖泥带水。", cCode = "索隆",
            d = "直来直去,声音比谁都大。", dCode = "鸣人"
        ),
        fun3Question4(
            "你最大的武器是……",
            a = "天才的头脑与钢铁的战甲。", aCode = "钢铁侠",
            b = "无可救药的乐观与信念。", bCode = "路飞",
            c = "严苛的纪律与对恐惧的掌控。", cCode = "蝙蝠侠",
            d = "说到做到、言出必行的承诺。", dCode = "索隆"
        ),
        fun3Question4(
            "深夜的你在做什么……",
            a = "和伙伴吃拉面撸串,大聊特聊。", aCode = "路飞",
            b = "高冷地站在天台俯瞰城市。", bCode = "蝙蝠侠",
            c = "等着猫头鹰送来远方的消息。", cCode = "哈利·波特",
            d = "一个人练到力竭,然后睡到天亮。", dCode = "鸣人"
        )
    )

    private val CHARACTER_ORDER = listOf("哈利·波特", "钢铁侠", "路飞", "索隆", "鸣人", "蝙蝠侠")

    private val CHARACTER_NAMES = mapOf(
        "哈利·波特" to "哈利·波特 · 救世主型", "钢铁侠" to "钢铁侠 · 天才型", "路飞" to "路飞 · 船长型",
        "索隆" to "索隆 · 剑豪型", "鸣人" to "鸣人 · 火影型", "蝙蝠侠" to "蝙蝠侠 · 黑暗骑士型"
    )

    private val CHARACTER_INTERP = mapOf(
        "哈利·波特" to "出自:《哈利·波特》系列\n描述:你心里住着一位天选之子,面对命运的安排从不认命,相信爱能战胜一切。\n特质:勇敢、重感情、有正义感,关键时候总能站出来,是朋友眼里靠谱的救场者。\nslogan:选择,比天赋更重要。",
        "钢铁侠" to "出自:漫威电影宇宙\n描述:你是个用头脑改变世界的天才,天塌下来不是扛,而是直接造一套战甲。\n特质:聪明、毒舌、有担当,嘴上漫不经心,心里装着整个宇宙的责任。\nslogan:我可以造出未来。",
        "路飞" to "出自:《海贼王》\n描述:你是天生的船长,为梦想可以放弃一切,脸上永远挂着没心没肺的笑。\n特质:乐观、义气、意志力惊人,认定的人与目标,刀山火海也拦不住你。\nslogan:我是要成为海贼王的男人。",
        "索隆" to "出自:《海贼王》\n描述:你是说到做到的剑士,受伤从不多言,看似路痴,方向感却强得离谱。\n特质:坚韧、自律、重承诺,一条路走到黑,是团队最可靠的武力担当。\nslogan:头断也不能言而无信。",
        "鸣人" to "出自:《火影忍者》\n描述:你是永不言弃的孤儿逆袭者,坚信努力可以超越天才,把孤独化作守护的力量。\n特质:热血、执着、嘴炮满级,最擅长把敌人说成朋友,把朋友处成家人。\nslogan:有话直说,这就是我的忍道。",
        "蝙蝠侠" to "出自:DC 漫画\n描述:你是黑暗中的义警,白天是亿万富翁,夜晚是让罪犯胆寒的蝙蝠。\n特质:冷静、自律、谋略家,把所有恐惧化作纪律,永远做好万全准备。\nslogan:我不是英雄,我只是别无选择。"
    )

    // ============ 4. 颜色心理(12 种颜色) ============
    val COLORPSYCH_QUESTIONS: List<Fun3Question> = listOf(
        fun3Question4(
            "你的性格底色更像……",
            a = "热情似火,藏不住。", aCode = "红",
            b = "沉静如水,稳得住。", bCode = "蓝",
            c = "生机盎然,慢节奏。", cCode = "绿",
            d = "明亮张扬,自带光芒。", dCode = "黄"
        ),
        fun3Question4(
            "压力爆表的时候,你会……",
            a = "躲进艺术与想象里自我疗愈。", aCode = "紫",
            b = "独自消化,谁也别来打扰。", bCode = "黑",
            c = "清空思绪,一切归零重启。", cCode = "白",
            d = "找朋友撒娇,求一个抱抱。", dCode = "粉"
        ),
        fun3Question4(
            "你在团队里通常是……",
            a = "元气担当,负责调动气氛。", aCode = "橙",
            b = "低调幕后,负责兜底收尾。", bCode = "灰",
            c = "耀眼主角,人群里的焦点。", cCode = "金",
            d = "百变多面,随时切换角色。", dCode = "彩"
        ),
        fun3Question4(
            "你的审美偏好更接近……",
            a = "高级简约,克制留白。", aCode = "蓝",
            b = "自然清新,贴近生活。", bCode = "绿",
            c = "明亮活泼,色彩跳跃。", cCode = "黄",
            d = "神秘优雅,带点距离感。", dCode = "紫"
        ),
        fun3Question4(
            "遇到新鲜事物,你……",
            a = "第一个冲上去尝鲜。", aCode = "红",
            b = "先远观,保持安全距离。", bCode = "黑",
            c = "太可爱了,必须拥有。", cCode = "粉",
            d = "拉上朋友,一起玩才香。", dCode = "橙"
        ),
        fun3Question4(
            "你理想的生活状态是……",
            a = "干净纯粹,简简单单。", aCode = "白",
            b = "精致闪耀,品质至上。", bCode = "金",
            c = "平静安稳,岁月静好。", cCode = "灰",
            d = "精彩纷呈,每天不同。", dCode = "彩"
        )
    )

    private val COLORPSYCH_ORDER = listOf("红", "蓝", "绿", "黄", "紫", "黑", "白", "粉", "橙", "灰", "金", "彩")

    private val COLORPSYCH_NAMES = mapOf(
        "红" to "红 · 热情型", "蓝" to "蓝 · 理性型", "绿" to "绿 · 平和型", "黄" to "黄 · 明亮型",
        "紫" to "紫 · 神秘型", "黑" to "黑 · 深邃型", "白" to "白 · 纯粹型", "粉" to "粉 · 温柔型",
        "橙" to "橙 · 活力型", "灰" to "灰 · 低调型", "金" to "金 · 耀眼型", "彩" to "彩 · 百变型"
    )

    private val COLORPSYCH_INTERP = mapOf(
        "红" to "描述:红色是你的心理底色,热烈、直接、充满生命力,你是人群中最容易点燃气氛的那个。\n特质:行动派、情绪外放、敢爱敢恨,喜欢就冲,讨厌磨叽。\n建议:情绪上头时先深呼吸,热情是燃料,别让它烧了方向盘。",
        "蓝" to "描述:蓝色是冷静的深海,你理性克制,遇事第一反应是分析而不是冲动。\n特质:沉稳、严谨、值得信赖,话不多但每句都在点上。\n建议:理性是你的铠甲,偶尔也要卸下来,让情感透透气。",
        "绿" to "描述:绿色是安静的森林,你温和包容,是身边人疲惫时的避风港。\n特质:平和、有耐心、与世无争,习惯照顾别人,不喜欢冲突。\n建议:你的需求同样值得被认真对待,学会说「我想要」。",
        "黄" to "描述:黄色是阳光的颜色,你乐观明亮,自带小太阳体质,走到哪里都亮堂堂。\n特质:开朗、自信、爱分享,心态好,挫折在你眼里都是段子。\n建议:阳光也别忘了充电,偶尔允许自己阴天,不需要时刻发光。",
        "紫" to "描述:紫色是神秘的高贵,你品味独特,内心世界丰富得像一片星云。\n特质:有想象力、追求精神契合,小众而坚定,不喜欢随大流。\n建议:神秘感是你的魅力,但也别把真心藏太深,懂你的人值得看见全貌。",
        "黑" to "描述:黑色是深邃的夜,你独立强大,习惯把情绪收进夜色里,一个人消化。\n特质:自律、有边界感、抗压能力强,看似疏离,实则内心坚定。\n建议:强大不是不示弱,偶尔让信任的人陪你走进光里。",
        "白" to "描述:白色是纯净的留白,你追求简单与秩序,把复杂的生活过成清爽的样子。\n特质:干净、真诚、有原则,讨厌算计,喜欢透明的关系。\n建议:纯粹的坚持很珍贵,但世界本就多彩,偶尔允许一点「杂色」。",
        "粉" to "描述:粉色是柔软的少女心,你温柔细腻,对世界抱有天真的善意。\n特质:可爱、有同理心、爱浪漫,是朋友圈里的小甜心,治愈力满分。\n建议:柔软不是软弱,温柔要有,但也要长出保护自己的刺。",
        "橙" to "描述:橙色是饱满的活力,你元气满满,总能把平淡日子过出火花。\n特质:热情、积极、感染力强,像一颗行走的维生素,随时给人充电。\n建议:元气是天赋,记得按时补充睡眠,别把电量耗到告急。",
        "灰" to "描述:灰色是高级的中立,你低调内敛,不爱出风头,却总在关键处可靠。\n特质:沉稳、务实、观察力强,边界感清晰,情绪管理一流。\n建议:低调不等于隐身,你的实力值得被更多人看见。",
        "金" to "描述:金色是闪耀的品质,你追求精致与高光,对自己和生活都有要求。\n特质:有野心、有审美、目标感强,不做则已,做就要做到耀眼。\n建议:光芒是努力换来的,但也别忘了欣赏沿途平凡的风景。",
        "彩" to "描述:彩色是无限的可能,你拒绝被定义,今天和明天可以是两个人。\n特质:多面、灵动、好奇心爆棚,适应力极强,永远对世界保持新鲜感。\n建议:百变是你的自由,但记得留一个真实的底色,那是最珍贵的你。"
    )

    // ============ 5. FBTI 美食 MBTI(8 题 · 16 种人格) ============
    val FBTI_QUESTIONS: List<Fun3Question> = listOf(
        fun3Question2(
            "周末满血复活的方式是……",
            a = "约朋友出门嗨,人越多越精神。", aCode = "E",
            b = "宅家独处回血,一个人最舒服。", bCode = "I"
        ),
        fun3Question2(
            "聚会里你通常是……",
            a = "全场话最多的那个,自带主持。", aCode = "E",
            b = "安静待在角落,暗中观察大家。", bCode = "I"
        ),
        fun3Question2(
            "点菜的时候你……",
            a = "认准吃过的好评招牌,稳稳的。", aCode = "S",
            b = "专点没试过的新品,追求惊喜。", bCode = "N"
        ),
        fun3Question2(
            "出门旅行你更喜欢……",
            a = "攻略精确到小时,行程满满。", aCode = "S",
            b = "走到哪算哪,随性才是真旅行。", bCode = "N"
        ),
        fun3Question2(
            "朋友向你吐槽时,你……",
            a = "先帮 TA 分析问题出在哪,给方案。", aCode = "T",
            b = "先共情:「你受委屈了」,给安慰。", bCode = "F"
        ),
        fun3Question2(
            "挑餐厅你更看重……",
            a = "评分口碑,性价比第一。", aCode = "T",
            b = "氛围感觉,合眼缘最重要。", bCode = "F"
        ),
        fun3Question2(
            "下周的下午茶聚会……",
            a = "提前一周定好店、选好位置。", aCode = "J",
            b = "到时候再说,走到哪吃到哪。", bCode = "P"
        ),
        fun3Question2(
            "点奶茶的时候你……",
            a = "固定配方万年不变,从不折腾。", aCode = "J",
            b = "每次都要试试新的组合。", bCode = "P"
        )
    )

    private val FBTI_ORDER = listOf("E", "I", "S", "N", "T", "F", "J", "P")

    private val FBTI_LETTER_NAMES = mapOf(
        "E" to "外向 E", "I" to "内向 I", "S" to "实感 S", "N" to "直觉 N",
        "T" to "理性 T", "F" to "感性 F", "J" to "计划 J", "P" to "随性 P"
    )

    private val FBTI_NAMES = mapOf(
        "ESTJ" to "火锅人格", "ESTP" to "麻辣烫人格", "ESFJ" to "蛋糕人格", "ESFP" to "奶茶人格",
        "ENTJ" to "牛排人格", "ENTP" to "芥末人格", "ENFJ" to "寿司人格", "ENFP" to "水果拼盘人格",
        "ISTJ" to "炖汤人格", "ISTP" to "烧烤人格", "ISFJ" to "家常菜人格", "ISFP" to "冰淇淋人格",
        "INTJ" to "黑咖人格", "INTP" to "泡面人格", "INFJ" to "抹茶人格", "INFP" to "巧克力人格"
    )

    private val FBTI_INTERP = mapOf(
        "ESTJ" to "描述:你是热气腾腾的火锅,天生的组织者,只要你在场,锅子就不会冷。\n特质:果断、务实、行动力强,规矩清楚,说到做到,讨厌没效率。\n建议:效率之外,偶尔也感受一下慢火慢炖的温柔,别把气氛煮过头。",
        "ESTP" to "描述:你是自由选择的麻辣烫,什么都敢夹,什么都敢试,活得洒脱又刺激。\n特质:胆大、反应快、享受当下,是朋友里的行动派,说走就走。\n建议:体验世界很爽,也记得把滚烫的冲动晾一晾,别烫着嘴。",
        "ESFJ" to "描述:你是全场最甜的蛋糕,把每个人的情绪都放在心上,天生操心的体质。\n特质:热情、体贴、责任感爆棚,擅长照顾人,是团队的暖心担当。\n建议:把甜分给全世界,也要记得给自己留一块,你值得被照顾。",
        "ESFP" to "描述:你是快乐续命的奶茶,甜是标配,和谁都能聊成老朋友。\n特质:开朗、爱分享、情绪外放,活在当下,是聚会永不冷场的保证。\n建议:甜而不腻是境界,偶尔换换口味,世界还有很多惊喜。",
        "ENTJ" to "描述:你是大火煎制的牛排,目标感强,出手就是主角,从不做配角。\n特质:自信、果断、有战略眼光,天生领导气场,敢想敢要。\n建议:王座之下,偶尔也听听小火慢炖的建议,你会赢得更多人心。",
        "ENTP" to "描述:你是呛口上头的芥末,思维跳跃,专挑别人想不到的角度,一针见血。\n特质:聪明、幽默、爱抬杠,是辩论场上永远不服输的那个。\n建议:犀利是天赋,偶尔把芥末调淡一点,别人的真心也值得入口。",
        "ENFJ" to "描述:你是精致用心的寿司,洞察人心,总能恰到好处地照顾到每个人。\n特质:温暖、有感染力、善于激励,是天生的精神领袖,人缘极好。\n建议:关心别人是本能,也别忘了留个位子给自己,别总当配菜。",
        "ENFP" to "描述:你是色彩缤纷的水果拼盘,对世界充满好奇,每个遇见都能聊出火花。\n特质:热情、创意无限、共情力强,是朋友里的梦想家与气氛担当。\n建议:热情是宝藏,但注意别一次切太多块,专注会让梦更甜。",
        "ISTJ" to "描述:你是慢火细熬的炖汤,不着急出彩,却把每一件事都做到扎实。\n特质:稳重、守时、重承诺,讨厌变数,是团队里最让人安心的存在。\n建议:你的稳定是稀世品质,偶尔加一点新食材,汤会更有层次。",
        "ISTP" to "描述:你是自己动手的烧烤,擅长解决问题,不爱空谈,说干就干。\n特质:冷静、手巧、独立思考,压力越大越稳,是行动派里的技术流。\n建议:独自忙碌之余,也加入热闹的餐桌,分享会让人生更有味。",
        "ISFJ" to "描述:你是最温暖的家常菜,不花哨,却让每个人记住家的味道。\n特质:细心、体贴、忠诚,习惯默默付出,把在乎的人照顾得妥妥当当。\n建议:你的付出都被看在眼里,偶尔也允许自己点一次外卖。",
        "ISFP" to "描述:你是甜甜柔柔的冰淇淋,热爱美好事物,温柔又有自己的想法。\n特质:随和、有审美、共情力强,慢热但熟了之后甜到融化。\n建议:你的甜很美,但记得别在高温里硬撑,适时给自己降温。",
        "INTJ" to "描述:你是深不见底的黑咖啡,独立清醒,用脑子构建整个世界的模型。\n特质:理性、自律、目标明确,不爱废话,效率与深度是信仰。\n建议:偶尔加一点奶和糖,生活不是所有事都需要最优化。",
        "INTP" to "描述:你是三分钟脑洞风暴的泡面,看起来简单,内核却藏着无数奇思妙想。\n特质:逻辑强、好奇心爆棚、脑内剧场永不落幕,讨厌被琐事打扰。\n建议:泡面好吃,也偶尔走出书房,现实世界也有值得研究的话题。",
        "INFJ" to "描述:你是清苦回甘的抹茶,温柔而深邃,总在安静处看见别人看不见的风景。\n特质:洞察力强、有理想主义、极其真诚,是灵魂共鸣的追求者。\n建议:你的深度很珍贵,别怕别人不懂,总有人愿意慢慢品你。",
        "INFP" to "描述:你是先苦后甜的巧克力,内心柔软而丰富,藏着大片浪漫的星野。\n特质:理想主义、善良、想象力非凡,对热爱的事无比执着。\n建议:理想与现实都要,偶尔把美好说出来,让世界分享你的甜。"
    )

    // ============ 6. 瑞文智力挑战(10 题 · 5 档评定) ============
    val RAVEN_QUESTIONS: List<Fun3Question> = listOf(
        fun3Raven(
            "数列:2, 6, 18, 54, ? 下一个数应该是……",
            correct = "B",
            a = "108", b = "162", c = "160", d = "216", e = "64"
        ),
        fun3Raven(
            "数列:1, 4, 9, 16, 25, ? 下一个数应该是……",
            correct = "C",
            a = "30", b = "35", c = "36", d = "49", e = "26"
        ),
        fun3Raven(
            "字母:Z, X, V, T, R, ? 下一个字母应该是……",
            correct = "C",
            a = "Q", b = "S", c = "P", d = "N", e = "O"
        ),
        fun3Raven(
            "数列:100, 81, 64, 49, 36, ? 下一个数应该是……",
            correct = "A",
            a = "25", b = "24", c = "30", d = "20", e = "16"
        ),
        fun3Raven(
            "类比:医生:患者 = 老师:?",
            correct = "C",
            a = "校长", b = "同学", c = "学生", d = "家长", e = "教室"
        ),
        fun3Raven(
            "数列:1, 1, 2, 3, 5, 8, ? 下一个数应该是……",
            correct = "B",
            a = "11", b = "13", c = "12", d = "15", e = "10"
        ),
        fun3Raven(
            "数列:3, 5, 9, 17, 33, ? 下一个数应该是……",
            correct = "D",
            a = "49", b = "63", c = "55", d = "65", e = "66"
        ),
        fun3Raven(
            "数列:1, 8, 27, 64, ? 下一个数应该是……",
            correct = "E",
            a = "81", b = "100", c = "90", d = "108", e = "125"
        ),
        fun3Raven(
            "类比:鸟:天空 = 鱼:?",
            correct = "A",
            a = "水", b = "陆地", c = "巢", d = "网", e = "缸"
        ),
        fun3Raven(
            "数列:2, 5, 10, 17, 26, ? 下一个数应该是……",
            correct = "C",
            a = "35", b = "34", c = "37", d = "33", e = "40"
        )
    )

    private val RAVEN_SCORE_ORDER = listOf("正确", "错误")

    private val RAVEN_SCORE_NAMES = mapOf("正确" to "答对", "错误" to "答错")

    private val RAVEN_NAMES = mapOf(
        "Genius" to "天才", "Excellent" to "优秀", "Good" to "良好",
        "Average" to "中等", "Improve" to "待提升"
    )

    private val RAVEN_INTERP = mapOf(
        "Genius" to "评定:天才级\n描述:10 题几乎全对,你的抽象推理能力堪称降维打击,数列规律、类比关系在你眼里一目了然。\n特质:逻辑严密、反应极快、善于从表象抓本质。\n建议:推理能力强是天赋,多把它用在解决真实问题上,别浪费在猜谜上。",
        "Excellent" to "评定:优秀级\n描述:你的抽象推理能力相当出色,大多数规律都逃不过你的眼睛,偶尔失手也只是小疏忽。\n特质:思维敏捷、观察细致、逻辑在线。\n建议:基础很好,再挑战一些高难度推理题,你的思维会更锋利。",
        "Good" to "评定:良好级\n描述:你的逻辑推理处于良好水平,常规的数列与类比题难不倒你,复杂一点的规律需要再多想一层。\n特质:思路清晰、有耐心、基础扎实。\n建议:多练多总结,规律见得多,自然反应就快了。",
        "Average" to "评定:中等级\n描述:你的推理能力中等偏稳,简单的规律一眼能看穿,综合型的题目容易卡壳。\n特质:认真仔细、不轻易放弃,只是接触这类题偏少。\n建议:推理和肌肉一样可以训练,每天来几道找规律题,进步看得见。",
        "Improve" to "评定:待提升\n描述:这次的推理题对你有点挑战,别灰心,抽象思维本来就需要刻意练习。\n特质:有勇气尝试,敢于面对不擅长的领域,这本身就是加分项。\n建议:从简单的找规律开始,循序渐进,你会看到自己的成长曲线。"
    )

    // ============ 子测试元信息与类型定义 ============
    val SUBTESTS: List<Fun3SubtestInfo> = listOf(
        Fun3SubtestInfo("Animal", "动物人格测试", "6 题 · 7 种动物", "通过情境选择映射七种动物意象，反映你的行事风格与内在特质。"),
        Fun3SubtestInfo("Food", "美食水果人格", "6 题 · 15 种美食", "以美食偏好为引，关联你在社交、决策与自我认知上的倾向。"),
        Fun3SubtestInfo("Character", "影视动漫角色", "6 题 · 6 位角色", "通过角色原型对照，观察你的价值取向与行为模式。"),
        Fun3SubtestInfo("ColorPsych", "颜色心理测试", "6 题 · 12 种颜色", "基于颜色偏好投射心理倾向，辅助了解你的情绪与性格底色。"),
        Fun3SubtestInfo("FBTI", "FBTI 美食 MBTI", "8 题 · 16 种人格", "以美食意象类比 MBTI 四维度，评估你的性格维度偏好。"),
        Fun3SubtestInfo("Raven", "瑞文标准推理测验", "10 题 · 推理题", "经典非言语推理测验，通过数列与类比评估抽象逻辑思维能力。")
    )

    fun questionsOf(subTest: String): List<Fun3Question> = when (subTest) {
        "Animal" -> ANIMAL_QUESTIONS
        "Food" -> FOOD_QUESTIONS
        "Character" -> CHARACTER_QUESTIONS
        "ColorPsych" -> COLORPSYCH_QUESTIONS
        "FBTI" -> FBTI_QUESTIONS
        else -> RAVEN_QUESTIONS
    }

    /** 各子测试的类型固定顺序,用于并列时取先出现的类型 */
    fun typeOrder(subTest: String): List<String> = when (subTest) {
        "Animal" -> ANIMAL_ORDER
        "Food" -> FOOD_ORDER
        "Character" -> CHARACTER_ORDER
        "ColorPsych" -> COLORPSYCH_ORDER
        "FBTI" -> FBTI_ORDER
        else -> RAVEN_SCORE_ORDER
    }

    fun typeNames(subTest: String): Map<String, String> = when (subTest) {
        "Animal" -> ANIMAL_NAMES
        "Food" -> FOOD_NAMES
        "Character" -> CHARACTER_NAMES
        "ColorPsych" -> COLORPSYCH_NAMES
        "FBTI" -> FBTI_LETTER_NAMES
        else -> RAVEN_SCORE_NAMES
    }

    /** 逐题选型计分:每题字母映射类型 +1,取最高分类型(并列按固定顺序取先出现者) */
    private fun highestOf(
        subTest: String,
        questions: List<Fun3Question>,
        order: List<String>,
        names: Map<String, String>,
        interp: Map<String, String>,
        answers: List<String>
    ): Fun3Result {
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
        return Fun3Result(
            subTest = subTest,
            scores = scores.toMap(),
            code = best,
            name = names[best] ?: best,
            interpretation = interp[best] ?: ""
        )
    }

    /** FBTI:四维度各取高分字母(并列按 E/S/T/J 优先),拼成四字母代码 */
    private fun fbtiResult(answers: List<String>): Fun3Result {
        val dims = listOf("E" to "I", "S" to "N", "T" to "F", "J" to "P")
        val counts = mutableMapOf<String, Int>()
        FBTI_QUESTIONS.forEachIndexed { i, q ->
            val letter = answers.getOrNull(i) ?: return@forEachIndexed
            val code = q.weights[letter] ?: return@forEachIndexed
            counts[code] = counts.getOrDefault(code, 0) + 1
        }
        val code = buildString {
            for ((a, b) in dims) {
                val ca = counts[a] ?: 0
                val cb = counts[b] ?: 0
                append(if (ca >= cb) a else b)
            }
        }
        val name = FBTI_NAMES[code] ?: code
        return Fun3Result(
            subTest = "FBTI",
            scores = counts.toMap(),
            code = code,
            name = name,
            interpretation = FBTI_INTERP[code] ?: ""
        )
    }

    /** 瑞文:统计对题数,按 9-10 / 7-8 / 5-6 / 3-4 / 0-2 分档评定 */
    private fun ravenResult(answers: List<String>): Fun3Result {
        var correct = 0
        RAVEN_QUESTIONS.forEachIndexed { i, q ->
            val letter = answers.getOrNull(i) ?: return@forEachIndexed
            if (q.weights["correct"] == letter) correct++
        }
        val wrong = RAVEN_QUESTIONS.size - correct
        val band = when (correct) {
            9, 10 -> "Genius"
            7, 8 -> "Excellent"
            5, 6 -> "Good"
            3, 4 -> "Average"
            else -> "Improve"
        }
        return Fun3Result(
            subTest = "Raven",
            scores = mapOf("正确" to correct, "错误" to wrong),
            code = band,
            name = RAVEN_NAMES[band] ?: band,
            interpretation = RAVEN_INTERP[band] ?: ""
        )
    }

    fun calculate(subTest: String, answers: List<String>): Fun3Result = when (subTest) {
        "Animal" -> highestOf("Animal", ANIMAL_QUESTIONS, ANIMAL_ORDER, ANIMAL_NAMES, ANIMAL_INTERP, answers)
        "Food" -> highestOf("Food", FOOD_QUESTIONS, FOOD_ORDER, FOOD_NAMES, FOOD_INTERP, answers)
        "Character" -> highestOf("Character", CHARACTER_QUESTIONS, CHARACTER_ORDER, CHARACTER_NAMES, CHARACTER_INTERP, answers)
        "ColorPsych" -> highestOf("ColorPsych", COLORPSYCH_QUESTIONS, COLORPSYCH_ORDER, COLORPSYCH_NAMES, COLORPSYCH_INTERP, answers)
        "FBTI" -> fbtiResult(answers)
        else -> ravenResult(answers)
    }
}
