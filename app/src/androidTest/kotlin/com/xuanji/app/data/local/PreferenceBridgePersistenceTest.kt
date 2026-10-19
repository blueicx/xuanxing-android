package com.xuanji.app.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID
import com.xuanji.app.domain.MysticStageCheckpoint
import com.xuanji.app.domain.MysticCharacterId
import com.xuanji.app.domain.MysticDrawerState

class PreferenceBridgePersistenceTest {
    @Test fun production_bridge_commits_atomically_and_reopens_only_isolated_synthetic_data() = runBlocking {
        // Instrumentation executes with the target UID. Only a fresh test-named cache
        // file is used: never the production preferences file or any existing profile.
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "isolated-${UUID.randomUUID()}.preferences_pb")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val first = DataStorePreferenceBridge(PreferenceDataStoreFactory.create(scope = firstScope, produceFile = { file }))
        val checkpoint = MysticStageCheckpoint("2026-10-13", MysticCharacterId.NadirRashid, true,
            mapOf(MysticCharacterId.NadirRashid to MysticDrawerState.Expanded))
        try {
            first.write("synthetic-a", "old")
            assertFalse(first.compareAndWriteBatch(mapOf("synthetic-a" to "wrong"), mapOf("synthetic-a" to "bad", "synthetic-b" to "bad")))
            assertEquals("old", first.read("synthetic-a"))
            assertNull(first.read("synthetic-b"))
            assertTrue(first.compareAndWriteBatch(mapOf("synthetic-a" to "old", "synthetic-b" to null), mapOf("synthetic-a" to "new", "synthetic-b" to "saved")))
            MysticStageCheckpointStore(first).save("isolated-stage", checkpoint)
        } finally { firstScope.coroutineContext[Job]!!.cancelAndJoin() }
        val restoredScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val restored = DataStorePreferenceBridge(PreferenceDataStoreFactory.create(scope = restoredScope, produceFile = { file }))
        try {
            assertEquals("new", restored.read("synthetic-a"))
            assertEquals("saved", restored.read("synthetic-b"))
            assertEquals(checkpoint, MysticStageCheckpointStore(restored).load("isolated-stage", "2026-10-13"))
            assertNull(MysticStageCheckpointStore(restored).load("different-stage", "2026-10-13"))
            assertNull(MysticStageCheckpointStore(restored).load("isolated-stage", "2026-10-14"))
            assertTrue(restored.compareAndWriteBatch(mapOf("synthetic-b" to "saved"), mapOf("synthetic-b" to null)))
            assertNull(restored.read("synthetic-b"))
        } finally { restoredScope.coroutineContext[Job]!!.cancelAndJoin() }
    }
}
