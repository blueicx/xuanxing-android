package com.xuanji.app.data.local

import com.xuanji.app.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MysticThreadClearTest {
    private class Memory : PreferenceBridge {
        val values = mutableMapOf<String, String>()
        override suspend fun read(key: String) = values[key]
        override suspend fun write(key: String, value: String) { values[key] = value }
        override suspend fun delete(key: String) { values.remove(key) }
    }
    @Test fun persisted_clear_advances_tokens_and_keeps_other_threads_and_shared_user_notes() = runBlocking {
        val store = MysticCharacterSessionStore(Memory())
        val role = MysticCharacterId.ShenYanzhou
        val old = reduce(MysticSessionState(sessionToken = 7), MysticEvent.SendInput("旧问题"))
        val other = reduce(MysticSessionState(), MysticEvent.SendInput("另一位的问题"))
        val notes = listOf(MysticMemoryNote("user-note", "用户主动保留"))
        store.save("synthetic", MysticCharacterSessionState.initial(role).copy(
            sessions = mapOf(role to old, MysticCharacterId.MoHeng to other), sharedMemoryNotes = notes))
        store.clearCharacter("synthetic", role)
        val saved = requireNotNull(store.load("synthetic"))
        val cleared = saved.session(role)
        assertEquals(8L, cleared.sessionToken)
        assertEquals(old.nextTurnId, cleared.nextTurnId)
        assertTrue(cleared.messages.isEmpty())
        assertEquals(notes, saved.sharedMemoryNotes)
        assertEquals(other.messages, saved.session(MysticCharacterId.MoHeng).messages)
        val newRequest = reduce(cleared, MysticEvent.SendInput("新问题"))
        assertEquals(newRequest, reduce(newRequest, MysticEvent.ReplySucceeded(7, 1, DialogueReply(MysticIntent.Chat, "", "旧结果"))))
        assertNull(store.load("other-profile"))
    }
}
