package com.xuanji.app.domain.game

import org.junit.Assert.*
import org.junit.Test

class ExpandedCompanionGamesTest {
    @Test fun legacy_completed_five_level_save_is_preserved_but_current_missing_fields_are_rejected() {
        var oldState = PoetryChainState(levelCountAtStart = 5)
        repeat(5) { oldState = PoetryChainEngine.reduce(oldState, PoetryChainEvent.Choose(PoetryChainEngine.prompt(oldState).options.first())) }
        val envelope = CompanionGameProgressEnvelope(2, "profile", "jiangnan", "poetry_chain",
            CompanionGameProgressCodec.encodeState(CompanionGameProgressState.Poetry(oldState)).replace(",\"levelCountAtStart\":5", ""), 1)
        val migrated = CompanionGameProgressCodec.decode(CompanionGameProgressCodec.encode(envelope))!!
        val state = (CompanionGameProgressCodec.decodeState(migrated) as CompanionGameProgressState.Poetry).value
        assertEquals(5, state.levelCountAtStart)
        assertTrue(state.completed)
        val malformed = envelope.copy(version = CompanionGameProgressCodec.VERSION)
        val decoded = CompanionGameProgressCodec.decode(CompanionGameProgressCodec.encode(malformed))
        assertTrue(decoded == null || CompanionGameProgressCodec.decodeState(decoded) == null)
    }
    @Test fun unknown_poetry_option_does_not_advance_or_corrupt_save() {
        val initial = PoetryChainState()
        val result = PoetryChainEngine.reduce(initial, PoetryChainEvent.Choose("不是当前选项"))
        assertEquals(0, result.promptIndex)
        assertTrue(PoetryChainEngine.isValidState(result))
    }
    @Test fun all_poetry_levels_finish_and_completed_answer_can_be_undone() {
        var state = PoetryChainState()
        repeat(24) {
            state = PoetryChainEngine.reduce(state, PoetryChainEvent.Choose(PoetryChainEngine.prompt(state).options.first()))
            assertTrue(PoetryChainEngine.isValidState(state))
        }
        assertTrue(state.completed)
        state = PoetryChainEngine.reduce(state, PoetryChainEvent.Undo)
        assertEquals(23, state.promptIndex)
        assertFalse(state.completed)
        assertTrue(PoetryChainEngine.isValidState(state))
    }
    @Test fun completed_star_level_can_undo_last_link() {
        var state = StarMapPuzzleEngine.initial()
        StarMapPuzzleEngine.puzzle(state).solution.drop(1).forEach {
            state = StarMapPuzzleEngine.reduce(state, StarMapEvent.Move(it))
        }
        assertTrue(state.completed)
        val undone = StarMapPuzzleEngine.reduce(state, StarMapEvent.Undo)
        assertFalse(undone.completed)
        assertEquals(state.path.dropLast(1), undone.path)
        assertTrue(StarMapPuzzleEngine.isValidState(undone))
    }
    @Test fun all_star_levels_complete_and_restart_independently() {
        repeat(24) { index ->
            var state = StarMapPuzzleEngine.initial(index)
            StarMapPuzzleEngine.puzzle(state).solution.drop(1).forEach {
                state = StarMapPuzzleEngine.reduce(state, StarMapEvent.Move(it))
                assertTrue(StarMapPuzzleEngine.isValidState(state))
            }
            assertTrue(state.completed)
            assertEquals(StarMapPuzzleEngine.initial(index), StarMapPuzzleEngine.reduce(state, StarMapEvent.RestartPuzzle))
        }
    }
    @Test fun poetry_has_24_distinct_sourced_levels_and_undo() {
        assertEquals(24, PoetryChainEngine.levelCount)
        val prompts = (0 until 24).map { PoetryChainEngine.prompt(PoetryChainState(promptIndex = it)) }
        assertEquals(24, prompts.map { it.id }.toSet().size)
        assertTrue(prompts.all { it.options.size >= 2 && it.hint.isNotBlank() })
    }
    @Test fun star_map_has_24_solvable_distinct_levels() {
        assertEquals(24, StarMapPuzzleEngine.puzzleCount)
        (0 until 24).forEach { index ->
            val puzzle = StarMapPuzzleEngine.puzzle(StarMapState(puzzleIndex = index))
            assertEquals(puzzle.start, puzzle.solution.first())
            assertEquals(puzzle.target, puzzle.solution.last())
            assertTrue(puzzle.solution.zipWithNext().all { (from, to) -> to in puzzle.edges[from].orEmpty() })
        }
    }
}
