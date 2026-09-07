package com.xuanji.app.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MysticDialogueTemplatesTest {
    @Test
    fun identity_template_uses_the_explicit_persona_label() {
        val answer = MysticDialogueTemplates.identityAnswer(
            scholar = true,
            styleKey = "archive",
            personaLabel = "玄学家"
        )

        assertEquals(
            "玄学家，一个守旧档的人。我不替你判命，只帮你看清手边能做的事。",
            answer
        )
    }

    @Test
    fun farewell_template_stays_independent_from_the_generator() {
        val answer = MysticDialogueTemplates.farewellAnswer(
            scholar = false,
            styleKey = "herald"
        )

        assertTrue(answer.contains("退场不催"))
    }
}
