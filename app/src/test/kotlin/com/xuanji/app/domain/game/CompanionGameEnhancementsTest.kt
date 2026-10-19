package com.xuanji.app.domain.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionGameEnhancementsTest {
    @Test
    fun poetry_has_five_fixed_prompts_hint_undo_progress_and_replayable_result() {
        assertTrue(PoetryChainEngine.levelCount >= 5)
        assertTrue(PoetryChainEngine.prompt(PoetryChainState()).hint.isNotBlank())
        val hinted = PoetryChainEngine.reduce(PoetryChainState(), PoetryChainEvent.Hint)
        assertTrue(hinted.feedback.isNotBlank())
        val answered = PoetryChainEngine.reduce(hinted, PoetryChainEvent.Choose(PoetryChainEngine.prompt(hinted).options.first()))
        assertEquals(1, answered.promptIndex)
        val undone = PoetryChainEngine.reduce(answered, PoetryChainEvent.Undo)
        assertEquals(0, undone.promptIndex)
        assertEquals(0, undone.score)
        assertTrue(PoetryChainEngine.isValidState(undone))

        var state = PoetryChainState()
        repeat(PoetryChainEngine.levelCount) {
            state = PoetryChainEngine.reduce(state, PoetryChainEvent.Choose(PoetryChainEngine.prompt(state).options.first()))
        }
        assertTrue(state.completed)
        assertEquals(PoetryChainEngine.levelCount, state.promptIndex)
        assertTrue(PoetryChainEngine.result(state).summary.contains("${PoetryChainEngine.levelCount} 题"))
        assertEquals(PoetryChainState(), PoetryChainEngine.reduce(state, PoetryChainEvent.Restart))
    }

    @Test
    fun star_map_has_fixed_progressive_puzzles_hint_undo_and_next_level() {
        assertTrue(StarMapPuzzleEngine.puzzleCount >= 5)
        var state = StarMapPuzzleEngine.initial()
        val puzzle = StarMapPuzzleEngine.puzzle(state)
        assertTrue(puzzle.hint.isNotBlank())
        state = StarMapPuzzleEngine.reduce(state, StarMapEvent.Hint)
        state = StarMapPuzzleEngine.reduce(state, StarMapEvent.Move(puzzle.solution[1]))
        state = StarMapPuzzleEngine.reduce(state, StarMapEvent.Undo)
        assertEquals(listOf(puzzle.start), state.path)
        assertEquals(puzzle.start, state.current)
        assertFalse(state.completed)

        puzzle.solution.drop(1).forEach { node -> state = StarMapPuzzleEngine.reduce(state, StarMapEvent.Move(node)) }
        assertTrue(state.completed)
        assertTrue(StarMapPuzzleEngine.hasNextPuzzle(state))
        val next = StarMapPuzzleEngine.reduce(state, StarMapEvent.NextPuzzle)
        assertEquals(state.puzzleIndex + 1, next.puzzleIndex)
        assertEquals(StarMapPuzzleEngine.puzzle(next).start, next.current)
        assertTrue(StarMapPuzzleEngine.isValidState(next))
    }

    @Test
    fun silk_road_undo_recomputes_remaining_resources_and_replay_uses_fixed_legs() {
        var state = SilkRoadState()
        state = SilkRoadRouteEngine.reduce(state, SilkRoadEvent.Travel("敦煌"))
        state = SilkRoadRouteEngine.reduce(state, SilkRoadEvent.Travel("喀什"))
        assertEquals(2, SilkRoadRouteEngine.replay(state).size)

        val undone = SilkRoadRouteEngine.reduce(state, SilkRoadEvent.Undo)
        assertEquals("敦煌", undone.current)
        assertEquals(listOf("长安", "敦煌"), undone.path)
        assertEquals(6, undone.daysLeft)
        assertEquals(9, undone.budgetLeft)
        assertFalse(undone.completed)
        assertTrue(SilkRoadRouteEngine.isValidState(undone))
        assertEquals(SilkRoadState(), SilkRoadRouteEngine.reduce(undone, SilkRoadEvent.Restart))
    }
}
