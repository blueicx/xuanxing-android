# X3 深陪伴实现交接（2026-09-12）

## 当前分支

- 分支：`codex/system-consistency`
- 计划基线：`69277f1`（`docs(陪伴): 编写深陪伴实现计划`）
- 本轮实现采用小提交，保留工作树中原有未跟踪截图、UI dump、`.superpowers/` 和宣传图，不做清理、不回退。
- 手机、TalkBack、Logcat、真实视觉截图按用户要求暂缓，不能用本机编译或 JVM 测试替代。

## 已完成切片

1. `436d8e9`：`MysticDialogueAnalyzer`，统一 NFKC/标点/大小写，输出主主题、次主题实体、置信度与上一轮承接。
2. `24b0459`：`DialogueReply` 澄清入口、`MysticSessionState` 事件和旧 token 丢弃。
3. `406a886`：`SoftMemoryTagCodec` / `SoftMemoryTagStore`，独立本地命名空间，软标签可撤回和清除，不进入 `RecollectionKind`。
4. `327eadb`：`DialogueReplyValidator` 与 Provider 离线回退，在线结果不得伪造盘面分数、长期记忆或医疗/财务结论。
5. `52dbc8e`：澄清行和软记忆面板接入 `MysticGuideCard` / `MysticConversationPanel`。
6. `75ea2f7`：`MysticStageLayout`、`MysticCultureBackdrop`、`MysticFigureCanvas`，舞台标题统一只显示 persona 名称，文化皮肤有场景/道具/动作/语汇差异。
7. `90312e7`：`MysticMessageList`、`MysticConversationInput` 与输入上限模型，保留现有公共参数与状态提升。
8. `2e3767b`：`MysticTopicAnswerTemplates`、`MysticCultureVoice`，主题回答和文化语气从生成器抽出，事实仍由现有盘面传入。
9. `0c86d0c`：契约脚本新增对话分析、软记忆、Provider 校验、舞台标题、消息窗口与输入上限扫描。

## 本轮验证入口

```text
node _dev/authentic_systems_contract_test.js
node _dev/dialogue_contract_test.js
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon --console=plain
```

Gradle 内存紧张时使用项目已验证的低内存参数：

```text
$env:GRADLE_OPTS='-Xmx1024m -XX:MaxMetaspaceSize=384m -XX:ActiveProcessorCount=4'
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --max-workers=2 --console=plain "-Pkotlin.compiler.execution.strategy=in-process"
```

AndroidTest 只需确认可编译：

```text
.\gradlew.bat :app:compileDebugAndroidTestKotlin --no-daemon --max-workers=2 --console=plain "-Pkotlin.compiler.execution.strategy=in-process"
```

本轮最终观测：

- `node _dev/authentic_systems_contract_test.js`：PASS。
- `node _dev/dialogue_contract_test.js`：PASS（45 条 golden）。
- `:app:testDebugUnitTest --rerun-tasks`：BUILD SUCCESSFUL。
- `:app:lintDebug`：BUILD SUCCESSFUL；报告已生成，仍有既有 warning，无 error。
- `:app:assembleDebug`：BUILD SUCCESSFUL；APK 在 `app/build/outputs/apk/debug/app-debug.apk`。
- `:app:compileDebugAndroidTestKotlin`：BUILD SUCCESSFUL；未连接设备执行 instrumented tests。
- lint/assemble 在 Windows 低内存环境使用 `-Dorg.gradle.jvmargs` 限制堆和 `ActiveProcessorCount=2`，避免 Gradle 原生内存峰值；这不改变代码或门禁结果。

## 后续边界

- 默认行为仍是离线；没有新增厂商、密钥、网络请求或占卜体系。
- `MysticFloatingGuide.kt` 中旧绘制 helper 暂留作回滚缓冲，后续确认无引用后再删除。
- 尚未执行手机安装、启动、截图、TalkBack、旋转/返回键、reduced-motion 和 Logcat 复测；等待用户明确通知。
- 合并、推送和远端 CI 不在本交接内自动执行。
