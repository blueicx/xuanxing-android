package com.xuanji.app.domain

import com.xuanji.app.domain.action.DailyActionPlan
import com.xuanji.app.domain.action.LifeProfile

sealed interface MysticCompanionAction {
    data object OpenConversation : MysticCompanionAction
    data class ShowTodayMeal(val plan: DailyActionPlan) : MysticCompanionAction
    data class ShowTodayActivity(val plan: DailyActionPlan) : MysticCompanionAction
    data class ShowTodayOuting(val plan: DailyActionPlan) : MysticCompanionAction
    data class ShowLifeProfile(val profile: LifeProfile) : MysticCompanionAction
    data object ShowEvidence : MysticCompanionAction
    data class OpenGame(val gameId: String) : MysticCompanionAction
    data object ResumeXiangqi : MysticCompanionAction
}

/** Maps explicit companion commands to a real local feature. A null result stays in dialogue. */
object MysticCompanionActionRouter {
    fun route(
        input: String,
        dailyActionPlan: DailyActionPlan?,
        lifeProfile: LifeProfile?,
        hasXiangqiArchive: Boolean,
        referenceCategory: String = "",
        referenceId: String? = null
    ): MysticCompanionAction? {
        val clean = input.trim().take(200)
        if (clean.isBlank()) return null
        if (MysticAgendaCommand.isRequest(clean)) return MysticAgendaCommand.route(clean, dailyActionPlan, referenceCategory, referenceId)
        if (clean.contains("继续棋局") || clean.contains("恢复棋局")) {
            return MysticCompanionAction.ResumeXiangqi.takeIf { hasXiangqiArchive }
        }
        val game = com.xuanji.app.domain.game.CompanionGameCatalog.gameIdForInput(clean)
        if (game != null && game != "xiangqi") return MysticCompanionAction.OpenGame(game)
        if (game == "xiangqi" && (clean.contains("继续") || clean.contains("恢复"))) {
            return MysticCompanionAction.ResumeXiangqi.takeIf { hasXiangqiArchive }
        }
        val intent = MysticIntentClassifier.classify(clean)
        when (intent) {
            MysticIntent.Why -> MysticCompanionAction.ShowEvidence
            MysticIntent.TodayMeal -> dailyActionPlan?.let(MysticCompanionAction::ShowTodayMeal)
            MysticIntent.TodayActivity -> dailyActionPlan?.let(MysticCompanionAction::ShowTodayActivity)
            MysticIntent.TodayOuting -> dailyActionPlan?.let(MysticCompanionAction::ShowTodayOuting)
            MysticIntent.LifeProfile -> lifeProfile?.let(MysticCompanionAction::ShowLifeProfile)
            MysticIntent.Greeting, MysticIntent.Smalltalk -> MysticCompanionAction.OpenConversation
            else -> null
        }?.let { return it }
        if (clean in setOf("解释", "解释一下", "解释刚才", "解释刚才那项", "解释这个结果") || clean.startsWith("解释刚才")) {
            return MysticCompanionAction.ShowEvidence
        }
        return null
    }
}
