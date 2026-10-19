package com.xuanji.app.ui.history

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xuanji.app.data.local.ActionFeedbackDataStore
import com.xuanji.app.data.local.LocalReviewTimelineRepository
import com.xuanji.app.data.local.PreferenceBridge
import com.xuanji.app.data.local.ReadingFavoriteStore
import com.xuanji.app.domain.ReadingFavorite
import com.xuanji.app.domain.action.ActionFeedback
import com.xuanji.app.domain.action.ActionFeedbackKind
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalReviewPeriodInteractionTest {
    @get:Rule val compose = createComposeRule()

    private class MemoryBridge : PreferenceBridge {
        private val values = mutableMapOf<String, String>()
        override suspend fun read(key: String): String? = values[key]
        override suspend fun write(key: String, value: String) { values[key] = value }
        override suspend fun delete(key: String) { values.remove(key) }
    }

    @Test
    fun weekly_and_monthly_summary_switches_over_explicit_local_records() {
        val bridge = MemoryBridge()
        val favorites = ReadingFavoriteStore(bridge) { System.currentTimeMillis() }
        val feedback = ActionFeedbackDataStore(bridge)
        val favoriteKey = "synthetic-favorite-profile"
        val actionKey = "synthetic-action-profile"
        runBlocking {
            favorites.upsert(favoriteKey, ReadingFavorite(
                id = "week-favorite", type = "fortune", title = "主动收藏", body = "合成", createdAt = System.currentTimeMillis()
            ))
            feedback.append(actionKey, ActionFeedback(
                id = "week-accepted", dateKey = "2026-10-11", category = "meal", candidateKey = "synthetic-meal",
                kind = ActionFeedbackKind.Accepted, createdAtEpochMs = System.currentTimeMillis()
            ))
        }

        compose.setContent {
            MaterialTheme {
                LocalReviewTimelineSection(
                    LocalReviewTimelineRepository(favorites, feedback), favoriteKey, actionKey
                )
            }
        }

        compose.waitUntil(5_000) { compose.onAllNodesWithTag("local-review-period-summary").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("local-review-period-summary").assertExists()
        compose.onNodeWithText("收藏 1 · 采纳 1 · 不合适 0 · 换掉 0").assertExists()
        compose.onNodeWithTag("local-review-period-month").performClick()
        compose.onNodeWithTag("local-review-period-month").assertIsSelected()
        compose.onNodeWithText("收藏 1 · 采纳 1 · 不合适 0 · 换掉 0").assertExists()
        compose.onNodeWithTag("local-review-timeline").assertExists()
    }
}
