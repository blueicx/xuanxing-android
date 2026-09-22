package com.xuanji.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MysticStageDrawerReducerTest {
    @Test
    fun drawer_starts_peek_and_each_character_keeps_its_state() {
        var state = MysticStageDrawerSession.initial(MysticCharacterId.ShenYanzhou)
        assertEquals(MysticDrawerState.Peek, state.currentDrawerState)

        state = reduceMysticStageDrawer(state, MysticStageDrawerEvent.Expand)
        state = reduceMysticStageDrawer(state, MysticStageDrawerEvent.SelectCharacter(MysticCharacterId.MoHeng))

        assertEquals(MysticDrawerState.Peek, state.currentDrawerState)
        state = reduceMysticStageDrawer(state, MysticStageDrawerEvent.SelectCharacter(MysticCharacterId.ShenYanzhou))
        assertEquals(MysticDrawerState.Expanded, state.currentDrawerState)
    }

    @Test
    fun stale_action_token_cannot_reopen_or_replace_current_drawer() {
        var state = MysticStageDrawerSession.initial(MysticCharacterId.EvelynNova)
        state = reduceMysticStageDrawer(state, MysticStageDrawerEvent.NewRequest(8L))
        state = reduceMysticStageDrawer(state, MysticStageDrawerEvent.ApplyAction(7L, MysticDrawerState.Expanded))
        assertEquals(MysticDrawerState.Peek, state.currentDrawerState)

        state = reduceMysticStageDrawer(state, MysticStageDrawerEvent.ApplyAction(8L, MysticDrawerState.Expanded))
        assertEquals(MysticDrawerState.Expanded, state.currentDrawerState)
        state = reduceMysticStageDrawer(state, MysticStageDrawerEvent.Collapse)
        assertEquals(MysticDrawerState.Peek, state.currentDrawerState)
    }
}
