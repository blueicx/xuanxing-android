package com.xuanji.app.data.local

import com.xuanji.app.domain.action.FoodPreference
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodPreferenceStoreTest {
    private class FakePreferenceBridge : PreferenceBridge {
        val values = linkedMapOf<String, String>()
        val deleted = mutableListOf<String>()

        override suspend fun read(key: String): String? = values[key]
        override suspend fun write(key: String, value: String) { values[key] = value }
        override suspend fun delete(key: String) { deleted += key; values.remove(key) }
    }

    @Test
    fun preferences_are_isolated_by_profile_and_clear_only_one_profile() = runTest {
        val bridge = FakePreferenceBridge()
        val store = FoodPreferenceStore(bridge)
        val vegetarian = FoodPreference(vegetarian = true, excludedIngredients = setOf("花生"))
        store.save("profile-a", vegetarian)
        store.save("profile-b", FoodPreference(halal = true))
        assertEquals(vegetarian, store.read("profile-a"))
        store.clear("profile-a")
        assertEquals(FoodPreference(), store.read("profile-a"))
        assertEquals(FoodPreference(halal = true), store.read("profile-b"))
        assertEquals(listOf(store.memoryKey("profile-a")), bridge.deleted)
    }

    @Test
    fun invalid_payload_is_reported_and_new_value_replaces_it() = runTest {
        val bridge = FakePreferenceBridge()
        val store = FoodPreferenceStore(bridge)
        bridge.values[store.memoryKey("profile-a")] = "broken"
        assertTrue(store.readSnapshot("profile-a").unreadable)
        store.save("profile-a", FoodPreference(avoidSpicy = true))
        assertEquals(FoodPreference(avoidSpicy = true), store.read("profile-a"))
    }
}
