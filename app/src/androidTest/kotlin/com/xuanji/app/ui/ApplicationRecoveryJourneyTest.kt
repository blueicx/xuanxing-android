package com.xuanji.app.ui

import android.content.Intent
import android.os.Build
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.xuanji.app.MainActivity
import com.xuanji.app.data.local.DataStorePreferenceBridge
import com.xuanji.app.data.local.MysticStageCheckpointStore
import com.xuanji.app.data.local.CompanionGameProgressStore
import com.xuanji.app.data.model.UserProfile
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.MysticCharacterId
import com.xuanji.app.domain.MysticDrawerState
import com.xuanji.app.domain.game.CompanionGameProgressState
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import java.time.LocalDate

/** Two explicit phases. An external emulator-only script kills the real process between them. */
class ApplicationRecoveryJourneyTest {
    @get:Rule val compose = createEmptyComposeRule()
    private var activity: ActivityScenario<MainActivity>? = null
    private val synthetic = UserProfile(2001, 2, 3, 9, 15, "隔离合成资料", "CN.SH", 31.2304, 121.4737, "女")

    @Before fun require_an_isolated_application_and_emulator() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Assume.assumeTrue(context.packageName == "com.xuanji.app.regression")
        Assume.assumeTrue(Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish"))
    }
    @After fun close_test_activity() { activity?.close() }

    @Test fun prepare_navigation_and_progress() {
        runBlocking {
            val current = AppModule.repository.userProfileFlow.first()
            require(current == null || current == synthetic) { "Refuse to overwrite any non-synthetic regression profile" }
            AppModule.repository.saveUserProfile(synthetic)
            AppModule.repository.setGuideSeen()
            AppModule.repository.setMysticGuideEnabled(true)
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val bridge = DataStorePreferenceBridge(context)
            val bazi = AppModule.repository.baziFullFlow.first { it != null }!!
            val visit = "bazi|${bazi.chart.display}"
            // Only this guarded, separate-package synthetic fixture is reset.
            CompanionGameProgressStore(bridge).clear(visit, MysticCharacterId.NadirRashid, "silkroad_route")
            com.xuanji.app.data.local.MysticCharacterSessionStore(bridge).clearAll(visit)
            MysticStageCheckpointStore(bridge).clear(AppModule.actionRepository.profileKey(synthetic))
        }
        openActualApplication()
        compose.waitUntil(20_000) { compose.onAllNodes(hasContentDescription("微光浮球", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasContentDescription("微光浮球", substring = true)).performClick()
        chooseNadir()
        compose.onNodeWithText("进入对话").performClick()
        compose.onNodeWithTag("mystic-conversation-input", useUnmergedTree = true).performScrollTo().assertIsDisplayed().performTextInput("今天吃什么")
        pressDeviceBack()
        compose.onNodeWithContentDescription("发送消息").performScrollTo().assertIsDisplayed().performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("companion-action-card", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText("食材：", substring = true) and hasAnyAncestor(hasTestTag("companion-action-card")), useUnmergedTree = true).assertExists()
        val expectedMeal = runBlocking { requireNotNull(AppModule.actionRepository.loadToday()).meals.first().title }
        compose.onNode(hasText(expectedMeal) and hasAnyAncestor(hasTestTag("companion-action-card")), useUnmergedTree = true).assertExists()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val visitProfile = runBlocking { AppModule.repository.baziFullFlow.first { it != null }!!.let { "bazi|${it.chart.display}" } }
        compose.waitUntil(10_000) {
            runBlocking {
                com.xuanji.app.data.local.MysticCharacterSessionStore(DataStorePreferenceBridge(context)).load(visitProfile)
                    ?.session(MysticCharacterId.NadirRashid)?.messages?.any {
                        it.role == com.xuanji.app.domain.MysticMessageRole.Mystic && it.text.contains(expectedMeal)
                    } == true
            }
        }
        val foodPreferenceBefore = runBlocking { AppModule.actionRepository.readFoodPreference(AppModule.actionRepository.profileKey(synthetic)) }
        sendInput("不要豆腐，只有十五分钟")
        val conditions = com.xuanji.app.domain.TemporaryActionConditions.parse("不要豆腐，只有十五分钟")
        val filtered = runBlocking { requireNotNull(AppModule.actionRepository.loadToday(temporary = conditions)) }
        Assert.assertTrue(filtered.meals.size >= 2)
        Assert.assertTrue(filtered.meals.all { it.prepMinutes != null && it.prepMinutes!! <= 15 && it.ingredients.none { ingredient -> ingredient.contains("豆腐") } })
        assertActionTitle(filtered.meals.first().title)
        sendInput("第二个")
        assertActionTitle(filtered.meals[1].title)
        compose.waitUntil(10_000) {
            runBlocking {
                val session = com.xuanji.app.data.local.MysticCharacterSessionStore(DataStorePreferenceBridge(context)).load(visitProfile)
                    ?.session(MysticCharacterId.NadirRashid)
                session?.interaction?.referenceId == filtered.meals[1].candidateKey && session.messages.any {
                    it.role == com.xuanji.app.domain.MysticMessageRole.Mystic && it.text.contains(filtered.meals[1].title)
                }
            }
        }
        Assert.assertEquals(foodPreferenceBefore, runBlocking { AppModule.actionRepository.readFoodPreference(AppModule.actionRepository.profileKey(synthetic)) })
        sendInput("为什么")
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasText("这条回答的依据") and hasAnyAncestor(hasTestTag("companion-action-card")), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasText("当前候选：${filtered.meals[1].title}") and hasAnyAncestor(hasTestTag("companion-action-card")), useUnmergedTree = true).assertExists()
        compose.waitUntil(10_000) {
            runBlocking {
                val session = com.xuanji.app.data.local.MysticCharacterSessionStore(DataStorePreferenceBridge(context)).load(visitProfile)
                    ?.session(MysticCharacterId.NadirRashid)
                session?.interaction?.referenceId == filtered.meals[1].candidateKey && session.messages
                    .lastOrNull { it.role == com.xuanji.app.domain.MysticMessageRole.Mystic }?.text?.contains(filtered.meals[1].title) == true
            }
        }
        compose.onNodeWithContentDescription("收起对话抽屉").performClick()
        compose.onNodeWithContentDescription("与纳迪尔·拉希德进入丝路路线规划").performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("silkroad-map-canvas", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.waitUntil(10_000) {
            !compose.onNodeWithText("前往敦煌（2天·3资源）").fetchSemanticsNode().config
                .contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled)
        }
        compose.onNodeWithText("前往敦煌（2天·3资源）", useUnmergedTree = true).performScrollTo().assertIsDisplayed().performClick()
        compose.waitUntil(10_000) {
            runBlocking {
                val progress = CompanionGameProgressStore(DataStorePreferenceBridge(context)).load(visitProfile, MysticCharacterId.NadirRashid, "silkroad_route")
                (progress as? CompanionGameProgressState.SilkRoad)?.value?.current == "敦煌"
            }
        }
        compose.waitUntil(10_000) {
            runBlocking {
                val saved = MysticStageCheckpointStore(DataStorePreferenceBridge(context)).load(AppModule.actionRepository.profileKey(synthetic), LocalDate.now().toString())
                saved?.stageOpen == true && saved.characterId == MysticCharacterId.NadirRashid && saved.drawers[saved.characterId] == MysticDrawerState.Expanded
            }
        }
        compose.waitUntil(10_000) {
            runBlocking {
                com.xuanji.app.data.local.MysticCharacterSessionStore(DataStorePreferenceBridge(context)).load(visitProfile)
                    ?.session(MysticCharacterId.NadirRashid)?.interaction?.gameId == "silkroad_route"
            }
        }
        runBlocking {
            val key = AppModule.actionRepository.profileKey(synthetic)
            val life = requireNotNull(AppModule.dailyLifeStore)
            // Guarded regression-package fixture only; never production or user inventory.
            val preferenceBridge = DataStorePreferenceBridge(context)
            val preferenceStore = com.xuanji.app.data.local.FoodPreferenceStore(preferenceBridge)
            val preferenceKey = preferenceStore.memoryKey(key)
            val previousPreference = preferenceBridge.read(preferenceKey)
            try {
                preferenceBridge.write(preferenceKey, """{"version":999,"profileKeyDigest":"${com.xuanji.app.data.local.FoodPreferenceStore.fingerprint(key)}","preference":{}}""")
                Assert.assertNull("Unreadable preferences must not remove restrictions", AppModule.actionRepository.loadToday())
                Assert.assertNull("Weekly pool must use the same preference boundary", AppModule.actionRepository.loadMealPool(key))
            } finally {
                if (previousPreference == null) preferenceBridge.delete(preferenceKey) else preferenceBridge.write(preferenceKey, previousPreference)
            }
            life.clearSections(key, purchases = true, weeklyDraft = true)
            life.removePantryItem(key, "回归番茄")
            life.savePurchase(key, com.xuanji.app.domain.ShoppingPurchase("2026-10-19", "回归番茄", true, "2", "个"))
            life.transferPurchaseToPantry(key, "2026-10-19", "回归番茄", "2026-10-23", "蔬果")
            life.saveWeeklyDraft(key, com.xuanji.app.domain.action.WeeklyMenuDraft("2026-10-19", "200", 2,
                listOf(com.xuanji.app.domain.action.WeeklyMealLock("2026-10-19", com.xuanji.app.domain.action.MealSlot.Lunch, "tomato-egg-noodle"))))
            life.saveJournal(key, com.xuanji.app.domain.DailyJournalEntry("2026-10-19", "隔离恢复日记", "平静", "只记录主动输入", 123))
            DataStorePreferenceBridge(context).write("regression_preparation_pid", android.os.Process.myPid().toString())
        }
    }

    @Test fun verify_after_process_restart() {
        runBlocking {
            require(AppModule.repository.userProfileFlow.first() == synthetic) { "Preparation phase has not saved the isolated fixture" }
            val before = DataStorePreferenceBridge(InstrumentationRegistry.getInstrumentation().targetContext).read("regression_preparation_pid")
            require(before != null && before != android.os.Process.myPid().toString()) { "Must run in a new process after preparation" }
        }
        openActualApplication()
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("stage-drawer-expanded", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        runBlocking {
            val visit = AppModule.repository.baziFullFlow.first { it != null }!!.let { "bazi|${it.chart.display}" }
            val restored = requireNotNull(com.xuanji.app.data.local.MysticCharacterSessionStore(DataStorePreferenceBridge(InstrumentationRegistry.getInstrumentation().targetContext)).load(visit))
                .session(MysticCharacterId.NadirRashid).interaction
            Assert.assertEquals(15, restored.conditions.maxMinutes)
            Assert.assertEquals(setOf("豆腐"), restored.conditions.excludedIngredients)
            val plan = requireNotNull(AppModule.actionRepository.loadToday(temporary = restored.conditions))
            Assert.assertEquals(plan.meals[1].candidateKey, restored.referenceId)
            val key = AppModule.actionRepository.profileKey(synthetic)
            val life = requireNotNull(AppModule.dailyLifeStore).load(key)
            Assert.assertTrue(life.readable)
            Assert.assertEquals(com.xuanji.app.domain.PantryItem("回归番茄", quantity = "2", unit = "个", expiresOn = "2026-10-23", category = "蔬果"),
                life.pantry.single { it.ingredient == "回归番茄" })
            Assert.assertTrue(life.shoppingPurchases.single { it.ingredient == "回归番茄" }.transferredToPantry)
            Assert.assertEquals("200", life.weeklyMenuDraft!!.budgetYuan)
            Assert.assertEquals("tomato-egg-noodle", life.weeklyMenuDraft!!.locks.single().candidateKey)
            Assert.assertEquals(com.xuanji.app.domain.DailyJournalEntry("2026-10-19", "隔离恢复日记", "平静", "只记录主动输入", 123), life.journal.single { it.dateKey == "2026-10-19" })
            Assert.assertNull(requireNotNull(AppModule.dailyLifeStore).load("isolated-other-profile").weeklyMenuDraft)
        }
        compose.onNodeWithTag("stage-current-character-name", useUnmergedTree = true).assertTextEquals("纳迪尔·拉希德")
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("silkroad-map-canvas", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("敦煌 · 剩余 6 天 / 9 点补给").assertExists()
        pressDeviceBack()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("stage-drawer-peek", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("stage-drawer-peek", useUnmergedTree = true).assertExists()
        pressDeviceBack()
        compose.waitUntil(10_000) { compose.onAllNodes(hasContentDescription("微光浮球", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasContentDescription("微光浮球", substring = true)).performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("stage-drawer-peek", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("stage-drawer-peek", useUnmergedTree = true).assertExists()
    }

    @Test fun four_roles_handoff_and_independent_game_progress_use_real_application() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val visit = runBlocking {
            require(AppModule.repository.userProfileFlow.first() == synthetic)
            AppModule.repository.baziFullFlow.first { it != null }!!.let { "bazi|${it.chart.display}" }
        }
        val store = CompanionGameProgressStore(DataStorePreferenceBridge(context))
        runBlocking {
            store.clear(visit, MysticCharacterId.ShenYanzhou, "poetry_chain")
            store.clear(visit, MysticCharacterId.EvelynNova, "star_map_observation")
        }
        openActualApplication()
        compose.waitUntil(20_000) {
            compose.onAllNodesWithTag("stage-current-character-name", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodes(hasContentDescription("微光浮球", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
        if(compose.onAllNodesWithTag("stage-current-character-name", useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
            compose.onNode(hasContentDescription("微光浮球", substring = true)).performClick()
        chooseCharacter("沈砚舟")
        compose.onNodeWithText("进入对话").performClick()
        sendInput("今天吃什么")
        assertActionTitle(runBlocking { requireNotNull(AppModule.actionRepository.loadToday()).meals.first().title })
        compose.onNode(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.HorizontalScrollAxisRange)
            and hasAnyDescendant(hasText("转交伊芙琳·诺瓦")), useUnmergedTree = true).performScrollTo()
        compose.onNodeWithText("转交伊芙琳·诺瓦", useUnmergedTree = true).performScrollTo().assertIsDisplayed().performClick()
        compose.waitUntil(10_000) { currentName() == "伊芙琳·诺瓦" }
        compose.onNodeWithContentDescription("收起对话抽屉").performClick()
        compose.onNodeWithContentDescription("与伊芙琳·诺瓦进入星图观测").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("starmap-option-0", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("starmap-option-0", useUnmergedTree = true).performScrollTo().performClick()
        compose.waitUntil(10_000) { runBlocking { (store.load(visit, MysticCharacterId.EvelynNova, "star_map_observation") as? CompanionGameProgressState.StarMap)?.value?.path?.size == 2 } }
        chooseCharacter("沈砚舟")
        compose.onNodeWithContentDescription("与沈砚舟进入诗句接龙").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("poetry-option-0", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("poetry-option-0", useUnmergedTree = true).performScrollTo().performClick()
        compose.waitUntil(10_000) { runBlocking { (store.load(visit, MysticCharacterId.ShenYanzhou, "poetry_chain") as? CompanionGameProgressState.Poetry)?.value?.undoHistory?.size == 1 } }
        chooseCharacter("墨衡")
        compose.onNodeWithContentDescription("与墨衡进入中国象棋").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("square-0-6", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("square-0-6", useUnmergedTree = true).performScrollTo().performClick()
        compose.onNodeWithTag("square-0-5", useUnmergedTree = true).performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("game-save").fetchSemanticsNodes().isNotEmpty() }
        compose.waitUntil(15_000) { !compose.onNodeWithTag("game-save", useUnmergedTree = true).fetchSemanticsNode().config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) }
        compose.onNodeWithTag("game-save", useUnmergedTree = true).performScrollTo().assertIsEnabled().assertIsDisplayed().performClick()
        compose.waitUntil(10_000) { runBlocking { com.xuanji.app.data.local.GameArchiveStore(context).load(visit)?.moves?.isNotEmpty() == true } }
        chooseNadir()
        compose.onNodeWithContentDescription("与纳迪尔·拉希德进入丝路路线规划").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("敦煌 · 剩余 6 天 / 9 点补给").fetchSemanticsNodes().isNotEmpty() }
        runBlocking {
            val threads = requireNotNull(com.xuanji.app.data.local.MysticCharacterSessionStore(DataStorePreferenceBridge(context)).load(visit))
            Assert.assertTrue(threads.session(MysticCharacterId.ShenYanzhou).messages.any { it.text == "今天吃什么" })
            Assert.assertTrue(threads.session(MysticCharacterId.EvelynNova).messages.none { it.role == com.xuanji.app.domain.MysticMessageRole.User && it.text == "今天吃什么" })
            Assert.assertNull(store.load("isolated-other-profile", MysticCharacterId.NadirRashid, "silkroad_route"))
            Assert.assertNull(store.load(visit, MysticCharacterId.ShenYanzhou, "silkroad_route"))
        }
        // Only fixture injection is replaced; navigation and data loading remain production code.
        val secondFixture = synthetic.copy(birthDay = 4, locationName = "隔离合成资料二")
        try {
            compose.onNodeWithContentDescription("关闭玄师台").performClick()
            activity?.close(); activity = null
            runBlocking {
                AppModule.repository.saveUserProfile(secondFixture)
                AppModule.repository.baziFullFlow.first { it != null && "bazi|${it.chart.display}" != visit }
            }
            openActualApplication()
            compose.waitUntil(20_000) {
                compose.onAllNodesWithTag("stage-current-character-name", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() ||
                    compose.onAllNodes(hasContentDescription("微光浮球", substring = true)).fetchSemanticsNodes().isNotEmpty()
            }
            if(compose.onAllNodesWithTag("stage-current-character-name", useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
                compose.onNode(hasContentDescription("微光浮球", substring = true)).performClick()
            chooseNadir()
            compose.onNodeWithContentDescription("与纳迪尔·拉希德进入丝路路线规划").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("长安 · 剩余 8 天 / 12 点补给").fetchSemanticsNodes().isNotEmpty() }
            Assert.assertEquals("敦煌", runBlocking { (store.load(visit, MysticCharacterId.NadirRashid,"silkroad_route") as CompanionGameProgressState.SilkRoad).value.current })
        } finally {
            activity?.close(); activity = null
            runBlocking { AppModule.repository.saveUserProfile(synthetic) }
        }
    }

    private fun currentName() = compose.onNodeWithTag("stage-current-character-name", useUnmergedTree = true)
        .fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].first().text

    private fun chooseCharacter(name: String) {
        val names = listOf("沈砚舟", "墨衡", "伊芙琳·诺瓦", "纳迪尔·拉希德")
        repeat(4) {
            if(currentName() == name) return
            val right = names.indexOf(currentName()) > names.indexOf(name)
            compose.onNodeWithContentDescription("玄师角色画廊，可左右滑动切换角色", useUnmergedTree = true)
                .performTouchInput { if(right) swipeRight() else swipeLeft() }
            compose.waitForIdle()
        }
        Assert.assertEquals(name, currentName())
    }

    private fun openActualApplication() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("pm grant com.xuanji.app.regression android.permission.POST_NOTIFICATIONS").close()
        activity = ActivityScenario.launch(Intent(instrumentation.targetContext, MainActivity::class.java))
    }
    private fun pressDeviceBack() {
        val command = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input keyevent 4")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(command).use { it.readBytes() }
        compose.waitForIdle()
    }
    private fun sendInput(text: String) {
        compose.waitUntil(10_000) {
            !compose.onNodeWithTag("mystic-conversation-input", useUnmergedTree = true).fetchSemanticsNode().config
                .contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled)
        }
        compose.onNodeWithTag("mystic-conversation-input", useUnmergedTree = true).performScrollTo().performTextInput(text)
        pressDeviceBack()
        compose.onNodeWithContentDescription("发送消息").performScrollTo().assertIsEnabled().performClick()
    }
    private fun assertActionTitle(title: String) {
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasText(title) and hasAnyAncestor(hasTestTag("companion-action-card")), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }
    private fun chooseNadir() {
        repeat(4) {
            if (compose.onNodeWithTag("stage-current-character-name", useUnmergedTree = true).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].first().text == "纳迪尔·拉希德") return
            compose.onNodeWithContentDescription("玄师角色画廊，可左右滑动切换角色", useUnmergedTree = true).performTouchInput { swipeLeft() }
            compose.waitForIdle()
        }
        compose.onNodeWithTag("stage-current-character-name", useUnmergedTree = true).assertTextEquals("纳迪尔·拉希德")
    }
}
