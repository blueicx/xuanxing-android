package com.xuanji.app.domain

import org.junit.Assert.assertTrue
import org.junit.Test

class MysticPrivacyExportTest {
    @Test
    fun export_is_local_summary_and_does_not_claim_upload_or_generated_memory() {
        val json = MysticPrivacyExportCodec.encode(
            MysticPrivacyExport(
                profileFingerprint = privacyFingerprint("birth|demo"),
                exportedDateKey = "2026-09-22",
                characterSessionSnapshot = "{\"version\":1}",
                longTermMemory = listOf("2026-09-22|user_input|我主动说的"),
                softMemoryLabels = listOf("topic|事业|Inferred"),
                actionFeedbackCount = 2,
                readingFavoriteCount = 1,
                readingFavorites = listOf(
                    MysticPrivacyFavorite(
                        id = "favorite-1",
                        type = "fortune",
                        title = "今日事业",
                        dateKey = "2026-09-22",
                        period = "今日",
                        mode = "综合",
                        characterId = null,
                        gameId = null,
                        body = "用户主动收藏的结果快照",
                        evidence = "当时展示的依据",
                        algorithmVersion = "x3-test",
                        createdAt = 1L
                    )
                )
            )
        )

        assertTrue(json.contains("profileFingerprint"))
        assertTrue(json.contains("longTermMemory"))
        assertTrue(json.contains("actionFeedbackCount"))
        assertTrue(json.contains("readingFavoriteCount"))
        assertTrue(json.contains("readingFavorites"))
        assertTrue(json.contains("用户主动收藏的结果快照"))
        assertTrue(!json.contains("上传"))
        assertTrue(!json.contains("角色生成内容"))
    }
}
