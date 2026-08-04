package com.xuanji.app.ui.divination

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.divination.NadiAstrology
import com.xuanji.app.domain.divination.Vedic
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.InfoRow
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation

@Composable
fun NadiAstrologyScreen() {
    val profile by AppModule.repository.userProfileFlow.collectAsStateWithLifecycle(initialValue = null)
    var fingerprint by rememberSaveable { mutableStateOf("") }
    var autoFingerprint by rememberSaveable { mutableStateOf<String?>(null) }  // 自动生成的指纹
    var autoTry by rememberSaveable { mutableStateOf(0) }                       // 换指纹的尝试序号
    var partnerFingerprint by rememberSaveable { mutableStateOf("") }

    // 根据出生日期确定性生成指纹（不依赖手动输入，离线可复现）
    val p = profile
    LaunchedEffect(p, autoTry) {
        val seed = if (p != null) "${p.birthYear}-${p.birthMonth}-${p.birthDay}-${p.gender}" else "nadi-anon"
        autoFingerprint = "nadi:$seed:$autoTry"
    }
    // 实际用于解读的指纹：手动输入优先，否则用自动生成的
    val effectiveFingerprint = fingerprint.trim().ifBlank { autoFingerprint.orEmpty() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("纳迪占星", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "源自印度泰米尔纳德邦的古老体系，以指纹索引个人「命运叶片」，结合月亮星宿（Nadi 分组）与星座推算人生领域与合婚。本页按你的出生日期自动生成一枚「命定指纹」，也可手动更换，仅供文化娱乐参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        var moonNakshatra: Int? = null
        if (p != null) {
            val res = Vedic.calculate(p)
            moonNakshatra = res.nakshatraIndex
            FortuneCard {
                SectionTitle("本命星宿（自动取出生日）")
                Spacer(Modifier.height(8.dp))
                InfoRow("月亮宿", "${res.nakshatraCn}（${res.nakshatra}）")
                InfoRow("Nadi", com.xuanji.app.domain.divination.nadiOfNakshatra(res.nakshatraIndex))
            }
        }

        FortuneCard {
            SectionTitle("命定指纹（按出生日期自动生成）")
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (autoFingerprint != null) "指纹：${autoFingerprint!!.take(20)}${if (autoFingerprint!!.length > 20) "…" else ""}"
                    else "正在生成指纹…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "换一枚",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .clickable { autoTry++ }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "也可手动输入自定义指纹：",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = fingerprint,
                onValueChange = { fingerprint = it },
                label = { Text("自定义指纹（可选）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (effectiveFingerprint.isNotBlank()) {
                val prof = NadiAstrology.profile(effectiveFingerprint, p?.gender ?: "male", moonNakshatra)
                Spacer(Modifier.height(8.dp))
                InfoRow("指纹类别", "${prof.fingerprintClass} / 108")
                InfoRow("拇指", prof.thumb)
                Spacer(Modifier.height(4.dp))
                Text(prof.fingerprintIndication, style = MaterialTheme.typography.bodyMedium)
            }
        }

        FortuneCard {
            SectionTitle("Nadi Dosha 合婚配对")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = partnerFingerprint,
                onValueChange = { partnerFingerprint = it },
                label = { Text("对方指纹标识（可选）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (moonNakshatra != null && partnerFingerprint.isNotBlank()) {
                // 对方无出生资料时，以指纹哈希推确定性虚拟月亮宿用于 Dosha 演示
                val girlNs = NadiAstrology.nakshatraFromFingerprint(partnerFingerprint)
                val dosha = NadiAstrology.nadiDosha(moonNakshatra, girlNs, null, null)
                Spacer(Modifier.height(8.dp))
                InfoRow("男方 Nadi", dosha.boyNadi ?: "未知")
                InfoRow("女方 Nadi", dosha.girlNadi ?: "未知")
                InfoRow("是否 Dosha", if (dosha.hasDosha) "是（需化解）" else if (dosha.isCancelled) "已化解" else "否")
                Spacer(Modifier.height(6.dp))
                Text("等级：${dosha.level}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text(dosha.text, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Text("建议：${dosha.advice}", style = MaterialTheme.typography.bodyMedium)
            } else if (partnerFingerprint.isNotBlank()) {
                Text(
                    "需要本命星宿信息（请先在「我的」设置出生信息）。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        FortuneCard {
            SectionTitle("16 Kandam 章节概览")
            Spacer(Modifier.height(8.dp))
            NadiAstrology.kandamOverview().forEachIndexed { i, name ->
                Text(
                    "Kandam ${i + 1}: $name",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }

        SystemExplanation("naadi")
    }
}
