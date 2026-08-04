package com.xuanji.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xuanji.app.R
import com.xuanji.app.data.model.ReferenceSystem

/** 全球玄学体系资料库（不可离线算法化的体系），来自 res/raw/systems_reference.json */
class ReferenceRepository(private val context: Context) {

    private val gson = Gson()

    fun loadAll(): List<ReferenceSystem> {
        val json = context.resources.openRawResource(R.raw.systems_reference).bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<ReferenceSystem>>() {}.type
        return gson.fromJson(json, type)
    }

    fun getByKey(key: String): ReferenceSystem? = loadAll().firstOrNull { it.key == key }
}
