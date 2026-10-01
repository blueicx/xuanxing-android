package com.xuanji.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xuanji.app.domain.MysticCharacterCatalog
import com.xuanji.app.domain.MysticCharacterId
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MysticStageLayoutTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun every_character_uses_the_same_accessible_stage_layers() {
        val selectedCharacter = mutableStateOf(MysticCharacterCatalog.all.first())
        compose.setContent {
            val character = selectedCharacter.value
            MysticStageLayout(
                character = character,
                skinId = character.legacySkinId,
                garment = androidx.compose.ui.graphics.Color(0xFF30203F),
                trimColor = androidx.compose.ui.graphics.Color(0xFFD9C58B),
                moodLevel = 0f,
                onCharacterSelected = { id ->
                    selectedCharacter.value = MysticCharacterCatalog.byId(id)
                },
                onConversation = {},
                onStartGame = {},
                onClose = {},
                content = {}
            )
        }

        MysticCharacterCatalog.all.forEach { character ->
            compose.runOnIdle { selectedCharacter.value = character }
            compose.onNodeWithTag("stage-current-character-name").assertTextEquals(character.displayName)
            compose.onNodeWithTag("stage-current-character-title").assertTextEquals(characterUiTitle(character))
            val visualDescription = when (MysticVisualAssetCatalog.forCharacter(character).renderMode) {
                MysticAssetRenderMode.CompleteScene -> "${character.displayName}完整文化场景"
                MysticAssetRenderMode.ForegroundOnScene,
                MysticAssetRenderMode.CanvasFallback -> MysticSceneCatalog.forCharacter(character).accessibilityDescription
            }
            compose.onNodeWithContentDescription(visualDescription).assertExists()
            compose.onNodeWithContentDescription("关闭玄师台").assertExists()
            compose.onNodeWithText("进入对话").assertExists()
            compose.onNodeWithText("查看专长").assertExists()
            compose.onNodeWithText("进入${com.xuanji.app.domain.game.CompanionGameCatalog.forCharacter(character.id).title}")
                .assertExists()
        }
    }

    @Test
    fun close_button_is_a_minimum_target_and_calls_close() {
        var closed = false
        val character = MysticCharacterCatalog.byId(MysticCharacterId.MoHeng)
        compose.setContent {
            MysticStageLayout(
                character = character,
                skinId = character.legacySkinId,
                garment = androidx.compose.ui.graphics.Color(0xFF30203F),
                trimColor = androidx.compose.ui.graphics.Color(0xFFD9C58B),
                moodLevel = 0f,
                onCharacterSelected = {},
                onConversation = {},
                onStartGame = {},
                onClose = { closed = true },
                content = {}
            )
        }

        compose.onNodeWithContentDescription("关闭玄师台").performClick()
        assertTrue(closed)
    }

    @Test
    fun narrow_stage_keeps_title_close_and_primary_action_discoverable() {
        val character = MysticCharacterCatalog.byId(MysticCharacterId.EvelynNova)
        compose.setContent {
            Box(Modifier.width(280.dp).height(480.dp)) {
                MysticStageLayout(
                    character = character,
                    skinId = character.legacySkinId,
                    garment = androidx.compose.ui.graphics.Color(0xFF30203F),
                    trimColor = androidx.compose.ui.graphics.Color(0xFFD9C58B),
                    moodLevel = 0f,
                    onCharacterSelected = {},
                    onConversation = {},
                    onStartGame = {},
                    onClose = {},
                    content = {}
                )
            }
        }

        compose.onNodeWithTag("stage-current-character-name").assertTextEquals(character.displayName)
        compose.onNodeWithContentDescription("关闭玄师台").assertExists()
        compose.onNodeWithText("进入对话").assertExists()
    }

    @Test
    fun drawer_starts_collapsed_and_expands_from_a_real_tap() {
        val character = MysticCharacterCatalog.byId(MysticCharacterId.ShenYanzhou)
        compose.setContent {
            MysticStageLayout(
                character = character,
                skinId = character.legacySkinId,
                garment = androidx.compose.ui.graphics.Color(0xFF30203F),
                trimColor = androidx.compose.ui.graphics.Color(0xFFD9C58B),
                moodLevel = 0f,
                onCharacterSelected = {},
                onConversation = {},
                onStartGame = {},
                onClose = {},
                content = { Text("真实对话内容") }
            )
        }

        compose.onNodeWithTag("stage-drawer-peek").assertExists().performClick()
        compose.onNodeWithTag("stage-drawer-expanded").assertExists()
        compose.onNodeWithContentDescription("收起对话抽屉").performClick()
        compose.onNodeWithTag("stage-drawer-peek").assertExists()
    }

    private fun characterUiTitle(character: com.xuanji.app.domain.MysticCharacterProfile): String =
        "${character.title} · ${MysticSceneCatalog.forCharacter(character).title}"
}
