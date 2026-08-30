package com.xuanji.app.domain

import com.xuanji.app.domain.test.IpipBigFive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IpipBigFiveTest {
    @Test fun ipip_has_50_items_and_reverse_scoring() {
        assertEquals(50, IpipBigFive.items.size)
        val low = IpipBigFive.score(List(50) { 1 }).scores
        assertTrue(low.all { it.raw in 10..50 })
        val high = IpipBigFive.score(List(50) { 5 }).scores
        assertEquals(30, high.first { it.scale == "外向性" }.raw)
    }
    @Test(expected = IllegalArgumentException::class)
    fun invalid_answer_count_is_rejected() { IpipBigFive.score(listOf(1, 2)) }
}
