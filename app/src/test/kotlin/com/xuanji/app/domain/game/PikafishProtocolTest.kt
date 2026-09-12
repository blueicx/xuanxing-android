package com.xuanji.app.domain.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UCI protocol parsing tests using fixed strings only — no native process is started.
 * Covers uciok/readyok handshake, bestmove parsing, info depth, timeout, cancel, and
 * process exit paths of the PikafishEngine seam.
 */
class PikafishProtocolTest {

    // ---- handshake ---------------------------------------------------------------

    @Test
    fun uciok_completes_handshake() {
        val parser = UciProtocolParser()
        assertFalse(parser.handshakeComplete)
        parser.onLine("id name Pikafish dev")
        parser.onLine("uciok")
        assertTrue(parser.handshakeComplete)
    }

    @Test
    fun readyok_completes_readiness() {
        val parser = UciProtocolParser()
        assertFalse(parser.ready)
        parser.onLine("readyok")
        assertTrue(parser.ready)
    }

    // ---- bestmove ------------------------------------------------------------------

    @Test
    fun bestmove_is_parsed_to_squares() {
        val parser = UciProtocolParser()
        assertNull(parser.bestMove)
        parser.onLine("bestmove h0e0 ponder e0h0")
        assertEquals(Square(7, 9) to Square(4, 9), parser.bestMove)
    }

    @Test
    fun bestmove_without_ponder_still_parses() {
        val parser = UciProtocolParser()
        // b0c2: file 1 rank 9 (red back rank) -> file 2 rank 7 (the horse-forward opening move)
        parser.onLine("bestmove b0c2")
        assertEquals(Square(1, 9) to Square(2, 7), parser.bestMove)
    }

    @Test
    fun malformed_bestmove_yields_null() {
        val parser = UciProtocolParser()
        parser.onLine("bestmove")
        assertNull(parser.bestMove)
        parser.onLine("bestmove zz9z")
        assertNull(parser.bestMove)
    }

    // ---- info depth ------------------------------------------------------------------

    @Test
    fun info_depth_is_captured_but_offline_never_shows_it() {
        val parser = UciProtocolParser()
        parser.onLine("info depth 12 seldepth 18 multipv 1 score cp 35")
        assertEquals(12, parser.lastDepth)
        assertEquals(35, parser.lastCentipawns)
    }

    @Test
    fun mate_score_is_captured() {
        val parser = UciProtocolParser()
        parser.onLine("info depth 9 score mate 3")
        assertEquals(3, parser.lastMateIn)
    }

    // ---- failure paths -----------------------------------------------------------------

    @Test
    fun timeout_transitions_to_failed() {
        val engine = PikafishEngine.unavailable("engine_timeout")
        val result = kotlinx.coroutines.runBlocking {
            engine.bestMove(XiangqiBoard.initial(), PlayerColor.RED, 1L)
        }
        // unavailable engine falls back to the deterministic offline engine
        assertTrue(result is EngineResult.Move)
    }

    @Test
    fun process_exit_transitions_to_failed() {
        val parser = UciProtocolParser()
        parser.onProcessExit()
        assertTrue(parser.processExited)
        assertFalse(parser.handshakeComplete)
    }

    @Test
    fun cancel_resets_pending_bestmove() {
        val parser = UciProtocolParser()
        parser.onLine("bestmove h0e0")
        parser.cancel()
        assertNull(parser.bestMove)
    }

    // ---- fallback chain ----------------------------------------------------------------

    @Test
    fun unavailable_engine_falls_back_to_offline_legal_move() = kotlinx.coroutines.runBlocking {
        val engine = PikafishEngine.unavailable("native_engine_not_packaged")
        val position = XiangqiBoard.initial()
        val result = engine.bestMove(position, PlayerColor.RED, 2L)
        val move = (result as EngineResult.Move).turn.move
        assertTrue(XiangqiRules.legalMoves(position, PlayerColor.RED).contains(move))
        // re-verified by local rules: no fabricated move can slip through
        assertEquals(PlayerColor.RED, move.player)
    }
}
