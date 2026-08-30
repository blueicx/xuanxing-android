package com.xuanji.app.domain.divination

/** Canonical 16x16 Ifá Odu product space. Verse text is intentionally provider-backed. */
data class IfaCombination(
    val index: Int,
    val outerIndex: Int,
    val innerIndex: Int,
    val outerName: String,
    val innerName: String,
    val canonicalName: String
)

enum class CorpusAvailability { Available, Unavailable }

data class EseSourceMetadata(
    val sourceId: String,
    val language: String,
    val license: String,
    val availability: CorpusAvailability
)

data class EseEntry(val combination: IfaCombination, val text: String, val source: EseSourceMetadata)

interface EseCorpusProvider {
    val metadata: EseSourceMetadata
    fun find(combination: IfaCombination): EseEntry?
}

/** Safe default: names/indexes are available, sacred verses are not fabricated. */
object UnavailableEseCorpusProvider : EseCorpusProvider {
    override val metadata = EseSourceMetadata("none", "", "not-provided", CorpusAvailability.Unavailable)
    override fun find(combination: IfaCombination): EseEntry? = null
}

object IfaCatalogue {
    val mainNames = listOf(
        "Eji Ogbe", "Oyeku Meji", "Iwori Meji", "Odi Meji", "Irosun Meji", "Owonrin Meji",
        "Obara Meji", "Okanran Meji", "Ogunda Meji", "Osa Meji", "Ika Meji", "Oturupon Meji",
        "Otura Meji", "Irete Meji", "Ose Meji", "Ofun Meji"
    )

    fun combination(outerIndex: Int, innerIndex: Int): IfaCombination {
        require(outerIndex in 0..15 && innerIndex in 0..15)
        val index = outerIndex * 16 + innerIndex
        val outer = mainNames[outerIndex]
        val inner = mainNames[innerIndex]
        return IfaCombination(index, outerIndex, innerIndex, outer, inner, "$outer × $inner")
    }

    fun all(): List<IfaCombination> = (0..15).flatMap { outer -> (0..15).map { inner -> combination(outer, inner) } }
}
