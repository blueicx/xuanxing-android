package com.xuanji.app.domain

import com.xuanji.app.domain.action.ActionFeedback
import com.xuanji.app.domain.action.ActionFeedbackKind
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields

enum class LocalReviewFilter { All, Favorites, ActionFeedback }
enum class LocalReviewSource { Favorite, AcceptedAction, NotSuitableAction, ReplacedAction }
enum class LocalReviewPeriod { Week, Month }

data class LocalReviewSummary(
    val label: String,
    val favorites: Int,
    val acceptedActions: Int,
    val notSuitableActions: Int,
    val replacedActions: Int
) {
    val total: Int get() = favorites + acceptedActions + notSuitableActions + replacedActions
}

data class LocalReviewItem(
    val id: String,
    val source: LocalReviewSource,
    val title: String,
    val summary: String,
    val createdAt: Long
)

data class LocalReviewSnapshot(
    val items: List<LocalReviewItem>,
    val favoriteState: ReadingFavoriteDataState,
    val feedbackAvailable: Boolean
)

/** A retrospective view of only explicit user actions; it never reads conversation or inferred traits. */
object LocalReviewTimeline {
    fun summarize(
        items: List<LocalReviewItem>,
        period: LocalReviewPeriod,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): LocalReviewSummary {
        val range = when (period) {
            LocalReviewPeriod.Week -> {
                val start = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                start..start.plusDays(6)
            }
            LocalReviewPeriod.Month -> {
                val start = today.withDayOfMonth(1)
                start..start.plusMonths(1).minusDays(1)
            }
        }
        val current = items.filter { item ->
            runCatching { java.time.Instant.ofEpochMilli(item.createdAt).atZone(zoneId).toLocalDate() in range }
                .getOrDefault(false)
        }
        val week = today.get(WeekFields.ISO.weekOfWeekBasedYear())
        val year = today.get(WeekFields.ISO.weekBasedYear())
        return LocalReviewSummary(
            label = when (period) {
                LocalReviewPeriod.Week -> "本周 · $year 年第 $week 周"
                LocalReviewPeriod.Month -> "本月 · ${today.year} 年 ${today.monthValue} 月"
            },
            favorites = current.count { it.source == LocalReviewSource.Favorite },
            acceptedActions = current.count { it.source == LocalReviewSource.AcceptedAction },
            notSuitableActions = current.count { it.source == LocalReviewSource.NotSuitableAction },
            replacedActions = current.count { it.source == LocalReviewSource.ReplacedAction }
        )
    }

    fun build(
        favorites: List<ReadingFavorite>,
        feedback: List<ActionFeedback>,
        filter: LocalReviewFilter = LocalReviewFilter.All
    ): List<LocalReviewItem> {
        val favoriteItems = if (filter == LocalReviewFilter.ActionFeedback) emptyList() else favorites.map { favorite ->
            LocalReviewItem(
                id = "favorite:${favorite.id}",
                source = LocalReviewSource.Favorite,
                title = favorite.title,
                summary = "${favoriteTypeLabel(favorite.type)} · ${favorite.body.take(180)}",
                createdAt = favorite.createdAt
            )
        }
        val feedbackItems = if (filter == LocalReviewFilter.Favorites) emptyList() else feedback.map { item ->
            LocalReviewItem(
                id = "feedback:${item.id}",
                source = when (item.kind) {
                    ActionFeedbackKind.Accepted -> LocalReviewSource.AcceptedAction
                    ActionFeedbackKind.NotSuitable -> LocalReviewSource.NotSuitableAction
                    ActionFeedbackKind.Replaced -> LocalReviewSource.ReplacedAction
                },
                title = when (item.kind) {
                    ActionFeedbackKind.Accepted -> "采纳了行动建议"
                    ActionFeedbackKind.NotSuitable -> "标记行动建议不合适"
                    ActionFeedbackKind.Replaced -> "换掉了一项行动建议"
                },
                summary = "${categoryLabel(item.category)} · ${item.candidateKey}" + item.note?.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty(),
                createdAt = item.createdAtEpochMs
            )
        }
        return (favoriteItems + feedbackItems)
            .distinctBy(LocalReviewItem::id)
            .sortedWith(compareByDescending<LocalReviewItem> { it.createdAt }.thenBy { it.id })
    }

    private fun favoriteTypeLabel(type: String): String = when (type) {
        "fortune" -> "运势收藏"
        "action" -> "行动收藏"
        "mystic" -> "玄师收藏"
        "game" -> "游戏摘要收藏"
        else -> "收藏快照"
    }

    private fun categoryLabel(category: String): String = when (category) {
        "meal", "吃什么" -> "饮食"
        "activity", "做什么" -> "行动"
        "outing", "去哪玩" -> "出行灵感"
        else -> category
    }
}
