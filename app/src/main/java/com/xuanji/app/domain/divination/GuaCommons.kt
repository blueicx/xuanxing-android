package com.xuanji.app.domain.divination

/**
 * 八卦与六十四卦公共工具：梅花易数、奇门遁甲等复用。
 * 二进制约定：6 位字符串，自下而上（初爻→上爻），阳=1 阴=0。
 */
object GuaCommons {

    data class Trigram(
        val cn: String,
        val symbol: String,
        val value: Int,     // 先天数 1-8
        val wx: String,     // 五行
        val lines: List<Boolean> // 自下而上 3 爻，true=阳
    )

    val TRIGRAMS = listOf(
        Trigram("乾", "☰", 1, "金", listOf(true, true, true)),
        Trigram("兑", "☱", 2, "金", listOf(true, true, false)),
        Trigram("离", "☲", 3, "火", listOf(true, false, true)),
        Trigram("震", "☳", 4, "木", listOf(true, false, false)),
        Trigram("巽", "☴", 5, "木", listOf(false, true, true)),
        Trigram("坎", "☵", 6, "水", listOf(false, true, false)),
        Trigram("艮", "☶", 7, "土", listOf(false, false, true)),
        Trigram("坤", "☷", 8, "土", listOf(false, false, false))
    )

    /** 六十四卦名（二进制自下而上 → 卦名），来自 res/raw/hexagrams.json */
    val BINARY_TO_NAME = mapOf(
        "000000" to "坤", "000001" to "剥", "000010" to "比", "000011" to "观",
        "000100" to "豫", "000101" to "晋", "000110" to "萃", "000111" to "否",
        "001000" to "谦", "001001" to "艮", "001010" to "蹇", "001011" to "渐",
        "001100" to "小过", "001101" to "旅", "001110" to "咸", "001111" to "遁",
        "010000" to "师", "010001" to "蒙", "010010" to "坎", "010011" to "涣",
        "010100" to "解", "010101" to "未济", "010110" to "困", "010111" to "讼",
        "011000" to "升", "011001" to "蛊", "011010" to "井", "011011" to "巽",
        "011100" to "恒", "011101" to "鼎", "011110" to "大过", "011111" to "姤",
        "100000" to "复", "100001" to "颐", "100010" to "屯", "100011" to "益",
        "100100" to "震", "100101" to "噬嗑", "100110" to "随", "100111" to "无妄",
        "101000" to "明夷", "101001" to "贲", "101010" to "既济", "101011" to "家人",
        "101100" to "丰", "101101" to "离", "101110" to "革", "101111" to "同人",
        "110000" to "临", "110001" to "损", "110010" to "节", "110011" to "中孚",
        "110100" to "归妹", "110101" to "睽", "110110" to "兑", "110111" to "履",
        "111000" to "泰", "111001" to "大畜", "111010" to "需", "111011" to "小畜",
        "111100" to "大壮", "111101" to "大有", "111110" to "夬", "111111" to "乾"
    )

    /** 先天数(1-8) → 八卦；0 视为 8(坤) */
    fun trigramByValue(v: Int): Trigram {
        val n = ((v % 8) + 8) % 8
        val idx = if (n == 0) 8 else n
        return TRIGRAMS.first { it.value == idx }
    }

    /** 由上下卦得到六爻二进制（自下而上） */
    fun toBinary(lower: Trigram, upper: Trigram): String =
        (lower.lines + upper.lines).joinToString("") { if (it) "1" else "0" }

    /** 卦名（含上下卦文字） */
    fun guaName(lower: Trigram, upper: Trigram): String {
        val binary = toBinary(lower, upper)
        val name = BINARY_TO_NAME[binary] ?: "未知卦"
        return "${upper.cn}上${lower.cn}下 · 《$name》"
    }

    fun guaNameByBinary(binary: String): String = BINARY_TO_NAME[binary] ?: "未知卦"

    /** 五行生克：以 a 为体、b 为用 */
    fun wxRelation(a: String, b: String): String {
        val SHENG = mapOf("木" to "火", "火" to "土", "土" to "金", "金" to "水", "水" to "木")
        val KE = mapOf("木" to "土", "土" to "水", "水" to "火", "火" to "金", "金" to "木")
        return when {
            a == b -> "比和（体用同心，事易成，吉）"
            SHENG[a] == b -> "体生用（我生，耗力付出，小凶）"
            KE[a] == b -> "体克用（我克，可控可得，吉）"
            SHENG[b] == a -> "用生体（生我，得外力相助，大吉）"
            KE[b] == a -> "用克体（克我，受制受阻，凶）"
            else -> "无生克（平）"
        }
    }
}
