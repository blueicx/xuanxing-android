package com.xuanji.app.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xuanji.app.data.local.PreferenceBridge
import com.xuanji.app.data.local.ReadingFavoriteStore
import com.xuanji.app.domain.ReadingFavorite
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReadingFavoritesInteractionTest {
    @get:Rule
    val compose = createComposeRule()
    private lateinit var activeStore: MutableState<ReadingFavoriteStore>

    private class MemoryBridge : PreferenceBridge {
        val values = mutableMapOf<String, String>()
        override suspend fun read(key: String): String? = values[key]
        override suspend fun write(key: String, value: String) { values[key] = value }
        override suspend fun delete(key: String) { values.remove(key) }
    }

    @Test
    fun favorites_filter_search_read_snapshot_remove_and_undo_use_saved_data() {
        val store = ReadingFavoriteStore(MemoryBridge())
        runBlocking {
            store.upsert("profile-a", favorite("fortune-1", "今日事业", "先完成手头安排。", "fortune"))
            store.upsert("profile-a", favorite("action-1", "丝路路线", "敦煌驿站与路线选择。", "action"))
        }
        show(store)

        compose.onNodeWithTag("reading-favorite-filter-action").performClick()
        compose.onNodeWithText("丝路路线").assertExists()
        compose.onNodeWithText("今日事业").assertDoesNotExist()

        compose.onNodeWithTag("reading-favorite-search").performTextInput("敦煌")
        compose.onNodeWithText("丝路路线").assertExists()
        compose.onNodeWithTag("reading-favorite-read-action-1").performClick()
        compose.onNodeWithText("只读快照 · 今日行动").assertExists()
        compose.onNodeWithText("敦煌驿站与路线选择。", useUnmergedTree = true).assertExists()

        compose.onNodeWithText("‹ 返回").performClick()
        compose.onNodeWithTag("reading-favorite-search").performTextClearance()
        compose.onNodeWithTag("reading-favorite-remove-action-1").performClick()
        compose.onNodeWithText("已取消收藏").assertExists()
        compose.onNodeWithText("撤销").performClick()
        compose.onNodeWithText("丝路路线").assertExists()
        assertEquals(2, runBlocking { store.list("profile-a").entries.size })
    }

    @Test
    fun empty_and_corrupt_states_require_explicit_confirmed_clear() {
        val bridge = MemoryBridge()
        val store = ReadingFavoriteStore(bridge)
        show(store)
        compose.onNodeWithText("还没有收藏", substring = true).assertExists()

        bridge.values[store.key("profile-a")] = "{damaged"
        compose.runOnIdle { activeStore.value = ReadingFavoriteStore(bridge) }
        compose.onNodeWithText("收藏数据无法读取，原始内容未被覆盖。").assertExists()
        compose.onNodeWithText("清除损坏收藏").performClick()
        compose.onNodeWithText("清除本档案收藏？").assertExists()
        compose.onNodeWithTag("reading-favorite-cancel-clear").performClick()
        compose.onNodeWithText("收藏数据无法读取，原始内容未被覆盖。").assertExists()

        compose.onNodeWithText("清除损坏收藏").performClick()
        compose.onNodeWithTag("reading-favorite-confirm-clear").performClick()
        compose.onNodeWithText("还没有收藏", substring = true).assertExists()
        assertEquals(null, runBlocking { bridge.read(store.key("profile-a")) })
    }

    private fun show(store: ReadingFavoriteStore) {
        activeStore = mutableStateOf(store)
        compose.setContent {
            MaterialTheme {
                val currentStore = activeStore.value
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    ReadingFavoritesSection(currentStore, "profile-a")
                }
            }
        }
    }

    private fun favorite(id: String, title: String, body: String, type: String) = ReadingFavorite(
        id = id,
        type = type,
        title = title,
        dateKey = "2026-10-02",
        period = "今日",
        mode = "综合",
        body = body,
        evidence = "只保存原结果",
        algorithmVersion = "x3-test",
        createdAt = if (type == "action") 2L else 1L
    )
}
