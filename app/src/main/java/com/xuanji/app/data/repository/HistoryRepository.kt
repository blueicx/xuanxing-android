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
 * 严格按当日月日（MM-dd）过滤，只展示当天发生的事件。
 * 无匹配时返回空列表，由 UI 显示空态，绝不以邻近日期或其他事件凑数。
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
        // 严格只匹配当天月日，无匹配即返回空（由 UI 显示空态）
        return allEvents.filter { it.date == todayKey }
    }
}
