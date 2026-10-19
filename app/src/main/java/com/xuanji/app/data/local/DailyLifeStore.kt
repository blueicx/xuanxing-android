package com.xuanji.app.data.local

import com.xuanji.app.domain.*
import com.xuanji.app.domain.action.StableScore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DailyLifeStore(private val bridge: PreferenceBridge) {
    private val changes = MutableStateFlow(0L)
    val revision = changes.asStateFlow()
    fun invalidate() { changes.value++ }
    fun storageKey(profileKey: String): String = "daily_life_" + StableScore.sha256(profileKey)

    suspend fun load(profileKey: String): DailyLifeDocument {
        require(profileKey.isNotBlank())
        return DailyLifeCodec.decode(bridge.read(storageKey(profileKey)), StableScore.sha256(profileKey))
    }

    private suspend fun update(profileKey: String, transform: (DailyLifeDocument) -> DailyLifeDocument) = mutex.withLock {
        val before = load(profileKey)
        check(before.readable) { "daily_life_unreadable" }
        val after = transform(before)
        bridge.write(storageKey(profileKey), DailyLifeCodec.encode(StableScore.sha256(profileKey), after))
        changes.value++
    }

    suspend fun upsertAgenda(profileKey: String, item: DailyAgendaItem, allowTimeConflict: Boolean = false) = update(profileKey) {
        val collisions = AgendaConflictRules.conflicts(it.agenda, item)
        require(allowTimeConflict || collisions.isEmpty()) { "agenda_time_conflict:${collisions.joinToString { collision -> collision.title }}" }
        it.copy(agenda = (it.agenda.filterNot { old -> old.id == item.id } + item).sortedWith(compareBy<DailyAgendaItem> { value -> value.dateKey }.thenBy { value -> value.time }))
    }
    suspend fun agendaConflicts(profileKey: String, item: DailyAgendaItem): List<DailyAgendaItem> =
        AgendaConflictRules.conflicts(load(profileKey).agenda, item)

    /** Validate and append the entire preview under one lock; never overwrite a saved occurrence. */
    suspend fun addAgendaBatch(profileKey: String, items: List<DailyAgendaItem>): AgendaCopyResult = mutex.withLock {
        val before = load(profileKey)
        check(before.readable) { "daily_life_unreadable" }
        require(DailyLifeCodec.valid(DailyLifeDocument(agenda = items))) { "invalid_agenda_batch" }
        val additions = items.filter { candidate -> before.agenda.none { it.id == candidate.id } }
        val after = before.copy(agenda = before.agenda + additions)
        require(DailyLifeCodec.valid(after)) { "invalid_or_oversized_agenda" }
        val scheduled = before.agenda.toMutableList()
        val conflicts = mutableListOf<DailyAgendaItem>()
        additions.forEach { candidate ->
            conflicts += AgendaConflictRules.conflicts(scheduled, candidate)
            scheduled += candidate
        }
        if (conflicts.isNotEmpty()) return@withLock AgendaCopyResult(conflicts = conflicts.distinctBy { it.id })
        if (additions.isNotEmpty()) {
            bridge.write(storageKey(profileKey), DailyLifeCodec.encode(StableScore.sha256(profileKey),
                after.copy(agenda = after.agenda.sortedWith(compareBy<DailyAgendaItem> { it.dateKey }.thenBy { it.time }))))
            changes.value++
        }
        AgendaCopyResult(copied = additions)
    }

    suspend fun moveAgenda(profileKey: String, id: String, targetDate: String, allowTimeConflict: Boolean = false) = update(profileKey) { document ->
        val current = document.agenda.firstOrNull { it.id == id } ?: error("agenda_item_missing")
        val moved = current.copy(dateKey = targetDate, completed = false)
        val collisions = AgendaConflictRules.conflicts(document.agenda, moved)
        require(allowTimeConflict || collisions.isEmpty()) { "agenda_time_conflict:${collisions.joinToString { it.title }}" }
        document.copy(agenda = document.agenda.map { if (it.id == id) moved else it }
            .sortedWith(compareBy<DailyAgendaItem> { it.dateKey }.thenBy { it.time }))
    }

    suspend fun moveAgendaDate(profileKey: String, sourceDate: String, targetDate: String): AgendaCopyResult = mutex.withLock {
        java.time.LocalDate.parse(sourceDate); java.time.LocalDate.parse(targetDate)
        val before = load(profileKey)
        check(before.readable) { "daily_life_unreadable" }
        if (sourceDate == targetDate) return@withLock AgendaCopyResult()
        val moving = before.agenda.filter { it.dateKey == sourceDate && !it.completed }
        val moved = moving.map { it.copy(dateKey = targetDate) }
        val retained = before.agenda.filterNot { old -> moving.any { it.id == old.id } }.toMutableList()
        val conflicts = mutableListOf<DailyAgendaItem>()
        moved.forEach { candidate ->
            conflicts += AgendaConflictRules.conflicts(retained, candidate)
            retained += candidate
        }
        if (conflicts.isNotEmpty()) return@withLock AgendaCopyResult(conflicts = conflicts.distinctBy { it.id })
        if (moved.isNotEmpty()) {
            val after = before.copy(agenda = retained.sortedWith(compareBy<DailyAgendaItem> { it.dateKey }.thenBy { it.time }))
            bridge.write(storageKey(profileKey), DailyLifeCodec.encode(StableScore.sha256(profileKey), after))
            changes.value++
        }
        AgendaCopyResult(copied = moved)
    }

    suspend fun copyAgendaDate(
        profileKey: String,
        sourceDate: String,
        targetDate: String,
        onlyMeals: Boolean = false
    ): AgendaCopyResult = mutex.withLock {
        val before = load(profileKey)
        check(before.readable) { "daily_life_unreadable" }
        val source = before.agenda.filter { it.dateKey == sourceDate && !it.completed }
        val result = AgendaConflictRules.copyToDate(source, targetDate, before.agenda, onlyMeals)
        if (result.copied.isNotEmpty()) {
            val after = before.copy(agenda = (before.agenda + result.copied).sortedWith(compareBy<DailyAgendaItem> { it.dateKey }.thenBy { it.time }))
            bridge.write(storageKey(profileKey), DailyLifeCodec.encode(StableScore.sha256(profileKey), after))
            changes.value++
        }
        result
    }

    suspend fun setCompleted(profileKey: String, id: String, completed: Boolean) = update(profileKey) {
        it.copy(agenda = it.agenda.map { item -> if (item.id == id) item.copy(completed = completed) else item })
    }
    suspend fun removeAgenda(profileKey: String, id: String) = update(profileKey) { it.copy(agenda = it.agenda.filterNot { item -> item.id == id }) }
    suspend fun saveSnapshot(profileKey: String, snapshot: ReadingSnapshot) = update(profileKey) {
        if (it.snapshots.any { old -> old.id == snapshot.id }) it else it.copy(snapshots = it.snapshots + snapshot)
    }
    suspend fun removeSnapshot(profileKey: String, id: String) = update(profileKey) { it.copy(snapshots = it.snapshots.filterNot { item -> item.id == id }) }
    suspend fun saveReminders(profileKey: String, preferences: ReminderPreferences) = update(profileKey) { it.copy(reminders = preferences) }
    suspend fun upsertPantryItem(profileKey: String, item: PantryItem) = update(profileKey) {
        val normalized = item.ingredient.trim()
        it.copy(pantry = (it.pantry.filterNot { old -> old.ingredient.equals(normalized, ignoreCase = true) } + item.copy(ingredient = normalized))
            .sortedBy { pantry -> pantry.ingredient.lowercase() })
    }
    suspend fun removePantryItem(profileKey: String, ingredient: String) = update(profileKey) {
        it.copy(pantry = it.pantry.filterNot { old -> old.ingredient.equals(ingredient, ignoreCase = true) })
    }
    suspend fun savePurchase(profileKey: String, purchase: ShoppingPurchase) = update(profileKey) { document ->
        val normalized = purchase.copy(ingredient = purchase.ingredient.trim())
        val old = document.shoppingPurchases.firstOrNull { it.key == normalized.key }
        require(old?.transferredToPantry != true || old == normalized) { "purchase_already_transferred" }
        require(!normalized.transferredToPantry || old?.transferredToPantry == true) { "use_explicit_transfer" }
        document.copy(shoppingPurchases = document.shoppingPurchases.filterNot { it.key == normalized.key } + normalized)
    }

    suspend fun transferPurchaseToPantry(
        profileKey: String, dateKey: String, ingredient: String, expiresOn: String? = null,
        category: String = "未分类", replaceExisting: Boolean = false
    ): Boolean {
        var transferred = false
        update(profileKey) { document ->
            val key = ShoppingPurchase(dateKey, ingredient).key
            val purchase = document.shoppingPurchases.firstOrNull { it.key == key } ?: error("purchase_missing")
            require(purchase.purchased) { "purchase_not_confirmed" }
            if (purchase.transferredToPantry) return@update document
            val existing = document.pantry.firstOrNull { PantryRules.nameKey(it.ingredient) == PantryRules.nameKey(ingredient) }
            require(existing == null || replaceExisting) { "pantry_replace_confirmation_required" }
            val item = PantryItem(purchase.ingredient, existing?.note.orEmpty(), purchase.quantity, purchase.unit, expiresOn, category)
            require(PantryRules.valid(item)) { "invalid_pantry" }
            transferred = true
            document.copy(pantry = document.pantry.filterNot { PantryRules.nameKey(it.ingredient) == PantryRules.nameKey(ingredient) } + item,
                shoppingPurchases = document.shoppingPurchases.map { if (it.key == key) it.copy(transferredToPantry = true) else it })
        }
        return transferred
    }

    suspend fun saveWeeklyDraft(profileKey: String, draft: com.xuanji.app.domain.action.WeeklyMenuDraft?) = update(profileKey) {
        it.copy(weeklyMenuDraft = draft)
    }
    suspend fun saveJournal(profileKey: String, entry: DailyJournalEntry) = update(profileKey) {
        require(DailyJournalRules.valid(entry)) { "invalid_journal" }
        val entries = it.journal.filterNot { old -> old.dateKey == entry.dateKey } + entry
        require(entries.size <= DailyJournalRules.MAX_ENTRIES) { "journal_capacity" }
        it.copy(journal = entries.sortedBy(DailyJournalEntry::dateKey))
    }
    suspend fun deleteJournal(profileKey: String, dateKey: String) = update(profileKey) {
        it.copy(journal = it.journal.filterNot { row -> row.dateKey == dateKey })
    }
    suspend fun replace(profileKey: String, document: DailyLifeDocument) = mutex.withLock {
        bridge.write(storageKey(profileKey), DailyLifeCodec.encode(StableScore.sha256(profileKey), document))
        changes.value++
    }
    suspend fun clear(profileKey: String) = mutex.withLock { bridge.delete(storageKey(profileKey)); changes.value++ }
    suspend fun clearSections(profileKey: String, agenda: Boolean = false, notes: Boolean = false, snapshots: Boolean = false, reminders: Boolean = false, pantry: Boolean = false,
        purchases: Boolean = false, weeklyDraft: Boolean = false, journal: Boolean = false) = update(profileKey) {
        it.copy(agenda = if (agenda) emptyList() else if (notes) it.agenda.map { item -> item.copy(note = "") } else it.agenda,
            snapshots = if (snapshots) emptyList() else it.snapshots,
            reminders = if (reminders) ReminderPreferences() else it.reminders,
            pantry = if (pantry) emptyList() else if (notes) it.pantry.map { item -> item.copy(note = "") } else it.pantry,
            shoppingPurchases = if (agenda || purchases) emptyList() else it.shoppingPurchases,
            weeklyMenuDraft = if (agenda || weeklyDraft) null else it.weeklyMenuDraft,
            journal = if (notes || journal) emptyList() else it.journal)
    }
    companion object { private val mutex = Mutex() }
}
