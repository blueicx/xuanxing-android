package com.xuanji.app.domain

/** Stable cultural voice metadata shared by dialogue and the visual skin layer. */
data class MysticCultureVoiceSpec(
    val skinId: String,
    val gesture: String,
    val lexicon: String
)

object MysticCultureVoice {
    private val voices = listOf(
        MysticCultureVoiceSpec("jiangnan-robe", "拈扇侧身", "慢、清、留白"),
        MysticCultureVoiceSpec("academy-gown", "翻页点记", "据、证、脉络"),
        MysticCultureVoiceSpec("silkroad-robe", "抬铃示路", "远、路、回声"),
        MysticCultureVoiceSpec("northland-mantle", "护符按心", "暖、守、火光"),
        MysticCultureVoiceSpec("cloud-daoist", "拂尘收势", "静、观、顺势"),
        MysticCultureVoiceSpec("street-jacket", "摘耳机倾听", "快、短、在场"),
        MysticCultureVoiceSpec("desert-traveler", "抬腕辨星", "风、沙、方向"),
        MysticCultureVoiceSpec("festival-costume", "击鼓定拍", "喜、拍、相逢")
    )

    fun forSkin(skinId: String): MysticCultureVoiceSpec =
        voices.firstOrNull { it.skinId == skinId }
            ?: MysticCultureVoiceSpec(skinId, "合掌安坐", "稳、听、慢慢来")

    fun all(): List<MysticCultureVoiceSpec> = voices
}
