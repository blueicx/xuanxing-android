# X3 玄师陪伴五项优化收口交接（2026-09-22）

## 当前分支

`codex/companion-five-finish`，工作树：`F:\huny\xuanji-android-companion-five`。

主目录未安装、未连接手机，用户要求手机验证后置。主目录中的截图、UI dump 和其他未跟踪成果未改动。

## 已完成

- 新增 `MysticDrawerState`、`MysticStageDrawerSession` 和 reducer；舞台支持 Peek/Expanded 语义与 TalkBack 文案。
- 新增 `MysticAssetRenderMode` 与 `MysticVisualAssetCatalog`：三张完整场景图与墨衡透明前景明确分流，完整场景不重复叠加背景。
- 新增 `MysticCompanionActionRouter` 与 `MysticCompanionActionCard`；今日饮食、行动、出游、人生画像和依据可从对话直接召回。
- `DialogueContext` 现在带入真实 `DailyActionPlan` / `LifeProfile`；对话仍默认离线并沿用安全护栏。
- 新增 `CompanionGameProgressEnvelope`、Codec 和 DataStore Store；诗句接龙、星图观测、丝路路线按 profile/角色/游戏隔离保存。
- `CompanionGameCard` 接入存档恢复、每步保存和重开清除。
- 新增 JVM 回归测试和舞台抽屉 AndroidTest 交互用例；Node 舞台/对话契约增加新结构检查。

## 已验证

- 四项 Node 契约通过：`authentic_systems_contract_test.js`、`dialogue_contract_test.js`、`unified_stage_contract_test.js`、`action_profile_contract_test.js`。
- 新增状态、路由、素材、Codec、Store 测试通过。
- 初始完整 JVM 基线通过 363 个测试；修改后的完整 JVM、lint、APK 和 AndroidTest 编译仍需在本分支收尾执行。

## 收尾命令

```powershell
node _dev/authentic_systems_contract_test.js
node _dev/dialogue_contract_test.js
node _dev/unified_stage_contract_test.js
node _dev/action_profile_contract_test.js
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleDebugAndroidTest
```

手机安装、TalkBack、旋转、返回键、Logcat 和真实截图需在用户通知后单独执行，不用编译结果替代设备验收。
