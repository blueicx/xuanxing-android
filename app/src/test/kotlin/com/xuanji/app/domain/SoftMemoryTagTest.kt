package com.xuanji.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoftMemoryTagTest {
    @Test
    fun inferred_tag_round_trips_with_source_and_can_be_revoked() {
        val tag = SoftMemoryTag(
            id = "work",
            key = "topic",
            label = "最近常聊",
            value = "工作",
            source = SoftMemorySource.Inferred,
            createdAt = "2026-09-12",
            lastSeenAt = "2026-09-12"
        )

        val decoded = SoftMemoryTagCodec.decode(SoftMemoryTagCodec.encode(listOf(tag)))

        assertEquals(tag, decoded.single())
        assertTrue(SoftMemoryTagCodec.revoke(decoded, "work").single().revoked)
    }

    @Test
    fun corrupt_or_future_payload_is_unreadable_but_does_not_throw() {
        assertTrue(SoftMemoryTagCodec.decodeResult("not-json").unreadable)
        assertTrue(SoftMemoryTagCodec.decodeResult("{\"version\":99,\"tags\":[]}").unreadable)
        assertTrue(SoftMemoryTagCodec.decodeResult(null).tags.isEmpty())
    }
}
