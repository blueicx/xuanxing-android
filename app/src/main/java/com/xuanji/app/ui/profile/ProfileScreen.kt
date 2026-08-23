package com.xuanji.app.ui.profile

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.DatePicker
import android.widget.TimePicker
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.viewmodel.ProfileViewModel
import com.xuanji.app.ui.xuanjiViewModel

@Composable
fun ProfileScreen() {
    val viewModel = xuanjiViewModel { ProfileViewModel(AppModule.repository) }
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 出生信息默认空白（由用户自行填写），避免预填他人/作者生日
    var birthYear by rememberSaveable { mutableStateOf<Int?>(null) }
    var birthMonth by rememberSaveable { mutableStateOf<Int?>(null) }
    var birthDay by rememberSaveable { mutableStateOf<Int?>(null) }
    var birthHour by rememberSaveable { mutableStateOf<Int?>(null) }
    var birthMinute by rememberSaveable { mutableStateOf<Int?>(null) }
    var location by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf<String?>(null) }
    var showClearDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(profile) {
        if (profile != null) {
            birthYear = profile?.birthYear
            birthMonth = profile?.birthMonth
            birthDay = profile?.birthDay
            birthHour = profile?.birthHour
            birthMinute = profile?.birthMinute
            location = profile?.locationName ?: ""
            gender = profile?.gender
        } else {
            birthYear = null
            birthMonth = null
            birthDay = null
            birthHour = null
            birthMinute = null
            location = ""
            gender = null
        }
    }

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
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("出生地点") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
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
                if (location.trim().isEmpty()) {
                    Toast.makeText(context, "请填写出生地点", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                viewModel.save(y, m, d, h, min, location.trim(), gender ?: "男")
                Toast.makeText(context, context.getString(R.string.saved_toast), Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存命盘")
        }
        FortuneCard {
            SectionTitle("隐私与数据")
            Spacer(Modifier.height(8.dp))
            Text(
                "玄星不联网、不登录、不上传或统计你的个人数据。出生档案和测试记录只保存在这台设备上，可随时清除。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
