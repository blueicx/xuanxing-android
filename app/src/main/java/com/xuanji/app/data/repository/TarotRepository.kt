package com.xuanji.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xuanji.app.R
import com.xuanji.app.data.model.TarotCard

/** 塔罗牌库（78 张，来自 res/raw/tarot.json） */
class TarotRepository(private val context: Context) {

    private val gson = Gson()

    fun loadDeck(): List<TarotCard> {
        val json = context.resources.openRawResource(R.raw.tarot).bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<TarotCard>>() {}.type
        return gson.fromJson(json, type)
    }
}
