package com.xuanji.app.domain.game

import org.junit.Assert.*
import org.junit.Test

class SilkRoadMapTest {
    @Test fun available_routes_are_compared_within_current_resources_before_infeasible_fallbacks() {
        val constrained = SilkRoadRouteEngine.initial("silk-06").copy(budgetLeft = 8)
        val plans = SilkRoadGeography.compareRoutes(constrained)
        assertEquals(3, plans.size)
        assertTrue("There is a 12-day / 8-supply branch; don't hide it behind unaffordable winners", plans.all { it.feasible })
        assertTrue(plans.all { it.days <= 12 && it.supplies <= 8 })
    }
    @Test fun city_projection_and_camera_hit_test_share_the_same_transform() {
        assertEquals(9, SilkRoadGeography.cities.size)
        val east = SilkRoadGeography.project("长安")
        val west = SilkRoadGeography.project("撒马尔罕")
        assertTrue(east.x > west.x)
        val camera = SilkRoadCamera(zoom = 2.0, panX = -0.1, panY = 0.05)
        val screen = camera.toScreen(east, 600.0, 400.0)
        assertEquals("长安", SilkRoadGeography.hit(screen, camera, 600.0, 400.0, 20.0))
        assertEquals(east.x, camera.fromScreen(screen, 600.0, 400.0).x, 0.0001)
    }

    @Test fun route_comparison_is_deterministic_and_costs_are_calculated_from_real_legs() {
        val ample = SilkRoadRouteEngine.initial("silk-06")
        val plans = SilkRoadGeography.compareRoutes(ample)
        assertEquals(plans, SilkRoadGeography.compareRoutes(ample))
        assertEquals(3, plans.size)
        assertTrue(plans.map { it.path }.distinct().size >= 2)
        plans.forEach { plan ->
            val legs = plan.path.zipWithNext().map { (from, to) -> SilkRoadGeography.leg(from, to)!! }
            assertEquals(legs.sumOf { it.days }, plan.days)
            assertEquals(legs.sumOf { it.supplies }, plan.supplies)
            assertEquals(plan.days <= ample.daysLeft && plan.supplies <= ample.budgetLeft, plan.feasible)
            assertTrue(plan.distanceKm > 0)
        }
    }

    @Test fun new_branches_keep_old_settlement_and_undo_and_planned_path_survive_codec() {
        val old = SilkRoadRouteEngine.reduce(SilkRoadState(), SilkRoadEvent.Travel("敦煌"))
        assertEquals(6, old.daysLeft)
        assertEquals(9, old.budgetLeft)
        assertTrue(SilkRoadRouteEngine.destinations(old).any { it.id == "吐鲁番" })
        val state = SilkRoadRouteEngine.reduce(SilkRoadRouteEngine.initial("silk-06"),
            SilkRoadEvent.PlanRoute(listOf("长安", "敦煌", "喀什", "塔什干", "撒马尔罕")))
        val envelope = CompanionGameProgressCodec.envelope("synthetic", com.xuanji.app.domain.MysticCharacterId.NadirRashid, CompanionGameProgressState.SilkRoad(state), 1)
        val restored = CompanionGameProgressCodec.decodeState(CompanionGameProgressCodec.decode(CompanionGameProgressCodec.encode(envelope))!!) as CompanionGameProgressState.SilkRoad
        assertEquals(state, restored.value)
        val travelled = SilkRoadRouteEngine.reduce(state, SilkRoadEvent.Travel("兰州"))
        assertTrue(travelled.plannedPath.isEmpty()) // Deliberate deviation cancels the old plan.
        assertTrue(SilkRoadRouteEngine.isValidState(travelled))
        assertEquals(state.daysLeft, SilkRoadRouteEngine.reduce(travelled, SilkRoadEvent.Undo).daysLeft)
    }
}
