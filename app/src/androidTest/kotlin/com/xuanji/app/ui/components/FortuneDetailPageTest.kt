package com.xuanji.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xuanji.app.domain.action.ActionEvidence
import com.xuanji.app.domain.action.ActionSource
import com.xuanji.app.domain.action.ActivitySuggestion
import com.xuanji.app.domain.action.ConfidenceLevel
import com.xuanji.app.domain.action.DailyActionPlan
import com.xuanji.app.domain.action.MealSlot
import com.xuanji.app.domain.action.MealSuggestion
import com.xuanji.app.domain.action.OutingSuggestion
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FortuneDetailPageTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun titled_fortune_card_opens_original_content_and_returns() {
        compose.setContent {
            MaterialTheme {
                FortuneCard(
                    modifier = Modifier.testTag("fortune-card-test"),
                    cardId = "career",
                    title = "今日事业"
                ) {
                    Text("工作节奏：先处理积压任务，再安排新计划。")
                    Text("盘面依据：沟通维度高于本周期均值。")
                }
            }
        }

        compose.onNodeWithTag("fortune-card-test").performClick()
        compose.onNodeWithTag("fortune-detail-page").assertExists()
        compose.onNodeWithText("工作节奏：先处理积压任务，再安排新计划。", useUnmergedTree = true)
            .assertExists()
        compose.onNodeWithText("盘面依据：沟通维度高于本周期均值。", useUnmergedTree = true)
            .assertExists()

        compose.onNodeWithContentDescription("返回今日事业").performClick()
        compose.onNodeWithText("点开看详细解说 ›").assertExists()

        compose.onNodeWithTag("fortune-detail-page").assertDoesNotExist()
        compose.onNodeWithText("工作节奏：先处理积压任务，再安排新计划。", useUnmergedTree = true)
            .assertExists()
        compose.onNodeWithText("盘面依据：沟通维度高于本周期均值。", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun meal_action_opens_its_own_detail_with_real_plan_fields() {
        val plan = DailyActionPlan(
            dateKey = "2026-09-30",
            profileKey = "test-profile",
            cityKey = null,
            meals = listOf(
                MealSuggestion(
                    slot = MealSlot.Lunch,
                    title = "菌菇鸡肉饭",
                    ingredients = listOf("鸡肉", "香菇", "米饭"),
                    substitute = "豆腐可替代鸡肉",
                    deliveryKeywords = listOf("鸡肉饭", "菌菇饭"),
                    score = 82,
                    evidence = listOf(
                        ActionEvidence(
                            source = ActionSource.FiveElements,
                            key = "wood",
                            label = "五行匹配",
                            contribution = 12
                        ),
                        ActionEvidence(
                            source = ActionSource.Preference,
                            key = "local",
                            label = "已按本机饮食偏好过滤",
                            contribution = 0
                        )
                    ),
                    estimatedPriceCents = 2400,
                    prepMinutes = 20
                )
            ),
            activities = listOf(
                ActivitySuggestion(
                    title = "公园慢走",
                    durationMinutes = 15..25,
                    bestPeriod = "午后",
                    avoid = "高温时段",
                    score = 76,
                    evidence = emptyList()
                )
            ),
            outings = listOf(
                OutingSuggestion(
                    placeType = "湖边步道",
                    reason = "环境开阔，适合短时散步",
                    cityLabel = "杭州 · 西湖",
                    score = 71,
                    evidence = emptyList(),
                    indoor = false
                )
            ),
            evidence = emptyList(),
            confidence = ConfidenceLevel.High,
            disclaimer = "生活方式灵感，不是医疗建议。"
        )

        compose.setContent {
            MaterialTheme {
                DailyActionSection(plan = plan)
            }
        }

        compose.onNodeWithText("换一个").assertDoesNotExist()
        compose.onNodeWithText("已采纳").assertDoesNotExist()
        compose.onNodeWithText("不合适").assertDoesNotExist()
        compose.onNodeWithText("为什么？").assertDoesNotExist()
        compose.onNodeWithTag("daily-action-card-吃什么").performClick()
        compose.onNodeWithTag("fortune-detail-page").assertExists()
        compose.onNodeWithText("替代食材").assertExists()
        compose.onNodeWithText("豆腐可替代鸡肉").assertExists()
        compose.onNodeWithText("准备时间").assertExists()
        compose.onNodeWithText("20 分钟").assertExists()
        compose.onNodeWithText("匹配依据").assertExists()
        compose.onNodeWithText("五行 · 五行匹配").assertExists()
        compose.onNodeWithText("加权参考 +12 分").assertExists()
        compose.onNodeWithText("用于偏好过滤，不计入评分").assertExists()
        compose.onNodeWithText("五行匹配", substring = true).assertExists()
        compose.onNodeWithText("相关行动").assertExists()
        compose.onNodeWithText("已采纳").assertExists()
        compose.onNodeWithText("不合适").assertExists()

        compose.onNodeWithText("查看做什么详情 ›").performScrollTo().performClick()
        compose.onNodeWithTag("fortune-detail-title-做什么").assertExists()
        compose.onNodeWithText("建议时长").assertExists()
        compose.onNodeWithText("15–25 分钟").assertExists()
        compose.onNodeWithText("适合时段").assertExists()
        compose.onNodeWithText("午后").assertExists()

        compose.onNodeWithContentDescription("返回做什么").performClick()
        compose.onNodeWithTag("daily-action-card-去哪玩").performClick()
        compose.onNodeWithText("地区示例").assertExists()
        compose.onAllNodesWithText("杭州 · 西湖")[0].assertExists()
        compose.onNodeWithText("建议理由").assertExists()
        compose.onNodeWithText("环境开阔，适合短时散步").assertExists()
    }
}
