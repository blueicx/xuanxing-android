package com.xuanji.app.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xuanji.app.domain.MysticCharacterCatalog
import com.xuanji.app.domain.MysticCharacterId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MysticCharacterGalleryTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun gallery_exposes_all_four_characters_and_accessible_switch_targets() {
        compose.setContent {
            MysticCharacterGallery(
                selectedId = MysticCharacterId.ShenYanzhou,
                onSelected = {}
            )
        }

        MysticCharacterCatalog.all.forEach { character ->
            compose.onNodeWithTag("character-card-${character.id.key}").assertExists()
            compose.onNodeWithContentDescription(
                "${character.displayName}，${character.title}，${character.cultureLabel}"
            ).assertExists()
        }
        compose.onNodeWithContentDescription("玄师角色画廊，可左右滑动切换角色").assertExists()
    }

    @Test
    fun action_bar_exposes_conversation_specialty_and_xiangqi_actions() {
        val character = MysticCharacterCatalog.byId(MysticCharacterId.EvelynNova)
        compose.setContent {
            MysticCharacterActionBar(
                character = character,
                onConversation = {},
                onSpecialties = {},
                onGame = {}
            )
        }

        compose.onNodeWithText("进入对话").assertExists()
        compose.onNodeWithText("查看专长").assertExists()
        compose.onNodeWithText("来一盘象棋").assertExists()
        compose.onNodeWithContentDescription("与${character.displayName}进入对话").assertExists()
        compose.onNodeWithContentDescription("查看${character.displayName}的专长").assertExists()
        compose.onNodeWithContentDescription("与${character.displayName}来一盘象棋")
            .performClick()
    }
}
