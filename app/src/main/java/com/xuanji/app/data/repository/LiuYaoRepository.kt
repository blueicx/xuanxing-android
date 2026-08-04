package com.xuanji.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xuanji.app.R
import com.xuanji.app.data.model.Hexagram

/** 周易 64 卦库（来自 res/raw/hexagrams.json，京房八宫推导） */
class LiuYaoRepository(private val context: Context) {

    private val gson = Gson()

    fun loadHexagrams(): List<Hexagram> {
        val json = context.resources.openRawResource(R.raw.hexagrams).bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<Hexagram>>() {}.type
        return gson.fromJson(json, type)
    }
}
