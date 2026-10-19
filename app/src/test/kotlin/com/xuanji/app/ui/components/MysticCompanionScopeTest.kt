package com.xuanji.app.ui.components

import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.model.EasternDailyFortune
import com.xuanji.app.data.model.WesternDailyFortune
import org.junit.Assert.assertNotSame
import org.junit.Test

class MysticCompanionScopeTest {
    @Test fun reopening_a_composition_never_reuses_an_unrevokable_process_thread_cache() {
        val fortune = CompositeDailyFortune("2026-10-15", 72, emptyList(), 6, "青", "东", "",
            EasternDailyFortune("2026-10-15", 68, 70, 65, 66, 69, "", "", "甲子", emptyList(), "青", "东"),
            WesternDailyFortune("2026-10-15", "处女座", 74, 73, 72, 71, 75, "", 6, "青", "东"))
        // Keep the UI implementation private; test the factory used by remember(companionKey).
        val factory = Class.forName("com.xuanji.app.ui.components.MysticGuideCardKt")
            .getDeclaredMethod("mysticCompanionState", String::class.java, CompositeDailyFortune::class.java)
            .apply { isAccessible = true }
        val firstScope = factory.invoke(null, "synthetic-deletion-scope", fortune)
        val reopenedScope = factory.invoke(null, "synthetic-deletion-scope", fortune)
        assertNotSame("Closed compositions must restore from storage, not revive deleted in-memory threads", firstScope, reopenedScope)
    }
}
