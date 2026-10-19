package com.xuanji.app.data.local

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LocalRestoreTransactionTest {
    private class Memory : PreferenceBridge {
        val values = linkedMapOf<String, String>()
        var failKey: String? = null
        override suspend fun read(key: String) = values[key]
        override suspend fun write(key: String, value: String) {
            if (failKey == key) { failKey = null; error("injected write failure") }
            values[key] = value
        }
        override suspend fun delete(key: String) { values.remove(key) }
    }
    @Test fun failed_partial_batch_restores_all_prior_values() = runBlocking {
        val bridge = Memory()
        bridge.values["food_preference_a"] = "old"
        bridge.failKey = "daily_life_a"
        val result = LocalRestoreTransaction(bridge).commit(
            mapOf("food_preference_a" to "old", "daily_life_a" to null),
            mapOf("food_preference_a" to "new", "daily_life_a" to "agenda")
        )
        assertFalse(result)
        assertEquals("old", bridge.values["food_preference_a"])
        assertNull(bridge.values["daily_life_a"])
    }
    @Test fun concurrent_change_is_not_overwritten_and_success_leaves_summary_log() = runBlocking {
        val bridge = Memory()
        bridge.values["food_preference_a"] = "user-edited"
        val transaction = LocalRestoreTransaction(bridge)
        assertFalse(transaction.commit(mapOf("food_preference_a" to "old"), mapOf("food_preference_a" to "restored")))
        assertEquals("user-edited", bridge.values["food_preference_a"])
        assertTrue(transaction.commit(mapOf("food_preference_a" to "user-edited"), mapOf("food_preference_a" to "restored")))
        assertNotNull(bridge.values[LocalRestoreTransaction.LOG_KEY])
        assertNull(bridge.values[LocalRestoreTransaction.JOURNAL_KEY])
        assertFalse(bridge.values[LocalRestoreTransaction.LOG_KEY]!!.contains("user-edited"))
    }
    @Test fun interrupted_write_is_rolled_back_on_next_start_but_invalid_journal_cannot_write_keys() = runBlocking {
        val bridge = Memory()
        bridge.values["food_preference_a"] = "half-restored"
        bridge.values[LocalRestoreTransaction.JOURNAL_KEY] = """{"version":1,"before":{"food_preference_a":"original"}}"""
        assertTrue(LocalRestoreTransaction(bridge).recover())
        assertEquals("original", bridge.values["food_preference_a"])
        bridge.values[LocalRestoreTransaction.JOURNAL_KEY] = """{"version":1,"before":{"birth_profile":"malicious"}}"""
        assertFalse(LocalRestoreTransaction(bridge).recover())
        assertNull(bridge.values["birth_profile"])
    }
    @Test fun native_atomic_interruption_does_not_overwrite_later_user_edits() = runBlocking {
        val bridge = Memory()
        bridge.values["food_preference_a"] = "user-edited-after-interruption"
        bridge.values[LocalRestoreTransaction.JOURNAL_KEY] = """{"version":1,"atomic":true,"before":{"food_preference_a":"original"}}"""
        assertTrue(LocalRestoreTransaction(bridge).recover())
        assertEquals("user-edited-after-interruption", bridge.values["food_preference_a"])
        assertNull(bridge.values[LocalRestoreTransaction.JOURNAL_KEY])
    }
}
