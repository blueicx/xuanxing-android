# X3 今日行动与人生画像设计规格

状态：已获用户确认，待用户审阅后进入实现计划。

日期：2026-09-19

## 1. 决策摘要

本功能采用“双层入口”方案：

1. 今日行动卡：根据每日五行、星座、现有综合运势和季节标签，给出具体的吃什么、做什么、去哪玩。
2. 人生画像：根据出生盘、星座和用户已经完成的测试，给出职业簇、色彩簇、生活环境类型以及少量候选国家/城市。

硬性约束：所有结果必须有可追溯的五行 + 星座依据；可叠加每日盘面、季节、测试和城市标签，但不能用随机抽取伪装成测算。只有匹配分完全相同时，才允许使用稳定 hash 做并列排序。

## 2. 范围与非目标

### 2.1 本阶段包含

- 综合页的“今日行动”卡组。
- “我的”页的“人生画像”入口。
- 饮食偏好本地设置：素食、清真、忌辣、忌酒和用户自定义不吃项。
- 手动选择城市，用于本日“去哪玩”的环境匹配。
- 离线城市标签目录和少量候选国家/城市。
- 对话引擎复用同一套今日行动和人生画像结果。
- 每条结果显示来源链、匹配分、置信度和边界说明。

### 2.2 明确不包含

- 不新增占卜体系，不接实时天气、地图、餐厅、旅游 API。
- 不请求 GPS，不上传位置，不做后台定位。
- 不给医疗、过敏、营养治疗、投资、迁居保证或命运断言。
- 不把 MBTI、MMPI、16PF 等未经授权的常模包装为标准结论。
- 不用随机数决定主结果；随机只允许作为未来明确标记的娱乐模式，且不在本功能启用。

## 3. 领域模型

### 3.1 今日行动

```kotlin
data class DailyActionPlan(
    val dateKey: String,
    val profileKey: String,
    val cityKey: String?,
    val meals: List<MealSuggestion>,
    val activities: List<ActivitySuggestion>,
    val outings: List<OutingSuggestion>,
    val evidence: List<ActionEvidence>,
    val confidence: ConfidenceLevel,
    val disclaimer: String
)

data class MealSuggestion(
    val slot: MealSlot,
    val title: String,
    val ingredients: List<String>,
    val substitute: String,
    val deliveryKeywords: List<String>,
    val score: Int,
    val evidence: List<ActionEvidence>
)

data class ActivitySuggestion(
    val title: String,
    val durationMinutes: IntRange,
    val bestPeriod: String,
    val avoid: String?,
    val score: Int,
    val evidence: List<ActionEvidence>
)

data class OutingSuggestion(
    val placeType: String,
    val reason: String,
    val cityLabel: String,
    val score: Int,
    val evidence: List<ActionEvidence>
)

data class ActionEvidence(
    val source: ActionSource,
    val key: String,
    val label: String,
    val contribution: Int
)

enum class ActionSource { FiveElements, Zodiac, DailyFortune, Season, CityProfile, Preference }
enum class ConfidenceLevel { Low, Medium, High }
enum class MealSlot { Breakfast, Lunch, Dinner, Drink }
```

`DailyActionPlan` 是纯结果对象，不负责保存状态。日期、profileKey、城市和偏好变化时重新计算；同一输入必须得到同一结果。

### 3.2 饮食偏好

```kotlin
data class FoodPreference(
    val vegetarian: Boolean = false,
    val halal: Boolean = false,
    val avoidSpicy: Boolean = false,
    val avoidAlcohol: Boolean = false,
    val excludedIngredients: Set<String> = emptySet()
)
```

偏好只做候选过滤和替换，不被解释为健康诊断。偏好存储在本机、按 profileKey 分仓，提供清除入口；未设置时不得假装知道用户忌口。

### 3.3 人生画像

```kotlin
data class LifeProfile(
    val profileKey: String,
    val careerClusters: List<CareerCluster>,
    val colorPalette: List<ColorCluster>,
    val environmentProfile: EnvironmentProfile,
    val regionCandidates: List<RegionCandidate>,
    val evidence: List<ActionEvidence>,
    val confidence: ConfidenceLevel,
    val missingInputs: List<String>,
    val disclaimer: String
)

data class CareerCluster(val key: String, val label: String, val score: Int, val reasons: List<String>)
data class ColorCluster(val key: String, val label: String, val hex: String, val reasons: List<String>)
data class EnvironmentProfile(val tags: List<String>, val rhythm: String, val reasons: List<String>)
data class RegionCandidate(
    val country: String,
    val city: String,
    val matchedTags: List<String>,
    val unmatchedFactors: List<String>,
    val score: Int
)
```

出生盘始终是基础输入；已有测试结果按测试类型读取，未完成测试不填默认分。没有测试时仍生成画像，但 `confidence=Low`，并列出 `missingInputs`。

## 4. 算法与确定性

### 4.1 共同输入

- 东方：`BaziChart` 的五行分布、日主、喜用神、当日干支/日元素。
- 西方：太阳星座、星座元素、已有 `WesternDailyFortune` 的维度和日期。
- 综合：`CompositeDailyFortune` 的维度、幸运色、幸运方向和 caution，作为当日增强信号。
- 季节：由 dateKey 得到四季标签，不能使用设备随机状态。
- 可选：霍兰德、MBTI、大五等已有 `TestRecord`。测试数据只读取应用已保存的结果和透明的原始维度。
- 城市：`CityProfileCatalog` 的公开静态标签；今日出游使用用户手动选择的 cityKey。

### 4.2 今日候选评分

每个食物、行动和地点类型在静态目录中带有五行、星座元素、季节、时段和环境标签。先根据 `FoodPreference` 过滤不可用候选，再计算：

```text
score =
    0.40 * fiveElementFit
  + 0.25 * zodiacFit
  + 0.20 * dailyFortuneFit
  + 0.15 * seasonOrCityFit
```

各项归一化到 0–100，四舍五入后限制在 0–100。权重固定在纯 Kotlin 常量中，并由测试锁定；不能在 UI 层偷偷改变权重。

- 食物：`seasonOrCityFit` 使用季节和菜系/可获得性标签。
- 做什么：`dailyFortuneFit` 读取事业、情绪、健康等已有维度，但不能把低分写成医学判断。
- 去哪玩：`seasonOrCityFit` 使用手动城市的临水、山地、绿地、文化活动、夜间等标签。

同分候选使用 `stableHash(profileKey|dateKey|candidateKey)` 排序，仅用于稳定打破并列，不改变匹配分，也不产生随机结果。

### 4.3 人生画像评分

职业簇：

```text
withTests = 0.45 * chartElementFit + 0.25 * zodiacFit + 0.30 * testFit
withoutTests = 0.60 * chartElementFit + 0.40 * zodiacFit
```

色彩簇：

```text
0.60 * elementColorFit + 0.25 * zodiacPaletteFit + 0.15 * testStyleFit
```

地区候选：

```text
0.35 * elementEnvironmentFit
+ 0.25 * zodiacRhythmFit
+ 0.20 * careerEnvironmentFit
+ 0.20 * explicitPreferenceFit
```

地区候选来自静态城市目录，不从网络搜索生成。国家/城市只作为环境匹配示例，界面同时显示匹配因素和未考虑因素。用户当前手动选择的城市用于今日出游，不会被偷偷当成“命中证明”。

### 4.4 置信度

- `High`：出生盘、星座、至少一个相关测试和有效城市/日期输入齐全。
- `Medium`：出生盘和星座齐全，缺一个增强来源。
- `Low`：仅有出生盘或存在无法解释的缺失输入。

置信度表达“输入完整度和解释稳定性”，不表达科学准确率或人生成功概率。

## 5. UI 与交互

### 5.1 今日行动卡

综合页新增可折叠的“今日行动”区块，默认展示三张摘要卡：吃什么、做什么、去哪玩。点击后展开具体菜名、替换项、外卖搜索词、时段和来源链。

每张卡必须显示：

- 标题和可执行内容。
- 匹配分与置信度。
- “为什么”展开入口。
- 边界说明。
- 若是食物，显示当前饮食偏好和“换一个符合偏好的方案”。

### 5.2 人生画像

“我的”页新增入口，页面分为职业、色彩、地区三段。地区段先展示环境画像，再展示最多 3 个候选城市；没有测试时在顶部显示低置信度说明和缺失输入。

候选城市卡片不显示“排名第一”“最适合你”等绝对措辞，使用“匹配示例”“符合的标签”“未考虑因素”。

### 5.3 对话复用

对话识别以下主题并复用引擎：

- “今天吃什么 / 早餐吃什么 / 外卖吃什么” → `DailyActionEngine` 的 meal。
- “今天做什么 / 去哪玩” → activity / outing。
- “我适合什么工作 / 什么颜色 / 哪个城市” → `LifeProfileEngine`。

回复中的分数、候选名称和依据必须来自对应结果对象，不能在 `MysticGuideGenerator` 中再维护一份模板事实。

## 6. 隐私与安全边界

- 默认完全离线，不请求网络、不请求 GPS、不写入密钥。
- 城市选择、饮食偏好和画像结果仅存本机；提供清除入口。
- 食物建议附“文化与生活方式灵感”说明，不构成医疗、营养或过敏建议。
- 职业建议是兴趣/倾向簇，不是能力评估、招聘建议或心理诊断。
- 地区候选是标签匹配示例，不是移民、旅行安全、签证、收入或迁居建议。
- 任何在线 Provider 未来接入时，结果仍必须经过现有本地事实校验与安全守卫。

## 7. 测试验收

纯 Kotlin 测试至少覆盖：

1. 同一 profile/date/fortune/城市/偏好产生完全相同的今日结果。
2. 修改五行喜用或星座输入会改变对应依据链，而不是只改变颜色。
3. 食物结果具体到菜名，且每条含替换项和外卖关键词。
4. 素食、清真、忌辣、忌酒和自定义排除项会过滤候选；过滤后无候选时返回明确降级。
5. 空城市使用“环境类型”降级，不崩溃、不伪造附近地点。
6. 未完成测试生成 `Low` 置信度并列出缺失输入。
7. 已完成测试会改变相应职业簇/色彩簇的解释来源。
8. 城市候选显示 matchedTags 与 unmatchedFactors，最多 3 个，排序稳定。
9. 所有主结果均来自五行 + 星座匹配；随机 seed 只能出现在并列排序测试。
10. 对话引擎与 UI 卡片共享同一结果对象，名称、分数、来源链完全一致。
11. 医疗、营养、投资、迁居保证等越界文本会被本地安全规则拒绝或改写。
12. DataStore 按 profileKey 隔离偏好，清除一个档案不会影响其他档案。

Android UI 编译和设备验收继续遵守既有边界：先跑 JVM、lint、assemble 和 AndroidTest 编译；手机、TalkBack、Logcat、真实视觉截图等用户通知后再执行。

## 8. 分阶段交付

1. 先落地纯 Kotlin 标签目录、评分器、结果对象和测试。
2. 接入饮食偏好 DataStore 与今日行动卡。
3. 接入人生画像页面和城市候选目录。
4. 接入对话主题复用、契约 golden 和文档。
5. 最后做设备和无障碍验收，不把旧截图当作当前证据。
