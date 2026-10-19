package com.xuanji.app.domain

import org.junit.Assert.*
import org.junit.Test

class MysticTaskReferenceTest {
    private val detail = MysticDetailContext("综合", "今日", "2026-10-19", "真实饮食卡", "合成菜名", "已有依据", "本地计算", "synthetic-v1")
    @Test fun taskSurvivesThreadCodecAndNextDayWithoutBecomingTodayFacts() {
        val task = MysticTaskReference(detail, MysticCharacterId.ShenYanzhou)
        val interaction = MysticInteractionState(dateKey = "2026-10-19", task = task)
        assertTrue(interaction.valid())
        val initial = MysticCharacterSessionState.initial(MysticCharacterId.ShenYanzhou)
        val state = initial.copy(sessions = initial.sessions + (MysticCharacterId.ShenYanzhou to MysticSessionState(interaction = interaction)))
        val restored = requireNotNull(MysticSessionSnapshotCodec.decode(MysticSessionSnapshotCodec.encode(state)))
        assertEquals(task, restored.session(MysticCharacterId.ShenYanzhou).interaction.task)
        assertEquals("2026-10-19", interaction.forDate("2026-10-20").task?.detail?.dateKey)
        assertNull(restored.session(MysticCharacterId.MoHeng).interaction.task)
    }
    @Test fun invalidTaskIsRejectedByExistingTokenCheckedReducer() {
        val state = MysticSessionState(sessionToken = 7)
        val task = MysticTaskReference(detail, MysticCharacterId.MoHeng)
        val requested = MysticInteractionState(task = task)
        assertEquals(state, reduce(state, MysticEvent.UpdateInteraction(6, requested)))
        assertEquals(task, reduce(state, MysticEvent.UpdateInteraction(7, requested)).interaction.task)
        assertFalse(requested.copy(task = task.copy(detail = detail.copy(result = "x".repeat(20_000)))).valid())
    }
}
