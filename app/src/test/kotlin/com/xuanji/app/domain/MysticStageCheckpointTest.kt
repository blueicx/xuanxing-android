package com.xuanji.app.domain

import com.xuanji.app.data.local.MysticStageCheckpointStore
import com.xuanji.app.data.local.PreferenceBridge
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MysticStageCheckpointTest {
    @Test fun stage_reopens_with_same_role_and_independent_drawers_but_not_on_another_date_or_profile() = runBlocking {
        val values = mutableMapOf<String, String>()
        val bridge = object : PreferenceBridge {
            override suspend fun read(key: String) = values[key]
            override suspend fun write(key: String, value: String) { values[key] = value }
            override suspend fun delete(key: String) { values.remove(key) }
        }
        val checkpoint = MysticStageCheckpoint("2026-10-13", MysticCharacterId.NadirRashid, true,
            mapOf(MysticCharacterId.NadirRashid to MysticDrawerState.Expanded, MysticCharacterId.MoHeng to MysticDrawerState.Peek))
        MysticStageCheckpointStore(bridge).save("synthetic-one", checkpoint)
        val recreated = MysticStageCheckpointStore(bridge)
        assertEquals(checkpoint, recreated.load("synthetic-one", "2026-10-13"))
        assertNull(recreated.load("synthetic-two", "2026-10-13"))
        assertNull(recreated.load("synthetic-one", "2026-10-14"))
        recreated.clear("synthetic-one")
        assertNull(recreated.load("synthetic-one", "2026-10-13"))
    }

    @Test fun corrupt_unknown_role_and_future_version_are_not_restored() {
        assertNull(MysticStageCheckpointCodec.decode("garbage"))
        assertNull(MysticStageCheckpointCodec.decode("""{"version":1,"dateKey":"2026-10-13","characterId":"not-a-role","stageOpen":true,"drawers":{}}"""))
        assertNull(MysticStageCheckpointCodec.decode("""{"version":99}"""))
    }
}
