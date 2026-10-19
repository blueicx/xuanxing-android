package com.xuanji.app.data.local

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.xuanji.app.domain.action.ActivityPreference
import com.xuanji.app.domain.action.EnergyLevel
import java.security.MessageDigest

data class ActionPreferenceSnapshot(
    val preference: ActivityPreference = ActivityPreference(),
    val unreadable: Boolean = false
)

/** Stores only the activity constraints a user explicitly selected, isolated by profile. */
class ActionConstraintStore(private val bridge: PreferenceBridge) {
    suspend fun hasStored(profileKey: String): Boolean = bridge.read(memoryKey(profileKey)) != null
    suspend fun read(profileKey: String): ActivityPreference = readSnapshot(profileKey).preference

    suspend fun readSnapshot(profileKey: String): ActionPreferenceSnapshot {
        if (profileKey.isBlank()) return ActionPreferenceSnapshot(unreadable = true)
        val raw = bridge.read(memoryKey(profileKey)) ?: return ActionPreferenceSnapshot()
        val parsed = runCatching { JsonParser.parseString(raw) }.getOrNull()
            ?: return ActionPreferenceSnapshot(unreadable = true)
        if (!parsed.isJsonObject) return ActionPreferenceSnapshot(unreadable = true)
        val root = parsed.asJsonObject
        if (root.intValue("version") != VERSION || root.stringValue("profileDigest") != fingerprint(profileKey)) {
            return ActionPreferenceSnapshot(unreadable = true)
        }
        return runCatching {
            val value = root.getAsJsonObject("preference") ?: error("missing_preference")
            val energyName = value.stringValue("energy")
            val energy = energyName?.let { name -> EnergyLevel.entries.firstOrNull { it.name == name } }
                ?: if (energyName == null) EnergyLevel.Any else error("invalid_energy")
            val indoorOnly = value.booleanValue("indoorOnly") ?: false
            val duration = value.intValue("maxDurationMinutes")?.coerceIn(5, 480)
            ActionPreferenceSnapshot(ActivityPreference(energy, indoorOnly, duration))
        }.getOrElse { ActionPreferenceSnapshot(unreadable = true) }
    }

    suspend fun save(profileKey: String, preference: ActivityPreference) {
        require(profileKey.isNotBlank())
        val root = JsonObject().apply {
            addProperty("version", VERSION)
            addProperty("profileDigest", fingerprint(profileKey))
            add("preference", JsonObject().apply {
                addProperty("energy", preference.energy.name)
                addProperty("indoorOnly", preference.indoorOnly)
                preference.maxDurationMinutes?.let { addProperty("maxDurationMinutes", it.coerceIn(5, 480)) }
            })
        }
        bridge.write(memoryKey(profileKey), root.toString())
    }

    suspend fun clear(profileKey: String) {
        if (profileKey.isNotBlank()) bridge.delete(memoryKey(profileKey))
    }

    fun memoryKey(profileKey: String): String = KEY_PREFIX + fingerprint(profileKey)

    private fun JsonObject.intValue(name: String): Int? = runCatching { get(name)?.asInt }.getOrNull()
    private fun JsonObject.stringValue(name: String): String? = runCatching { get(name)?.asString }.getOrNull()
    private fun JsonObject.booleanValue(name: String): Boolean? = runCatching { get(name)?.asBoolean }.getOrNull()

    companion object {
        const val VERSION = 1
        const val KEY_PREFIX = "activity_preference_"

        private fun fingerprint(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
