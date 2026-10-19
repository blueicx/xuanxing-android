package com.xuanji.app.data.local

import com.xuanji.app.domain.LocalReviewFilter
import com.xuanji.app.domain.LocalReviewSnapshot
import com.xuanji.app.domain.LocalReviewTimeline
import com.xuanji.app.domain.ReadingFavoriteDataState

/** Reads only the current profile's explicit favorite snapshots and action feedback. */
class LocalReviewTimelineRepository(
    private val favoriteStore: ReadingFavoriteStore,
    private val feedbackStore: ActionFeedbackDataStore
) {
    suspend fun load(
        favoriteProfileKey: String?,
        actionProfileKey: String?,
        filter: LocalReviewFilter = LocalReviewFilter.All
    ): LocalReviewSnapshot {
        val favorites = favoriteProfileKey?.takeIf(String::isNotBlank)?.let { key ->
            runCatching { favoriteStore.list(key) }.getOrNull()
        }
        val feedbackResult = actionProfileKey?.takeIf(String::isNotBlank)?.let { key ->
            runCatching { feedbackStore.list(key, 100) }
        }
        val favoriteSnapshot = favorites
        val usableFavorites = favoriteSnapshot?.entries.orEmpty().takeIf {
            favoriteSnapshot?.state in setOf(ReadingFavoriteDataState.Ready, ReadingFavoriteDataState.Migrated, ReadingFavoriteDataState.Missing)
        }.orEmpty()
        val feedback = feedbackResult?.getOrNull().orEmpty()
        return LocalReviewSnapshot(
            items = LocalReviewTimeline.build(usableFavorites, feedback, filter),
            favoriteState = favoriteSnapshot?.state ?: ReadingFavoriteDataState.InvalidProfile,
            feedbackAvailable = feedbackResult == null || feedbackResult.isSuccess
        )
    }
}
