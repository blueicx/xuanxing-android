package com.xuanji.app.domain

import com.xuanji.app.data.local.MysticCharacterSessionStore
import com.xuanji.app.data.local.PreferenceBridge
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MysticSessionSnapshotCodecTest {
    @Test
    fun round_trip_keeps_threads_shared_notes_and_drops_pending_work() {
        val initial = MysticCharacterSessionState.initial(MysticCharacterId.EvelynNova)
        val withMessages = reduceCharacterSession(
            reduceCharacterSession(
                initial,
                MysticCharacterEvent.Session(
                    MysticCharacterId.EvelynNova,
                    MysticEvent.SendInput("看一下塔罗")
                )
            ),
            MysticCharacterEvent.RememberShared(MysticMemoryNote("n1", "我主动选择保留这句"))
        )
        val decoded = MysticSessionSnapshotCodec.decode(MysticSessionSnapshotCodec.encode(withMessages))

        requireNotNull(decoded)
        assertEquals(MysticCharacterId.EvelynNova, decoded.activeCharacterId)
        assertEquals("我主动选择保留这句", decoded.sharedMemoryNotes.single().text)
        assertTrue(decoded.session(MysticCharacterId.EvelynNova).requestState is MysticRequestState.Idle)
        assertEquals(null, decoded.session(MysticCharacterId.EvelynNova).pendingInput)
    }

    @Test
    fun malformed_or_future_payload_is_not_treated_as_an_empty_thread() {
        assertNull(MysticSessionSnapshotCodec.decode("not-json"))
        assertNull(MysticSessionSnapshotCodec.decode("{\"version\":99}"))
    }

    @Test
    fun store_supports_reload_per_character_clear_and_clear_all() = runBlocking {
        val bridge = MemoryBridge()
        val store = MysticCharacterSessionStore(bridge)
        val profile = "bazi|demo"
        val state = MysticCharacterSessionState.initial(MysticCharacterId.MoHeng)
        store.save(profile, state)
        assertEquals(state, store.load(profile))

        store.clearCharacter(profile, MysticCharacterId.MoHeng)
        assertEquals(MysticCharacterId.ShenYanzhou, store.load(profile)?.activeCharacterId)
        store.clearAll(profile)
        assertNull(store.load(profile))
    }

    private class MemoryBridge : PreferenceBridge {
        private val values = mutableMapOf<String, String>()
        override suspend fun read(key: String): String? = values[key]
        override suspend fun write(key: String, value: String) { values[key] = value }
        override suspend fun delete(key: String) { values.remove(key) }
    }
}
