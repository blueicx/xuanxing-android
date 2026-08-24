package com.xuanji.app.domain

import com.xuanji.app.data.model.BaziFull
import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.model.TestRecord
import java.text.Normalizer
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

data class MysticOpeningOption(
    val key: String,
    val label: String,
    val response: String
)

data class MysticOpeningCheckin(
    val prompt: String,
    val options: List<MysticOpeningOption>
)

data class MysticRhythmOption(
    val key: String,
    val label: String,
    val response: String
)

data class MysticRhythmCheckin(
    val prompt: String,
    val options: List<MysticRhythmOption>
)

data class MysticGuestCameo(
    val roleName: String,
    val line: String
)

data class MysticGuestChoice(
    val key: String,
    val label: String
)

data class MysticGuestExit(
    val roleName: String,
    val line: String
)

data class MysticClarifierOption(
    val key: String,
    val label: String,
    val answer: String
)

data class MysticClarifier(
    val title: String,
    val options: List<MysticClarifierOption>
)

data class MysticSkin(
    val id: String,
    val label: String,
    val detail: String,
    val back: Long,
    val garment: Long,
    val trim: Long,
    val voiceLabel: String,
    val voiceIntro: String,
    val gameLead: String,
    val reactionTail: String
)

/**
 * 双面灵语：玄学家负责基于现有算法结果做心理按摩，半仙负责浮夸调侃。
 * 不使用随机数；同一个人、同一天、同一问题、同一模式必然得到同一回答。
 */
object MysticGuideGenerator {
    private val skinGameOrder = mapOf(
        "jiangnan-robe" to listOf("最小一步", "回声提问", "强弱接力"),
        "academy-gown" to listOf("六十秒校准", "两栏笔记", "开关实验"),
        "silkroad-robe" to listOf("可控分拣", "三分钟观察站", "强弱接力"),
        "northland-mantle" to listOf("三格沙盘", "边界清点", "开关实验"),
        "cloud-daoist" to listOf("仙家三宝", "云上签筒", "仙家调配室"),
        "street-jacket" to listOf("天庭弹幕", "小道消息分拣", "幸运快递·定制"),
        "desert-traveler" to listOf("仙气盲盒", "仙界快递", "仙家调配室"),
        "festival-costume" to listOf("云朵点名", "天庭弹幕", "幸运快递·定制")
    )

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

    fun mysticSkins(mode: String): List<MysticSkin> = if (mode == "half") {
        listOf(
            MysticSkin(
                "cloud-daoist", "云纹道袍", "朱砂绦 · 金云补子", 0xFFF5E3D4, 0xFF97654A, 0xFFD89B62,
                "云雾仙腔", "天机不急，咱先把它翻译成人话。", "云柜开了一条缝——", "仙气登记完毕！"
            ),
            MysticSkin(
                "street-jacket", "街口短打", "铜铃袖口 · 布扣", 0xFFF0E4DA, 0xFF84604F, 0xFFCBA96C,
                "街口直给", "咱不绕弯子，说完就去做。", "茶碗一放，", "成，这事就这么办！"
            ),
            MysticSkin(
                "desert-traveler", "流沙旅袍", "铜镜腰牌 · 沙金披肩", 0xFFF3E6CB, 0xFFA87C4F, 0xFFE4C57C,
                "流沙慢热", "沙子里走路，先试一步再落脚。", "风把签筒推过来——", "嗯，脚印留下了。"
            ),
            MysticSkin(
                "festival-costume", "节庆戏袍", "纸符袋 · 撞色滚边", 0xFFF4DBD8, 0xFFA05F63, 0xFFD9A05B,
                "锣鼓戏腔", "好戏开场，但台词都从盘面里来！", "锣鼓点起——", "漂亮！这一幕记进戏折！"
            )
        )
    } else {
        listOf(
            MysticSkin(
                "jiangnan-robe", "江南书生袍", "青玉襟 · 素袖", 0xFFDCEAE4, 0xFF6F9C90, 0xFFD9C58B,
                "温润书卷", "我把话说软一点，但依据不会少。", "先把茶放稳——", "这一步选得妥帖。"
            ),
            MysticSkin(
                "academy-gown", "星港学士服", "星扣领 · 深灰披巾", 0xFFDFE5F3, 0xFF77809F, 0xFFE2C275,
                "清朗学理", "我们按证据走，一句一句对齐。", "先立个假设：", "记录已入册，结论留给你验证。"
            ),
            MysticSkin(
                "silkroad-robe", "丝路学者袍", "藏书腰带 · 松石缠巾", 0xFFDCEDEA, 0xFF48948F, 0xFFE7BE68,
                "远行务实", "路要分段走，盘面也按驿站看。", "换到下一站前，", "好，这匹骆驼先驮这一件。"
            ),
            MysticSkin(
                "northland-mantle", "北地游学斗篷", "银扣 · 苔绿毛边", 0xFFE1EBDC, 0xFF799458, 0xFFD7E0C6,
                "稳拙守边", "风大的时候，边界比速度更要紧。", "先扎稳帐篷：", "行，这一步踩得实。"
            )
        )
    }

    fun mysticSkinVoice(mode: String, skinId: String): MysticSkin? =
        mysticSkins(mode).firstOrNull { it.id == skinId }

    private fun orderedInteractionGames(
        games: List<MysticInteraction>,
        skinId: String
    ): List<MysticInteraction> {
        val preferredTitles = skinGameOrder[skinId].orEmpty()
        val preferred = preferredTitles.mapNotNull { title -> games.firstOrNull { it.title == title } }
        return preferred + games.filter { game -> preferred.none { it.title == game.title } }
    }

    fun defaultMysticSkin(mode: String, fortune: CompositeDailyFortune): MysticSkin {
        val skins = mysticSkins(mode)
        val source = "${canonicalDateKey(fortune.dateKey)}|$mode|${fortune.overallScore}|${fortune.luckyNumber}|mystic-skin"
        var hash = 5381L
        for (char in source) {
            hash = (hash * 33L + char.code) % 2147483647L
        }
        return skins[(hash % skins.size).toInt()]
    }

    fun nextMysticSkin(mode: String, currentId: String): MysticSkin {
        val skins = mysticSkins(mode)
        val index = skins.indexOfFirst { it.id == currentId }
        return skins[(if (index < 0) 0 else index + 1) % skins.size]
    }

    /** 同一天、同一命盘默认由同一位玄师陪伴；好运坏运不预设谁来接话。 */
    @Suppress("UNUSED_PARAMETER")
    fun suggestedMode(topicKey: String, fortune: CompositeDailyFortune): String {
        val seed = companionSeed(fortune)
        return if (seed % 2L == 0L) "scholar" else "half"
    }

    /** 卡片头部的短状态：只说明玄师此刻的工作姿态，不判断用户运势。 */
    fun presenceState(mode: String, styleKey: String, exchangeCount: Int): String {
        val count = exchangeCount.coerceAtLeast(0)
        val scholar = mode != "half"
        return if (scholar) {
            when (styleKey) {
                "archive" -> when {
                    count <= 0 -> "档案刚翻开"
                    count <= 2 -> "档案翻了几页"
                    count <= 4 -> "档案桌上线索渐多"
                    else -> "档案桌正忙着核对"
                }
                "harbor" -> when {
                    count <= 0 -> "灯刚点起来"
                    count <= 2 -> "垫子上放进了几句话"
                    count <= 4 -> "港口正在换气"
                    else -> "潮声来回正热闹"
                }
                "compass" -> when {
                    count <= 0 -> "罗盘刚归位"
                    count <= 2 -> "罗盘微调过一格"
                    count <= 4 -> "指针还在慢慢对齐"
                    else -> "几条方向并排摆着"
                }
                else -> when {
                    count <= 0 -> "档案刚翻开"
                    count <= 2 -> "档案翻了几页"
                    count <= 4 -> "档案桌上线索渐多"
                    else -> "档案桌正忙着核对"
                }
            }
        } else {
            when (styleKey) {
                "herald" -> when {
                    count <= 0 -> "锣鼓正在候场"
                    count <= 2 -> "台词已经排开"
                    count <= 4 -> "场记单渐渐变厚"
                    else -> "舞台正处在换场节奏"
                }
                "alley" -> when {
                    count <= 0 -> "大碗茶刚放下"
                    count <= 2 -> "茶已经续了一轮"
                    count <= 4 -> "街口聊出了热气"
                    else -> "几摊话题一起开着"
                }
                else -> when {
                    count <= 0 -> "云端工单刚新建"
                    count <= 2 -> "小本本添了几行"
                    count <= 4 -> "便签开始排起队"
                    else -> "多张工单并行流转"
                }
            }
        }
    }

    /** 把小游戏选项折成一句短回声；下一次非游戏反应用完即清。 */
    fun interactionCarryover(mode: String, styleKey: String, optionLabel: String): String {
        val label = optionLabel.trim()
        if (label.isEmpty()) return ""
        val scholar = mode != "half"
        return if (scholar) {
            when (styleKey) {
                "archive" -> "档案边角先记一笔：你刚才选了「$label」。"
                "harbor" -> "我记得你刚才选了「$label」，先把它放在手边。"
                "compass" -> "你刚才选的「$label」，我当作一个参照点留着。"
                else -> "你刚才选的「$label」，我当作一个参照点留着。"
            }
        } else {
            when (styleKey) {
                "herald" -> "刚才那句「$label」已在后台登记！"
                "alley" -> "行，你刚挑的是「$label」，咱记着这茬。"
                else -> "工单备注：你刚才选了「$label」。"
            }
        }
    }

    fun composeReaction(carryover: String?, reaction: String): String {
        val echo = carryover?.trim().orEmpty()
        val base = reaction.trim()
        return when {
            echo.isEmpty() -> base
            base.isEmpty() -> echo
            else -> "$echo $base"
        }
    }

    /** 现场手记只记录陪伴动作；同输入固定输出，不追加运势判断。 */
    fun memoryNote(mode: String, styleKey: String, kind: String, detail: String): String {
        val familyStyles = when (mode) {
            "scholar" -> setOf("archive", "harbor", "compass")
            "half" -> setOf("herald", "alley", "intern")
            else -> return ""
        }
        if (styleKey !in familyStyles || kind !in setOf("opening", "rhythm", "game", "guest", "ask", "handoff")) {
            return ""
        }

        var clean = Normalizer.normalize(detail, Normalizer.Form.NFC)
        clean = clean.replace(Regex("[\\u0000-\\u0008\\u000B-\\u001F\\u007F-\\u009F\\u200B-\\u200F\\uFEFF]"), "")
        clean = clean.replace(Regex("[\\s\\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000]+"), " ").trim()
        if (clean.isEmpty()) return ""
        if (clean.codePointCount(0, clean.length) > 24) {
            clean = clean.substring(0, clean.offsetByCodePoints(0, 24)) + "…"
        }

        return when (styleKey) {
            "archive" -> when (kind) {
                "opening" -> "我把开场这一步记进档案：$clean。"
                "rhythm" -> "节奏栏补了一笔：$clean。"
                "game" -> "小游戏这一步留下记录：$clean。"
                "guest" -> "客串那阵的立场已记下：$clean。"
                "ask" -> "你问的这句被页边折角保留：$clean。"
                else -> "从${clean}换页，旧线索仍夹在原处。"
            }
            "harbor" -> when (kind) {
                "opening" -> "我把开场这一步放到灯下：$clean。"
                "rhythm" -> "你的节奏是${clean}，我先替你收着。"
                "game" -> "小游戏里的${clean}，我摆在容易看见的地方。"
                "guest" -> "客串退开后，你的立场是${clean}。"
                "ask" -> "你问到的${clean}，我先把这句接稳。"
                else -> "从${clean}走过来，水面还留着刚才的痕迹。"
            }
            "compass" -> when (kind) {
                "opening" -> "开场签到成为第一个参照点：$clean。"
                "rhythm" -> "我把${clean}标进今天的速度栏。"
                "game" -> "小游戏选过${clean}，指针旁留了个标记。"
                "guest" -> "客串立场停在罗盘边缘：${clean}。"
                "ask" -> "这句问题钉在当前方位：${clean}。"
                else -> "从${clean}转页，旧标记没有抹掉。"
            }
            "herald" -> when (kind) {
                "opening" -> "开场锣鼓收到：${clean}已入场！"
                "rhythm" -> "节奏台本补一笔：${clean}登记完毕！"
                "game" -> "小游戏这一手挂上侧幕：${clean}！"
                "guest" -> "客串台词痕迹留下：${clean}！"
                "ask" -> "这句被递到台前，场记先收好：${clean}！"
                else -> "从${clean}换幕，旧场记继续跟着！"
            }
            "alley" -> when (kind) {
                "opening" -> "开场这句咱先搁茶碗边：${clean}。"
                "rhythm" -> "今天这个劲儿我给你记着：${clean}。"
                "game" -> "小游戏挑的这茬留下了：${clean}。"
                "guest" -> "客串那茬你的接法是：${clean}。"
                "ask" -> "你问的这句先摆桌面：${clean}。"
                else -> "从${clean}挪过来，旧茬还在茶边放着。"
            }
            else -> when (kind) {
                "opening" -> "开场记录已提交：${clean}。"
                "rhythm" -> "节奏工单更新为：${clean}。"
                "game" -> "小游戏结果已归档：${clean}。"
                "guest" -> "客串互动备注：${clean}。"
                "ask" -> "问题已加入待核对清单：${clean}。"
                else -> "从${clean}交接，旧标签继续保留。"
            }
        }
    }

    /** 开场签到：只把已有盘面变成一句可回应的入口，不新增命运判断。 */
    fun openingCheckin(
        mode: String,
        topicKey: String,
        styleKey: String,
        fortune: CompositeDailyFortune
    ): MysticOpeningCheckin? {
        if (fortune.dimensions.isEmpty()) return null
        val strong = fortune.dimensions.maxByOrNull { it.score } ?: return null
        val weak = fortune.dimensions.minByOrNull { it.score } ?: return null
        val useColor = fortune.overallScore % 2 == 0
        val switchLabel = if (useColor) fortune.luckyColor else fortune.luckyDirection
        val switchSource = if (useColor) "幸运色" else "吉利方位"
        val scholar = mode != "half"
        val prompt = openingPrompt(scholar, styleKey, topicKey)
        val options = listOf(
            MysticOpeningOption(
                key = "strength",
                label = if (scholar) {
                    "从「${strong.label}」聊起"
                } else {
                    "看看「${strong.label}」的排面"
                },
                response = openingResponse(scholar, styleKey, topicKey, true, strong.label, strong.score)
            ),
            MysticOpeningOption(
                key = "pressure",
                label = if (scholar) {
                    "先照看「${weak.label}」"
                } else {
                    "给「${weak.label}」搭个梯子"
                },
                response = openingResponse(scholar, styleKey, topicKey, false, weak.label, weak.score)
            ),
            MysticOpeningOption(
                key = "switch",
                label = if (scholar) {
                    "用${switchSource}「${switchLabel}」提醒自己"
                } else {
                    "领「${switchLabel}」${switchSource}便签"
                },
                response = openingSwitchResponse(scholar, styleKey, topicKey, switchSource, switchLabel)
            )
        )
        return MysticOpeningCheckin(prompt, options)
    }

    /** 开场选择前的短反应；保持玄师接话的节奏，不做结果承诺。 */
    fun openingReaction(mode: String, styleKey: String): String {
        val scholar = mode != "half"
        return if (scholar) {
            when (styleKey) {
                "archive" -> "好，这一句我先放进今天的档案。"
                "harbor" -> "嗯，你选的门我看见了；慢慢说。"
                else -> "罗盘先停在这里，我们把这条线看清楚。"
            }
        } else {
            when (styleKey) {
                "herald" -> "开场签到收到！锣鼓先收半个音！"
                "alley" -> "行，就从这个茬开聊；茶给你续上。"
                else -> "工单已接！实习生这就翻开对应页！"
            }
        }
    }

    /** 今日节奏签到：把真实盘面锚到用户自报状态上，不新增命运判断。 */
    fun rhythmCheckin(
        mode: String,
        topicKey: String,
        styleKey: String,
        fortune: CompositeDailyFortune
    ): MysticRhythmCheckin? {
        if (fortune.dimensions.isEmpty()) return null
        val strong = fortune.dimensions.maxByOrNull { it.score } ?: return null
        val weak = fortune.dimensions.minByOrNull { it.score } ?: return null
        val useColor = fortune.overallScore % 2 == 0
        val switchLabel = if (useColor) fortune.luckyColor else fortune.luckyDirection
        val switchSource = if (useColor) "幸运色" else "吉利方位"
        val caution = fortune.cautions.trim().replace(Regex("\\s+"), " ")
        val prompt = rhythmPrompt(mode != "half", styleKey, topicKey)
        val options = listOf(
            MysticRhythmOption(
                key = "steady",
                label = "稳稳推进",
                response = rhythmSteadyResponse(
                    mode != "half",
                    styleKey,
                    topicKey,
                    strong.label,
                    strong.score,
                    switchSource,
                    switchLabel
                )
            ),
            MysticRhythmOption(
                key = "tired",
                label = "有点累",
                response = rhythmTiredResponse(
                    mode != "half",
                    styleKey,
                    topicKey,
                    weak.label,
                    weak.score,
                    caution
                )
            ),
            MysticRhythmOption(
                key = "rushed",
                label = "被赶着走",
                response = rhythmRushedResponse(
                    mode != "half",
                    styleKey,
                    topicKey,
                    caution,
                    switchSource,
                    switchLabel
                )
            )
        )
        return MysticRhythmCheckin(prompt, options)
    }

    fun rhythmReaction(mode: String, styleKey: String): String {
        return if (mode != "half") {
            when (styleKey) {
                "archive" -> "好，节奏这一栏我先记下。"
                "harbor" -> "嗯，你的节奏我听见了；不用赶。"
                else -> "速度先标在这里；路线仍由你调。"
            }
        } else {
            when (styleKey) {
                "herald" -> "节奏档位登记完毕！锣鼓跟着你收放！"
                "alley" -> "行，今天按这个劲儿来；茶不催你。"
                else -> "表单提交成功！实习生帮你把节奏置顶！"
            }
        }
    }

    /** 节奏只保存语义档位；下一次可消费动作由当时的人设重新表达。 */
    fun rhythmCarryover(mode: String, styleKey: String, rhythmKey: String): String {
        return if (mode != "half") {
            when (styleKey) {
                "archive" -> when (rhythmKey) {
                    "steady" -> "档案边角补了一笔：你今天选了稳速。"
                    "tired" -> "档案边角记着：你今天有点累。"
                    "rushed" -> "档案边角记着：你今天被催得紧。"
                    else -> ""
                }
                "harbor" -> when (rhythmKey) {
                    "steady" -> "我记得你说今天还稳得住。"
                    "tired" -> "我记得你说今天有些累。"
                    "rushed" -> "我记得你说今天被人推着走。"
                    else -> ""
                }
                else -> when (rhythmKey) {
                    "steady" -> "罗盘旁留了个标记：今天的速度是稳的。"
                    "tired" -> "罗盘旁留了个标记：今天要省一点力。"
                    "rushed" -> "罗盘旁留了个标记：今天的速度偏急。"
                    else -> ""
                }
            }
        } else {
            when (styleKey) {
                "herald" -> when (rhythmKey) {
                    "steady" -> "后台字幕已记：今日稳速前进！"
                    "tired" -> "后台字幕已记：今日电量偏低！"
                    "rushed" -> "后台字幕已记：今日场务别乱催！"
                    else -> ""
                }
                "alley" -> when (rhythmKey) {
                    "steady" -> "你刚说今天还算稳，咱记着这茬。"
                    "tired" -> "你刚说今天累，咱不装没听见。"
                    "rushed" -> "你刚说今天赶，咱先把这事记下。"
                    else -> ""
                }
                else -> when (rhythmKey) {
                    "steady" -> "工单备注：今日节奏稳定。"
                    "tired" -> "工单备注：今日需要省电。"
                    "rushed" -> "工单备注：今日外部催促较多。"
                    else -> ""
                }
            }
        }
    }

    /** 另一位角色只在节奏签到后探一次头；判定与文案都由盘面稳定取样。 */
    fun guestCameo(
        mode: String,
        topicKey: String,
        fortune: CompositeDailyFortune,
        rhythmKey: String = ""
    ): MysticGuestCameo? {
        if (fortune.dimensions.isEmpty()) return null
        val source = listOf(
            canonicalDateKey(fortune.dateKey),
            "guest",
            mode,
            topicKey,
            fortune.overallScore,
            fortune.luckyNumber,
            rhythmKey
        ).joinToString("|")
        var hash = 733L
        for (char in source) {
            hash = (hash * 41L + char.code) % 2147483647L
        }
        if (hash % 5L != 0L) return null

        val high = fortune.dimensions.maxByOrNull { it.score } ?: fortune.dimensions.first()
        val low = fortune.dimensions.minByOrNull { it.score } ?: fortune.dimensions.first()
        val focus = if (topicKey == "test") {
            high
        } else {
            fortune.dimensions.firstOrNull { it.key == topicKey }
                ?: fortune.dimensions.firstOrNull { it.key == "emotion" && topicKey == "love" }
                ?: fortune.dimensions.first()
        }
        val score = fortune.overallScore
        val useColor = score % 2 == 0
        val value = if (useColor) fortune.luckyColor else fortune.luckyDirection
        val sourceName = if (useColor) "幸运色" else "吉利方位"
        val scholarMain = mode != "half"
        val lines = if (scholarMain) {
            when {
                score >= 65 -> listOf(
                    "哟，「${focus.label}」 ${focus.score} 分？行，今天不用本半仙救场，我就在旁边看你得意。",
                    "好家伙，「${high.label}」 ${high.score} 都冒仙气了！先别谢天，明天记得也这么精神。",
                    "${sourceName}「$value」都来捧场了；小心走太快，仙鹤也要看红绿灯。"
                )
                score < 45 -> listOf(
                    "咳，「${low.label}」 ${low.score} 是有点蔫；本半仙不吓你，先把饭吃热、觉睡够。",
                    "别硬撑，「${low.label}」 ${low.score} 只是让你收着走；留三分力气，明天还能翻云。",
                    "这页我看过了，不算完蛋。「${low.label}」要小步走，${sourceName}「$value」就当个提醒。"
                )
                else -> listOf(
                    "我探头看了看，「${focus.label}」 ${focus.score} 分；温吞也有温吞的走法，别自己吓自己。",
                    "这盘不惊不喜，「${low.label}」 ${low.score} 先照顾好；大戏改天再唱。",
                    "${sourceName}「$value」路过递个提醒：今天把琐事收拾干净就够体面了。"
                )
            }
        } else {
            when {
                score >= 65 -> listOf(
                    "我从旁边瞄了一眼：「${focus.label}」 ${focus.score} 分，确实值得高兴；别把这份稳当成必须表演的戏。",
                    "路过替你记一笔：「${high.label}」 ${high.score} 在线。锣鼓可以听，别跟着把自己催热。",
                    "这盘面不算差。${sourceName}「$value」只是提醒，你已经有能接住它的节奏。"
                )
                score < 45 -> listOf(
                    "我在旁边看了一会儿：「${low.label}」 ${low.score} 分只是提醒，不是给你定性的结论。",
                    "路过核对了一遍，「${low.label}」 ${low.score} 要收着照顾；先做最小的一件就好。",
                    "${sourceName}「$value」可以当停顿记号；「${low.label}」需要休息，不需要责备。"
                )
                else -> listOf(
                    "我看了一眼这页：「${focus.label}」 ${focus.score} 分，适合慢慢整理，不必逼它立刻开花。",
                    "路过留下一句：「${low.label}」 ${low.score} 分值得照看；小事做完就可以停下。",
                    "这盘面平稳。${sourceName}「$value」当提醒就好，路线还是由你定。"
                )
            }
        }
        return MysticGuestCameo(
            roleName = if (scholarMain) "半仙" else "玄学家",
            line = lines[((hash / 13L) % lines.size).toInt()]
        )
    }

    /** 客串回应后固定离席；文案只引用盘面上已有的最高和最低维度。 */
    fun guestExitCameo(
        mode: String,
        styleKey: String,
        fortune: CompositeDailyFortune,
        choiceKey: String
    ): MysticGuestExit? {
        if (
            fortune.dimensions.isEmpty() ||
            choiceKey !in setOf("why", "accept", "pushback")
        ) {
            return null
        }

        val high = fortune.dimensions.maxByOrNull { it.score } ?: return null
        val low = fortune.dimensions.minByOrNull { it.score } ?: return null
        val line = when (mode) {
            "scholar" -> when (styleKey) {
                "archive" -> when (choiceKey) {
                    "why" -> "合上档案前补一句：「${high.label}」 ${high.score} 是明面依据；「${low.label}」 ${low.score} 我折了角。"
                    "accept" -> "行，这页先收进「${fortune.luckyColor}」那格；本半仙去隔壁喝茶。"
                    else -> "得得得，不围了！方向留给「${fortune.luckyDirection}」，本半仙退到台侧。"
                }
                "harbor" -> when (choiceKey) {
                    "why" -> "潮水把我推回来半步：「${high.label}」 ${high.score} 看得见，「${low.label}」 ${low.score} 先别硬压。"
                    "accept" -> "你把提醒接住了；我踩着「${fortune.luckyColor}」的光退出去。"
                    else -> "不看了不看了，船交给「${fortune.luckyDirection}」；你自己的舵自己握。"
                }
                "compass" -> when (choiceKey) {
                    "why" -> "指针替我作证：「${high.label}」 ${high.score}，「${low.label}」 ${low.score} 也在盘面上；我先退出刻度外。"
                    "accept" -> "这笔记在「${fortune.luckyColor}」旁边就行；我不挡你的北。"
                    else -> "好好好，罗盘只留一个人看；我去「${fortune.luckyDirection}」那头晃。"
                }
                else -> return null
            }
            "half" -> when (styleKey) {
                "herald" -> when (choiceKey) {
                    "why" -> "场记补一笔：「${high.label}」 ${high.score}，「${low.label}」 ${low.score} 都来自盘面；我先退回侧幕。"
                    "accept" -> "你稳稳接住了提醒；这一幕用「${fortune.luckyColor}」打光刚刚好。"
                    else -> "幕布收了，围观结束；出口朝「${fortune.luckyDirection}」。"
                }
                "alley" -> when (choiceKey) {
                    "why" -> "我把账摆在桌角：「${high.label}」 ${high.score}、「${low.label}」 ${low.score}，都是现成页码；茶碗端走啦。"
                    "accept" -> "成，这句咱搁「${fortune.luckyColor}」边上；我先溜达去街口。"
                    else -> "行行行，不围着瞧；你往「${fortune.luckyDirection}」走，我给你留着道。"
                }
                "intern" -> when (choiceKey) {
                    "why" -> "离席备注已提交：「${high.label}」 ${high.score} / 「${low.label}」 ${low.score}，来源为当前盘面。"
                    "accept" -> "工单关闭：提醒已接收，颜色标记 ${fortune.luckyColor}。"
                    else -> "协作人已移出：围观暂停，下一步朝 ${fortune.luckyDirection}。"
                }
                else -> return null
            }
            else -> return null
        }

        return MysticGuestExit(
            roleName = if (mode == "scholar") "半仙" else "玄学家",
            line = line
        )
    }

    /** 自由提问反问落定后，另一位角色可按稳定取样补一句；不使用运行时随机。 */
    fun clarifierGuestCameo(
        mode: String,
        topicKey: String,
        fortune: CompositeDailyFortune,
        optionKey: String
    ): MysticGuestCameo? {
        val allowedOptionKeys = setOf(
            "low",
            "specific",
            "pause",
            "small",
            "guard",
            "timing",
            "source",
            "risk",
            "next"
        )
        if (fortune.dimensions.isEmpty() || optionKey !in allowedOptionKeys) return null

        val source =
            "${canonicalDateKey(fortune.dateKey)}|clarify-guest|$mode|$topicKey|${fortune.overallScore}|${fortune.luckyNumber}|$optionKey"
        var hash = 811L
        for (char in source) {
            hash = (hash * 37L + char.code) % 2147483647L
        }
        if (hash % 4L != 0L) return null

        val high = fortune.dimensions.maxByOrNull { it.score } ?: fortune.dimensions.first()
        val low = fortune.dimensions.minByOrNull { it.score } ?: fortune.dimensions.first()
        val scholarMain = mode != "half"
        val baseLine = if (scholarMain) {
            when (optionKey) {
                "low" -> "「${low.label}」 ${low.score} 分确实要护住；不过别把它供起来，先给口饭、给口气。"
                "specific" -> "问得细是对的。「${high.label}」 ${high.score} 能用，「${low.label}」 ${low.score} 别硬拉；挑一件说清楚就行。"
                "pause" -> "歇就歇嘛！综合 ${fortune.overallScore} 分又不是熄火；别把休息也排成任务。"
                "small" -> "最小一步最实在。「${high.label}」 ${high.score} 在手边，做完就收工，别贪第三件。"
                "guard" -> "护栏这词好。「${low.label}」 ${low.score} 爱挖坑，大额、大话、大熬夜都先拦住。"
                "timing" -> "时机不用等神仙批文；「${high.label}」 ${high.score} 先开场，「${low.label}」 ${low.score} 那段缓半拍。"
                "source" -> "来源就是那页盘面：综合 ${fortune.overallScore} 分，「${high.label}」 ${high.score}，「${low.label}」 ${low.score}；不是本半仙现编的。"
                "risk" -> "风险在明面上：「${low.label}」 ${low.score} 爱使绊子；嘴慢一点，身体电量留足。"
                else -> "看完就散场！回「${high.label}」 ${high.score} 能推的小事，比赖在这页反复算强。"
            }
        } else {
            when (optionKey) {
                "low" -> "「${low.label}」 ${low.score} 分需要照看，不是定性；先把睡眠和一顿热饭安排稳。"
                "specific" -> "具体一点会更轻。综合 ${fortune.overallScore} 分里，「${high.label}」 ${high.score} 可借力，「${low.label}」 ${low.score} 只要照看。"
                "pause" -> "停十分钟不算耽误。「${low.label}」 ${low.score} 分在提醒省力，先安静一下就好。"
                "small" -> "从「${high.label}」 ${high.score} 挑一件今天能完成的小事；完成比铺开更稳。"
                "guard" -> "先给「${low.label}」 ${low.score} 设边界：金额、承诺和睡眠都留余量。"
                "timing" -> "综合 ${fortune.overallScore} 分适合分批推进；先用「${high.label}」 ${high.score}，难处放到状态好些时。"
                "source" -> "来源是现有算法：综合 ${fortune.overallScore} 分，「${high.label}」 ${high.score} 最强，「${low.label}」 ${low.score} 最需照看；它是参照，不是判决。"
                "risk" -> "眼下先留意「${low.label}」 ${low.score}；别让过度承诺和忽略身体信号抢跑。"
                else -> "看完回到「${high.label}」 ${high.score} 能推动的一小步；「${low.label}」 ${low.score} 只安排照顾动作。"
            }
        }
        val prefix = when ((hash / 17L) % 3L) {
            1L -> "我又瞄了一眼；"
            2L -> "旁听补一句："
            else -> ""
        }

        return MysticGuestCameo(
            roleName = if (scholarMain) "半仙" else "玄学家",
            line = prefix + baseLine
        )
    }

    /** 主玄师回答后，用户可邀请对面角色补一句；出场者固定为当前模式的反面。 */
    fun asideInvite(
        mode: String,
        styleKey: String,
        topicKey: String,
        kind: String,
        fortune: CompositeDailyFortune,
        detail: String = ""
    ): MysticGuestCameo? {
        val allowedKinds = listOf("opening", "rhythm", "ask", "custom", "clarify", "game", "handoff")
        val scholarStyles = listOf("archive", "harbor", "compass")
        val halfStyles = listOf("herald", "alley", "intern")
        if (
            fortune.dimensions.isEmpty() ||
            fortune.luckyColor.isBlank() ||
            fortune.luckyDirection.isBlank() ||
            kind !in allowedKinds ||
            (styleKey !in scholarStyles && styleKey !in halfStyles)
        ) {
            return null
        }

        val cleanDetail = detail.trim().replace(Regex("\\s+"), " ").take(60)
        if (cleanDetail.isEmpty()) return null

        val source = listOf(
            canonicalDateKey(fortune.dateKey),
            "aside-invite",
            mode,
            styleKey,
            topicKey,
            kind,
            fortune.overallScore,
            fortune.luckyNumber,
            cleanDetail
        ).joinToString("|")
        var hash = 977L
        for (char in source) {
            hash = (hash * 47L + char.code) % 2147483647L
        }

        val high = fortune.dimensions.maxByOrNull { it.score } ?: return null
        val low = fortune.dimensions.minByOrNull { it.score } ?: return null
        val focus = if (topicKey == "test") {
            high
        } else {
            fortune.dimensions.firstOrNull { it.key == topicKey }
                ?: fortune.dimensions.firstOrNull { it.key == "emotion" && topicKey == "love" }
                ?: fortune.dimensions.first()
        }
        val scholarHost = mode != "half"
        val roleName = if (scholarHost) "半仙" else "玄学家"
        val score = fortune.overallScore
        val useColor = score % 2 == 0
        val value = if (useColor) fortune.luckyColor else fortune.luckyDirection
        val sourceName = if (useColor) "幸运色" else "吉利方位"
        val lines = if (scholarHost) {
            when {
                score >= 65 -> listOf(
                    "你刚说完「${cleanDetail}」，「${focus.label}」 ${focus.score} 分就跟着冒仙气；先让你神气一会儿。",
                    "好家伙，「${high.label}」 ${high.score} 分撑腰，难怪「${cleanDetail}」说得这么顺。",
                    "${sourceName}「$value」也来捧场了；小心走太快，仙鹤也要看红绿灯。"
                )
                score < 45 -> listOf(
                    "「${cleanDetail}」这句我听见了；「${low.label}」 ${low.score} 分只是累了，先把饭吃热。",
                    "别硬撑，「${low.label}」 ${low.score} 分要省着用；本半仙不吓你。",
                    "这页不算完蛋。「${low.label}」 ${low.score} 分小步走，${sourceName}「$value」当提醒。"
                )
                else -> listOf(
                    "我探头看了看「${cleanDetail}」：「${focus.label}」 ${focus.score} 分，温吞也有温吞的走法。",
                    "不惊不喜的一页。「${low.label}」 ${low.score} 分先照顾好，大戏改天再唱。",
                    "${sourceName}「$value」路过递个提醒：把琐事收拾干净就够体面了。"
                )
            }
        } else {
            when {
                score >= 65 -> listOf(
                    "「${cleanDetail}」问得清楚。「${high.label}」 ${high.score} 分可以用，但不必变成表演。",
                    "我顺着「${cleanDetail}」看了一眼：「${focus.label}」 ${focus.score} 分值得高兴，也可以慢慢用。",
                    "好消息不用急着放大；「${high.label}」 ${high.score} 分和${sourceName}「$value」都只是参照。"
                )
                score < 45 -> listOf(
                    "关于「${cleanDetail}」，我先替你说一句：「${low.label}」 ${low.score} 分是提醒，不是定性。",
                    "「${low.label}」 ${low.score} 分需要照看；先做最小一件，不需要责备自己。",
                    "${sourceName}「$value」可以当停顿记号；「${low.label}」 ${low.score} 分先休息。"
                )
                else -> listOf(
                    "看完「${cleanDetail}」，我想说：「${focus.label}」 ${focus.score} 分适合慢慢整理。",
                    "这一页平稳。「${low.label}」 ${low.score} 分值得照看，小事做完就可以停下。",
                    "${sourceName}「$value」只作提醒；路线还是由你在「${cleanDetail}」之后自己定。"
                )
            }
        }

        val leads = mapOf(
            "archive" to listOf("档案旁补一句：", "我把这页折了个角：", "核对完这一行："),
            "harbor" to listOf("灯下我也看见了：", "泊在旁边听完了：", "水面那边补一句："),
            "compass" to listOf("罗盘旁边插一句：", "指针停在刚才那页：", "路线旁做个小注："),
            "herald" to listOf("锣鼓压低半拍！", "台侧字幕飘过：", "场记快速补一笔："),
            "alley" to listOf("茶碗边上搭一句：", "街口这茬我也听见了；", "我凑近看了半眼；"),
            "intern" to listOf("工单旁批注：", "协作备注送达：", "云端便签轻轻弹出：")
        )
        val leadSet = leads[styleKey]
            ?: leads[if (scholarHost) "herald" else "archive"].orEmpty()
        if (leadSet.isEmpty()) return null
        val leadIndex = ((hash / 13L) % leadSet.size.toLong()).toInt()
        val lineIndex = ((hash / 17L) % lines.size.toLong()).toInt()
        return MysticGuestCameo(roleName, leadSet[leadIndex] + lines[lineIndex])
    }

    /** 客串出现后的三种接法；选项固定，回答只复述盘面线索和节奏语义。 */
    fun guestChoices(): List<MysticGuestChoice> = listOf(
        MysticGuestChoice("why", "问依据：这个判断从哪来？"),
        MysticGuestChoice("accept", "接一句：我先收下提醒。"),
        MysticGuestChoice("pushback", "拦一句：别俩人一起看我。")
    )

    /** 自由提问落定后的固定反问；同一个人对同族问题的接法完全确定。 */
    fun customClarifier(
        mode: String,
        styleKey: String,
        question: String,
        fortune: CompositeDailyFortune,
        test: TestRecord? = null
    ): MysticClarifier? {
        if (fortune.dimensions.isEmpty()) return null
        val high = fortune.dimensions.maxByOrNull { it.score } ?: return null
        val low = fortune.dimensions.minByOrNull { it.score } ?: return null
        val family = when (customIntent(question)) {
            "mood", "care", "health", "love" -> "reflect"
            "career", "wealth", "study", "action", "topic" -> "act"
            "why", "outcome" -> "check"
            else -> return null
        }
        val scholar = mode != "half"

        val title = if (scholar) {
            when (styleKey) {
                "harbor" -> when (family) {
                    "reflect" -> "灯下想轻轻问一句"
                    "act" -> "先把船桨放稳一点"
                    else -> "浪头下面先看一眼锚点"
                }
                "compass" -> when (family) {
                    "reflect" -> "罗盘停在一个岔口"
                    "act" -> "指针想再校一次方向"
                    else -> "北针先确认读数"
                }
                else -> when (family) {
                    "reflect" -> "档案页边有个小问号"
                    "act" -> "档案里还差一行注记"
                    else -> "这份记录还要对个来源"
                }
            }
        } else {
            when (styleKey) {
                "alley" -> when (family) {
                    "reflect" -> "大碗茶边上冒出个问题"
                    "act" -> "动手前咱把袖口掸一掸"
                    else -> "这摊账得翻两页看看"
                }
                "intern" -> when (family) {
                    "reflect" -> "工单备注栏亮了一下"
                    "act" -> "执行前先补一张便签"
                    else -> "归档前先跑一次自检"
                }
                else -> when (family) {
                    "reflect" -> "锣鼓暂停，司仪要补一句"
                    "act" -> "登台前先对一遍台本"
                    else -> "谢幕前先核对节目单"
                }
            }
        }

        val options = when (family) {
            "reflect" -> listOf(
                MysticClarifierOption(
                    key = "low",
                    label = "最想护住哪块？",
                    answer = if (scholar) {
                        "先看「${low.label}」 ${low.score} 分；它不是判决，只是今天最需要照看的信号。综合 ${fortune.overallScore} 分，给它留一点余量就够了。"
                    } else {
                        "「${low.label}」 ${low.score} 分在打盹，综合 ${fortune.overallScore} 分还没塌！别把全部力气都押上去，先护住这块就行。"
                    }
                ),
                MysticClarifierOption(
                    key = "specific",
                    label = "能不能说得更具体？",
                    answer = if (scholar) {
                        "把刚才的问题落到一件事上：综合 ${fortune.overallScore} 分，「${high.label}」 ${high.score} 可用，「${low.label}」 ${low.score} 要照看。越具体，越不容易被情绪带偏。"
                    } else {
                        "别问天机，问具体事！综合 ${fortune.overallScore} 分，「${high.label}」 ${high.score} 在线，「${low.label}」 ${low.score} 爱闹。说清一件事，本半仙才好帮你拆。"
                    }
                ),
                MysticClarifierOption(
                    key = "pause",
                    label = "要不要先歇一步？",
                    answer = if (scholar) {
                        "今天综合 ${fortune.overallScore} 分，幸运色「${fortune.luckyColor}」可以当休息提醒。「${low.label}」 ${low.score} 需要照看，先停十分钟不丢人。"
                    } else {
                        "${fortune.overallScore} 分还想硬冲？「${low.label}」 ${low.score} 都举白旗了！用「${fortune.luckyColor}」提醒自己歇口气，神仙也讲究可持续摸鱼。"
                    }
                )
            )
            "check" -> listOf(
                MysticClarifierOption(
                    key = "source",
                    label = "这个判断从哪来？",
                    answer = if (scholar) {
                        "来源是现有盘面：综合 ${fortune.overallScore} 分，「${high.label}」 ${high.score} 最强，「${low.label}」 ${low.score} 最需照看。它是参照，不是命运盖章。"
                    } else {
                        "账本在这：综合 ${fortune.overallScore} 分，「${high.label}」 ${high.score} 举火把，「${low.label}」 ${low.score} 坐轿子。数字来自算法，不是本半仙半夜编的。"
                    }
                ),
                MysticClarifierOption(
                    key = "risk",
                    label = "眼下最该防什么？",
                    answer = if (scholar) {
                        "最该留意「${low.label}」 ${low.score}；综合 ${fortune.overallScore} 分时，风险常藏在过度承诺和忽略身体信号里。先把边界写清楚。"
                    } else {
                        "「${low.label}」 ${low.score} 爱使绊子，综合 ${fortune.overallScore} 分时最怕嘴上答应太快、身体电量太低。先留退路，别硬闯。"
                    }
                ),
                MysticClarifierOption(
                    key = "next",
                    label = "看完后往哪走？",
                    answer = if (scholar) {
                        "看完这一格，先回到「${high.label}」 ${high.score} 能推动的小事；「${low.label}」 ${low.score} 只安排照看动作，不用反复占卜。"
                    } else {
                        "别赖在签筒前啦！综合 ${fortune.overallScore} 分，「${high.label}」 ${high.score} 已备好；回去做一件小事，比再抽十次都灵。"
                    }
                )
            )
            else -> listOf(
                MysticClarifierOption(
                    key = "small",
                    label = "最小一步选哪个？",
                    answer = if (scholar) {
                        "从「${high.label}」 ${high.score} 借力，挑一件最小、今天一定能完成的事；综合 ${fortune.overallScore} 分，完成比铺开更有用。"
                    } else {
                        "别摆十八般武艺！综合 ${fortune.overallScore} 分，「${high.label}」 ${high.score} 是你的趁手家伙；先做一小步，功劳簿也好记账。"
                    }
                ),
                MysticClarifierOption(
                    key = "guard",
                    label = "哪里需要先设护栏？",
                    answer = if (scholar) {
                        "「${low.label}」 ${low.score} 是今天的护栏位。金额、承诺和睡眠先设上限，幸运色「${fortune.luckyColor}」只当冷静开关。"
                    } else {
                        "「${low.label}」 ${low.score} 爱挖坑，综合 ${fortune.overallScore} 分也别浪！大额、大话和大熬夜都先拦住；「${fortune.luckyColor}」是刹车贴纸，不是护身符。"
                    }
                ),
                MysticClarifierOption(
                    key = "timing",
                    label = "什么时候出手合适？",
                    answer = if (scholar) {
                        "综合 ${fortune.overallScore} 分说明今天适合分批推进。先用「${high.label}」 ${high.score} 开场，遇到「${low.label}」 ${low.score} 的环节放到状态好一点的时候。"
                    } else {
                        "什么时候出手？先看「${high.label}」 ${high.score} 什么时候在线！综合 ${fortune.overallScore} 分，别等黄道吉时等成搁浅；小事现在就能动。"
                    }
                )
            )
        }
        return MysticClarifier(title, options)
    }

    /** 客串退场后留给主角的精确回声；只有同一 mode/style family 的已知选择才有效。 */
    fun guestChoiceCarryover(mode: String, styleKey: String, choiceKey: String): String {
        return when (mode) {
            "scholar" -> if (styleKey !in listOf("archive", "harbor", "compass")) {
                ""
            } else {
                when (styleKey) {
                    "archive" -> when (choiceKey) {
                        "why" -> "客串退场后，档案页边多了一行：依据已被当面问过。"
                        "accept" -> "客串退场后，档案里夹了一张便签：提醒已经由你收下。"
                        "pushback" -> "客串退场后，档案合上半页：围观已经被你叫停。"
                        else -> ""
                    }
                    "harbor" -> when (choiceKey) {
                        "why" -> "水面安静下来，你把依据这件事稳稳放上了岸。"
                        "accept" -> "提醒被你先接住，泊位边少了一件悬着的事。"
                        "pushback" -> "你拦住了围拢的视线，水面重新留给你自己。"
                        else -> ""
                    }
                    else -> when (choiceKey) {
                        "why" -> "罗盘指针在「依据」一格停留过，来源核对已经发生。"
                        "accept" -> "罗盘旁留了一个小记号：那句提醒已被收进手边。"
                        "pushback" -> "罗盘让出中心位置，两人围观的状态已经解除。"
                        else -> ""
                    }
                }
            }
            "half" -> if (styleKey !in listOf("herald", "alley", "intern")) {
                ""
            } else {
                when (styleKey) {
                    "herald" -> when (choiceKey) {
                        "why" -> "锣鼓停了半拍：依据问题已经递到台前！"
                        "accept" -> "台侧记下一笔：提醒先被稳稳接住！"
                        "pushback" -> "幕布收窄半尺：两个人一起看戏的状态被你叫停！"
                        else -> ""
                    }
                    "alley" -> when (choiceKey) {
                        "why" -> "这茬摆在茶碗边上：依据你已经当面问过了。"
                        "accept" -> "提醒接住了，咱先把它放在顺手的地方。"
                        "pushback" -> "你一句话拦住了，俩人不再一起围着你看了。"
                        else -> ""
                    }
                    else -> when (choiceKey) {
                        "why" -> "工单状态更新为「依据已问过」。"
                        "accept" -> "工单状态更新为「提醒已接收」。"
                        "pushback" -> "工单状态更新为「围观已暂停」。"
                        else -> ""
                    }
                }
            }
            else -> ""
        }
    }

    fun guestReply(
        mode: String,
        topicKey: String,
        fortune: CompositeDailyFortune,
        rhythmKey: String = "",
        choiceKey: String = "why"
    ): String {
        if (fortune.dimensions.isEmpty()) return ""
        val choice = guestChoices().firstOrNull { it.key == choiceKey } ?: guestChoices().first()
        val source = listOf(
            canonicalDateKey(fortune.dateKey),
            "guest-answer",
            mode,
            topicKey,
            fortune.overallScore,
            fortune.luckyNumber,
            rhythmKey,
            choice.key
        ).joinToString("|")
        var hash = 937L
        for (char in source) {
            hash = (hash * 43L + char.code) % 2147483647L
        }

        val high = fortune.dimensions.maxByOrNull { it.score } ?: fortune.dimensions.first()
        val low = fortune.dimensions.minByOrNull { it.score } ?: fortune.dimensions.first()
        val focus = if (topicKey == "test") {
            high
        } else {
            fortune.dimensions.firstOrNull { it.key == topicKey }
                ?: fortune.dimensions.firstOrNull { it.key == "emotion" && topicKey == "love" }
                ?: fortune.dimensions.first()
        }
        val score = fortune.overallScore
        val useColor = score % 2 == 0
        val value = if (useColor) fortune.luckyColor else fortune.luckyDirection
        val sourceName = if (useColor) "幸运色" else "吉利方位"
        val rhythmNote = when (rhythmKey) {
            "steady" -> "你选的稳速还摆在桌上"
            "tired" -> "你说过的累也摆在桌上"
            "rushed" -> "你说的被催着走也摆在桌上"
            else -> "节奏先按刚才那页记着"
        }
        val scholarMain = mode != "half"
        val answer = if (scholarMain) {
            when {
                score >= 65 -> when (choice.key) {
                    "accept" -> "算你会接。「${focus.label}」 ${focus.score} 分先用在小处；${rhythmNote}，${sourceName}「$value」当个记号。"
                    "pushback" -> "本半仙退后半步；可「${focus.label}」 ${focus.score} 分摆在这儿，${rhythmNote}，你别装没看见。"
                    else -> "「${focus.label}」 ${focus.score} 是明面证据，「${high.label}」 ${high.score} 在后面撑着；${sourceName}「$value」只是路标，${rhythmNote}。"
                }
                score < 45 -> when (choice.key) {
                    "accept" -> "收下就行。「${low.label}」 ${low.score} 分先照顾一口饭、一觉觉；${rhythmNote}，${sourceName}「$value」不用背锅。"
                    "pushback" -> "我这就少说两句；但「${low.label}」 ${low.score} 分还在页面上，${rhythmNote}，先做最小一件。"
                    else -> "我看的是「${low.label}」 ${low.score} 分，它只说今天要省力；${rhythmNote}，${sourceName}「$value」不是判决。"
                }
                else -> when (choice.key) {
                    "accept" -> "稳稳接住就够。「${focus.label}」 ${focus.score} 分适合小步走；${rhythmNote}，${sourceName}「$value」当便签。"
                    "pushback" -> "行行行，我不围观点评；可「${low.label}」 ${low.score} 分得照看，${rhythmNote}，大戏改天再唱。"
                    else -> "「${focus.label}」 ${focus.score} 分是主线索：温吞不是坏事；「${high.label}」能借力，「${low.label}」要照顾，${rhythmNote}。"
                }
            }
        } else {
            when {
                score >= 65 -> when (choice.key) {
                    "accept" -> "好，那就轻轻收下。「${focus.label}」 ${focus.score} 分值得用一次小行动；${rhythmNote}，${sourceName}「$value」当提醒。"
                    "pushback" -> "我往旁边挪一步。「${high.label}」 ${high.score} 还亮着，「${low.label}」也要留口气；${rhythmNote}。"
                    else -> "主证据是「${focus.label}」 ${focus.score} 分，「${high.label}」 ${high.score} 在旁证；${sourceName}「$value」不是护身符，${rhythmNote}。"
                }
                score < 45 -> when (choice.key) {
                    "accept" -> "先收下这句：「${low.label}」 ${low.score} 分需要休息；${rhythmNote}，${sourceName}「$value」帮你停一下。"
                    "pushback" -> "好，我不多站了；但「${low.label}」 ${low.score} 分值得照看，${rhythmNote}，先吃饭睡觉。"
                    else -> "我看到的是「${low.label}」 ${low.score} 分；它只是状态页，不是结论。${sourceName}「$value」当暂停记号，${rhythmNote}。"
                }
                else -> when (choice.key) {
                    "accept" -> "收得很稳。「${focus.label}」 ${focus.score} 分不急不缓；${rhythmNote}，${sourceName}「$value」放在手边即可。"
                    "pushback" -> "我退到门边。「${high.label}」能搭把手，「${low.label}」 ${low.score} 别硬压；${rhythmNote}。"
                    else -> "我把线捋过了：「${focus.label}」 ${focus.score} 分；${sourceName}「$value」只作参照，${rhythmNote}。"
                }
            }
        }
        val lead = when ((hash / 19L) % 3L) {
            1L -> "我又看了一眼；"
            2L -> "按这一页说；"
            else -> ""
        }
        return lead + answer
    }

    fun guestHostWrapup(
        mode: String,
        styleKey: String,
        topicKey: String,
        fortune: CompositeDailyFortune,
        rhythmKey: String = "",
        choiceKey: String = "why"
    ): String {
        if (fortune.dimensions.isEmpty()) return ""
        val high = fortune.dimensions.maxByOrNull { it.score } ?: fortune.dimensions.first()
        val low = fortune.dimensions.minByOrNull { it.score } ?: fortune.dimensions.first()
        val focus = if (topicKey == "test") {
            high
        } else {
            fortune.dimensions.firstOrNull { it.key == topicKey }
                ?: fortune.dimensions.firstOrNull { it.key == "emotion" && topicKey == "love" }
                ?: fortune.dimensions.first()
        }
        val score = fortune.overallScore
        val useColor = score % 2 == 0
        val value = if (useColor) fortune.luckyColor else fortune.luckyDirection
        val sourceName = if (useColor) "幸运色" else "吉利方位"
        val rhythmNote = when (rhythmKey) {
            "steady" -> "稳速那栏先合上"
            "tired" -> "累的那栏先合上"
            "rushed" -> "被催那栏先合上"
            else -> "节奏栏先按刚才记着"
        }
        val scholar = mode != "half"
        val choiceNote = when (choiceKey) {
            "accept" -> "你接得住。"
            "pushback" -> "好，都退半步。"
            else -> "问得对。"
        }
        val body = if (scholar) {
            when (styleKey) {
                "harbor" -> when {
                    score >= 65 -> "客串的话我先接住。${focus.label}有 ${focus.score} 分，${low.label}也留着位置；${rhythmNote}。"
                    score < 45 -> "这里不用急着翻页。${low.label} ${low.score} 分先被看见，${rhythmNote}；${sourceName}「$value」放门口就好。"
                    else -> "灯还留着。${focus.label} ${focus.score} 分可以慢慢走，${rhythmNote}；${sourceName}「$value」只作提醒。"
                }
                "compass" -> when {
                    score >= 65 -> "方向没有变大，只是更清楚。${focus.label} ${focus.score} 分可用一小步验证；${rhythmNote}。"
                    score < 45 -> "先把针放慢。${low.label} ${low.score} 分需要照顾，${rhythmNote}；${sourceName}「$value」当暂停点。"
                    else -> "指针停在这里就够了。${focus.label} ${focus.score} 分宜整理，${rhythmNote}；${sourceName}「$value」留作参照。"
                }
                else -> when {
                    score >= 65 -> "我把客串那句夹进档案。${focus.label} ${focus.score} 分是入口，${low.label}做备注；${rhythmNote}。"
                    score < 45 -> "档案里补一行：${low.label} ${low.score} 分需要休息，不是定罪；${rhythmNote}。"
                    else -> "这一页归档为观察项。${focus.label} ${focus.score} 分先小步走；${rhythmNote}，${sourceName}「$value」当便签。"
                }
            }
        } else {
            when (styleKey) {
                "alley" -> when {
                    score >= 65 -> "茶先放下！${focus.label} ${focus.score} 分是真排面；${low.label}也带一口，${rhythmNote}。"
                    score < 45 -> "咱不唱衰。${low.label} ${low.score} 分先歇口气，${rhythmNote}；${sourceName}「$value」压在杯底当提醒。"
                    else -> "街口风不大。${focus.label} ${focus.score} 分慢慢晃过去就行，${rhythmNote}；${sourceName}「$value」顺手看一眼。"
                }
                "intern" -> when {
                    score >= 65 -> "工单备注：${focus.label} ${focus.score} 分可用在一件小事上；${low.label}另开一栏，${rhythmNote}。"
                    score < 45 -> "工单已降速：${low.label} ${low.score} 分优先休息；${rhythmNote}，${sourceName}「$value」设成暂停标签。"
                    else -> "云端记录：${focus.label} ${focus.score} 分保持小步推进；${rhythmNote}，${sourceName}「$value」仅作提示。"
                }
                else -> when {
                    score >= 65 -> "锣鼓停半拍！${focus.label} ${focus.score} 分确实亮眼；给${low.label}留口气，${rhythmNote}。"
                    score < 45 -> "场务别催！${low.label} ${low.score} 分先回血，${rhythmNote}；${sourceName}「$value」只是台侧暗号。"
                    else -> "今日戏码平稳。${focus.label} ${focus.score} 分按小段演，${rhythmNote}；${sourceName}「$value」当道具提示。"
                }
            }
        }
        return choiceNote + body
    }

    private fun rhythmPrompt(scholar: Boolean, styleKey: String, topicKey: String): String {
        val topic = topicLabel(topicKey)
        return if (scholar) {
            when (styleKey) {
                "archive" -> "${topic}档案旁多了一栏状态；今天你选哪一档？"
                "harbor" -> "灯先留着；${topic}之外，你的节奏是哪一种？"
                else -> "盘面归位了；先标一下你今天的速度。"
            }
        } else {
            when (styleKey) {
                "herald" -> "开场登记补充项！今天的节奏档位报一个！"
                "alley" -> "先别急着上茶；今天你是稳、累还是赶？"
                else -> "云端表单新增一行：今日节奏选哪个？"
            }
        }
    }

    private fun rhythmSteadyResponse(
        scholar: Boolean,
        styleKey: String,
        topicKey: String,
        label: String,
        score: Int,
        switchSource: String,
        switchValue: String
    ): String {
        val topic = topicLabel(topicKey)
        return if (scholar) {
            when (styleKey) {
                "archive" -> "${topic}档案收到「稳稳推进」。最强项是「$label」，$score 分；${switchSource}「${switchValue}」可以当作提醒。"
                "harbor" -> "稳着来很好。「$label」现在有 $score 分；${switchSource}「${switchValue}」放在顺手处就好。"
                else -> "${topic}的速度标成稳档。「$label」 $score 分，${switchSource}「${switchValue}」只作小路标。"
            }
        } else {
            when (styleKey) {
                "herald" -> "稳速档批准！「$label」 $score 分；${switchSource}「${switchValue}」小旗已举起！"
                "alley" -> "行，稳住就行。「$label」有 $score 分；${switchSource}「${switchValue}」压在茶杯边。"
                else -> "云端备注：稳速推进。「$label」 $score 分，${switchSource}「${switchValue}」便签已贴好！"
            }
        }
    }

    private fun rhythmTiredResponse(
        scholar: Boolean,
        styleKey: String,
        topicKey: String,
        label: String,
        score: Int,
        caution: String
    ): String {
        val topic = topicLabel(topicKey)
        val cautionLine = if (caution.isEmpty()) "" else if (scholar) " 盘面提醒：${caution}。" else " 小黑板写着：${caution}！"
        return if (scholar) {
            when (styleKey) {
                "archive" -> "${topic}档案记下「有点累」。最需照看的是「$label」，$score 分。${cautionLine}先做十分钟最小的一步。"
                "harbor" -> "累了就先承认这件事。「$label」 $score 分。${cautionLine}先做十分钟最小的一步。"
                else -> "速度降一档也没关系。「$label」 $score 分。${cautionLine}先做十分钟最小的一步。"
            }
        } else {
            when (styleKey) {
                "herald" -> "低电量档登记！「$label」只有 $score 分。${cautionLine}先回血，锣鼓调小声！"
                "alley" -> "累就直说，挺好。「$label」才 $score 分。${cautionLine}先回血，别硬扛！"
                else -> "云端状态：需要休息。「$label」 $score 分。${cautionLine}先回血十分钟！"
            }
        }
    }

    private fun rhythmRushedResponse(
        scholar: Boolean,
        styleKey: String,
        topicKey: String,
        caution: String,
        switchSource: String,
        switchValue: String
    ): String {
        val topic = topicLabel(topicKey)
        val cautionLine = if (caution.isEmpty()) "" else if (scholar) " 盘面提醒：${caution}。" else " 小黑板写着：${caution}！"
        return if (scholar) {
            when (styleKey) {
                "archive" -> "${topic}这一页被催出了折角。${cautionLine}清单先砍成一步；${switchSource}「${switchValue}」用来换挡。"
                "harbor" -> "被赶着走时，先给自己留个泊位。${cautionLine}清单砍成一步；${switchSource}「${switchValue}」帮你换气。"
                else -> "急速指针需要慢半拍。${cautionLine}把清单砍成一步；${switchSource}「${switchValue}」当换挡提醒。"
            }
        } else {
            when (styleKey) {
                "herald" -> "急档收到！场务都别催了！${cautionLine}先刹车三分钟；${switchSource}「${switchValue}」便签送上！"
                "alley" -> "谁把你催成这样？${cautionLine}先刹车三分钟；${switchSource}「${switchValue}」压在清单上面。"
                else -> "云端提示：速度过载。${cautionLine}刹车三分钟；${switchSource}「${switchValue}」便签已弹出！"
            }
        }
    }

    private fun openingPrompt(scholar: Boolean, styleKey: String, topicKey: String): String {
        val topic = topicLabel(topicKey)
        return if (scholar) {
            when (styleKey) {
                "archive" -> when (topicKey) {
                    "composite" -> "综合档案已摊平，先用哪一条做今天的书签？"
                    "career" -> "事业卷宗有一处折角，你想从哪里核对？"
                    "love" -> "感情这页写着具体的事；先翻哪一行？"
                    "wealth" -> "钱袋账册摆在右手边，先看哪一栏？"
                    "study" -> "学习笔记还留着一页空白，先补哪里？"
                    "health" -> "身体档案不催促人；先记录哪一项？"
                    else -> "最近测试只是材料；先取哪一面镜子？"
                }
                "harbor" -> when (topicKey) {
                    "composite" -> "灯亮了，${topic}这件事想先从哪头靠岸？"
                    "career" -> "事业的潮水不急着赶；你想先卸下哪一件？"
                    "love" -> "感情这片水面很安静；先说哪一句？"
                    "wealth" -> "钱袋的小船系着呢；先看哪里吃水？"
                    "study" -> "学习像整理行囊；先放下哪本书？"
                    "health" -> "身体也需要泊位；先让它歇在哪一处？"
                    else -> "测试结果不是判决；先坐下来照哪一面？"
                }
                else -> when (topicKey) {
                    "composite" -> "${topic}盘面已经归位；罗盘先对哪一格？"
                    "career" -> "事业方向有几条并排；先确认哪条小路？"
                    "love" -> "感情指针很轻；先停在哪个词上？"
                    "wealth" -> "钱袋路线可以慢走；先核哪一个路标？"
                    "study" -> "学习是一格一格推进；先转哪一页？"
                    "health" -> "身体坐标值得看清；先校准哪一项？"
                    else -> "测试材料已经编号；先读哪一段注脚？"
                }
            }
        } else {
            when (styleKey) {
                "herald" -> when (topicKey) {
                    "composite" -> "锣鼓轻一点！${topic}开场签到，先递哪张名帖？"
                    "career" -> "事业大幕拉开一条缝；先报哪个节目？"
                    "love" -> "感情舞台不打追光；先点哪盏小灯？"
                    "wealth" -> "钱袋账本已呈上来；先翻哪页奏折？"
                    "study" -> "文昌香火已备好；先点哪炷？"
                    "health" -> "仙体保养司就位；先验哪件行李？"
                    else -> "测试榜单暂不宣读；先挑哪面镜子？"
                }
                "alley" -> when (topicKey) {
                    "composite" -> "大碗茶放好了；${topic}这摊先唠哪句？"
                    "career" -> "班还是得上；先把哪件事摆上桌？"
                    "love" -> "感情这事不猜谜；先从哪句实话开始？"
                    "wealth" -> "钱包不用晒；先看哪个口子？"
                    "study" -> "书山有近道也有远路；先迈哪步？"
                    "health" -> "神仙也怕硬熬；先顾哪一块？"
                    else -> "测试单别吓自己；先拿哪面照照？"
                }
                else -> when (topicKey) {
                    "composite" -> "云端签到页打开啦；${topic}先勾哪个框？"
                    "career" -> "事业工单已建号；先处理哪条备注？"
                    "love" -> "感情信号稳定；先发送哪句草稿？"
                    "wealth" -> "钱包云账本同步中；先核对哪一笔？"
                    "study" -> "学习进度条不催人；先点亮哪格？"
                    "health" -> "仙体巡检开始啦；先贴哪张便签？"
                    else -> "测试报告已脱敏；先展开哪段摘要？"
                }
            }
        }
    }

    private fun openingResponse(
        scholar: Boolean,
        styleKey: String,
        topicKey: String,
        strength: Boolean,
        label: String,
        score: Int
    ): String {
        val topic = topicLabel(topicKey)
        val position = if (strength) "较强" else "较需照看"
        return if (scholar) {
            when (styleKey) {
                "archive" -> "${topic}档案里，「$label」$position（$score 分）。先把它当作参照，不改结论，也不急着定义今天。"
                "harbor" -> "「$label」现在$position，$score 分。我先把这句话放在桌上；它值得被慢慢说清。"
                else -> "${topic}的「$label」$position（$score 分）。罗盘只标这个位置，下一步仍由你选。"
            }
        } else {
            when (styleKey) {
                "herald" -> "报——「$label」$position，$score 分！这不是判决，只是今天${topic}的开场字幕！"
                "alley" -> "「$label」$position，$score 分。咱把话摊开：它能当线索，不能替你过日子。"
                else -> "${topic}工单显示「$label」$position（$score 分）。已登记！用法说明：观察优先，不吹法术。"
            }
        }
    }

    private fun openingSwitchResponse(
        scholar: Boolean,
        styleKey: String,
        topicKey: String,
        source: String,
        value: String
    ): String {
        val topic = topicLabel(topicKey)
        return if (scholar) {
            when (styleKey) {
                "archive" -> "$source「$value」可以夹进${topic}那一页；它适合当提醒，不适合当保证。"
                "harbor" -> "把$source「$value」放在顺手的地方；${topic}需要时，让它帮你想起歇一口气。"
                else -> "$source「$value」先当作${topic}的小路标；走到哪儿、歇多久，都由你定。"
            }
        } else {
            when (styleKey) {
                "herald" -> "$source「$value」已盖章！${topic}专用提醒送达，但它不包办结局！"
                "alley" -> "$source「$value」给你压在茶杯底下；${topic}忙起来时看一眼就行，别迷信。"
                else -> "$source「$value」已写进${topic}便签！功能只有一条：提醒你回来照顾自己。"
            }
        }
    }

    /** 作风仍按话题微调：同一个人换到不同话题，会有不同的讲解姿势。 */
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

    /** 盘面定制局：只用已有分数和幸运开关做轻量实验提示，不新增命运判决。 */
    private fun contextualGames(
        scholar: Boolean,
        fortune: CompositeDailyFortune
    ): List<MysticInteraction> {
        val high = fortune.dimensions.maxByOrNull { it.score } ?: fortune.dimensions.first()
        val low = fortune.dimensions.minByOrNull { it.score } ?: fortune.dimensions.first()
        return if (scholar) {
            listOf(
                MysticInteraction(
                    title = "强弱接力",
                    description = "今天「${high.label}」 ${high.score} 分，「${low.label}」 ${low.score} 分。选一个衔接方式。",
                    options = listOf(
                        MysticInteractionOption(
                            "用强项带一带",
                            "好。「${high.label}」的 ${high.score} 分不是拿来炫耀的，拿它给「${low.label}」开个头。"
                        ),
                        MysticInteractionOption(
                            "先照顾弱项",
                            "对，${low.score} 分只需要一个小动作；别逼它今天变成满分。"
                        ),
                        MysticInteractionOption("只观察不改动", "可以，记录本身就是校准；明天的对照会更清楚。")
                    )
                ),
                MysticInteraction(
                    title = "开关实验",
                    description = "幸运开关是「${fortune.luckyColor}」和「${fortune.luckyDirection}」。选一个轻量实验。",
                    options = listOf(
                        MysticInteractionOption(
                            "带上幸运色",
                            "把它当作提醒，不是护身符：看到颜色就回到那件小事。"
                        ),
                        MysticInteractionOption(
                            "顺吉利方向走走",
                            "如果顺路就走一段；重点是换气，不是改命。"
                        ),
                        MysticInteractionOption(
                            "定一个十分钟提醒",
                            "十分钟后回看「${low.label}」，只问一句：现在能做的最小步是什么？"
                        )
                    )
                )
            )
        } else {
            listOf(
                MysticInteraction(
                    title = "仙家调配室",
                    description = "「${high.label}」举着 ${high.score} 分，「${low.label}」只有 ${low.score} 分！选个调法。",
                    options = listOf(
                        MysticInteractionOption(
                            "抽高项借火力",
                            "批准借用！把「${high.label}」的劲头挪一点去暖「${low.label}」。"
                        ),
                        MysticInteractionOption(
                            "给低项加云朵棉",
                            "安排！软处理不丢人，${low.score} 分也能慢慢爬坡。"
                        ),
                        MysticInteractionOption("先盖休息章", "章已盖好！神仙也得充电，别拿硬撑当法术。")
                    )
                ),
                MysticInteraction(
                    title = "幸运快递·定制",
                    description = "今日包裹按盘面打包：${fortune.luckyColor}、${fortune.luckyDirection}，签一样！",
                    options = listOf(
                        MysticInteractionOption(
                            "${fortune.luckyColor}便签",
                            "签收！上面写着：颜色只是开关，真正动手的还是你。"
                        ),
                        MysticInteractionOption(
                            "${fortune.luckyDirection}绕路券",
                            "券已生效！顺路就绕一小段，不顺路就原地做小事。"
                        ),
                        MysticInteractionOption(
                            "${high.label}试用装",
                            "发货啦！先用一小时，别贪多；用完写一句哪里顺手。"
                        )
                    )
                )
            )
        }
    }

    /** 小互动也走稳定取样；“再来一局”通过 round 换到下一个可选局面。 */
    fun interaction(
        mode: String,
        topicKey: String,
        fortune: CompositeDailyFortune,
        round: Int,
        skinId: String = ""
    ): MysticInteraction {
        val scholar = mode != "half"
        val staticGames = if (scholar) {
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
                ),
                MysticInteraction(
                    title = "三格沙盘",
                    description = "把眼前的事分成启动、等待、收尾三格。",
                    options = listOf(
                        MysticInteractionOption("放入启动格", "好，就给它一个明确的开始时间；别让准备变成新任务。"),
                        MysticInteractionOption("放入等待格", "写清在等谁、等到什么时候；悬着的事有了坐标会轻很多。"),
                        MysticInteractionOption("放入收尾格", "收尾最容易被忘掉。补上最后一步，才算真正腾出手。")
                    )
                ),
                MysticInteraction(
                    title = "回声提问",
                    description = "挑一句今天最常在心里响起的话。",
                    options = listOf(
                        MysticInteractionOption("“我必须快点”", "先把“快”换成“下一步”。速度是结果，不必硬逼成前提。"),
                        MysticInteractionOption("“我还没准备好”", "那就列出最小装备。准备好不是感觉，是能拿出来的东西。"),
                        MysticInteractionOption("“别人会怎么看”", "把观众缩小到一个真正相关的人；他的具体需求更值得听。")
                    )
                ),
                MysticInteraction(
                    title = "边界清点",
                    description = "为今天的注意力画一条干净的边界。",
                    options = listOf(
                        MysticInteractionOption("保留一件事", "对，主线只留一件；其余的事可以排队，不用同时喊话。"),
                        MysticInteractionOption("延后一件事", "延后要落到时间点。这样它不会被丢掉，也不会一直追着你。"),
                        MysticInteractionOption("拒绝一件事", "拒绝可以很轻：“这次先不接。”留出的空间就是今天的余地。")
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
                ),
                MysticInteraction(
                    title = "仙气盲盒",
                    description = "本半仙摇了摇云柜，先摸一个状态！",
                    options = listOf(
                        MysticInteractionOption("摸到小锣鼓", "开场锣响！先做五分钟，别等仙气排面齐了才动。"),
                        MysticInteractionOption("摸到云朵毯", "充电令已下！歇十分钟，起来时只带一件事回去。"),
                        MysticInteractionOption("摸到小算盘", "算盘说了：把大目标切成三小块，神仙也怕一口吞桌。")
                    )
                ),
                MysticInteraction(
                    title = "云上账房",
                    description = "报出今天最占心思的一笔，账房先生帮你分账！",
                    options = listOf(
                        MysticInteractionOption("记成精力账", "入账！先问它给你耗多少、回多少；亏本的事要设个止损点。"),
                        MysticInteractionOption("记成时间账", "记账成功！给它一个钟点，别让这件事偷偷包场。"),
                        MysticInteractionOption("记成人情账", "记下了！该说清楚就说清楚，人情不能全靠猜谜维持。")
                    )
                ),
                MysticInteraction(
                    title = "小道消息分拣",
                    description = "云外传来三条消息，你只能带走一条！",
                    options = listOf(
                        MysticInteractionOption("带走“先落地”", "这条灵！想法落成一个动作，比再想十遍都有仙效。"),
                        MysticInteractionOption("带走“别贪多”", "盖章！三个开头不如一个完成；剩下的先封进云罐。"),
                        MysticInteractionOption("带走“留证据”", "妙啊！做完随手记一笔，回头就不用靠仙忆硬猜。")
                    )
                )
            )
        }
        val knownSkin = mysticSkinVoice(mode, skinId)
        val games = orderedInteractionGames(staticGames + contextualGames(scholar, fortune), skinId)
        val seed = interactionSeed(mode, topicKey, fortune, 0, knownSkin?.id.orEmpty())
        val safeRound = round.coerceAtLeast(0)

        // 固定互质步长让每次换局都换内容，且完整轮转后才回到起点。
        var picked = ((seed / 19L) % games.size).toInt()
        var stride = (seed % (games.size - 1L)).toInt() + 1
        while (greatestCommonDivisor(stride, games.size) != 1) {
            stride = (stride % (games.size - 1)) + 1
        }
        repeat(safeRound) {
            picked = (picked + stride) % games.size
        }
        val pickedGame = games[picked]
        return if (knownSkin == null) {
            pickedGame
        } else {
            pickedGame.copy(description = "${knownSkin.gameLead}${pickedGame.description}")
        }
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
    fun interactionReaction(mode: String, styleKey: String, skinId: String = ""): String {
        val scholar = mode != "half"
        val tail = mysticSkinVoice(mode, skinId)?.let { " ${it.reactionTail}" } ?: ""
        return if (scholar) {
            when (styleKey) {
                "archive" -> "我先把这项记在旁边；做法比说法更重要。$tail"
                "harbor" -> "好，你选的这一步我已经接住了。$tail"
                else -> "这个方向够小，适合真的走一步。$tail"
            }
        } else {
            when (styleKey) {
                "herald" -> "恭喜！这条选择已经敲锣送进云海！$tail"
                "alley" -> "行，就按这个来；咱不整虚的。$tail"
                else -> "已登记工单！实习生保证不把它弄丢。$tail"
            }
        }
    }

    fun topicLabel(key: String): String = topics[key] ?: "综合"

    /** 供界面校验异步回应仍属于当前话题与作风；结果完全由既有盘面输入决定。 */
    fun styleKeyFor(mode: String, topicKey: String, fortune: CompositeDailyFortune): String =
        style(mode != "half", topicKey, fortune).second

    /** 等待行也带作风，让“正在处理”像一个人手上的小动作。 */
    fun thinkingLine(mode: String, styleKey: String, kind: String): String {
        val scholar = mode != "half"
        return if (scholar) {
            when (styleKey) {
                "archive" -> when (kind) {
                    "game" -> "正在把你的选择抄进档案"
                    "handoff" -> "正在把上一页夹好"
                    "opening" -> "正在给签到句找位置"
                    "rhythm" -> "正在把节奏栏补上"
                    else -> "正在翻对应的那一页"
                }
                "harbor" -> when (kind) {
                    "game" -> "正在看你选出的那一步"
                    "handoff" -> "正在给话题换个坐姿"
                    "opening" -> "正在接住开场那句"
                    "rhythm" -> "正在接住你的节奏"
                    else -> "先接住这句话"
                }
                else -> when (kind) {
                    "game" -> "正在核对最小一步"
                    "handoff" -> "罗盘准备只转一格"
                    "opening" -> "罗盘正在对准入口"
                    "rhythm" -> "指针正按你的速度调整"
                    else -> "指针正在慢慢对齐"
                }
            }
        } else {
            when (styleKey) {
                "herald" -> when (kind) {
                    "game" -> "锣鼓小队正在验票"
                    "handoff" -> "换场锣鼓正在调音"
                    "opening" -> "开场名帖正在登记"
                    "rhythm" -> "节奏档位正在登记"
                    else -> "天庭司仪正翻到那一页"
                }
                "alley" -> when (kind) {
                    "game" -> "半仙正在给你递签"
                    "handoff" -> "大碗茶先挪个位置"
                    "opening" -> "开场这茬正摆上桌"
                    "rhythm" -> "半仙正在掂量你的劲儿"
                    else -> "街口半仙正在打听"
                }
                else -> when (kind) {
                    "game" -> "云上工单正在登记"
                    "handoff" -> "云端工单正在改派"
                    "opening" -> "签到表单提交中"
                    "rhythm" -> "节奏表单提交中"
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

    /** 自由提问先被“听见”；同一个人对同一句话的态度稳定，但不同问法不会都像模板。 */
    fun customReaction(mode: String, styleKey: String, question: String): String {
        val scholar = mode != "half"
        val variant = (customPulse(question) % 2L).toInt()
        return if (scholar) {
            when (styleKey) {
                "archive" -> if (variant == 0) "我把这句话放在这一页旁边看。" else "先留住你的原话，再对照盘面。"
                "harbor" -> if (variant == 0) "这句话我接住了；我们慢慢拆。" else "嗯，这里可以不用急着要答案。"
                else -> if (variant == 0) "问题收到了；罗盘只按这一句转。" else "先把范围收窄，再看它指向哪里。"
            }
        } else {
            when (styleKey) {
                "herald" -> if (variant == 0) "锣鼓轻一点，这句我听清了！" else "好胆量！当面问得这么直接！"
                "alley" -> if (variant == 0) "哟，这话够直；大碗茶先放下。" else "行，咱不绕弯子，直接看这摊。"
                else -> if (variant == 0) "工单已登记！实习生不弄丢这句。" else "收到收到！法术加载中，态度拉满！"
            }
        }
    }

    /** 自由提问仍回到真实盘面：分数、强弱项、幸运开关和注意事项都是已有计算结果。 */
    fun customAnswer(
        mode: String,
        topicKey: String,
        question: String,
        fortune: CompositeDailyFortune,
        test: TestRecord? = null
    ): String {
        val label = topics[topicKey] ?: "综合"
        val focus = if (topicKey == "test") {
            fortune.dimensions.maxByOrNull { it.score } ?: fortune.dimensions.first()
        } else {
            fortune.dimensions.firstOrNull { it.key == topicKey }
                ?: fortune.dimensions.firstOrNull { it.key == "emotion" && topicKey == "love" }
                ?: fortune.dimensions.first()
        }
        val high = fortune.dimensions.maxByOrNull { it.score } ?: focus
        val low = fortune.dimensions.minByOrNull { it.score } ?: focus
        val intent = customIntent(question)
        val scholar = mode != "half"
        val cleanCautions = fortune.cautions.replace("\n", " ")
        return when (intent) {
            "mood" -> if (scholar) {
                "今天综合 ${fortune.overallScore} 分，最需要照看的是「${low.label}」 ${low.score} 分。" +
                    "先把睡眠、吃饭和一件最小的事安排好；情绪紧的时候，判断可以晚一点再做。"
            } else {
                "综合 ${fortune.overallScore} 分，「${low.label}」只有 ${low.score} 分，仙界都不催你现在硬撑！" +
                    "先喝口热的、歇十分钟，再把最麻烦的事切成一小块。"
            }
            "love" -> {
                val dim = fortune.dimensions.firstOrNull { it.key == if (question.contains("桃花")) "peach" else "emotion" } ?: focus
                if (scholar) {
                    "「${dim.label}」当前 ${dim.score} 分。比起猜结果，今天更适合把想说的一件事说清楚；" +
                        "关系里的安全感来自具体表达，不是反复试探。"
                } else {
                    "「${dim.label}」 ${dim.score} 分！别把话筒扔给对方猜，想要什么直接讲；" +
                        "暧昧让神仙算账都费劲，直球省电！"
                }
            }
            "wealth" -> {
                val dim = fortune.dimensions.firstOrNull { it.key == "wealth" } ?: focus
                if (scholar) {
                    "「财富」 ${dim.score} 分。今天优先守住必要支出；若要尝试，金额小到失败也不影响生活。" +
                        "幸运色「${fortune.luckyColor}」可以当作提醒自己冷静消费的开关。"
                } else {
                    "财库信号 ${dim.score} 分！小额快乐可以投喂，大额冲动先冷冻三天；" +
                        "往「${fortune.luckyDirection}」挪一挪，不如先打开记账本！"
                }
            }
            "career" -> {
                val dim = fortune.dimensions.firstOrNull { it.key == "career" } ?: focus
                if (scholar) {
                    "「事业」 ${dim.score} 分。今天挑一件最重要的事推进；沟通时把需求、时间和需要的支持说清楚，" +
                        "比同时开五个头更能建立可信度。"
                } else {
                    "事业炉火 ${dim.score} 分！主打一招，别十八般武艺一起抡；" +
                        "把关键话说漂亮，胜过加班到冒烟。"
                }
            }
            "study" -> {
                val dim = fortune.dimensions.firstOrNull { it.key == "study" } ?: focus
                if (scholar) {
                    "「学习」 ${dim.score} 分。把它切成二十五分钟的小段：先回顾一次，再处理最难的一块；" +
                        "完成比完美更容易带走停滞感。"
                } else {
                    "文昌香火 ${dim.score} 分！番茄钟启动，先把最烦的那块啃一小口；" +
                        "成就感会自动续杯，别靠焦虑续命。"
                }
            }
            "health" -> {
                val dim = fortune.dimensions.firstOrNull { it.key == "health" } ?: focus
                if (scholar) {
                    "「健康」 ${dim.score} 分。优先睡眠、饮食和活动量；身体信号值得认真对待，" +
                        "持续不舒服时请优先休息或寻求专业帮助。"
                } else {
                    "健康炉温 ${dim.score} 分！早点躺、好好吃、动一动；" +
                        "别和沙发签永久契约，真不舒服也别硬撑成苦瓜。"
                }
            }
            "why" -> if (scholar) {
                "你问的这句落在「${label}」 ${focus.score} 分；全天综合 ${fortune.overallScore} 分。" +
                    "最强是「${high.label}」 ${high.score}，最需照看是「${low.label}」 ${low.score}。这是现有盘面算法的参照，不是命运判决。"
            } else {
                "别急，本半仙把账摊开：「${label}」 ${focus.score} 分，综合 ${fortune.overallScore} 分！" +
                    "「${high.label}」举火把，「${low.label}」坐轿子；数字来自既有算法，不是拍脑袋。"
            }
            "care" -> careAnswer(scholar, low.label, cleanCautions)
            "outcome" -> if (scholar) {
                "我不替未来盖章。当前能看见的是：「${high.label}」 ${high.score} 可用，「${low.label}」 ${low.score} 要照看。" +
                    "把可控的一步做完，结果会比空等更清楚。"
            } else {
                "天机不打包票，打包票的都是卖符的！不过「${high.label}」 ${high.score} 在线，" +
                    "「${low.label}」 ${low.score} 别硬闯；先做小事，再谈成不成。"
            }
            "action" -> actionAnswer(scholar, high.label, low.label, fortune.luckyColor, fortune.luckyDirection)
            else -> topicAnswer(
                scholar,
                topicKey,
                label,
                focus.score,
                high.label,
                low.label,
                test?.testName.orEmpty()
            )
        }
    }

    private fun customIntent(question: String): String {
        val q = question.trim().lowercase()
        return when {
            listOf("焦虑", "压力", "害怕", "担心", "难过", "崩溃", "很累", "内耗").any { q.contains(it) } -> "mood"
            listOf("感情", "恋爱", "对象", "复合", "暗恋", "表白", "桃花", "分手", "他", "她").any { q.contains(it) } -> "love"
            listOf("财", "钱", "赚钱", "投资", "生意", "消费", "钱包").any { q.contains(it) } -> "wealth"
            listOf("工作", "上班", "事业", "老板", "同事", "面试", "升职", "跳槽").any { q.contains(it) } -> "career"
            listOf("学习", "考试", "复习", "作业", "论文", "背", "题").any { q.contains(it) } -> "study"
            listOf("健康", "身体", "睡觉", "睡眠", "失眠", "生病", "累").any { q.contains(it) } -> "health"
            listOf("为什么", "怎么来", "怎么算", "依据", "来源", "多少分").any { q.contains(it) } -> "why"
            listOf("留意", "注意", "风险", "小心", "避免", "坑").any { q.contains(it) } -> "care"
            listOf("能不能", "会不会", "可不可以", "行不行", "成不成", "该不该").any { q.contains(it) } -> "outcome"
            listOf("什么时候", "几点", "哪天", "现在适合", "今天适合").any { q.contains(it) } -> "action"
            listOf("怎么做", "怎么办", "如何", "建议", "行动", "开始", "计划", "破", "解").any { q.contains(it) } -> "action"
            else -> "topic"
        }
    }

    private fun customPulse(value: String): Long {
        var hash = 5381L
        for (char in value.trim()) {
            hash = (hash * 33L + char.code) % 2147483647L
        }
        return hash
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

    private fun companionSeed(fortune: CompositeDailyFortune): Long {
        val source = "${canonicalDateKey(fortune.dateKey)}|${fortune.overallScore}|${fortune.luckyNumber}"
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
        round: Int,
        skinId: String = ""
    ): Long {
        val safeRound = round.coerceAtLeast(0)
        val skinSuffix = mysticSkinVoice(mode, skinId)?.let { "|${it.id}" } ?: ""
        val source = "${canonicalDateKey(fortune.dateKey)}|$mode|$topicKey|${fortune.overallScore}|$safeRound$skinSuffix"
        var hash = 52711L
        for (char in source) {
            hash = (hash * 37L + char.code) % 2147483647L
        }
        return hash
    }

    private fun greatestCommonDivisor(a: Int, b: Int): Int {
        var left = a
        var right = b
        while (right != 0) {
            val next = left % right
            left = right
            right = next
        }
        return left
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
