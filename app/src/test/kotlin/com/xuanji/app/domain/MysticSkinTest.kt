package com.xuanji.app.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MysticSkinTest {
    @Test
    fun visual_styles_are_bounded_and_mapped() {
        val allowed = setOf("jiangnan_scholar", "elder_ink", "academy_astral", "silkroad_astrologer")
        val all = listOf("scholar", "half").flatMap(MysticGuideGenerator::mysticSkins)

        assertEquals(8, all.size)
        assertTrue(all.all { it.visualStyleId in allowed })
        assertEquals("jiangnan_scholar", all.first { it.id == "jiangnan-robe" }.visualStyleId)
        assertEquals("elder_ink", all.first { it.id == "cloud-daoist" }.visualStyleId)
        assertEquals("academy_astral", all.first { it.id == "academy-gown" }.visualStyleId)
        assertEquals("silkroad_astrologer", all.first { it.id == "desert-traveler" }.visualStyleId)
        assertEquals(4, all.map { it.visualStyleId }.toSet().size)
        assertTrue(listOf("scholar", "half").all { mode ->
            MysticGuideGenerator.mysticSkins(mode).map { it.visualStyleId }.toSet().size == 4
        })
    }
}
