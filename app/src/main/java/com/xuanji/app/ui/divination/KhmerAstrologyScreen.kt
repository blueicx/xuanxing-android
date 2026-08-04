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
import com.xuanji.app.domain.divination.KhmerAstrology
import com.xuanji.app.domain.divination.KhmerProfile
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import java.time.LocalDate

@Composable
fun KhmerAstrologyScreen() {
    val profile by AppModule.repository.userProfileFlow.collectAsStateWithLifecycle(initialValue = null)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("高棉占星", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "柬埔寨高棉传统以 Chhankitek 农历推算生肖（十二兽）、纪元（Sak）与星期主星，用于了解个人性格与每日宜忌。本页基于确定性算法离线计算，仅供文化娱乐参考。",
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
            val k = KhmerAstrology.profile(birth)
            ProfileSection(k)
            DailySection(k)
            ZodiacTableSection()
        }

        SystemExplanation("khmer")
    }
}

@Composable
private fun ProfileSection(k: KhmerProfile) {
    FortuneCard {
        SectionTitle("本命档案 · ${k.zodiac.take(4)}")
        Spacer(Modifier.height(8.dp))
        Text(
            "高棉历：${k.khmerYear}年 ${k.khmerMonth}月 ${k.khmerDay}日${if (k.isLeapMonth) "（闰月）" else ""}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Text("生肖：${k.zodiac}", style = MaterialTheme.typography.bodyMedium)
        Text("纪元：${k.sak}", style = MaterialTheme.typography.bodyMedium)
        Text("星期主星：${k.planet}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Text("【生肖解读】", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(4.dp))
        Text("性格：${k.zodiacInfo.personality}", style = MaterialTheme.typography.bodyMedium)
        Text("优点：${k.zodiacInfo.strength}", style = MaterialTheme.typography.bodyMedium)
        Text("缺点：${k.zodiacInfo.weakness}", style = MaterialTheme.typography.bodyMedium)
        Text("事业：${k.zodiacInfo.career}", style = MaterialTheme.typography.bodyMedium)
        Text("爱情：${k.zodiacInfo.love}", style = MaterialTheme.typography.bodyMedium)
        Text("幸运色：${k.zodiacInfo.luckyColor}　幸运数字：${k.zodiacInfo.luckyNumber}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Text("【主星解读】", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(4.dp))
        Text("元素：${k.planetAttr.element}　吉凶：${k.planetAttr.luck}", style = MaterialTheme.typography.bodyMedium)
        Text("特质：${k.planetAttr.trait}", style = MaterialTheme.typography.bodyMedium)
        Text("象征：${k.planetAttr.symbol}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Text("【六维综合解读】", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(4.dp))
        Text(k.verdict, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DailySection(k: KhmerProfile) {
    FortuneCard {
        SectionTitle("今日 · ${k.weekday}（${k.planet.take(4)}）")
        Spacer(Modifier.height(8.dp))
        Text(k.dailyFortune.text, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        Text("宜：${k.dailyFortune.favorable}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Text("忌：${k.dailyFortune.avoid}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        Text("纪元·${k.sak}：${k.sakMeaning}", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ZodiacTableSection() {
    FortuneCard {
        SectionTitle("高棉十二生肖 · 性格一览")
        Spacer(Modifier.height(8.dp))
        KhmerAstrology.zodiacTable().chunked(2).forEach { row ->
            androidx.compose.foundation.layout.Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
            ) {
                row.forEach { (name, personality) ->
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Text(personality, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
