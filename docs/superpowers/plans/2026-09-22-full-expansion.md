# X3 玄师全量扩充实施计划

## 波次

1. 纯 Kotlin 角色语气/专长适配与 40 条相关性回归，扩展 `DialogueContext`。
2. 线程快照 DataStore、单角色/全部清除和进程恢复测试。
3. 依据模型、来源标签、隐私导出/清除入口。
4. 每日行动具体菜名、替代项、预算和反馈；人生画像仅显示匹配依据。
5. 四角色游戏契约与三个新本地引擎；象棋复用现有真实引擎。
6. 真正 Horizontal Pager、气泡折叠、reduced-motion、窄屏和 TalkBack 语义。
7. 大文件新 seam、契约/JVM/lint/APK/androidTest 编译和设备验收记录。

## 每波验收

- 先添加会失败的 JVM/契约测试，再实现。
- 每波通过 `:app:testDebugUnitTest`；最终增加 `lintDebug`、`assembleDebug`、`:app:assembleDebugAndroidTest` 和 Node 契约测试。
- 设备视觉、TalkBack、触控和 Logcat 必须有单独证据；没有证据时标记未验证。

## 回滚边界

保留 `fba5ee2` 基线及用户已有未跟踪素材；新增域模块可以独立移除，不修改既有盘面算法和默认离线 provider。
