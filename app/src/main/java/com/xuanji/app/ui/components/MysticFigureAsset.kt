package com.xuanji.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.xuanji.app.R

/** Stable visual families used by the culture switcher. */
internal enum class MysticFigureAssetId {
    Jiangnan,
    Elder,
    Academy,
    Silkroad
}

internal fun figureAssetId(styleId: String): MysticFigureAssetId = when (styleId) {
    "jiangnan_scholar" -> MysticFigureAssetId.Jiangnan
    "elder_ink" -> MysticFigureAssetId.Elder
    "academy_astral" -> MysticFigureAssetId.Academy
    "silkroad_astrologer" -> MysticFigureAssetId.Silkroad
    else -> MysticFigureAssetId.Jiangnan
}

private fun resourceId(assetId: MysticFigureAssetId): Int = when (assetId) {
    MysticFigureAssetId.Jiangnan -> R.drawable.mystic_figure_ink
    MysticFigureAssetId.Elder -> R.drawable.mystic_figure_elder
    MysticFigureAssetId.Academy -> R.drawable.mystic_figure_cel
    MysticFigureAssetId.Silkroad -> R.drawable.mystic_figure_lowpoly
}

/**
 * Renders a local culture figure and leaves the caller a deterministic Canvas fallback.
 * Keeping the fallback matters when a resource is stripped by an OEM build or an old split APK.
 */
@Composable
internal fun MysticFigureAsset(
    styleId: String,
    contentDescription: String,
    modifier: Modifier,
    fallback: @Composable () -> Unit
) {
    val resources = LocalContext.current.resources
    val bitmap = remember(styleId, resources) {
        runCatching { BitmapFactory.decodeResource(resources, resourceId(figureAssetId(styleId))) }.getOrNull()
    }
    if (bitmap == null) {
        fallback()
    } else {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = modifier.semantics { this.contentDescription = contentDescription }
        )
    }
}
