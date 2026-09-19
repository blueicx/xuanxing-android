# X3 今日行动与人生画像实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 在保持离线默认和现有命盘算法不变的前提下，新增由五行 + 星座共同驱动的今日饮食/行动/出游建议，以及带置信度和证据链的人生职业、色彩、环境与地区画像。

**架构：** 新功能拆成纯 Kotlin `action` 领域模块、按 profileKey 隔离的本地偏好存储、一个组合既有命盘/运势/测试记录的仓库和两个 Compose 展示入口。评分器只接受显式输入，主结果由固定权重计算；稳定 hash 只用于同分排序。对话上下文引用同一结果对象，避免 UI、对话和文案各自维护一份事实。

**技术栈：** Kotlin 1.9/JVM 纯领域代码、Android DataStore Preferences、Kotlin Coroutines/StateFlow、Jetpack Compose Material 3、JUnit 4、现有 Node golden contract。

---

## 文件与职责锁定

**新增领域文件：**

- `app/src/main/java/com/xuanji/app/domain/action/ActionModels.kt`：今日行动、食物偏好、人生画像、证据链和置信度的数据契约。
- `app/src/main/java/com/xuanji/app/domain/action/ActionCatalog.kt`：离线食物/行动/环境候选目录；每个候选明确五行、星座元素、季节和环境标签。
- `app/src/main/java/com/xuanji/app/domain/action/StableScore.kt`：固定权重、归一化、稳定 hash 和同分排序。
- `app/src/main/java/com/xuanji/app/domain/action/DailyActionPlanner.kt`：根据命盘、星座、综合运势、季节、城市和偏好生成 `DailyActionPlan`。
- `app/src/main/java/com/xuanji/app/domain/action/LifeProfilePlanner.kt`：根据命盘、星座、已完成测试和城市目录生成 `LifeProfile`。

**新增数据/UI 文件：**

- `app/src/main/java/com/xuanji/app/data/local/FoodPreferenceStore.kt`：按 profileKey 读写饮食偏好，提供撤销和清除。
- `app/src/main/java/com/xuanji/app/data/repository/ActionRepository.kt`：组合现有 `FortuneRepository`、`TestRecordRepository`、偏好存储和评分器。
- `app/src/main/java/com/xuanji/app/ui/viewmodel/ActionViewModel.kt`：暴露今日行动、人生画像、偏好编辑状态和加载状态。
- `app/src/main/java/com/xuanji/app/ui/components/DailyActionCards.kt`：综合页三张可折叠行动卡。
- `app/src/main/java/com/xuanji/app/ui/components/LifeProfileCards.kt`：我的页职业、色彩、环境和地区候选卡。
- `app/src/main/java/com/xuanji/app/ui/components/FoodPreferenceEditor.kt`：素食、清真、忌辣、忌酒和自定义排除项编辑器。

**修改文件：**

- `app/src/main/java/com/xuanji/app/data/local/DataStoreModule.kt`：暴露偏好存储构造器。
- `app/src/main/java/com/xuanji/app/di/AppModule.kt`：注册 `ActionRepository`。
- `app/src/main/java/com/xuanji/app/ui/viewmodel/CompositeFortuneViewModel.kt`：在现有命盘状态上提供今日行动输入。
- `app/src/main/java/com/xuanji/app/ui/composite/CompositeFortuneScreen.kt`：挂载今日行动区块，不破坏现有卡片布局控制器。
- `app/src/main/java/com/xuanji/app/ui/profile/ProfileScreen.kt`：挂载人生画像、饮食偏好和本地清除入口。
- `app/src/main/java/com/xuanji/app/domain/MysticDialogueEngine.kt`、`MysticIntentClassifier.kt`、`MysticGuideGenerator.kt`：新增行动/画像意图，并复用结果对象。
- `app/src/main/java/com/xuanji/app/domain/DialogueReplyValidator.kt`：校验新结果的事实边界和迁居/营养/医疗越界文案。
- `README.md`、`docs/SYSTEMS_OVERVIEW.md`、`docs/TECHNICAL_DEBT.md`、`docs/superpowers/handoffs/2026-09-19-daily-actions-life-profile-handoff.md`：同步离线、算法、隐私和验收证据。

**测试与契约文件：**

- `app/src/test/kotlin/com/xuanji/app/domain/action/DailyActionPlannerTest.kt`
- `app/src/test/kotlin/com/xuanji/app/domain/action/LifeProfilePlannerTest.kt`
- `app/src/test/kotlin/com/xuanji/app/data/local/FoodPreferenceStoreTest.kt`
- `app/src/test/kotlin/com/xuanji/app/data/repository/ActionRepositoryTest.kt`
- `app/src/test/kotlin/com/xuanji/app/domain/MysticDialogueActionTest.kt`
- `_dev/action_profile_contract.json`
- `_dev/action_profile_contract_test.js`

---

### 任务 1：建立纯 Kotlin 契约、目录和确定性工具

**文件：**
- 创建：`app/src/main/java/com/xuanji/app/domain/action/ActionModels.kt`
- 创建：`app/src/main/java/com/xuanji/app/domain/action/ActionCatalog.kt`
- 创建：`app/src/main/java/com/xuanji/app/domain/action/StableScore.kt`
- 创建：`app/src/test/kotlin/com/xuanji/app/domain/action/DailyActionPlannerTest.kt`

- [ ] **步骤 1：编写失败的契约测试**

```kotlin
@Test
fun score_uses_fixed_weights_and_never_randomizes_the_main_result() {
    val first = StableScore.combine(100, 80, 60, 40)
    val second = StableScore.combine(100, 80, 60, 40)
    assertEquals(78, first)
    assertEquals(first, second)
    assertEquals(
        listOf("a", "b"),
        StableScore.orderTies(listOf(Candidate("a", 70), Candidate("b", 70)), "profile", "2026-09-19")
            .map { it.key }
    )
}
```

`Candidate` 在本任务中定义为 `data class Candidate(val key: String, val score: Int)`；测试首次运行应因 `StableScore` 和模型不存在而失败。

- [ ] **步骤 2：运行测试确认缺少实现**

运行：`./gradlew :app:testDebugUnitTest --tests com.xuanji.app.domain.action.DailyActionPlannerTest`

预期：编译失败，错误指向尚未创建的 `StableScore` 或 `Candidate`。

- [ ] **步骤 3：实现数据模型和稳定评分**

`ActionModels.kt` 必须包含规格中的 `DailyActionPlan`、`MealSuggestion`、`ActivitySuggestion`、`OutingSuggestion`、`ActionEvidence`、`FoodPreference`、`LifeProfile`、`CareerCluster`、`ColorCluster`、`EnvironmentProfile`、`RegionCandidate`、`CityProfile`、`Candidate` 以及 `ActionSource`、`ConfidenceLevel`、`MealSlot`。`StableScore.kt` 使用以下实现，主分数不读取时间、设备状态或随机源：

```kotlin
object StableScore {
    const val FIVE_ELEMENT_WEIGHT = 0.40
    const val ZODIAC_WEIGHT = 0.25
    const val DAILY_FORTUNE_WEIGHT = 0.20
    const val SEASON_OR_CITY_WEIGHT = 0.15

    fun combine(five: Int, zodiac: Int, fortune: Int, seasonOrCity: Int): Int =
        (five * FIVE_ELEMENT_WEIGHT + zodiac * ZODIAC_WEIGHT +
            fortune * DAILY_FORTUNE_WEIGHT + seasonOrCity * SEASON_OR_CITY_WEIGHT)
            .roundToInt().coerceIn(0, 100)

    fun tieBreak(profileKey: String, dateKey: String, candidateKey: String): Long =
        sha256("$profileKey|$dateKey|$candidateKey").take(16).toLong(16)

    fun <T : Candidate> orderTies(items: List<T>, profileKey: String, dateKey: String): List<T> =
        items.sortedWith(compareByDescending<T> { it.score }
            .thenBy { tieBreak(profileKey, dateKey, it.key) })
}
```

`ActionCatalog.kt` 至少提供 8 个具体食物（含主料、替换项和外卖关键词）、6 个行动候选、6 个环境候选和 8 个国家/城市候选。每个条目同时声明 `elementTags`、`zodiacTags`、`seasonTags`；目录不包含“保证健康”“保证升职”等绝对词。

`ActionModels.kt` 中的 `CityProfile` 固定为 `key/label/tags/country/city` 五个字段，`Candidate` 固定为 `key/score` 两个字段；`StableScore.kt` 内提供私有 UTF-8 SHA-256 函数，保证 Android/JVM 结果一致。

- [ ] **步骤 4：运行测试确认契约通过**

运行：`./gradlew :app:testDebugUnitTest --tests com.xuanji.app.domain.action.DailyActionPlannerTest`

预期：PASS，并能证明同一输入的主分数不依赖随机数。

- [ ] **步骤 5：Commit**

```bash
git add app/src/main/java/com/xuanji/app/domain/action app/src/test/kotlin/com/xuanji/app/domain/action/DailyActionPlannerTest.kt
git commit -m "feat(行动): 建立今日行动与人生画像领域契约"
```

### 任务 2：实现今日行动评分器和食物偏好过滤

**文件：**
- 修改：`app/src/main/java/com/xuanji/app/domain/action/DailyActionPlanner.kt`
- 修改：`app/src/test/kotlin/com/xuanji/app/domain/action/DailyActionPlannerTest.kt`

- [ ] **步骤 1：编写失败的评分与过滤测试**

```kotlin
@Test
fun plan_is_specific_and_exposes_both_required_sources() {
    val plan = planner.plan(input(preference = FoodPreference(vegetarian = true)))
    assertTrue(plan.meals.all { it.ingredients.isNotEmpty() && it.substitute.isNotBlank() })
    assertTrue(plan.meals.all { it.deliveryKeywords.isNotEmpty() })
    assertTrue(plan.meals.flatMap { it.evidence }.any { it.source == ActionSource.FiveElements })
    assertTrue(plan.meals.flatMap { it.evidence }.any { it.source == ActionSource.Zodiac })
}

@Test
fun excluded_ingredients_filter_every_meal_and_report_degradation() {
    val plan = planner.plan(input(preference = FoodPreference(excludedIngredients = setOf("花生", "酒"))))
    assertTrue(plan.meals.none { it.ingredients.any { ingredient -> ingredient.contains("花生") } })
    assertFalse(plan.disclaimer.contains("无法计算"))
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`./gradlew :app:testDebugUnitTest --tests com.xuanji.app.domain.action.DailyActionPlannerTest`

预期：因 `DailyActionPlanner` 和输入对象尚未实现而失败。

- [ ] **步骤 3：实现评分器**

定义以下输入契约，所有字段都由调用方显式传入：

```kotlin
data class DailyActionInput(
    val profileKey: String,
    val date: LocalDate,
    val chart: BaziChart,
    val zodiacKey: String,
    val zodiacElement: Element,
    val fortune: CompositeDailyFortune,
    val city: CityProfile?,
    val preference: FoodPreference
)

class DailyActionPlanner {
    fun plan(input: DailyActionInput): DailyActionPlan
}
```

测试文件顶部提供 `input(preference: FoodPreference = FoodPreference(), city: CityProfile? = CityProfileCatalog.defaultCity, date: LocalDate = LocalDate.of(2026, 9, 19)): DailyActionInput`，其中 `chart` 和 `fortune` 使用固定命盘/运势 fixture；每个测试只改变一个输入因素，不依赖系统时钟。

实现顺序固定为：先过滤 `FoodPreference`，再对剩余候选按 `StableScore.combine` 计算；五行匹配取 `chart.favorableElements` 与候选 `elementTags` 的交集比例，星座匹配取 `zodiacElement`/`zodiacKey` 与候选标签的交集，日运匹配读取 `fortune.dimensions` 的事业、情绪、健康分，季节或城市匹配读取 `LocalDate` 的春夏秋冬标签或 `CityProfile` 环境标签。每个结果的 `evidence` 至少包含 `FiveElements` 与 `Zodiac` 两项，食物附 `substitute` 和 `deliveryKeywords`。城市为空时只生成“环境类型”出游建议，`cityKey=null` 且不写附近地点。

- [ ] **步骤 4：运行全量领域测试**

运行：`./gradlew :app:testDebugUnitTest --tests com.xuanji.app.domain.action.DailyActionPlannerTest`

预期：PASS，且覆盖素食、清真、忌辣、忌酒、自定义排除项和无城市降级。

- [ ] **步骤 5：Commit**

```bash
git add app/src/main/java/com/xuanji/app/domain/action/DailyActionPlanner.kt app/src/test/kotlin/com/xuanji/app/domain/action/DailyActionPlannerTest.kt
git commit -m "feat(行动): 按五行星座生成具体饮食行动建议"
```

### 任务 3：实现职业、色彩和地区画像评分器

**文件：**
- 创建：`app/src/main/java/com/xuanji/app/domain/action/LifeProfilePlanner.kt`
- 创建：`app/src/test/kotlin/com/xuanji/app/domain/action/LifeProfilePlannerTest.kt`

- [ ] **步骤 1：编写失败的画像测试**

```kotlin
@Test
fun no_test_records_produce_low_confidence_and_explicit_missing_inputs() {
    val result = planner.plan(input(testRecords = emptyList()))
    assertEquals(ConfidenceLevel.Low, result.confidence)
    assertTrue(result.missingInputs.contains("职业/性格测试"))
    assertTrue(result.regionCandidates.size <= 3)
}

@Test
fun test_records_change_reasons_but_do_not_replace_chart_and_zodiac_evidence() {
    val result = planner.plan(input(testRecords = listOf(TestRecord("霍兰德", "职业", "IAS", "艺术社会研究", "2026-09-19 09:00"))))
    assertTrue(result.careerClusters.flatMap { it.reasons }.any { it.contains("霍兰德") })
    assertTrue(result.evidence.any { it.source == ActionSource.FiveElements })
    assertTrue(result.evidence.any { it.source == ActionSource.Zodiac })
}
```

- [ ] **步骤 2：运行测试确认失败**

运行：`./gradlew :app:testDebugUnitTest --tests com.xuanji.app.domain.action.LifeProfilePlannerTest`

预期：因 `LifeProfilePlanner` 尚未存在而失败。

- [ ] **步骤 3：实现固定公式和静态候选**

定义：

```kotlin
data class LifeProfileInput(
    val profileKey: String,
    val chart: BaziChart,
    val zodiacKey: String,
    val zodiacElement: Element,
    val tests: List<TestRecord>,
    val preferredCityKey: String?
)

class LifeProfilePlanner {
    fun plan(input: LifeProfileInput): LifeProfile
}
```

测试文件顶部提供固定的 `input(testRecords: List<TestRecord>, preferredCityKey: String? = "shanghai")` 和 `chartFixture()`；`chartFixture()` 的五行分布、喜用神、日主和生肖均写死，避免测试把设备日期或随机状态带入评分。

职业簇有测试时使用 `0.45*chartElementFit + 0.25*zodiacFit + 0.30*testFit`，没有测试时使用 `0.60*chartElementFit + 0.40*zodiacFit`；色彩使用 `0.60/0.25/0.15`；地区使用 `0.35/0.25/0.20/0.20`。测试只读取已保存的 `TestRecord.resultCode/resultName`，不把结果包装成心理诊断或标准常模。候选从 `ActionCatalog` 的静态列表最多取 3 个，卡片数据同时填 `matchedTags` 和 `unmatchedFactors`；输入缺少测试时设 `Low` 并加入 `missingInputs`，出生盘和星座齐全但缺少测试时不降低为无结果。

- [ ] **步骤 4：运行画像测试**

运行：`./gradlew :app:testDebugUnitTest --tests com.xuanji.app.domain.action.LifeProfilePlannerTest`

预期：PASS，验证输入变化会改变证据链/解释而非只改变颜色，同分排序稳定且候选不超过 3 个。

- [ ] **步骤 5：Commit**

```bash
git add app/src/main/java/com/xuanji/app/domain/action/LifeProfilePlanner.kt app/src/test/kotlin/com/xuanji/app/domain/action/LifeProfilePlannerTest.kt
git commit -m "feat(画像): 增加职业色彩与地区匹配"
```

### 任务 4：加入按档案隔离的本地饮食偏好

**文件：**
- 创建：`app/src/main/java/com/xuanji/app/data/local/FoodPreferenceStore.kt`
- 创建：`app/src/test/kotlin/com/xuanji/app/data/local/FoodPreferenceStoreTest.kt`
- 修改：`app/src/main/java/com/xuanji/app/data/local/DataStoreModule.kt`

- [ ] **步骤 1：编写失败的隔离与清除测试**

```kotlin
@Test
fun preferences_are_isolated_by_profile_and_clear_only_one_profile() = runTest {
    val bridge = FakePreferenceBridge()
    val store = FoodPreferenceStore(bridge)
    val vegetarian = FoodPreference(vegetarian = true, excludedIngredients = setOf("花生"))
    store.save("profile-a", vegetarian)
    store.save("profile-b", FoodPreference(halal = true))
    assertEquals(vegetarian, store.read("profile-a"))
    store.clear("profile-a")
    assertEquals(FoodPreference(), store.read("profile-a"))
    assertEquals(FoodPreference(halal = true), store.read("profile-b"))
}
```

- [ ] **步骤 2：运行测试确认失败**

运行：`./gradlew :app:testDebugUnitTest --tests com.xuanji.app.data.local.FoodPreferenceStoreTest`

预期：因 `FoodPreferenceStore` 不存在而失败。

- [ ] **步骤 3：实现 DataStore 编解码**

使用 `PreferenceBridge` 的单键方案，键名为 `food_preference_<sha256(profileKey)>`。编码采用 Gson 的稳定字段对象，`excludedIngredients` 写入排序后的数组；读取坏数据返回默认偏好并保留 `unreadable` 状态给 UI 提示。`DataStoreModule.kt` 增加：

```kotlin
fun Context.foodPreferenceStore(): FoodPreferenceStore =
    FoodPreferenceStore(DataStorePreferenceBridge(this))
```

清除只删除当前 profileKey 的键，不调用 `dataStore.edit { it.clear() }`。

- [ ] **步骤 4：运行 DataStore 测试**

运行：`./gradlew :app:testDebugUnitTest --tests com.xuanji.app.data.local.FoodPreferenceStoreTest`

预期：PASS，证明隔离、空值默认和单档案清除。

- [ ] **步骤 5：Commit**

```bash
git add app/src/main/java/com/xuanji/app/data/local/FoodPreferenceStore.kt app/src/main/java/com/xuanji/app/data/local/DataStoreModule.kt app/src/test/kotlin/com/xuanji/app/data/local/FoodPreferenceStoreTest.kt
git commit -m "feat(偏好): 持久化按档案隔离的饮食设置"
```

### 任务 5：组合数据层和 ViewModel

**文件：**
- 创建：`app/src/main/java/com/xuanji/app/data/repository/ActionRepository.kt`
- 创建：`app/src/main/java/com/xuanji/app/ui/viewmodel/ActionViewModel.kt`
- 修改：`app/src/main/java/com/xuanji/app/di/AppModule.kt`
- 修改：`app/src/main/java/com/xuanji/app/data/repository/TestRecordRepository.kt`

- [ ] **步骤 1：增加仓库输入测试替身**

在 `ActionRepository` 的纯计算入口保留可注入函数：

```kotlin
class ActionRepository(
    private val fortuneRepository: FortuneRepository,
    private val testRecords: TestRecordRepository,
    private val preferences: FoodPreferenceStore,
    private val dailyPlanner: DailyActionPlanner = DailyActionPlanner(),
    private val lifePlanner: LifeProfilePlanner = LifeProfilePlanner()
) {
    suspend fun loadToday(date: LocalDate): DailyActionPlan?
    suspend fun loadLifeProfile(): LifeProfile?
    suspend fun saveFoodPreference(profileKey: String, preference: FoodPreference)
    suspend fun clearFoodPreference(profileKey: String)
}
```

`ActionRepositoryTest.kt` 使用一个内存 `FakeActionSource` 返回固定 `UserProfile`、`BaziFull`、`ZodiacInfo`、`CompositeDailyFortune` 和测试记录，先锁定 `null` 档案、缺少城市、空测试记录三种状态；不得在 UI 中直接读取 `StateFlow.value` 代替仓库输入。

- [ ] **步骤 2：实现仓库和 StateFlow**

`loadToday` 从 `userProfileState`、`baziFullFlow`、`natalChartFlow` 读取档案，调用 `getCompositeFortune(..., LocalDate, "day")`，再把用户选择的区县映射到静态 `CityProfile`；找不到映射时传 `null`。`loadLifeProfile` 读取最新最多一条相关 `TestRecord`，同时保留测试类别和原始结果码。`ActionViewModel` 暴露 `StateFlow<ActionUiState>`，并在 `viewModelScope` 的 `Dispatchers.Default` 计算，保存偏好后重新加载当日计划。

`AppModule.init` 增加 `ActionRepository` 参数并在 `XuanjiApplication` 初始化时传入；现有调用方保留默认行为，应用仍然离线启动。

- [ ] **步骤 3：运行编译与仓库测试**

运行：`./gradlew :app:testDebugUnitTest --tests com.xuanji.app.data.repository.ActionRepositoryTest :app:compileDebugKotlin`

预期：PASS 且 `compileDebugKotlin` 成功；无 GPS、网络或新的外部服务依赖。

- [ ] **步骤 4：Commit**

```bash
git add app/src/main/java/com/xuanji/app/data/repository/ActionRepository.kt app/src/main/java/com/xuanji/app/ui/viewmodel/ActionViewModel.kt app/src/main/java/com/xuanji/app/di/AppModule.kt app/src/main/java/com/xuanji/app/data/repository/TestRecordRepository.kt
git commit -m "feat(数据层): 组合今日行动与人生画像输入"
```

### 任务 6：综合页加入今日行动卡和偏好编辑器

**文件：**
- 创建：`app/src/main/java/com/xuanji/app/ui/components/DailyActionCards.kt`
- 创建：`app/src/main/java/com/xuanji/app/ui/components/FoodPreferenceEditor.kt`
- 修改：`app/src/main/java/com/xuanji/app/ui/composite/CompositeFortuneScreen.kt`
- 修改：`app/src/main/java/com/xuanji/app/ui/viewmodel/CompositeFortuneViewModel.kt`

- [ ] **步骤 1：先写 UI model 测试**

```kotlin
@Test
fun action_card_model_exposes_content_score_confidence_and_why() {
    val plan = samplePlan()
    val cards = DailyActionCardModel.from(plan)
    assertEquals(listOf("吃什么", "做什么", "去哪玩"), cards.map { it.title })
    assertTrue(cards.all { it.why.isNotEmpty() && it.boundary.isNotBlank() })
}
```

- [ ] **步骤 2：实现三张可折叠卡**

`DailyActionCards.kt` 使用 `rememberSaveable` 保存展开状态，默认只显示菜名/行动/环境类型摘要；展开后显示匹配分、`ConfidenceLevel`、来源链、边界说明。食物卡显示当前偏好和“换一个符合偏好的方案”按钮，按钮只调用同一 planner 的稳定下一个同分/次高候选，不创建随机结果。所有按钮提供中文 `contentDescription` 和最小 48dp 触控区域。

- [ ] **步骤 3：挂载综合页**

在 `CompositeContent` 的 `SummaryBlock` 后加入 `DailyActionSection`，使用 `ActionViewModel` 的 `todayPlan`；加载和空档案状态分别显示明确文字。`CompositeFortuneViewModel` 保持现有综合运势状态，不复制算法，只传递 `profileKey/dateKey/fortune` 给行动 ViewModel。不要改变现有卡片排序或浮球层级。

- [ ] **步骤 4：运行 UI 编译和纯 Kotlin 测试**

运行：`./gradlew :app:testDebugUnitTest :app:compileDebugKotlin`

预期：PASS，且综合页继续保留既有滚动、卡片隐藏恢复和微光浮球行为。

- [ ] **步骤 5：Commit**

```bash
git add app/src/main/java/com/xuanji/app/ui/components/DailyActionCards.kt app/src/main/java/com/xuanji/app/ui/components/FoodPreferenceEditor.kt app/src/main/java/com/xuanji/app/ui/composite/CompositeFortuneScreen.kt app/src/main/java/com/xuanji/app/ui/viewmodel/CompositeFortuneViewModel.kt
git commit -m "feat(综合页): 展示可解释的今日行动卡"
```

### 任务 7：我的页加入人生画像和本地清除入口

**文件：**
- 创建：`app/src/main/java/com/xuanji/app/ui/components/LifeProfileCards.kt`
- 修改：`app/src/main/java/com/xuanji/app/ui/profile/ProfileScreen.kt`
- 修改：`app/src/main/java/com/xuanji/app/ui/viewmodel/ProfileViewModel.kt`

- [ ] **步骤 1：实现画像卡 UI model 测试**

```kotlin
@Test
fun region_cards_use_match_language_and_show_unmatched_factors() {
    val card = LifeProfileCardModel.from(sampleLifeProfile()).regions.single()
    assertTrue(card.title.contains("匹配示例"))
    assertTrue(card.body.contains("未考虑因素"))
    assertFalse(card.body.contains("最适合你"))
}
```

- [ ] **步骤 2：实现分段 UI**

`LifeProfileCards.kt` 分为职业簇、色彩簇、环境画像和地区匹配示例；顶部显示 `Low/Medium/High` 的输入完整度说明。地区最多展示 3 个 `RegionCandidate`，显示匹配标签和未考虑因素，不显示排名第一、命定、移民/旅行安全/收入保证等词。色彩只作为配色灵感，不宣称性格诊断。

- [ ] **步骤 3：接入 ProfileScreen**

在出生信息和角色设置之后加入 `LifeProfileSection` 与 `FoodPreferenceEditor`。自定义排除项保存前 trim、去重、限制 12 项；清除按钮只清除当前档案偏好。档案为空时显示“先填写出生信息”，不创建伪造画像。

- [ ] **步骤 4：运行编译与 lint**

运行：`./gradlew :app:testDebugUnitTest :app:lintDebug`

预期：BUILD SUCCESSFUL，lint 无 error；新增文本无溢出风险，长城市名使用换行而非水平裁切。

- [ ] **步骤 5：Commit**

```bash
git add app/src/main/java/com/xuanji/app/ui/components/LifeProfileCards.kt app/src/main/java/com/xuanji/app/ui/profile/ProfileScreen.kt app/src/main/java/com/xuanji/app/ui/viewmodel/ProfileViewModel.kt
git commit -m "feat(我的): 增加职业色彩与地区人生画像"
```

### 任务 8：让角色对话复用同一结果对象

**文件：**
- 修改：`app/src/main/java/com/xuanji/app/domain/MysticDialogueEngine.kt`
- 修改：`app/src/main/java/com/xuanji/app/domain/MysticIntentClassifier.kt`
- 修改：`app/src/main/java/com/xuanji/app/domain/MysticGuideGenerator.kt`
- 修改：`app/src/main/java/com/xuanji/app/domain/DialogueReplyValidator.kt`
- 创建：`app/src/test/kotlin/com/xuanji/app/domain/MysticDialogueActionTest.kt`

- [ ] **步骤 1：编写失败的意图与事实一致性测试**

```kotlin
@Test
fun meal_question_uses_the_same_title_score_and_evidence_as_the_card() {
    val plan = samplePlan()
    val reply = engine.reply(context(dailyActionPlan = plan), "今天早餐吃什么")
    assertTrue(reply.text.contains(plan.meals.first().title))
    assertTrue(reply.text.contains("${plan.meals.first().score}"))
    assertTrue(reply.groundedFacts.contains(plan.meals.first().evidence.first().label))
}

@Test
fun moving_or_health_guarantees_are_not_generated_from_action_results() {
    val reply = engine.reply(context(lifeProfile = sampleLifeProfile()), "哪个国家最适合我移民")
    assertFalse(reply.text.contains("保证"))
    assertFalse(reply.text.contains("一定要搬"))
}
```

- [ ] **步骤 2：扩展上下文和意图枚举**

在 `MysticIntent` 增加 `TodayMeal`、`TodayActivity`、`TodayOuting`、`LifeProfile`；`DialogueContext` 增加默认可空的 `dailyActionPlan: DailyActionPlan?` 和 `lifeProfile: LifeProfile?`，保留旧构造调用的默认值。分类器规则先匹配明确的“早餐/午餐/晚餐/外卖/吃什么”“今天做什么”“去哪玩”“适合什么工作/颜色/城市”，再回退现有 `Daily/Action/Career`，避免打断旧 golden。

- [ ] **步骤 3：实现事实复用与安全守卫**

`MysticGuideGenerator` 只读取 `DailyActionPlan`/`LifeProfile` 字段生成候选名、分数、来源和边界；找不到对应对象时回复“请先打开综合页计算今日行动”而不自行生成。`DialogueReplyValidator` 增加迁居保证、医疗/营养处方、投资收益保证的拒绝规则，在线 provider 与离线结果共用校验。

- [ ] **步骤 4：运行对话测试和既有 golden**

运行：`./gradlew :app:testDebugUnitTest --tests com.xuanji.app.domain.MysticDialogueActionTest`; `node _dev/dialogue_contract_test.js`

预期：新增意图测试 PASS，既有 45 条 golden 全部 PASS，旧问候/闲聊输出不出现“你问：「……」”。

- [ ] **步骤 5：Commit**

```bash
git add app/src/main/java/com/xuanji/app/domain/MysticDialogueEngine.kt app/src/main/java/com/xuanji/app/domain/MysticIntentClassifier.kt app/src/main/java/com/xuanji/app/domain/MysticGuideGenerator.kt app/src/main/java/com/xuanji/app/domain/DialogueReplyValidator.kt app/src/test/kotlin/com/xuanji/app/domain/MysticDialogueActionTest.kt
git commit -m "feat(对话): 复用今日行动与人生画像事实"
```

### 任务 9：建立双端契约和文档说明

**文件：**
- 创建：`_dev/action_profile_contract.json`
- 创建：`_dev/action_profile_contract_test.js`
- 修改：`README.md`
- 修改：`docs/SYSTEMS_OVERVIEW.md`
- 修改：`docs/TECHNICAL_DEBT.md`

- [ ] **步骤 1：写入 golden 契约**

契约固定以下字段：`dateKey/profileKey`、五行/星座输入摘要、四项今日权重、食物偏好过滤、`ConfidenceLevel`、职业/色彩/地区公式、同分排序 seed 形状和越界词列表。每条 golden 记录期望候选 key、score、证据 source，不能只断言颜色或文案。

- [ ] **步骤 2：实现 Node 结构校验**

`_dev/action_profile_contract_test.js` 使用 Node 内置 `assert` 读取契约，验证：权重和为 1；每条食物有 ingredients/substitute/deliveryKeywords；所有主候选同时声明 FiveElements/Zodiac；地区候选最多 3 个；禁止“保证/最适合/命中注定/移民一定”等词；seed 只出现在 tie-break 字段。运行失败时输出具体候选 key 和字段名。

- [ ] **步骤 3：更新产品边界文档**

README 和系统概览明确：默认离线、无 GPS/联网、结果是文化与生活方式灵感、职业是倾向簇、地区是标签匹配示例、食品不构成医学/过敏建议；技术债记录静态目录维护、城市标签覆盖范围和设备验收仍需人工确认。

- [ ] **步骤 4：运行契约测试**

运行：`node _dev/action_profile_contract_test.js`; `node _dev/authentic_systems_contract_test.js`; `node _dev/dialogue_contract_test.js`

预期：三个脚本均 PASS。

- [ ] **步骤 5：Commit**

```bash
git add _dev/action_profile_contract.json _dev/action_profile_contract_test.js README.md docs/SYSTEMS_OVERVIEW.md docs/TECHNICAL_DEBT.md
git commit -m "docs(契约): 固化行动与画像双端边界"
```

### 任务 10：全量验证、交接和可审计输出

**文件：**
- 创建：`docs/superpowers/handoffs/2026-09-19-daily-actions-life-profile-handoff.md`
- 不修改：已有未提交截图、UI dump、脚本和 `.superpowers` 视觉资料。

- [ ] **步骤 1：运行 Android 门禁**

按独立命令运行并保存输出：

```powershell
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
./gradlew :app:compileDebugAndroidTestKotlin
```

预期：四项均 `BUILD SUCCESSFUL`；lint error 为 0；APK 路径为 `app/build/outputs/apk/debug/app-debug.apk`。

- [ ] **步骤 2：运行双端契约门禁**

```powershell
node _dev/action_profile_contract_test.js
node _dev/authentic_systems_contract_test.js
node _dev/dialogue_contract_test.js
```

预期：行动/画像契约、传统体系契约和对话 golden 全部 PASS。

- [ ] **步骤 3：写入交接证据**

交接文档记录：实现 commit 列表、测试原始输出位置、APK SHA-256、默认离线声明、已知静态目录覆盖范围、未执行的手机/TalkBack/Logcat/真实截图项目。明确手机验收要等用户通知，不把旧截图当作当前证据。

- [ ] **步骤 4：检查工作树并提交交接文档**

运行：`git status --short`; `git diff --check`

预期：没有由本功能误删或覆盖用户原有未跟踪成果，且无空白错误。

```bash
git add docs/superpowers/handoffs/2026-09-19-daily-actions-life-profile-handoff.md
git commit -m "docs(交接): 记录今日行动与人生画像验收证据"
```
