package com.xuanji.app.domain.game

import org.junit.Assert.*
import org.junit.Test

class CompanionGameTeachingTest {
    @Test fun allStarLessonsHaveActualShortestLegalRoutesAndDoNotChangeProgress() {
        for (index in 0 until StarMapPuzzleEngine.puzzleCount) {
            val initial = StarMapPuzzleEngine.initial(index)
            val puzzle = StarMapPuzzleEngine.puzzle(initial)
            val route = CompanionGameTeaching.shortestStarPath(initial)
            assertEquals(puzzle.start, route.first())
            assertEquals(puzzle.target, route.last())
            assertTrue(route.zipWithNext().all { (a, b) -> b in puzzle.edges[a].orEmpty() })
            assertFalse(initial.completed)
        }
    }
    @Test fun antithesisReportsStructureOnlyAndDoesNotPretendToKnowClassicalTones() {
        val matching = CompanionGameTeaching.poetryStructure("小桥收晚风", "灯影落江中")
        assertTrue(matching.equalLength)
        assertTrue(matching.explanation.contains("不判断平仄"))
        assertFalse(CompanionGameTeaching.poetryStructure("小桥收晚风", "太短").equalLength)
    }
    @Test fun chessLessonAcceptsOnlyMovesVerifiedOnTheActualBoard() {
        val position = XiangqiBoard.initial()
        val legal = XiangqiRules.legalMoves(position, position.sideToMove).first()
        assertTrue(CompanionGameTeaching.xiangqiSteps(position, legal).size >= 3)
        assertTrue(CompanionGameTeaching.xiangqiSteps(position, BoardMove(notation = "", from = Square(4, 9), to = Square(0, 0))).isEmpty())
    }
}
