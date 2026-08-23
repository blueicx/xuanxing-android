package com.xuanji.app.ui.profile

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.DatePicker
import android.widget.TimePicker
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.R
import com.xuanji.app.daily.ReminderScheduler
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.ChinaLocations
import com.xuanji.app.domain.SelectedLocation
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.viewmodel.ProfileViewModel
import com.xuanji.app.ui.xuanjiViewModel

@Composable
fun ProfileScreen() {
    val viewModel = xuanjiViewModel { ProfileViewModel(AppModule.repository) }
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locations = remember { ChinaLocations.load(context) }

    // 出生信息默认空白（由用户自行填写），避免预填他人/作者生日
    var birthYear by rememberSaveable { mutableStateOf<Int?>(null) }
    var birthMonth by rememberSaveable { mutableStateOf<Int?>(null) }
    var birthDay by rememberSaveable { mutableStateOf<Int?>(null) }
    var birthHour by rememberSaveable { mutableStateOf<Int?>(null) }
    var birthMinute by rememberSaveable { mutableStateOf<Int?>(null) }
    var provinceIndex by rememberSaveable { mutableStateOf(-1) }
    var cityIndex by rememberSaveable { mutableStateOf(-1) }
    var districtIndex by rememberSaveable { mutableStateOf(-1) }
    var locationDialog by rememberSaveable { mutableStateOf<String?>(null) }
    var gender by rememberSaveable { mutableStateOf<String?>(null) }
    var showClearDialog by rememberSaveable { mutableStateOf(false) }
    val dailyReminderOn by viewModel.dailyReminderOn.collectAsStateWithLifecycle()

    val selectedLocation = if (
        provinceIndex >= 0 && cityIndex >= 0 && districtIndex >= 0 &&
        provinceIndex < locations.provinces.size &&
        cityIndex < locations.provinces[provinceIndex].cities.size &&
        districtIndex < locations.provinces[provinceIndex].cities[cityIndex].districts.size
    ) {
        SelectedLocation(provinceIndex, cityIndex, districtIndex)
    } else null
    val currentProfile = profile
    val savedLocation = currentProfile?.let {
        ChinaLocations.find(it.locationCode) ?: ChinaLocations.findLegacyCity(it.locationName)
    }
    val profileDirty = if (currentProfile == null) {
        birthYear != null || birthMonth != null || birthDay != null ||
            birthHour != null || birthMinute != null ||
            selectedLocation != null || gender != null
    } else {
        birthYear != currentProfile.birthYear ||
            birthMonth != currentProfile.birthMonth ||
            birthDay != currentProfile.birthDay ||
            birthHour != currentProfile.birthHour ||
            birthMinute != currentProfile.birthMinute ||
            provinceIndex != (savedLocation?.provinceIndex ?: -1) ||
            cityIndex != (savedLocation?.cityIndex ?: -1) ||
            districtIndex != (savedLocation?.districtIndex ?: -1) ||
            gender != currentProfile.gender
    }

    LaunchedEffect(profile) {
        if (profile != null) {
            birthYear = profile?.birthYear
            birthMonth = profile?.birthMonth
            birthDay = profile?.birthDay
            birthHour = profile?.birthHour
            birthMinute = profile?.birthMinute
            gender = profile?.gender
            val saved = ChinaLocations.find(profile?.locationCode)
                ?: ChinaLocations.findLegacyCity(profile?.locationName)
            provinceIndex = saved?.provinceIndex ?: -1
            cityIndex = saved?.cityIndex ?: -1
            districtIndex = saved?.districtIndex ?: -1
        } else {
            birthYear = null
            birthMonth = null
            birthDay = null
            birthHour = null
            birthMinute = null
            provinceIndex = -1
            cityIndex = -1
            districtIndex = -1
            gender = null
        }
    }

    val province = locations.provinces.getOrNull(provinceIndex)
    val city = province?.cities?.getOrNull(cityIndex)
    val district = city?.districts?.getOrNull(districtIndex)
    val missingProfileFields = listOfNotNull(
        if (birthYear == null || birthMonth == null || birthDay == null) "日期" else null,
        if (birthHour == null || birthMinute == null) "时间" else null,
        if (province == null) "省份" else null,
        if (city == null) "城市" else null,
        if (district == null) "县（区）" else null
    )

    // 日期/时间选择器需要具体初值，用合理占位（仅作为打开选择器时的起点，未保存前仍是空白）
    val pickerYear = birthYear ?: 2000
    val pickerMonth = birthMonth ?: 1
    val pickerDay = birthDay ?: 1
    val pickerHour = birthHour ?: 12
    val pickerMinute = birthMinute ?: 0

    val datePicker = DatePickerDialog(
        context,
        { _: DatePicker, y: Int, m: Int, d: Int ->
            birthYear = y
            birthMonth = m + 1
            birthDay = d
        },
        pickerYear, pickerMonth - 1, pickerDay
    )
    val timePicker = TimePickerDialog(
        context,
        { _: TimePicker, h: Int, m: Int ->
            birthHour = h
            birthMinute = m
        },
        pickerHour, pickerMinute, true
    )

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "命盘设置",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        FortuneCard {
            SectionTitle("出生信息")
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { datePicker.show() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (birthYear != null && birthMonth != null && birthDay != null)
                        "出生日期：${birthYear}年${birthMonth}月${birthDay}日"
                    else "出生日期：未设置（点击选择）"
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { timePicker.show() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (birthHour != null && birthMinute != null)
                        "出生时间：${String.format("%02d:%02d", birthHour, birthMinute)}"
                    else "出生时间：未设置（点击选择）"
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { locationDialog = "province" },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("省份：${province?.name ?: "请选择"}")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { locationDialog = "city" },
                enabled = province != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("市：${city?.name ?: "请先选择省份"}")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { locationDialog = "district" },
                enabled = city != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("县（区）：${district?.name ?: "请先选择市"}")
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("性别：", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { gender = "男" }) {
                    Text("男", color = if (gender == "男") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { gender = "女" }) {
                    Text("女", color = if (gender == "女") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        Button(
            onClick = {
                val y = birthYear ?: return@Button
                val m = birthMonth ?: return@Button
                val d = birthDay ?: return@Button
                val h = birthHour ?: return@Button
                val min = birthMinute ?: return@Button
                val selected = selectedLocation
                if (selected == null) {
                    Toast.makeText(context, "请填写出生地点", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                viewModel.save(y, m, d, h, min, selected, gender)
                Toast.makeText(context, context.getString(R.string.saved_toast), Toast.LENGTH_SHORT).show()
            },
            enabled = missingProfileFields.isEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存命盘")
        }
        if (missingProfileFields.isNotEmpty()) {
            Text(
                "还需填写：${missingProfileFields.joinToString(" / ")}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FortuneCard {
            SectionTitle("当前档案")
            Spacer(Modifier.height(8.dp))
            StatusChip(
                label = when {
                    currentProfile == null && !profileDirty -> "尚未设置"
                    profileDirty -> "有未保存更改"
                    else -> "已保存"
                },
                dirty = profileDirty
            )
            ProfileInfoRow(
                label = "生日",
                value = currentProfile?.let {
                    String.format(
                        "%04d-%02d-%02d %02d:%02d",
                        it.birthYear, it.birthMonth, it.birthDay, it.birthHour, it.birthMinute
                    )
                } ?: "未设置"
            )
            ProfileInfoRow(
                label = "地点",
                value = currentProfile?.locationName ?: "未设置"
            )
            ProfileInfoRow(
                label = "性别",
                value = currentProfile?.gender ?: "未设置"
            )
            if (currentProfile != null) {
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.clearUserProfile()
                        Toast.makeText(context, "已清除", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("清除并重新设置")
                }
            }
        }
        FortuneCard {
            SectionTitle("关于玄星")
            Spacer(Modifier.height(8.dp))
            Text(
                "玄星融合东方八字与西方星座，提供每日运势、占卜抽签、心理测试与手相参考。所有命理推算仅供娱乐与自我探索，请理性看待。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "占卜抽签均为纯随机抽取，每一次结果都独一无二。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FortuneCard {
            SectionTitle("隐私与数据")
            Spacer(Modifier.height(8.dp))
            Text(
                "玄星的命理推算在本地完成，不上传或统计你的出生档案与测试记录。开启每日提醒时仅使用本地通知。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "每日运势提醒",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "每天 09:00 · 本地通知",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = dailyReminderOn,
                    onCheckedChange = { enabled ->
                        viewModel.setDailyReminderOn(enabled)
                        Toast.makeText(context, if (enabled) "已开启" else "已关闭", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { showClearDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "清除全部本地数据",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        locationDialog?.let { level ->
            AlertDialog(
                onDismissRequest = { locationDialog = null },
                confirmButton = {},
                dismissButton = {
                    Row {
                        if (level != "province") {
                            TextButton(
                                onClick = {
                                    locationDialog = if (level == "district") "city" else "province"
                                }
                            ) { Text("上一级") }
                        }
                        TextButton(onClick = { locationDialog = null }) { Text("关闭") }
                    }
                },
                title = {
                    Text(
                        when (level) {
                            "city" -> "选择市 · ${province?.name ?: ""}"
                            "district" -> "选择县（区） · ${city?.name ?: ""}"
                            else -> "选择省 / 直辖市 / 自治区"
                        }
                    )
                },
                text = {
                    LazyColumn(Modifier.height(360.dp)) {
                        if (level == "province") {
                            items(locations.provinces.size) { index ->
                                LocationOption(
                                    "${locations.provinces[index].name}（${locations.provinces[index].cities.size}市）",
                                    selected = index == provinceIndex
                                ) {
                                    provinceIndex = index
                                    cityIndex = -1
                                    districtIndex = -1
                                    locationDialog = "city"
                                }
                            }
                        } else if (level == "city" && province != null) {
                            items(province.cities.size) { index ->
                                LocationOption(
                                    "${province.cities[index].name}（${province.cities[index].districts.size}区县）",
                                    selected = index == cityIndex
                                ) {
                                    cityIndex = index
                                    districtIndex = -1
                                    locationDialog = "district"
                                }
                            }
                        } else if (level == "district" && city != null) {
                            items(city.districts.size) { index ->
                                LocationOption(
                                    city.districts[index].name,
                                    selected = index == districtIndex
                                ) {
                                    districtIndex = index
                                    locationDialog = null
                                }
                            }
                        }
                    }
                }
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "作者：吴家希（WJX）",
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "感谢使用",
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(10.dp))
        // —— 右下角赞助码（小小小，约两个图标那么大）——
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomEnd
        ) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "如果喜欢，可以赞助一下",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(
                        com.xuanji.app.R.drawable.donate_qrcode
                    ),
                    contentDescription = "微信赞赏码",
                    modifier = Modifier
                        .width(72.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        )
                )
            }
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("清除全部本地数据") },
                text = { Text("将删除本机的出生档案和心理测试记录，且无法恢复。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showClearDialog = false
                            viewModel.clearAllLocalData()
                            ReminderScheduler.cancel(context)
                            Toast.makeText(context, "已全部清除", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("清除", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@Composable
private fun LocationOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Text(
            label,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            style = if (selected) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyLarge
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    }
}

@Composable
private fun StatusChip(label: String, dirty: Boolean) {
    val shape = RoundedCornerShape(50)
    val color = if (dirty) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.tertiary
    }
    Text(
        label,
        modifier = Modifier
            .clip(shape)
            .background(color.copy(alpha = if (dirty) 0.14f else 0.12f), shape)
            .border(1.dp, color.copy(alpha = if (dirty) 0.48f else 0.34f), shape)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        color = color,
        style = MaterialTheme.typography.labelMedium
    )
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
