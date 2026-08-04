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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xuanji.app.ui.components.AnimalTestIcon
import com.xuanji.app.ui.components.BigFiveTestIcon
import com.xuanji.app.ui.components.CattellTestIcon
import com.xuanji.app.ui.components.CharacterTestIcon
import com.xuanji.app.ui.components.ColorPsychTestIcon
import com.xuanji.app.ui.components.ColorTestIcon
import com.xuanji.app.ui.components.CowHorseTestIcon
import com.xuanji.app.ui.components.DiscTestIcon
import com.xuanji.app.ui.components.EnneagramTestIcon
import com.xuanji.app.ui.components.FbtiTestIcon
import com.xuanji.app.ui.components.FoodTestIcon
import com.xuanji.app.ui.components.HogwartsTestIcon
import com.xuanji.app.ui.components.HollandTestIcon
import com.xuanji.app.ui.components.Love16TestIcon
import com.xuanji.app.ui.components.LoveManualTestIcon
import com.xuanji.app.ui.components.MbtiTestIcon
import com.xuanji.app.ui.components.MmpiTestIcon
import com.xuanji.app.ui.components.PhilTestIcon
import com.xuanji.app.ui.components.RavenTestIcon
import com.xuanji.app.ui.components.SbtiTestIcon

/** 测试 tab：入口列表，点击进入具体测试。支持带 initialSubTest 直达合集内子测试。 */
@Composable
fun TestHubScreen() {
    var current by remember { mutableStateOf<TestEntry?>(null) }
    val entry = current
    when (entry) {
        null -> TestList(onPick = { current = it })
        is TestEntry.Mbti -> TestShell("MBTI 职业性格测试", onBack = { current = null }) { MbtiScreen() }
        is TestEntry.BigFive -> TestShell("大五人格测试", onBack = { current = null }) { BigFiveScreen() }
        is TestEntry.Cattell16 -> TestShell("卡特尔 16PF 人格测试", onBack = { current = null }) { Cattell16Screen() }
        is TestEntry.Mmpi -> TestShell("MMPI 心理测试", onBack = { current = null }) { MmpiScreen() }
        is TestEntry.Holland -> TestShell("霍兰德职业兴趣测试", onBack = { current = null }) { HollandScreen() }
        is TestEntry.Hogwarts -> TestShell("霍格沃茨学院测试", onBack = { current = null }) { HogwartsScreen() }
        is TestEntry.FunSub -> TestShell("性格 · 趣味合集", onBack = { current = null }) { FunTestsScreen(initialSubTest = entry.sub) }
        is TestEntry.Fun2Sub -> TestShell("趣味合集 2", onBack = { current = null }) { FunTests2Screen(initialSubTest = entry.sub) }
        is TestEntry.Fun3Sub -> TestShell("趣味 · 能力合集", onBack = { current = null }) { FunTests3Screen(initialSubTest = entry.sub) }
    }
}

/** 测试入口：独立测试或合集内的子测试（sub 为空表示进合集菜单） */
private sealed class TestEntry {
    data object Mbti : TestEntry()
    data object BigFive : TestEntry()
    data object Cattell16 : TestEntry()
    data object Mmpi : TestEntry()
    data object Holland : TestEntry()
    data object Hogwarts : TestEntry()
    data class FunSub(val sub: String) : TestEntry()
    data class Fun2Sub(val sub: String) : TestEntry()
    data class Fun3Sub(val sub: String) : TestEntry()
}

/** 给每个测试一个带返回头的容器（顶部返回 + 下方测试内容）。 */
@Composable
private fun TestShell(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.primary)
            }
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        }
        content()
    }
}

@Composable
private fun TestList(onPick: (TestEntry) -> Unit) {
    // 分组折叠状态（默认全部收纳，点击展开）
    var careerOpen by remember { mutableStateOf(false) }
    var personalityOpen by remember { mutableStateOf(false) }
    var funOpen by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("心理测试", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "精选经典心理学与能力测评，帮助你更了解自己的性格、职业倾向与潜能。所有测试离线进行，结果仅供自我探索参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TestRecordsSection()

        // —— 职业 / 能力 ——
        CollapsibleGroup("职业 · 能力", "职业性格、兴趣与认知能力测评", careerOpen, { careerOpen = !careerOpen }) {
            TestCard("MBTI 职业性格测试", "40 题 · 16 种人格类型", "了解你的人格类型与适合的职业方向，含 16 型详解。",
                MbtiTestIcon, onPick = { onPick(TestEntry.Mbti) })
            TestCard("卡特尔 16PF 人格测试", "34 题 · 16 种人格因素", "从乐群性、聪慧性到紧张性等 16 个因素全面描绘人格特质，每个因素含六维解读。",
                CattellTestIcon, onPick = { onPick(TestEntry.Cattell16) })
            TestCard("霍兰德职业兴趣测试", "60 题 · 六型 RIASEC", "从现实、研究、艺术、社会、企业、常规六种职业兴趣认识你的偏好，生成三码与综合建议。",
                HollandTestIcon, onPick = { onPick(TestEntry.Holland) })
            TestCard("瑞文标准推理测验", "12 题 · 图形推理", "经典的非言语推理能力测评，通过图形逻辑题评估你的抽象推理与问题解决能力。",
                RavenTestIcon, onPick = { onPick(TestEntry.Fun3Sub("Raven")) })
        }

        // —— 性格 · 心理 ——
        CollapsibleGroup("性格 · 心理", "人格特质、心理倾向与内在动机", personalityOpen, { personalityOpen = !personalityOpen }) {
            TestCard("大五人格测试（Big Five）", "50 题 · 五大维度（OCEAN）", "从开放性、尽责性、外向性、宜人性、神经质五个维度全面认识自己，含超详细解读。",
                BigFiveTestIcon, onPick = { onPick(TestEntry.BigFive) })
            TestCard("MMPI 心理测试", "20 题 · 14 个量表", "包含效度量表与临床量表，评估你的心理健康倾向，T 分仅供参考。",
                MmpiTestIcon, onPick = { onPick(TestEntry.Mmpi) })
            TestCard("九型人格", "10 题 · 9 种类型", "经典九型人格测评，识别驱动你行为的核心动机与性格模式。",
                EnneagramTestIcon, onPick = { onPick(TestEntry.FunSub("Enneagram")) })
            TestCard("DISC 行为风格", "10 题 · 4 种风格", "DISC 行为风格测评，分析你在职场与社交中的行为倾向。",
                DiscTestIcon, onPick = { onPick(TestEntry.FunSub("DISC")) })
            TestCard("性格色彩（FPA）", "10 题 · 4 种颜色", "FPA 性格色彩测评，从力量、热情、平和、理性四个维度刻画性格。",
                ColorTestIcon, onPick = { onPick(TestEntry.FunSub("Color")) })
            TestCard("菲尔人格", "10 题 · 4 种类型", "经典菲尔人格测试，按总分分档评估你的性格底色与处事基调。",
                PhilTestIcon, onPick = { onPick(TestEntry.Fun2Sub("Phil")) })
            TestCard("颜色心理测试", "6 题 · 12 种颜色", "基于颜色偏好投射心理倾向，辅助了解你的情绪与性格底色。",
                ColorPsychTestIcon, onPick = { onPick(TestEntry.Fun3Sub("ColorPsych")) })
        }

        // —— 趣味 ——
        CollapsibleGroup("趣味", "轻松主题小测，娱乐为主", funOpen, { funOpen = !funOpen }) {
            TestCard("霍格沃茨学院测试", "40 题 · 四大学院", "根据你的价值取向，由分院帽判断你属于格兰芬多、拉文克劳、赫奇帕奇还是斯莱特林，含学院深度解读。",
                HogwartsTestIcon, onPick = { onPick(TestEntry.Hogwarts) })
            TestCard("SBTI 处事风格", "10 题 · 4 种类型", "以躺平、进取、表达、纠结四类处事风格，反映你的生活与工作姿态。",
                SbtiTestIcon, onPick = { onPick(TestEntry.FunSub("SBTI")) })
            TestCard("牛马浓度", "10 题 · 4 种浓度", "以职场状态视角，趣味评估你的工作节奏与压力应对倾向。",
                CowHorseTestIcon, onPick = { onPick(TestEntry.Fun2Sub("CowHorse")) })
            TestCard("恋爱16型", "12 题 · 16 种类型", "从四个维度分析你在亲密关系中的相处模式与情感倾向。",
                Love16TestIcon, onPick = { onPick(TestEntry.Fun2Sub("Love16")) })
            TestCard("恋爱说明书", "10 题 · 4 种动物系", "通过意象对照，呈现你在亲密关系中的互动方式与需求表达。",
                LoveManualTestIcon, onPick = { onPick(TestEntry.Fun2Sub("LoveManual")) })
            TestCard("动物人格测试", "6 题 · 7 种动物", "通过情境选择映射七种动物意象，反映你的行事风格与内在特质。",
                AnimalTestIcon, onPick = { onPick(TestEntry.Fun3Sub("Animal")) })
            TestCard("美食水果人格", "6 题 · 15 种美食", "以美食偏好为引，关联你在社交、决策与自我认知上的倾向。",
                FoodTestIcon, onPick = { onPick(TestEntry.Fun3Sub("Food")) })
            TestCard("影视动漫角色", "6 题 · 6 位角色", "通过角色原型对照，观察你的价值取向与行为模式。",
                CharacterTestIcon, onPick = { onPick(TestEntry.Fun3Sub("Character")) })
            TestCard("FBTI 美食 MBTI", "8 题 · 16 种人格", "以美食意象类比 MBTI 四维度，评估你的性格维度偏好。",
                FbtiTestIcon, onPick = { onPick(TestEntry.Fun3Sub("FBTI")) })
        }
    }
}

/** 可折叠分组：标题 + 副标 + ���开/收起箭头，点击切换 */
@Composable
private fun CollapsibleGroup(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(Modifier.padding(top = 4.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                if (expanded) "收起 ⌃" else "展开 ⌄",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun TestCard(title: String, badge: String, desc: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onPick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(badge, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(4.dp))
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
