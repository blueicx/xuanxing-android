package com.xuanji.app.ui.divination

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import com.xuanji.app.domain.divination.TaiYi
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.InfoRow
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import java.time.LocalDate

@Composable
fun TaiYiScreen() {
    val profile by AppModule.repository.userProfileFlow.collectAsStateWithLifecycle(initialValue = null)
    val today = LocalDate.now()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("太乙神数（八宫）", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "古代「三式」之一，此简化版以日期推八宫神煞与吉凶。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SystemExplanation("taiyi")
        val res = TaiYi.calculate(today.year, today.monthValue, today.dayOfMonth)
        FortuneCard {
            SectionTitle("今日神煞 · ${today}")
            Spacer(Modifier.height(8.dp))
            InfoRow("所落之宫", res.palace)
            InfoRow("主神", res.spirit)
            Spacer(Modifier.height(8.dp))
            Text("宫位解读：${res.palaceMeaning}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text("神煞解读：${res.spiritMeaning}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                if (res.isLucky) "综合吉凶：吉，宜积极行动。" else "综合吉凶：凶，宜谨慎守成。",
                style = MaterialTheme.typography.titleMedium,
                color = if (res.isLucky) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(8.dp))
            Text(res.verdict, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        if (profile != null) {
            val birth = TaiYi.calculate(profile!!.birthYear, profile!!.birthMonth, profile!!.birthDay)
            FortuneCard {
                SectionTitle("本命神煞 · 出生日")
                Spacer(Modifier.height(8.dp))
                InfoRow("所落之宫", birth.palace)
                InfoRow("主神", birth.spirit)
                Spacer(Modifier.height(8.dp))
                Text(birth.palaceMeaning, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text(birth.spiritMeaning, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
