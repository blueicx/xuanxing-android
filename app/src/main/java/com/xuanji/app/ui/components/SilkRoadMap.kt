package com.xuanji.app.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xuanji.app.domain.game.*

@Composable
internal fun SilkRoadMap(state: SilkRoadState, enabled: Boolean, fullScreen: Boolean = false, onEvent: (SilkRoadEvent) -> Unit) {
    var zoom by rememberSaveable(state.missionId) { mutableDoubleStateOf(1.0) }
    var panX by rememberSaveable(state.missionId) { mutableDoubleStateOf(0.0) }
    var panY by rememberSaveable(state.missionId) { mutableDoubleStateOf(0.0) }
    var selected by rememberSaveable(state.missionId) { mutableStateOf(state.current) }
    var panelOpen by rememberSaveable(state.missionId) { mutableStateOf(false) }
    var enlarged by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current.density
    val camera = SilkRoadCamera(zoom, panX, panY)
    val reachable = SilkRoadRouteEngine.destinations(state)
    val selectedLeg = reachable.firstOrNull { it.id == selected }
    val paint = remember(density) { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(244, 219, 172); textSize = 12 * density } }
    fun setCamera(value: SilkRoadCamera) { zoom = value.zoom; panX = value.panX; panY = value.panY }

    if (enlarged && !fullScreen) Dialog(onDismissRequest = { enlarged = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
            Column(Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                TextButton(onClick = { enlarged = false }) { Text("关闭全屏地图") }
                SilkRoadMap(state, enabled, fullScreen = true, onEvent = onEvent)
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${state.current} · 剩余 ${state.daysLeft} 天 / ${state.budgetLeft} 点补给", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { setCamera(camera.adjusted(1.3)) }, modifier = Modifier.testTag("silkroad-zoom-in")) { Text("放大") }
            TextButton(onClick = { setCamera(camera.adjusted(1 / 1.3)) }) { Text("缩小") }
            TextButton(onClick = { setCamera(SilkRoadCamera()) }, modifier = Modifier.testTag("silkroad-camera-reset")) { Text("复位") }
            if (!fullScreen) TextButton(onClick = { enlarged = true }) { Text("全屏地图") }
        }
        Canvas(Modifier.fillMaxWidth().height(if (fullScreen) 380.dp else 300.dp)
            .testTag("silkroad-map-canvas").background(Color(0xFF211B2C))
            .semantics { contentDescription = "离线丝路地图；缩放 $zoom 倍。城市按真实坐标定位，连接线为游戏示意路线；可用下方站点按钮替代触控。" }
            .onSizeChanged { canvasSize = it }
            .pointerInput(state.missionId) {
                detectTransformGestures { _, pan, scale, _ ->
                    setCamera(SilkRoadCamera(zoom, panX, panY).adjusted(scale.toDouble(), pan.x / canvasSize.width.coerceAtLeast(1).toDouble(), pan.y / canvasSize.height.coerceAtLeast(1).toDouble()))
                }
            }
            .pointerInput(state.missionId) {
                detectTapGestures { point ->
                    SilkRoadGeography.hit(SilkRoadPoint(point.x.toDouble(), point.y.toDouble()), SilkRoadCamera(zoom, panX, panY),
                        canvasSize.width.toDouble(), canvasSize.height.toDouble(), 24.0 * density)?.let { selected = it; panelOpen = true }
                }
            }) {
            fun position(id: String): Offset {
                val p = camera.toScreen(SilkRoadGeography.project(id), size.width.toDouble(), size.height.toDouble())
                return Offset(p.x.toFloat(), p.y.toFloat())
            }
            // Geographic graticule only, not fabricated national borders or historical roads.
            for (x in 1..6) drawLine(Color(0xFF4C405C), Offset(size.width * x / 7, 0f), Offset(size.width * x / 7, size.height), 1f)
            for (y in 1..3) drawLine(Color(0xFF4C405C), Offset(0f, size.height * y / 4), Offset(size.width, size.height * y / 4), 1f)
            SilkRoadGeography.edges.distinctBy { listOf(it.from, it.to).sorted() }.forEach { leg ->
                val planned = state.plannedPath.zipWithNext().any { (a, b) -> a == leg.from && b == leg.to || a == leg.to && b == leg.from }
                drawLine(if (planned) Color(0xFFF3D18D) else Color(0xFF71617F), position(leg.from), position(leg.to), if (planned) 4 * density else 1.5f * density)
            }
            SilkRoadGeography.cities.forEach { city ->
                val p = position(city.id)
                val active = city.id == state.current || city.id == selected
                drawCircle(if (active) Color(0xFFF3D18D) else if (city.id == state.target) Color(0xFFB3D8C0) else Color(0xFFAD94BD), if (active) 7 * density else 4 * density, p)
                if (active || city.id == state.target || reachable.any { it.id == city.id } || zoom >= 1.5)
                    drawContext.canvas.nativeCanvas.drawText(city.id, (p.x + 9 * density).coerceAtMost(size.width - paint.measureText(city.id)), p.y - 9 * density, paint)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("←" to (-.1 to 0.0), "→" to (.1 to 0.0), "↑" to (0.0 to -.1), "↓" to (0.0 to .1)).forEach { (label, shift) ->
                TextButton(onClick = { setCamera(camera.adjusted(dx = shift.first, dy = shift.second)) }, modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "移动地图 $label" }) { Text(label) }
            }
        }
        Text("选中：$selected${if (selectedLeg == null) " · 非当前相邻站，可通过完整路线规划前往" else " · ${selectedLeg.days}游戏天 / ${selectedLeg.cost}点补给"}", modifier = Modifier.testTag("silkroad-selected-city"))
        if (selectedLeg != null) {
            Text("城市参考点直线距离约 ${SilkRoadGeography.distanceKm(state.current, selected)} km，不是道路里程。", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { onEvent(SilkRoadEvent.Travel(selected)); panelOpen = false },
                enabled = enabled && !state.completed && !state.failed && state.daysLeft >= selectedLeg.days && state.budgetLeft >= selectedLeg.cost,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("silkroad-confirm-travel")) { Text("确认前往$selected") }
        }
        TextButton(onClick = { panelOpen = !panelOpen }) { Text(if (panelOpen) "收起行程与站点" else "展开行程与站点") }
        if (panelOpen) {
            Text("站点选择（可替代地图触控）", style = MaterialTheme.typography.titleSmall)
            SilkRoadGeography.cities.chunked(3).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { city -> TextButton(onClick = { selected = city.id }, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("silkroad-city-${city.id}")) { Text(city.id) } }
            } }
            Text("已走：${state.path.joinToString(" → ")}")
            if (state.plannedPath.isNotEmpty()) Text("已选剩余路线：${state.plannedPath.joinToString(" → ")}")
        }
        SilkRoadGeography.compareRoutes(state).forEach { plan ->
            OutlinedButton(onClick = { onEvent(SilkRoadEvent.PlanRoute(plan.path)); selected = plan.path.getOrNull(1) ?: state.current; panelOpen = true },
                enabled = enabled && plan.feasible, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("silkroad-plan-${plan.id}")) {
                Column { Text("${plan.title} · ${plan.days}游戏天 / ${plan.supplies}补给${if (plan.feasible) "" else " · 资源不足"}")
                    Text(plan.path.joinToString(" → "), style = MaterialTheme.typography.labelSmall) }
            }
        }
        Text("先比较完整路线，再逐站确认；允许改道与重新规划。方案可能重合，资源不足时不虚构可行路线。", style = MaterialTheme.typography.bodySmall)
        Text(SilkRoadGeography.ATTRIBUTION, style = MaterialTheme.typography.labelSmall)
    }
}
