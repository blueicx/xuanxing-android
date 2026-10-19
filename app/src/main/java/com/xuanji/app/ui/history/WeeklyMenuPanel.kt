package com.xuanji.app.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.xuanji.app.data.local.DailyLifeStore
import com.xuanji.app.domain.action.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private val WeeklyLocksSaver = listSaver<List<WeeklyMealLock>, String>(
    save = { locks -> locks.flatMap { listOf(it.dateKey, it.slot.name, it.candidateKey) } },
    restore = { values -> runCatching {
        require(values.size % 3 == 0 && values.size <= 63)
        values.chunked(3).map { WeeklyMealLock(it[0], MealSlot.valueOf(it[1]), it[2]) }
    }.getOrDefault(emptyList()) }
)

@Composable
internal fun WeeklyMenuPanel(
    store: DailyLifeStore, profileKey: String, date: LocalDate,
    loadPool: suspend () -> List<MealSuggestion>?
) {
    var opened by rememberSaveable(profileKey) { mutableStateOf(false) }
    var pool by remember(profileKey) { mutableStateOf<List<MealSuggestion>?>(null) }
    var loading by remember(profileKey) { mutableStateOf(false) }
    var status by remember(profileKey) { mutableStateOf<String?>(null) }
    var budget by rememberSaveable(profileKey) { mutableStateOf("") }
    var variation by rememberSaveable(profileKey) { mutableIntStateOf(0) }
    var startDate by rememberSaveable(profileKey) { mutableStateOf(date.toString()) }
    var locks by rememberSaveable(profileKey, stateSaver = WeeklyLocksSaver) { mutableStateOf(emptyList<WeeklyMealLock>()) }
    var initialized by rememberSaveable(profileKey) { mutableStateOf(false) }
    var draftStorageSeen by rememberSaveable(profileKey) { mutableStateOf<String?>(null) }
    var saving by remember(profileKey) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(opened, profileKey) {
        if (opened) {
            pool = null; loading = true
            try {
                val stored = store.load(profileKey)
                if (!stored.readable) status = "本地安排数据暂不可读；不能覆盖原草稿。"
                if (!initialized || draftStorageSeen != stored.weeklyMenuDraft?.toString()) {
                    startDate = date.toString(); budget = ""; variation = 0; locks = emptyList()
                    stored.weeklyMenuDraft?.let { draft ->
                        startDate = draft.startDate; budget = draft.budgetYuan; variation = draft.variation; locks = draft.locks
                        status = "已恢复草稿条件，正在按当前饮食偏好重新核对候选；不会自动加入安排。"
                    }
                    draftStorageSeen = stored.weeklyMenuDraft?.toString()
                    initialized = true
                }
                pool = withContext(Dispatchers.Default) { loadPool() }
            }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { status = "候选读取失败，现有安排未改动。" }
            finally { loading = false }
        }
    }
    OutlinedButton(modifier = Modifier.testTag("weekly-menu-open"), onClick = { opened = true }) { Text("组合七日餐单") }
    if (!opened) status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    if (!opened) return
    val cents = budget.toIntOrNull()?.takeIf { it in 0..100000 }?.times(100)
    val validBudget = budget.isBlank() || cents != null
    val first = runCatching { LocalDate.parse(startDate) }.getOrNull()
    val plan = remember(pool, locks, cents, variation, profileKey, first) {
        if (first == null) null else pool?.let { WeeklyMealPlanner.plan(profileKey, first, it, locks, cents, variation) }
    }
    AlertDialog(onDismissRequest = { if (!saving) opened = false }, title = { Text("七日餐单 · $startDate 起") }, text = {
        Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("每人每天三餐。复用当前档案的饮食过滤与五行/星座文化分数，再平衡重复与食材复用；不是未来七天运势预测，也不是营养配餐。")
            OutlinedTextField(startDate, { startDate = it.take(10) }, label = { Text("起始日期 YYYY-MM-DD") }, modifier = Modifier.testTag("weekly-menu-start"))
            TextButton(enabled = !saving, onClick = { startDate = date.toString(); locks = emptyList(); variation = 0 }) {
                Text("以当前日历日期开始（不删除原草稿）")
            }
            OutlinedTextField(budget, { budget = it.filter(Char::isDigit).take(6) }, label = { Text("整周每人目录预算上限（元，可留空）") }, modifier = Modifier.testTag("weekly-menu-budget"))
            if (!validBudget || first == null) Text("请填写有效日期，预算留空或填 0–100000 元。", color = MaterialTheme.colorScheme.error)
            Text("价格仅为目录食材参考，非商家报价；未计人工、水电和损耗。贪心组合不保证全局最优，预算太低可能留空。", style = MaterialTheme.typography.bodySmall)
            if (loading) Text("读取真实候选中…")
            else if (pool == null) Text("当前资料不足或档案已切换，无法生成餐单。")
            plan?.let { preview ->
                Text("${preview.meals.size}/21 餐 · 已知参考约 ¥${"%.2f".format(java.util.Locale.ROOT, preview.referenceBudgetCents / 100.0)} · ${preview.unknownPrices} 餐价格未知", modifier = Modifier.testTag("weekly-menu-preview"))
                preview.problems.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
                if (preview.unfilled.isNotEmpty()) Text("${preview.unfilled.size} 餐缺少符合约束的候选；请调整预算或偏好。不会自动绕过排除和过敏过滤。")
                preview.meals.forEach { entry ->
                    val lock = WeeklyMealLock(entry.dateKey, entry.meal.slot, entry.meal.candidateKey)
                    Row(Modifier.fillMaxWidth()) {
                        Checkbox(entry.locked, onCheckedChange = { checked ->
                            locks = if (checked) locks + lock else locks.filterNot { it.dateKey == lock.dateKey && it.slot == lock.slot }
                        }, modifier = Modifier.testTag("weekly-lock-${entry.dateKey}-${entry.meal.slot}"))
                        Column(Modifier.weight(1f)) {
                            Text("${entry.dateKey} · ${when (entry.meal.slot) { MealSlot.Breakfast -> "早餐"; MealSlot.Lunch -> "午餐"; MealSlot.Dinner -> "晚餐"; else -> "饮品" }}")
                            Text(entry.meal.title)
                            Text("食材：${entry.meal.ingredients.joinToString("、")}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            TextButton(enabled = !loading && !saving, onClick = { variation = (variation + 1) % 1_000_001 }) { Text("保留锁定，重新组合其他餐食") }
            if (locks.isNotEmpty()) TextButton(enabled = !saving, onClick = { locks = emptyList() }) { Text("取消全部锁定（尚未保存更改）") }
            val draft = WeeklyMenuDraft(startDate, budget, variation, locks)
            Text("草稿只保存起始日期、预算、锁定与重排序号；下次打开会重新校验当前候选，不保存为未来盘面结论。", style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(enabled = !saving && draft.valid(), modifier = Modifier.testTag("weekly-draft-save"), onClick = {
                    scope.launch { status = runCatching { store.saveWeeklyDraft(profileKey, draft) }
                        .fold({ draftStorageSeen = draft.toString(); "草稿已保存在当前档案，尚未加入安排。" }, { "草稿保存失败，原草稿保留。" }) }
                }) { Text("保存草稿") }
                TextButton(enabled = !saving, modifier = Modifier.testTag("weekly-draft-clear"), onClick = {
                    scope.launch { status = runCatching { store.saveWeeklyDraft(profileKey, null) }.fold({
                        startDate = date.toString(); budget = ""; variation = 0; locks = emptyList(); draftStorageSeen = null
                        "草稿已清除，已保存安排不受影响。"
                    }, { "清除失败，原草稿保留。" }) }
                }) { Text("清除草稿") }
            }
            status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text("确认后加入为独立安排（08:00 / 12:00 / 18:00）；同一生成结果不重复加入，时段冲突整批不写入。")
        }
    }, confirmButton = { TextButton(
        enabled = !saving && !loading && validBudget && plan?.let { it.unfilled.isEmpty() && it.problems.isEmpty() } == true,
        modifier = Modifier.testTag("weekly-menu-save"), onClick = {
            val preview = plan ?: return@TextButton
            saving = true
            scope.launch {
                status = runCatching { store.addAgendaBatch(profileKey, preview.toAgenda()) }.fold({
                    if (it.conflicts.isNotEmpty()) "时段冲突：${it.conflicts.joinToString("、") { item -> item.title }}；未加入任何餐食。"
                    else "已加入 ${it.copied.size} 餐，原安排和已完成记录保留。"
                }, { "保存失败，原安排保留。" })
                saving = false; opened = false
            }
        }) { Text(if (saving) "保存中…" else "确认加入七日安排") }
    }, dismissButton = { TextButton(enabled = !saving, onClick = { opened = false }) { Text("取消") } })
}
