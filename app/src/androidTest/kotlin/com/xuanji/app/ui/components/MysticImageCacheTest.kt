package com.xuanji.app.ui.components

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MysticImageCacheTest {
    @Test fun four_real_assets_are_bounded_and_reused_without_upscaling() = runBlocking {
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        for (id in MysticFigureAssetId.entries) {
            val first = MysticFigureBitmapCache.load(resources, resourceId(id))
            assertNotNull(first)
            assertTrue(maxOf(first!!.width, first.height) <= 1600)
            assertSame(first, MysticFigureBitmapCache.load(resources, resourceId(id)))
        }
    }
}
