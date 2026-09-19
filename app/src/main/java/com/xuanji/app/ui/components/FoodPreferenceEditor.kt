package com.xuanji.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.xuanji.app.domain.action.FoodPreference

@Composable
fun FoodPreferenceEditor(
    preference: FoodPreference,
    onSave: (FoodPreference) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    var vegetarian by remember(preference) { mutableStateOf(preference.vegetarian) }
    var halal by remember(preference) { mutableStateOf(preference.halal) }
    var avoidSpicy by remember(preference) { mutableStateOf(preference.avoidSpicy) }
    var avoidAlcohol by remember(preference) { mutableStateOf(preference.avoidAlcohol) }
    var excluded by remember(preference) { mutableStateOf(preference.excludedIngredients.joinToString("、")) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("饮食偏好（只用于本机候选过滤）")
        PreferenceToggle("素食", vegetarian) { vegetarian = it }
        PreferenceToggle("清真", halal) { halal = it }
        PreferenceToggle("忌辣", avoidSpicy) { avoidSpicy = it }
        PreferenceToggle("忌酒", avoidAlcohol) { avoidAlcohol = it }
        OutlinedTextField(
            value = excluded,
            onValueChange = { excluded = it.take(240) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("不吃的食材（用顿号分隔，最多 12 项）") },
            singleLine = false
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val items = excluded.split('、', ',', '，')
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .distinct()
                    .take(12)
                    .toSet()
                onSave(FoodPreference(vegetarian, halal, avoidSpicy, avoidAlcohol, items))
            }) { Text("保存偏好") }
            Button(onClick = onClear) { Text("清除偏好") }
        }
    }
}

@Composable
private fun PreferenceToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label)
    }
}
