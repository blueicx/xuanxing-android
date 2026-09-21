package com.xuanji.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MysticCharacterSessionStateTest {
    @Test
    fun switching_character_preserves_threads_and_invalidates_old_pending_reply() {
        var state = MysticCharacterSessionState.initial(MysticCharacterId.ShenYanzhou)
        state = reduceCharacterSession(
            state,
            MysticCharacterEvent.Session(MysticCharacterId.ShenYanzhou, MysticEvent.SendInput("今天工作怎么办"))
        )
        val oldToken = state.session(MysticCharacterId.ShenYanzhou).sessionToken

        state = reduceCharacterSession(
            state,
            MysticCharacterEvent.Switch(MysticCharacterId.MoHeng)
        )

        assertEquals(MysticCharacterId.MoHeng, state.activeCharacterId)
        assertEquals(MysticRequestState.Idle, state.session(MysticCharacterId.ShenYanzhou).requestState)
        assertTrue(state.session(MysticCharacterId.MoHeng).messages.last().text.contains("墨衡"))

        val stale = reduceCharacterSession(
            state,
            MysticCharacterEvent.Session(
                MysticCharacterId.ShenYanzhou,
                MysticEvent.ReplySucceeded(
                    oldToken,
                    1,
                    DialogueReply(MysticIntent.Career, "", "旧角色回复")
                )
            )
        )
        assertFalse(stale.session(MysticCharacterId.ShenYanzhou).messages.any { it.text == "旧角色回复" })
    }

    @Test
    fun shared_memory_is_available_after_switch_without_merging_dialogue_threads() {
        var state = MysticCharacterSessionState.initial(MysticCharacterId.ShenYanzhou)
        state = reduceCharacterSession(
            state,
            MysticCharacterEvent.RememberShared(MysticMemoryNote("m1", "用户主动说想换工作"))
        )
        state = reduceCharacterSession(state, MysticCharacterEvent.Switch(MysticCharacterId.EvelynNova))
        state = reduceCharacterSession(
            state,
            MysticCharacterEvent.Session(MysticCharacterId.EvelynNova, MysticEvent.SendInput("记得我刚才说的吗"))
        )

        assertEquals(listOf("m1"), state.sharedMemoryNotes.map { it.id })
        assertEquals(0, state.session(MysticCharacterId.ShenYanzhou).messages.count { it.role == MysticMessageRole.User })
        assertEquals(1, state.session(MysticCharacterId.EvelynNova).messages.count { it.role == MysticMessageRole.User })
    }

    @Test
    fun switching_to_the_same_character_does_not_add_a_duplicate_handoff() {
        val initial = MysticCharacterSessionState.initial(MysticCharacterId.MoHeng)
        val unchanged = reduceCharacterSession(initial, MysticCharacterEvent.Switch(MysticCharacterId.MoHeng))

        assertEquals(initial, unchanged)
    }
}
