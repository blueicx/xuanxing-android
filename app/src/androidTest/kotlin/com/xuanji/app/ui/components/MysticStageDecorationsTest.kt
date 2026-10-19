package com.xuanji.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MysticStageDecorationsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun extracted_stage_speech_and_persona_button_keep_text_and_click_behavior() {
        var clicked = false
        compose.setContent {
            MaterialTheme {
                Column {
                    MysticStageSpeech("人物旁的短句", Color(0xFFB78E58))
                    MysticPersonaButton(
                        title = "沈砚舟",
                        subtitle = "江南书生",
                        selected = true,
                        accent = Color(0xFFB78E58),
                        onClick = { clicked = true }
                    )
                }
            }
        }

        compose.onNodeWithText("人物旁的短句").assertExists()
        compose.onNodeWithText("沈砚舟").performClick()
        assertTrue(clicked)
    }
}
