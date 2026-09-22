package com.xuanji.app.domain.game

import com.xuanji.app.data.local.CompanionGameProgressStore
import com.xuanji.app.data.local.PreferenceBridge
import com.xuanji.app.domain.MysticCharacterId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionGameProgressStoreTest {
    private class MemoryBridge : PreferenceBridge {
        val values = mutableMapOf<String, String>()
        override suspend fun read(key: String): String? = values[key]
        override suspend fun write(key: String, value: String) { values[key] = value }
        override suspend fun delete(key: String) { values.remove(key) }
    }

    @Test
    fun codec_round_trips_all_three_local_game_states_and_rejects_damage() {
        val states = listOf<CompanionGameProgressState>(
            CompanionGameProgressState.Poetry(PoetryChainState(2, "山色入衣宽", 2, false, "好")),
            CompanionGameProgressState.StarMap(StarMapState(1, "天津", listOf("织女", "天津"), false, "继续")),
            CompanionGameProgressState.SilkRoad(SilkRoadState("敦煌", daysLeft = 6, budgetLeft = 9, path = listOf("长安", "敦煌")))
        )
        states.forEach { state ->
            val encoded = CompanionGameProgressCodec.encode(
                CompanionGameProgressEnvelope(1, "fingerprint", MysticCharacterId.ShenYanzhou.key, state.gameId, CompanionGameProgressCodec.encodeState(state), 123L)
            )
            val decoded = CompanionGameProgressCodec.decode(encoded)?.let(CompanionGameProgressCodec::decodeState)
            assertEquals(state, decoded)
        }
        assertNull(CompanionGameProgressCodec.decode("{broken"))
        assertNull(CompanionGameProgressCodec.decode("{\"version\":99}"))
    }

    @Test
    fun store_isolates_profile_character_and_game_and_clear_is_local() = runTest {
        val bridge = MemoryBridge()
        val store = CompanionGameProgressStore(bridge) { 456L }
        val state = CompanionGameProgressState.Poetry(PoetryChainState(score = 1))
        store.save("alice", MysticCharacterId.ShenYanzhou, state)

        assertEquals(state, store.load("alice", MysticCharacterId.ShenYanzhou, "poetry_chain"))
        assertNull(store.load("bob", MysticCharacterId.ShenYanzhou, "poetry_chain"))
        assertNull(store.load("alice", MysticCharacterId.MoHeng, "poetry_chain"))
        assertTrue(bridge.values.keys.single().startsWith(CompanionGameProgressStore.KEY_PREFIX))

        store.clear("alice", MysticCharacterId.ShenYanzhou, "poetry_chain")
        assertNull(store.load("alice", MysticCharacterId.ShenYanzhou, "poetry_chain"))
    }
}
