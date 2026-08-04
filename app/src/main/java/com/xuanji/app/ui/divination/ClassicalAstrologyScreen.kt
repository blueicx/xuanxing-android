package com.xuanji.app.ui.divination

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.divination.ClassicalAstrology
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import com.xuanji.app.ui.viewmodel.ClassicalAstrologyViewModel
import com.xuanji.app.ui.xuanjiViewModel

@Composable
fun ClassicalAstrologyScreen(initialTradition: String = "all") {
    val viewModel = xuanjiViewModel { ClassicalAstrologyViewModel(AppModule.repository) }
    val result by viewModel.result.collectAsStateWithLifecycle()
    val framework by viewModel.framework.collectAsStateWithLifecycle()
    val hasProfile by viewModel.hasProfile.collectAsStateWithLifecycle()

    val singleTradition = initialTradition != "all"
    val fixedFramework = when (initialTradition) {
        "greek" -> "希腊"
        "persia" -> "波斯"
        "babylon" -> "巴比伦"
        else -> "希腊"
    }
    val showFramework = if (singleTradition) fixedFramework else framework

    val introText = if (singleTradition) {
        when (fixedFramework) {
            "希腊" -> "基于你的本命星盘，演示希腊古典框架：十度区间（Decan）、行星庙旺落陷、幸运点（Part of Fortune）。"
            "波斯" -> "基于你的本命星盘，演示波斯（阿拉伯点）古典框架：各类 Persian Parts 的位置推演。"
            else -> "基于你的本命星盘，演示巴比伦星象预兆（Omen）古典框架。"
        }
    } else {
        "同一张本命星盘下，演示三种古典框架：希腊（十度区间·庙旺落陷·幸运点）、波斯（阿拉伯点）、巴比伦（星象预兆）。"
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            if (singleTradition) "古典占星 · $fixedFramework" else "古典占星",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            introText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SystemExplanation("classical")

        if (!hasProfile) {
            Text("尚未设置出生信息，请先在「我的」中填写生日。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            return@Column
        }

        if (!singleTradition) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("希腊", "波斯", "巴比伦").forEach { f ->
                    FilterChip(selected = framework == f, onClick = { viewModel.setFramework(f) }, label = { Text(f) })
                }
            }
        }

        result?.let { res ->
            FortuneCard {
                SectionTitle("六维综合解读")
                Spacer(Modifier.height(8.dp))
                Text(res.verdict, style = MaterialTheme.typography.bodyMedium)
            }
            when (showFramework) {
                "希腊" -> GreekView(res.greek)
                "波斯" -> PersianView(res.persian)
                "巴比伦" -> BabylonianView(res.babylonian)
            }
            FortuneCard {
                SectionTitle("说明")
                Spacer(Modifier.height(8.dp))
                Text(res.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun GreekView(g: ClassicalAstrology.GreekResult) {
    FortuneCard {
        SectionTitle("十度区间（Decan）")
        Spacer(Modifier.height(8.dp))
        Text("太阳：${g.sunDecan}", style = MaterialTheme.typography.bodyMedium)
        Text("上升：${g.ascDecan}", style = MaterialTheme.typography.bodyMedium)
    }
    FortuneCard {
        SectionTitle("行星庙旺落陷")
        Spacer(Modifier.height(8.dp))
        g.dignities.forEach {
            Text("${it.planet} 在 ${it.sign}：${it.status}", style = MaterialTheme.typography.bodyMedium)
        }
    }
    FortuneCard {
        SectionTitle("幸运点（Part of Fortune）")
        Spacer(Modifier.height(8.dp))
        Text(g.partOfFortune, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PersianView(p: ClassicalAstrology.PersianResult) {
    FortuneCard {
        SectionTitle("阿拉伯点（Persian Parts）")
        Spacer(Modifier.height(8.dp))
        p.parts.forEach {
            Text("${it.name}：${it.pos}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun BabylonianView(b: ClassicalAstrology.BabylonianResult) {
    FortuneCard {
        SectionTitle("星象预兆（Omen）")
        Spacer(Modifier.height(8.dp))
        b.omens.forEach {
            Text("· $it", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
