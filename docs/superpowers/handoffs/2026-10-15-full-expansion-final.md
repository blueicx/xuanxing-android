# X3 全量扩充最终交接（2026-10-15）

最新记录以本文为准。保留工作区、截图和所有既有产物；未使用子代理，未提交、推送或安装手机。人工视觉、大字号和 TalkBack 仍按约定排除。模拟器和源码验证不等于实体设备验收。

后续会话清除修复、lint整理及新APK记录见[会话与工程交接](2026-10-15-thread-safety-and-lint.md)。本文保留全量扩充实现说明与当时的514单测/旧APK证据，不再作为最新产物清单。

## 已落地的七波功能

| 波次 | 实现与验证入口 |
|---|---|
| 目录与确定性规则 | 64 食物、32 活动、16 环境、24 城市；替代候选同规则过滤、实际时长受约束、依据贡献不重复。`DailyActionPlannerTest`、行动契约 |
| 安排与回顾 | 按档案安排、备注、完成撤销、购物清单、原文快照、日历、显式近似提醒。`DailyLifeTest`、`DailyLifeInteractionTest` |
| 阅读与搜索 | 60 术语、16 自编短解说、真实详情入口、本地搜索，不索引私密聊天。`LearningSearchTest`、详情/收藏交互 |
| 四角色联动 | 临时条件、候选指代、具体依据、主动交接、独立线程、旧 token 丢弃；202 输入 × 4 角色真实引擎评估。`MysticDialogueRelevanceMatrixTest`、完整应用三阶段 |
| 画像与游戏 | 职业任务/技能/体验、色彩应用、独立现实气候参考；诗句/星图各24关、丝路12固定任务与九城地图、象棋真实规则和存档。领域单测及四角色真实页面推进 |
| 数据闭环 | 备份v2、新旧协议、加密边界、暂存验证、冲突预览、CAS/日志/回滚；按功能清除、摘要、提醒撤销。备份/恢复单测和交互 |
| 工程 | 状态与渲染模块拆分、后台下采样/缓存、棋局取消与预算、组件和整应用PSS基线；CI保留全部UI测试与隔离恢复链路、产物哈希 |

## 本轮补齐的缺口

- 气候参考由18城扩至全部24城：20个JMA测站、1个KNMI测站、3个NASA POWER/MERRA-2网格参考点。同为1991–2020期，但明确区分测量方法、机场/参考点，不把网格估计称为当地实测。12月简单平均与项目温暖/凉爽阈值独立于文化分。详见[来源与数值](../../data/JMA_CLIMATE_NORMALS.md)。
- 城市列表不再只能看前六项：可展开全部城市，单城按需查看/收起完整依据。新增真实点击及保存实例恢复测试，未注入展开状态绕过输入。
- 808次评估增加了行动方案、人生画像上下文及菜名/活动/地点/职业内容断言。旧用例只检查路由和非空文本，不足以证明回答与卡片一致；新增断言先失败，再补测试上下文。Node验证样例契约，实际引擎仍由JVM执行，不混淆两种证据。
- 删除进程级`mysticCompanionStates`缓存。每次新建界面作用域只由Compose remember持有，关闭后从可清除的DataStore线程恢复，避免持久化删除后重新拾起旧内存线程。新增同键新作用域不复用旧对象的回归，先失败后修复。
- 迁出`MysticGuideLocalState.kt`，分离线程复原、访问记录存储和局部UI状态；同步迁移访问记录键的契约文件位置，不降低原有键隔离校验。`MysticGuideCard.kt`由3233降至3067行，仍是渐进拆分，不宣称所有大文件已消失。

## 明确不伪造的能力

- 城市预算与生活节奏没有可靠、同口径的现实资料，仍显示资料不足、不参与现实排序；不把文化标签充作物价、收入或迁居结论。界面条件选择不等于取得事实数据。
- 围棋/国际象棋provider、Ifá Ese/纳迪原文、官方心理常模和在线模型沿用未启用/授权边界，不属于本轮新增实现。
- 现有lint警告仍需长期分级治理；Debug模拟器PSS不是Release或实体低端机性能证明。
- 手机安装、实体机验收和远端CI没有执行；不能因本机门禁通过而宣称已交付到手机或GitHub。

## 最终验证与产物

全部十项Node契约通过，45条golden保留；JVM为514项、0失败/错误/跳过，其中矩阵实际执行808次四角色回复。`lintDebug`为0 error / 84 warning / 20 information。Debug、DebugAndroidTest、Regression及其AndroidTest构建成功。

完整Debug模拟器报告62项、0失败/错误、3项隔离保护跳过，即59项实际执行通过（233.786秒）。新增两项城市交互/实例恢复也在完整集内，不是只编译或只检查节点存在。

三个独立真实应用阶段均通过，证据：`work/.dispatch-backup/20261014-application-recovery/2026-10-15T07-10-33-029Z/`。实际PID为16526→16578。第三阶段整应用PSS连续采样79次，观察最大305307 KiB、中位数256217 KiB；有Debug/instrumentation开销，是观测样本最大值，不是实体机、Release或绝对峰值，也不据此宣称优化百分比。

待安装APK：`app/build/outputs/apk/debug/app-debug.apk`，包名`com.xuanji.app`，26529856 bytes，SHA256：

```text
F724646B60CE784FFC0A7A808E4AAD106BF10C9D14AEAEE8F0251277D9211413
```

Debug版本名仍为1.0.0/versionCode 1，因此应以APK哈希区分，不只看版本名。构建清单：`work/.dispatch-backup/20261015-region-completion/final-build-manifest.json`。分支`codex/reading-expansion`、HEAD `0456228af1cc061d97acec98e24201943a580cc7`，包含未提交修改，不能只靠HEAD重现产物。

实际验收命令：

```powershell
$contracts = @(rg --files _dev -g '*contract_test.js')
foreach ($contract in $contracts) {
    node $contract
    if ($LASTEXITCODE -ne 0) { throw "Contract failed: $contract" }
}
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
$env:ANDROID_ADB_SERVER_PORT = '5038'
$env:ANDROID_SERIAL = '127.0.0.1:5581'
.\gradlew.bat :app:connectedDebugAndroidTest
.\gradlew.bat -PxuanjiRegression=true :app:assembleRegression :app:assembleRegressionAndroidTest
$env:XUANJI_ADB = 'C:/Users/blueice/AppData/Local/Android/Sdk/platform-tools/adb.exe'
$env:XUANJI_ADB_PORT = '5038'
node _dev/isolated_application_recovery.js 127.0.0.1:5581
```

复现默认UI前应先核对指定serial确为隔离模拟器；恢复运行器另有模拟器属性与APK包名双重保护，不卸载、不`pm clear`。本轮未安装手机，未进行人工视觉/TalkBack，未提交/推送或触发远端CI。旧报告保留于[上一轮记录](2026-10-15-remaining-work-closeout.md)，其中18城、510单测及旧APK哈希不作为本轮结果。
