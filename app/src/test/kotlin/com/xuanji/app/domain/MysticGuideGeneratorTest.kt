package com.xuanji.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MysticGuideGeneratorTest {

    @Test
    fun customAnswerPrefix_returnsEmptyForBlankQuestion() {
        assertEquals("", MysticGuideGenerator.customAnswerPrefix("   "))
    }
}
