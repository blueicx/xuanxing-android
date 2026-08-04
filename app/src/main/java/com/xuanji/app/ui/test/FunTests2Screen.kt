package com.xuanji.app.ui.test

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.test.Fun2Result
import com.xuanji.app.domain.test.FunTests2
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle

/** 页面状态：菜单 / 某个子测试作答中 / 某个子测试结果 */
private sealed class Fun2Stage {
    object Menu : Fun2Stage()
    data class Taking(val subTest: String) : Fun2Stage()
    data class Result(val result: Fun2Result) : Fun2Stage()
}

/** 趣味人格测试合集 2：选择页 → 逐题作答 → 结果页。支持 initialSubTest 直接进入指定子测试。 */
@Composable
fun FunTests2Screen(initialSubTest: String? = null) {
    var stage by remember { mutableStateOf<Fun2Stage>(if (initialSubTest != null) Fun2Stage.Taking(initialSubTest) else Fun2Stage.Menu) }
    when (val s = stage) {
        is Fun2Stage.Menu -> Fun2Menu(onPick = { stage = Fun2Stage.Taking(it) })
        is Fun2Stage.Taking -> key(s.subTest) {
            Fun2TakingScreen(
                subTest = s.subTest,
                onBackToMenu = { stage = Fun2Stage.Menu },
                onFinished = { stage = Fun2Stage.Result(it) }
            )
        }
        is Fun2Stage.Result -> Fun2ResultPage(
            result = s.result,
            onRestart = { stage = Fun2Stage.Taking(s.result.subTest) },
            onBackToMenu = { stage = Fun2Stage.Menu }
        )
    }
}

@Composable
private fun Fun2Menu(onPick: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("趣味人格测试合集 2", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "第二弹轻松小测试：菲尔人格、牛马浓度、恋爱16型、恋爱说明书。别太较真，测完图个乐，全离线进行。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FunTests2.SUBTESTS.forEach { info ->
            Fun2TestCard(info.title, info.badge, info.desc, onPick = { onPick(info.id) })
        }
    }
}

@Composable
private fun Fun2TestCard(title: String, badge: String, desc: String, onPick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(badge, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(4.dp))
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Fun2TakingScreen(subTest: String, onBackToMenu: () -> Unit, onFinished: (Fun2Result) -> Unit) {
    val questions = FunTests2.questionsOf(subTest)
    var index by remember { mutableIntStateOf(0) }   // 0=说明, 1..N=答题
    val answers = remember { mutableStateListOf<String>() }
    var pick by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when {
            index == 0 -> Fun2Intro(subTest, onStart = { index = 1; answers.clear(); pick = null }, onBackToMenu = onBackToMenu)
            index in 1..questions.size -> {
                val q = questions[index - 1]
                LinearProgressIndicator(
                    progress = { index.toFloat() / questions.size },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("第 $index / ${questions.size} 题", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                FortuneCard {
                    Text(q.text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(12.dp))
                    q.options.forEach { (letter, label) ->
                        val selected = pick == letter
                        val border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                        OutlinedButton(onClick = { pick = letter }, modifier = Modifier.fillMaxWidth(), border = border) {
                            Text("$letter · $label")
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (index > 1) {
                        OutlinedButton(onClick = {
                            index--
                            pick = if (answers.isNotEmpty()) answers.removeAt(answers.size - 1) else null
                        }, modifier = Modifier.weight(1f)) { Text("上一题") }
                    }
                    Button(onClick = {
                        if (pick == null) return@Button
                        answers.add(pick!!)
                        pick = null
                        if (index == questions.size) {
                            onFinished(FunTests2.calculate(subTest, answers.toList()))
                        } else {
                            index++
                        }
                    }, modifier = Modifier.weight(1f)) {
                        Text(if (index == questions.size) "查看结果" else "下一题")
                    }
                }
            }
        }
    }
}

@Composable
private fun Fun2Intro(subTest: String, onStart: () -> Unit, onBackToMenu: () -> Unit) {
    val info = FunTests2.SUBTESTS.first { it.id == subTest }
    val count = FunTests2.questionsOf(subTest).size
    FortuneCard {
        SectionTitle(info.title)
        Spacer(Modifier.height(8.dp))
        Text(info.desc, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            "共 ${count} 题，凭第一感觉作答，全程约 2 分钟。结果仅供娱乐参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("开始测试（${count} 题）") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBackToMenu, modifier = Modifier.fillMaxWidth()) { Text("返回测试列表") }
    }
}

@Composable
private fun Fun2ResultPage(result: Fun2Result, onRestart: () -> Unit, onBackToMenu: () -> Unit) {
    // 菲尔为经典性格测评 → 性格类；牛马/恋爱为趣味
    val category = if (result.subTest == "Phil") "性格" else "趣味"
    androidx.compose.runtime.LaunchedEffect(Unit) {
        TestRecordRecorder.save("趣味2·${result.subTest}", category, result.name, result.interpretation.take(20), "Fun2|${result.subTest}|${result.code}")
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FortuneCard {
            SectionTitle("${result.name} · 结果")
            Spacer(Modifier.height(8.dp))
            Text("你的结果是：${result.name}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            when (result.subTest) {
                "Phil", "CowHorse" -> {
                    val total = result.scores["总分"] ?: 0
                    Text("总分：${total} 分", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary)
                    LinearProgressIndicator(progress = { total / 40f }, modifier = Modifier.fillMaxWidth())
                }
                "Love16" -> {
                    FunTests2.love16Dims().forEach { (a, b) ->
                        val sa = result.scores[a] ?: 0
                        val sb = result.scores[b] ?: 0
                        val dominant = if (sa >= sb) a else b
                        Text("$a $sa 分 · $b $sb 分 → 倾向 $dominant", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary)
                    }
                }
                else -> {
                    FunTests2.loveManualOrder().forEach { animal ->
                        val score = result.scores[animal] ?: 0
                        Text("${animal}系 · ${score} 分", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary)
                        LinearProgressIndicator(progress = { score / 10f }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
        FortuneCard {
            SectionTitle("类型解读")
            Spacer(Modifier.height(8.dp))
            Text(result.interpretation, style = MaterialTheme.typography.bodyMedium)
        }
        OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text("重新测试") }
        OutlinedButton(onClick = onBackToMenu, modifier = Modifier.fillMaxWidth()) { Text("返回测试列表") }
    }
}
