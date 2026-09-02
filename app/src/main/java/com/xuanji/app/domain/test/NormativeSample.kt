package com.xuanji.app.domain.test

data class NormativeSample(
    val id: String,
    val version: String,
    val population: String,
    val size: Int,
    val meanByScale: Map<String, Double>,
    val sdByScale: Map<String, Double>
) {
    fun percentile(scale: String, raw: Double): Int? {
        val mean = meanByScale[scale] ?: return null
        val sd = sdByScale[scale] ?: return null
        if (sd <= 0.0) return null
        val z = (raw - mean) / sd
        // Normal CDF approximation (Abramowitz-Stegun), rounded to 0..100.
        val sign = if (z < 0) -1 else 1
        val x = kotlin.math.abs(z) / kotlin.math.sqrt(2.0)
        val t = 1.0 / (1.0 + 0.3275911 * x)
        val erf = 1 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t + 0.254829592) * t * kotlin.math.exp(-x * x)
        val cdf = 0.5 * (1 + sign * erf)
        return (cdf * 100).toInt().coerceIn(0, 100)
    }
}
