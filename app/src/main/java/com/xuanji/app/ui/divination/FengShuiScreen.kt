package com.xuanji.app.ui.divination

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Slider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.cos
import kotlin.math.sin
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.divination.FengShui
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import com.xuanji.app.ui.viewmodel.FengShuiViewModel
import com.xuanji.app.ui.xuanjiViewModel

@Composable
fun FengShuiScreen() {
    val viewModel = xuanjiViewModel { FengShuiViewModel(AppModule.repository) }
    val result by viewModel.result.collectAsStateWithLifecycle()
    val zuoShan by viewModel.zuoShan.collectAsStateWithLifecycle()
    val hasProfile by viewModel.hasProfile.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("八宅风水", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "依出生年与性别推命卦（东四/西四命），再以坐山为伏位排八游星吉凶方位。仅供文化娱乐参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        CompassCard()

        SystemExplanation("fengshui")

        if (!hasProfile) {
            Text("尚未设置出生信息，请先在「我的」中填写生日。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            return@Column
        }

        Text("选择坐山（你的住宅朝向）", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FengShui.ZUO_SHAN.forEach { g ->
                FilterChip(
                    onClick = { viewModel.setZuoShan(g) },
                    label = { Text("坐${g}向${opposite(g)}") },
                    selected = zuoShan == g
                )
            }
        }

        result?.let { res ->
            FortuneCard {
                SectionTitle("命卦与宅命")
                Spacer(Modifier.height(8.dp))
                Text("命卦：${res.mingGua}（${res.dongXi}）　坐山：坐${res.zuoShan}向${opposite(res.zuoShan)}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Text(res.match, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }
            FortuneCard {
                SectionTitle("八方吉凶")
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    res.palaces.forEach { p ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .width(72.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (p.lucky) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer)
                                    .padding(6.dp)
                            ) {
                                Text(p.dir, style = MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text("${p.gua}方 · ${p.star}", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.width(8.dp))
                            Text(if (p.lucky) "吉" else "凶", style = MaterialTheme.typography.labelMedium, color = if (p.lucky) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            FortuneCard {
                SectionTitle("六维解读")
                Spacer(Modifier.height(8.dp))
                Text(
                    res.verdict,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun opposite(g: String): String = mapOf(
    "坎" to "离", "离" to "坎", "震" to "兑", "兑" to "震",
    "乾" to "巽", "巽" to "乾", "艮" to "坤", "坤" to "艮"
).getValue(g)

@Composable
private fun CompassCard() {
    val context = LocalContext.current
    var azimuth by remember { mutableStateOf(0f) }
    var hasSensor by remember { mutableStateOf(true) }
    var manual by remember { mutableStateOf(0f) }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_ORIENTATION)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                azimuth = e.values[0]
                hasSensor = true
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (sensor != null) {
            sm!!.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            hasSensor = false
        }
        onDispose { sm?.unregisterListener(listener) }
    }

    val heading = if (hasSensor) azimuth else manual
    val r = 78.dp

    FortuneCard {
        SectionTitle("指南针 / 罗盘")
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(184.dp)) {
                val c = size.minDimension / 2f
                val center = Offset(c, c)
                drawCircle(color = Color.LightGray, radius = c, style = Stroke(2.dp.toPx()))
                drawCircle(color = Color.LightGray, radius = c * 0.66f, style = Stroke(1.dp.toPx()))
                val h = Math.toRadians(heading.toDouble())
                val L = c - 18.dp.toPx()
                val dx = (L * sin(h)).toFloat()
                val dy = (L * cos(h)).toFloat()
                drawLine(
                    color = Color.Red,
                    start = center,
                    end = Offset(center.x - dx, center.y - dy),
                    strokeWidth = 4.dp.toPx()
                )
                drawLine(
                    color = Color.Gray,
                    start = center,
                    end = Offset(center.x + dx, center.y + dy),
                    strokeWidth = 4.dp.toPx()
                )
                drawCircle(color = Color.DarkGray, radius = 4.dp.toPx(), center = center)
            }
            val dirs = listOf("北", "东北", "东", "东南", "南", "西南", "西", "西北")
            dirs.forEachIndexed { i, d ->
                val ang = Math.toRadians((i * 45).toDouble())
                val dx = (r.value * sin(ang)).dp
                val dy = (-r.value * cos(ang)).dp
                Text(
                    d,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.absoluteOffset(x = dx, y = dy)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        if (!hasSensor) {
            Text(
                "设备未提供方向传感器，可手动旋转查看：",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(value = manual, onValueChange = { manual = it }, valueRange = 0f..360f)
            Text("手动朝向：北偏 ${manual.toInt()}°", style = MaterialTheme.typography.bodySmall)
        } else {
            Text(
                "当前朝向：北偏 ${azimuth.toInt()}°（设备顶部指向）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
