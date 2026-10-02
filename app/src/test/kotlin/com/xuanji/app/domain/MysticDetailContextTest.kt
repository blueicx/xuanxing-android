package com.xuanji.app.domain

import org.junit.Assert.assertTrue
import org.junit.Test

class MysticDetailContextTest {
    @Test
    fun question_carries_displayed_result_and_evidence_without_birth_data() {
        val question = MysticDetailContext(
            mode = "东方",
            period = "本周",
            dateKey = "农历丙午年八月初二",
            title = "事业",
            result = "本周适合稳步推进；当前评分 76。",
            evidence = "日主偏木；本周事业维度 76 分。",
            sourceStatus = "本地计算",
            algorithmVersion = "x3-test"
        ).toQuestion()

        assertTrue(question.contains("东方"))
        assertTrue(question.contains("本周"))
        assertTrue(question.contains("农历丙午年八月初二"))
        assertTrue(question.contains("事业"))
        assertTrue(question.contains("评分 76"))
        assertTrue(question.contains("日主偏木"))
        assertTrue(question.contains("本地计算"))
        assertTrue(question.length <= MysticDetailContext.MAX_QUESTION_LENGTH)
        assertTrue(!question.contains("出生资料全文"))
    }

    @Test
    fun empty_evidence_is_not_filled_with_invented_basis() {
        val question = MysticDetailContext(
            mode = "综合",
            period = "今日",
            dateKey = "2026-10-02",
            title = "综合结论",
            result = "当前没有可用结果",
            evidence = "   ",
            sourceStatus = "资料不足",
            algorithmVersion = ""
        ).toQuestion()

        assertTrue(question.contains("当前没有可用结果"))
        assertTrue(question.contains("依据：暂无"))
        assertTrue(question.contains("资料不足"))
        assertTrue(!question.contains("依据：暂无（根据"))
    }

    @Test
    fun oversized_fields_are_bounded_in_combined_question() {
        val context = MysticDetailContext(
            mode = "m".repeat(2000),
            period = "p".repeat(2000),
            dateKey = "d".repeat(2000),
            title = "t".repeat(2000),
            result = "r".repeat(8000),
            evidence = "e".repeat(8000),
            sourceStatus = "s".repeat(2000),
            algorithmVersion = "v".repeat(1000)
        )

        assertTrue(context.toQuestion().length <= MysticDetailContext.MAX_QUESTION_LENGTH)
    }
}
