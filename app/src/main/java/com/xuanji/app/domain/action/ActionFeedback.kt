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

/** Keeps feedback explicit; it never turns a choice into an inferred personality trait. */
class ActionFeedbackReducer {
    fun reduce(
        state: List<ActionFeedback>,
        feedback: ActionFeedback
    ): List<ActionFeedback> = (state.filterNot { it.id == feedback.id } + feedback).takeLast(100)

    fun remove(state: List<ActionFeedback>, id: String): List<ActionFeedback> =
        state.filterNot { it.id == id }
}
