package com.xuanji.app.domain.divination

/** Project's documented convention, not a scientific measure or a claim of one universal tradition. */
object NumerologyReduction {
    const val VERSION = "master-preserving-reduction-v2"
    fun reduce(value: Int): Int = steps(value).last()
    fun steps(value: Int): List<Int> {
        require(value >= 0)
        val result = mutableListOf(value)
        while (result.last() > 9 && result.last() !in setOf(11, 22, 33)) {
            result += result.last().toString().sumOf { it.digitToInt() }
        }
        return result
    }
    fun explain(value: Int): String {
        val steps = steps(value)
        return buildString {
            append(value)
            steps.zipWithNext().forEach { (before, after) ->
                append(" → ${before.toString().toList().joinToString("+")}=$after")
            }
            if (steps.last() in setOf(11, 22, 33)) append("（保留大师数）")
        }
    }
}
