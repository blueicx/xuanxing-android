package com.xuanji.app.data.local

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xuanji.app.domain.action.ActionFeedback
import java.security.MessageDigest

/** Explicit action feedback storage; never inferred from passive behavior. */
class ActionFeedbackDataStore(private val bridge: PreferenceBridge) {
    private val gson = Gson()
    private val type = object : TypeToken<List<ActionFeedback>>() {}.type

    suspend fun list(profileKey: String, limit: Int = 100): List<ActionFeedback> {
        val raw = bridge.read(key(profileKey)) ?: return emptyList()
        return runCatching { gson.fromJson<List<ActionFeedback>>(raw, type).orEmpty() }
            .getOrDefault(emptyList())
            .asReversed()
            .take(limit.coerceAtLeast(0))
    }

    suspend fun append(profileKey: String, feedback: ActionFeedback) {
        val current = list(profileKey, 100).asReversed().toMutableList()
        current.removeAll { it.id == feedback.id }
        current += feedback
        bridge.write(key(profileKey), gson.toJson(current.takeLast(100)))
    }

    suspend fun clear(profileKey: String) = bridge.delete(key(profileKey))

    fun key(profileKey: String): String = KEY_PREFIX + fingerprint(profileKey)

    companion object {
        const val KEY_PREFIX = "action_feedback_"
        fun fingerprint(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}

fun android.content.Context.actionFeedbackStore(): ActionFeedbackDataStore =
    ActionFeedbackDataStore(DataStorePreferenceBridge(this))
