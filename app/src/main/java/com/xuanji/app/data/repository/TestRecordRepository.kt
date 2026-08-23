package com.xuanji.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xuanji.app.data.local.dataStore
import com.xuanji.app.data.model.TestRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 测试记录仓库：用 DataStore 持久化每次测试的结果，供「测试记录」板块分组展示。
 * 离线、确定性。
 */
class TestRecordRepository(private val context: Context) {

    private val gson = Gson()
    private val key = stringPreferencesKey("test_records")

    /** 记录列表（按时间倒序，最新在前） */
    val records: Flow<List<TestRecord>> = context.dataStore.data.map { prefs ->
        val json = prefs[key] ?: return@map emptyList()
        val type = object : TypeToken<List<TestRecord>>() {}.type
        try {
            (gson.fromJson<List<TestRecord>>(json, type) ?: emptyList()).reversed()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun addRecord(testName: String, category: String, resultCode: String, resultName: String) {
        val record = TestRecord(
            testName = testName,
            category = category,
            resultCode = resultCode,
            resultName = resultName,
            date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        )
        context.dataStore.edit { prefs ->
            val oldJson = prefs[key] ?: ""
            val old = try {
                val t = object : TypeToken<List<TestRecord>>() {}.type
                gson.fromJson<List<TestRecord>>(oldJson, t) ?: emptyList()
            } catch (_: Exception) { emptyList() }
            val merged = (listOf(record) + old).take(20)  // 对齐小程序，最多保留 20 条
            prefs[key] = gson.toJson(merged)
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.remove(key) }
    }
}
