package com.xuanji.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptedLocalBackupTest {
    @Test
    fun encrypted_container_round_trips_and_does_not_expose_plaintext_or_password() {
        val plaintext = "{\"format\":\"xuanji-local-backup\",\"private\":\"收藏内容\"}"
        val password = "local-only-passphrase-2026".toCharArray()

        val encrypted = EncryptedLocalBackupCodec.encode(plaintext, password)
        val decoded = EncryptedLocalBackupCodec.decode(encrypted, password)

        assertTrue(EncryptedLocalBackupCodec.isEncrypted(encrypted))
        assertFalse(encrypted.contains("收藏内容"))
        assertFalse(encrypted.contains("local-only-passphrase-2026"))
        assertEquals(plaintext, decoded.plaintext)
        assertNull(decoded.error)
    }

    @Test
    fun wrong_password_and_truncated_container_are_rejected_without_plaintext() {
        val encrypted = EncryptedLocalBackupCodec.encode("private payload", "correct-horse-2026".toCharArray())

        val wrongPassword = EncryptedLocalBackupCodec.decode(encrypted, "different-pass-2026".toCharArray())
        val damaged = EncryptedLocalBackupCodec.decode(encrypted.take(30), "correct-horse-2026".toCharArray())

        assertEquals(EncryptedLocalBackupError.AuthenticationFailed, wrongPassword.error)
        assertNull(wrongPassword.plaintext)
        assertEquals(EncryptedLocalBackupError.Malformed, damaged.error)
        assertNull(damaged.plaintext)
    }

    @Test
    fun short_password_is_rejected_and_plain_legacy_backup_is_not_misclassified() {
        val error = runCatching { EncryptedLocalBackupCodec.encode("payload", "short".toCharArray()) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertFalse(EncryptedLocalBackupCodec.isEncrypted("{\"version\":1}"))
    }
}
