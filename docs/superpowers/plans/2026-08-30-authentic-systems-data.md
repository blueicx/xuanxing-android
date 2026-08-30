# 真实体系数据边界实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [x]`）语法来跟踪进度。

**目标：** 为紫微农历、Ifá 256、纳迪叶文和心理测验建立可核验的数据模型、授权边界与离线回退，避免把近似或自编内容冒充正式体系。

**架构：** 领域层通过 Provider 接口隔离历表、宗教语料、叶文与常模；离线默认使用可审计的内置数据或明确标注的模拟结果。紫微增加农历输入路径，Ifá 使用 16×16 组合索引，纳迪使用授权语料适配器，心理测验使用开放 IPIP 基线并保留商业量表适配器。

**技术栈：** Kotlin/JVM、Android Gradle、JUnit4、JSON/CSV 资源、现有 Compose UI；小程序使用 Node.js 纯 JS 测试。

---

### 任务 1：农历/闰月领域模型与 Provider

**文件：**
- 创建：`app/src/main/java/com/xuanji/app/domain/calendar/LunisolarDate.kt`
- 创建：`app/src/main/java/com/xuanji/app/domain/calendar/LunisolarCalendarProvider.kt`
- 创建：`app/src/main/java/com/xuanji/app/domain/calendar/TableLunisolarCalendarProvider.kt`
- 修改：`app/src/main/java/com/xuanji/app/domain/ZiweiCalculator.kt`
- 测试：`app/src/test/kotlin/com/xuanji/app/domain/LunisolarCalendarTest.kt`

- [x] 编写 1901–2100 表驱动接口、闰月表示和缺失数据错误类型。
- [x] 为 2020 闰四月、2023 闰二月、2024 无闰月等样例加入可核验 golden case。
- [x] 增加 Ziwei 的 `calculate(lunisolarDate, ...)` 正式入口，旧公历入口标记 approximation fallback。
- [x] 运行定向测试并确认缺失年份不会静默伪造。

### 任务 2：Ifá 256 组合与 Ese 语料边界

**文件：**
- 修改：`app/src/main/java/com/xuanji/app/domain/divination/Ifa.kt`
- 创建：`app/src/main/java/com/xuanji/app/domain/divination/IfaCorpus.kt`
- 测试：`app/src/test/kotlin/com/xuanji/app/domain/IfaCorpusTest.kt`
- 修改：`app/src/main/java/com/xuanji/app/ui/divination/IfaScreen.kt`

- [x] 生成稳定的 256 个有序组合（outer/inner），保留 16 个主 Odu 的规范名称。
- [x] 定义 `EseCorpusProvider`、来源/许可证元数据和缺省状态；无授权时返回“正文不可用”。
- [x] 禁止 UI 将 hash 索引写成仪式、经文或占卜结论。
- [x] 测试 256 唯一性、确定性和未授权回退。

### 任务 3：纳迪真实叶文 Provider seam

**文件：**
- 修改：`app/src/main/java/com/xuanji/app/domain/divination/NadiAstrology.kt`
- 创建：`app/src/main/java/com/xuanji/app/domain/divination/NadiCorpus.kt`
- 测试：`app/src/test/kotlin/com/xuanji/app/domain/NadiCorpusTest.kt`
- 修改：`docs/SYSTEMS_OVERVIEW.md`

- [x] 定义叶文查询键、语言、来源、授权和匹配置信度模型。
- [x] 实现 `UnavailableNadiCorpusProvider` 与现有确定性模拟回退，输出显式 provenance。
- [x] UI 文案区分“授权叶文”与“离线模拟”，禁止生成伪造原叶文本。
- [x] 测试无语料、命中语料、模拟回退三种状态。

### 任务 4：开放心理测验与官方量表适配边界

**文件：**
- 创建：`app/src/main/java/com/xuanji/app/domain/test/PsychometricCatalog.kt`
- 创建：`app/src/main/java/com/xuanji/app/domain/test/IpipBigFive.kt`
- 创建：`app/src/main/java/com/xuanji/app/domain/test/NormativeSample.kt`
- 测试：`app/src/test/kotlin/com/xuanji/app/domain/IpipBigFiveTest.kt`
- 修改：`app/src/main/java/com/xuanji/app/ui/test/TestHubScreen.kt`
- 修改：`docs/SYSTEMS_OVERVIEW.md`

- [x] 增加 IPIP Big Five-50 的开放题目/反向计分/维度分数模型，明确不等同于商业量表。
- [x] 定义常模版本、样本描述、百分位计算和本地导入格式；无常模时只显示原始/标准化分数。
- [x] 为 MMPI、Raven、16PF、MBTI 提供授权适配器状态，不内置受版权保护题目、答案或官方常模。
- [x] 测试反向题、边界分数、常模缺失与许可证缺失。

### 任务 5：双端契约、文档与全量验证

**文件：**
- 创建：`_dev/authentic_systems_contract.json`
- 修改：`_dev/.system_consistency_test.js`
- 修改：`docs/SYSTEMS_OVERVIEW.md`
- 修改：`README.md`

- [x] 同步历法、Ifá、纳迪、心理测验的状态枚举、provenance 和安全文案。
- [x] 运行 Android `testDebugUnitTest`、`lintDebug`、`assembleDebug`。
- [x] 运行 `F:\huny\xuanji-miniprogram` 的 7 套测试、结构 lint、跨体系一致性及新增契约测试。
- [x] 记录 ADB 设备为空时的未验证边界，不宣称实体机 UI 完成。
