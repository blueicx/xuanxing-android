package com.xuanji.app.ui.components

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.xuanji.app.domain.MysticImageDecodePolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Only bundled artwork. No user pictures, network images, or personal data. */
internal object MysticFigureBitmapCache {
    private val cache = object : LruCache<Int, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: Int, value: Bitmap): Int = value.allocationByteCount
    }
    suspend fun load(resources: Resources, id: Int): Bitmap? = withContext(Dispatchers.IO) {
        synchronized(cache) {
            cache.get(id) ?: runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true; inScaled = false }
                BitmapFactory.decodeResource(resources, id, bounds)
                val options = BitmapFactory.Options().apply {
                    inScaled = false
                    inSampleSize = MysticImageDecodePolicy.sampleSize(bounds.outWidth, bounds.outHeight)
                }
                BitmapFactory.decodeResource(resources, id, options)?.also { cache.put(id, it) }
            }.getOrNull()
        }
    }
}
