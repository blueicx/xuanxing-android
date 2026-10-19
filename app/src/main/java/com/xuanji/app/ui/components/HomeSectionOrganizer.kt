package com.xuanji.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
internal fun HomeSectionOrganizer(controller: CardLayoutController) {
    var open by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { open = !open }, modifier = Modifier.testTag("home-organize")) { Text(if (open) "完成顺序调整" else "调整首页顺序") }
    if (!open) return
    val labels = mapOf("hero" to "总览", "summary" to "综合结论", "dimensions" to "六项运势", "actions" to "今日行动", "extras" to "幸运与依据")
    Column {
        controller.state.order.keys.sortedBy { controller.state.order[it] }.forEach { id ->
            Row(Modifier.fillMaxWidth()) {
                Text(labels[id].orEmpty(), Modifier.weight(1f))
                TextButton(onClick = { controller.move(id, -1) }, modifier = Modifier.testTag("home-up-$id")) { Text("上移") }
                TextButton(onClick = { controller.move(id, 1) }, modifier = Modifier.testTag("home-down-$id")) { Text("下移") }
                TextButton(onClick = { controller.moveBy(id, -5) }, modifier = Modifier.testTag("home-pin-$id")) { Text("置顶") }
            }
        }
        TextButton(onClick = { controller.reset() }) { Text("恢复默认顺序") }
        Text("只改变板块顺序，不删减内容；今日行动在周、月、年仍不显示。")
    }
}
