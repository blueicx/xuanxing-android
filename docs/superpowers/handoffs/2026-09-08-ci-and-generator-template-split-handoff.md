# X3 玄机交接：CI 对话契约与模板拆分

## 当前状态

- 日期：2026-09-08
- 项目：`F:\huny\xuanji-android`
- 分支：`codex/system-consistency`
- 起始基线：`3ed9806 docs: replace the "not pushed" claims with what run #17 actually showed`
- 本轮代码尚未推送；远端 CI 尚未针对本轮改动重跑。
- 工作树中的既有截图、UI dump、脚本、宣传图和 `.superpowers/` 文件均为用户成果，未清理、未回退。

## 本轮已完成

### 1. 对话契约进入 CI

`.github/workflows/build.yml` 现在在 Gradle 之前安装 Node 20 并执行：

```text
node _dev/dialogue_contract_test.js
```

契约脚本仍校验 45 条 golden wording、测试用例映射和安全/角色/灵签约束；模板拆分后，身份文案同时从 `MysticGuideGenerator.kt` 与 `MysticDialogueTemplates.kt` 读取，避免把文案移文件后门禁失效。

### 2. 生成器第一刀模板拆分

新增：

- `app/src/main/java/com/xuanji/app/domain/MysticDialogueTemplates.kt`
- `app/src/test/kotlin/com/xuanji/app/domain/MysticDialogueTemplatesTest.kt`

已从 `MysticGuideGenerator.kt` 移出 greeting/farewell/thanks/identity/smalltalk/daily/chat 模板和确定性 pulse hash。生成器由 3468 行降到 3191 行；`customAnswer` 的路由、`MysticSafetyGuard.enforce` 唯一接线点、persona/style key 和离线默认行为保持不变。

## 验证证据

本机最终树执行：

```text
node _dev/dialogue_contract_test.js
dialogue contract: PASS (45 golden entries)

./gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon --console=plain -Pkotlin.compiler.execution.strategy=in-process
BUILD SUCCESSFUL
```

三项 Gradle 任务均成功：`testDebugUnitTest`、`lintDebug`、`assembleDebug`。Lint 没有 error，剩余为既有 warning/SDK XML 提示。

TDD 过程：先添加 `MysticDialogueTemplatesTest` 并确认缺少实现时失败，再实现模板模块，最后目标测试与全量门禁通过。

## 尚未完成 / 不要误报

1. 本轮改动尚未推送，不能宣称新的 GitHub Actions run 已通过。
2. `app/src/androidTest` 的棋盘/浮球/舞台用例仍只完成编译准备，未在设备或 CI 执行。
3. 手机复测、TalkBack、reduced-motion、safe area、DataStore 真机往返按用户要求等待通知，不要主动执行 `adb install` 或 UI 复测。
4. `MysticGuideCard.kt`、`MysticFloatingGuide.kt` 和生成器剩余 topic/fortune 大段仍可继续按稳定边界拆分；每次拆分后重复测试、lint、assemble。
5. 小程序结构 lint 与双端 golden contract 的本轮远端/完整复核尚未补做。

## 下一步建议

1. 先将本轮显式文件提交并推送，等待 CI 验证 Node 契约门禁。
2. 远端通过后，再拆 `MysticGuideGenerator.kt` 的 topic/fortune 分支；保持一次一个边界。
3. 用户通知手机可测后，采集 `adb devices`、安装、启动、三页浮球/舞台、TalkBack 和 Logcat 证据，并更新本交接。
