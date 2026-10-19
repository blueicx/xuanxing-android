package com.xuanji.app.domain

import org.junit.Assert.*
import org.junit.Test

class MysticScoreVoiceTest {
    @Test fun extractedScoreVoicePreservesExistingBandBoundariesAndInternalModes() {
        assertEquals("事业有势能，你可以安心接住", MysticScoreVoice.scholarHeadline(80, "事业"))
        assertEquals("事业方向清楚，节奏可以温柔些", MysticScoreVoice.scholarHeadline(79, "事业"))
        assertEquals("事业正在蓄力，不必逼它开花", MysticScoreVoice.scholarHeadline(64, "事业"))
        assertEquals("事业需要小步确认，而不是大步证明", MysticScoreVoice.scholarHeadline(49, "事业"))
        assertEquals("先把事业安顿好，再安排世界", MysticScoreVoice.scholarHeadline(34, "事业"))
        assertTrue(MysticScoreVoice.halfHeadline(80, "事业").startsWith("不得了"))
        for (score in 0..100) {
            assertTrue(MysticScoreVoice.bandSentence(score).isNotBlank())
            assertTrue(MysticScoreVoice.halfBandSentence(score).isNotBlank())
        }
    }
}
