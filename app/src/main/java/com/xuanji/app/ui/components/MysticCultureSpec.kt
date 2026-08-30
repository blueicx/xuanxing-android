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
    val propGlyph: String
) {
    companion object {
        private val SPECS = mapOf(
            "jiangnan-robe" to MysticCultureSpec(CulturalScene.JIANGNAN_GARDEN, "江南水榭", "折扇与水纹", "扇"),
            "academy-gown" to MysticCultureSpec(CulturalScene.ACADEMY_ARCHIVE, "学院档案室", "书卷与羽笔", "卷"),
            "silkroad-robe" to MysticCultureSpec(CulturalScene.SILKROAD_CARAVANSERAI, "丝路驿站", "香料囊与旅铃", "铃"),
            "northland-mantle" to MysticCultureSpec(CulturalScene.NORTHLAND_FIRE, "北境火塘", "鹿角护符", "角"),
            "cloud-daoist" to MysticCultureSpec(CulturalScene.DAOIST_CLOUD_TERRACE, "云台观", "拂尘与玉符", "尘"),
            "street-jacket" to MysticCultureSpec(CulturalScene.CITY_NIGHT, "城市夜行", "耳机与霓虹贴纸", "音"),
            "desert-traveler" to MysticCultureSpec(CulturalScene.DESERT_DUSK, "沙海暮色", "水囊与星盘", "星"),
            "festival-costume" to MysticCultureSpec(CulturalScene.FESTIVAL_COURTYARD, "节庆院落", "鼓与彩绸", "鼓")
        )

        private val NEUTRAL = MysticCultureSpec(
            CulturalScene.NEUTRAL_STAGE,
            "静默舞台",
            "随身手记",
            "记"
        )

        fun forSkin(skinId: String): MysticCultureSpec = SPECS[skinId] ?: NEUTRAL
    }
}
