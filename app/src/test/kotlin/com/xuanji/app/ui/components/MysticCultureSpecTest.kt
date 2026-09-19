package com.xuanji.app.ui.components

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MysticCultureSpecTest {
    @Test
    fun cultural_skins_have_distinct_props_and_scenes() {
        val jiangnan = MysticCultureSpec.forSkin("jiangnan-robe")
        val desert = MysticCultureSpec.forSkin("desert-traveler")

        assertNotEquals(jiangnan.scene, desert.scene)
        assertNotEquals(jiangnan.prop, desert.prop)
        assertTrue(jiangnan.sceneLabel.isNotBlank())
        assertTrue(desert.sceneLabel.isNotBlank())
    }

    @Test
    fun unknown_skin_falls_back_to_neutral_stage() {
        val spec = MysticCultureSpec.forSkin("unknown")

        assertTrue(spec.sceneLabel.isNotBlank())
        assertTrue(spec.prop.isNotBlank())
    }
}
