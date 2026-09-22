package com.xuanji.app.domain

import com.xuanji.app.ui.components.MysticAssetRenderMode
import com.xuanji.app.ui.components.MysticVisualAssetCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MysticVisualAssetContractTest {
    @Test
    fun three_complete_scenes_and_one_foreground_are_declared_explicitly() {
        val specs = MysticCharacterCatalog.all.associate { profile ->
            profile.id to MysticVisualAssetCatalog.forCharacter(profile)
        }
        assertEquals(MysticAssetRenderMode.CompleteScene, specs.getValue(MysticCharacterId.ShenYanzhou).renderMode)
        assertEquals(MysticAssetRenderMode.ForegroundOnScene, specs.getValue(MysticCharacterId.MoHeng).renderMode)
        assertEquals(MysticAssetRenderMode.CompleteScene, specs.getValue(MysticCharacterId.EvelynNova).renderMode)
        assertEquals(MysticAssetRenderMode.CompleteScene, specs.getValue(MysticCharacterId.NadirRashid).renderMode)
        assertTrue(specs.values.all { it.focusY in 0f..1f })
    }

    @Test
    fun complete_scene_never_requests_a_second_backdrop() {
        MysticCharacterCatalog.all.forEach { profile ->
            val spec = MysticVisualAssetCatalog.forCharacter(profile)
            assertEquals(
                spec.renderMode == MysticAssetRenderMode.ForegroundOnScene,
                spec.requiresCultureBackdrop
            )
            if (spec.renderMode == MysticAssetRenderMode.CompleteScene) assertFalse(spec.requiresCultureBackdrop)
        }
    }
}
