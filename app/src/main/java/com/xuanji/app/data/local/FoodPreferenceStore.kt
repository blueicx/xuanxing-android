package com.xuanji.app.data.local

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.xuanji.app.domain.action.FoodPreference
import java.security.MessageDigest

data class FoodPreferenceSnapshot(
    val preference: FoodPreference = FoodPreference(),
    val unreadable: Boolean = false
)

class FoodPreferenceStore(private val bridge: PreferenceBridge) {
    private val gson = Gson()
    suspend fun hasStored(profileKey: String): Boolean = bridge.read(memoryKey(profileKey)) != null

    suspend fun read(profileKey: String): FoodPreference = readSnapshot(profileKey).preference

    suspend fun readSnapshot(profileKey: String): FoodPreferenceSnapshot {
        val json = bridge.read(memoryKey(profileKey)) ?: return FoodPreferenceSnapshot()
        val parsed = runCatching { JsonParser.parseString(json) }.getOrNull()
            ?: return FoodPreferenceSnapshot(unreadable = true)
        if (!parsed.isJsonObject) return FoodPreferenceSnapshot(unreadable = true)
        val root = parsed.asJsonObject
        return runCatching {
            require(root.get("profileKeyDigest")?.asString == fingerprint(profileKey))
            require(root.get("version")?.asInt in 1..VERSION)
            val preference = com.xuanji.app.domain.action.FoodPreferenceCodec.decode(root.get("preference"))
            FoodPreferenceSnapshot(preference = preference)
        }.getOrElse { FoodPreferenceSnapshot(unreadable = true) }
    }

    suspend fun save(profileKey: String, preference: FoodPreference) {
        val root = JsonObject()
        root.addProperty("version", VERSION)
        root.addProperty("profileKeyDigest", fingerprint(profileKey))
        val clean = preference.copy(
            excludedIngredients = preference.excludedIngredients
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .take(MAX_EXCLUDED)
                .toSet(),
            allergens = preference.allergens
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .take(MAX_EXCLUDED)
                .toSet(),
            maxMealBudgetCents = preference.maxMealBudgetCents?.coerceIn(100, 1_000_000),
            maxPrepMinutes = preference.maxPrepMinutes?.coerceIn(5, 240)
        )
        root.add("preference", gson.toJsonTree(clean))
        bridge.write(memoryKey(profileKey), root.toString())
    }

    suspend fun clear(profileKey: String) {
        bridge.delete(memoryKey(profileKey))
    }

    fun memoryKey(profileKey: String): String = KEY_PREFIX + fingerprint(profileKey)

    companion object {
        const val VERSION = 2
        const val MAX_EXCLUDED = 12
        const val KEY_PREFIX = "food_preference_"

        fun fingerprint(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
