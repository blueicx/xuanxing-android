# X3 深陪伴扩展实现计划

> 本计划在现有 dirty tree 上增量执行。所有既有修改、截图、dump 与交接文件均保留；只对明确列出的源码/测试/文档文件做编辑。

## 波次 1：领域模型与确定性占卜

1. 新增 `domain/divination/DivinationQuery.kt`、`DeterministicDraw.kt` 和 `DivinationHistory.kt`。
2. 为塔罗、卢恩新增稳定牌阵接口：单张、三张、五张；结果携带 seed、算法版本、位置和免责声明。
3. 为数字命理新增个人年/月/日周期、计算步骤和英文/拼音姓名输入校验；保留大师数规则并标注文化娱乐性质。
4. 新增 JVM 测试验证相同输入一致、不同问题可区分、牌阵不重复、历史摘要不泄露原始姓名。

## 波次 2：今日行动与画像扩展

1. 扩展 `FoodPreference`、`DailyActionInput` 增加预算、准备时间、能量和过敏/忌口字段，修正过滤顺序。
2. 新增 `ActionFeedback`、`ActionHistoryStore` 及纯 Kotlin reducer；反馈只保存用户主动选择，不推断用户性格。
3. 在今日行动卡中加入具体食材、替代菜、时间/预算标签和“换一个/已采纳/不合适”操作；周/月/年不挂载该卡。
4. 画像增加显式输入来源与场景优先级模型，地区结果继续以匹配示例显示。

## 波次 3：天气/地点 Provider seam

1. 新增 `external/ExternalContextProvider.kt`、`OfflineContextProvider.kt`、`OpenMeteoContextProvider.kt`、`ExternalContextConsent.kt`。
2. 只支持用户手动城市搜索；默认无网络、无定位。网络开关关闭或请求失败时返回缓存/离线目录结果。
3. 在 Manifest 增加 `INTERNET`，不增加定位权限；隐私页提供同意、撤回、缓存清除。
4. 增加网络 provider 的 URL、超时、错误映射和 JSON 解析测试；不在测试中访问真实网络。

## 波次 4：安全与对话接入

1. 新增 `SafetyResponseGuard` 和 `PersonalitySource`，将健康/投资/人格边界统一到 DialogueProvider 与离线引擎。
2. 扩展意图枚举与模板，支持“抽塔罗/抽符文/算数字/今日吃什么/天气/附近去哪”等明确请求。
3. provider 结果必须先通过本地事实/安全校验，失败回退离线；session token 过期结果丢弃。
4. 新增 golden matrix：核心回答、澄清入口、多主题、旧请求、空输入、超长输入、健康/投资拒答、人格来源。

## 波次 5：UI 与文档

1. 在占卜页增加可解释结果与历史摘要；在 Profile 隐私页加入联网同意与清除入口。
2. 更新 README、SYSTEMS_OVERVIEW、技术债和交接文档，明确默认离线和安全边界。
3. 运行三条 Android gate 与契约测试，记录失败原因；手机安装、截图、TalkBack 延后到用户明确通知。

## 文件清单

实现文件：

- `app/src/main/java/com/xuanji/app/domain/divination/DivinationQuery.kt`
- `app/src/main/java/com/xuanji/app/domain/divination/DeterministicDraw.kt`
- `app/src/main/java/com/xuanji/app/domain/divination/DivinationHistory.kt`
- `app/src/main/java/com/xuanji/app/domain/action/ActionFeedback.kt`
- `app/src/main/java/com/xuanji/app/domain/external/*`
- `app/src/main/java/com/xuanji/app/domain/SafetyResponseGuard.kt`
- 对话与现有 UI 的最小必要接入文件

测试文件：

- `app/src/test/kotlin/com/xuanji/app/domain/divination/*Test.kt`
- `app/src/test/kotlin/com/xuanji/app/domain/action/*Test.kt`
- `app/src/test/kotlin/com/xuanji/app/domain/external/*Test.kt`
- `app/src/test/kotlin/com/xuanji/app/domain/SafetyResponseGuardTest.kt`

文档文件：

- `README.md`
- `docs/SYSTEMS_OVERVIEW.md`
- `docs/superpowers/handoffs/2026-09-19-expanded-companion-handoff.md`
