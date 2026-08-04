package com.xuanji.app.domain.divination

import com.xuanji.app.data.model.Hexagram
import kotlin.random.Random

/**
 * 六爻（周易纳甲筮法）核心算法：
 * 1. 三枚铜钱摇出六爻（老阴6/少阳7/少阴8/老阳9，6、9 为动爻）
 * 2. 排本卦、定卦宫与世爻（来自八宫数据）
 * 3. 纳甲：每爻配地支（按上下卦纳甲法）
 * 4. 六亲：以卦宫五行为「我」，依五行生克定 兄弟/子孙/父母/妻财/官鬼
 * 5. 世应：世爻 +3 为应爻
 * 6. 动爻化变卦，断语综合本卦辞、动爻六亲与世应
 */
object LiuYao {

    private data class TrigramInfo(val cn: String, val symbol: String, val wx: String)

    private val TRIGRAM = mapOf(
        "QIAN" to TrigramInfo("乾", "☰", "金"),
        "DUI" to TrigramInfo("兑", "☱", "金"),
        "LI" to TrigramInfo("离", "☲", "火"),
        "ZHEN" to TrigramInfo("震", "☳", "木"),
        "XUN" to TrigramInfo("巽", "☴", "木"),
        "KAN" to TrigramInfo("坎", "☵", "水"),
        "GEN" to TrigramInfo("艮", "☶", "土"),
        "KUN" to TrigramInfo("坤", "☷", "土")
    )

    /** 纳甲地支：八卦自下而上 6 爻 */
    private val NA_JIA = mapOf(
        "QIAN" to listOf("子", "寅", "辰", "午", "申", "戌"),
        "KUN" to listOf("未", "巳", "卯", "丑", "亥", "酉"),
        "ZHEN" to listOf("子", "寅", "辰", "午", "申", "戌"),
        "XUN" to listOf("丑", "亥", "酉", "未", "巳", "卯"),
        "KAN" to listOf("寅", "辰", "午", "申", "戌", "子"),
        "LI" to listOf("卯", "丑", "亥", "酉", "未", "巳"),
        "GEN" to listOf("辰", "午", "申", "戌", "子", "寅"),
        "DUI" to listOf("巳", "卯", "丑", "亥", "酉", "未")
    )

    private val DIZHI_WX = mapOf(
        "子" to "水", "丑" to "土", "寅" to "木", "卯" to "木", "辰" to "土",
        "巳" to "火", "午" to "火", "未" to "土", "申" to "金", "酉" to "金",
        "戌" to "土", "亥" to "水"
    )

    private val WX_SHENG = mapOf("木" to "火", "火" to "土", "土" to "金", "金" to "水", "水" to "木")
    private val WX_KE = mapOf("木" to "土", "土" to "水", "水" to "火", "火" to "金", "金" to "木")

    /** 单爻信息（自下而上） */
    data class LineInfo(
        val pos: Int,         // 1-6（1=初爻）
        val yang: Boolean,    // true=阳爻
        val changing: Boolean, // true=动爻（老阴/老阳）
        val diZhi: String,    // 纳甲地支
        val liuQin: String,   // 六亲
        val isShi: Boolean,   // 世爻
        val isYing: Boolean   // 应爻
    )

    data class LiuYaoResult(
        val original: Hexagram,
        val changed: Hexagram?,
        val lines: List<LineInfo>,
        val shiYao: Int,      // 世爻 1-6
        val yingYao: Int,     // 应爻 1-6
        val changingIdx: List<Int>, // 动爻 0-based（自下而上）
        val palaceWx: String,
        val reading: String
    )

    /** 摇卦并装卦。hexagrams：全部 64 卦（用于按二进制查找本卦/变卦）。 */
    fun cast(hexagrams: List<Hexagram>, random: Random = Random.Default): LiuYaoResult {
        // 1. 六爻：每爻三枚铜钱
        val values = List(6) { tossThreeCoins(random) } // 6/7/8/9，下标 0=初爻
        val originalBinary = values.joinToString("") { if (it == 7 || it == 9) "1" else "0" }
        val changingIdx = values.indices.filter { values[it] == 6 || values[it] == 9 }
        val changedBinary = buildString {
            values.forEachIndexed { i, v ->
                val c = if (v == 6 || v == 9) (if (v == 6) '1' else '0') else (if (v == 7) '1' else '0')
                append(c)
            }
        }

        val original = hexagrams.first { it.binary == originalBinary }
        val changed = if (changingIdx.isEmpty()) null
        else hexagrams.firstOrNull { it.binary == changedBinary }

        val palaceWx = original.palace
        val shiYao = original.world
        val yingYao = (shiYao - 1 + 3) % 6 + 1

        // 2. 纳甲 + 六亲
        val lowerNa = NA_JIA.getValue(original.lower)
        val upperNa = NA_JIA.getValue(original.upper)
        val naJia = lowerNa + upperNa // 6 地支自下而上

        val lines = values.mapIndexed { i, v ->
            val diZhi = naJia[i]
            val wireWx = DIZHI_WX.getValue(diZhi)
            LineInfo(
                pos = i + 1,
                yang = (v == 7 || v == 9),
                changing = (v == 6 || v == 9),
                diZhi = diZhi,
                liuQin = liuQin(wireWx, palaceWx),
                isShi = (i + 1 == shiYao),
                isYing = (i + 1 == yingYao)
            )
        }

        return LiuYaoResult(
            original = original,
            changed = changed,
            lines = lines,
            shiYao = shiYao,
            yingYao = yingYao,
            changingIdx = changingIdx,
            palaceWx = palaceWx,
            reading = buildReading(original, changed, lines, shiYao, changingIdx, palaceWx)
        )
    }

    /** 三枚铜钱：正面(阳)记 3，反面(阴)记 2；6=老阴 7=少阳 8=少阴 9=老阳 */
    private fun tossThreeCoins(random: Random): Int {
        var sum = 0
        repeat(3) { sum += if (random.nextBoolean()) 3 else 2 }
        return sum // 6,7,8,9
    }

    private fun liuQin(wireWx: String, palaceWx: String): String {
        return when {
            wireWx == palaceWx -> "兄弟"
            WX_SHENG[wireWx] == palaceWx -> "父母"   // 地支生宫 = 生我
            WX_SHENG[palaceWx] == wireWx -> "子孙"   // 宫生地支 = 我生
            WX_KE[wireWx] == palaceWx -> "官鬼"      // 地支克宫 = 克我
            else -> "妻财"                            // 宫克地支 = 我克
        }
    }

    private fun buildReading(
        original: Hexagram,
        changed: Hexagram?,
        lines: List<LineInfo>,
        shiYao: Int,
        changingIdx: List<Int>,
        palaceWx: String
    ): String {
        val sb = StringBuilder()
        sb.append("本卦《${original.name}》（${original.palace}宫，世在${shiYao}爻），卦辞：${original.judgment}。")
        if (changed == null) {
            sb.append("静卦无动爻，以本卦卦辞与世爻之六亲断事体根基。")
        } else {
            val moving = changingIdx.map { idx ->
                val ln = lines[idx]
                "第${idx + 1}爻（${ln.liuQin}${if (ln.yang) "阳" else "阴"}动）"
            }.joinToString("、")
            sb.append("动爻：$moving，化出变卦《${changed.name}》。")
            sb.append("动爻临「${lines[changingIdx.first()].liuQin}」，为事之枢机；世爻为我，应爻为人，观生克以断吉凶。")
        }
        sb.append("（纳甲筮法为传统数术，结果仅供文化娱乐参考）")
        return sb.toString()
    }

    /** 问事类别：决定「用神」与解说口径 */
    enum class Category(val key: String, val label: String, val yongShen: String?, val useShi: Boolean) {
        LOST("lost", "寻找失物", "妻财", false),
        CAREER("career", "事业功名", "官鬼", false),
        LOVE("love", "感情姻缘", "妻财", false),
        HEALTH("health", "健康疾病", "官鬼", false),
        WEALTH("wealth", "求财谋利", "妻财", false),
        TRAVEL("travel", "出行远行", null, true),
        STUDY("study", "考试学业", "官鬼", false),
        GENERAL("general", "综合问事", null, true)
    }

    data class LiuYaoReading(
        val question: String,
        val category: Category,
        val yongShenLine: LineInfo?,
        val general: String,
        val specific: String
    )

    fun detectCategory(q: String): Category {
        val s = q.lowercase()
        return when {
            "找" in s || "丢" in s || "失" in s || "寻" in s || "东西" in s || "物" in s || "lost" in s -> Category.LOST
            "事业" in s || "工作" in s || "官" in s || "职位" in s || "升职" in s || "career" in s -> Category.CAREER
            "感情" in s || "姻缘" in s || "恋爱" in s || "婚姻" in s || "妻" in s || "夫" in s || "对象" in s || "love" in s -> Category.LOVE
            "健康" in s || "病" in s || "身体" in s || "医" in s || "health" in s -> Category.HEALTH
            "财" in s || "钱" in s || "富" in s || "收入" in s || "wealth" in s -> Category.WEALTH
            "出行" in s || "旅行" in s || "出门" in s || "远行" in s || "travel" in s -> Category.TRAVEL
            "考试" in s || "学业" in s || "考" in s || "升学" in s || "读书" in s || "study" in s -> Category.STUDY
            else -> Category.GENERAL
        }
    }

    private fun lineWx(line: LineInfo): String = DIZHI_WX.getValue(line.diZhi)

    /** 两爻五行关系：以世(我)为基准描述与应(对方/外因)的生克 */
    private fun relation(a: String, b: String): String {
        return when {
            a == b -> "比和（同心协力）"
            WX_SHENG[a] == b -> "我生（付出、泄气）"
            WX_KE[a] == b -> "我克（掌控、用力）"
            WX_SHENG[b] == a -> "生我（得助、易成）"
            WX_KE[b] == a -> "克我（受制、阻碍）"
            else -> "无生克（平淡）"
        }
    }

    /** 根据问题 + 卦象，给出针对性的现代解说 */
    fun interpret(result: LiuYaoResult, questionRaw: String): LiuYaoReading {
        val q = questionRaw.trim()
        val cat = if (q.isEmpty()) Category.GENERAL else detectCategory(q)
        val yong = if (cat.useShi) result.lines.firstOrNull { it.isShi }
        else {
            val cands = result.lines.filter { it.liuQin == cat.yongShen }
            if (cands.isEmpty()) null else cands.firstOrNull { it.changing } ?: cands.first()
        }
        val shi = result.lines.first { it.isShi }
        val ying = result.lines.first { it.isYing }

        val moving = result.changingIdx.isNotEmpty()
        val general = buildString {
            append("就「${if (q.isEmpty()) "心中所问" else q}」（${cat.label}）起得本卦《${result.original.name}》")
            if (result.changed != null) append("，化出变卦《${result.changed.name}》")
            append("。")
            if (!moving) append("静卦无动爻，事体尚未发动，宜静观其变。")
            else {
                val posName = listOf("初", "二", "三", "四", "五", "上")
                val ms = result.changingIdx.map { idx ->
                    val p = idx + 1
                    (if (p == 6) "上" else if (p == 1) "初" else posName[p]) + (if (result.lines[idx].yang) "九" else "六")
                }.joinToString("、")
                append("有 ${result.changingIdx.size} 个动爻（$ms），事机已动，吉凶在变。")
            }
        }

        val specific = buildSpecific(cat, yong, shi, ying)
        return LiuYaoReading(q, cat, yong, general, specific)
    }

    private fun buildSpecific(cat: Category, yong: LineInfo?, shi: LineInfo, ying: LineInfo): String {
        val posName = listOf("初", "二", "三", "四", "五", "上")
        val yongDesc = if (yong == null) "用神不现于卦中，事体隐晦"
        else {
            val p = yong.pos
            val pos = if (p == 6) "上" else if (p == 1) "初" else posName[p]
            val dyn = if (yong.changing) "发动（必有变化）" else "安静（伏藏待时）"
            "用神在${pos}爻（${yong.liuQin}），$dyn"
        }
        val rel = relation(lineWx(shi), lineWx(ying))
        val head = "$yongDesc。世应之间$rel。"
        val advice = when (cat) {
            Category.LOST -> "妻财为所寻之物。用神安静多在原处，发动则已移位；世生应、比和多在近处，应克世则难回。建议从最后接触、常放之处细寻。"
            Category.CAREER -> "官鬼为功名职位。用神旺相发动主升迁变动，安静则守成；世爻得用神生扶，谋事易成。"
            Category.LOVE -> "男问妻财、女问官鬼为对方。用神发动主感情生变，安静则平稳；世应相生比和则两情相悦，相克则多磨合。"
            Category.HEALTH -> "官鬼为病根，子孙为医药。用神（官鬼）发动病势有变，宜早诊；静卦则平稳。世爻强健则自身可抗，偏弱则需调摄。"
            Category.WEALTH -> "妻财为财源。用神发动主财来财去，安静则积滞；世爻能生扶用神，求财可得，克之则须费力。"
            Category.TRAVEL -> "以世爻为自身、为出行人。世爻安静出行平顺，发动则有变（改期或绕路）；世应相生出行得助，相克宜谨慎。"
            Category.STUDY -> "官鬼为功名、父母为文书。用神（官鬼）旺相发动主考运上扬，安静则平稳；世爻得生扶则临场有神。"
            Category.GENERAL -> "以世爻为问事之人。世爻旺相、得应方生扶则事可成；世应相克则多阻滞，宜审时度势。"
        }
        return head + advice + "（纳甲筮法为传统数术，结果仅供文化娱乐参考）"
    }
}
