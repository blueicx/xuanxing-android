package com.xuanji.app.domain.divination

data class NadiQuery(
    val fingerprint: String,
    val language: String = "en",
    val kandam: Int? = null
)

enum class NadiReadingKind { LicensedLeafText, OfflineSimulation, Unavailable }

data class NadiReading(
    val text: String?,
    val kind: NadiReadingKind,
    val sourceId: String,
    val license: String,
    val confidence: Double? = null
)

interface NadiCorpusProvider {
    fun lookup(query: NadiQuery): NadiReading
}

object UnavailableNadiCorpusProvider : NadiCorpusProvider {
    override fun lookup(query: NadiQuery): NadiReading =
        NadiReading(null, NadiReadingKind.Unavailable, "none", "not-provided")
}

/** Deterministic fallback used by the app when no licensed leaf corpus is configured. */
object OfflineNadiSimulationProvider : NadiCorpusProvider {
    override fun lookup(query: NadiQuery): NadiReading {
        val cls = NadiAstrology.classifyFingerprint(query.fingerprint)
        val kandam = query.kandam?.coerceIn(1, 16)?.let { NADI_KANDAMS[it - 1] }
        val text = buildString {
            append("离线纳迪模拟（指纹类别 $cls）")
            if (kandam != null) append("；章节：$kandam")
            append("。这不是可追溯的真实叶脉原文，也不代表叶库命中。")
        }
        return NadiReading(text, NadiReadingKind.OfflineSimulation, "xuanji-offline-simulation", "original")
    }
}
