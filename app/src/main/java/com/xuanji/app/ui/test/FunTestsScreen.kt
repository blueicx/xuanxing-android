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
import com.xuanji.app.domain.test.FunResult
import com.xuanji.app.domain.test.FunTests
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle

/** 页面状态：菜单 / 某个子测试作答中 / 某个子测试结果 */
private sealed class FunStage {
    object Menu : FunStage()
    data class Taking(val subTest: String) : FunStage()
    data class Result(val result: FunResult) : FunStage()
}

/** 趣味人格测试合集：选择页 → 逐题作答 → 结果页。支持 initialSubTest 直接进入指定子测试。 */
@Composable
fun FunTestsScreen(initialSubTest: String? = null) {
    var stage by remember { mutableStateOf<FunStage>(if (initialSubTest != null) FunStage.Taking(initialSubTest) else FunStage.Menu) }
    when (val s = stage) {
        is FunStage.Menu -> FunMenu(onPick = { stage = FunStage.Taking(it) })
        is FunStage.Taking -> key(s.subTest) {
            FunTakingScreen(
                subTest = s.subTest,
                onBackToMenu = { stage = FunStage.Menu },
                onFinished = { stage = FunStage.Result(it) }
            )
        }
        is FunStage.Result -> FunResultPage(
            result = s.result,
            onRestart = { stage = FunStage.Taking(s.result.subTest) },
            onBackToMenu = { stage = FunStage.Menu }
        )
    }
}

@Composable
private fun FunMenu(onPick: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("趣味人格测试合集", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "四个轻松有趣的性格小测试，别太较真，测完图个乐。每套 10 题，约 2 分钟，全离线进行。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FunTests.SUBTESTS.forEach { info ->
            FunTestCard(info.title, info.badge, info.desc, onPick = { onPick(info.id) })
        }
    }
}

@Composable
private fun FunTestCard(title: String, badge: String, desc: String, onPick: () -> Unit) {
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
private fun FunTakingScreen(subTest: String, onBackToMenu: () -> Unit, onFinished: (FunResult) -> Unit) {
    val questions = FunTests.questionsOf(subTest)
    var index by remember { mutableIntStateOf(0) }   // 0=说明, 1..10=答题
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
            index == 0 -> FunIntro(subTest, onStart = { index = 1; answers.clear(); pick = null }, onBackToMenu = onBackToMenu)
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
                            onFinished(FunTests.calculate(subTest, answers.toList()))
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
private fun FunIntro(subTest: String, onStart: () -> Unit, onBackToMenu: () -> Unit) {
    val info = FunTests.SUBTESTS.first { it.id == subTest }
    FortuneCard {
        SectionTitle(info.title)
        Spacer(Modifier.height(8.dp))
        Text(info.desc, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            "共 10 题，每题四个选项，凭第一感觉作答，全程约 2 分钟。结果仅供娱乐参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("开始测试（10 题）") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBackToMenu, modifier = Modifier.fillMaxWidth()) { Text("返回测试列表") }
    }
}

@Composable
private fun FunResultPage(result: FunResult, onRestart: () -> Unit, onBackToMenu: () -> Unit) {
    val info = FunTests.SUBTESTS.first { it.id == result.subTest }
    val order = FunTests.typeOrder(result.subTest)
    val names = FunTests.typeNames(result.subTest)
    // 九型/DISC/性格色彩为经典性格测评 → 性格类；SBTI 为趣味
    val category = when (result.subTest) {
        "Enneagram", "DISC", "Color" -> "性格"
        else -> "趣味"
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        TestRecordRecorder.save("趣味·${info.title}", category, result.name, result.interpretation.take(20), "Fun|${result.subTest}|${result.code}")
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FortuneCard {
            SectionTitle("${info.title} · 结果")
            Spacer(Modifier.height(8.dp))
            Text("你的主导类型：${result.name}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            order.forEach { code ->
                val score = result.scores[code] ?: 0
                Text("${names[code] ?: code} · $score 分", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary)
                LinearProgressIndicator(progress = { score / 10f }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
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
