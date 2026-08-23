package com.xuanji.app.domain

import com.xuanji.app.data.model.BaziFull
import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.model.TestRecord
import kotlin.math.roundToInt

data class MysticGuide(
    val mode: String,
    val topicKey: String,
    val roleName: String,
    val styleKey: String,
    val styleName: String,
    val styleIntro: String,
    val signature: String,
    val arrival: String,
    val headline: String,
    val body: String,
    val evidence: List<String> = emptyList(),
    val followUps: List<MysticFollowUp> = emptyList()
)

data class MysticFollowUp(
    val key: String,
    val question: String,
    val answer: String
)

data class MysticInteractionOption(
    val label: String,
    val feedback: String
)

data class MysticInteraction(
    val title: String,
    val description: String,
    val options: List<MysticInteractionOption>
)

/**
 * 双面灵语：玄学家负责基于现有算法结果做心理按摩，半仙负责浮夸调侃。
 * 不使用随机数；同一个人、同一天、同一问题、同一模式必然得到同一回答。
 */
object MysticGuideGenerator {
    private val topics = linkedMapOf(
        "composite" to "综合",
        "career" to "事业",
        "love" to "感情",
        "wealth" to "财富",
        "study" to "学习",
        "health" to "健康",
        "test" to "测试"
    )

    fun topicLabels(): List<Pair<String, String>> = topics.map { it.key to it.value }

    /** 同一天、同一话题、同一分数稳定“随机”；但好运坏运不预设谁来接话。 */
    fun suggestedMode(topicKey: String, fortune: CompositeDailyFortune): String {
        val seed = presenceSeed(topicKey, fortune)
        return if (seed % 2L == 0L) "scholar" else "half"
    }

    /** 作风仍由稳定种子决定：同一天、同一话题、同一盘面遇到的是同一位“熟人”。 */
    private fun style(scholar: Boolean, topicKey: String, fortune: CompositeDailyFortune): Triple<String, String, String> {
        val index = ((presenceSeed(topicKey, fortune) / 7L) % 3L).toInt()
        return if (scholar) {
            when (index) {
                0 -> Triple("archive", "档案室学者", "翻页很轻 · 先核对再说话")
                1 -> Triple("harbor", "灯下倾听者", "先接情绪 · 再看盘面")
                else -> Triple("compass", "慢速罗盘", "给方向 · 不催你出发")
            }
        } else {
            when (index) {
                0 -> Triple("herald", "天庭司仪", "开场锣鼓 · 顺便阴阳")
                1 -> Triple("alley", "街口半仙", "嘴硬心软 · 人间气")
                else -> Triple("intern", "云端的实习生", "法术不稳 · 态度很好")
            }
        }
    }

    fun generate(
        mode: String,
        topicKey: String,
        bazi: BaziFull,
        fortune: CompositeDailyFortune,
        test: TestRecord? = null,
        divinationSummary: String? = null
    ): MysticGuide {
        val label = topics[topicKey] ?: "综合"
        val focus = if (topicKey == "test") {
            fortune.dimensions.maxByOrNull { it.score } ?: fortune.dimensions.first()
        } else {
            fortune.dimensions.firstOrNull { it.key == topicKey }
                ?: fortune.dimensions.firstOrNull { it.key == "emotion" && topicKey == "love" }
                ?: fortune.dimensions.first()
        }
        val eastScore = when (topicKey) {
            "career" -> fortune.eastern.careerScore
            "wealth" -> fortune.eastern.wealthScore
            "love", "test" -> fortune.eastern.loveScore
            "health" -> fortune.eastern.healthScore
            "study" -> ((fortune.eastern.careerScore + fortune.eastern.overallScore) / 2.0).roundToInt()
            else -> fortune.eastern.overallScore
        }
        val westScore = when (topicKey) {
            "career" -> fortune.western.careerScore
            "wealth" -> fortune.western.wealthScore
            "love", "test" -> fortune.western.loveScore
            "health" -> fortune.western.healthScore
            "study" -> ((fortune.western.careerScore + fortune.western.overallScore) / 2.0).roundToInt()
            else -> fortune.western.overallScore
        }
        val high = fortune.dimensions.maxByOrNull { it.score } ?: focus
        val low = fortune.dimensions.minByOrNull { it.score } ?: focus
        val facts = buildList {
            add(
                "东方盘：${bazi.chart.dayMaster.chinese}（${elementName(bazi.chart.dayMasterElement)}），" +
                    "${bazi.strength.level}；今日${fortune.eastern.dayPillarText}，喜${bazi.yongJi.useful.joinToString("、") { elementName(it) }}。"
            )
            add("西方盘：太阳${fortune.western.sign}，今日整体 ${fortune.western.overallScore} 分。")
            add("综合盘：${fortune.overallScore} 分；最强是${high.label} ${high.score}，最需照看是${low.label} ${low.score}。")
            add("今日开关：幸运数字 ${fortune.luckyNumber}，幸运色${fortune.luckyColor}，吉利方位${fortune.luckyDirection}。")
            if (!divinationSummary.isNullOrBlank()) add("占卜参照：${divinationSummary.trim()}")
            if (test != null) add("最近测试：${test.testName} → ${test.resultCode}（${test.resultName}）")
        }
        val scholar = mode != "half"
        val (styleKey, styleName, styleIntro) = style(scholar, topicKey, fortune)
        val arrival = arrivalLine(scholar, fortune.overallScore, presenceSeed(topicKey, fortune))
        val headline = if (scholar) {
            scholarHeadline(focus.score, label)
        } else {
            halfHeadline(focus.score, label)
        }
        val body = if (scholar) {
            "我把「${label}」放回完整命盘看：综合 ${focus.score} 分，东方 $eastScore 分，西方 $westScore 分。" +
                bandSentence(focus.score) +
                if (focus.score >= low.score && low.score < 55) {
                    "真正想被照顾的是「${low.label}」，今天给它一个十分钟的小承诺就够了。"
                } else {
                    "你不需要立刻变成另一个人，只要让已有的稳定继续发生。"
                }
        } else {
            "天界吐槽频道已锁定「${label}」：综合 ${focus.score} 分，东方 $eastScore 分，西方 $westScore 分！" +
                halfBandSentence(focus.score) +
                if (high.score >= 65) "「${high.label}」简直在冒仙气，别端着了，赶紧去接住这波排面！" else "连半仙都看不下去啦，先别硬冲，留点力气明天封神！"
        }

        val followUps = listOf(
            MysticFollowUp(
                key = "why",
                question = "这个数怎么来？",
                answer = whyAnswer(scholar, label, focus.score, eastScore, westScore)
            ),
            MysticFollowUp(
                key = "action",
                question = "现在怎么做？",
                answer = actionAnswer(scholar, high.label, low.label, fortune.luckyColor, fortune.luckyDirection)
            ),
            MysticFollowUp(
                key = "care",
                question = "要留意什么？",
                answer = careAnswer(scholar, low.label, fortune.cautions)
            ),
            MysticFollowUp(
                key = "focus",
                question = "${label}怎么破？",
                answer = topicAnswer(
                    scholar,
                    topicKey,
                    label,
                    focus.score,
                    high.label,
                    low.label,
                    test?.testName.orEmpty()
                )
            )
        )

        return MysticGuide(
            mode = mode,
            topicKey = topicKey,
            roleName = if (scholar) "玄学家" else "半仙",
            styleKey = styleKey,
            styleName = styleName,
            styleIntro = styleIntro,
            signature = if (scholar) "只讲盘面依据 · 仅供娱乐参考" else "浮夸但讲逻辑 · 仅供娱乐参考",
            arrival = arrival,
            headline = headline,
            body = body,
            evidence = facts,
            followUps = followUps
        )
    }

    /** 小互动也走稳定取样；“再来一局”通过 round 换到下一个可选局面。 */
    fun interaction(
        mode: String,
        topicKey: String,
        fortune: CompositeDailyFortune,
        round: Int
    ): MysticInteraction {
        val scholar = mode != "half"
        val games = if (scholar) {
            listOf(
                MysticInteraction(
                    title = "六十秒校准",
                    description = "先别改命盘，只给接下来一小时定一个方向。",
                    options = listOf(
                        MysticInteractionOption("写下最重要的一件", "好，把它放在视线里；其他事先排队。"),
                        MysticInteractionOption("把干扰挪远一点", "对，给注意力留一条干净的通道。"),
                        MysticInteractionOption("做三分钟热身", "很好，启动比完美更能带走停滞感。")
                    )
                ),
                MysticInteraction(
                    title = "可控分拣",
                    description = "把心里盘旋的事放进三只匣子。",
                    options = listOf(
                        MysticInteractionOption("现在就能做", "这只匣子最轻，先从这里拿回掌控感。"),
                        MysticInteractionOption("今晚再处理", "可以，给它一个具体时间就不算悬着。"),
                        MysticInteractionOption("其实可以先放下", "承认不必做，也是一种很干净的整理。")
                    )
                ),
                MysticInteraction(
                    title = "最小一步",
                    description = "为最需要照看的地方挑一件小事。",
                    options = listOf(
                        MysticInteractionOption("十分钟整理", "十分钟后停下即可；小承诺更容易守住。"),
                        MysticInteractionOption("说一句真话", "表达清楚需求，关系里的雾会散掉一些。"),
                        MysticInteractionOption("先休息一下", "低电量时，休息不是偷懒，是校准。")
                    )
                ),
                MysticInteraction(
                    title = "两栏笔记",
                    description = "把今天的事分成“我能做”和“我只能等”。",
                    options = listOf(
                        MysticInteractionOption("我能做的一件", "很好，把它放到下一个二十五分钟里。"),
                        MysticInteractionOption("只能等的一件", "写下来就够了；等待也可以被安放。"),
                        MysticInteractionOption("先划掉一件", "少一件事，盘面会立刻清爽一点。")
                    )
                ),
                MysticInteraction(
                    title = "三分钟观察站",
                    description = "选一个信号，接下来只观察它。",
                    options = listOf(
                        MysticInteractionOption("呼吸的节奏", "先让身体成为参照物，答案会慢下来。"),
                        MysticInteractionOption("最常打开的软件", "看清注意力的去向，不做批评，只做记录。"),
                        MysticInteractionOption("今天的低电量时刻", "找到它，明天就能提前设一个休息点。")
                    )
                )
            )
        } else {
            listOf(
                MysticInteraction(
                    title = "仙家三宝",
                    description = "本半仙打开云柜，你只能摸一样！",
                    options = listOf(
                        MysticInteractionOption("摸锦囊", "锦囊里没有天机，只有一句话：先把最难的事啃一小口。"),
                        MysticInteractionOption("摇小铃铛", "叮！这是提醒信号，不是催命符；该问就去问。"),
                        MysticInteractionOption("抱云朵枕", "抱紧了。软一点没关系，今天允许你边回血边推进。")
                    )
                ),
                MysticInteraction(
                    title = "天庭弹幕",
                    description = "选一条弹幕挂在你头顶护体。",
                    options = listOf(
                        MysticInteractionOption("稳住，能赢", "弹幕已置顶！但赢的定义由你来定，不用硬撑给别人看。"),
                        MysticInteractionOption("退一步不丢人", "这条弹幕很贵。绕路不是输，是聪明的神仙都会用的导航。"),
                        MysticInteractionOption("先吃口热的", "天庭认证！胃暖了，脑子的仙气才会通。")
                    )
                ),
                MysticInteraction(
                    title = "云朵点名",
                    description = "点一位仙官来值班。",
                    options = listOf(
                        MysticInteractionOption("财神候场", "他只管机会，不管冲动账单；小额清醒花，别让他打瞌睡。"),
                        MysticInteractionOption("月老探头", "他递来的不是红线，是话筒：把想要什么说清楚。"),
                        MysticInteractionOption("太白记笔记", "老头子写下四个字：少开五个头。专一比热闹灵光。")
                    )
                ),
                MysticInteraction(
                    title = "云上签筒",
                    description = "抽一支不吓人的仙家提示！",
                    options = listOf(
                        MysticInteractionOption("先做小事", "签文只有四个字：小事先行。别小看它，这招最灵。"),
                        MysticInteractionOption("说清楚点", "上签！把需求讲明白，神仙都省得猜谜。"),
                        MysticInteractionOption("歇一小会儿", "云朵盖章：休息不是偷懒，是给仙气充电。")
                    )
                ),
                MysticInteraction(
                    title = "仙界快递",
                    description = "本半仙给你寄个包裹，选一个！",
                    options = listOf(
                        MysticInteractionOption("一盒勇气", "已发货！用量说明：先用于那件拖了很久的小事。"),
                        MysticInteractionOption("一条边界", "签收成功。今天可以对多余的任务说：仙鹤也要下班。"),
                        MysticInteractionOption("十分钟安静", "包裹有点轻，效果不小；安静完记得回来。")
                    )
                )
            )
        }
        val seed = interactionSeed(mode, topicKey, fortune, 0)
        val picked = ((seed / 19L).toInt() + round.coerceAtLeast(0)) % games.size
        return games[picked]
    }

    /** 追问前的短反应；repeat 用次数递进，让连续追问像被同一个人记住了。 */
    fun reaction(
        mode: String,
        action: String,
        askedCount: Int,
        styleKey: String = ""
    ): String {
        val scholar = mode != "half"
        val count = askedCount.coerceAtLeast(1)
        return if (scholar) {
            when (action) {
                "branch" -> "先接住刚才那句；这条我们分开看，不急着混在一起。"
                "repeat" -> if (count == 2) {
                    "你又问了一遍。我猜不是没听懂，是这句话还没落进心里。"
                } else {
                    "还在想这件事？那我们把入口再缩小一点。"
                }
                else -> when (styleKey) {
                    "archive" -> when (count % 3) {
                        0 -> "我把这一页又翻了一遍，你问到点子上了。"
                        1 -> "这个问题我先归档；慢慢拆，不急着下结论。"
                        else -> "嗯，档案里最稳的线索还是盘面。"
                    }
                    "harbor" -> when (count % 3) {
                        0 -> "好，这句话我听见了；我们把它的来路拆开。"
                        1 -> "可以慢慢问，这里不用赶时间。"
                        else -> "我先陪你把情绪放稳，再看数字怎么走。"
                    }
                    else -> when (count % 3) {
                        0 -> "这个问得好，罗盘可以先指一个小方向。"
                        1 -> "我们只转一格，看看哪里最先清楚。"
                        else -> "方向要能落地；我来帮你收窄一点。"
                    }
                }
            }
        } else {
            when (action) {
                "branch" -> "喂喂，话题拐弯也要给云朵一点反应时间！"
                "repeat" -> if (count == 2) {
                    "又问？行，本半仙就喜欢你这份不死心。"
                } else {
                    "还惦记着呢？好吧，仙界给你加播一次。"
                }
                else -> when (styleKey) {
                    "herald" -> when (count % 3) {
                        0 -> "锣鼓已响！这个问题有点锋利，本司仪先垫块云！"
                        1 -> "好问题！开场词都替你想好了！"
                        else -> "稍等，天庭司仪正在翻盘面！"
                    }
                    "alley" -> when (count % 3) {
                        0 -> "哟，这话够直接；本半仙先给你沏口大碗茶。"
                        1 -> "好问题！街口消息灵通，但咱不吓人。"
                        else -> "稍等，半仙正在跟云朵打听！"
                    }
                    else -> when (count % 3) {
                        0 -> "这个问题有点锋利，实习生小本本已掏出来！"
                        1 -> "好问题！法术不稳，态度先拉满。"
                        else -> "稍等，云端工单正在流转！"
                    }
                }
            }
        }
    }

    /** 小游戏选择也会被当作一句“用户发言”，玄师按自己的作风先接一句。 */
    fun interactionReaction(mode: String, styleKey: String): String {
        val scholar = mode != "half"
        return if (scholar) {
            when (styleKey) {
                "archive" -> "我先把这项记在旁边；做法比说法更重要。"
                "harbor" -> "好，你选的这一步我已经接住了。"
                else -> "这个方向够小，适合真的走一步。"
            }
        } else {
            when (styleKey) {
                "herald" -> "恭喜！这条选择已经敲锣送进云海！"
                "alley" -> "行，就按这个来；咱不整虚的。"
                else -> "已登记工单！实习生保证不把它弄丢。"
            }
        }
    }

    fun topicLabel(key: String): String = topics[key] ?: "综合"

    /** 等待行也带作风，让“正在处理”像一个人手上的小动作。 */
    fun thinkingLine(mode: String, styleKey: String, kind: String): String {
        val scholar = mode != "half"
        return if (scholar) {
            when (styleKey) {
                "archive" -> when (kind) {
                    "game" -> "正在把你的选择抄进档案"
                    "handoff" -> "正在把上一页夹好"
                    else -> "正在翻对应的那一页"
                }
                "harbor" -> when (kind) {
                    "game" -> "正在看你选出的那一步"
                    "handoff" -> "正在给话题换个坐姿"
                    else -> "先接住这句话"
                }
                else -> when (kind) {
                    "game" -> "正在核对最小一步"
                    "handoff" -> "罗盘准备只转一格"
                    else -> "指针正在慢慢对齐"
                }
            }
        } else {
            when (styleKey) {
                "herald" -> when (kind) {
                    "game" -> "锣鼓小队正在验票"
                    "handoff" -> "换场锣鼓正在调音"
                    else -> "天庭司仪正翻到那一页"
                }
                "alley" -> when (kind) {
                    "game" -> "半仙正在给你递签"
                    "handoff" -> "大碗茶先挪个位置"
                    else -> "街口半仙正在打听"
                }
                else -> when (kind) {
                    "game" -> "云上工单正在登记"
                    "handoff" -> "云端工单正在改派"
                    else -> "实习生法术加载中"
                }
            }
        }
    }

    fun handoffReaction(mode: String, styleKey: String): String {
        val scholar = mode != "half"
        return if (scholar) {
            when (styleKey) {
                "archive" -> "旧线索已夹进书页，不会丢。"
                "harbor" -> "刚才的话题先放在垫子上，随时可以回来。"
                else -> "方向记下了；现在只把指针转向新的一格。"
            }
        } else {
            when (styleKey) {
                "herald" -> "换场锣鼓已响，旧话题在后台候场！"
                "alley" -> "行，大碗茶不撤；咱先聊新来的这摊。"
                else -> "改派成功！旧话题挂起，新工单置顶。"
            }
        }
    }

    fun topicHandoff(
        mode: String,
        styleKey: String,
        fromTopicKey: String,
        toTopicKey: String
    ): String {
        val from = topicLabel(fromTopicKey)
        val to = topicLabel(toTopicKey)
        val scholar = mode != "half"
        return if (scholar) {
            when (styleKey) {
                "archive" -> "我把「$from」那页先夹好，现在翻到「${to}」。两条线索可以互相参照。"
                "harbor" -> "「${from}」先放在旁边歇一会儿；我们轻轻转到「${to}」，不用把它关门外。"
                else -> "「${from}」的方向已经记下。罗盘只转一格，先看「${to}」最清楚的位置。"
            }
        } else {
            when (styleKey) {
                "herald" -> "换场！「${from}」先去后台候着，「${to}」带着盘面登台！"
                "alley" -> "换得挺快啊？行，「${from}」的茶还温着；咱先看看「${to}」这摊。"
                else -> "工单已改派：「${from}」暂时挂起，「${to}」置顶。实习生保证旧话题不弄丢！"
            }
        }
    }

    /** DJB2 取样；两端用同一字符码与质数模，避免各端人设漂移。 */
    private fun presenceSeed(topicKey: String, fortune: CompositeDailyFortune): Long {
        val source = "${canonicalDateKey(fortune.dateKey)}|$topicKey|${fortune.overallScore}|${fortune.luckyNumber}"
        var hash = 5381L
        for (char in source) {
            hash = (hash * 33L + char.code) % 2147483647L
        }
        return hash
    }

    private fun interactionSeed(
        mode: String,
        topicKey: String,
        fortune: CompositeDailyFortune,
        round: Int
    ): Long {
        val safeRound = round.coerceAtLeast(0)
        val source = "${canonicalDateKey(fortune.dateKey)}|$mode|$topicKey|${fortune.overallScore}|$safeRound"
        var hash = 52711L
        for (char in source) {
            hash = (hash * 37L + char.code) % 2147483647L
        }
        return hash
    }

    private fun canonicalDateKey(value: String): String {
        val parts = value.split("-")
        if (parts.size != 3) return value
        val year = parts[0].toIntOrNull() ?: return value
        val month = parts[1].toIntOrNull() ?: return value
        val day = parts[2].toIntOrNull() ?: return value
        return "$year-$month-$day"
    }

    private fun arrivalLine(scholar: Boolean, score: Int, seed: Long): String {
        val lines = when {
            scholar && score >= 65 -> listOf(
                "我刚看完这页盘面。数不错，你可以少怀疑自己一点。",
                "路过看见这个分，先坐下来替你说一句：它值得高兴。",
                "今天这份势能是真的，不过别急着把它一天用完。"
            )
            scholar && score < 45 -> listOf(
                "我在旁边看了一会儿。先别骂自己，这只是提醒，不是结论。",
                "低分的意思是该收着走，不是你不行的证据。",
                "我把盘面又核了一遍。慢一点，先把最要紧的一件事照顾好。"
            )
            scholar -> listOf(
                "我刚好经过，看了一眼。平稳也是一种能继续走的状态。",
                "不用逼它开花，今天的节奏适合把细节捋顺。",
                "我在这里陪你看一会儿，有疑问就慢慢问。"
            )
            score >= 65 -> listOf(
                "哟，这么体面？看来本半仙准备的符水要放凉了。",
                "行啊你，这排面都敢摆出来；别得意，本半仙还盯着呢。",
                "啧，好运用得挺熟练啊，记得留一点明天阴阳我。"
            )
            score < 45 -> listOf(
                "咳，这盘面有点害羞。笑什么，本半仙又不是来收你加班费的。",
                "别看我锣敲得响，今天只准你退三步，不准你认输。",
                "这信号确实闹脾气了；听半仙一句，先躺平回血再说。"
            )
            else -> listOf(
                "本半仙掐指一算：不惊不喜，适合把琐事一个个收拾掉。",
                "温吞仙汤一碗，喝了不惊艳，但至少不会烫嘴。",
                "我路过闻了闻，今天没有大雷，也没有免费馅饼。"
            )
        }
        return lines[((seed / 11L) % lines.size).toInt()]
    }

    private fun scholarHeadline(score: Int, label: String): String = when {
        score >= 80 -> "${label}有势能，你可以安心接住"
        score >= 65 -> "${label}方向清楚，节奏可以温柔些"
        score >= 50 -> "${label}正在蓄力，不必逼它开花"
        score >= 35 -> "${label}需要小步确认，而不是大步证明"
        else -> "先把${label}安顿好，再安排世界"
    }

    private fun halfHeadline(score: Int, label: String): String = when {
        score >= 80 -> "不得了！${label}直接踩着祥云起飞"
        score >= 65 -> "${label}火力在线，神仙都要侧目"
        score >= 50 -> "${label}稳如老君炉，别慌"
        score >= 35 -> "${label}有点闹脾气，得哄"
        else -> " ${label}暂时躲进云里充电了"
    }

    private fun bandSentence(score: Int): String = when {
        score >= 80 -> "现在的关键不是怀疑机会，而是把注意力放在能让你稳定发挥的选择上。"
        score >= 65 -> "推进是合适的，只是把期待拆成几个可完成的小节点，会更轻松。"
        score >= 50 -> "平稳不代表平淡，它给你空间整理节奏、修补细节。"
        score >= 35 -> "低分不是否定，而是身体和情绪在提醒你收缩战线。"
        else -> "此刻最有效的行动是休息、求助和把任务缩小到不会吓跑自己的程度。"
    }

    private fun halfBandSentence(score: Int): String = when {
        score >= 80 -> "这分数都快溢出八卦炉了，好运追着你跑，记得留个门！"
        score >= 65 -> "运势小火苗烧得很旺，适合把计划端上桌，别让它干等！"
        score >= 50 -> "不惊不喜，像一碗温吞仙汤，喝完照样能走路带风。"
        score >= 35 -> "星星在天上挤眉弄眼：今天别硬闯，绕个路更灵光！"
        else -> "云层信号有点差，宜躺平回血，不宜跟命运掰手腕！"
    }

    private fun whyAnswer(
        scholar: Boolean,
        label: String,
        focusScore: Int,
        eastScore: Int,
        westScore: Int
    ): String {
        val gap = kotlin.math.abs(eastScore - westScore)
        val stronger = if (eastScore >= westScore) "东方盘" else "西方盘"
        val weaker = if (eastScore >= westScore) "西方盘" else "东方盘"
        return if (scholar) {
            "${label}的 $focusScore 分来自两侧交叉核对：东方 $eastScore 分，西方 $westScore 分。" +
                if (gap >= 20) {
                    "${stronger}更给力，${weaker}偏保守；不必硬选一边，先让稳的那边带路。"
                } else {
                    "两边口径接近，说明这个判断比较稳，可以放心当作今天的参照。"
                }
        } else {
            "别看只是一个 $focusScore，背后可是东方 $eastScore 分、西方 $westScore 分在开会！" +
                if (gap >= 20) {
                    "${stronger}嗓门最大，${weaker}在旁边泼温水；先听强的，也别把弱的锁门外。"
                } else {
                    "两边意见罕见一致，这信号可信度直接拉满！"
                }
        }
    }

    private fun actionAnswer(scholar: Boolean, highLabel: String, lowLabel: String, color: String, direction: String): String =
        if (scholar) {
            "先给「$lowLabel」十分钟的照看，再做一件能让「$highLabel」落地的小事。" +
                "今天可用「$color」和「$direction」当状态开关：换颜色、调座位或出门方向，都是提醒自己切换节奏。"
        } else {
            "给「$lowLabel」递杯仙气水，再让「$highLabel」冲锋！" +
                "记得带上「$color」，往「$direction」挪一挪；这不是魔法命令，是给你换个心理档位。"
        }

    private fun careAnswer(scholar: Boolean, lowLabel: String, cautions: String): String {
        val cleanCaution = cautions.trim().ifBlank { "保持规律，别把日程塞太满" }
        return if (scholar) {
            "盘面提醒的重点是「$lowLabel」：$cleanCaution。" +
                "这些是倾向描述，不是判决；如果状态持续不舒服，请优先休息或寻求专业帮助。"
        } else {
            "天界小黑板写的是「$lowLabel」：$cleanCaution！" +
                "半仙只负责敲锣，不负责吓人；真不舒服就去休息，别硬撑成苦瓜。"
        }
    }

    private fun topicAnswer(
        scholar: Boolean,
        topicKey: String,
        label: String,
        focusScore: Int,
        highLabel: String,
        lowLabel: String,
        testName: String
    ): String {
        val strong = focusScore >= 65
        val mid = focusScore >= 35 && !strong
        val opener = when {
            strong -> "「$label」有空间"
            mid -> "「$label」适合小步走"
            else -> "「$label」要先减负"
        }
        return if (scholar) {
            val tail = when (topicKey) {
                "composite" -> "把注意力放在「$highLabel」，同时给「$lowLabel」留缓冲。"
                "career" -> "挑一件最重要的事推进，沟通时把需求说清楚，比同时开五个头更有力。"
                "love" -> "少一点猜测，多一点具体表达；关系里的安全感的来源之一是把话说开。"
                "wealth" -> "先守住必要支出，再考虑尝试；金额越小，决策越清醒。"
                "study" -> "把目标切成二十五分钟的小段，先完成一次回顾，再谈突破。"
                "health" -> "优先睡眠、饮食和活动量；身体信号值得被认真对待。"
                "test" -> "可以把「${testName.ifBlank { "最近测试" }}」当自我观察材料，与命盘互相参照，不单独下结论。"
                else -> "结合「$highLabel」推进，同时照看「$lowLabel」。"
            }
            "$opener。$tail"
        } else {
            val tail = when (topicKey) {
                "composite" -> "「$highLabel」举火把，「$lowLabel」坐轿子，路线已经很清楚啦！"
                "career" -> "主打一招，别十八般武艺同时抡；把关键话说漂亮，胜过加班到冒烟。"
                "love" -> "直球可以扔，阴阳怪气快收起来；具体说想要什么，才不会被误会的云雾罩住。"
                "wealth" -> "钱包系好绳，小额定投快乐可以，大额冲动先冷冻三天。"
                "study" -> "番茄钟启动！先把最烦的那块啃一小口，成就感会自动续杯。"
                "health" -> "仙体也要保养：早点躺，好好吃，动一动，别和沙发签订永久契约。"
                "test" -> "「${testName.ifBlank { "最近测试" }}」只是镜子，不是审判书；拿来认识自己刚刚好。"
                else -> "让「$highLabel」打头阵，别把「$lowLabel」丢在后山。"
            }
            "$opener！$tail"
        }
    }
}
