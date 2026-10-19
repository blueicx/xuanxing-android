package com.xuanji.app.data.local

import com.xuanji.app.domain.action.StableScore

class ReadingPositionStore(private val bridge: PreferenceBridge) {
    private fun key(owner: String, reading: String): String {
        require(owner.isNotBlank() && reading.isNotBlank())
        return "reading_position_" + StableScore.sha256(owner + "|" + reading)
    }
    suspend fun load(owner: String, reading: String): Int = bridge.read(key(owner, reading))?.toIntOrNull()?.takeIf { it in 0..2_000_000 } ?: 0
    suspend fun save(owner: String, reading: String, position: Int) {
        require(position in 0..2_000_000)
        bridge.write(key(owner, reading), position.toString())
    }
    suspend fun clear(owner: String, reading: String) = bridge.delete(key(owner, reading))
}
