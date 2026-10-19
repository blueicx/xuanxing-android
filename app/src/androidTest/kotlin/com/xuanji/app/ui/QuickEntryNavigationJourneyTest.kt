package com.xuanji.app.ui

import android.os.Build
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.xuanji.app.MainActivity
import com.xuanji.app.daily.DailyQuickEntryWidget
import com.xuanji.app.data.model.UserProfile
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.DailyQuickEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*

/** Real app routes, only in the separately installed synthetic regression package. */
class QuickEntryNavigationJourneyTest {
    @get:Rule val compose = createEmptyComposeRule()
    private var activity: ActivityScenario<MainActivity>? = null
    private val synthetic = UserProfile(2001, 2, 3, 9, 15, "隔离合成资料", "CN.SH", 31.2304, 121.4737, "女")
    @Before fun requireSyntheticPackage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Assume.assumeTrue(context.packageName == "com.xuanji.app.regression")
        Assume.assumeTrue(Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish"))
        runBlocking {
            val old = AppModule.repository.userProfileFlow.first()
            require(old == null || old == synthetic) { "Refuse to overwrite a non-synthetic regression profile" }
            AppModule.repository.saveUserProfile(synthetic)
            AppModule.repository.setGuideSeen()
        }
    }
    @After fun closeActivity() { activity?.close() }
    @Test fun agendaShortcutOpensActualAgendaBeforeHistoryContent() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        activity = ActivityScenario.launch(DailyQuickEntryWidget.entryIntent(context, DailyQuickEntry.Agenda))
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("daily-life-panel").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("daily-life-panel").assertIsDisplayed()
        compose.onNodeWithText("安排与记录日历", substring = true).assertExists()
    }
    @Test fun resumeShortcutActuallyOpensSavedCompanionStageNotJustTodayTab() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        activity = ActivityScenario.launch(DailyQuickEntryWidget.entryIntent(context, DailyQuickEntry.Resume))
        compose.waitUntil(25_000) { compose.onAllNodesWithTag("stage-current-character-name", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("stage-current-character-name", useUnmergedTree = true).assertIsDisplayed()
        Assert.assertEquals("companion", DailyQuickEntryWidget.entryIntent(context, DailyQuickEntry.Resume).getStringExtra("open_tab"))
    }
}
