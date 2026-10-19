package com.xuanji.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xuanji.app.data.local.CompanionGameProgressStore
import com.xuanji.app.data.local.PreferenceBridge
import com.xuanji.app.domain.MysticCharacterId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompanionGamesInteractionTest {
    @get:Rule val compose = createComposeRule()

    private class MemoryBridge : PreferenceBridge {
        private val values = mutableMapOf<String, String>()
        override suspend fun read(key: String): String? = values[key]
        override suspend fun write(key: String, value: String) { values[key] = value }
        override suspend fun delete(key: String) { values.remove(key) }
    }

    @Test
    fun star_map_hint_undo_next_level_and_reopen_restore_use_memory_storage() {
        val store = CompanionGameProgressStore(MemoryBridge()) { 5L }
        compose.setContent {
            var open by mutableStateOf(true)
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (open) CompanionGameCard(
                        gameId = "star_map_observation",
                        characterId = MysticCharacterId.EvelynNova,
                        profileKey = "synthetic-star-profile",
                        onClose = { open = false },
                        progressStoreOverride = store
                    ) else Button(onClick = { open = true }) { Text("重新打开游戏") }
                }
            }
        }

        compose.onNodeWithTag("starmap-hint").performClick()
        compose.onNodeWithText("提示：先走到唯一相邻的‘天津’，再前往目标。").assertExists()
        compose.onNodeWithText("观测 天津").performClick()
        compose.onNodeWithTag("starmap-undo").performClick()
        compose.onNodeWithText("当前位置：织女").assertExists()
        compose.onNodeWithText("观测 天津").performClick()
        compose.onNodeWithText("观测 天狼").performClick()
        compose.onNodeWithTag("starmap-result-summary").assertExists()
        compose.onNodeWithTag("starmap-next-level").performClick()
        compose.onNodeWithText("第 2 / 24 题 · 北斗一 → 北斗三").assertExists()
        compose.onNodeWithText("收起").performClick()
        compose.onNodeWithText("重新打开游戏").performClick()
        compose.onNodeWithText("第 2 / 24 题 · 北斗一 → 北斗三").assertExists()
    }

    @Test
    fun poetry_hint_and_undo_are_real_controls_and_fixed_progress_advances() {
        val store = CompanionGameProgressStore(MemoryBridge()) { 6L }
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    CompanionGameCard(
                        gameId = "poetry_chain",
                        characterId = MysticCharacterId.ShenYanzhou,
                        profileKey = "synthetic-poetry-profile",
                        onClose = {},
                        progressStoreOverride = store
                    )
                }
            }
        }

        compose.onNodeWithTag("poetry-hint").performClick()
        compose.onNodeWithText("提示：句尾可用‘中’或‘空’的韵脚。").assertExists()
        compose.onNodeWithTag("poetry-option-0").performClick()
        compose.onNodeWithText("第 2 / 24 题 · 接下一句：一盏照书夜").assertExists()
        compose.onNodeWithTag("poetry-undo").performClick()
        compose.onNodeWithText("第 1 / 24 题 · 接下一句：小桥收晚风").assertExists()
    }

    @Test
    fun silk_road_undo_restores_route_and_resource_totals_and_shows_replay_legs() {
        val store = CompanionGameProgressStore(MemoryBridge()) { 7L }
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    CompanionGameCard(
                        gameId = "silkroad_route",
                        characterId = MysticCharacterId.NadirRashid,
                        profileKey = "synthetic-silk-profile",
                        onClose = {},
                        progressStoreOverride = store
                    )
                }
            }
        }

        compose.onNodeWithText("前往敦煌（2天·3资源）").performScrollTo().performClick()
        compose.onNodeWithText("前往喀什（3天·4资源）").performScrollTo().performClick()
        compose.onNodeWithTag("silkroad-replay-leg-1").assertExists()
        compose.onNodeWithText("撤销上一步").performScrollTo().performClick()
        compose.onNodeWithText("敦煌 · 剩余 6 天 / 9 点资源").assertExists()
        compose.onNodeWithTag("silkroad-route-path").assertExists()
        compose.onNodeWithTag("silkroad-route-path").assertTextContains("长安 → 敦煌", substring = true)
    }
}
