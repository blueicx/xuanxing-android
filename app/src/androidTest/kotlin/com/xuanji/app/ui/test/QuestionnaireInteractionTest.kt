package com.xuanji.app.ui.test

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.xuanji.app.data.local.PreferenceBridge
import com.xuanji.app.data.local.QuestionnaireDraftStore
import com.xuanji.app.domain.test.BigFive
import com.xuanji.app.domain.test.QuestionnaireDraft
import com.xuanji.app.domain.test.QuestionnaireSpec
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class QuestionnaireInteractionTest {
    @get:Rule val compose = createComposeRule()
    private class Memory : PreferenceBridge {
        private val values = mutableMapOf<String, String>()
        override suspend fun read(key: String) = values[key]
        override suspend fun write(key: String, value: String) { values[key] = value }
        override suspend fun delete(key: String) { values.remove(key) }
    }
    @Test fun actual_last_answer_opens_review_edits_preserve_suffix_and_reopen_restores_draft() {
        val store = QuestionnaireDraftStore(Memory())
        val spec = QuestionnaireSpec("bigfive", List(BigFive.QUESTIONS.size) { (1..5).map(Int::toString).toSet() })
        runBlocking { store.save("synthetic", spec, QuestionnaireDraft(50, List(49) { "3" })) }
        var mounted by mutableStateOf(true)
        compose.setContent { MaterialTheme {
            CompositionLocalProvider(LocalQuestionnairePersistence provides QuestionnairePersistence("synthetic", store)) {
                if (mounted) BigFiveScreen()
            }
        } }
        compose.onNodeWithText("第 50 / 50 题").assertExists()
        compose.onNodeWithText("5 · 非常符合").performScrollTo().performClick()
        compose.onNodeWithText("查看结果").performScrollTo().performClick()
        compose.onNodeWithTag("questionnaire-review").assertExists()
        compose.onNodeWithTag("review-0-1").performScrollTo().performClick()
        compose.waitUntil(5000) { runBlocking { store.load("synthetic", spec).answers.firstOrNull() == "1" } }
        runBlocking {
            assertEquals(50, store.load("synthetic", spec).answers.size)
            assertEquals("5", store.load("synthetic", spec).answers.last())
            assertTrue(store.load("other", spec).answers.isEmpty())
        }
        compose.runOnIdle { mounted = false }
        compose.waitForIdle()
        compose.runOnIdle { mounted = true }
        compose.onNodeWithTag("review-0-1").performScrollTo().assertTextContains("✓", substring = true)
    }

    @Test fun review_confirmation_is_explicit_and_uses_real_calculator_after_edit() {
        val spec = QuestionnaireSpec("synthetic", List(50) { (1..5).map(Int::toString).toSet() })
        val progress = QuestionnaireProgress(mutableStateOf(51), mutableStateListOf<Int>().also { it.addAll(List(50) { 3 }) }, mutableStateOf(0), spec, String::toInt, 0)
        compose.setContent { MaterialTheme {
            Column(Modifier.verticalScroll(rememberScrollState())) { QuestionnaireReview(progress, BigFive.QUESTIONS.map { it.text }) }
        } }
        val before = BigFive.calculate(List(50) { 3 })
        compose.onNodeWithTag("review-0-1").performScrollTo().performClick()
        compose.onNodeWithTag("questionnaire-score").performScrollTo().performClick()
        compose.runOnIdle {
            assertTrue(progress.reviewed)
            assertNotEquals(before, BigFive.calculate(progress.answers.toList()))
            assertEquals(List(49) { 3 }, progress.answers.drop(1))
        }
    }
}
