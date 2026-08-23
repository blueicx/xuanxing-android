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
    private val SIGN_NAMES = listOf(
        "白羊座", "金牛座", "双子座", "巨蟹座", "狮子座", "处女座",
        "天秤座", "天蝎座", "射手座", "摩羯座", "水瓶座", "双鱼座"
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

    /** 太阳当前星座 index（近似节气法，与小程序对齐） */
    fun sunSignIndex(date: LocalDate): Int {
        val m = date.monthValue
        val d = date.dayOfMonth
        val boundaries = intArrayOf(20, 19, 21, 20, 21, 22, 23, 23, 23, 24, 23, 22)
        return if (d >= boundaries[m - 1]) ((m - 3) % 12 + 12) % 12
        else (((m - 4) % 12) + 12) % 12
    }

    /** 月亮近似位置：2000-01-06 月亮在白羊，27.3天一周 */
    fun moonSignIndex(date: LocalDate): Int {
        val epochMoon = LocalDate.of(2000, 1, 6).toEpochDay()
        val daysSince = date.toEpochDay() - epochMoon
        return ((Math.floor(daysSince / 2.238).toInt() % 12) + 12) % 12
    }

    /** 相位分（占星学标准） */
    fun aspectScore(signA: Int, signB: Int): Pair<Int, String> {
        var dist = Math.abs(signA - signB) % 12
        if (dist > 6) dist = 12 - dist
        return when (dist) {
            0 -> 5 to "合相(强化)"
            1 -> 0 to "半六分(中性)"
            2 -> 10 to "六合(机会)"
            3 -> -12 to "刑相(挑战)"
            4 -> 15 to "三合(和谐)"
            5 -> -3 to "梅花(调适)"
            6 -> -8 to "对冲(极化)"
            else -> 0 to "平"
        }
    }

    /** 西方每周运势 — 用月亮位置做相位分析（与小程序完全对齐） */
    fun generateWeekly(info: ZodiacInfo, date: LocalDate): WesternDailyFortune {
        val userIdx = SIGN_NAMES.indexOf(info.sign).let { if (it < 0) 0 else it }
        val moonIdx = moonSignIndex(date)
        return buildByAspect(userIdx, moonIdx, "本周月亮", date, "week")
    }

    /** 西方每月运势 — 用太阳当前位置做相位分析 */
    fun generateMonthly(info: ZodiacInfo, date: LocalDate): WesternDailyFortune {
        val userIdx = SIGN_NAMES.indexOf(info.sign).let { if (it < 0) 0 else it }
        val sunIdx = sunSignIndex(date)
        return buildByAspect(userIdx, sunIdx, "本月太阳", date, "month")
    }

    private fun buildByAspect(
        userIdx: Int,
        transitIdx: Int,
        transitLabel: String,
        date: LocalDate,
        periodTag: String
    ): WesternDailyFortune {
        val asp = aspectScore(userIdx, transitIdx)
        val overall = (55 + asp.first).coerceIn(20, 95)

        fun cat(base: Int, bonus: Int) = (base + bonus).coerceIn(12, 98)
        val (careerBonus, wealthBonus, loveBonus, healthBonus) = when (asp.second) {
            "三合(和谐)" -> listOf(4, 3, 4, 3)
            "六合(机会)" -> listOf(3, 4, 2, 2)
            "合相(强化)" -> listOf(2, 1, 3, 1)
            "刑相(挑战)" -> listOf(-4, -3, -2, -4)
            "对冲(极化)" -> listOf(-2, -1, -4, -2)
            "梅花(调适)" -> listOf(-1, 0, -1, -1)
            else -> listOf(0, 0, 0, 0)
        }
        val career = cat(overall, careerBonus)
        val wealth = cat(overall, wealthBonus)
        val love = cat(overall, loveBonus)
        val health = cat(overall, healthBonus)

        val userSign = SIGN_NAMES[userIdx]
        val summary = "$userSign 与 $transitLabel(${SIGN_NAMES[transitIdx]})形成${asp.second}(${asp.first})。"

        val luckyNumber = 1 + ((userIdx + transitIdx * 7) % 9)
        val luckyColor = COLOR_POOL[(userIdx + transitIdx) % COLOR_POOL.size]
        val luckyDirection = DIR_POOL[(userIdx * 2 + transitIdx * 3) % DIR_POOL.size]

        return WesternDailyFortune(
            dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
            sign = userSign,
            overallScore = overall,
            careerScore = career,
            wealthScore = wealth,
            loveScore = love,
            healthScore = health,
            summary = summary,
            luckyNumber = luckyNumber,
            luckyColor = luckyColor,
            luckyDirection = luckyDirection,
            period = periodTag
        )
    }
}
