package com.xuanji.app.ui.components

import com.xuanji.app.domain.MysticCharacterId
import com.xuanji.app.domain.MysticCharacterProfile

enum class MysticAssetRenderMode { CompleteScene, ForegroundOnScene, CanvasFallback }

data class MysticVisualAssetSpec(
    val assetId: String,
    val renderMode: MysticAssetRenderMode,
    val focusX: Float = .5f,
    val focusY: Float = .5f,
    val requiresCultureBackdrop: Boolean = renderMode == MysticAssetRenderMode.ForegroundOnScene
)

/** Resource truth is explicit so a complete scene can never receive a duplicate backdrop. */
object MysticVisualAssetCatalog {
    private val specs = mapOf(
        MysticCharacterId.ShenYanzhou to MysticVisualAssetSpec("mystic_figure_ink", MysticAssetRenderMode.CompleteScene, .5f, .52f),
        MysticCharacterId.MoHeng to MysticVisualAssetSpec("mystic_figure_elder", MysticAssetRenderMode.ForegroundOnScene, .52f, .5f),
        MysticCharacterId.EvelynNova to MysticVisualAssetSpec("mystic_figure_cel", MysticAssetRenderMode.CompleteScene, .5f, .5f),
        MysticCharacterId.NadirRashid to MysticVisualAssetSpec("mystic_figure_lowpoly", MysticAssetRenderMode.CompleteScene, .5f, .52f)
    )

    fun forCharacter(profile: MysticCharacterProfile): MysticVisualAssetSpec =
        specs[profile.id] ?: MysticVisualAssetSpec("fallback", MysticAssetRenderMode.CanvasFallback)
}
