package com.xuanji.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.xuanji.app.domain.game.GameRecord
import com.xuanji.app.domain.game.GameSave
import java.security.MessageDigest
import kotlinx.coroutines.flow.first

/**
 * One resumable board archive and one scoreboard per profile. Profile keys are hashed the
 * same way as the companion visit memory, and only [GameSave] / [GameRecord] cross this
 * boundary: board data, never character commentary.
 */
class GameArchiveStore(private val context: Context) {

    private val gson = Gson()

    suspend fun load(profileKey: String): GameSave? =
        read(saveKey(profileKey))?.let { json ->
            runCatching { gson.fromJson(json, GameSave::class.java) }.getOrNull()
        }

    suspend fun save(profileKey: String, save: GameSave) {
        write(saveKey(profileKey), gson.toJson(save))
    }

    suspend fun loadRecord(profileKey: String): GameRecord =
        read(recordKey(profileKey))?.let { json ->
            runCatching { gson.fromJson(json, GameRecord::class.java) }.getOrNull()
        } ?: GameRecord()

    /** Tally one settled result and return the scoreboard as stored afterwards. */
    suspend fun record(profileKey: String, result: String?): GameRecord {
        val current = loadRecord(profileKey)
        val next = current.tally(result)
        if (next == current) return current
        write(recordKey(profileKey), gson.toJson(next))
        return next
    }

    private suspend fun read(key: String): String? = context.dataStore.data.first()[stringPreferencesKey(key)]

    private suspend fun write(key: String, value: String) {
        val preferenceKey = stringPreferencesKey(key)
        context.dataStore.edit { it[preferenceKey] = value }
    }

    private fun saveKey(profileKey: String): String = "game_save_${fingerprint(profileKey)}"

    private fun recordKey(profileKey: String): String = "game_record_${fingerprint(profileKey)}"

    private fun fingerprint(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
