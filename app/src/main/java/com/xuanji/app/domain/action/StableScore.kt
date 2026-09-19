package com.xuanji.app.domain.action

import java.math.BigInteger
import java.security.MessageDigest
import kotlin.math.roundToInt

object StableScore {
    const val FIVE_ELEMENT_WEIGHT = 0.40
    const val ZODIAC_WEIGHT = 0.25
    const val DAILY_FORTUNE_WEIGHT = 0.20
    const val SEASON_OR_CITY_WEIGHT = 0.15

    fun combine(five: Int, zodiac: Int, fortune: Int, seasonOrCity: Int): Int =
        (five * FIVE_ELEMENT_WEIGHT + zodiac * ZODIAC_WEIGHT +
            fortune * DAILY_FORTUNE_WEIGHT + seasonOrCity * SEASON_OR_CITY_WEIGHT)
            .roundToInt().coerceIn(0, 100)

    fun fit(actual: Set<String>, expected: Set<String>): Int {
        if (actual.isEmpty() || expected.isEmpty()) return 0
        return ((actual.intersect(expected).size.toDouble() / expected.size) * 100.0)
            .roundToInt().coerceIn(0, 100)
    }

    fun tieBreak(profileKey: String, dateKey: String, candidateKey: String): Long =
        BigInteger(sha256("$profileKey|$dateKey|$candidateKey").take(16), 16).toLong()

    fun orderTies(items: List<Candidate>, profileKey: String, dateKey: String): List<Candidate> =
        items.sortedWith(compareByDescending<Candidate> { it.score }
            .thenBy { tieBreak(profileKey, dateKey, it.key) })

    fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
