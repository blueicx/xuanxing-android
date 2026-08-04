package com.xuanji.app.data.model

/** 五行 */
enum class Element { WOOD, FIRE, EARTH, METAL, WATER }

/** 天干 */
enum class Stem(
    val chinese: String,
    val element: Element,
    val isYang: Boolean
) {
    甲("甲", Element.WOOD, true),
    乙("乙", Element.WOOD, false),
    丙("丙", Element.FIRE, true),
    丁("丁", Element.FIRE, false),
    戊("戊", Element.EARTH, true),
    己("己", Element.EARTH, false),
    庚("庚", Element.METAL, true),
    辛("辛", Element.METAL, false),
    壬("壬", Element.WATER, true),
    癸("癸", Element.WATER, false);
}

/** 地支（含藏干） */
enum class Branch(
    val chinese: String,
    val element: Element,
    val isYang: Boolean,
    val zodiac: String,
    /** 藏干：本气、中气、余气（由强到弱） */
    val hidden: List<Stem>
) {
    子("子", Element.WATER, true, "鼠", listOf(Stem.癸)),
    丑("丑", Element.EARTH, false, "牛", listOf(Stem.己, Stem.癸, Stem.辛)),
    寅("寅", Element.WOOD, true, "虎", listOf(Stem.甲, Stem.丙, Stem.戊)),
    卯("卯", Element.WOOD, false, "兔", listOf(Stem.乙)),
    辰("辰", Element.EARTH, true, "龙", listOf(Stem.戊, Stem.乙, Stem.癸)),
    巳("巳", Element.FIRE, false, "蛇", listOf(Stem.丙, Stem.庚, Stem.戊)),
    午("午", Element.FIRE, true, "马", listOf(Stem.丁, Stem.己)),
    未("未", Element.EARTH, false, "羊", listOf(Stem.己, Stem.丁, Stem.乙)),
    申("申", Element.METAL, true, "猴", listOf(Stem.庚, Stem.壬, Stem.戊)),
    酉("酉", Element.METAL, false, "鸡", listOf(Stem.辛)),
    戌("戌", Element.EARTH, true, "狗", listOf(Stem.戊, Stem.辛, Stem.丁)),
    亥("亥", Element.WATER, false, "猪", listOf(Stem.壬, Stem.甲));
}

/** 一柱（天干 + 地支） */
data class Pillar(val stem: Stem, val branch: Branch) {
    val display: String get() = stem.chinese + branch.chinese
    val stemElement: Element get() = stem.element
    val branchElement: Element get() = branch.element
}

/** 完整八字命盘 */
data class BaziChart(
    val yearPillar: Pillar,
    val monthPillar: Pillar,
    val dayPillar: Pillar,
    val hourPillar: Pillar,
    val dayMaster: Stem,
    val zodiac: String,
    val elementCounts: Map<Element, Int>,
    val favorableElements: List<Element>,
    val unfavorableElements: List<Element>
) {
    val dayMasterElement: Element get() = dayMaster.element
    val display: String
        get() = "${yearPillar.display} ${monthPillar.display} ${dayPillar.display} ${hourPillar.display}"
}

/** 十神 */
enum class TenGod(val chinese: String, val category: String) {
    比肩("比肩", "同我"),
    劫财("劫财", "同我"),
    正印("正印", "生我"),
    偏印("偏印", "生我"),
    食神("食神", "我生"),
    伤官("伤官", "我生"),
    正财("正财", "我克"),
    偏财("偏财", "我克"),
    正官("正官", "克我"),
    七杀("七杀", "克我");
}

/** 单个十神条目 */
data class TenGodItem(
    val pillarLabel: String, // 年/月/日/时
    val position: String,    // 天干/本气/中气/余气
    val stem: Stem,
    val tenGod: TenGod
)

/** 旺衰判定结果 */
data class StrengthResult(
    val level: String,    // 身强 / 身弱 / 中和
    val score: Int,
    val desc: String
)

/** 用神 / 忌神 */
data class YongJi(
    val useful: List<Element>,
    val avoidance: List<Element>,
    val desc: String
)

/** 一步大运 */
data class DaYun(
    val index: Int,    // 第几运，1 起
    val startAge: Int,
    val endAge: Int,
    val pillar: Pillar,
    val desc: String
)

/** 生辰格局（以月令取格） */
data class Geju(
    val name: String,       // 正官格 / 七杀格 / 建禄格 ...
    val category: String,   // 吉格 / 凶格转吉 / 特殊格 / 中格
    val desc: String
)

/** 单条命中所带神煞 */
data class ShenShaItem(
    val name: String,       // 天乙贵人
    val nature: String,     // 吉 / 凶 / 中
    val branch: Branch?,    // 落于何地支（无则 null）
    val desc: String
)

/** 神煞图鉴条目（静态知识库） */
data class ShenShaMeta(
    val name: String,
    val icon: String,       // emoji 图标
    val nature: String,     // 吉 / 凶 / 中
    val summary: String     // 一句话释义
)

/** 综合结论的单个维度 */
data class ConclusionItem(
    val title: String,      // 性格底色 / 事业与行业 / 财运 ...
    val icon: String,       // emoji
    val headline: String,   // 一句话结论
    val body: String,       // 详细论述
    val tags: List<String> = emptyList(),  // 关键词标签（行业、方位…）
    /**
     * 仅「健康」项使用：列出需要在人体图上高亮的身体部位 key
     * 取值范围：肝/胆/眼/筋/心/血脉/小肠/脾胃/肌肉/消化/肺/大肠/呼吸/皮肤/肾/膀胱/骨/泌尿
     */
    val highlightParts: List<String> = emptyList()
)

/** 八字总论：综合命盘全部要素给出的结论 */
data class BaziConclusion(
    val summary: String,            // 命局总述
    val items: List<ConclusionItem> // 分维度论述
)

/** 地支间的关系（合/冲/害/刑/会） */
data class BranchRelation(
    val type: String, // 六合 / 三合 / 三会 / 六冲 / 六害 / 三刑
    val branches: List<Branch>,
    val desc: String
)

/** 八字完整分析（排盘 → 定日主 → 十神 → 旺衰 → 用忌 → 大运流年 → 刑冲合害） */
data class BaziFull(
    val chart: BaziChart,
    val tenGods: List<TenGodItem>,
    val strength: StrengthResult,
    val yongJi: YongJi,
    val daYun: List<DaYun>,
    val currentYearPillar: Pillar,          // 今年流年
    val futureYears: List<Pair<String, Pillar>>, // 明年/后年/大后年
    val relations: List<BranchRelation>,
    val geju: Geju,
    val shenSha: List<ShenShaItem>,
    val conclusion: BaziConclusion,
    val note: String
)
