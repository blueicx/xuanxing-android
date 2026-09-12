package com.xuanji.app.domain

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class MysticTopicTemplatesTest {
    @Test
    fun topicAnswerKeepsFactsAndTopicAdvice() {
        val answer = MysticTopicAnswerTemplates.answer(
            scholar = true,
            topicKey = "career",
            label = "事业",
            focusScore = 72,
            highLabel = "事业",
            lowLabel = "健康",
            testName = ""
        )

        assertContains(answer, "事业")
        assertContains(answer, "最重要的事")
        assertEquals(answer, MysticTopicAnswerTemplates.answer(true, "career", "事业", 72, "事业", "健康", ""))
    }

    @Test
    fun cultureVoiceExposesDistinctLexiconAndGesture() {
        val voices = MysticCultureVoice.all()

        assertEquals(8, voices.size)
        assertEquals(8, voices.map { it.lexicon }.distinct().size)
        assertEquals(8, voices.map { it.gesture }.distinct().size)
        assertContains(MysticCultureVoice.forSkin("street-jacket").lexicon, "在场")
    }
}
