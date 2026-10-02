package com.xuanji.app.data.local

import com.xuanji.app.domain.ReadingFavorite
import com.xuanji.app.domain.ReadingFavoriteCodec
import com.xuanji.app.domain.ReadingFavoriteDataState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingFavoriteStoreTest {
    private class MemoryBridge : PreferenceBridge {
        val values = mutableMapOf<String, String>()
        override suspend fun read(key: String): String? = values[key]
        override suspend fun write(key: String, value: String) { values[key] = value }
        override suspend fun delete(key: String) { values.remove(key) }
    }

    @Test
    fun codec_round_trips_snapshot_without_raw_profile_identity() {
        val favorite = favorite(id = "f-1")
        val encoded = ReadingFavoriteCodec.encode("hashed-profile", listOf(favorite))
        val decoded = ReadingFavoriteCodec.decode(encoded)

        assertEquals(ReadingFavoriteDataState.Ready, decoded.state)
        assertEquals("hashed-profile", decoded.profileFingerprint)
        assertEquals(listOf(favorite), decoded.entries)
        assertFalse(encoded.contains("private-birth-profile"))
        assertFalse(encoded.contains("profileKey"))
    }

    @Test
    fun store_isolates_profiles_sorts_updates_by_created_time_and_supports_remove_and_clear() = runTest {
        val bridge = MemoryBridge()
        val store = ReadingFavoriteStore(bridge) { 500L }
        bridge.values["character_sessions_private"] = "untouched"
        store.upsert("private-birth-profile", favorite("old", createdAt = 100L))
        store.upsert("private-birth-profile", favorite("new", createdAt = 300L))
        store.upsert("another-profile", favorite("other", createdAt = 400L))

        assertEquals(listOf("new", "old"), store.list("private-birth-profile").entries.map { it.id })
        assertEquals(listOf("other"), store.list("another-profile").entries.map { it.id })
        assertTrue(store.key("private-birth-profile").contains(ReadingFavoriteStore.KEY_PREFIX))
        assertFalse(store.key("private-birth-profile").contains("private-birth-profile"))

        store.upsert("private-birth-profile", favorite("old", title = "更新后的标题", createdAt = 600L))
        assertEquals(listOf("old", "new"), store.list("private-birth-profile").entries.map { it.id })
        assertEquals("更新后的标题", store.list("private-birth-profile").entries.first().title)
        store.remove("private-birth-profile", "old")
        assertEquals(listOf("new"), store.list("private-birth-profile").entries.map { it.id })
        store.clear("private-birth-profile")
        assertTrue(store.list("private-birth-profile").entries.isEmpty())
        assertEquals(listOf("other"), store.list("another-profile").entries.map { it.id })
        assertEquals("untouched", bridge.values["character_sessions_private"])
    }

    @Test
    fun unknown_or_corrupt_data_is_preserved_until_explicit_clear() = runTest {
        val bridge = MemoryBridge()
        val store = ReadingFavoriteStore(bridge)
        val key = store.key("alice")
        val corrupted = "{broken-json"
        bridge.values[key] = corrupted

        val loaded = store.list("alice")
        assertEquals(ReadingFavoriteDataState.Corrupt, loaded.state)
        store.upsert("alice", favorite("f-1"))
        assertEquals(corrupted, bridge.values[key])

        bridge.values[key] = """{"version":99,"profileFingerprint":"x","entries":[]}"""
        assertEquals(ReadingFavoriteDataState.UnsupportedVersion, store.list("alice").state)
        store.clear("alice")
        assertEquals(ReadingFavoriteDataState.Missing, store.list("alice").state)
    }

    @Test
    fun legacy_envelope_migrates_and_profile_mismatch_is_not_read() = runTest {
        val legacy = """{"version":1,"profileFingerprint":"alice-hash","entries":[{"id":"legacy","type":"fortune","title":"旧收藏","dateKey":"2026-01-01","period":"今日","mode":"综合","body":"旧结果","evidence":"旧依据","algorithmVersion":"v1","createdAt":10}]}"""
        val decoded = ReadingFavoriteCodec.decode(legacy)
        assertEquals(ReadingFavoriteDataState.Migrated, decoded.state)
        assertEquals("legacy", decoded.entries.single().id)

        val bridge = MemoryBridge()
        val store = ReadingFavoriteStore(bridge)
        bridge.values[store.key("alice")] = ReadingFavoriteCodec.encode("not-alice", listOf(favorite("wrong")))
        assertEquals(ReadingFavoriteDataState.ProfileMismatch, store.list("alice").state)
    }

    private fun favorite(
        id: String,
        title: String = "事业详情",
        createdAt: Long = 200L
    ) = ReadingFavorite(
        id = id,
        type = "fortune",
        title = title,
        dateKey = "2026-10-02",
        period = "今日",
        mode = "综合",
        body = "本地计算结果",
        evidence = "当前周期维度",
        algorithmVersion = "x3-test",
        createdAt = createdAt
    )
}
