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
    val recordingError = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private val profileHistory = com.xuanji.app.data.local.ProfileTestHistoryStore(com.xuanji.app.data.local.DataStorePreferenceBridge(context))
    fun recordsForProfile(owner: String?): Flow<List<TestRecord>> = if (owner == null) records else context.dataStore.data.map { prefs ->
        runCatching {
            com.xuanji.app.data.local.ProfileTestHistoryStore.decode(prefs[stringPreferencesKey(com.xuanji.app.data.local.ProfileTestHistoryStore.key(owner))], owner)
        }.onFailure { recordingError.value = "当前档案测验记录不可读，原数据未覆盖。" }.getOrDefault(emptyList())
    }

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

    suspend fun addRecord(testName: String, category: String, resultCode: String, resultName: String,
        owner: String? = null, rawScores: Map<String, Int> = emptyMap(), expectedEpoch: Long? = null) {
        val record = TestRecord(
            testName = testName,
            category = category,
            resultCode = resultCode,
            resultName = resultName,
            date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), rawScores = rawScores
        )
        if (owner != null) {
            runCatching { profileHistory.add(owner, record, expectedEpoch) }.onSuccess { recordingError.value = null }
                .onFailure { recordingError.value = "结果未保存：记录已满、数据不可读或写入失败。可先导出再清除。" }
            return
        }
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
