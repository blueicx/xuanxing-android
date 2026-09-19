package com.xuanji.app.domain.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contract tests that freeze the shared game vocabulary before any rules engine exists. */
class GoContractsTest {

    @Test
    fun gameType_and_move_are_serializable_contract_values() {
        val move = BoardMove(from = Square(0, 0), to = Square(0, 1), notation = "车九进一")
        assertEquals(GameType.XIANGQI, GameType.valueOf("XIANGQI"))
        assertEquals("车九进一", move.notation)
    }

    @Test
    fun go_contract_is_explicitly_unavailable_until_provider_is_supplied() {
        val result = GoRulesAvailability.unavailable("go_provider_not_enabled")
        assertFalse(result.available)
        assertEquals("go_provider_not_enabled", result.reason)
    }

    @Test
    fun player_color_and_position_are_stable_value_objects() {
        assertEquals(PlayerColor.RED, PlayerColor.valueOf("RED"))
        assertEquals(PlayerColor.BLACK, PlayerColor.valueOf("BLACK"))
        assertEquals(PlayerColor.WHITE, PlayerColor.valueOf("WHITE"))
        assertEquals(Square(4, 5), Square(file = 4, rank = 5))
    }

    @Test
    fun move_capture_and_player_default_to_contract_shape() {
        val move = BoardMove(from = Square(1, 2), to = Square(1, 4), notation = "马八进七")
        assertTrue(move.captured == null)
        assertEquals(PlayerColor.RED, move.player)
    }

    @Test
    fun engine_evaluation_and_turn_carry_optional_metrics_only() {
        val evaluation = EngineEvaluation(centipawns = 35, mateIn = null, depth = 6)
        val turn = EngineTurn(BoardMove(Square(7, 2), Square(7, 5), "炮二平五"), evaluation)
        assertEquals(35, turn.evaluation?.centipawns)
        assertEquals("炮二平五", turn.move.notation)
    }

    @Test
    fun go_position_and_place_move_are_reserved_but_inert() {
        val position = GoPosition(captures = mapOf(PlayerColor.BLACK to 3, PlayerColor.WHITE to 0))
        val placement = GoPlaceMove(Square(3, 3), PlayerColor.BLACK)
        assertFalse(position.playable)
        assertEquals(PlayerColor.BLACK, placement.color)
    }

    @Test
    fun go_provider_reason_codes_are_not_silent_fakes() {
        assertEquals("go_provider_not_enabled", GoRulesAvailability.PROVIDER_NOT_ENABLED)
        val unavailable = GoRulesAvailability.unavailable(GoRulesAvailability.PROVIDER_NOT_ENABLED)
        assertFalse(unavailable.available)
    }
}
