package com.xuanji.app.domain

import com.xuanji.app.data.local.MysticCharacterSessionStore
import com.xuanji.app.data.local.PreferenceBridge
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MysticInteractionStateTest {
    @Test fun cross_day_keeps_the_current_game_but_discards_yesterday_food_conditions_and_reference() {
        val before = MysticInteractionState("2026-10-13", TemporaryActionConditions(maxMinutes = 15), "meal", "old-dish", "game", true, "silkroad_route")
        val after = before.forDate("2026-10-14")
        assertEquals("silkroad_route", after.gameId)
        assertEquals("game", after.actionKey)
        assertEquals(TemporaryActionConditions(), after.conditions)
        assertNull(after.referenceId)
        assertFalse(after.evidenceOpen)
    }

    @Test fun snapshot_restores_conditions_reference_and_evidence_but_not_pending_work() {
        val interaction = MysticInteractionState("2026-10-13", TemporaryActionConditions(maxMinutes = 15, excludedIngredients = setOf("豆腐")), "meal", "second", "meal", true)
        val state = MysticSessionState(sessionToken = 4, interaction = interaction)
        val updated = reduce(state, MysticEvent.UpdateInteraction(4, interaction))
        val snapshot = MysticCharacterSessionState.initial(MysticCharacterId.ShenYanzhou)
            .copy(sessions = mapOf(MysticCharacterId.ShenYanzhou to updated))
        val decoded = requireNotNull(MysticSessionSnapshotCodec.decode(MysticSessionSnapshotCodec.encode(snapshot)))
        assertEquals(interaction, decoded.session(MysticCharacterId.ShenYanzhou).interaction)
        assertEquals(MysticRequestState.Idle, decoded.session(MysticCharacterId.ShenYanzhou).requestState)
        assertEquals(MysticInteractionState(dateKey = "2026-10-14"), interaction.forDate("2026-10-14"))
    }

    @Test fun obsolete_interaction_updates_are_discarded_and_clear_removes_all_transient_choices() {
        val state = MysticSessionState(sessionToken = 8)
        val interaction = MysticInteractionState("2026-10-13", actionKey = "evidence", evidenceOpen = true)
        assertEquals(state, reduce(state, MysticEvent.UpdateInteraction(7, interaction)))
        val updated = reduce(state, MysticEvent.UpdateInteraction(8, interaction))
        assertEquals(interaction, updated.interaction)
        assertEquals(MysticInteractionState(), reduce(updated, MysticEvent.Clear).interaction)
        assertEquals(updated, reduce(updated, MysticEvent.UpdateInteraction(8, interaction.copy(actionKey = "fake-provider"))))
    }

    @Test fun store_recreation_preserves_independent_character_choices_and_profile_isolation() = runBlocking {
        val values = mutableMapOf<String, String>()
        val bridge = object : PreferenceBridge {
            override suspend fun read(key: String) = values[key]
            override suspend fun write(key: String, value: String) { values[key] = value }
            override suspend fun delete(key: String) { values.remove(key) }
        }
        val first = MysticSessionState(interaction = MysticInteractionState("2026-10-13", referenceCategory = "meal", referenceId = "dish"))
        val second = MysticSessionState(interaction = MysticInteractionState("2026-10-13", actionKey = "profile"))
        MysticCharacterSessionStore(bridge).save("synthetic-a", MysticCharacterSessionState.initial(MysticCharacterId.MoHeng)
            .copy(sessions = mapOf(MysticCharacterId.ShenYanzhou to first, MysticCharacterId.MoHeng to second)))
        val recreated = MysticCharacterSessionStore(bridge)
        val restored = requireNotNull(recreated.load("synthetic-a"))
        assertEquals(first.interaction, restored.session(MysticCharacterId.ShenYanzhou).interaction)
        assertEquals(second.interaction, restored.session(MysticCharacterId.MoHeng).interaction)
        assertNull(recreated.load("synthetic-b"))
        recreated.clearCharacter("synthetic-a", MysticCharacterId.ShenYanzhou)
        assertEquals(MysticInteractionState(), recreated.load("synthetic-a")!!.session(MysticCharacterId.ShenYanzhou).interaction)
        assertEquals(second.interaction, recreated.load("synthetic-a")!!.session(MysticCharacterId.MoHeng).interaction)
    }

    @Test fun legacy_snapshot_remains_readable_without_any_automatic_action() {
        val legacy = """{"version":1,"activeCharacterId":"shen_yanzhou","sessions":[],"sharedMemoryNotes":[]}"""
        val restored = requireNotNull(MysticSessionSnapshotCodec.decode(legacy))
        assertEquals(MysticInteractionState(), restored.session(MysticCharacterId.ShenYanzhou).interaction)
    }
}
