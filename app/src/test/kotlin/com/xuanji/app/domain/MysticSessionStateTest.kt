package com.xuanji.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MysticSessionStateTest {
    @Test
    fun context_change_invalidates_stale_reply() {
        val initial = reduce(
            reduce(MysticSessionState(sessionToken = 7), MysticEvent.Input("你好")),
            MysticEvent.Reply(7, MysticTurn("你好", "在"))
        )
        val changed = reduce(initial, MysticEvent.ChangeContext(skinId = "alley"))
        val stale = reduce(changed, MysticEvent.Reply(7, MysticTurn("你好", "在")))
        assertEquals(8, changed.sessionToken)
        assertEquals(emptyList<MysticTurn>(), changed.recentTurns)
        assertEquals(changed, stale)
        assertNull(stale.pendingInput)
    }

    @Test
    fun input_is_trimmed_and_bounded() {
        val state = reduce(MysticSessionState(), MysticEvent.Input("  你好  "))
        assertEquals("你好", state.pendingInput)

        val bounded = reduce(MysticSessionState(), MysticEvent.Input("x".repeat(260)))
        assertEquals(200, bounded.pendingInput?.length)
        assertEquals(null, reduce(MysticSessionState(), MysticEvent.Input("  ")).pendingInput)
    }

    @Test
    fun recent_turns_and_memory_notes_are_bounded() {
        var state = MysticSessionState()
        repeat(15) { index ->
            state = reduce(state, MysticEvent.Reply(0, MysticTurn("q$index", "a$index")))
            state = reduce(state, MysticEvent.Remember(MysticMemoryNote("m$index", "note$index")))
        }
        assertEquals(12, state.recentTurns.size)
        assertEquals(12, state.memoryNotes.size)
        assertEquals("q3", state.recentTurns.first().question)
        assertEquals("m3", state.memoryNotes.first().id)
    }

    @Test
    fun clear_invalidates_pending_reply_and_forgets_session_notes() {
        var state = MysticSessionState(sessionToken = 3, pendingInput = "问题")
        state = reduce(state, MysticEvent.Remember(MysticMemoryNote("m", "用户主动记录")))
        state = reduce(state, MysticEvent.Clear)

        assertEquals(4, state.sessionToken)
        assertEquals(emptyList<MysticMemoryNote>(), state.memoryNotes)
        assertEquals(state, reduce(state, MysticEvent.Reply(3, MysticTurn("问题", "旧回复"))))
    }

    @Test
    fun send_input_creates_user_message_and_pending_request() {
        val state = reduce(MysticSessionState(), MysticEvent.SendInput("  你好  "))
        assertEquals(MysticRequestState.Pending(0, 1, "你好"), state.requestState)
        assertEquals(MysticMessageRole.User, state.messages.single().role)
    }

    @Test
    fun success_and_failure_only_apply_to_matching_token_and_turn() {
        var state = reduce(MysticSessionState(), MysticEvent.SendInput("今天如何"))
        state = reduce(state, MysticEvent.ReplyFailed(0, 1, "网络不可用"))
        assertEquals(MysticRequestState.Failed(0, 1, "今天如何", "网络不可用"), state.requestState)
        val stale = reduce(state, MysticEvent.ReplySucceeded(9, 1, DialogueReply(MysticIntent.Daily, "", "旧")))
        assertEquals(state, stale)
    }

    @Test
    fun retry_uses_new_turn_id_and_cancel_keeps_user_message() {
        var state = reduce(MysticSessionState(), MysticEvent.SendInput("继续说"))
        state = reduce(state, MysticEvent.ReplyFailed(0, 1, "失败"))
        state = reduce(state, MysticEvent.RetryTurn)
        assertEquals(MysticRequestState.Pending(0, 2, "继续说"), state.requestState)
        state = reduce(state, MysticEvent.CancelReply(0, 2))
        assertEquals(MysticRequestState.Idle, state.requestState)
        assertEquals(3, state.messages.size)
        assertEquals(MysticMessageRole.System, state.messages.last().role)
    }

    @Test
    fun context_change_invalidates_pending_request_and_adds_system_message() {
        var state = reduce(MysticSessionState(), MysticEvent.SendInput("问题"))
        state = reduce(state, MysticEvent.ChangeContext(mode = "half"))
        assertEquals(MysticRequestState.Idle, state.requestState)
        assertEquals(MysticMessageRole.System, state.messages.last().role)
        assertEquals(state, reduce(state, MysticEvent.ReplySucceeded(0, 1, DialogueReply(MysticIntent.Chat, "", "旧"))))
    }
}
