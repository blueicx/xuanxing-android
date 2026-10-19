package com.xuanji.app.data.local

import com.xuanji.app.domain.action.ActivityPreference
import com.xuanji.app.domain.action.EnergyLevel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionConstraintStoreTest {
    private class MemoryBridge : PreferenceBridge {
        val values = mutableMapOf<String, String>()
        override suspend fun read(key: String): String? = values[key]
        override suspend fun write(key: String, value: String) { values[key] = value }
        override suspend fun delete(key: String) { values.remove(key) }
    }

    @Test
    fun activity_preferences_round_trip_and_are_profile_scoped() = runTest {
        val bridge = MemoryBridge()
        val store = ActionConstraintStore(bridge)
        val preference = ActivityPreference(EnergyLevel.Low, indoorOnly = true, maxDurationMinutes = 35)

        store.save("profile-a", preference)

        assertEquals(preference, store.read("profile-a"))
        assertEquals(ActivityPreference(), store.read("profile-b"))
        assertNotEquals(store.memoryKey("profile-a"), store.memoryKey("profile-b"))
    }

    @Test
    fun malformed_or_out_of_range_preferences_fall_back_safely() = runTest {
        val bridge = MemoryBridge()
        val store = ActionConstraintStore(bridge)
        bridge.values[store.memoryKey("profile-a")] = "{broken"
        assertTrue(store.readSnapshot("profile-a").unreadable)
        store.save("profile-a", ActivityPreference(maxDurationMinutes = 999))
        assertEquals(480, store.read("profile-a").maxDurationMinutes)
    }
}
