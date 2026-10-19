package com.xuanji.app.domain

import com.google.gson.JsonObject
import java.time.LocalDate
import java.util.Locale

/** Explicit inventory facts, not recipe quantities, expiry inference or a food-safety guarantee. */
object PantryRules {
    val categories = listOf("未分类", "蔬果", "主食", "蛋奶豆", "肉类水产", "调味饮品", "其他")
    fun nameKey(value: String): String = value.trim().lowercase(Locale.ROOT)
    fun validQuantity(quantity: String, unit: String): Boolean =
        unit.length <= 20 && unit.none { it.isISOControl() } &&
            (quantity.isEmpty() || quantity.matches(Regex("(?:0|[1-9][0-9]{0,6})(?:\\.[0-9]{1,3})?")) && quantity.toBigDecimal() <= 1_000_000.toBigDecimal())

    fun valid(item: PantryItem): Boolean = runCatching {
        item.ingredient.isNotBlank() && item.ingredient.length <= 100 && item.note.length <= 200 &&
            validQuantity(item.quantity, item.unit) && item.category in categories &&
            (item.expiresOn == null || LocalDate.parse(item.expiresOn).toString() == item.expiresOn)
    }.getOrDefault(false)

    fun available(item: PantryItem, date: LocalDate): Boolean =
        valid(item) && (item.expiresOn == null || !LocalDate.parse(item.expiresOn).isBefore(date)) &&
            (item.quantity.isEmpty() || item.quantity.toBigDecimal() > java.math.BigDecimal.ZERO)

    fun nearExpiry(item: PantryItem, today: LocalDate, days: Int = 3): Boolean =
        item.expiresOn?.let { !LocalDate.parse(it).isAfter(today.plusDays(days.toLong())) } ?: false

    fun reminderItems(document: DailyLifeDocument, today: LocalDate): List<PantryItem> =
        if (!document.readable || !document.reminders.enabled || !document.reminders.pantry || today.dayOfWeek.value !in document.reminders.weekdays) emptyList()
        else document.pantry.filter { item -> valid(item) && nearExpiry(item, today) &&
            (item.quantity.isBlank() || item.quantity.toBigDecimal() > java.math.BigDecimal.ZERO) }

    internal fun addLegacyDefaults(value: JsonObject) {
        if (!value.has("quantity")) value.addProperty("quantity", "")
        if (!value.has("unit")) value.addProperty("unit", "")
        if (!value.has("category")) value.addProperty("category", "未分类")
    }

    /** Old backups lacked structured fields; do not erase newer local fields during a legacy import. */
    fun preserveMetadataForLegacy(incoming: PantryItem, current: PantryItem?, backupVersion: Int): PantryItem =
        if (backupVersion >= 6 || current == null) incoming else incoming.copy(
            quantity = current.quantity, unit = current.unit, expiresOn = current.expiresOn, category = current.category
        )
}
