package com.xuanji.app.data.local

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/** 全局唯一的 Preferences DataStore 实例 */
val Context.dataStore by preferencesDataStore(name = "xuanji_prefs")
