package com.xuanji.app.ui.components

import com.xuanji.app.domain.MysticCultureVoice

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
            "jiangnan-robe" to spec(CulturalScene.JIANGNAN_GARDEN, "江南水榭", "折扇与水纹", "扇", "jiangnan-robe"),
            "academy-gown" to spec(CulturalScene.ACADEMY_ARCHIVE, "学院档案室", "书卷与羽笔", "卷", "academy-gown"),
            "silkroad-robe" to spec(CulturalScene.SILKROAD_CARAVANSERAI, "丝路驿站", "香料囊与旅铃", "铃", "silkroad-robe"),
            "northland-mantle" to spec(CulturalScene.DAOIST_CLOUD_TERRACE, "墨色湖山", "折扇与竹杖", "墨", "northland-mantle"),
            "cloud-daoist" to spec(CulturalScene.DAOIST_CLOUD_TERRACE, "云台观", "拂尘与玉符", "尘", "cloud-daoist"),
            "street-jacket" to spec(CulturalScene.CITY_NIGHT, "城市夜行", "耳机与霓虹贴纸", "音", "street-jacket"),
            "desert-traveler" to spec(CulturalScene.DESERT_DUSK, "沙海暮色", "水囊与星盘", "星", "desert-traveler"),
            "festival-costume" to spec(CulturalScene.FESTIVAL_COURTYARD, "节庆院落", "鼓与彩绸", "鼓", "festival-costume")
        )

        private fun spec(
            scene: CulturalScene,
            label: String,
            prop: String,
            glyph: String,
            skinId: String
        ): MysticCultureSpec {
            val voice = MysticCultureVoice.forSkin(skinId)
            return MysticCultureSpec(scene, label, prop, glyph, voice.gesture, voice.lexicon)
        }

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
