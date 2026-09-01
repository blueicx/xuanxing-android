package com.xuanji.app.domain.game

import com.xuanji.app.domain.MysticIntent
import com.xuanji.app.domain.MysticIntentClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The dialogue intent classifier must reach the game path and protect existing intents. */
class MysticDialogueGameIntentTest {

    @Test
    fun game_start_phrases_classify_to_game() {
        assertEquals(MysticIntent.Game, MysticIntentClassifier.classify("来一盘象棋"))
        assertEquals(MysticIntent.Game, MysticIntentClassifier.classify("来一盘中国象棋"))
        assertEquals(MysticIntent.Game, MysticIntentClassifier.classify("下一盘象棋"))
        assertEquals(MysticIntent.Game, MysticIntentClassifier.classify("陪我下象棋"))
    }

    @Test
    fun game_notation_phrases_classify_to_game() {
        assertEquals(MysticIntent.Game, MysticIntentClassifier.classify("走炮二平五"))
        assertEquals(MysticIntent.Game, MysticIntentClassifier.classify("炮二平五"))
        assertEquals(MysticIntent.Game, MysticIntentClassifier.classify("马8进7"))
        assertEquals(MysticIntent.Game, MysticIntentClassifier.classify("悔棋"))
        assertEquals(MysticIntent.Game, MysticIntentClassifier.classify("给我提示"))
        assertEquals(MysticIntent.Game, MysticIntentClassifier.classify("复盘刚才那步"))
        assertEquals(MysticIntent.Game, MysticIntentClassifier.classify("退出棋局"))
        assertEquals(MysticIntent.Game, MysticIntentClassifier.classify("保存棋局"))
    }

    @Test
    fun everyday_phrases_are_not_misread_as_game() {
        // greeting, fortune, health, and wealth wording must keep their original intents
        assertEquals(MysticIntent.Greeting, MysticIntentClassifier.classify("你好"))
        assertEquals(MysticIntent.Fortune, MysticIntentClassifier.classify("今天运势怎么样"))
        assertEquals(MysticIntent.Health, MysticIntentClassifier.classify("最近睡眠不好"))
        assertEquals(MysticIntent.Wealth, MysticIntentClassifier.classify("最近财运如何"))
        // 车厘子 contains a piece char but no verb+numeral: must NOT reach the game path
        val cherry = MysticIntentClassifier.classify("车厘子好吃吗")
        assertTrue(cherry != MysticIntent.Game)
        // a move-like phrase stays game
        assertEquals(MysticIntent.Game, MysticIntentClassifier.classify("走炮二平五"))
    }

    @Test
    fun game_intent_value_is_stable() {
        assertEquals("game", MysticIntent.Game.value)
    }
}
