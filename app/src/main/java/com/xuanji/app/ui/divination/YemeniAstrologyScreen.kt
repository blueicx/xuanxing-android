package com.xuanji.app.ui.divination

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.divination.YemeniAstrology
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import java.time.LocalDate

@Composable
fun YemeniAstrologyScreen() {
    val profile by AppModule.repository.userProfileFlow.collectAsStateWithLifecycle(initialValue = null)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("也门占星", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "也门占星（南阿拉伯星学）将黄道均等划分为十二个 30° 星宫，以春分点为白羊宫起点。因岁差，星宫日期与天文星座日期存在差异。本页以太阳黄经（Jean Meeus 近似）确定性推算，仅供文化娱乐参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val p = profile
        if (p == null) {
            FortuneCard { Text("尚未设置出生信息，请先到「我的」填写。", style = MaterialTheme.typography.bodyMedium) }
        } else {
            val date = LocalDate.of(p.birthYear, p.birthMonth, p.birthDay)
            val r = YemeniAstrology.calculate(date)
            FortuneCard {
                SectionTitle("本命星宫 · ${r.sign} ${r.symbol}")
                Spacer(Modifier.height(8.dp))
                Text("太阳黄经：${"%.2f".format(r.sunLongitude)}°", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text("元素：${r.element}　特质：${r.quality}　阴阳：${r.polarity}", style = MaterialTheme.typography.bodyMedium)
                Text("附庸星：${r.subRuler}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text("【性格】${r.personality}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Text("【命运】${r.fate}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text(r.verdict, style = MaterialTheme.typography.bodyMedium)
            }
        }

        FortuneCard {
            SectionTitle("十二星宫一览")
            Spacer(Modifier.height(8.dp))
            YemeniAstrology.signTable().forEach { s ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Text("${s.sign} ${s.symbol}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(100.dp))
                    Text("${s.element}·${s.quality}·${s.polarity}·${s.subRuler}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        SystemExplanation("yemen")
    }
}
