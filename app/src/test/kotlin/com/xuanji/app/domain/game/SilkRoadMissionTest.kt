package com.xuanji.app.domain.game

import org.junit.Assert.*
import org.junit.Test

class SilkRoadMissionTest {
    @Test fun old_route_save_migrates_to_default_mission_and_new_mission_roundtrips() {
        val original = SilkRoadRouteEngine.reduce(SilkRoadState(), SilkRoadEvent.Travel("敦煌"))
        val envelope = CompanionGameProgressEnvelope(2, "profile", "silkroad", "silkroad_route",
            CompanionGameProgressCodec.encodeState(CompanionGameProgressState.SilkRoad(original)).replace(",\"missionId\":\"silk-01\"", ""), 1)
        val old = CompanionGameProgressCodec.decode(CompanionGameProgressCodec.encode(envelope))!!
        assertEquals(original, (CompanionGameProgressCodec.decodeState(old) as CompanionGameProgressState.SilkRoad).value)
        val next = SilkRoadRouteEngine.initial("silk-12")
        val newEnvelope = old.copy(stateJson = CompanionGameProgressCodec.encodeState(CompanionGameProgressState.SilkRoad(next)))
        assertEquals(next, (CompanionGameProgressCodec.decodeState(newEnvelope) as CompanionGameProgressState.SilkRoad).value)
    }
    @Test fun twelve_fixed_missions_are_unique_solvable_and_resource_consistent() {
        assertEquals(12, SilkRoadMissionCatalog.all.size)
        assertEquals(12, SilkRoadMissionCatalog.all.map { it.id }.toSet().size)
        SilkRoadMissionCatalog.all.forEach { mission ->
            var state = SilkRoadRouteEngine.initial(mission.id)
            assertTrue(SilkRoadRouteEngine.isValidState(state))
            mission.solution.drop(1).forEach { city ->
                state = SilkRoadRouteEngine.reduce(state, SilkRoadEvent.Travel(city))
                assertTrue("${mission.id}: $city", SilkRoadRouteEngine.isValidState(state))
            }
            assertTrue(state.completed)
            assertTrue(state.daysLeft >= 0 && state.budgetLeft >= 0)
            assertTrue(SilkRoadRouteEngine.result(state).summary.contains(mission.title))
            val undone = SilkRoadRouteEngine.reduce(state, SilkRoadEvent.Undo)
            assertFalse(undone.completed)
            assertTrue(SilkRoadRouteEngine.isValidState(undone))
            assertEquals(SilkRoadRouteEngine.initial(mission.id), SilkRoadRouteEngine.reduce(state, SilkRoadEvent.Restart))
        }
    }
    @Test fun legacy_route_keeps_its_original_budget_and_mission_switch_is_explicit() {
        assertEquals(SilkRoadState(), SilkRoadRouteEngine.initial("silk-01"))
        val old = SilkRoadRouteEngine.reduce(SilkRoadState(), SilkRoadEvent.Travel("敦煌"))
        assertEquals(6, old.daysLeft)
        assertEquals(9, old.budgetLeft)
        val switched = SilkRoadRouteEngine.reduce(old, SilkRoadEvent.SelectMission("silk-02"))
        assertEquals(SilkRoadRouteEngine.initial("silk-02"), switched)
        assertEquals(switched, SilkRoadRouteEngine.reduce(switched, SilkRoadEvent.SelectMission("unknown")))
    }
}
