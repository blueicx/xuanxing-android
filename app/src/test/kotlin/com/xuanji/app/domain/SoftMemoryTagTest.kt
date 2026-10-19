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

    @Test fun legacy_tag_has_no_expiry_and_new_expiry_is_inclusive() {
        val legacy = """{"version":1,"tags":[{"id":"work","key":"topic","label":"常聊","value":"工作","source":"inferred","createdAt":"2026-10-16","lastSeenAt":"2026-10-16"}]}"""
        val tag = SoftMemoryTagCodec.decodeResult(legacy).tags.single()
        assertEquals(null, tag.expiresAtDate)
        val dated = tag.withLifetime(7, java.time.LocalDate.parse("2026-10-16"))
        assertEquals("2026-10-23", dated.expiresAtDate)
        assertTrue(!dated.isExpiredOn(java.time.LocalDate.parse("2026-10-23")))
        assertTrue(dated.isExpiredOn(java.time.LocalDate.parse("2026-10-24")))
        assertEquals(dated, SoftMemoryTagCodec.decode(SoftMemoryTagCodec.encode(listOf(dated))).single())
    }

    @Test fun malformed_expiry_does_not_silently_become_permanent() {
        val tag = SoftMemoryTag("work", "topic", "常聊", "工作", SoftMemorySource.Inferred, "2026-10-16", "2026-10-16", expiresAtDate = "2026-99-99")
        assertTrue(SoftMemoryTagCodec.decodeResult(SoftMemoryTagCodec.encode(listOf(tag))).unreadable)
    }
}
