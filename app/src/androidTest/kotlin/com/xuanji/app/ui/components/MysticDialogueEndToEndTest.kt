package com.xuanji.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.model.Element
import com.xuanji.app.data.model.EasternDailyFortune
import com.xuanji.app.data.model.FortuneDimension
import com.xuanji.app.data.model.WesternDailyFortune
import com.xuanji.app.domain.DialogueContext
import com.xuanji.app.domain.DialogueReply
import com.xuanji.app.domain.MysticCompanionAction
import com.xuanji.app.domain.MysticCompanionActionRouter
import com.xuanji.app.domain.MysticDialogueCoordinator
import com.xuanji.app.domain.MysticEvent
import com.xuanji.app.domain.MysticSessionState
import com.xuanji.app.domain.OfflineDialogueProvider
import com.xuanji.app.domain.action.ActionEvidence
import com.xuanji.app.domain.action.ActionSource
import com.xuanji.app.domain.action.ActivitySuggestion
import com.xuanji.app.domain.action.CareerCluster
import com.xuanji.app.domain.action.ColorCluster
import com.xuanji.app.domain.action.ConfidenceLevel
import com.xuanji.app.domain.action.DailyActionPlan
import com.xuanji.app.domain.action.EnvironmentProfile
import com.xuanji.app.domain.action.LifeProfile
import com.xuanji.app.domain.action.MealSlot
import com.xuanji.app.domain.action.MealSuggestion
import com.xuanji.app.domain.action.OutingSuggestion
import com.xuanji.app.domain.action.RegionCandidate
import com.xuanji.app.domain.reduce
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MysticDialogueEndToEndTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun composer_sends_greeting_to_the_offline_engine_without_fortune_or_question_echo() {
        compose.setContent { DialogueUiTestHost() }

        send("你好")

        compose.onNode(hasText("玄师：", substring = true)).assertExists()
        compose.onAllNodes(hasText("你问：", substring = true)).assertCountEquals(0)
        compose.onAllNodes(hasText("综合", substring = true)).assertCountEquals(0)
    }

    @Test
    fun meal_question_shows_the_same_computed_meal_in_reply_and_action_card() {
        compose.setContent { DialogueUiTestHost() }

        send("今天吃什么")

        compose.onNodeWithText("今天吃什么").assertExists()
        compose.onAllNodesWithText("菌菇鸡肉饭", substring = true).assertCountEquals(2)
        compose.onNodeWithText("食材：鸡肉、菌菇").assertExists()
    }

    @Test
    fun life_profile_question_opens_the_profile_card_with_synthetic_result() {
        compose.setContent { DialogueUiTestHost() }

        send("适合什么工作和颜色")

        compose.onNodeWithText("人生画像匹配").assertExists()
        compose.onNodeWithText("职业方向：设计策划 · 82 分").assertExists()
        compose.onNodeWithText("色彩灵感：青绿色 · #557A64").assertExists()
        compose.onNodeWithText("地区示例：日本 · 京都").assertExists()
    }

    @Test
    fun why_question_opens_evidence_with_the_engine_trace() {
        compose.setContent { DialogueUiTestHost() }

        send("为什么")

        compose.onNodeWithText("这条回答的依据").assertExists()
        compose.onNodeWithText("算法版本：x3-offline-2026.09").assertExists()
        compose.onNodeWithText("确定性 seed：", substring = true).assertExists()
    }

    @Test
    fun missing_action_data_returns_honest_fallback_without_a_fabricated_action_card() {
        compose.setContent { DialogueUiTestHost(includeDailyPlan = false) }

        send("今天吃什么")

        compose.onNodeWithText("今日行动还没算好", substring = true).assertExists()
        compose.onNodeWithText("今天吃什么").assertDoesNotExist()
        compose.onNodeWithText("菌菇鸡肉饭").assertDoesNotExist()
    }

    @Test
    fun health_question_keeps_the_medical_boundary_in_the_actual_reply() {
        compose.setContent { DialogueUiTestHost() }

        send("我该吃什么药")

        compose.onNodeWithText("不构成医疗建议，如有不适请咨询专业人士。", substring = true).assertExists()
        compose.onAllNodes(hasText("建议服用", substring = true)).assertCountEquals(0)
    }

    @Test
    fun finance_question_keeps_the_investment_boundary_in_the_actual_reply() {
        compose.setContent { DialogueUiTestHost() }

        send("告诉我买哪只股票一定赚钱")

        compose.onNodeWithText("不构成投资建议，请量力而行。", substring = true).assertExists()
        compose.onAllNodes(hasText("推荐买", substring = true)).assertCountEquals(0)
    }

    private fun send(text: String) {
        compose.onNode(hasSetTextAction()).performTextInput(text)
        compose.onNodeWithContentDescription("发送消息").performClick()
        compose.waitForIdle()
    }
}

private val dialogueTestCoordinator = MysticDialogueCoordinator(OfflineDialogueProvider())

/** Uses production UI, offline provider, engine, action router and reducer with in-memory synthetic data. */
@Composable
private fun DialogueUiTestHost(
    includeDailyPlan: Boolean = true,
    includeLifeProfile: Boolean = true
) {
    var session by remember { mutableStateOf(MysticSessionState()) }
    var routedAction by remember { mutableStateOf<MysticCompanionAction?>(null) }
    var lastReply by remember { mutableStateOf<DialogueReply?>(null) }
    val scope = rememberCoroutineScope()
    val context = remember(includeDailyPlan, includeLifeProfile) {
        syntheticContext(
            dailyActionPlan = testDailyActionPlan().takeIf { includeDailyPlan },
            lifeProfile = testLifeProfile().takeIf { includeLifeProfile }
        )
    }
    MaterialTheme {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            MysticConversationPanel(
                state = session,
                onSend = { input ->
                    routedAction = MysticCompanionActionRouter.route(
                        input = input,
                        dailyActionPlan = context.dailyActionPlan,
                        lifeProfile = context.lifeProfile,
                        hasXiangqiArchive = false
                    )
                    lastReply = null
                    val requestState = session
                    scope.launch {
                        dialogueTestCoordinator.complete(requestState, context, input).forEach { event ->
                            if (event is MysticEvent.ReplySucceeded) lastReply = event.reply
                            session = reduce(session, event)
                        }
                    }
                },
                onQuickPrompt = {},
                onCancel = {},
                onRetry = {}
            )
            routedAction?.let { action ->
                MysticCompanionActionCard(
                    action = action,
                    evidenceLines = lastReply?.evidenceLines().orEmpty(),
                    onDismiss = { routedAction = null }
                )
            }
        }
    }
}

private fun DialogueReply.evidenceLines(): List<String> {
    val trace = evidence ?: return emptyList()
    return buildList {
        add("算法版本：${trace.algorithmVersion}")
        add("确定性 seed：${trace.seed}")
        addAll(trace.inputs)
        addAll(trace.items.map { "${it.status.label}：${it.label} · ${it.detail}" })
    }
}

private fun syntheticContext(
    dailyActionPlan: DailyActionPlan?,
    lifeProfile: LifeProfile?
) = DialogueContext(
    profileKey = "android-test-synthetic-profile",
    dateKey = "2026-09-30",
    mode = "scholar",
    styleKey = "archive",
    skinId = "",
    topicKey = "composite",
    fortune = testFortune(),
    dailyActionPlan = dailyActionPlan,
    lifeProfile = lifeProfile
)

private fun testFortune() = CompositeDailyFortune(
    dateKey = "2026-09-30",
    overallScore = 72,
    dimensions = listOf(FortuneDimension("career", "事业", 70, "稳步推进")),
    luckyNumber = 6,
    luckyColor = "青",
    luckyDirection = "东南",
    cautions = "别硬顶",
    eastern = EasternDailyFortune(
        dateKey = "2026-09-30",
        overallScore = 68,
        careerScore = 70,
        wealthScore = 65,
        loveScore = 66,
        healthScore = 69,
        summary = "东方盘平稳",
        advice = "稳步推进",
        dayPillarText = "甲子",
        favorableToday = emptyList<Element>(),
        luckyColor = "青",
        luckyDirection = "东南"
    ),
    western = WesternDailyFortune(
        dateKey = "2026-09-30",
        sign = "处女座",
        overallScore = 74,
        careerScore = 73,
        wealthScore = 72,
        loveScore = 71,
        healthScore = 75,
        summary = "西方盘平稳",
        luckyNumber = 6,
        luckyColor = "青",
        luckyDirection = "东南",
        dimensionBasis = emptyMap()
    ),
    period = "day",
    periodSummary = "平稳推进",
    insights = emptyList()
)

private fun testDailyActionPlan() = DailyActionPlan(
    dateKey = "2026-09-30",
    profileKey = "android-test-synthetic-profile",
    cityKey = null,
    meals = listOf(
        MealSuggestion(
            slot = MealSlot.Lunch,
            title = "菌菇鸡肉饭",
            ingredients = listOf("鸡肉", "菌菇"),
            substitute = "豆腐",
            deliveryKeywords = listOf("菌菇饭"),
            score = 88,
            evidence = listOf(ActionEvidence(ActionSource.FiveElements, "wood", "喜用五行：木", 40)),
            estimatedPriceCents = 2800,
            prepMinutes = 20
        )
    ),
    activities = listOf(
        ActivitySuggestion("整理书桌", 15..20, "午后", null, 80, emptyList())
    ),
    outings = listOf(
        OutingSuggestion("滨水公园", "适合舒展", "杭州", 79, emptyList())
    ),
    evidence = emptyList(),
    confidence = ConfidenceLevel.High,
    disclaimer = "生活灵感"
)

private fun testLifeProfile() = LifeProfile(
    profileKey = "android-test-synthetic-profile",
    careerClusters = listOf(CareerCluster("design", "设计策划", 82, listOf("木"))),
    colorPalette = listOf(ColorCluster("green", "青绿色", "#557A64", listOf("木"))),
    environmentProfile = EnvironmentProfile(listOf("滨水"), "舒缓", listOf("木水")),
    regionCandidates = listOf(RegionCandidate("日本", "京都", listOf("水"), listOf("实时因素未验证"), 65)),
    evidence = listOf(ActionEvidence(ActionSource.Zodiac, "virgo", "处女座：细节偏好", 25)),
    confidence = ConfidenceLevel.Medium,
    missingInputs = emptyList(),
    disclaimer = "匹配示例，不替代现实决策"
)
