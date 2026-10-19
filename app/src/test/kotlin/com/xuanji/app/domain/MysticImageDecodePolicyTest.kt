package com.xuanji.app.domain

import org.junit.Assert.*
import org.junit.Test

class MysticImageDecodePolicyTest {
    @Test fun large_images_are_power_of_two_sampled_and_small_assets_keep_detail() {
        assertEquals(4, MysticImageDecodePolicy.sampleSize(4096, 6144))
        assertEquals(1, MysticImageDecodePolicy.sampleSize(600, 1024))
        assertEquals(1, MysticImageDecodePolicy.sampleSize(0, 0))
        assertTrue(6144 / MysticImageDecodePolicy.sampleSize(4096, 6144) <= 1600)
    }
}
