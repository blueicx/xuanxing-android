package com.xuanji.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.data.model.HistoryEvent
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.SameDayBirth
import com.xuanji.app.ui.viewmodel.HistoryViewModel
import com.xuanji.app.ui.xuanjiViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen() {
    val viewModel = xuanjiViewModel { HistoryViewModel(AppModule.historyRepository) }
    val events by viewModel.events.collectAsStateWithLifecycle()
    val profile by AppModule.repository.userProfileFlow.collectAsStateWithLifecycle(initialValue = null)
    val today = LocalDate.now()
    val todayStr = today.format(DateTimeFormatter.ofPattern("M月d日"))

    // 同月同日生：以「用户出生日期」为准（若已设置），否则退化为当天日期并提示
    val birthDate = profile?.let {
        try {
            LocalDate.of(it.birthYear, it.birthMonth, it.birthDay)
        } catch (_: Exception) { null }
    }
    val sameDayDate = birthDate ?: today
    val sameDayStr = sameDayDate.format(DateTimeFormatter.ofPattern("M月d日"))

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "历史上的今天",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "$todayStr · 岁月长河中的玄学与人文印记",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (events.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "📭",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        "今天暂无收录的历史事件",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "$todayStr 这一天尚未收录专属的玄学与人文印记，日后会继续补充。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            events.forEach { EventCard(it) }
        }

        // 与你同月同日生（以你的出生日期为准）
        Spacer(Modifier.height(4.dp))
        Text(
            "同月同日生",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            if (birthDate != null)
                "$sameDayStr 出生的名人 · 与你（出生日）同月同日生"
            else
                "$sameDayStr 出生的名人 · 暂未设置出生日期，暂按今日展示（请到「我的」填写）",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SameDayBirth.forToday(sameDayDate).forEach { SameDayBirthCard(it) }
    }
}

@Composable
private fun SameDayBirthCard(fig: SameDayBirth.Figure) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "${fig.name} · ${fig.title}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                fig.note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "出生 · ${fig.date}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun EventCard(event: HistoryEvent) {
    // 事件自身的真实月日（"MM-dd" → "M月d日"），避免旧版「日期被写死」的 Bug 复发
    val dateStr = run {
        val parts = event.date.split("-")
        if (parts.size == 2) "${parts[0].toInt()}月${parts[1].toInt()}日" else event.date
    }
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                dateStr,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                event.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                event.desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                buildString {
                    event.year?.let { append("${it} · ") }
                    append(event.tag)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
