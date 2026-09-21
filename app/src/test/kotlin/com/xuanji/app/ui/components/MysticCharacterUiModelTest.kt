package com.xuanji.app.ui.components

import com.xuanji.app.domain.MysticCharacterCatalog
import com.xuanji.app.domain.MysticCharacterId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MysticCharacterUiModelTest {
    @Test
    fun gallery_model_exposes_the_three_stage_actions() {
        val model = MysticCharacterUiModel.from(
            MysticCharacterCatalog.byId(MysticCharacterId.ShenYanzhou)
        )

        assertEquals(listOf("进入对话", "查看专长", "来一盘象棋"), model.actions)
        assertEquals("沈砚舟", model.title)
        assertTrue(model.cultureLabel.contains("江南"))
        assertEquals("八字与五行", model.primarySpecialty)
        assertTrue(model.specialties.isNotEmpty())
    }

    @Test
    fun triptych_profiles_use_scene_plates_but_elder_uses_ink_backdrop() {
        assertTrue(
            MysticCharacterUiModel.from(
                MysticCharacterCatalog.byId(MysticCharacterId.ShenYanzhou)
            ).usesScenePlate
        )
        assertTrue(
            MysticCharacterUiModel.from(
                MysticCharacterCatalog.byId(MysticCharacterId.EvelynNova)
            ).usesScenePlate
        )
        assertTrue(
            MysticCharacterUiModel.from(
                MysticCharacterCatalog.byId(MysticCharacterId.NadirRashid)
            ).usesScenePlate
        )
        assertFalse(
            MysticCharacterUiModel.from(
                MysticCharacterCatalog.byId(MysticCharacterId.MoHeng)
            ).usesScenePlate
        )
    }

    @Test
    fun long_bubble_is_collapsed_until_user_expands_it() {
        val model = MysticDialogueBubbleModel("这是一个很长的角色回复".repeat(20))

        assertFalse(model.expanded)
        assertTrue(model.shouldCollapse)
        assertEquals(model.fullText, model.toggle().preview)
        assertTrue(model.toggle().expanded)
    }

    @Test
    fun game_theme_palette_changes_by_character_without_changing_the_rules() {
        val palettes = listOf("jiangnan_wood", "ink_paper", "academy_star", "silkroad_copper")
            .map(::mysticGameThemePalette)

        assertEquals(4, palettes.map { it.key }.toSet().size)
        assertEquals(4, palettes.map { it.boardColor }.toSet().size)
        assertTrue(palettes.all { it.label.isNotBlank() })
    }
}
