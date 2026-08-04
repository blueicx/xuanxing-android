package com.xuanji.app.ui.divination

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.divination.MayaTzolkin
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import com.xuanji.app.ui.viewmodel.MayaTzolkinViewModel
import com.xuanji.app.ui.xuanjiViewModel

@Composable
fun MayaTzolkinScreen() {
    val viewModel = xuanjiViewModel { MayaTzolkinViewModel(AppModule.repository) }
    val today by viewModel.today.collectAsStateWithLifecycle()
    val birth by viewModel.birth.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("玛雅卓尔金历", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "260 天神圣历：卓尔金数 1-13 与 20 日名循环组合，并附 365 天 Haab 太阳历。仅供文化娱乐参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        today?.let { t ->
            FortuneCard {
                SectionTitle("今日（${java.time.LocalDate.now()}）")
                Spacer(Modifier.height(8.dp))
                TzolkinBody(t)
            }
        }
        birth?.let { b ->
            FortuneCard {
                SectionTitle("你的生日")
                Spacer(Modifier.height(8.dp))
                TzolkinBody(b)
            }
        }
        SystemExplanation("maya")
    }
}

@Composable
private fun TzolkinBody(r: MayaTzolkin.MayaResult) {
    Text(r.tzolkin.number.toString(), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
    Text("日名：${r.tzolkin.name}　${r.tzolkin.nameCn}", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(6.dp))
    Text("Haab：${r.haab.month} ${r.haab.day}", style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(4.dp))
    Text("长纪历（Long Count）：", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
    Text(
        r.longCount.label,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary
    )
    Text(
        "巴克顿·卡盾·盾·乌纳尔·金 = ${r.longCount.baktun}.${r.longCount.katun}.${r.longCount.tun}.${r.longCount.uinal}.${r.longCount.kin}（自创世累计 ${r.longCount.totalDays} 天，GMT 相关 584283）",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(4.dp))
    Text(r.dateLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    Text(r.verdict, style = MaterialTheme.typography.bodyMedium)
}
