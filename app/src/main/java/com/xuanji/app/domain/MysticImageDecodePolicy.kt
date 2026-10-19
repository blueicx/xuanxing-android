package com.xuanji.app.domain

object MysticImageDecodePolicy {
    const val MAX_EDGE = 1600
    fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        val edge = maxOf(width, height)
        while (edge > MAX_EDGE.toLong() * sample) sample *= 2
        return sample
    }
}
