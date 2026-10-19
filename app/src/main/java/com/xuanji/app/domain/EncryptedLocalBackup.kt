package com.xuanji.app.domain

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

enum class EncryptedLocalBackupError { PasswordTooShort, Malformed, UnsupportedVersion, AuthenticationFailed, Oversized }

data class EncryptedLocalBackupDecodeResult(
    val plaintext: String? = null,
    val error: EncryptedLocalBackupError? = null
)

/** Password-protected container around the existing checksummed backup JSON. */
object EncryptedLocalBackupCodec {
    const val MIN_PASSWORD_LENGTH = 12
    const val MAX_CONTAINER_BYTES = LocalDataBackupCodec.MAX_BYTES * 2
    private const val FORMAT = "xuanji-encrypted-local-backup"
    private const val VERSION = 1
    private const val ITERATIONS = 210_000
    private const val SALT_BYTES = 16
    private const val NONCE_BYTES = 12
    private const val TAG_BITS = 128
    private val random = SecureRandom()
    private val aad = "xuanji-local-backup|v$VERSION".toByteArray(StandardCharsets.UTF_8)

    fun isEncrypted(container: String): Boolean = runCatching {
        JsonParser.parseString(container).asJsonObject.stringValue("format") == FORMAT
    }.getOrDefault(false)

    fun encode(plaintext: String, password: CharArray): String {
        require(password.size >= MIN_PASSWORD_LENGTH) { "password_too_short" }
        require(plaintext.toByteArray(StandardCharsets.UTF_8).size <= LocalDataBackupCodec.MAX_BYTES) { "backup_oversized" }
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val key = deriveKey(password, salt)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
            cipher.updateAAD(aad)
            val ciphertext = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
            JsonObject().apply {
                addProperty("format", FORMAT)
                addProperty("version", VERSION)
                addProperty("kdf", "PBKDF2WithHmacSHA256")
                addProperty("iterations", ITERATIONS)
                addProperty("cipher", "AES/GCM/NoPadding")
                addProperty("salt", Base64.getEncoder().encodeToString(salt))
                addProperty("nonce", Base64.getEncoder().encodeToString(nonce))
                addProperty("ciphertext", Base64.getEncoder().encodeToString(ciphertext))
            }.toString()
        } finally {
            key.fill(0)
        }
    }

    fun decode(container: String, password: CharArray): EncryptedLocalBackupDecodeResult {
        if (container.toByteArray(StandardCharsets.UTF_8).size > MAX_CONTAINER_BYTES) {
            return EncryptedLocalBackupDecodeResult(error = EncryptedLocalBackupError.Oversized)
        }
        if (password.size < MIN_PASSWORD_LENGTH) {
            return EncryptedLocalBackupDecodeResult(error = EncryptedLocalBackupError.PasswordTooShort)
        }
        val root = runCatching { JsonParser.parseString(container).asJsonObject }
            .getOrElse { return EncryptedLocalBackupDecodeResult(error = EncryptedLocalBackupError.Malformed) }
        if (root.stringValue("format") != FORMAT) return EncryptedLocalBackupDecodeResult(error = EncryptedLocalBackupError.Malformed)
        if (root.intValue("version") != VERSION || root.stringValue("kdf") != "PBKDF2WithHmacSHA256" ||
            root.intValue("iterations") != ITERATIONS || root.stringValue("cipher") != "AES/GCM/NoPadding") {
            return EncryptedLocalBackupDecodeResult(error = EncryptedLocalBackupError.UnsupportedVersion)
        }
        val salt = root.bytesValue("salt") ?: return EncryptedLocalBackupDecodeResult(error = EncryptedLocalBackupError.Malformed)
        val nonce = root.bytesValue("nonce") ?: return EncryptedLocalBackupDecodeResult(error = EncryptedLocalBackupError.Malformed)
        val ciphertext = root.bytesValue("ciphertext") ?: return EncryptedLocalBackupDecodeResult(error = EncryptedLocalBackupError.Malformed)
        if (salt.size != SALT_BYTES || nonce.size != NONCE_BYTES || ciphertext.size > LocalDataBackupCodec.MAX_BYTES + TAG_BITS / 8) {
            return EncryptedLocalBackupDecodeResult(error = EncryptedLocalBackupError.Malformed)
        }
        val key = runCatching { deriveKey(password, salt) }
            .getOrElse { return EncryptedLocalBackupDecodeResult(error = EncryptedLocalBackupError.Malformed) }
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
            cipher.updateAAD(aad)
            val plaintext = cipher.doFinal(ciphertext)
            if (plaintext.size > LocalDataBackupCodec.MAX_BYTES) {
                EncryptedLocalBackupDecodeResult(error = EncryptedLocalBackupError.Oversized)
            } else {
                EncryptedLocalBackupDecodeResult(plaintext = String(plaintext, StandardCharsets.UTF_8))
            }
        } catch (_: AEADBadTagException) {
            EncryptedLocalBackupDecodeResult(error = EncryptedLocalBackupError.AuthenticationFailed)
        } catch (_: Exception) {
            EncryptedLocalBackupDecodeResult(error = EncryptedLocalBackupError.Malformed)
        } finally {
            key.fill(0)
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): ByteArray {
        val protectedPassword = password.copyOf()
        val spec = PBEKeySpec(protectedPassword, salt, ITERATIONS, 256)
        protectedPassword.fill('\u0000')
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun JsonObject.stringValue(name: String): String? = runCatching { get(name)?.asString }.getOrNull()
    private fun JsonObject.intValue(name: String): Int? = runCatching { get(name)?.asInt }.getOrNull()
    private fun JsonObject.bytesValue(name: String): ByteArray? {
        val encoded = stringValue(name) ?: return null
        return runCatching { Base64.getDecoder().decode(encoded) }.getOrNull()
    }
}
