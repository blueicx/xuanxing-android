package com.xuanji.app.ui.components

import com.xuanji.app.domain.MysticCharacterProfile

/** Stable scene identifiers used by the immersive character stage. */
enum class MysticSceneId(val key: String) {
    Jiangnan("jiangnan_triptych"),
    InkElder("ink_elder"),
    Academy("academy_triptych"),
    Silkroad("silkroad_triptych"),
    InkFallback("ink_fallback")
}

/**
 * Pure scene metadata. The palette is expressed as ARGB longs so scene lookup
 * remains JVM-testable without importing Compose or Android resources.
 */
data class MysticSceneSpec(
    val id: MysticSceneId,
    val key: String,
    val title: String,
    val culturalMotifs: List<String>,
    val accessibilityDescription: String,
    val backgroundColorArgb: Long,
    val accentColorArgb: Long,
    val hasCompleteBackdrop: Boolean = true
)

object MysticSceneCatalog {
    val fallback: MysticSceneSpec = MysticSceneSpec(
        id = MysticSceneId.InkFallback,
        key = MysticSceneId.InkFallback.key,
        title = "水墨静室",
        culturalMotifs = listOf("宣纸", "墨线", "远山屏风"),
        accessibilityDescription = "低对比度水墨静室背景，含宣纸、墨线与远山屏风",
        backgroundColorArgb = 0xFF17151A,
        accentColorArgb = 0xFFC9C0B5
    )

    private val scenes = listOf(
        MysticSceneSpec(
            id = MysticSceneId.Jiangnan,
            key = MysticSceneId.Jiangnan.key,
            title = "江南水榭",
            culturalMotifs = listOf("月桥", "水榭灯笼", "远山与折扇"),
            accessibilityDescription = "江南水榭场景，含月桥、灯笼、远山与折扇意象",
            backgroundColorArgb = 0xFF23302E,
            accentColorArgb = 0xFFD9C58B
        ),
        MysticSceneSpec(
            id = MysticSceneId.InkElder,
            key = MysticSceneId.InkElder.key,
            title = "水墨案头",
            culturalMotifs = listOf("宣纸案", "墨砚", "山水屏风"),
            accessibilityDescription = "完整水墨案头场景，含宣纸、墨砚、笔架与山水屏风",
            backgroundColorArgb = 0xFF282827,
            accentColorArgb = 0xFFC4B9A5
        ),
        MysticSceneSpec(
            id = MysticSceneId.Academy,
            key = MysticSceneId.Academy.key,
            title = "学院观测台",
            culturalMotifs = listOf("星图刻度", "观测桌", "档案书架"),
            accessibilityDescription = "学院观测台场景，含星图刻度、档案桌与书架",
            backgroundColorArgb = 0xFF191B35,
            accentColorArgb = 0xFFBBA7E8
        ),
        MysticSceneSpec(
            id = MysticSceneId.Silkroad,
            key = MysticSceneId.Silkroad.key,
            title = "丝路驿站",
            culturalMotifs = listOf("驿站拱门", "沙丘", "铜盘与旅灯"),
            accessibilityDescription = "丝路驿站场景，含拱门、沙丘、铜盘与暖色旅灯",
            backgroundColorArgb = 0xFF352318,
            accentColorArgb = 0xFFE2B56E
        )
    )

    fun forScene(sceneId: String): MysticSceneSpec =
        scenes.firstOrNull { it.key == sceneId } ?: fallback

    fun forCharacter(profile: MysticCharacterProfile): MysticSceneSpec =
        forScene(profile.sceneId)
}
