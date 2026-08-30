package com.xuanji.app.domain

import com.xuanji.app.domain.divination.IChing
import com.xuanji.app.domain.test.BigFive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PsychometricBoundaryTest {
    @Test
    fun big_five_rejects_answer_count_that_does_not_match_questionnaire() {
        val error = runCatching { BigFive.calculate(List(BigFive.QUESTIONS.size - 1) { 3 }) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun big_five_rejects_answers_outside_one_to_five() {
        val answers = List(BigFive.QUESTIONS.size) { 3 }.toMutableList()
        answers[0] = 0
        val error = runCatching { BigFive.calculate(answers) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun iching_uses_a_named_hexagram_for_every_binary_result() {
        val names = IChing.HEXAGRAM_NAMES
        assertEquals(64, names.distinct().size)
        assertTrue(names.none { it.startsWith("第") })
    }
}
