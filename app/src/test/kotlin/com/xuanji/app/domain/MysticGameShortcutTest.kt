package com.xuanji.app.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class MysticGameShortcutTest {
    @Test
    fun game_shortcut_text_is_classified_as_game() {
        assertEquals(MysticIntent.Game, MysticIntentClassifier.classify("来一盘象棋"))
    }
}
