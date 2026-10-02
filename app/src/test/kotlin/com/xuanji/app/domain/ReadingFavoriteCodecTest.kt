package com.xuanji.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingFavoriteCodecTest {
    @Test
    fun encoding_is_versioned_bounded_and_deduplicated() {
        val records = listOf(favorite("favorite-0", title = "updated", createdAt = 1000)) + (0..ReadingFavoriteCodec.MAX_ENTRIES).map { index ->
            favorite("favorite-$index", createdAt = index.toLong())
        }

        val encoded = ReadingFavoriteCodec.encode("fingerprint", records)
        val decoded = ReadingFavoriteCodec.decode(encoded)

        assertTrue(encoded.contains("\"version\":${ReadingFavoriteCodec.CURRENT_VERSION}"))
        assertEquals(ReadingFavoriteDataState.Ready, decoded.state)
        assertEquals(ReadingFavoriteCodec.MAX_ENTRIES, decoded.entries.size)
        assertEquals("updated", decoded.entries.first().title)
        assertFalse(encoded.contains("private-profile-id"))
    }

    @Test
    fun legacy_summaries_and_basis_migrate_to_snapshot_fields() {
        val decoded = ReadingFavoriteCodec.decode(
            """{"version":1,"profileFingerprint":"fingerprint","entries":[{"id":"old","type":"fortune","title":"旧摘要","summary":"旧结果","basis":"旧依据","savedAt":42}]}"""
        )

        assertEquals(ReadingFavoriteDataState.Migrated, decoded.state)
        assertEquals("旧结果", decoded.entries.single().body)
        assertEquals("旧依据", decoded.entries.single().evidence)
        assertEquals(42L, decoded.entries.single().createdAt)
    }

    @Test
    fun malformed_payload_fails_closed_and_stable_ids_are_deterministic() {
        assertEquals(ReadingFavoriteDataState.Corrupt, ReadingFavoriteCodec.decode("not-json").state)
        val context = MysticDetailContext(
            mode = "东方",
            period = "今日",
            dateKey = "2026-10-02",
            title = "事业",
            result = "稳中求进",
            evidence = "木气偏旺",
            sourceStatus = "离线计算",
            algorithmVersion = "test"
        )
        assertEquals(
            ReadingFavoriteCodec.stableId("fortune", context),
            ReadingFavoriteCodec.stableId("fortune", context)
        )
        assertTrue(ReadingFavoriteCodec.stableId("action", context) != ReadingFavoriteCodec.stableId("fortune", context))
    }

    private fun favorite(id: String, title: String = "详情", createdAt: Long = 1L) = ReadingFavorite(
        id = id,
        type = "fortune",
        title = title,
        body = "可回看的本地快照",
        createdAt = createdAt
    )
}
