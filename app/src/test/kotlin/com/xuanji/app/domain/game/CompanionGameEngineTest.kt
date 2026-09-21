package com.xuanji.app.domain.game

import com.xuanji.app.domain.MysticCharacterId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionGameEngineTest {
    @Test
    fun catalog_maps_one_game_to_each_character_and_keeps_unenabled_games_explicit() {
        assertEquals(4, CompanionGameCatalog.all.size)
        assertEquals(4, CompanionGameCatalog.all.map { it.characterId }.toSet().size)
        assertEquals(GameAvailability.Enabled, CompanionGameCatalog.forCharacter(MysticCharacterId.MoHeng).availability)
        assertTrue(CompanionGameCatalog.all.filter { it.characterId != MysticCharacterId.MoHeng }
            .all { it.availability == GameAvailability.CulturalReference })
    }

    @Test
    fun poetry_chain_is_deterministic_restartable_and_requires_the_local_rule() {
        var state = PoetryChainState()
        val prompt = PoetryChainEngine.prompt(state)
        state = PoetryChainEngine.reduce(state, PoetryChainEvent.Choose(prompt.options.first()))
        assertEquals(1, state.score)
        assertFalse(state.completed)
        state = PoetryChainEngine.reduce(state, PoetryChainEvent.Choose("完全不押韵"))
        assertEquals(1, state.score)
        assertTrue(state.feedback.contains("没有接上"))
        assertEquals(PoetryChainState(), PoetryChainEngine.reduce(state, PoetryChainEvent.Restart))
    }

    @Test
    fun star_map_rejects_non_adjacent_move_and_completes_valid_path() {
        var state = StarMapPuzzleEngine.initial()
        state = StarMapPuzzleEngine.reduce(state, StarMapEvent.Move("天狼"))
        assertFalse(state.completed)
        assertTrue(state.feedback.contains("没有直接连线"))
        state = StarMapPuzzleEngine.reduce(state, StarMapEvent.Move("天津"))
        state = StarMapPuzzleEngine.reduce(state, StarMapEvent.Move("天狼"))
        assertTrue(state.completed)
        assertEquals(listOf("织女", "天津", "天狼"), state.path)
    }

    @Test
    fun silk_road_requires_adjacent_city_and_finishes_within_fixed_resources() {
        var state = SilkRoadState()
        state = SilkRoadRouteEngine.reduce(state, SilkRoadEvent.Travel("撒马尔罕"))
        assertFalse(state.completed)
        assertTrue(state.feedback.contains("没有相邻"))
        state = SilkRoadRouteEngine.reduce(state, SilkRoadEvent.Travel("敦煌"))
        state = SilkRoadRouteEngine.reduce(state, SilkRoadEvent.Travel("喀什"))
        state = SilkRoadRouteEngine.reduce(state, SilkRoadEvent.Travel("撒马尔罕"))
        val result = SilkRoadRouteEngine.result(state)
        assertTrue(result.completed)
        assertFalse(result.failed)
        assertEquals(4, result.path.size)
    }
}
