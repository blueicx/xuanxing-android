package com.xuanji.app.ui.components

import com.xuanji.app.domain.MysticCharacterCatalog
import com.xuanji.app.domain.MysticCharacterId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MysticSceneSpecTest {
    @Test
    fun all_characters_have_distinct_complete_scene_specs() {
        val specs = MysticCharacterCatalog.all.map { MysticSceneCatalog.forScene(it.sceneId) }

        assertEquals(4, specs.map { it.id }.distinct().size)
        assertTrue(specs.all { it.hasCompleteBackdrop })
        assertTrue(specs.all { it.title.isNotBlank() })
        assertTrue(specs.all { it.culturalMotifs.isNotEmpty() })
        assertTrue(specs.all { it.accessibilityDescription.isNotBlank() })
    }

    @Test
    fun unknown_scene_uses_a_non_empty_ink_fallback() {
        val fallback = MysticSceneCatalog.forScene("unknown_scene")

        assertEquals(MysticSceneId.InkFallback, fallback.id)
        assertTrue(fallback.hasCompleteBackdrop)
        assertTrue(fallback.culturalMotifs.isNotEmpty())
        assertTrue(fallback.accessibilityDescription.contains("水墨"))
    }

    @Test
    fun scene_specs_keep_cultural_motifs_distinct_from_palette() {
        val shen = MysticSceneCatalog.forScene("jiangnan_triptych")
        val mo = MysticSceneCatalog.forScene("ink_elder")
        val evelyn = MysticSceneCatalog.forScene("academy_triptych")
        val nadir = MysticSceneCatalog.forScene("silkroad_triptych")

        assertNotEquals(shen.culturalMotifs, mo.culturalMotifs)
        assertNotEquals(mo.culturalMotifs, evelyn.culturalMotifs)
        assertNotEquals(evelyn.culturalMotifs, nadir.culturalMotifs)
        assertTrue(listOf(shen, mo, evelyn, nadir).all { it.accentColorArgb != it.backgroundColorArgb })
    }

    @Test
    fun character_ui_model_exposes_scene_spec_without_layout_boolean() {
        val model = MysticCharacterUiModel.from(
            MysticCharacterCatalog.byId(MysticCharacterId.MoHeng)
        )

        assertEquals(MysticSceneId.InkElder, model.sceneSpec.id)
        assertTrue(model.sceneSpec.hasCompleteBackdrop)
        assertFalse(model.sceneSpec === MysticSceneCatalog.fallback)
    }
}
