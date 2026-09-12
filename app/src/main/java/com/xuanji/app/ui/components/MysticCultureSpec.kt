package com.xuanji.app.ui.components

/** 文化皮肤的结构化视觉契约：道具与场景必须随文化变化，而非只换配色。 */
enum class CulturalScene {
    JIANGNAN_GARDEN,
    ACADEMY_ARCHIVE,
    SILKROAD_CARAVANSERAI,
    NORTHLAND_FIRE,
    DAOIST_CLOUD_TERRACE,
    CITY_NIGHT,
    DESERT_DUSK,
    FESTIVAL_COURTYARD,
    NEUTRAL_STAGE
}

data class MysticCultureSpec(
    val scene: CulturalScene,
    val sceneLabel: String,
    val prop: String,
    val propGlyph: String,
    val gesture: String = "静坐",
    val voiceLexicon: String = "平和"
) {
    companion object {
        private val SPECS = mapOf(
            "jiangnan-robe" to MysticCultureSpec(CulturalScene.JIANGNAN_GARDEN, "江南水榭", "折扇与水纹", "扇", "拈扇侧身", "慢、清、留白"),
            "academy-gown" to MysticCultureSpec(CulturalScene.ACADEMY_ARCHIVE, "学院档案室", "书卷与羽笔", "卷", "翻页点记", "据、证、脉络"),
            "silkroad-robe" to MysticCultureSpec(CulturalScene.SILKROAD_CARAVANSERAI, "丝路驿站", "香料囊与旅铃", "铃", "抬铃示路", "远、路、回声"),
            "northland-mantle" to MysticCultureSpec(CulturalScene.NORTHLAND_FIRE, "北境火塘", "鹿角护符", "角", "护符按心", "暖、守、火光"),
            "cloud-daoist" to MysticCultureSpec(CulturalScene.DAOIST_CLOUD_TERRACE, "云台观", "拂尘与玉符", "尘", "拂尘收势", "静、观、顺势"),
            "street-jacket" to MysticCultureSpec(CulturalScene.CITY_NIGHT, "城市夜行", "耳机与霓虹贴纸", "音", "摘耳机倾听", "快、短、在场"),
            "desert-traveler" to MysticCultureSpec(CulturalScene.DESERT_DUSK, "沙海暮色", "水囊与星盘", "星", "抬腕辨星", "风、沙、方向"),
            "festival-costume" to MysticCultureSpec(CulturalScene.FESTIVAL_COURTYARD, "节庆院落", "鼓与彩绸", "鼓", "击鼓定拍", "喜、拍、相逢")
        )

        private val NEUTRAL = MysticCultureSpec(
            CulturalScene.NEUTRAL_STAGE,
            "静默舞台",
            "随身手记",
            "记",
            "合掌安坐",
            "稳、听、慢慢来"
        )

        fun forSkin(skinId: String): MysticCultureSpec = SPECS[skinId] ?: NEUTRAL

        fun all(): List<MysticCultureSpec> = SPECS.values.toList()
    }
}
