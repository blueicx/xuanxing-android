package com.xuanji.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MysticCultureVoiceTest {
    @Test
    fun each_skin_has_prop_gesture_and_voice_differences() {
        val specs = MysticCultureSpec.all()

        assertEquals(8, specs.size)
        assertEquals(8, specs.map { it.prop }.toSet().size)
        assertEquals(8, specs.map { it.gesture }.toSet().size)
        assertEquals(8, specs.map { it.voiceLexicon }.toSet().size)
        assertNotEquals(specs.first().voiceLexicon, specs.last().voiceLexicon)
    }
}
