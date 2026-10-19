package com.xuanji.app.data.local

import com.xuanji.app.domain.LocalBackupSelection

enum class LocalBackupPartition {
    Favorites, Feedback, Games, Xiangqi, Food, Activity, Agenda, Notes, Snapshots;

    fun selected(selection: LocalBackupSelection): Boolean = when (this) {
        Favorites -> selection.favorites
        Feedback -> selection.actionFeedback
        Games -> selection.companionGames
        Xiangqi -> selection.xiangqiSave
        Food -> selection.foodPreferences
        Activity -> selection.activityPreferences
        Agenda -> selection.agenda
        Notes -> selection.agenda && selection.notes
        Snapshots -> selection.snapshots
    }
}

enum class LocalBackupDisposition { Add, Merge, Overwrite }
data class LocalBackupPreviewItem(
    val partition: LocalBackupPartition,
    val disposition: LocalBackupDisposition,
    val conflictKey: String? = null
)
data class LocalBackupPreviewStatistics(
    val added: Int, val merged: Int, val overwritten: Int, val omitted: Int
)

/** Counts records/preferences; note fields are separate selectable items, not extra agenda records. */
fun LocalBackupImportPreview.statistics(selection: LocalBackupSelection): LocalBackupPreviewStatistics {
    val selected = items.filter { it.partition.selected(selection) }
    return LocalBackupPreviewStatistics(
        selected.count { it.disposition == LocalBackupDisposition.Add },
        selected.count { it.disposition == LocalBackupDisposition.Merge },
        selected.count { it.disposition == LocalBackupDisposition.Overwrite },
        items.size - selected.size
    )
}

fun LocalBackupImportPreview.selectedConflicts(selection: LocalBackupSelection): Set<String> =
    items.filter { it.partition.selected(selection) }.mapNotNull { it.conflictKey }.toSet()
