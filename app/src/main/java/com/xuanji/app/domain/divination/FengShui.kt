package com.xuanji.app.domain.divination

import com.xuanji.app.data.model.UserProfile

/**
 * 八宅风水：
 * - 命卦：由出生年 + 性别推东四命 / 西四命
 * - 坐向：以坐山为伏位，依「大游年歌」排出八游星（四吉：伏位/生气/延年/天医；四凶：绝命/五鬼/六煞/祸害）到八卦方位
 * 离线确定性推算，仅供文化娱乐参考。
 */
object FengShui {

    /** 八卦方位（卦 → 方位中文） */
    val DIR = mapOf(
        "坎" to "北", "艮" to "东北", "震" to "东", "巽" to "东南",
        "离" to "南", "坤" to "西南", "兑" to "西", "乾" to "西北"
    )

    /** 八宅大游年歌：坐山(伏位) → 该卦方位所临游星 */
    private val YOU_XING = mapOf(
        "坎" to mapOf("坎" to "伏位", "巽" to "生气", "离" to "延年", "震" to "天医", "坤" to "绝命", "兑" to "五鬼", "乾" to "六煞", "艮" to "祸害"),
        "离" to mapOf("离" to "伏位", "震" to "生气", "坎" to "延年", "巽" to "天医", "乾" to "绝命", "艮" to "五鬼", "兑" to "六煞", "坤" to "祸害"),
        "震" to mapOf("震" to "伏位", "离" to "生气", "巽" to "延年", "坎" to "天医", "兑" to "绝命", "坤" to "五鬼", "艮" to "六煞", "乾" to "祸害"),
        "巽" to mapOf("巽" to "伏位", "坎" to "生气", "震" to "延年", "离" to "天医", "艮" to "绝命", "乾" to "五鬼", "坤" to "六煞", "兑" to "祸害"),
        "乾" to mapOf("乾" to "伏位", "兑" to "生气", "坤" to "延年", "艮" to "天医", "离" to "绝命", "震" to "五鬼", "巽" to "六煞", "坎" to "祸害"),
        "坤" to mapOf("坤" to "伏位", "艮" to "生气", "乾" to "延年", "兑" to "天医", "坎" to "绝命", "巽" to "五鬼", "震" to "六煞", "离" to "祸害"),
        "兑" to mapOf("兑" to "伏位", "乾" to "生气", "艮" to "延年", "坤" to "天医", "震" to "绝命", "坎" to "五鬼", "离" to "六煞", "巽" to "祸害"),
        "艮" to mapOf("艮" to "伏位", "坤" to "生气", "兑" to "延年", "乾" to "天医", "巽" to "绝命", "离" to "五鬼", "坎" to "六煞", "震" to "祸害")
    )

    private val NUM_TO_GUA = mapOf(1 to "坎", 2 to "坤", 3 to "震", 4 to "巽", 6 to "乾", 7 to "兑", 8 to "艮", 9 to "离")

    /** 八坐山选项 */
    val ZUO_SHAN = listOf("坎", "坤", "震", "巽", "乾", "兑", "艮", "离")

    data class Palace(
        val gua: String,
        val dir: String,
        val star: String,
        val lucky: Boolean
    )

    data class FengShuiResult(
        val mingGua: String,        // 命卦（如 坎）
        val dongXi: String,         // 东四命 / 西四命
        val zuoShan: String,        // 坐山
        val palaces: List<Palace>,  // 8 方位
        val match: String,          // 宅命匹配说明
        val verdict: String         // 六维解读（总评/事业/财运/感情/健康/建议）
    )

    /** 命卦：返回 1-9（5 已按性别转为 坤/艮） */
    fun mingGuaNumber(year: Int, gender: String?): Int {
        val y = year % 100
        val isFemale = gender == "女"
        val sum = if (year >= 2000) {
            if (isFemale) y + 6 else 99 - y
        } else {
            if (isFemale) y + 5 else 100 - y
        }
        var n = sum % 9
        if (n == 0) n = 9
        if (n == 5) n = if (isFemale) 8 else 2 // 5 寄宫：男坤、女艮
        return n
    }

    fun dongXiMing(mingGua: String): String =
        if (mingGua in listOf("坎", "离", "震", "巽")) "东四命" else "西四命"

    fun cast(profile: UserProfile, zuoShan: String): FengShuiResult {
        val num = mingGuaNumber(profile.birthYear, profile.gender)
        val mingGua = NUM_TO_GUA[num] ?: "坎"
        val dx = dongXiMing(mingGua)
        val youXing = YOU_XING.getValue(zuoShan)
        val order = listOf("坎", "艮", "震", "巽", "离", "坤", "兑", "乾")
        val palaces = order.map { g ->
            val star = youXing.getValue(g)
            Palace(g, DIR.getValue(g), star, star in listOf("伏位", "生气", "延年", "天医"))
        }
        val zhaiDongXi = dongXiMing(zuoShan)
        val match = if (dx == zhaiDongXi) "你的命卦（$dx）与本宅（${zhaiDongXi}）同属，宅命相配，大吉。"
        else "你的命卦为$dx，本宅为${zhaiDongXi}，宅命不同属，宜多用本命四吉方。"
        return FengShuiResult(mingGua, dx, zuoShan, palaces, match, buildVerdict(dx, zhaiDongXi, palaces))
    }

    /** 六维解读：按命卦、坐山与八游星落位生成（离线确定性） */
    private fun buildVerdict(
        dongXi: String,
        zhaiDongXi: String,
        palaces: List<Palace>
    ): String {
        fun starAt(s: String): Palace = palaces.first { it.star == s }
        val shengQi = starAt("生气")
        val tianYi = starAt("天医")
        val yanNian = starAt("延年")
        val jueMing = starAt("绝命")
        val wuGui = starAt("五鬼")
        val liuSha = starAt("六煞")
        val huoHai = starAt("祸害")
        val matched = dongXi == zhaiDongXi

        val zong = "总评：命卦属「$dongXi」，本宅坐山为「$zhaiDongXi」宅，${if (matched) "宅命同属，气场相合，居家诸事顺遂。" else "宅命不同属，需借四吉方补益气场，方能安居。"}"
        val career = "事业：生气星落${shengQi.dir}（${shengQi.gua}）方为事业财源之枢，宜在此方设办公桌案、多朝其坐卧，利进取招财。"
        val wealth = "财运：生气亦主财禄，${shengQi.dir}方宜多经营走动；六煞落${liuSha.dir}主破财漏耗，忌在此方设钱柜、灶台与水口。"
        val love = "感情：延年星落${yanNian.dir}（${yanNian.gua}）方主婚姻情缘，卧室床头宜向此方；命主多在此方活动，可增夫妻和合之气。"
        val health = "健康：天医星落${tianYi.dir}（${tianYi.gua}）方主健康疗愈，宜作休养之所；绝命落${jueMing.dir}主病厄，忌置卧床与厨房，宜化凶为安。"
        val advice = "建议：${if (matched) "宅命相配，可安心居住，日常多往来于四吉方（生气/延年/天医/伏位）。" else "宅命不同属，宜多用本命四吉方，并避开绝命、五鬼等凶方。"}五鬼落${wuGui.dir}主官非小人、祸害落${huoHai.dir}主是非小伤，此二方宜静置杂物、勿作主位。"
        return listOf(zong, career, wealth, love, health, advice).joinToString("\n")
    }
}
