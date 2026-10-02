package com.xuanji.app.domain.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionFeedbackRankingTest {
    @Test
    fun accepted_feedback_boosts_same_category_candidate_by_at_most_five() {
        val candidates = listOf(Candidate("a", 90), Candidate("b", 94))
        val feedback = (1..20).map { index ->
            actionFeedback("accepted-$index", "meal", "a", ActionFeedbackKind.Accepted)
        }

        val ranked = applyExplicitFeedback(candidates, feedback, "meal", DATE)

        assertEquals(listOf("a", "b"), ranked.map { it.key })
        assertEquals(95, ranked.first().score)
    }

    @Test
    fun not_suitable_feedback_penalty_is_capped_at_eight_and_keeps_score_floor() {
        val candidates = listOf(Candidate("a", 50), Candidate("b", 43), Candidate("c", 5))
        val feedback = buildList {
            repeat(20) { index -> add(actionFeedback("not-suitable-a-$index", "activity", "a", ActionFeedbackKind.NotSuitable)) }
            repeat(20) { index -> add(actionFeedback("not-suitable-c-$index", "activity", "c", ActionFeedbackKind.NotSuitable)) }
        }

        val ranked = applyExplicitFeedback(candidates, feedback, "activity", DATE)

        assertEquals(listOf("b", "a", "c"), ranked.map { it.key })
        assertEquals(42, ranked[1].score)
        assertEquals(0, ranked[2].score)
        assertTrue(ranked.all { it.score >= 0 })
    }

    @Test
    fun replaced_candidate_is_hidden_only_for_same_category_and_date() {
        val candidates = listOf(Candidate("a", 90), Candidate("b", 80))
        val feedback = listOf(actionFeedback("replace", "meal", "a", ActionFeedbackKind.Replaced))

        val sameCategoryAndDate = applyExplicitFeedback(candidates, feedback, "meal", DATE)
        val otherCategory = applyExplicitFeedback(candidates, feedback, "outing", DATE)
        val otherDate = applyExplicitFeedback(candidates, feedback, "meal", "2026-10-03")

        assertEquals(listOf("b"), sameCategoryAndDate.map { it.key })
        assertEquals(listOf("a", "b"), otherCategory.map { it.key })
        assertEquals(listOf("a", "b"), otherDate.map { it.key })
    }

    @Test
    fun feedback_isolated_by_category_and_never_reintroduces_filtered_candidates() {
        val onlyEligibleCandidates = listOf(Candidate("eligible", 50))
        val feedback = listOf(
            actionFeedback("accepted", "outing", "eligible", ActionFeedbackKind.Accepted),
            actionFeedback("excluded", "meal", "hard-filtered", ActionFeedbackKind.Accepted)
        )

        val ranked = applyExplicitFeedback(onlyEligibleCandidates, feedback, "meal", DATE)

        assertEquals(listOf(Candidate("eligible", 50)), ranked)
        assertFalse(ranked.any { it.key == "hard-filtered" })
    }

    @Test
    fun legacy_display_categories_are_mapped_to_stable_category_keys() {
        val candidates = listOf(Candidate("a", 60), Candidate("b", 50))
        val feedback = listOf(actionFeedback("legacy", "吃什么", "b", ActionFeedbackKind.Accepted))

        val ranked = applyExplicitFeedback(candidates, feedback, "meal", DATE)

        assertEquals(listOf("a", "b"), ranked.map { it.key })
        assertEquals(51, ranked.last().score)
    }

    private fun actionFeedback(
        id: String,
        category: String,
        candidateKey: String,
        kind: ActionFeedbackKind
    ) = ActionFeedback(id, DATE, category, candidateKey, kind, createdAtEpochMs = id.hashCode().toLong())

    companion object {
        private const val DATE = "2026-10-02"
    }
}
