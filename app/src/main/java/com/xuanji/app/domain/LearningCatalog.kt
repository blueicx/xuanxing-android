package com.xuanji.app.domain

data class LearningEntry(
    val id: String, val title: String, val category: String, val body: String,
    val source: String, val scope: String, val aliases: List<String> = emptyList(), val article: Boolean = false
)

/** Original explanations of this application's calculation and reading conventions. */
object LearningCatalog {
    private val rows = """
        eastern|pillars|四柱|出生年、月、日、时分别形成一柱，每柱由天干与地支组合。
        eastern|stem|天干|甲乙丙丁戊己庚辛壬癸十个符号，在本项目中映射阴阳和五行。
        eastern|branch|地支|子丑寅卯辰巳午未申酉戌亥十二个符号，用于纪时与干支组合。
        eastern|day-master|日主|日柱的天干，是项目五行关系说明的参照点。
        eastern|element|五行|木火土金水是文化分类标签；项目用它们统计命盘和策展生活灵感。
        eastern|generation|相生|木生火、火生土、土生金、金生水、水生木的循环关系。
        eastern|control|相克|木克土、土克水、水克火、火克金、金克木的循环关系。
        eastern|solar-term|节气|东方月柱依赖节气边界；项目采用离线近似并说明计算精度。
        eastern|lunar|农历|项目用1900–2100年表转换公历日期，超范围不猜测。
        eastern|leap-month|闰月|农历中额外加入的月份；转换结果显式区分普通月与闰月。
        eastern|favorable|喜用五行|项目规则给出的五行参考，用于离线匹配，不是医疗或现实结果保证。
        eastern|dayun|大运|按传统时间分段组织的命理解读；顺逆等规则依赖明确输入。
        western|zodiac|黄道星座|项目将黄道划分为十二个30度区段，以星体位置确定所在区段。
        western|sun-sign|太阳星座|太阳在出生时黄道上的星座区段；项目读取出生盘的太阳位置。
        western|moon-sign|月亮星座|月亮所在星座区段；依赖出生时刻与项目采用的天文近似。
        western|ascendant|上升点|东方地平线与黄道的交点；出生时间与地点会影响其计算。
        western|house|宫位|用于组织星盘空间的分区；不同宫制的定义和结果可能不同。
        western|aspect|相位|两个星体黄经之间的角度关系，须结合采用的容许度阅读。
        western|conjunction|合相|角距接近0度的关系，是否成立取决于项目相位容许度。
        western|opposition|对冲|角距接近180度的关系；标签本身不等于现实事件预测。
        western|trine|三分相|角距接近120度的关系，属于星盘几何关系说明。
        western|square|四分相|角距接近90度的关系；解读是文化说明，不是必然结论。
        western|longitude|黄经|沿黄道测量的角坐标，项目使用0至360度的循环值。
        western|timezone|时区|将当地出生时刻换算为统一时刻所需的信息，不能忽略日期跨越。
        divination|tarot|塔罗|项目支持牌阵与抽牌解读，抽取方式与牌面事实分开展示。
        divination|spread|牌阵|卡牌位置组成的解释结构；不同位置承担不同提问视角。
        divination|rune|卢恩|项目用符文目录开展文化解读；不宣称替代历史仪式。
        divination|numerology|数字命理|按明确的数字化简步骤计算个人数字，并展示周期计算过程。
        divination|hexagram|六十四卦|由六条阴阳爻组成的64种结构，卦名与卦辞覆盖程度须分开看。
        divination|line|爻|卦中每一条阴阳线；六爻的位置与变化参与卦结构表达。
        divination|moving-line|动爻|占卦方法中标记发生变化的爻；不能用普通静爻替代其含义。
        divination|oracle|签文|项目签库中的文本与等级，用作文化阅读；来源标签说明语料范围。
        divination|odu|Odu|Ifá组合目录包含256个稳定索引；组合索引不等于Ese经文。
        divination|ese|Ese经文|须由有授权和来源的语料提供；项目默认没有真实经文正文。
        divination|nadi|纳迪叶文|真实文本必须携带来源与授权；项目默认提供明确标记的模拟内容。
        divination|corpus|语料来源|说明文本由谁提供、是否授权、是否示例，是判断解读边界的重要信息。
        tests|raw-score|原始分|按题目与计分规则得到的分数，尚未与正式常模人群比较。
        tests|norm|常模|来自规定人群的数据参照；缺少常模时不能包装成标准化百分位。
        tests|percentile|百分位|相对指定参照样本的位置；不是答对题目的百分比。
        tests|reverse-score|反向计分|将反向题按规则转换，再与同维度题目汇总。
        tests|dimension|测验维度|题目按明确结构归属的测量方面，不能从一次聊天直接推断。
        tests|ipip|IPIP|公开人格题库体系；项目所用量表题目与计分口径需明确标注。
        tests|big-five|大五人格|开放性、尽责性、外向性、宜人性和情绪稳定相关维度的自我报告框架。
        tests|ria-sec|职业兴趣|用户主动测验的兴趣结果，可作为探索工作任务的参考输入。
        tests|reliability|信度|测量结果一致性的统计概念；本项目娱乐题库不默认具备正式信度。
        tests|validity|效度|对量表用途是否得到证据支持的判断，不能只由题目数量推出。
        tests|assessment|自我报告|回答来自用户当时的选择，可能随情境变化，不等于人格诊断。
        tests|screening|筛查与诊断|筛查结果和专业诊断不同；本项目不提供临床诊断结论。
        app|weight|匹配权重|每日行动固定采用40%五行、25%星座、20%盘面、15%季节或城市标签。
        app|seed|确定性种子|由档案、日期和候选标识构成的稳定输入，仅用于同分排序。
        app|filter|偏好过滤|先移除不满足用户明确条件的候选，再计算匹配次序。
        app|evidence|依据|展示实际输入、计算规则和各项贡献，用于复核当前结果。
        app|snapshot|结果快照|用户主动保存的原始正文、依据和版本，回看时保持当时内容。
        app|agenda|今日安排|用户主动加入的事项、时间和备注，与运势分数分开保存。
        app|shopping|购物清单|合并已加入安排的菜品食材，并保留每项食材对应的菜名。
        app|memory|主动记忆|只有用户明确输入和确认的资料才可保存，不虚构长期经历。
        app|offline|离线计算|核心计算在本机进行；可选网络能力须由用户主动开启。
        app|backup|加密备份|密码派生密钥后认证加密；导入先校验再预览，密码不保存。
        app|matching|匹配分|文化标签与当前输入的规则分数，不是现实成功概率或职业能力分。
        app|source-label|来源标签|区分计算结果、用户输入、文化参考和未启用能力，帮助读者理解边界。
    """.trimIndent()

    val terms: List<LearningEntry> = rows.lines().map { line ->
        val f = line.trim().split('|')
        LearningEntry("term-${f[1]}", f[2], f[0], f[3], "玄星自编术语与离线规则说明 v1",
            "用于理解本应用；传统简化与未启用内容以结果页标签为准。",
            aliases = when (f[1]) { "lunar" -> listOf("阴历", "农历日期"); "weight" -> listOf("为什么", "评分依据"); "spread" -> listOf("塔罗牌阵"); else -> emptyList() })
    }
    private val topics = listOf(
        "五行与今日行动" to "先从已保存命盘取得喜用五行，再与候选的文化标签比较。五行只是文化策展输入，不承担营养或治疗判断。",
        "每日行动如何计算" to "候选先通过偏好、预算、时间和场景过滤。四项适配按40、25、20、15的百分比加权；总分相同才使用稳定排序。",
        "如何理解星座输入" to "今日行动读取出生盘太阳星座元素。完整出生盘和只看生日的太阳星座不同，缺少信息会限制可解释的范围。",
        "出生时刻与地点" to "时区、日期跨越与地点相关坐标会影响时刻相关计算。资料完整度清单只说明哪些字段保存，不自动提高算法精度。",
        "农历与闰月" to "项目采用年表转换公历日期，普通月与闰月分别表示。范围外显示不支持，旧公历近似入口需结合页面说明阅读。",
        "今日与一生命盘" to "今日、周、月、年采用各自周期结果。一生命盘解释出生时的输入结构，今日详情不会转成一生命盘结果。",
        "贡献与过滤的区别" to "依据中匹配贡献加总为候选实际分数。偏好过滤是准入条件，不重复加分；城市和季节共用环境权重，不能各算一遍。",
        "测验原始分与常模" to "题目计分可复算，但正式常模需要特定样本和授权。没有常模时阅读原始分和维度，不能自行转换为诊断或人群百分位。",
        "职业兴趣如何使用" to "职业簇用于尝试具体任务：先了解典型活动和所需技能，再用小型练习判断兴趣。兴趣标签不等于招聘或能力结论。",
        "塔罗抽牌与牌阵" to "牌阵指定位置含义，抽牌方式决定本次卡牌。确定性模式可按同一输入复现；文化解读不能证明未来事件必然发生。",
        "卢恩与数字命理" to "卢恩阅读采用已标明的符文目录；数字命理展示数值化简和周期步骤。两者来源、输入和解释口径分别记录。",
        "Ifá与纳迪的资料边界" to "256个Odu索引不是Ese正文。真实经文和纳迪叶文必须由可靠授权语料提供；未启用与模拟标签不能省略。",
        "收藏与快照回看" to "主动收藏方便按类型查找；保存结果快照保留正文、依据、日期和算法版本。回看旧结果无需用新算法覆盖。",
        "安排与购物清单" to "把建议加入安排后可设置时间、备注、完成或撤销。购物清单按食材合并，保留来源菜品；提醒受系统调度影响。",
        "四角色与共享资料" to "四位角色共享出生资料与盘面事实，各有独立线程。主动交接传递当前问题摘要，角色语气不会覆盖算法结果。",
        "游戏与现实解释" to "象棋按棋盘规则判定；诗句、星图和丝路是离线游戏。游戏存档只描述游戏进度，不参与运势或人格诊断。"
    )
    val articles: List<LearningEntry> = topics.mapIndexed { index, (title, body) ->
        LearningEntry("article-${index + 1}", title, "app", body, "玄星自编算法与使用说明 v1",
            "本项目实现说明；文化解释与正式资料范围请同时查看结果页来源。", article = true)
    }
    fun find(id: String): LearningEntry? = (terms + articles).firstOrNull { it.id == id }
    fun related(text: String): List<LearningEntry> = terms.filter { term -> (listOf(term.title) + term.aliases).any(text::contains) }.take(6)
}

enum class SearchCategory { All, Feature, Term, Article, Favorite }
data class LocalSearchResult(val id: String, val title: String, val category: SearchCategory,
    val preview: String, val route: String? = null, val entryId: String? = null, val favoriteId: String? = null)

object LocalSearchIndex {
    private val features = listOf(
        Triple("今日运势", "today", "综合 东方 西方 今日行动 吃什么 做什么 去哪玩"),
        Triple("一生命盘", "charts", "排盘 人生画像 职业 色彩 地区"),
        Triple("塔罗牌阵", "divination/tarot", "塔罗 抽牌 单张 三张 五张"),
        Triple("北欧卢恩", "divination/rune", "符文 卢恩"),
        Triple("数字命理", "divination/numerology", "生命数字 个人年"),
        Triple("紫微斗数", "divination/ziwei", "紫微 农历 闰月"),
        Triple("安排与记录", "history", "日历 安排 购物清单 收藏 快照"),
        Triple("个人设置", "profile", "出生资料 偏好 备份 隐私")
    )
    fun search(query: String, favorites: List<ReadingFavorite>, category: SearchCategory = SearchCategory.All): List<LocalSearchResult> {
        val needle = query.trim().take(100).lowercase()
        val entries = features.map { (title, route, aliases) -> LocalSearchResult("feature-$route", title, SearchCategory.Feature, aliases, route) } +
            (LearningCatalog.terms + LearningCatalog.articles).map { entry -> LocalSearchResult(entry.id, entry.title,
                if (entry.article) SearchCategory.Article else SearchCategory.Term, entry.body + " " + entry.aliases.joinToString(" "), entryId = entry.id) } +
            favorites.map { LocalSearchResult("favorite-${it.id}", it.title, SearchCategory.Favorite, it.body, favoriteId = it.id) }
        return entries.filter { (category == SearchCategory.All || it.category == category) &&
            (needle.isBlank() || "${it.title} ${it.preview}".lowercase().contains(needle)) }
            .sortedWith(compareByDescending<LocalSearchResult> { it.title.lowercase() == needle }.thenByDescending { it.title.lowercase().contains(needle) }.thenBy { it.id })
            .take(100)
    }
}
