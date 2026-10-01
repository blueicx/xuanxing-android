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
        hasXiangqiArchive: Boolean
    ): MysticCompanionAction? {
        val clean = input.trim().take(200)
        if (clean.isBlank()) return null
        if (clean.contains("继续棋局") || clean.contains("恢复棋局")) {
            return MysticCompanionAction.ResumeXiangqi.takeIf { hasXiangqiArchive }
        }
        val game = com.xuanji.app.domain.game.CompanionGameCatalog.gameIdForInput(clean)
        if (game != null && game != "xiangqi") return MysticCompanionAction.OpenGame(game)
        if (game == "xiangqi" && (clean.contains("继续") || clean.contains("恢复"))) {
            return MysticCompanionAction.ResumeXiangqi.takeIf { hasXiangqiArchive }
        }
        if (clean.contains("解释") || clean.contains("依据") || clean.contains("怎么算") || clean.contains("来源")) {
            return MysticCompanionAction.ShowEvidence
        }
        return when (MysticIntentClassifier.classify(clean)) {
            MysticIntent.Why -> MysticCompanionAction.ShowEvidence
            MysticIntent.TodayMeal -> dailyActionPlan?.let(MysticCompanionAction::ShowTodayMeal)
            MysticIntent.TodayActivity -> dailyActionPlan?.let(MysticCompanionAction::ShowTodayActivity)
            MysticIntent.TodayOuting -> dailyActionPlan?.let(MysticCompanionAction::ShowTodayOuting)
            MysticIntent.LifeProfile -> lifeProfile?.let(MysticCompanionAction::ShowLifeProfile)
            MysticIntent.Greeting, MysticIntent.Smalltalk -> MysticCompanionAction.OpenConversation
            else -> null
        }
    }
}
