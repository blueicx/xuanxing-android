package com.xuanji.app.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MysticSkinTest {
    @Test
    fun visual_styles_are_bounded_and_mapped() {
        val allowed = setOf("ink_scholar", "cel_astrologer", "lowpoly_guardian")
        val all = listOf("scholar", "half").flatMap(MysticGuideGenerator::mysticSkins)

        assertEquals(8, all.size)
        assertTrue(all.all { it.visualStyleId in allowed })
        assertEquals("ink_scholar", all.first { it.id == "jiangnan-robe" }.visualStyleId)
        assertEquals("cel_astrologer", all.first { it.id == "academy-gown" }.visualStyleId)
        assertEquals("lowpoly_guardian", all.first { it.id == "desert-traveler" }.visualStyleId)
        assertEquals(3, all.map { it.visualStyleId }.toSet().size)
    }
}
