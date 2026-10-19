package com.xuanji.app.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertTextEquals
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xuanji.app.data.local.ActionFeedbackDataStore
import com.xuanji.app.data.local.CompanionGameProgressStore
import com.xuanji.app.data.local.LocalBackupProfileKeys
import com.xuanji.app.data.local.LocalDataBackupRepository
import com.xuanji.app.data.local.PreferenceBridge
import com.xuanji.app.data.local.ReadingFavoriteStore
import com.xuanji.app.data.local.XiangqiSaveBackupStore
import com.xuanji.app.domain.LocalBackupSelection
import com.xuanji.app.domain.LocalDataBackupCodec
import com.xuanji.app.domain.EncryptedLocalBackupCodec
import com.xuanji.app.domain.ReadingFavorite
import com.xuanji.app.domain.action.ActionFeedback
import com.xuanji.app.domain.action.ActionFeedbackKind
import com.xuanji.app.domain.game.GameSave
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalBackupInteractionTest {
    @get:Rule val compose = createComposeRule()

    private class MemoryBridge : PreferenceBridge {
        private val values = mutableMapOf<String, String>()
        override suspend fun read(key: String): String? = values[key]
        override suspend fun write(key: String, value: String) { values[key] = value }
        override suspend fun delete(key: String) { values.remove(key) }
    }

    private class MemoryXiangqiStore : XiangqiSaveBackupStore {
        override suspend fun load(profileKey: String): GameSave? = null
        override suspend fun save(profileKey: String, save: GameSave) = Unit
    }

    @Test
    fun export_selection_and_import_preview_cancel_and_confirm_use_only_memory_stores() {
        val keys = LocalBackupProfileKeys("synthetic-favorites", "synthetic-local")
        val source = repository()
        runBlocking {
            source.favorites.upsert(keys.favoriteProfileKey, ReadingFavorite(
                id = "synthetic-favorite", type = "fortune", title = "本地快照", body = "合成结果", createdAt = 10L
            ))
            source.feedback.append(keys.localProfileKey, ActionFeedback(
                id = "synthetic-feedback", dateKey = "2026-10-03", category = "meal",
                candidateKey = "synthetic-meal", kind = ActionFeedbackKind.Accepted, createdAtEpochMs = 11L
            ))
        }
        val backup = requireNotNull(runBlocking {
            source.repository.export(
                keys, LocalBackupSelection(favorites = true, actionFeedback = true), exportedAtEpochMs = 12L
            ).json
        })
        val target = repository()
        var exportedJson: String? = null
        val importDocument = mutableStateOf<String?>(backup)

        compose.setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    LocalBackupPanelContent(
                        repository = target.repository,
                        profileKeys = keys,
                        onCreateDocument = { _, json, onFinished ->
                            exportedJson = json
                            onFinished(true)
                        },
                        onOpenDocument = { onSelected -> onSelected(importDocument.value) }
                    )
                }
            }
        }

        compose.onNodeWithTag("local-backup-export-password").performTextInput("xuanji-backup-pass-2026")
        compose.onNodeWithTag("local-backup-export-password-confirm").performTextInput("xuanji-backup-pass-2026")
        compose.onNodeWithTag("local-backup-export-feedback-checkbox").performClick()
        compose.onNodeWithTag("local-backup-export").performScrollTo().performClick()
        compose.waitUntil(5_000) { exportedJson != null }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("已保存选择的本地数据备份。").fetchSemanticsNodes().isNotEmpty()
        }
        val decryptedExport = EncryptedLocalBackupCodec.decode(
            requireNotNull(exportedJson), "xuanji-backup-pass-2026".toCharArray()
        ).plaintext
        val selectedExport = LocalDataBackupCodec.decode(decryptedExport, keys.fingerprintKey).backup
        assertNotNull(selectedExport)
        assertEquals(LocalBackupSelection(favorites = true), selectedExport?.selection)
        assertEquals(emptyList<ActionFeedback>(), selectedExport?.payload?.actionFeedback)

        compose.onNodeWithTag("local-backup-import").performScrollTo().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("local-backup-preview").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("收藏 1 条 · 行动反馈 1 条 · 游戏进度 0 份 · 象棋棋谱 0 份").assertExists()
        compose.onNodeWithTag("local-backup-cancel-import").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(0, runBlocking { target.favorites.list(keys.favoriteProfileKey).entries.size })
        assertEquals(0, runBlocking { target.feedback.list(keys.localProfileKey).size })

        compose.onNodeWithTag("local-backup-import").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("local-backup-import-feedback-checkbox").performScrollTo().performClick()
        compose.onNodeWithTag("local-backup-preview-statistics").assertTextEquals("新增 1 · 合并保留 0 · 覆盖 0 · 本次遗漏 1")
        compose.onNodeWithTag("local-backup-restore").performScrollTo().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("恢复完成；收藏和行动反馈已按 ID 合并。").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(listOf("synthetic-favorite"), runBlocking {
            target.favorites.list(keys.favoriteProfileKey).entries.map { it.id }
        })
        assertEquals(0, runBlocking { target.feedback.list(keys.localProfileKey).size })
        compose.onNodeWithText("恢复完成；收藏和行动反馈已按 ID 合并。").assertExists()
    }

    @Test
    fun encrypted_import_requires_password_and_restores_only_after_authenticated_preview() {
        val keys = LocalBackupProfileKeys("synthetic-encrypted-favorites", "synthetic-encrypted-local")
        val source = repository()
        runBlocking {
            source.favorites.upsert(keys.favoriteProfileKey, ReadingFavorite(
                id = "encrypted-favorite", type = "fortune", title = "加密快照", body = "合成内容", createdAt = 20L
            ))
        }
        val encrypted = requireNotNull(runBlocking {
            source.repository.export(
                keys, LocalBackupSelection(favorites = true), exportedAtEpochMs = 30L,
                passphrase = "test-backup-password".toCharArray()
            ).json
        })
        val target = repository()
        val importDocument = mutableStateOf<String?>(encrypted)

        compose.setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    LocalBackupPanelContent(
                        repository = target.repository,
                        profileKeys = keys,
                        onCreateDocument = { _, _, onFinished -> onFinished(true) },
                        onOpenDocument = { onSelected -> onSelected(importDocument.value) }
                    )
                }
            }
        }

        compose.onNodeWithTag("local-backup-import").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("local-backup-import-password").assertExists()
        compose.onNodeWithTag("local-backup-preview").assertDoesNotExist()
        assertEquals(0, runBlocking { target.favorites.list(keys.favoriteProfileKey).entries.size })

        compose.onNodeWithTag("local-backup-import-password").performTextInput("test-backup-password")
        compose.onNodeWithTag("local-backup-unlock").performScrollTo().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("local-backup-preview").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("密码认证、档案指纹与完整性校验均通过。").assertExists()
        compose.onNodeWithTag("local-backup-restore").performScrollTo().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("恢复完成；收藏和行动反馈已按 ID 合并。").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(listOf("encrypted-favorite"), runBlocking {
            target.favorites.list(keys.favoriteProfileKey).entries.map { it.id }
        })
    }

    @Test
    fun profile_mismatch_is_shown_in_preview_and_never_offers_restore() {
        val foreignKeys = LocalBackupProfileKeys("foreign-favorites", "foreign-local")
        val currentKeys = LocalBackupProfileKeys("current-favorites", "current-local")
        val source = repository()
        runBlocking {
            source.favorites.upsert(foreignKeys.favoriteProfileKey, ReadingFavorite(
                id = "foreign-favorite", type = "fortune", title = "其他档案", body = "不应导入", createdAt = 1L
            ))
        }
        val foreignBackup = requireNotNull(runBlocking {
            source.repository.export(foreignKeys, LocalBackupSelection(favorites = true), 2L).json
        })
        val target = repository()

        compose.setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    LocalBackupPanelContent(
                        repository = target.repository,
                        profileKeys = currentKeys,
                        onCreateDocument = { _, _, onFinished -> onFinished(true) },
                        onOpenDocument = { onSelected -> onSelected(foreignBackup) }
                    )
                }
            }
        }

        compose.onNodeWithTag("local-backup-import").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("该备份属于其他档案，不能恢复到当前档案。").assertExists()
        compose.onNodeWithTag("local-backup-restore").assertDoesNotExist()
        assertEquals(0, runBlocking { target.favorites.list(currentKeys.favoriteProfileKey).entries.size })
    }

    private data class RepositoryFixture(
        val repository: LocalDataBackupRepository,
        val favorites: ReadingFavoriteStore,
        val feedback: ActionFeedbackDataStore
    )

    private fun repository(): RepositoryFixture {
        val bridge = MemoryBridge()
        val favorites = ReadingFavoriteStore(bridge) { 1L }
        val feedback = ActionFeedbackDataStore(bridge)
        val games = CompanionGameProgressStore(bridge) { 1L }
        return RepositoryFixture(
            LocalDataBackupRepository(favorites, feedback, games, MemoryXiangqiStore()),
            favorites,
            feedback
        )
    }
}
