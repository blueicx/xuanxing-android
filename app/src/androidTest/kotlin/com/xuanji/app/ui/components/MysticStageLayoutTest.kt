package com.xuanji.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
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
        MysticCharacterCatalog.all.forEach { character ->
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
                    content = {}
                )
            }

            compose.onNodeWithText(character.displayName).assertExists()
            compose.onNodeWithText(characterUiTitle(character)).assertExists()
            compose.onNodeWithContentDescription(
                MysticSceneCatalog.forCharacter(character).accessibilityDescription
            ).assertExists()
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

        compose.onNodeWithText(character.displayName).assertExists()
        compose.onNodeWithContentDescription("关闭玄师台").assertExists()
        compose.onNodeWithText("进入对话").assertExists()
    }

    private fun characterUiTitle(character: com.xuanji.app.domain.MysticCharacterProfile): String =
        "${character.title} · ${MysticSceneCatalog.forCharacter(character).title}"
}
