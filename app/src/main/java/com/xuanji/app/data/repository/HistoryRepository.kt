package com.xuanji.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xuanji.app.R
import com.xuanji.app.data.model.HistoryEvent
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 历史上的今天：从打包的 res/raw/history.json 读取事件，
 * 按当日月日过滤，缺省时按日期哈希确定性地挑选若干条，保证每天都有内容。
 */
class HistoryRepository(context: Context) {

    private val allEvents: List<HistoryEvent> = load(context)

    private fun load(context: Context): List<HistoryEvent> {
        val json = context.resources.openRawResource(R.raw.history)
            .bufferedReader()
            .use { it.readText() }
        val type = object : TypeToken<List<HistoryEvent>>() {}.type
        return Gson().fromJson(json, type) ?: emptyList()
    }

    fun eventsForToday(): List<HistoryEvent> {
        val today = LocalDate.now()
        val todayKey = today.format(DateTimeFormatter.ofPattern("MM-dd"))
        val matched = allEvents.filter { it.date == todayKey }
        if (matched.isNotEmpty()) return matched

        // 当天无专属事件：从「今天 ±5 天」范围内按年内第几天确定性挑选 2 条。
        // 保证日期贴近今天，不会挑出「冬至」这类日期错乱的事件。
        val windowKeys = (-5..5)
            .map { today.plusDays(it.toLong()).format(DateTimeFormatter.ofPattern("MM-dd")) }
            .toSet()
        val windowPool = allEvents.filter { it.date in windowKeys && it.tag.isNotEmpty() }
        if (windowPool.isNotEmpty()) {
            val dayHash = today.dayOfYear
            return (0 until windowPool.size)
                .map { windowPool[(it * 7 + dayHash) % windowPool.size] }
                .distinct()
                .take(2)
        }

        // 极端兜底：才从全部事件中挑选（仍按确定性）
        val pool = allEvents.filter { it.tag.isNotEmpty() }.ifEmpty { allEvents }
        if (pool.isEmpty()) return emptyList()
        val dayHash = today.dayOfYear
        return (0 until pool.size)
            .map { pool[(it * 7 + dayHash) % pool.size] }
            .distinct()
            .take(2)
    }
}
