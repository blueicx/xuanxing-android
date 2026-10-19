package com.xuanji.app.domain

import com.xuanji.app.domain.action.DailyActionPlan

object MysticAgendaCommand {
    fun isRequest(input: String): Boolean = listOf("加入今日安排", "加入今天安排", "安排到今天", "加入日程").any(input::contains)

    fun route(input: String, plan: DailyActionPlan?, category: String, referenceId: String?): MysticCompanionAction? {
        if (!isRequest(input) || plan == null) return null
        val selectedCategory = when {
            listOf("饮食", "吃的", "午饭", "晚饭", "早餐").any(input::contains) -> "meal"
            "活动" in input -> "activity"
            "地点" in input || "出游" in input -> "outing"
            else -> category
        }
        val candidates = MysticReferenceResolver.fromPlan(plan, selectedCategory)
        val index = MysticReferenceResolver.requestedIndex(input)
        val id = if (index != null) candidates.getOrNull(index - 1)?.id else referenceId ?: candidates.firstOrNull()?.id
        val selected = MysticReferenceResolver.selectPlan(plan, selectedCategory, id) ?: return null
        return when (selectedCategory) {
            "meal" -> MysticCompanionAction.ShowTodayMeal(selected)
            "activity" -> MysticCompanionAction.ShowTodayActivity(selected)
            "outing" -> MysticCompanionAction.ShowTodayOuting(selected)
            else -> null
        }
    }
}
