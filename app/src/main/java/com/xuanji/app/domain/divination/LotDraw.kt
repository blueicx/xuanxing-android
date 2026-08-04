package com.xuanji.app.domain.divination

import java.time.LocalDate

/**
 * 抽签式占卜集合（9 系统）：
 * 中国灵签、藏传签卜、泰国暹罗签、古希腊神谕、圣经掣签、
 * 非洲贝壳占卜、翻书占卜、水占神签、日本御神签。
 * 原代码用 random 抽取，本实现全部改为「日期种子」确定性——当日结果固定，离线可复现。
 */
object LotDraw {

    enum class LotSystem(val key: String, val label: String) {
        ChineseKauCim("chinese_kau_cim", "中国灵签（观音）"),
        TibetanDivination("tibetan_div", "藏传佛教签卜"),
        ThaiSiamCee("thai_siam", "泰国暹罗签"),
        GreekOracle("greek_oracle", "古希腊神谕"),
        BibleLot("bible_lot", "《圣经》掣签"),
        CowrieShell("cowrie", "非洲贝壳占卜"),
        Bibliomancy("bibliomancy", "翻书占卜"),
        MizuKuji("mizu_kuji", "水占卜神签"),
        Omikuji("omikuji", "日本御神签")
    }

    // ---------- 1. 中国灵签 ----------
    private data class KauCim(val grade: String, val title: String, val poem: String, val meaning: String, val advice: String)
    private val KAU_CIM = listOf(
        KauCim("上上", "飞龙在天", "飞龙在天，利见大人。英雄得志，福禄自臻。", "大吉大利，事业有成，贵人相助，前程似锦。", "宜积极进取，把握时机，但需谦虚待人。"),
        KauCim("上吉", "春景融和", "春景融和，万象更新。花开富贵，月满华庭。", "好运到来，事事顺利，家庭和睦，喜庆临门。", "宜保持乐观心态，趁势而为。"),
        KauCim("中吉", "平步青云", "平步青云，志向可期。功名遂愿，福寿齐眉。", "努力将获得回报，事业渐入佳境，健康平稳。", "坚持努力，勿骄勿躁。"),
        KauCim("中平", "待时而动", "待时而动，不可妄为。静心守志，终见光辉。", "时机未成熟，宜耐心等待，不宜冒进。", "沉淀心志，加强学习，等待机会。"),
        KauCim("下下", "枯木逢春", "枯木逢春，花开有期。但行好事，莫问前程。", "困境中见转机，需主动行善积德，扭转运势。", "保持希望，从善如流，逆境终将过去。"),
        KauCim("上吉", "锦上添花", "锦上添花，福星高照。家宅安宁，财源广进。", "好运继续，可能获得意外之财或喜事。", "保持感恩，乐于助人，福气更旺。"),
        KauCim("中吉", "安居乐业", "安居乐业，心定神宁。所求有得，所行有成。", "生活安稳，工作和家庭和谐，小有收获。", "知足常乐，稳健发展。"),
        KauCim("凶", "风雨交加", "风雨交加，行舟艰难。谨慎守成，免生祸端。", "当前运势不佳，需谨言慎行，防范小人。", "保守为主，避免冲突和冒险。"),
        KauCim("上上", "凤鸣高岗", "凤鸣高岗，祥瑞降临。功业成就，万福咸臻。", "事业将达到高峰，名声远播，福气盈门。", "巩固成果，谦逊待人，可保长久。"),
        KauCim("下吉", "暗中寻路", "暗中寻路，渐见光明。努力不懈，终获安宁。", "虽有迷茫，但方向渐明，需坚持。", "保持信心，不断探索，曙光在前。"),
        KauCim("大吉", "日月同辉", "日月同辉，吉星高照。万事如意，福寿绵长。", "极佳运势，心想事成，家运昌隆。", "珍惜好运，多行善事，福泽绵长。"),
        KauCim("平", "顺水行舟", "顺水行舟，自然顺畅。但凭心志，可达远方。", "运势平稳，一切按计划进行，无需焦虑。", "保持稳定，按部就班即可。")
    )

    // ---------- 2. 藏传签卜 ----------
    private data class TibetanSign(val grade: String, val title: String, val meaning: String, val advice: String)
    private val TIBETAN = listOf(
        TibetanSign("上善", "莲花盛开", "心地清净，善缘成熟，所愿皆成。", "保持慈悲心，修行精进，利他自利。"),
        TibetanSign("中平", "雪山流水", "事情平缓进行，需持之以恒，不急不躁。", "培养耐心，脚踏实地，终有成就。"),
        TibetanSign("凶险", "狂风骤雨", "障碍显现，需谨慎行事，以忏悔和祈祷化解。", "多做功德，持诵经咒，回向众生。"),
        TibetanSign("上吉", "金刚铠甲", "护佑强大，坚固不动，战胜困难。", "坚定信念，勇猛精进，诸魔不侵。"),
        TibetanSign("下劣", "孤鸟失群", "孤立无援，易生误解，宜独自修持。", "静心独处，反省自身，慎言慎行。"),
        TibetanSign("大吉", "佛光普照", "智慧现前，福慧双增，一切障碍消除。", "珍惜此缘，广行布施，利益众生。")
    )

    // ---------- 3. 泰国暹罗签 ----------
    private data class SiamSign(val grade: String, val title: String, val overall: String, val work: String, val love: String, val health: String, val advice: String)
    private val SIAM = listOf(
        SiamSign("ดีมาก（很好）", "金翅鸟", "运势极佳，心想事成，有贵人相助。", "工作顺利，有望升迁。", "感情甜蜜，桃花旺盛。", "身体健康，精力充沛。", "把握机会，但勿骄傲。"),
        SiamSign("ดี（好）", "白象", "好运相随，宜积极行动。", "事业稳定，可拓展业务。", "关系和谐，互相理解。", "注意饮食，无大碍。", "保持现状，继续努力。"),
        SiamSign("ปานกลาง（中）", "莲花", "运势平稳，需耐心等待。", "工作进展缓慢，勿急。", "平淡是真，珍惜眼前。", "注意休息，避免劳累。", "静心等待时机。"),
        SiamSign("เสีย（差）", "蛇", "运势不佳，易有口舌是非。", "工作有阻碍，谨言慎行。", "感情易生误会，冷静沟通。", "小病易发，及时就医。", "持戒忍辱，化解逆境。"),
        SiamSign("ดีมาก（极好）", "金龙", "大吉大利，一切圆满。", "事业突飞猛进，名利双收。", "良缘天成，喜事临门。", "福寿安康。", "回馈社会，积累福报。")
    )

    // ---------- 4. 希腊神谕 ----------
    private val GREEK = listOf(
        "认识你自己（Know thyself）",
        "凡事勿过度（Nothing in excess）",
        "万物皆流，无物常驻（Everything flows, nothing stands still）",
        "命运是性格的奴隶（Character is destiny）",
        "把握今日（Carpe diem）",
        "智慧是灵魂的向导（Wisdom is the guide of the soul）",
        "勇者面前无绝路（Fortune favors the bold）",
        "静默是最高的智慧（Silence is the highest wisdom）",
        "时间揭示一切（Time reveals all things）",
        "真实的谎言（The lie that tells the truth）"
    )

    // ---------- 5. 圣经掣签 ----------
    private val BIBLE = listOf(
        "是的，这是正路，你要行在其中。",
        "不，你所求的并非最善。",
        "等待，我的时候还未到。",
        "我已听见你的祷告，必为你成就。",
        "你当刚强壮胆，不要惧怕。",
        "不可偏离左右，要专心仰赖。",
        "看哪，我必与你同在，直到世界的末了。",
        "你所做的，要交托我，我就必成全。"
    )

    // ---------- 6. 非洲贝壳 ----------
    private data class Cowrie(val name: String, val meaning: String, val advice: String)
    private val COWRIE = listOf(
        Cowrie("白贝", "大吉，万事顺利，福运亨通。", "感恩前行，广结善缘。"),
        Cowrie("黑贝", "有阻碍，需谨慎，但仍有转机。", "耐心应对，可化险为夷。"),
        Cowrie("花贝", "人际关系佳，得贵人助。", "积极社交，把握机会。"),
        Cowrie("黄贝", "财运上升，但有竞争。", "理财需谨慎，勿贪。"),
        Cowrie("蓝贝", "健康需关注，注意休息。", "调整作息，增强体质。"),
        Cowrie("红贝", "爱情运佳，表白成功。", "真诚以待，幸福可期。"),
        Cowrie("绿贝", "事业有进展，但宜稳扎稳打。", "积累经验，水到渠成。"),
        Cowrie("紫贝", "灵性提升，悟性增强。", "向内探索，智慧自现。"),
        Cowrie("金贝", "大凶，诸事不顺，宜避锋芒。", "守静反思，修养身心。"),
        Cowrie("银贝", "人际关系有隙，需和解。", "宽容大度，重修旧好。"),
        Cowrie("铜贝", "财运平平，不宜投资。", "储蓄为主，求稳。"),
        Cowrie("铁贝", "工作中易有冲突，忍耐为上。", "谦和待人，避免争端。"),
        Cowrie("石贝", "健康有隐患，及时检查。", "重视身体，及时就医。"),
        Cowrie("玉贝", "恋爱中略有波折，沟通解决。", "真诚沟通，增进理解。"),
        Cowrie("贝母", "事业将迎来转机，需耐心。", "坚定信念，等待时机。"),
        Cowrie("皇贝", "极吉之兆，百事亨通。", "大展宏图，福泽深厚。")
    )

    // ---------- 7. 翻书占卜 ----------
    private val BIBLIO = listOf(
        "莎士比亚" to "存在或不存在，这是个问题。",
        "但丁" to "在人生旅程的中途，我发现自己身处黑暗的森林。",
        "歌德" to "你若要喜爱你自己的价值，你就得给世界创造价值。",
        "孔子" to "学而不思则罔，思而不学则殆。",
        "老子" to "上善若水，水善利万物而不争。",
        "庄子" to "吾生也有涯，而知也无涯。",
        "柏拉图" to "理想存在于现实之中，而非现实之上。",
        "亚里士多德" to "求知是人类的天性。",
        "卢梭" to "人人生而自由，却无处不在枷锁之中。",
        "尼采" to "凡不能毁灭我的，必使我更强大。",
        "托尔斯泰" to "幸福的家庭都是相似的，不幸的家庭各有各的不幸。",
        "海明威" to "一个人可以被毁灭，但不能被打败。",
        "村上春树" to "不是所有的鱼，都会生活在同一片海里。",
        "圣-埃克苏佩里" to "只有用心看，才能看得清楚；重要的东西用眼睛是看不见的。"
    )

    // ---------- 8. 水占神签 ----------
    private data class Mizu(val grade: String, val text: String, val meaning: String, val advice: String)
    private val MIZU = listOf(
        Mizu("大吉", "水清影现，心诚则灵。万事如意，福运随行。", "澄净心灵，愿望将如水中倒影般清晰实现。", "保持纯净的心念，好运自然降临。"),
        Mizu("吉", "溪流潺潺，指引方向。循序渐进，可达远方。", "运势平稳向上，如溪流般持之以恒，终汇入海。", "脚踏实地，勿急于求成。"),
        Mizu("中吉", "水波微澜，隐藏机遇。洞察细微，方得先机。", "表面平静下暗藏机会，需敏锐观察。", "培养洞察力，抓住一闪而过的灵感。"),
        Mizu("凶", "浊浪滔天，切勿涉险。守静待时，风平浪静。", "当前环境不利，宜退守自保。", "避免冲动，静待时机好转。"),
        Mizu("大凶", "深渊无底，警惕陷阱。回头是岸，方可保全。", "陷入困境，需果断回头，否则危险。", "及时止损，寻求帮助。"),
        Mizu("末吉", "细雨沾衣，润物无声。小事积累，终成大用。", "小事上努力，将带来长远收益。", "重视细节，不轻视微小的进步。")
    )

    // ---------- 9. 日本御神签 ----------
    private data class OmikujiLevel(val id: String, val nameJa: String, val nameEn: String)
    private val OMIKUJI_LEVELS = listOf(
        OmikujiLevel("dai_kichi", "大吉", "Dai-kichi"),
        OmikujiLevel("kichi", "吉", "Kichi"),
        OmikujiLevel("chu_kichi", "中吉", "Chu-kichi"),
        OmikujiLevel("sho_kichi", "小吉", "Sho-kichi"),
        OmikujiLevel("sue_kichi", "末吉", "Sue-kichi"),
        OmikujiLevel("kyo", "凶", "Kyo"),
        OmikujiLevel("dai_kyo", "大凶", "Dai-kyo")
    )
    private val OMIKUJI_MEANING = mapOf(
        "dai_kichi" to ("这是御神签中最好的运势！" to ("代表绝佳好运，诸事顺遂，是神明赐予的最高祝福。" to "这是难得的好运，请保持谦逊与感恩之心，继续努力，好运将会持续。")),
        "kichi" to ("这是仅次于「大吉」的好运。" to ("代表好运，没有大吉那样的运势急剧下降的可能性，是稳定向好的运势。" to "运势平稳向好，只要保持现状、脚踏实地，好事会自然而然地发生。")),
        "chu_kichi" to ("这是中等程度的吉签。" to ("代表中等好运。根据个人努力，今后运气可能会进一步上升。" to "当前运势中等，但未来可期。关键在于你接下来的努力和选择。")),
        "sho_kichi" to ("这是「中吉」和「末吉」之间的运势。" to ("代表少许好运，说不上好但也不算差，据说是会有小小的幸福。" to "会有一些小的幸福和幸运，保持平和的心态，珍惜身边的点滴美好。")),
        "sue_kichi" to ("这是「末吉」，表示运势正在向好的方向发展。" to ("虽然现在可能感受不强，但未来会逐渐好转，是个好的兆头。" to "不要着急，好事多磨。耐心等待，运势会慢慢提升。")),
        "kyo" to ("这是「凶」签，代表运势不佳。" to ("代表恶运，可能会遇到一些困难或挑战。" to "不必灰心！可将此签绑在神社的指定处，寓意将厄运留下，并更加谨慎地行动。")),
        "dai_kyo" to ("这是比「凶」更严重的「大凶」。" to ("代表非常不好的运势，预示着较大的困难或挑战。" to "这是最需要警惕的签。请务必保持冷静，行事加倍谨慎。传统上将此签绑在神社并诚心祈祷，可趋吉避凶。")),
    )
    private val OMIKUJI_CATS = mapOf(
        "dai_kichi" to listOf("愿望将会顺利实现，甚至超出预期。", "身体健康，精力充沛。", "恋情甜蜜，关系进一步发展。", "工作顺风顺水，有晋升或获得赏识的机会。", "学业进步显著，考试运极佳。", "旅行顺利，充满愉快的邂逅。", "财运亨通，有意外的收入。", "你等待的人或消息会很快到来。"),
        "kichi" to listOf("愿望有实现的可能，需要你主动争取。", "健康状况良好，注意规律作息。", "感情稳定，适合培养默契。", "工作进展顺利，按部就班即可。", "学习状态不错，保持专注。", "旅行平安愉快，会有小惊喜。", "财运平稳，不宜进行高风险投资。", "耐心等待，好消息会在合适的时候到来。"),
        "chu_kichi" to listOf("愿望能否实现，关键在于你的努力。", "注意劳逸结合，小病痛不可忽视。", "感情需要更多的沟通和理解。", "工作上可能会遇到一些小挑战，是锻炼的机会。", "学习上遇到瓶颈，需要寻找新的方法。", "旅行可能会有小的波折，但总体无碍。", "财运普通，注意节制消费。", "你等待的事情可能需要更长时间。"),
        "sho_kichi" to listOf("愿望会有小的进展，不要期望一蹴而就。", "身体无大碍，但要注意季节变化。", "会有小小的浪漫或心动的时刻。", "工作中会有小的成就，值得庆祝。", "学习上会有小的突破，积少成多。", "旅行中会发现一些不起眼但有趣的事物。", "会有小的进账，知足常乐。", "你等待的人或事，可能会有一些眉目了。"),
        "sue_kichi" to listOf("愿望正在朝着好的方向发展，需要耐心。", "健康运势在回升，保持良好习惯。", "感情在慢慢升温，细水长流。", "工作运势在好转，之前的问题会逐渐解决。", "学习状态在恢复，坚持下去会看到成果。", "旅行运势转好，适合计划出行。", "财运在回升，但还需谨慎。", "耐心等待，转机即将出现。"),
        "kyo" to listOf("愿望暂时难以实现，建议重新审视目标。", "需特别注意身体状况，及时就医。", "感情上可能会有摩擦或误会，需要冷静处理。", "工作会遇到阻碍，需谨言慎行。", "学习效率低下，可能需要休息调整。", "旅行计划可能受阻，建议延期或更改。", "财运不佳，避免投资和借贷。", "你等待的事情可能会落空，做好心理准备。"),
        "dai_kyo" to listOf("愿望短期内难以实现，需重新规划。", "健康亮起红灯，务必进行详细检查。", "感情面临严峻考验，需坦诚沟通。", "工作陷入困境，需寻求帮助或暂时退守。", "学业遇到重大挫折，需调整心态和方法。", "旅行凶险，建议取消或推迟。", "财运极差，有破财之兆，务必守财。", "你等待的事情可能以失望告终。")
    )
    private val OMIKUJI_CAT_NAMES = listOf("愿望", "健康", "恋爱", "工作", "学业", "旅行", "财运", "待人")

    // ---------- 统一结果模型 ----------
    data class LotResult(
        val system: LotSystem,
        val date: LocalDate,
        val title: String,
        val detail: List<Pair<String, String>>
    )

    /** 按日期种子确定性抽取（seed 用于「再抽一次」时变化结果） */
    fun draw(system: LotSystem, date: LocalDate = LocalDate.now(), seedOffset: Int = 0): LotResult {
        val seed = date.year * 372 + date.monthValue * 31 + date.dayOfMonth + seedOffset * 7919
        return when (system) {
            LotSystem.ChineseKauCim -> {
                val s = KAU_CIM[seed % KAU_CIM.size]
                LotResult(system, date, "签号 ${seed % KAU_CIM.size + 1} · ${s.grade} · ${s.title}", listOf(
                    "签诗" to s.poem, "释义" to s.meaning, "建议" to s.advice
                ))
            }
            LotSystem.TibetanDivination -> {
                val s = TIBETAN[seed % TIBETAN.size]
                LotResult(system, date, "${s.grade} · ${s.title}", listOf(
                    "解读" to s.meaning, "教诫" to s.advice
                ))
            }
            LotSystem.ThaiSiamCee -> {
                val s = SIAM[seed % SIAM.size]
                LotResult(system, date, "${s.grade} · ${s.title}", listOf(
                    "整体运势" to s.overall, "事业" to s.work, "爱情" to s.love,
                    "健康" to s.health, "建议" to s.advice
                ))
            }
            LotSystem.GreekOracle -> {
                val o = GREEK[seed % GREEK.size]
                LotResult(system, date, "德尔斐神谕", listOf(
                    "神谕" to o,
                    "解读" to "此箴言提醒你审视内心，顺应自然之道，在纷扰中保持清醒，命运由你的选择塑造。"
                ))
            }
            LotSystem.BibleLot -> {
                val m = BIBLE[seed % BIBLE.size]
                LotResult(system, date, "乌陵与土明", listOf(
                    "神谕" to m,
                    "指引" to "这是神圣的回应，请以信心接受，并遵行其指引，必得平安。"
                ))
            }
            LotSystem.CowrieShell -> {
                val c = COWRIE[seed % COWRIE.size]
                LotResult(system, date, "${c.name}（贝壳组合 ${(seed % 16).toString(2).padStart(4, '0')}）", listOf(
                    "解读" to c.meaning, "建议" to c.advice
                ))
            }
            LotSystem.Bibliomancy -> {
                val (author, quote) = BIBLIO[seed % BIBLIO.size]
                LotResult(system, date, author, listOf(
                    "箴言" to "「$quote」",
                    "启示" to "此句引导你思考当前处境，以智慧之光照亮前路。"
                ))
            }
            LotSystem.MizuKuji -> {
                val s = MIZU[seed % MIZU.size]
                LotResult(system, date, "${s.grade}", listOf(
                    "签文" to s.text, "释义" to s.meaning, "建议" to s.advice,
                    "备注" to "（此签放入水中方显文字，意为心诚则灵）"
                ))
            }
            LotSystem.Omikuji -> {
                val lv = OMIKUJI_LEVELS[seed % OMIKUJI_LEVELS.size]
                val m = OMIKUJI_MEANING.getValue(lv.id)
                val summary = m.first
                val meaning = m.second.first
                val advice = m.second.second
                val cats = OMIKUJI_CATS.getValue(lv.id)
                val detail = mutableListOf<Pair<String, String>>()
                detail.add("含义" to meaning)
                OMIKUJI_CAT_NAMES.zip(cats).forEach { (name, txt) -> detail.add(name to txt) }
                detail.add("建议" to advice)
                LotResult(system, date, "${lv.nameJa}（${lv.nameEn}）· $summary", detail)
            }
        }
    }

    val ALL = LotSystem.entries
}
