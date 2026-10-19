package com.xuanji.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

/**
 * 字符串键值读写的接缝。有了它，「存进去了吗」「清掉了吗」才能在 JVM 测试里被真的验证，
 * 而不是只证明一段 JSON 会编解码。
 */
interface PreferenceBridge {
    val atomicBatch: Boolean get() = false
    suspend fun read(key: String): String?

    suspend fun write(key: String, value: String)

    suspend fun delete(key: String)

    /** Native stores override with one atomic transaction; fallback is journal-protected by caller. */
    suspend fun compareAndWriteBatch(expected: Map<String, String?>, changes: Map<String, String?>): Boolean {
        if (expected.any { (key, value) -> read(key) != value }) return false
        changes.forEach { (key, value) -> if (value == null) delete(key) else write(key, value) }
        return true
    }
}

class DataStorePreferenceBridge(private val store: DataStore<Preferences>) : PreferenceBridge {
    constructor(context: Context) : this(context.applicationContext.dataStore)
    override val atomicBatch: Boolean get() = true

    override suspend fun read(key: String): String? =
        store.data.first()[stringPreferencesKey(key)]

    override suspend fun write(key: String, value: String) {
        val preferenceKey = stringPreferencesKey(key)
        store.edit { it[preferenceKey] = value }
    }

    override suspend fun delete(key: String) {
        val preferenceKey = stringPreferencesKey(key)
        store.edit { it.remove(preferenceKey) }
    }

    override suspend fun compareAndWriteBatch(expected: Map<String, String?>, changes: Map<String, String?>): Boolean {
        var committed = false
        store.edit { preferences ->
            if (expected.all { (key, value) -> preferences[stringPreferencesKey(key)] == value }) {
                changes.forEach { (key, value) ->
                    val preferenceKey = stringPreferencesKey(key)
                    if (value == null) preferences.remove(preferenceKey) else preferences[preferenceKey] = value
                }
                committed = true
            }
        }
        return committed
    }
}
