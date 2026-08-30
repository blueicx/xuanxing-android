package com.xuanji.app.domain

import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.model.EasternDailyFortune
import com.xuanji.app.data.model.WesternDailyFortune
import com.xuanji.app.data.model.Element
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking

class MysticDialogueCoordinatorTest {
    private val fortune = CompositeDailyFortune(
        dateKey = "2026-08-31", overallScore = 80, dimensions = emptyList(), luckyNumber = 7,
        luckyColor = "青", luckyDirection = "东", cautions = "慢一点",
        eastern = EasternDailyFortune("2026-08-31", 80, 80, 80, 80, 80, "稳定", "慢一点", "甲子", listOf(Element.WOOD), "青", "东"),
        western = WesternDailyFortune("2026-08-31", "白羊座", 80, 80, 80, 80, 80, "稳定", 7, "青", "东")
    )

    @Test fun success_returns_send_then_reply_events() = runBlocking {
        val provider = object : DialogueProvider {
            override suspend fun complete(request: DialogueRequest) = ProviderResult.Success("收到：${request.input}")
        }
        val events = MysticDialogueCoordinator(provider).complete(
            MysticSessionState(), DialogueContext(mode = "scholar", styleKey = "", topicKey = "composite", fortune = fortune), "你好"
        )
        assertEquals(2, events.size)
        assertTrue(events[0] is MysticEvent.SendInput)
        assertTrue(events[1] is MysticEvent.ReplySucceeded)
    }

    @Test fun failure_is_retryable_at_state_layer() = runBlocking {
        val provider = object : DialogueProvider {
            override suspend fun complete(request: DialogueRequest) = ProviderResult.Failure("busy", retryable = true)
        }
        val events = MysticDialogueCoordinator(provider).complete(
            MysticSessionState(), DialogueContext(mode = "scholar", styleKey = "", topicKey = "composite", fortune = fortune), "继续"
        )
        var state = MysticSessionState()
        events.forEach { state = reduce(state, it) }
        assertTrue(state.requestState is MysticRequestState.Failed)
    }

    @Test fun a_context_change_drops_the_old_reply() = runBlocking {
        val provider = object : DialogueProvider {
            override suspend fun complete(request: DialogueRequest) = ProviderResult.Success("旧回复")
        }
        val events = MysticDialogueCoordinator(provider).complete(
            MysticSessionState(), DialogueContext(mode = "scholar", styleKey = "", topicKey = "composite", fortune = fortune), "旧问题"
        )
        var state = MysticSessionState()
        state = reduce(state, events.first())
        state = reduce(state, MysticEvent.ChangeContext(mode = "half"))
        state = reduce(state, events.last())
        assertTrue(state.messages.none { it.text == "旧回复" })
    }
}
