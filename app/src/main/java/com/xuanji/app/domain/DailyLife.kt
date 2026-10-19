package com.xuanji.app.domain

import com.google.gson.Gson
import com.google.gson.JsonParser
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

data class DailyAgendaItem(
    val id: String, val dateKey: String, val time: String, val title: String, val category: String,
    val ingredients: List<String> = emptyList(), val candidateKey: String = "",
    val note: String = "", val completed: Boolean = false, val createdAt: Long = System.currentTimeMillis(),
    val portions: Int = 1, val estimatedPriceCentsPerPortion: Int? = null,
    /** Zero means unknown, not an invented default duration for migrated records. */
    val durationMinutes: Int = 0
)

data class PantryItem(
    val ingredient: String, val note: String = "", val quantity: String = "", val unit: String = "",
    val expiresOn: String? = null, val category: String = "未分类"
)

data class ShoppingPurchase(
    val dateKey: String, val ingredient: String, val purchased: Boolean = false,
    val quantity: String = "", val unit: String = "", val transferredToPantry: Boolean = false
) {
    val key: String get() = "$dateKey|${PantryRules.nameKey(ingredient)}"
}

data class AgendaCopyResult(
    val copied: List<DailyAgendaItem> = emptyList(),
    val conflicts: List<DailyAgendaItem> = emptyList()
)

object AgendaConflictRules {
    fun conflicts(existing: List<DailyAgendaItem>, candidate: DailyAgendaItem): List<DailyAgendaItem> =
        if (candidate.time.isBlank() || candidate.completed) emptyList() else existing.filter {
            it.id != candidate.id && !it.completed && it.time.isNotBlank() && overlaps(it, candidate)
        }

    private fun overlaps(a: DailyAgendaItem, b: DailyAgendaItem): Boolean {
        val startA = LocalDate.parse(a.dateKey).atTime(LocalTime.parse(a.time))
        val startB = LocalDate.parse(b.dateKey).atTime(LocalTime.parse(b.time))
        val endA = startA.plusMinutes(a.durationMinutes.toLong())
        val endB = startB.plusMinutes(b.durationMinutes.toLong())
        return when {
            a.durationMinutes == 0 && b.durationMinutes == 0 -> startA == startB
            a.durationMinutes == 0 -> startA >= startB && startA < endB
            b.durationMinutes == 0 -> startB >= startA && startB < endA
            else -> startA < endB && startB < endA
        }
    }

    fun copyToDate(
        items: List<DailyAgendaItem>,
        targetDate: String,
        existing: List<DailyAgendaItem>,
        onlyMeals: Boolean = false
    ): AgendaCopyResult {
        val source = items.filter { !onlyMeals || it.category == "meal" }
        val copies = source.map { it.copy(id = UUID.randomUUID().toString(), dateKey = targetDate, completed = false, createdAt = System.currentTimeMillis()) }
        val scheduled = existing.toMutableList()
        val foundConflicts = mutableListOf<DailyAgendaItem>()
        copies.forEach { candidate ->
            val collisions = conflicts(scheduled, candidate)
            if (collisions.isNotEmpty()) foundConflicts += collisions else scheduled += candidate
        }
        return if (foundConflicts.isNotEmpty()) AgendaCopyResult(conflicts = foundConflicts.distinctBy(DailyAgendaItem::id))
        else AgendaCopyResult(copied = copies)
    }
}

data class ReadingSnapshot(
    val id: String, val dateKey: String, val period: String, val mode: String,
    val body: String, val evidence: String, val algorithmVersion: String, val title: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class ShoppingIngredient(val ingredient: String, val mealTitles: List<String>, val inPantry: Boolean = false, val expiredStock: Boolean = false)
data class ShoppingList(val items: List<ShoppingIngredient>) {
    companion object {
        fun from(agenda: List<DailyAgendaItem>, dateKey: String, pantry: List<PantryItem> = emptyList()): ShoppingList = ShoppingList(
            agenda.filter { it.dateKey == dateKey && it.category == "meal" }
                .flatMap { meal -> meal.ingredients.filter(String::isNotBlank).map { it.trim() to meal.title } }
                .groupBy({ it.first }, { it.second }).toSortedMap()
                .map { (ingredient, titles) -> ShoppingIngredient(
                    ingredient, titles.distinct(), pantry.any { PantryRules.nameKey(it.ingredient) == PantryRules.nameKey(ingredient) && PantryRules.available(it, LocalDate.parse(dateKey)) },
                    pantry.any { PantryRules.nameKey(it.ingredient) == PantryRules.nameKey(ingredient) && it.expiresOn?.let { date -> LocalDate.parse(date).isBefore(LocalDate.parse(dateKey)) } == true }
                ) }
        )
    }
}

data class ReminderPreferences(
    val enabled: Boolean = false, val hour: Int = 9, val minute: Int = 0,
    val weekdays: Set<Int> = (1..7).toSet(), val agenda: Boolean = true, val savedResults: Boolean = false,
    val pantry: Boolean = false
) {
    fun nextAfter(now: LocalDateTime): LocalDateTime? {
        if (!enabled || weekdays.isEmpty() || (!agenda && !savedResults && !pantry)) return null
        return (0L..7L).map { now.toLocalDate().plusDays(it).atTime(hour, minute) }
            .firstOrNull { it.isAfter(now) && it.dayOfWeek.value in weekdays }
    }
}

data class DailyLifeDocument(
    val agenda: List<DailyAgendaItem> = emptyList(), val snapshots: List<ReadingSnapshot> = emptyList(),
    val reminders: ReminderPreferences = ReminderPreferences(), val readable: Boolean = true,
    val pantry: List<PantryItem> = emptyList(),
    val shoppingPurchases: List<ShoppingPurchase> = emptyList(),
    val weeklyMenuDraft: com.xuanji.app.domain.action.WeeklyMenuDraft? = null,
    val journal: List<DailyJournalEntry> = emptyList()
)

/** Explicit local records only. Over-limit and malformed documents are rejected without truncation. */
object DailyLifeCodec {
    const val VERSION = 6
    const val MAX_AGENDA = 500
    const val MAX_SNAPSHOTS = 200
    const val MAX_PANTRY_ITEMS = 100
    const val MAX_PURCHASES = 500
    const val MAX_BYTES = 2_000_000
    private val gson = Gson()

    fun encode(profileDigest: String, document: DailyLifeDocument): String {
        require(document.readable && valid(document)) { "invalid_daily_life" }
        val root = com.google.gson.JsonObject().apply {
            addProperty("version", VERSION); addProperty("profileDigest", profileDigest)
            add("document", gson.toJsonTree(document))
        }
        return root.toString().also { require(it.toByteArray().size <= MAX_BYTES) { "daily_life_oversized" } }
    }

    fun decode(raw: String?, profileDigest: String): DailyLifeDocument {
        if (raw == null) return DailyLifeDocument()
        return runCatching {
            require(raw.toByteArray().size <= MAX_BYTES)
            val root = JsonParser.parseString(raw).asJsonObject
            val version = root.get("version").asInt
            require(version in 1..VERSION && root.get("profileDigest").asString == profileDigest)
            val body = root.getAsJsonObject("document").deepCopy()
            if (version < 6) body.add("journal", com.google.gson.JsonArray())
            if (!body.has("pantry") || body.get("pantry").isJsonNull) body.add("pantry", com.google.gson.JsonArray())
            if (version < 5) {
                body.add("shoppingPurchases", com.google.gson.JsonArray())
                body.getAsJsonArray("pantry")?.forEach { entry -> PantryRules.addLegacyDefaults(entry.asJsonObject) }
            }
            body.getAsJsonArray("agenda")?.forEach { entry ->
                if (entry.isJsonObject && !entry.asJsonObject.has("portions")) entry.asJsonObject.addProperty("portions", 1)
            }
            val document = gson.fromJson(body, DailyLifeDocument::class.java)
            require(document.readable && valid(document))
            document
        }.getOrElse { DailyLifeDocument(readable = false) }
    }

    fun valid(document: DailyLifeDocument): Boolean = runCatching {
        require(document.agenda.size <= MAX_AGENDA && document.snapshots.size <= MAX_SNAPSHOTS)
        require(document.pantry.size <= MAX_PANTRY_ITEMS)
        require(document.shoppingPurchases.size <= MAX_PURCHASES)
        require(document.journal.size <= DailyJournalRules.MAX_ENTRIES && document.journal.all(DailyJournalRules::valid))
        require(document.journal.map { it.dateKey }.distinct().size == document.journal.size)
        require(document.agenda.map { it.id }.toSet().size == document.agenda.size)
        require(document.snapshots.map { it.id }.toSet().size == document.snapshots.size)
        document.agenda.forEach { item ->
            require(item.id.isNotBlank() && item.id.length <= 128 && item.title.isNotBlank() && item.title.length <= 200)
            LocalDate.parse(item.dateKey)
            if (item.time.isNotBlank()) LocalTime.parse(item.time)
            require(item.category in setOf("meal", "activity", "outing", "custom"))
            require(item.note.length <= 2000 && item.ingredients.size <= 50 && item.ingredients.all { it.length <= 100 })
            require(item.candidateKey.length <= 128 && item.createdAt >= 0)
            require(item.portions in 1..20)
            require(item.durationMinutes in 0..1440)
            require(item.estimatedPriceCentsPerPortion == null || item.estimatedPriceCentsPerPortion in 0..1_000_000)
            require(item.category == "meal" || item.estimatedPriceCentsPerPortion == null)
        }
        document.snapshots.forEach { snapshot ->
            require(snapshot.id.isNotBlank() && snapshot.id.length <= 128 && snapshot.title.isNotBlank())
            LocalDate.parse(snapshot.dateKey)
            require(snapshot.body.length <= 20_000 && snapshot.evidence.length <= 20_000)
            require(snapshot.period.isNotBlank() && snapshot.mode.isNotBlank() && snapshot.algorithmVersion.length <= 128)
            require(snapshot.createdAt >= 0)
        }
        require(document.reminders.hour in 0..23 && document.reminders.minute in 0..59)
        require(document.reminders.weekdays.all { it in 1..7 })
        require(document.pantry.map { PantryRules.nameKey(it.ingredient) }.toSet().size == document.pantry.size)
        require(document.pantry.all(PantryRules::valid))
        require(document.shoppingPurchases.map { it.key }.distinct().size == document.shoppingPurchases.size)
        document.shoppingPurchases.forEach {
            require(LocalDate.parse(it.dateKey).toString() == it.dateKey)
            require(it.ingredient.isNotBlank() && it.ingredient.length <= 100 && PantryRules.validQuantity(it.quantity, it.unit))
            require(!it.transferredToPantry || it.purchased)
        }
        require(document.weeklyMenuDraft?.valid() != false)
        true
    }.getOrDefault(false)
}
