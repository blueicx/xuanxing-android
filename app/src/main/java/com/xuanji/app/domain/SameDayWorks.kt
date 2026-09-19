package com.xuanji.app.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 同日生页使用的音乐与诗歌作品目录。
 *
 * 目录只保存作品元数据；公版作品可以展示短摘录，仍受本地文案安全边界约束。
 * 非公版作品永远不保存或返回正文，避免把版权内容伪装成摘录。
 */
enum class WorkKind {
    MUSIC,
    POETRY
}

data class SameDayWork(
    val monthDay: String,
    val kind: WorkKind,
    val title: String,
    val creator: String,
    val year: Int?,
    val style: String,
    val publicDomain: Boolean,
    val excerpt: String? = null,
    val note: String
)

object SameDayWorks {
    private val monthDayFormatter = DateTimeFormatter.ofPattern("MM-dd")

    val CATALOG: List<SameDayWork> = listOf(
        SameDayWork(
            monthDay = "08-31",
            kind = WorkKind.MUSIC,
            title = "月光",
            creator = "德彪西",
            year = 1890,
            style = "印象主义钢琴",
            publicDomain = true,
            note = "适合把一天收束下来；作品本身不替你给出答案。"
        ),
        SameDayWork(
            monthDay = "08-31",
            kind = WorkKind.POETRY,
            title = "春晓",
            creator = "孟浩然",
            year = 0,
            style = "唐诗",
            publicDomain = true,
            excerpt = "春眠不觉晓，处处闻啼鸟。",
            note = "公版短摘录；可以把它当作今天的节奏提示。"
        ),
        SameDayWork(
            monthDay = "08-31",
            kind = WorkKind.MUSIC,
            title = "夜曲 Op.9 No.2",
            creator = "肖邦",
            year = 1832,
            style = "浪漫主义钢琴",
            publicDomain = true,
            note = "轻声聆听即可，不必把它当成命运结论。"
        ),
        SameDayWork(
            monthDay = "08-31",
            kind = WorkKind.MUSIC,
            title = "午夜电台",
            creator = "独立音乐目录",
            year = 2024,
            style = "当代氛围音乐",
            publicDomain = false,
            note = "仅展示作品信息；请通过正版平台收听。"
        )
    )

    /** 按月日确定性返回作品；无专属条目时从完整目录稳定挑选。 */
    fun forDate(date: LocalDate): List<SameDayWork> {
        val monthDay = date.format(monthDayFormatter)
        val matched = CATALOG.filter { it.monthDay == monthDay }
        if (matched.isNotEmpty()) return matched

        val start = Math.floorMod(date.dayOfYear * 31 + date.year, CATALOG.size)
        return CATALOG.indices
            .map { CATALOG[(start + it * 3) % CATALOG.size] }
            .distinctBy { it.title }
            .take(3)
    }
}
