package com.xuanji.app.domain.divination

import com.xuanji.app.data.model.UserProfile
import java.time.LocalDate

data class NumerologyCycle(
    val label: String,
    val number: Int,
    val calculation: String,
    val interpretation: String
)

/**
 * 常见的个人年/月/日周期。它是可复核的数字游戏，不替代现实规划。
 * 与基础生命路径共用 11/22/33 大师数保留规则。
 */
object NumerologyCycles {
    fun calculate(profile: UserProfile, date: LocalDate): List<NumerologyCycle> {
        val month = reduce(date.monthValue)
        val day = reduce(date.dayOfMonth)
        val year = reduce(date.year)
        val birthMonth = reduce(profile.birthMonth)
        val birthDay = reduce(profile.birthDay)
        val birthYear = reduce(profile.birthYear)
        val personalYear = reduce(birthMonth + birthDay + year)
        val personalMonth = reduce(personalYear + month)
        val personalDay = reduce(personalMonth + day)
        return listOf(
            NumerologyCycle("个人年", personalYear, "$birthMonth+$birthDay+$year", text(personalYear)),
            NumerologyCycle("个人月", personalMonth, "$personalYear+$month", text(personalMonth)),
            NumerologyCycle("个人日", personalDay, "$personalMonth+$day", text(personalDay)),
            NumerologyCycle("出生年根数", birthYear, "${profile.birthYear} → $birthYear", text(birthYear))
        )
    }

    private fun reduce(value: Int): Int {
        var result = value.toString().filter(Char::isDigit).sumOf { it.digitToInt() }
        while (result > 9 && result !in setOf(11, 22, 33)) {
            result = result.toString().sumOf { it.digitToInt() }
        }
        return result
    }

    private fun text(number: Int): String = when (number) {
        1 -> "启动与自我负责：适合定一个可完成的起点。"
        2 -> "协作与耐心：适合沟通、倾听和修复关系。"
        3 -> "表达与创意：适合写作、分享和轻量尝试。"
        4 -> "秩序与积累：适合整理、复盘和建立流程。"
        5 -> "变化与探索：适合在可控范围内换个方法。"
        6 -> "照顾与承诺：先照看重要的人和生活底盘。"
        7 -> "研究与内省：给自己一段安静、深度的时间。"
        8 -> "资源与执行：把目标拆成预算、步骤和边界。"
        9 -> "收束与分享：完成旧事，再把经验交给下一步。"
        11 -> "直觉与灵感：记录想法，再用事实验证。"
        22 -> "长期构筑：小步搭建，不用一次完成宏大目标。"
        33 -> "照顾与教化：帮助别人前，先留出自己的余量。"
        else -> "保持观察，把象征当作自我反思入口。"
    }
}
