package com.xuanji.app.domain.test

import com.xuanji.app.domain.action.StableScore

/** Project questionnaires, not licensed instruments or population norms. */
data class QuestionnaireSpec(val id: String, val options: List<Set<String>>) {
    init {
        require(id.matches(Regex("[a-z0-9_-]{1,80}")))
        require(options.size in 1..200 && options.all { it.isNotEmpty() && it.size <= 10 && it.all { value -> value.length in 1..32 } })
    }
    val fingerprint: String get() = StableScore.sha256(id + options.joinToString(";") { it.sorted().joinToString(",") })
    fun valid(draft: QuestionnaireDraft): Boolean = draft.readable &&
        draft.index in 0..options.size + 1 && draft.answers.size <= options.size &&
        draft.answers.withIndex().all { (index, answer) -> answer in options[index] } &&
        (draft.pick == null || (draft.index in 1..options.size && draft.pick in options[draft.index - 1])) &&
        (draft.index == 0 || draft.answers.size >= draft.index - 1)
    fun complete(draft: QuestionnaireDraft): Boolean = valid(draft) && draft.answers.size == options.size
}

data class QuestionnaireDraft(
    val index: Int = 0,
    val answers: List<String> = emptyList(),
    val pick: String? = null,
    val readable: Boolean = true
)
