package com.xuanji.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.xuanji.app.domain.game.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DeepeningDeliveryInteractionTest {
    @get:Rule val compose = createComposeRule()
    @Test fun selected_share_fields_match_confirmed_payload_without_hidden_score() {
        var selected: ShareCard? = null
        val card = ShareCard("玄星", "合成分享", headline = "80", summary = "合成摘要", rows = listOf("色彩" to "青"))
        compose.setContent { MaterialTheme { SelectiveShareDialog(card, {}, { selected = it }) } }
        compose.onNodeWithTag("share-row-0").performClick()
        compose.onNodeWithTag("share-confirm").performClick()
        compose.runOnIdle {
            assertEquals("", selected?.headline)
            assertEquals(listOf("色彩" to "青"), selected?.rows)
            assertEquals("合成摘要", selected?.summary)
        }
    }
    @Test fun homepage_pin_changes_actual_order_and_reset_restores_all_content() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        var saved: CardLayoutState? = null
        val controller = CardLayoutController("today-sections", "synthetic", CardLayoutStore(context), CardLayouts.default("today-sections")) { saved = it }
        compose.setContent { MaterialTheme { Column { HomeSectionOrganizer(controller) } } }
        compose.onNodeWithTag("home-organize").performClick()
        compose.onNodeWithTag("home-pin-actions").performClick()
        compose.runOnIdle { assertEquals("actions", saved?.order?.minByOrNull { it.value }?.key) }
        compose.onNodeWithText("恢复默认顺序").performClick()
        compose.runOnIdle { assertEquals(CardLayouts.default("today-sections"), saved) }
    }
    @Test fun poetry_user_line_and_star_solution_are_inspectable_without_changing_progress() {
        val state = StarMapPuzzleEngine.initial(2)
        compose.setContent { MaterialTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            PoetryStructurePanel(PoetryChainEngine.prompt(PoetryChainState()))
            StarLogicLearningPanel(state)
        } } }
        compose.onNodeWithText("句式与对仗练习").performClick()
        compose.onNodeWithTag("poetry-own-line").performTextInput("灯影落江中")
        compose.onNodeWithTag("poetry-structure-result").assertTextContains("字数相称", substring = true)
        compose.onNodeWithTag("star-solution-toggle").performScrollTo().performClick()
        compose.onNodeWithText("从当前点至少还需", substring = true).performScrollTo().assertExists()
        assertFalse(state.completed)
    }
    @Test fun full_selected_text_requires_a_taller_card_not_silent_three_line_truncation() {
        val bitmap = ResultShareCardRenderer.render(ShareCard("玄星", "合成资料", summary = "完整的合成解说。".repeat(100), rows = listOf("末尾字段" to "必须保留")))
        try { assertTrue("Full text must not be silently clipped to 880px", bitmap.height > 880) }
        finally { bitmap.recycle() }
    }
    @Test fun exploration_button_runs_real_calculations_with_synthetic_inputs_and_exposes_actual_weights() {
        val profile = com.xuanji.app.data.model.UserProfile(2001, 2, 3, 9, 15, "合成资料")
        val detail = com.xuanji.app.domain.MysticDetailContext("综合", "今日", "2026-10-19", "合成结论", "测试结果", "已有依据", "本地计算", "synthetic-v1")
        compose.setContent { MaterialTheme {
            CompositionLocalProvider(LocalReadingExplorationInputs provides ReadingExplorationInputs(profile)) {
                Column(Modifier.verticalScroll(rememberScrollState())) { ReadingExplorationPanel(detail) }
            }
        } }
        compose.onNodeWithTag("reading-explore-toggle").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("实际计分步骤").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("学习：", substring = true).performScrollTo().assertTextContains("60%", substring = true)
        compose.onNodeWithText("八字出生时间 ±60 分钟试算").performScrollTo().assertExists()
    }
}
