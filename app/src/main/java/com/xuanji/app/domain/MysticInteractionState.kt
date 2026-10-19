package com.xuanji.app.domain

import com.xuanji.app.domain.action.DailyActionPlan
import com.xuanji.app.domain.action.LifeProfile
import java.time.LocalDate

/** Only explicit session choices/IDs, never a second copy of fortune facts or generated prose. */
data class MysticInteractionState(
    val dateKey: String = "",
    val conditions: TemporaryActionConditions = TemporaryActionConditions(),
    val referenceCategory: String = "",
    val referenceId: String? = null,
    val actionKey: String = "",
    val evidenceOpen: Boolean = false,
    val gameId: String? = null,
    val task: MysticTaskReference? = null
) {
    fun forDate(date: String): MysticInteractionState = when {
        !valid() -> MysticInteractionState(dateKey = date)
        dateKey == date -> this
        else -> MysticInteractionState(dateKey = date, gameId = gameId, actionKey = if (actionKey == "game") "game" else "", task = task)
    }

    fun valid(): Boolean = runCatching {
        (dateKey.isEmpty() || LocalDate.parse(dateKey).toString() == dateKey) &&
            (task == null || task.valid()) &&
            referenceCategory in setOf("", "meal", "activity", "outing") &&
            (referenceId == null || referenceId.length <= 180) &&
            actionKey in setOf("", "conversation", "meal", "activity", "outing", "profile", "evidence", "game", "resume_xiangqi") &&
            (gameId == null || gameId in setOf("poetry_chain", "star_map_observation", "silkroad_route", "xiangqi")) &&
            (conditions.maxMinutes == null || conditions.maxMinutes in 5..480) &&
            (conditions.maxBudgetCents == null || conditions.maxBudgetCents in 100..1_000_000) &&
            conditions.excludedIngredients.size <= 12 && conditions.excludedIngredients.all { it.isNotBlank() && it.length <= 100 }
    }.getOrDefault(false)

    fun restoreAction(plan: DailyActionPlan?, profile: LifeProfile?): MysticCompanionAction? {
        val realPlan = if (referenceId == null) plan else MysticReferenceResolver.selectPlan(plan, referenceCategory, referenceId)
        return when (actionKey) {
            "conversation" -> MysticCompanionAction.OpenConversation
            "meal" -> realPlan?.let(MysticCompanionAction::ShowTodayMeal)
            "activity" -> realPlan?.let(MysticCompanionAction::ShowTodayActivity)
            "outing" -> realPlan?.let(MysticCompanionAction::ShowTodayOuting)
            "profile" -> profile?.let(MysticCompanionAction::ShowLifeProfile)
            "evidence" -> MysticCompanionAction.ShowEvidence
            "game" -> gameId?.let(MysticCompanionAction::OpenGame)
            else -> null // Restoring metadata must never auto-start a Xiangqi game without its archive.
        }
    }

    fun withAction(action: MysticCompanionAction?): MysticInteractionState = copy(actionKey = when (action) {
        null -> ""
        MysticCompanionAction.OpenConversation -> "conversation"
        is MysticCompanionAction.ShowTodayMeal -> "meal"
        is MysticCompanionAction.ShowTodayActivity -> "activity"
        is MysticCompanionAction.ShowTodayOuting -> "outing"
        is MysticCompanionAction.ShowLifeProfile -> "profile"
        MysticCompanionAction.ShowEvidence -> "evidence"
        is MysticCompanionAction.OpenGame -> "game"
        MysticCompanionAction.ResumeXiangqi -> "resume_xiangqi"
    }, gameId = (action as? MysticCompanionAction.OpenGame)?.gameId ?: gameId)
}
