package com.xuanji.app.ui.divination

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.ui.graphics.vector.ImageVector
import com.xuanji.app.ui.Screen
import com.xuanji.app.ui.components.ArabicAstrologyIcon
import com.xuanji.app.ui.components.AstroStarIcon
import com.xuanji.app.ui.components.AztecIcon
import com.xuanji.app.ui.components.BabylonianIcon
import com.xuanji.app.ui.components.BibleLotIcon
import com.xuanji.app.ui.components.BibliomancyIcon
import com.xuanji.app.ui.components.CelticTreeIcon
import com.xuanji.app.ui.components.ChakraIcon
import com.xuanji.app.ui.components.CrystalBallIcon
import com.xuanji.app.ui.components.ErshibaIcon
import com.xuanji.app.ui.components.FengShuiIcon
import com.xuanji.app.ui.components.GreekAstrologyIcon
import com.xuanji.app.ui.components.GreekOracleIcon
import com.xuanji.app.ui.components.HermesAlchemyIcon
import com.xuanji.app.ui.components.HumanDesignIcon
import com.xuanji.app.ui.components.IChingIcon
import com.xuanji.app.ui.components.IfaIcon
import com.xuanji.app.ui.components.KabbalahIcon
import com.xuanji.app.ui.components.KhmerIcon
import com.xuanji.app.ui.components.LawOfAttractionIcon
import com.xuanji.app.ui.components.LenormandIcon
import com.xuanji.app.ui.components.LiuRenIcon
import com.xuanji.app.ui.components.LiuYaoIcon
import com.xuanji.app.ui.components.LotteryStickIcon
import com.xuanji.app.ui.components.MahaboteIcon
import com.xuanji.app.ui.components.MayaGalacticIcon
import com.xuanji.app.ui.components.MayaTzolkinIcon
import com.xuanji.app.ui.components.MedicineWheelIcon
import com.xuanji.app.ui.components.MeiHuaIcon
import com.xuanji.app.ui.components.MizuKujiIcon
import com.xuanji.app.ui.components.NadiIcon
import com.xuanji.app.ui.components.NagaRainIcon
import com.xuanji.app.ui.components.NameologyIcon
import com.xuanji.app.ui.components.NineStarsIcon
import com.xuanji.app.ui.components.NumerologyIcon
import com.xuanji.app.ui.components.OmikujiIcon
import com.xuanji.app.ui.components.OnmyodoIcon
import com.xuanji.app.ui.components.PalmistryIcon
import com.xuanji.app.ui.components.PersianAstrologyIcon
import com.xuanji.app.ui.components.PhysiognomyIcon
import com.xuanji.app.ui.components.PrasnaIcon
import com.xuanji.app.ui.components.QiMenIcon
import com.xuanji.app.ui.components.QiZhengIcon
import com.xuanji.app.ui.components.RuneIcon
import com.xuanji.app.ui.components.ShellIcon
import com.xuanji.app.ui.components.TaiYiIcon
import com.xuanji.app.ui.components.TajulMulukIcon
import com.xuanji.app.ui.components.TarotIcon
import com.xuanji.app.ui.components.ThaiSiamIcon
import com.xuanji.app.ui.components.ThirteenMoonIcon
import com.xuanji.app.ui.components.TibetAstrologyIcon
import com.xuanji.app.ui.components.TibetanDivIcon
import com.xuanji.app.ui.components.TodayOracleIcon
import com.xuanji.app.ui.components.VastuIcon
import com.xuanji.app.ui.components.VedicIcon
import com.xuanji.app.ui.components.YemeniAstrologyIcon
import com.xuanji.app.ui.components.ZiweiIcon

/**
 * 占卜体系的三级数据：地区(Region) → 子地区(Subregion) → 体系(System)。
 * 导航采用 drill-down：枢纽 → 地区 → 子地区(含总览) → 体系详情。
 * 这样既满足「东亚→中国/日本/西藏→紫微/奇门…」的分级浏览，
 * 又借助 NavHost 返回栈天然保留每一级的位置与滚动，修复「返回后折叠回初始形态」的不适。
 */

data class DivSys(
    val key: String,
    val name: String,
    val desc: String,
    val route: String?,      // null = 规划中
    val icon: ImageVector = Icons.Filled.Casino
)

data class DivSubregion(
    val key: String,
    val name: String,
    val overview: String,
    val systems: List<DivSys> = emptyList(),
    val children: List<DivSubregion> = emptyList()
)

data class DivRegion(
    val key: String,
    val name: String,
    val subregions: List<DivSubregion>
)

fun regionByKey(key: String): DivRegion? = DIVINATION_REGIONS.firstOrNull { it.key == key }
fun subregionByKey(regionKey: String, subKey: String): DivSubregion? {
    val region = regionByKey(regionKey) ?: return null
    // 若命中「仅有一个子分区」的目录层（systems 为空、children 恰一个），直接返回实际列表层，
    // 避免点击后出现空白列表（如 亚洲→中东与西亚）。
    fun unwrap(sub: DivSubregion): DivSubregion =
        if (sub.systems.isEmpty() && sub.children.size == 1) unwrap(sub.children[0]) else sub
    region.subregions.forEach { sub ->
        if (sub.key == subKey) return unwrap(sub)
        sub.children.forEach { child -> if (child.key == subKey) return unwrap(child) }
    }
    return null
}
/** 取某分区下某一级子分区（用于 亚洲→东亚→中国 的三级导航） */
fun childByKey(regionKey: String, parentKey: String, childKey: String): DivSubregion? =
    subregionByKey(regionKey, parentKey)?.children?.firstOrNull { it.key == childKey }

// ============================ 数据 ============================

private fun sys(
    key: String, name: String, desc: String,
    route: String?, icon: ImageVector = Icons.Filled.Casino
): DivSys = DivSys(key, name, desc, route, icon)

val DIVINATION_REGIONS: List<DivRegion> = listOf(
    // ============ 亚洲大板块（含 东亚 / 东南亚 / 南亚 / 中东与西亚） ============
    DivRegion("asia", "亚洲", listOf(
        DivSubregion("eastasia", "东亚", "东亚以中国阴阳五行与天人合一思想为基石，孕育了从汉代八字、唐朝紫微斗数到奇门遁甲、六爻、梅花易数等庞大命理体系，并东传日本形成阴阳道与九星气学。", children = listOf(
            DivSubregion("china", "中国", "中华文明孕育了世界上最庞大的占卜体系：从汉代成熟的八字、唐朝的紫微斗数，到三国两晋的奇门遁甲、京房易传的六爻纳甲，再到北宋邵雍的梅花易数与杨筠松的形法理气风水，以及七政四余的星命之学。以下按出土/成型的经典体系逐一列出。", listOf(
                sys("ziwei", "紫微斗数", "十二宫 + 十四主星排盘，年干四化", Screen.Ziwei.route, AstroStarIcon),
                sys("qimen", "奇门遁甲", "阴阳遁局、九宫八门排盘", Screen.QiMen.route, QiMenIcon),
                sys("fengshui", "风水", "方位八卦与形法理气", Screen.FengShui.route, FengShuiIcon),
                sys("liuyao", "六爻", "铜钱起卦、纳甲六亲世应", Screen.LiuYao.route, LiuYaoIcon),
                sys("iching", "易经六爻占", "64 卦摇卦与卦辞解读", Screen.IChingCast.route, IChingIcon),
                sys("physiognomy", "相术", "面相特征解读", Screen.Physiognomy.route, PhysiognomyIcon),
                sys("nameology", "姓名学", "五格剖象法", Screen.Nameology.route, NameologyIcon),
                sys("taiyi", "太乙神数", "八宫神煞吉凶", Screen.TaiYi.route, TaiYiIcon),
                sys("liuren", "大六壬", "四课三传", Screen.LiuRen.route, LiuRenIcon),
                sys("meihua", "梅花易数", "先天卦数、体用生克", Screen.MeiHua.route, MeiHuaIcon),
                sys("qizheng", "七政四余", "日月五星 + 四余星盘", Screen.QiZheng.route, QiZhengIcon),
                sys("ershiba", "二十八宿", "东方星官与值日", Screen.TwentyEightMansions.route, ErshibaIcon),
                sys("tibet", "西藏占星", "时轮历·五要素·月宿", Screen.TibetanAstrology.route, TibetAstrologyIcon)
            )),
            DivSubregion("japan", "日本", "中国阴阳五行与本土神道信仰融合，形成阴阳道、九星气学等独特体系；其中九星气学以出生年份定本命星，并据五行生克推荐适宜参拜的神社与方位。", listOf(
                sys("ninestars", "九星气学", "本命星 + 神社五行匹配", Screen.NineStars.route, NineStarsIcon),
                sys("onmyodo", "阴阳道", "本命星 + 九曜属星", Screen.Onmyodo.route, OnmyodoIcon)
            ))
        )),
        DivSubregion("southeastasia", "东南亚", "中南半岛诸国在印度文明与本土信仰交织下，发展出以农耕、降雨、方位为核心的地方占卜。", children = listOf(
            DivSubregion("sea", "东南亚", "中南半岛诸国在印度文明与本土信仰交织下，发展出以农耕、降雨、方位为核心的地方占卜。", listOf(
                sys("myanmar", "缅甸黄道带", "玛哈图·主星·七宫", Screen.Mahabote.route, MahaboteIcon),
                sys("khmer", "高棉占星", "生肖·纪元·主星", Screen.KhmerAstrology.route, KhmerIcon),
                sys("naga", "那伽占雨", "佛历生肖·那迦数量", Screen.NagaRain.route, NagaRainIcon),
                sys("tajulmuluk", "Tajul Muluk", "Abjad 姓名合婚", Screen.TajulMuluk.route, TajulMulukIcon)
            ))
        )),
        DivSubregion("southasia", "南亚", "印度文明以吠陀占星（Jyotish）为宗，发展出二十七宿、九星大运与脉轮能量体系。", children = listOf(
            DivSubregion("india", "南亚", "印度文明以吠陀占星（Jyotish）为宗，发展出二十七宿、九星大运与脉轮能量体系。", listOf(
                sys("vedic", "印度占星", "吠陀月亮星座与二十七宿 + Vimshottari 大运", Screen.Vedic.route, VedicIcon),
                sys("naadi", "纳迪占星", "指纹·Nadi·Kandam", Screen.NadiAstrology.route, NadiIcon),
                sys("vastu", "瓦斯图", "VPM 建筑能量分析", Screen.Vastu.route, VastuIcon),
                sys("chakra", "脉轮系统", "人体七大能量中心", Screen.Chakra.route, ChakraIcon),
                sys("prasna", "普拉萨那", "卜卦占星·即时问答", Screen.Prasna.route, PrasnaIcon)
            ))
        )),
        DivSubregion("middleeast", "中东与西亚", "两河流域与阿拉伯、波斯、犹太传统，是西方占星术的源头与技法延伸。", children = listOf(
            DivSubregion("middleeast", "中东与西亚", "两河流域与阿拉伯、波斯、犹太传统，是西方占星术的源头与技法延伸。", listOf(
                sys("babylon", "巴比伦占星", "System A/B 星历", Screen.BabylonianAstrology.route, BabylonianIcon),
                sys("arab", "阿拉伯占星", "阿拉伯点·周期·Abjad", Screen.ArabicAstrology.route, ArabicAstrologyIcon),
                sys("persia", "波斯占星", "Jarbakhtar·法达", Screen.PersianAstrology.route, PersianAstrologyIcon),
                sys("yemen", "也门占星", "南阿拉伯星学", Screen.YemeniAstrology.route, YemeniAstrologyIcon),
                sys("jewish", "犹太占星", "卡巴拉星象", Screen.KabbalahAstrology.route, KabbalahIcon)
            ))
        ))
    )),
    DivRegion("africa", "非洲", listOf(
        DivSubregion("africa", "非洲", "约鲁巴文明的艾法预言体系于 2008 年列入联合国非物质文化遗产。", listOf(
            sys("ifa", "艾法预言", "约鲁巴 256 章奥杜体系（联合国非遗）", Screen.Ifa.route, IfaIcon)
        ))
    )),
    DivRegion("europe", "欧洲", listOf(
        DivSubregion("europe", "欧洲", "从希腊本命盘到凯尔特法器、北欧符文，再到文艺复兴的塔罗与雷诺曼，欧洲占卜以符号与牌卡见长。", listOf(
            sys("greek", "希腊占星", "推运·法达·幸运点", Screen.HellenisticAstrology.route, GreekAstrologyIcon),
            sys("celtic", "凯尔特树历", "欧甘字母·树月", Screen.CelticTree.route, CelticTreeIcon),
            sys("rune", "北欧符文", "Elder Futhark 二十四符文", Screen.Rune.route, RuneIcon),
            sys("lenormand", "雷诺曼", "符号卡牌解读", Screen.Lenormand.route, LenormandIcon),
            sys("palm", "手相", "掌纹特征解读", Screen.Palmistry.route, PalmistryIcon),
            sys("numerology", "数字命理学", "生命路径·命运·成熟数", Screen.Numerology.route, NumerologyIcon),
            sys("hermes", "赫尔墨斯 / 炼金术", "宇宙法则·符号·生命之树", Screen.HermesAlchemy.route, HermesAlchemyIcon)
        ))
    )),
    DivRegion("america", "美洲", listOf(
        DivSubregion("america", "美洲", "中美洲文明以精确的周期历法著称，玛雅卓尔金历与长纪历即是数论与天文的结晶。", listOf(
            sys("maya", "玛雅占星", "260 天卓尔金历 + 长纪历", Screen.MayaTzolkin.route, MayaTzolkinIcon),
            sys("mayagalactic", "玛雅星系印记", "20 图腾 × 13 音阶", Screen.MayaGalactic.route, MayaGalacticIcon),
            sys("aztec", "阿兹特克占星", "Tonalpohualli 日符", Screen.AztecAstrology.route, AztecIcon),
            sys("medicinewheel", "北美药轮", "四大方向·动物图腾", Screen.MedicineWheel.route, MedicineWheelIcon)
        ))
    )),
    DivRegion("modern", "近现代新兴", listOf(
        DivSubregion("modern", "近现代新兴", "20 世纪以后融合多传统的新型体系。", listOf(
            sys("humandesign", "人类图", "占星 + 卡巴拉 + 脉轮融合", Screen.HumanDesign.route, HumanDesignIcon),
            sys("loa", "吸引力法则", "思想吸引现实", Screen.LawOfAttraction.route, LawOfAttractionIcon),
            sys("dreamspell", "13 月亮历", "新纪元历法", Screen.ThirteenMoon.route, ThirteenMoonIcon)
        ))
    )),
    DivRegion("common", "常用占卜", listOf(
        DivSubregion("common", "常用占卜", "日常随手可玩的小占卜。", listOf(
            sys("today", "今日算命", "当日首抽固化 · 跨天再换", Screen.TodayOracle.route, TodayOracleIcon),
            sys("tarot", "塔罗牌", "78 张牌库，单张 / 三张牌阵", Screen.Tarot.route, TarotIcon),
            sys("crystalball", "水晶球占卜", "凝视象征解读", Screen.CrystalBall.route, CrystalBallIcon),
            sys("kau_cim", "中国灵签", "观音灵签 · 求签解签", "divination/lot/chinese_kau_cim", LotteryStickIcon),
            sys("tibetan_div", "藏传签卜", "佛门签喻 · 吉凶教诫", "divination/lot/tibetan_div", TibetanDivIcon),
            sys("thai_siam", "泰国暹罗签", "金翅鸟 / 白象 / 金龙", "divination/lot/thai_siam", ThaiSiamIcon),
            sys("greek_oracle", "古希腊神谕", "德尔斐箴言", "divination/lot/greek_oracle", GreekOracleIcon),
            sys("bible_lot", "圣经掣签", "乌陵与土明", "divination/lot/bible_lot", BibleLotIcon),
            sys("cowrie", "非洲贝壳占卜", "四贝正反卦象", "divination/lot/cowrie", ShellIcon),
            sys("bibliomancy", "翻书占卜", "经典箴言启示", "divination/lot/bibliomancy", BibliomancyIcon),
            sys("mizu_kuji", "水占卜神签", "贵船神社水签", "divination/lot/mizu_kuji", MizuKujiIcon),
            sys("omikuji", "日本御神签", "大吉至大凶 · 分项运势", "divination/lot/omikuji", OmikujiIcon)
        ))
    ))
)
