package com.xuanji.app.domain

import java.time.LocalDate

/** Explicitly opened cards only; this is a session checkpoint, not inferred long-term memory. */
data class MysticTaskReference(
    val detail: MysticDetailContext,
    val fromCharacter: MysticCharacterId,
    val completed: Boolean = false
) {
    fun valid(): Boolean = runCatching {
        LocalDate.parse(detail.dateKey)
        detail.title.length in 1..160 && detail.result.length in 1..4000 && detail.evidence.length <= 4000 &&
            detail.mode.length in 1..40 && detail.period.length in 1..40 &&
            detail.sourceStatus.length <= 200 && detail.algorithmVersion.length in 1..100
    }.getOrDefault(false)
}
