package com.xuanji.app.domain

import com.xuanji.app.ui.components.*
import org.junit.Assert.*
import org.junit.Test

class SelectiveShareTest {
    private val card = ShareCard("玄星", "合成结果", badge = "80分", headline = "80", headlineUnit = "分",
        summary = "合成摘要", rows = listOf("色彩" to "青", "依据" to "自编规则"))
    @Test fun defaultSelectionExcludesScoreAndRowsAndCannotAddPrivateData() {
        val selected = card.selectFields(ShareFieldSelection())
        assertEquals("", selected.badge)
        assertEquals("", selected.headline)
        assertEquals(emptyList<Pair<String, String>>(), selected.rows)
        assertEquals("合成摘要", selected.summary)
        assertEquals(card.title, selected.title)
    }
    @Test fun explicitSelectionsUseOnlyExistingFields() {
        val selected = card.selectFields(ShareFieldSelection(includeHeadline = true, includeSummary = false, rowIndices = setOf(1, 999)))
        assertEquals("80", selected.headline)
        assertEquals("", selected.summary)
        assertEquals(listOf("依据" to "自编规则"), selected.rows)
        assertEquals(card, card.selectFields(ShareFieldSelection(true, true, setOf(0, 1))))
    }
}
