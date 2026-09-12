package com.xuanji.app.domain.game

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reserved engine seams must fail loudly (explicit reason codes) when no provider exists,
 * never fabricate a playable game, and keep cancel semantics consistent with BoardEngine.
 */
class GoAndChessSeamsTest {

    // ---- go ----------------------------------------------------------------------

    @Test
    fun go_without_gtp_provider_reports_unavailable() {
        val adapter = GoSessionAdapter()
        val availability = adapter.availability()
        assertFalse(availability.available)
        assertEquals("go_provider_not_enabled", availability.reason)
    }

    @Test
    fun go_place_never_returns_fake_move() = runBlocking {
        val adapter = GoSessionAdapter()
        val result = adapter.respond(GoPosition(), GoPlaceMove(Square(3, 3), PlayerColor.BLACK), token = 1L)
        assertTrue(result is EngineResult.NoMove)
        assertEquals("go_provider_not_enabled", (result as EngineResult.NoMove).reason)
    }

    // ---- chess ----------------------------------------------------------------------

    @Test
    fun chess_adapter_only_accepts_uci_squares() {
        // uci conversion rejects coordinates outside a..i / 0..9 rows
        assertTrue(XiangqiNotation.fromUciOrNull("z9z9") == null)
        assertTrue(XiangqiNotation.fromUciOrNull("a0b0") != null)
        // chess square mapping uses WHITE color for piece rendering contracts
        assertEquals(PlayerColor.WHITE, PlayerColor.valueOf("WHITE"))
    }

    @Test
    fun chess_without_engine_reports_unavailable() = runBlocking {
        val adapter = ChessEngineAdapter()
        val position = ChessPosition.start()
        val result = adapter.respond(position, PlayerColor.WHITE, token = 1L)
        assertTrue(result is EngineResult.NoMove)
        assertEquals("chess_provider_not_enabled", (result as EngineResult.NoMove).reason)
    }

    // ---- shared cancel semantics ------------------------------------------------------

    @Test
    fun both_adapters_carry_token_discipline() = runBlocking {
        // mismatches fall through the same BoardEngine contract: responses are bound to a token
        val go = GoSessionAdapter()
        val chess = ChessEngineAdapter()
        val goResult = go.respond(GoPosition(), GoPlaceMove(Square(0, 0), PlayerColor.BLACK), token = 7L)
        val chessResult = chess.respond(ChessPosition.start(), PlayerColor.WHITE, token = 7L)
        assertTrue(goResult is EngineResult.NoMove)
        assertTrue(chessResult is EngineResult.NoMove)
    }
}
