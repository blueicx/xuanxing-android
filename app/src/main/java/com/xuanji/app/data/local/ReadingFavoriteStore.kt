package com.xuanji.app.data.local

import com.xuanji.app.domain.ReadingFavorite
import com.xuanji.app.domain.ReadingFavoriteCodec
import com.xuanji.app.domain.ReadingFavoriteDataState
import com.xuanji.app.domain.ReadingFavoriteSnapshot
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest

/** Per-profile local snapshots. Corrupt payloads are never overwritten by an ordinary save. */
class ReadingFavoriteStore(
    private val bridge: PreferenceBridge,
    private val now: () -> Long = { System.currentTimeMillis() }
) {
    private val mutex = Mutex()

    suspend fun list(profileKey: String): ReadingFavoriteSnapshot = mutex.withLock {
        loadUnlocked(profileKey)
    }

    suspend fun upsert(profileKey: String, favorite: ReadingFavorite): ReadingFavoriteSnapshot = mutex.withLock {
        if (profileKey.isBlank()) return@withLock ReadingFavoriteSnapshot(ReadingFavoriteDataState.InvalidProfile)
        val current = loadUnlocked(profileKey)
        if (!current.state.canMutate()) return@withLock current
        val normalizedFavorite = favorite.bounded().let {
            if (it.createdAt > 0L) it else it.copy(createdAt = now())
        }
        val updated = (current.entries.filterNot { it.id == normalizedFavorite.id } + normalizedFavorite)
            .distinctBy { it.id }
            .sortedByDescending { it.createdAt }
            .take(ReadingFavoriteCodec.MAX_ENTRIES)
        bridge.write(key(profileKey), ReadingFavoriteCodec.encode(fingerprint(profileKey), updated))
        ReadingFavoriteSnapshot(ReadingFavoriteDataState.Ready, updated)
    }

    suspend fun remove(profileKey: String, id: String): ReadingFavoriteSnapshot = mutex.withLock {
        if (profileKey.isBlank()) return@withLock ReadingFavoriteSnapshot(ReadingFavoriteDataState.InvalidProfile)
        val current = loadUnlocked(profileKey)
        if (!current.state.canMutate()) return@withLock current
        val updated = current.entries.filterNot { it.id == id }
        if (updated.isEmpty()) {
            bridge.delete(key(profileKey))
            ReadingFavoriteSnapshot(ReadingFavoriteDataState.Missing)
        } else {
            bridge.write(key(profileKey), ReadingFavoriteCodec.encode(fingerprint(profileKey), updated))
            ReadingFavoriteSnapshot(ReadingFavoriteDataState.Ready, updated)
        }
    }

    /** Explicit privacy action; also removes unreadable data after the user has seen its error state. */
    suspend fun clear(profileKey: String): ReadingFavoriteSnapshot = mutex.withLock {
        if (profileKey.isBlank()) return@withLock ReadingFavoriteSnapshot(ReadingFavoriteDataState.InvalidProfile)
        bridge.delete(key(profileKey))
        ReadingFavoriteSnapshot(ReadingFavoriteDataState.Missing)
    }

    fun key(profileKey: String): String = KEY_PREFIX + fingerprint(profileKey)

    private suspend fun loadUnlocked(profileKey: String): ReadingFavoriteSnapshot {
        if (profileKey.isBlank()) return ReadingFavoriteSnapshot(ReadingFavoriteDataState.InvalidProfile)
        val decoded = ReadingFavoriteCodec.decode(bridge.read(key(profileKey)))
        if (decoded.state !in setOf(ReadingFavoriteDataState.Ready, ReadingFavoriteDataState.Migrated)) {
            return ReadingFavoriteSnapshot(decoded.state, decoded.entries)
        }
        if (decoded.profileFingerprint != fingerprint(profileKey)) {
            return ReadingFavoriteSnapshot(ReadingFavoriteDataState.ProfileMismatch)
        }
        if (decoded.state == ReadingFavoriteDataState.Migrated) {
            bridge.write(key(profileKey), ReadingFavoriteCodec.encode(fingerprint(profileKey), decoded.entries))
        }
        return ReadingFavoriteSnapshot(decoded.state, decoded.entries)
    }

    private fun ReadingFavoriteDataState.canMutate(): Boolean =
        this == ReadingFavoriteDataState.Missing || this == ReadingFavoriteDataState.Ready || this == ReadingFavoriteDataState.Migrated

    private fun ReadingFavorite.bounded(): ReadingFavorite = copy(
        id = id.trim().take(120),
        type = type.trim().take(40),
        title = title.trim().take(180),
        dateKey = dateKey.take(100),
        period = period.take(80),
        mode = mode.take(100),
        characterId = characterId?.take(80),
        gameId = gameId?.take(80),
        body = body.take(6_000),
        evidence = evidence.take(3_000),
        algorithmVersion = algorithmVersion.take(100)
    )

    private fun fingerprint(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    companion object {
        const val KEY_PREFIX = "reading_favorites_"
    }
}
