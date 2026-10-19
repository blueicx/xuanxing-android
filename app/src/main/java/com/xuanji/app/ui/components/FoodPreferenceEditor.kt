package com.xuanji.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import com.xuanji.app.domain.action.FoodPreference
import com.xuanji.app.domain.action.KitchenEquipment
import com.xuanji.app.domain.action.CookingDifficulty

@Composable
fun FoodPreferenceEditor(
    preference: FoodPreference,
    onSave: (FoodPreference) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    unreadable: Boolean = false
) {
    var vegetarian by remember(preference) { mutableStateOf(preference.vegetarian) }
    var halal by remember(preference) { mutableStateOf(preference.halal) }
    var avoidSpicy by remember(preference) { mutableStateOf(preference.avoidSpicy) }
    var avoidAlcohol by remember(preference) { mutableStateOf(preference.avoidAlcohol) }
    var excluded by remember(preference) { mutableStateOf(preference.excludedIngredients.joinToString("、")) }
    var allergens by remember(preference) { mutableStateOf(preference.allergens.joinToString("、")) }
    var budget by remember(preference) { mutableStateOf(preference.maxMealBudgetCents?.let { (it / 100.0).toString() }.orEmpty()) }
    var prep by remember(preference) { mutableStateOf(preference.maxPrepMinutes?.toString().orEmpty()) }
    var equipment by remember(preference) { mutableStateOf(preference.availableEquipment) }
    var easyOnly by remember(preference) { mutableStateOf(preference.maxCookingDifficulty == CookingDifficulty.Easy) }
    var lunchbox by remember(preference) { mutableStateOf(preference.lunchboxOnly) }
    var pantryFirst by remember(preference) { mutableStateOf(preference.prioritizePantry) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("饮食偏好（只用于本机候选过滤）")
        if (unreadable) Text("偏好暂不可读，已暂停饮食推荐，未用默认值覆盖过敏或其他限制。可先导出/保留原数据，或明确清除偏好后重新设置。")
        PreferenceToggle("素食", vegetarian) { vegetarian = it }
        PreferenceToggle("清真", halal) { halal = it }
        PreferenceToggle("忌辣", avoidSpicy) { avoidSpicy = it }
        PreferenceToggle("忌酒", avoidAlcohol) { avoidAlcohol = it }
        PreferenceToggle("限定可用加热设备", equipment != null) { equipment = if (it) emptySet() else null }
        if (equipment != null) {
            Text("不勾选任何设备时，仅保留无需加热的目录做法。刀具餐具不在此列。")
            KitchenEquipment.entries.forEach { item ->
                PreferenceToggle(item.label, item in equipment.orEmpty()) { checked ->
                    equipment = if (checked) equipment.orEmpty() + item else equipment.orEmpty() - item
                }
            }
        }
        PreferenceToggle("只选简单做法", easyOnly) { easyOnly = it }
        PreferenceToggle("只选便当形式", lunchbox) { lunchbox = it }
        PreferenceToggle("优先使用可用同名库存", pantryFirst) { pantryFirst = it }
        Text("设备、难度和便当用于过滤；库存只调整顺序，不改变五行/星座文化分数，不保证食材数量足够或储运安全。")
        OutlinedTextField(
            value = excluded,
            onValueChange = { excluded = it.take(240) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("不吃的食材（用顿号分隔，最多 12 项）") },
            singleLine = false
        )
        OutlinedTextField(
            value = allergens,
            onValueChange = { allergens = it.take(240) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("过敏原（用顿号分隔，最多 12 项）") },
            singleLine = false
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = budget,
                onValueChange = { budget = it.filter { char -> char.isDigit() || char == '.' }.take(8) },
                modifier = Modifier.weight(1f),
                label = { Text("单餐预算（元）") },
                singleLine = true
            )
            OutlinedTextField(
                value = prep,
                onValueChange = { prep = it.filter(Char::isDigit).take(3) },
                modifier = Modifier.weight(1f),
                label = { Text("准备上限（分钟）") },
                singleLine = true
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val items = excluded.split('、', ',', '，')
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .distinct()
                    .take(12)
                    .toSet()
                val allergyItems = allergens.split('、', ',', '，')
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .distinct()
                    .take(12)
                    .toSet()
                onSave(
                    FoodPreference(
                        vegetarian = vegetarian,
                        halal = halal,
                        avoidSpicy = avoidSpicy,
                        avoidAlcohol = avoidAlcohol,
                        excludedIngredients = items,
                        allergens = allergyItems,
                        maxMealBudgetCents = budget.toDoubleOrNull()?.let { (it * 100).toInt() },
                        maxPrepMinutes = prep.toIntOrNull(),
                        availableEquipment = equipment,
                        maxCookingDifficulty = if (easyOnly) CookingDifficulty.Easy else null,
                        lunchboxOnly = lunchbox,
                        prioritizePantry = pantryFirst
                    )
                )
            }, enabled = !unreadable) { Text("保存偏好") }
            Button(onClick = onClear) { Text("清除偏好") }
        }
    }
}

@Composable
private fun PreferenceToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(label)
    }
}
