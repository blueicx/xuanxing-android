# Xuanji 真实体系一致性收尾计划

> **面向 AI 代理的工作者：** 必需子技能：使用 `subagent-driven-development` 逐任务实现此计划。

**目标：** 修复已确认的确定性算法错误，统一 Android/小程序的输入与黄金样例，并把无法声称完整的传统/心理系统明确降级为简化或文化娱乐实现。

**架构：** 共享可验证的历法/天文/输入边界；传统体系保留教学骨架但不伪称完整；心理测验与原创模块统一采用非诊断、自我探索标签。每个修复先加回归测试，再改生产代码。

**技术栈：** Kotlin/JUnit/Gradle；微信小程序 CommonJS/Node；现有离线实现，不引入在线模型或密钥。

---

### 任务 1：Android 核心算法边界

**文件：** `app/src/main/java/com/xuanji/app/domain/ZiweiCalculator.kt`、`BaziCalculator.kt`、`ZodiacCalculator.kt`、`divination/QiMen.kt`、`divination/HellenisticAstrology.kt`、`divination/MayaTzolkin.kt`；新增对应 domain tests。

- [x] 为紫微命宫/身宫索引、八字子初边界、奇门 2000-01-07 甲子、玛雅 2012-12-21、古典昼夜幸运点、星盘时区写失败测试。
- [x] 修复索引、共享日柱、相关性锚点和昼夜公式；无法引入完整历法的地方增加显式 approximation policy。
- [x] 运行 `./gradlew :app:testDebugUnitTest`。

### 任务 2：Android 传统/原创系统安全降级

**文件：** `IChing.kt`、`Ifa.kt`、`NadiAstrology.kt`、`KabbalahAstrology.kt`、`Nameology.kt`、`LotDraw.kt`、相关 `DivinationData/SystemGuide/README/docs`。

- [x] 先测试 UI/结果标签包含“简化/文化模拟/非标准化”。
- [x] 统一修改标题、说明和危险的临床/真实完整措辞；保留现有离线行为。
- [x] 对易经、Ifá、纳迪、姓名学、灵签补充数据完整度说明，不伪造经典来源。

### 任务 3：Android 心理测试边界

**文件：** `domain/mbti/Mbti.kt`、`domain/test/Mmpi.kt`、`BigFive.kt`、`Holland.kt`、`Cattell16PF.kt`、`FunTests3.kt`、测试 UI 文案；新增输入边界测试。

- [x] 先写超长/缺答/越界输入测试。
- [x] 修复 Big Five 越界与非法分数处理；所有自编题改为风格/简版/自我探索名称；移除 MMPI/Raven 临床或标准化暗示。
- [x] 运行 Android 单测和 lint。

### 任务 4：小程序算法错误与双端契约

**文件：** `services/baziExtra.js`、`services/easternFortune.js`、`services/westernFortune.js`、`services/lunar.js`、`subpackages/divination-detail/engines/batch1.js`、`batch2.js`、`batch3.js`、`batch4.js`、`services/systemGuides.js`、测试脚本。

- [x] 为神煞 `in`、立春前月干、月亮锚点、奇门日干支、古典幸运点、八宅坐山、Vastu 输入写 Node 失败测试。
- [x] 修复算法或将输出改名为象征性模拟；删除重复 western fortune 实现。
- [x] 增加跨端 golden JSON，运行 `_dev/run_all_tests.js`。

### 任务 5：文案、契约和验证

**文件：** 两端 README/SYSTEMS_OVERVIEW、`dialogue_contract.json`、系统目录与测试目录。

- [x] 统一三档标签：历法/天文近似、传统简化演示、文化娱乐/自编问卷。
- [x] 添加 2000-01-07、立春前后、昼夜盘、2012-12-21、空/超长输入黄金样例。
- [x] 运行 Android `testDebugUnitTest lintDebug assembleDebug`、小程序 7 项测试、`git diff --check`；无设备时明确未做设备验证。
