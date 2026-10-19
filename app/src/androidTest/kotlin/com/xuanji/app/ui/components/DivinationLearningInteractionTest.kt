package com.xuanji.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import com.xuanji.app.data.local.*
import com.xuanji.app.data.model.*
import com.xuanji.app.domain.divination.*
import com.xuanji.app.ui.divination.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DivinationLearningInteractionTest {
    @get:Rule val compose = createComposeRule()
    @Test fun rune_three_positions_and_learning_expansion_survive_recreation() {
        val reading = Rune.read(DivinationQuery("rune", "synthetic", "2026-10-19", "学习", "three"))
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MaterialTheme {
            Column(Modifier.verticalScroll(rememberScrollState())) { RuneLearningPanel(reading) }
        } }
        reading.draws.forEachIndexed { index, _ ->
            compose.onNodeWithTag("rune-position-$index").performScrollTo().assertTextContains(DivinationLearning.runePositions(reading)[index])
        }
        compose.onNodeWithTag("rune-learning-toggle").performScrollTo().performClick()
        compose.onNodeWithText("ᚠ Fehu").performScrollTo().assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("ᛟ Othala").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("rune-learning-toggle").performScrollTo().performClick()
        compose.onNodeWithText("ᚠ Fehu").assertDoesNotExist()
    }

    @Test fun tarot_explicit_save_preserves_exact_cards_and_evidence_in_synthetic_history_only() {
        val data = mutableMapOf<String, String>()
        val store = DailyLifeStore(object : PreferenceBridge {
            override suspend fun read(key: String) = data[key]
            override suspend fun write(key: String, value: String) { data[key] = value }
            override suspend fun delete(key: String) { data.remove(key) }
        })
        val cards = listOf(
            DrawnTarot(TarotCard("测试甲牌", "A", "minor", "圣杯", "正位甲", "逆位甲"), false, "过去"),
            DrawnTarot(TarotCard("测试乙牌", "B", "minor", "圣杯", "正位乙", "逆位乙"), true, "现在")
        )
        compose.setContent { MaterialTheme {
            CompositionLocalProvider(LocalDailyLifeStore provides store, LocalDailyLifeProfileKey provides "synthetic") {
                Column(Modifier.verticalScroll(rememberScrollState())) { TarotLearningPanel(cards, "2026-10-19", "test-seed") }
            }
        } }
        runBlocking { assertTrue(store.load("synthetic").snapshots.isEmpty()) }
        compose.onNodeWithTag("save-reading-snapshot").performScrollTo().performClick()
        compose.waitForIdle()
        runBlocking {
            val snapshot = store.load("synthetic").snapshots.single()
            assertTrue(snapshot.body.contains("测试乙牌（逆位）") && snapshot.body.contains("逆位乙"))
            assertTrue(snapshot.evidence.contains("test-seed"))
            assertEquals("draw-observation-v1", snapshot.algorithmVersion)
            assertTrue(store.load("other").snapshots.isEmpty())
        }
    }
}
