package com.xuanji.app.domain

import com.xuanji.app.data.model.TestRecord

object TestHistoryComparison {
    fun differences(before: TestRecord, after: TestRecord): Map<String, Int> {
        if (before.testName != after.testName || before.scoringVersion.isNullOrBlank() || before.scoringVersion != after.scoringVersion) return emptyMap()
        val previous = before.rawScores.orEmpty()
        return after.rawScores.orEmpty().filterKeys { it in previous }.mapValues { (key, value) -> value - previous.getValue(key) }
    }
}
