package com.xuanji.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

/**
 * 字符串键值读写的接缝。有了它，「存进去了吗」「清掉了吗」才能在 JVM 测试里被真的验证，
 * 而不是只证明一段 JSON 会编解码。
 */
interface PreferenceBridge {
    suspend fun read(key: String): String?

    suspend fun write(key: String, value: String)

    suspend fun delete(key: String)
}

class DataStorePreferenceBridge(private val context: Context) : PreferenceBridge {

    override suspend fun read(key: String): String? =
        context.dataStore.data.first()[stringPreferencesKey(key)]

    override suspend fun write(key: String, value: String) {
        val preferenceKey = stringPreferencesKey(key)
        context.dataStore.edit { it[preferenceKey] = value }
    }

    override suspend fun delete(key: String) {
        val preferenceKey = stringPreferencesKey(key)
        context.dataStore.edit { it.remove(preferenceKey) }
    }
}
