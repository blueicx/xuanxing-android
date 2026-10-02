package com.xuanji.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.xuanji.app.domain.game.GameOutcome
import com.xuanji.app.domain.game.PlayerColor

class CompanionGameFavoriteFactoryTest {
    @Test
    fun explicitly_selected_game_summary_is_saved_as_a_profile_free_local_favorite() {
        val favorite = CompanionGameFavoriteFactory.fromResult(
            id = "game-summary-test-1",
            characterId = MysticCharacterId.NadirRashid,
            gameId = "silkroad_route",
            dateKey = "2026-10-02",
            summary = "本局抵达撒马尔罕：长安 → 敦煌 → 喀什 → 撒马尔罕；剩余 1 天、0 点资源。",
            sourceNote = "依据本机保存的丝路路线进度。"
        )

        assertEquals("game", favorite?.type)
        assertEquals("silkroad_route", favorite?.gameId)
        assertEquals("nadir_rashid", favorite?.characterId)
        assertEquals("本局", favorite?.period)
        assertEquals("2026-10-02", favorite?.dateKey)
        assertTrue(favorite?.body?.contains("长安 → 敦煌 → 喀什 → 撒马尔罕") == true)
        assertTrue(favorite?.evidence?.contains("本机保存") == true)
        val encoded = ReadingFavoriteCodec.encode("profile-digest-only", listOf(requireNotNull(favorite)))
        assertFalse(encoded.contains("profileKey"))
    }

    @Test
    fun favorite_factory_rejects_game_owned_by_another_character() {
        assertNull(
            CompanionGameFavoriteFactory.fromResult(
                id = "wrong-owner",
                characterId = MysticCharacterId.ShenYanzhou,
                gameId = "silkroad_route",
                dateKey = "2026-10-02",
                summary = "不应保存",
                sourceNote = ""
            )
        )
    }

    @Test
    fun xiangqi_favorite_summary_requires_a_real_finished_result() {
        assertNull(CompanionGameFavoriteFactory.xiangqiSummary(GameOutcome.InProgress, 12, ""))
        assertNull(CompanionGameFavoriteFactory.xiangqiSummary(GameOutcome.Check(PlayerColor.RED), 12, ""))
        assertEquals(
            "中国象棋本局结束：和棋；棋谱复核后共 12 个有效半回合。",
            CompanionGameFavoriteFactory.xiangqiSummary(GameOutcome.Draw, 12, "")
        )
    }
}
