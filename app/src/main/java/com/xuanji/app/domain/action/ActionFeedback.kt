package com.xuanji.app.domain.action

import java.time.Instant

enum class ActionFeedbackKind { Accepted, NotSuitable, Replaced }

data class ActionFeedback(
    val id: String,
    val dateKey: String,
    val category: String,
    val candidateKey: String,
    val kind: ActionFeedbackKind,
    val note: String? = null,
    val createdAtEpochMs: Long = Instant.now().toEpochMilli()
)

/**
 * Applies only explicit, local feedback to already-eligible candidates.
 * One acceptance adds 1 point (capped at +5); one rejection subtracts 2 (capped at -8).
 * Replacements are temporary exclusions for the same date and category, not a preference.
 */
fun applyExplicitFeedback(
    candidates: List<Candidate>,
    feedback: List<ActionFeedback>,
    category: String,
    dateKey: String
): List<Candidate> {
    val normalizedCategory = normalizeActionCategory(category)
    val scopedFeedback = feedback.filter { normalizeActionCategory(it.category) == normalizedCategory }
    val replacedKeys = scopedFeedback.asSequence()
        .filter { it.kind == ActionFeedbackKind.Replaced && it.dateKey == dateKey }
        .map { it.candidateKey }
        .toSet()
    val adjustments = scopedFeedback.asSequence()
        .filter { it.kind != ActionFeedbackKind.Replaced }
        .groupBy { it.candidateKey }
        .mapValues { (_, entries) ->
            val accepted = entries.count { it.kind == ActionFeedbackKind.Accepted }.coerceAtMost(5)
            val notSuitable = entries.count { it.kind == ActionFeedbackKind.NotSuitable }.coerceAtMost(4)
            (accepted - notSuitable * 2).coerceIn(-8, 5)
        }

    // Sorting is stable, so equal adjusted scores retain the deterministic base order supplied by the planner.
    return candidates.asSequence()
        .filterNot { it.key in replacedKeys }
        .map { candidate ->
            candidate.copy(score = (candidate.score + adjustments[candidate.key].orZero()).coerceIn(0, 100))
        }
        .sortedByDescending(Candidate::score)
        .toList()
}

fun actionFeedbackId(dateKey: String, category: String, candidateKey: String, kind: ActionFeedbackKind): String =
    "$dateKey|${normalizeActionCategory(category)}|$candidateKey|${kind.name}"

private fun normalizeActionCategory(category: String): String = when (category) {
    "吃什么" -> "meal"
    "做什么" -> "activity"
    "去哪玩" -> "outing"
    else -> category
}

private fun Int?.orZero(): Int = this ?: 0

/** Keeps feedback explicit; it never turns a choice into an inferred personality trait. */
class ActionFeedbackReducer {
    fun reduce(
        state: List<ActionFeedback>,
        feedback: ActionFeedback
    ): List<ActionFeedback> = (state.filterNot { it.id == feedback.id } + feedback).takeLast(100)

    fun remove(state: List<ActionFeedback>, id: String): List<ActionFeedback> =
        state.filterNot { it.id == id }
}
