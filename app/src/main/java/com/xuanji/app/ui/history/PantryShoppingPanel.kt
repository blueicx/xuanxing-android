package com.xuanji.app.ui.history

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.xuanji.app.data.local.DailyLifeStore
import com.xuanji.app.domain.*
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
internal fun PantryShoppingPanel(store: DailyLifeStore, profileKey: String, document: DailyLifeDocument, dateKey: String) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var name by rememberSaveable(profileKey) { mutableStateOf("") }
    var note by rememberSaveable(profileKey) { mutableStateOf("") }
    var editStock by remember(profileKey) { mutableStateOf<PantryItem?>(null) }
    var purchaseName by rememberSaveable(profileKey, dateKey) { mutableStateOf<String?>(null) }
    var transferName by rememberSaveable(profileKey, dateKey) { mutableStateOf<String?>(null) }
    var nearOnly by rememberSaveable(profileKey) { mutableStateOf(false) }
    var status by remember(profileKey) { mutableStateOf<String?>(null) }
    fun operate(action: suspend () -> Unit) { scope.launch {
        status = runCatching { action() }.fold({ null }, { "操作未完成，原记录保留；同名食材请使用编辑，不会自动相加或换算单位。" })
    } }
    val derived = ShoppingList.from(document.agenda, dateKey, document.pantry)
    val orphaned = document.shoppingPurchases.filter { purchase -> purchase.dateKey == dateKey &&
        derived.items.none { PantryRules.nameKey(it.ingredient) == PantryRules.nameKey(purchase.ingredient) } }
        .map { ShoppingIngredient(it.ingredient, listOf("已保存采购记录（原餐食已调整）"),
            document.pantry.any { stock -> PantryRules.nameKey(stock.ingredient) == PantryRules.nameKey(it.ingredient) && PantryRules.available(stock, LocalDate.parse(dateKey)) }) }
    val shopping = ShoppingList(derived.items + orphaned)
    if (shopping.items.isNotEmpty()) {
        Text("购物清单", style = MaterialTheme.typography.titleMedium)
        shopping.items.forEach { ingredient ->
            val purchase = document.shoppingPurchases.firstOrNull { it.key == ShoppingPurchase(dateKey, ingredient.ingredient).key }
                ?: ShoppingPurchase(dateKey, ingredient.ingredient)
            Text("${ingredient.ingredient} · ${if (ingredient.inPantry) "库存已有" else "待补充"} · 用于 ${ingredient.mealTitles.joinToString("、")}")
            if (ingredient.expiredStock) Text("库存有该食材，但超过已记录期限；不计入可用标记。", style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth()) {
                Checkbox(purchase.purchased, onCheckedChange = { checked -> operate { store.savePurchase(profileKey, purchase.copy(purchased = checked)) } },
                    enabled = !purchase.transferredToPantry && document.readable, modifier = Modifier.testTag("purchase-check-${ingredient.ingredient}"))
                Column(Modifier.weight(1f)) {
                    Text(if (purchase.transferredToPantry) "已入库；之后的余量在库存编辑" else if (purchase.purchased) "已买，尚未入库" else "尚未标记已买")
                    Text(if (purchase.quantity.isBlank()) "采购数量未知" else "采购：${purchase.quantity} ${purchase.unit.ifBlank { "（未填单位）" }}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                TextButton(enabled = !purchase.transferredToPantry && document.readable, modifier = Modifier.testTag("purchase-edit-${ingredient.ingredient}"),
                    onClick = { purchaseName = ingredient.ingredient }) { Text("采购详情") }
                if (purchase.purchased && !purchase.transferredToPantry) TextButton(modifier = Modifier.testTag("purchase-transfer-${ingredient.ingredient}"),
                    onClick = { transferName = ingredient.ingredient }) { Text("确认入库") }
            }
        }
        Text("勾选已买不会改库存。入库不自动累加；菜谱没有标准重量，不推算需要多少克或库存是否足量。", style = MaterialTheme.typography.bodySmall)
    }
    Text("现有食材", style = MaterialTheme.typography.titleMedium)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(name, { name = it.take(100) }, label = { Text("食材名称") }, modifier = Modifier.weight(1f).testTag("pantry-ingredient"))
        TextButton(enabled = name.isNotBlank() && document.readable, onClick = { operate {
            require(document.pantry.none { PantryRules.nameKey(it.ingredient) == PantryRules.nameKey(name) })
            store.upsertPantryItem(profileKey, PantryItem(name, note)); name = ""; note = ""
            focusManager.clearFocus(); keyboard?.hide()
        } }) { Text("加入库存") }
    }
    OutlinedTextField(note, { note = it.take(200) }, label = { Text("库存备注") }, modifier = Modifier.fillMaxWidth())
    Row(Modifier.horizontalScroll(rememberScrollState())) {
        OutlinedButton(enabled = document.readable, onClick = { editStock = PantryItem(name.trim(), note) }) { Text("详细录入食材") }
        FilterChip(nearOnly, onClick = { nearOnly = !nearOnly }, label = { Text("仅临近／超过记录期限") }, modifier = Modifier.testTag("pantry-near-filter"))
    }
    Text("数量、单位和期限均由你填写；期限标签不是食品安全判断。库存不随完成餐单自动扣减。", style = MaterialTheme.typography.bodySmall)
    val today = LocalDate.now()
    val visible = document.pantry.filter { !nearOnly || PantryRules.nearExpiry(it, today) }
        .sortedWith(compareBy<PantryItem> { it.category }.thenBy { it.expiresOn ?: "9999" }.thenBy { it.ingredient })
    visible.forEach { item ->
        Text(item.ingredient + item.note.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty())
        Text("${item.category} · ${if (item.quantity.isBlank()) "数量未知" else item.quantity + " " + item.unit.ifBlank { "（未填单位）" }}", style = MaterialTheme.typography.bodySmall)
        Text(item.expiresOn?.let { expiry ->
            when { LocalDate.parse(expiry).isBefore(today) -> "超过记录期限：$expiry"
                PantryRules.nearExpiry(item, today) -> "临近记录期限：$expiry（3 天内）"
                else -> "记录期限：$expiry" }
        } ?: "未记录期限", style = MaterialTheme.typography.bodySmall)
        Row {
            TextButton(onClick = { editStock = item }, modifier = Modifier.testTag("pantry-edit-${item.ingredient}")) { Text("编辑库存") }
            TextButton(onClick = { operate { store.removePantryItem(profileKey, item.ingredient) } }) { Text("移除") }
        }
    }
    if (visible.isEmpty()) Text(if (nearOnly) "没有临近或超过手填期限的食材。" else "没有记录库存食材。")
    status?.let { Text(it) }
    editStock?.let { item ->
        PantryItemEditor(item, isExisting = document.pantry.any { it.ingredient == item.ingredient }, onDismiss = { editStock = null }) { changed ->
            require(document.pantry.none { PantryRules.nameKey(it.ingredient) == PantryRules.nameKey(changed.ingredient) } || document.pantry.any { it.ingredient == item.ingredient })
            store.upsertPantryItem(profileKey, changed); editStock = null
        }
    }
    purchaseName?.let { ingredient ->
        val purchase = document.shoppingPurchases.firstOrNull { it.key == ShoppingPurchase(dateKey, ingredient).key } ?: ShoppingPurchase(dateKey, ingredient)
        var quantity by remember(purchase) { mutableStateOf(purchase.quantity) }
        var unit by remember(purchase) { mutableStateOf(purchase.unit) }
        AlertDialog(onDismissRequest = { purchaseName = null }, title = { Text("采购 · $ingredient") }, text = { Column {
            QuantityFields(quantity, { quantity = it }, unit, { unit = it }, "purchase")
            Text("留空表示数量未知；保存详情不会自动标已买或改库存。")
        } }, confirmButton = { TextButton(enabled = PantryRules.validQuantity(quantity, unit), modifier = Modifier.testTag("purchase-save"), onClick = { operate {
            store.savePurchase(profileKey, purchase.copy(quantity = quantity, unit = unit)); purchaseName = null
        } }) { Text("保存采购详情") } }, dismissButton = { TextButton(onClick = { purchaseName = null }) { Text("取消") } })
    }
    transferName?.let { ingredient ->
        val existing = document.pantry.firstOrNull { PantryRules.nameKey(it.ingredient) == PantryRules.nameKey(ingredient) }
        var expiry by remember(ingredient) { mutableStateOf("") }
        var category by remember(ingredient) { mutableStateOf("未分类") }
        var replace by remember(ingredient) { mutableStateOf(false) }
        AlertDialog(onDismissRequest = { transferName = null }, title = { Text("确认入库 · $ingredient") }, text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                Text("使用采购详情里的数量和单位；不做克数换算，不自动相加。")
                ExpiryField(expiry) { expiry = it }
                CategoryField(category) { category = it }
                if (existing != null) Row { Checkbox(replace, { replace = it }); Text("以本次采购数量替换同名库存（保留原备注）") }
            }
        }, confirmButton = { TextButton(enabled = validExpiry(expiry) && (existing == null || replace), modifier = Modifier.testTag("pantry-transfer-confirm"), onClick = { operate {
            store.transferPurchaseToPantry(profileKey, dateKey, ingredient, expiry.takeIf(String::isNotBlank), category, replace); transferName = null
        } }) { Text("确认写入库存") } }, dismissButton = { TextButton(onClick = { transferName = null }) { Text("取消") } })
    }
}

@Composable
private fun PantryItemEditor(item: PantryItem, isExisting: Boolean, onDismiss: () -> Unit, onSave: suspend (PantryItem) -> Unit) {
    var name by remember(item) { mutableStateOf(item.ingredient) }
    var note by remember(item) { mutableStateOf(item.note) }
    var quantity by remember(item) { mutableStateOf(item.quantity) }
    var unit by remember(item) { mutableStateOf(item.unit) }
    var expiry by remember(item) { mutableStateOf(item.expiresOn.orEmpty()) }
    var category by remember(item) { mutableStateOf(item.category) }
    var error by remember(item) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val value = PantryItem(name.trim(), note, quantity, unit, expiry.takeIf(String::isNotBlank), category)
    AlertDialog(onDismissRequest = onDismiss, title = { Text("${if (isExisting) "编辑" else "录入"}库存") }, text = {
        Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(name, { name = it.take(100) }, enabled = !isExisting, label = { Text("食材名称") })
            QuantityFields(quantity, { quantity = it }, unit, { unit = it }, "pantry")
            ExpiryField(expiry) { expiry = it }
            CategoryField(category) { category = it }
            OutlinedTextField(note, { note = it.take(200) }, label = { Text("备注") })
            Text("数量可留空；最多 3 位小数。不自动推断保质期或营养用量。", style = MaterialTheme.typography.bodySmall)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(enabled = PantryRules.valid(value), modifier = Modifier.testTag("pantry-save"), onClick = {
        scope.launch { runCatching { onSave(value) }.onFailure { error = "保存未完成，同名食材请编辑已有条目；原记录保留。" } }
    }) { Text("保存库存") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

@Composable
private fun QuantityFields(quantity: String, onQuantity: (String) -> Unit, unit: String, onUnit: (String) -> Unit, prefix: String) {
    OutlinedTextField(quantity, { onQuantity(it.take(12)) }, label = { Text("数量（可留空）") }, modifier = Modifier.testTag("$prefix-quantity"))
    OutlinedTextField(unit, { onUnit(it.take(20)) }, label = { Text("单位（如个／克／包，可留空）") }, modifier = Modifier.testTag("$prefix-unit"))
}

private fun validExpiry(value: String): Boolean = value.isBlank() || runCatching { LocalDate.parse(value).toString() == value }.getOrDefault(false)

@Composable
private fun ExpiryField(value: String, onValue: (String) -> Unit) {
    OutlinedTextField(value, { onValue(it.take(10)) }, label = { Text("手填期限 YYYY-MM-DD，可留空") }, modifier = Modifier.testTag("pantry-expiry"))
}

@Composable
private fun CategoryField(value: String, onValue: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState())) {
        PantryRules.categories.forEach { category -> FilterChip(category == value, onClick = { onValue(category) }, label = { Text(category) }) }
    }
}
