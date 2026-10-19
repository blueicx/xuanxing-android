package com.xuanji.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.action.ActivityPreference
import com.xuanji.app.domain.action.EnergyLevel

@Composable
fun ActivityPreferenceEditor(
    preference: ActivityPreference,
    unreadable: Boolean,
    onSave: (ActivityPreference) -> Unit,
    onClear: () -> Unit
) {
    var energy by remember(preference) { mutableStateOf(preference.energy) }
    var indoorOnly by remember(preference) { mutableStateOf(preference.indoorOnly) }
    var duration by remember(preference) { mutableStateOf(preference.maxDurationMinutes?.toString().orEmpty()) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("活动偏好（仅本机过滤今日行动候选，不作性格推断）")
        if (unreadable) Text("已保存偏好无法读取；当前按默认值计算，保存后会替换损坏偏好。")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                EnergyLevel.Any to "不限",
                EnergyLevel.Low to "轻松",
                EnergyLevel.Medium to "适中",
                EnergyLevel.High to "充足"
            ).forEach { (level, label) ->
                FilterChip(
                    selected = energy == level,
                    onClick = { energy = level },
                    modifier = Modifier.widthIn(min = 48.dp),
                    label = { Text(label) }
                )
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = indoorOnly,
                onCheckedChange = { indoorOnly = it },
                modifier = Modifier.testTag("activity-preference-indoor")
            )
            Text("只推荐室内活动")
        }
        OutlinedTextField(
            value = duration,
            onValueChange = { duration = it.filter(Char::isDigit).take(3) },
            modifier = Modifier.fillMaxWidth().testTag("activity-preference-duration"),
            label = { Text("单次活动最长时长（分钟，可留空）") },
            supportingText = { Text("保存范围 5–480 分钟；这些约束只影响活动候选。") },
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                onSave(ActivityPreference(energy, indoorOnly, duration.toIntOrNull()))
            }) { Text("保存活动偏好") }
            Button(onClick = onClear) { Text("清除活动偏好") }
        }
    }
}
