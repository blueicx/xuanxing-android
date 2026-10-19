package com.xuanji.app.data.local

import com.xuanji.app.domain.MysticStageCheckpoint
import com.xuanji.app.domain.MysticStageCheckpointCodec

class MysticStageCheckpointStore(private val bridge: PreferenceBridge) {
    suspend fun load(profileKey: String, dateKey: String): MysticStageCheckpoint? =
        if (profileKey.isBlank()) null else MysticStageCheckpointCodec.decode(bridge.read(key(profileKey)))?.takeIf { it.dateKey == dateKey }
    suspend fun save(profileKey: String, checkpoint: MysticStageCheckpoint) {
        require(profileKey.isNotBlank())
        bridge.write(key(profileKey), MysticStageCheckpointCodec.encode(checkpoint))
    }
    suspend fun clear(profileKey: String) { if (profileKey.isNotBlank()) bridge.delete(key(profileKey)) }
    fun key(profileKey: String) = "mystic_stage_checkpoint_" + MysticCharacterSessionStore.fingerprint(profileKey)
}
