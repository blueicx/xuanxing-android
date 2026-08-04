package com.xuanji.app.domain

import com.xuanji.app.data.model.WesternDailyFortune
import com.xuanji.app.domain.ZodiacCalculator.ZodiacInfo
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * 西方（星座）每日运势生成器。基于日期与星座生成稳定的每日运势。
 */
object WesternFortuneGenerator {

    private val COLOR_POOL = listOf(
        "星空蓝", "暮光紫", "晨曦金", "月光银", "松石绿", "珊瑚红", "雾灰", "琥珀橙"
    )
    private val DIR_POOL = listOf(
        "正东", "东南", "正南", "西南", "正西", "西北", "正北", "东北"
    )
    private val SUMMARY_HIGH = listOf(
        "今天是你闪耀的一天，宇宙为你亮起绿灯。",
        "能量满格，适合推进拖延已久的计划。"
    )
    private val SUMMARY_MID = listOf(
        "平稳的一天，细节里藏着机会。",
        "保持自己的节奏，好运在转角处。"
    )
    private val SUMMARY_LOW = listOf(
        "今天宜放慢脚步，给自己一点喘息。",
        "低能量日，避免重大决定，静待时机。"
    )

    fun generate(info: ZodiacInfo, date: LocalDate): WesternDailyFortune {
        val seed = run {
            var s = date.toEpochDay() xor info.sign.hashCode().toLong()
            if (s == 0L) s = date.toEpochDay()
            s
        }
        val rnd = Random(seed)
        val overall = 42 + rnd.nextInt(53) // 42..94

        fun cat(): Int = (overall + rnd.nextInt(25) - 12).coerceIn(12, 98)
        val career = cat()
        val wealth = cat()
        val love = cat()
        val health = cat()

        val summary = when {
            overall >= 75 -> SUMMARY_HIGH[rnd.nextInt(SUMMARY_HIGH.size)]
            overall >= 50 -> SUMMARY_MID[rnd.nextInt(SUMMARY_MID.size)]
            else -> SUMMARY_LOW[rnd.nextInt(SUMMARY_LOW.size)]
        }

        val luckyNumber = 1 + rnd.nextInt(9)
        val luckyColor = COLOR_POOL[rnd.nextInt(COLOR_POOL.size)]
        val luckyDirection = DIR_POOL[rnd.nextInt(DIR_POOL.size)]

        return WesternDailyFortune(
            dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
            sign = info.sign,
            overallScore = overall,
            careerScore = career,
            wealthScore = wealth,
            loveScore = love,
            healthScore = health,
            summary = summary,
            luckyNumber = luckyNumber,
            luckyColor = luckyColor,
            luckyDirection = luckyDirection
        )
    }
}
