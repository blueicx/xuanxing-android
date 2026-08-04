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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.divination.Mahabote
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import java.time.LocalDate

@Composable
fun MahaboteScreen() {
    val profile by AppModule.repository.userProfileFlow.collectAsStateWithLifecycle(initialValue = null)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("缅甸黄道带 · 玛哈图法", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "缅甸传统以出生日星期定「主星」，对应八种动物象征（狮子、虎、大象、鼠、天竺鼠、龙）与方位、元素；主星再排入玛哈图七宫方阵断人生领域。本页基于确定性算法离线计算，仅供文化娱乐参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val p = profile
        if (p == null) {
            FortuneCard {
                Text("尚未设置出生信息，请先到「我的」填写。", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            val birth = LocalDate.of(p.birthYear, p.birthMonth, p.birthDay)
            val prof = Mahabote.profile(birth, p.birthHour)
            FortuneCard {
                SectionTitle("本命主星 · ${prof.planet}")
                Spacer(Modifier.height(8.dp))
                Text(
                    "出生：${prof.birthDate.year}年${prof.birthDate.monthValue}月${prof.birthDate.dayOfMonth}日（${prof.weekday}" +
                        (if (prof.isWednesdayPm) "下午" else "") + "）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text("动物象征：${prof.animal}", style = MaterialTheme.typography.bodyMedium)
                Text("方位：${prof.direction}　元素：${prof.element}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text(prof.meaning, style = MaterialTheme.typography.bodyMedium)
            }

            val square = Mahabote.houseSquare(birth, p.birthHour)
            FortuneCard {
                SectionTitle("玛哈图七宫方阵")
                Spacer(Modifier.height(8.dp))
                square.forEachIndexed { i, h ->
                    if (i > 0) Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            h.house,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(88.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(h.meaning, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${h.planet}（${h.animal} · ${h.direction} · ${h.element}）",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        SystemExplanation("myanmar")
    }
}
