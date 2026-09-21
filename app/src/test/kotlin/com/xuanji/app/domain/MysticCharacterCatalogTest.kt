package com.xuanji.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MysticCharacterCatalogTest {
    @Test
    fun catalog_exposes_the_four_reference_characters() {
        val profiles = MysticCharacterCatalog.all

        assertEquals(
            setOf(
                MysticCharacterId.ShenYanzhou,
                MysticCharacterId.MoHeng,
                MysticCharacterId.EvelynNova,
                MysticCharacterId.NadirRashid
            ),
            profiles.map { it.id }.toSet()
        )
        assertEquals("沈砚舟", profiles.first { it.id == MysticCharacterId.ShenYanzhou }.displayName)
        assertEquals("墨衡", profiles.first { it.id == MysticCharacterId.MoHeng }.displayName)
        assertEquals("伊芙琳·诺瓦", profiles.first { it.id == MysticCharacterId.EvelynNova }.displayName)
        assertEquals("纳迪尔·拉希德", profiles.first { it.id == MysticCharacterId.NadirRashid }.displayName)
        assertEquals(4, profiles.map { it.visualStyleId }.toSet().size)
        assertEquals(4, profiles.map { it.gameTheme }.toSet().size)
    }

    @Test
    fun each_character_has_source_labeled_specialties() {
        MysticCharacterCatalog.all.forEach { profile ->
            assertTrue(profile.specialties.isNotEmpty())
            profile.specialties.forEach { specialty ->
                assertTrue(specialty.key.isNotBlank())
                assertTrue(specialty.sourceLabel.isNotBlank())
                assertTrue(specialty.ritual.isNotBlank())
            }
        }
    }

    @Test
    fun recommendation_is_deterministic_and_uses_the_topic_seed() {
        val first = MysticCharacterCatalog.recommend("career", "2026-09-22", 72, 6)
        val second = MysticCharacterCatalog.recommend("career", "2026-09-22", 72, 6)
        val otherTopic = MysticCharacterCatalog.recommend("love", "2026-09-22", 72, 6)

        assertEquals(first, second)
        assertNotEquals(first.id, otherTopic.id)
    }

    @Test
    fun legacy_persona_labels_are_not_character_names() {
        MysticCharacterCatalog.all.forEach { profile ->
            assertTrue(profile.displayName != "玄学家")
            assertTrue(profile.displayName != "半仙")
            assertTrue(profile.displayName != "玄师")
        }
    }
}
