package com.xuanji.app.ui.divination

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.divination.*
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle

@Composable
fun RuneLearningPanel(reading: RuneReading?) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    FortuneCard {
        SectionTitle("符文学习与位置对照")
        Text(DivinationLearning.SCOPE, style = MaterialTheme.typography.bodySmall)
        Text("${DivinationLearning.VERSION} · 24 个名称与字形沿用项目 Elder Futhark 目录；下列正逆位为本项目现代自编评论，不是古代经文或唯一流派。", style = MaterialTheme.typography.bodySmall)
        reading?.let {
            DivinationLearning.runePositions(it).forEachIndexed { index, line ->
                Text(line, Modifier.testTag("rune-position-$index"))
                Spacer(Modifier.height(8.dp))
            }
            Text("练习：分别记录根因、当下、方向的现实证据；没有证据时写“不符合”，不把符文当现实决策依据。")
            com.xuanji.app.ui.components.SaveReadingSnapshotAction(com.xuanji.app.domain.MysticDetailContext(
                "卢恩", "draw", it.query.dateKey, "符文位置与复盘", DivinationLearning.runePositions(it).mapIndexed { index, line -> "$line\n${it.draws[index].result.verdict}" }.joinToString("\n\n"),
                "${DivinationLearning.SCOPE}\n确定性种子 ${it.seed}", DivinationLearning.SCOPE, DivinationLearning.VERSION
            ))
        }
        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.testTag("rune-learning-toggle")) {
            Text(if (expanded) "收起 24 符目录" else "展开 24 符目录")
        }
        if (expanded) Rune.learningCatalog().forEach { rune ->
            Text("${rune.symbol} ${rune.name}", style = MaterialTheme.typography.titleSmall)
            Text("项目正位：${rune.upright}\n项目逆位：${rune.reversed}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }
    }
}
