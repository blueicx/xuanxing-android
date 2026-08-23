package com.xuanji.app.ui.western

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.data.model.WesternDailyFortune
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.ZodiacCalculator
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.InfoRow
import com.xuanji.app.ui.components.MysticGuideCard
import com.xuanji.app.ui.components.NatalWheelChart
import com.xuanji.app.ui.components.PeriodToggleRow
import com.xuanji.app.ui.components.ResultShare
import com.xuanji.app.ui.components.ScoreRow
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import com.xuanji.app.ui.components.ShareButton
import com.xuanji.app.ui.viewmodel.WesternUiState
import com.xuanji.app.ui.viewmodel.WesternViewModel
import com.xuanji.app.ui.xuanjiViewModel

@Composable
fun WesternScreen() {
    val viewModel = xuanjiViewModel { WesternViewModel(AppModule.repository) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "星座运势",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        when (val s = uiState) {
            is WesternUiState.Loading -> Text("正在推算星盘…")
            is WesternUiState.Empty -> Text("尚未设置出生信息，请先到「我的」填写。")
            is WesternUiState.Ready -> WesternContent(
                bazi = s.bazi,
                detail = s.detail,
                fortune = s.fortune,
                chart = s.chart,
                composite = s.composite,
                period = s.period,
                onPeriodChange = viewModel::setPeriod
            )
        }
        SystemExplanation("western")
    }
}

@Composable
private fun SignBlock(title: String, info: ZodiacCalculator.ZodiacInfo) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            info.symbol,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                info.sign,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "${info.element}象 · ${info.dateRange}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Spacer(Modifier.height(6.dp))
    Text(
        info.trait,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun WesternContent(
    bazi: com.xuanji.app.data.model.BaziFull?,
    detail: ZodiacCalculator.WesternDetail,
    fortune: WesternDailyFortune,
    chart: ZodiacCalculator.NatalChart,
    composite: com.xuanji.app.data.model.CompositeDailyFortune?,
    period: String,
    onPeriodChange: (String) -> Unit
) {
    val interp = ZodiacCalculator.interpretChart(chart)
    PeriodToggleRow(period, onPeriodChange)
    if (bazi != null && composite != null) MysticGuideCard(bazi, composite)
    FortuneCard {
        SectionTitle("圆盘星盘")
        Spacer(Modifier.height(8.dp))
        Text(
            "外圈为十二星座，中圈为十二宫位（自上升点 ASC 起逆时针），盘内圆点为十大行星落点；ASC/MC 为上升与天顶。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        NatalWheelChart(chart)
        Spacer(Modifier.height(10.dp))
        PlanetLegend()
    }
    FortuneCard {
        SectionTitle("本命星盘")
        Spacer(Modifier.height(12.dp))
        SignBlock("太阳星座", detail.sun)
        Spacer(Modifier.height(12.dp))
        SignBlock("上升星座", detail.rising)
        Spacer(Modifier.height(12.dp))
        SignBlock("月亮星座", detail.moon)
        Spacer(Modifier.height(8.dp))
        Text(
            detail.note,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    FortuneCard {
        SectionTitle("四轴解析 · ASC / DSC / MC / IC")
        Spacer(Modifier.height(4.dp))
        Text(
            "四轴是星盘的坐标骨架：上升(ASC)与下降(DSC)构成地平线，天顶(MC)与天底(IC)构成子午线，勾勒出你与世界互动的基本框架。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        interp.axes.forEach { ax ->
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    ax.key,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.width(40.dp)
                )
                Column {
                    Text(
                        "${ax.name} · ${ax.sign} ${ax.degree}°",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        ax.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
    FortuneCard {
        SectionTitle("本命星盘 · 十大行星")
        Spacer(Modifier.height(4.dp))
        Text(
            "ASC ${chart.ascendant.toInt()}° · MC ${chart.midheaven.toInt()}°",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        chart.planets.forEach { p ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 3.dp)
            ) {
                Text(
                    p.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.width(28.dp)
                )
                Text(
                    "${p.name}　${p.sign} ${p.degreeInSign}°　第 ${p.house} 宫",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
    FortuneCard {
        SectionTitle("行星落座落宫含义")
        Spacer(Modifier.height(8.dp))
        interp.planets.forEach { pm ->
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    pm.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.width(28.dp)
                )
                Column {
                    Text(
                        "${pm.name} · ${pm.sign} 第 ${pm.house} 宫",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        pm.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
    FortuneCard {
        SectionTitle("主要相位")
        Spacer(Modifier.height(8.dp))
        if (chart.aspects.isEmpty()) {
            Text("未检测到主要相位。", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            chart.aspects.forEach { a ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 3.dp)
                ) {
                    Text(
                        "${a.p1} ${a.type} ${a.p2}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "（${"%.1f".format(a.orb)}°）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    FortuneCard {
        SectionTitle("相位文字解读")
        Spacer(Modifier.height(8.dp))
        if (interp.aspects.isEmpty()) {
            Text(
                "未检测到主要相位。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            interp.aspects.forEach { am ->
                Column(Modifier.padding(vertical = 4.dp)) {
                    Text(
                        "${am.p1} ${am.type} ${am.p2}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        am.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
    WesternConclusionSection(ZodiacCalculator.computeConclusion(detail, chart))
    FortuneCard {
        val periodTitle = when (period) {
            "week" -> "本周"
            "month" -> "本月"
            else -> "今日"
        }
        SectionTitle("${periodTitle}运势 · ${fortune.dateKey}") {
            ShareButton(
                sharedText = ResultShare.fortuneTitle("星座运势", period, fortune.overallScore)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(fortune.summary, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(12.dp))
        ScoreRow("综合", fortune.overallScore)
        ScoreRow("事业", fortune.careerScore)
        ScoreRow("财运", fortune.wealthScore)
        ScoreRow("感情", fortune.loveScore)
        ScoreRow("健康", fortune.healthScore)
        Spacer(Modifier.height(8.dp))
        InfoRow("幸运数字", "${fortune.luckyNumber}")
        InfoRow("幸运色", fortune.luckyColor)
        InfoRow("吉利方位", fortune.luckyDirection)
    }
}

private val PLANET_LEGEND = listOf(
    "日" to "太阳", "月" to "月亮", "水" to "水星", "金" to "金星",
    "火" to "火星", "木" to "木星", "土" to "土星", "天" to "天王",
    "海" to "海王", "冥" to "冥王", "北" to "北交"
)

@Composable
private fun PlanetLegend() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PLANET_LEGEND.chunked(4).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                row.forEach { (s, full) ->
                    Text(
                        "$s·$full",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun WesternConclusionSection(c: ZodiacCalculator.WesternConclusion) {
    FortuneCard {
        SectionTitle("综合解读")
        Spacer(Modifier.height(8.dp))
        Text(
            c.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(14.dp))
        c.items.forEach { item ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.icon, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    item.headline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    item.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (item.tags.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        item.tags.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { t ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            t,
                                            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}
