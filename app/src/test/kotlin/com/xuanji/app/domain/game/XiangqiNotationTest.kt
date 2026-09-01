package com.xuanji.app.domain.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XiangqiNotationTest {

    // Convention: file 0..8 -> Red numerals 九..一 (file 0 = 九), Black numerals 1..9 (file 0 = 1).
    // rank 9 is the Red back rank; rank 0 the Black back rank. Red advances by decreasing rank.
    // Diagonal pieces (马相仕) record the destination file after 进/退.
    // With a 前/后 prefix the source file numeral is omitted.

    @Test
    fun initial_cannon_sideways_formats_and_round_trips() {
        val position = XiangqiBoard.initial()
        // red cannon on file 7 (二) moving sideways to file 4 (五) => 炮二平五
        val move = BoardMove(Square(7, 7), Square(4, 7), "炮二平五", player = PlayerColor.RED)
        assertEquals("炮二平五", XiangqiNotation.format(move, position))
        val parsed = XiangqiNotation.parse("炮二平五", position)
        assertEquals(Square(7, 7), parsed.from)
        assertEquals(Square(4, 7), parsed.to)
    }

    @Test
    fun horse_move_records_destination_file_and_round_trips() {
        val position = XiangqiBoard.initial()
        // red horse file 1 (九) to file 2 (八)? standard opening is 马八进七: file 1 (八) -> file 2 (七)
        val move = BoardMove(Square(1, 9), Square(2, 7), "马八进七", player = PlayerColor.RED)
        assertEquals("马八进七", XiangqiNotation.format(move, position))
        val parsed = XiangqiNotation.parse("马八进七", position)
        assertEquals(Square(1, 9), parsed.from)
        assertEquals(Square(2, 7), parsed.to)
    }

    @Test
    fun pawn_advance_uses_step_count() {
        val position = XiangqiBoard.initial()
        val move = BoardMove(Square(0, 6), Square(0, 5), "兵九进一", player = PlayerColor.RED)
        assertEquals("兵九进一", XiangqiNotation.format(move, position))
        val parsed = XiangqiNotation.parse("兵九进一", position)
        assertEquals(Square(0, 5), parsed.to)
    }

    @Test
    fun ambiguous_two_rooks_use_front_or_rear_prefix_without_file_numeral() {
        val custom = XiangqiBoard.empty()
            .withPiece(Square(3, 9), Piece(PlayerColor.RED, PieceKind.GENERAL))
            .withPiece(Square(5, 0), Piece(PlayerColor.BLACK, PieceKind.GENERAL))
            .withPiece(Square(0, 5), Piece(PlayerColor.RED, PieceKind.ROOK))
            .withPiece(Square(0, 2), Piece(PlayerColor.RED, PieceKind.ROOK))
        // front rook = the one deeper in enemy territory (rank 2 for Red) => 前车
        val frontSideways = BoardMove(Square(0, 2), Square(3, 2), "", player = PlayerColor.RED)
        assertEquals("前车平六", XiangqiNotation.format(frontSideways, custom))
        val parsed = XiangqiNotation.parse("前车平六", custom)
        assertEquals(Square(0, 2), parsed.from)
        assertEquals(Square(3, 2), parsed.to)
        // rear rook (rank 5) straight advance one => 后车进一
        val rearAdvance = BoardMove(Square(0, 5), Square(0, 4), "", player = PlayerColor.RED)
        assertEquals("后车进一", XiangqiNotation.format(rearAdvance, custom))
    }

    @Test
    fun black_moves_use_black_side_arabic_numerals() {
        val position = XiangqiBoard.initial()
        // black horse on file 7 (= 8 for Black) to file 6 (= 7): classic 马8进7
        val horse = BoardMove(Square(7, 0), Square(6, 2), "马8进7", player = PlayerColor.BLACK)
        assertEquals("马8进7", XiangqiNotation.format(horse, position))
        val parsedHorse = XiangqiNotation.parse("马8进7", position)
        assertEquals(Square(7, 0), parsedHorse.from)
        assertEquals(Square(6, 2), parsedHorse.to)
        // black rook on file 0 (= 1) advancing one step: 车1进1
        val rook = BoardMove(Square(0, 0), Square(0, 1), "车1进1", player = PlayerColor.BLACK)
        assertEquals("车1进1", XiangqiNotation.format(rook, position))
        val parsedRook = XiangqiNotation.parse("车1进1", position)
        assertEquals(Square(0, 1), parsedRook.to)
    }

    @Test
    fun parse_rejects_empty_garbled_and_impossible_text() {
        val position = XiangqiBoard.initial()
        assertTrue(XiangqiNotation.parseOrNull("", position) == null)
        assertTrue(XiangqiNotation.parseOrNull("你好", position) == null)
        assertTrue(XiangqiNotation.parseOrNull("炮九平九", position) == null) // same-file null move
        assertTrue(XiangqiNotation.parseOrNull("炮二进八", position) == null) // walks off board
    }

    @Test
    fun parse_rejects_when_piece_missing_or_wrong_turn() {
        val position = XiangqiBoard.initial()
        // Red has no horse on file 4 (四)
        assertTrue(XiangqiNotation.parseOrNull("马四进三", position) == null)
        // Black piece on Red's turn resolves but keeps the mover's color for the caller to reject
        val parsed = XiangqiNotation.parseOrNull("车1进3", position)
        assertTrue(parsed == null || parsed.player == PlayerColor.BLACK)
    }

    @Test
    fun uci_conversion_uses_file_letter_and_row_digit() {
        // file 7 -> 'h', rank 9 -> row '0'
        assertEquals("h0e0", XiangqiNotation.toUci(Square(7, 9), Square(4, 9)))
        val pair = XiangqiNotation.fromUci("h0e0")
        assertEquals(Square(7, 9), pair.first)
        assertEquals(Square(4, 9), pair.second)
        assertTrue(XiangqiNotation.fromUciOrNull("zz") == null)
        assertTrue(XiangqiNotation.fromUciOrNull("h0e") == null)
    }

    @Test
    fun full_width_punctuation_tolerance_in_parse() {
        val position = XiangqiBoard.initial()
        val parsed = XiangqiNotation.parseOrNull("炮二平五。", position)
        assertTrue(parsed != null)
        assertEquals(Square(4, 7), parsed!!.to)
    }
}
