# 今日行动与人生画像实现交接

日期：2026-09-19  
分支：`codex/system-consistency`

## 已完成

- 纯 Kotlin `domain/action` 模块：
  - `DailyActionPlanner`：五行 0.40、星座 0.25、今日盘面 0.20、季节/城市 0.15。
  - `LifeProfilePlanner`：职业、色彩、环境和最多 3 个国家/城市匹配示例。
  - `ActionModels`、静态食物/城市目录、稳定 hash 同分排序。
- 本地饮食偏好：素食、清真、忌辣、忌酒、自定义排除项；键名按 `food_preference_<sha256(profileKey)>` 隔离。
- 综合页“今日行动”可折叠卡：吃什么、做什么、去哪玩；显示具体菜名、替换项、外卖关键词、时段、分数、依据和边界。
- “我的”页人生画像：职业倾向、色彩灵感、环境节奏、地区匹配示例和置信度。
- 对话复用同一 `DailyActionPlan` / `LifeProfile`：支持“今天早餐吃什么”“今天做什么”“今天去哪玩”“什么工作适合我”“哪个城市适合我”等问法。
- 双端契约：`_dev/action_profile_contract.json` / `_dev/action_profile_contract_test.js`。
- 文档同步：README、系统概览、技术债台账。

## 本轮提交

```text
5140766 feat(行动): 建立今日行动与人生画像领域契约
d3d8cd7 feat(行动): 按五行星座生成具体饮食行动建议
6aecfcd feat(画像): 增加职业色彩与地区匹配
3ee0c8b feat(偏好): 持久化按档案隔离的饮食设置
5523992 feat(数据层): 组合今日行动与人生画像输入
00139f9 feat(综合页): 展示可解释的今日行动卡
30f2621 feat(我的): 增加职业色彩与地区人生画像
5dbb0b0 feat(对话): 复用今日行动与人生画像事实
256cf7d docs(契约): 固化行动与画像双端边界
34d32e1 chore(行动): 收敛稳定排序类型签名
```

## 代码级验收证据

以下命令在本机工作树执行并通过：

```text
./gradlew :app:testDebugUnitTest --rerun-tasks       BUILD SUCCESSFUL
./gradlew :app:lintDebug                            BUILD SUCCESSFUL
./gradlew :app:assembleDebug                        BUILD SUCCESSFUL
./gradlew :app:compileDebugAndroidTestKotlin        BUILD SUCCESSFUL
node _dev/action_profile_contract_test.js           PASS (8 meals, offline, deterministic tie-break)
node _dev/authentic_systems_contract_test.js        PASS
node _dev/dialogue_contract_test.js                 PASS (45 golden entries)
git diff --check                                    PASS
```

Debug APK：`app/build/outputs/apk/debug/app-debug.apk`  
SHA-256：`ED54776F5EDD5B4222175BAB25FFA6CC3EE6BA537D3E4A93CF10211475A42A7D`  
大小：23,845,578 bytes

## 运行边界

- 默认完全离线；没有 GPS、网络、天气、地图或外部模型调用。
- 手动城市只用于静态标签匹配；空城市按环境类型降级，不伪造附近地点。
- 食物是生活方式灵感，不是营养、过敏或医疗建议。
- 职业是兴趣/倾向簇，不是能力评估、招聘建议或心理诊断。
- 国家/城市是环境匹配示例，不是迁居、签证、收入、旅行安全或“最适合”结论。
- 测试记录只读取应用本地已保存的结果；没有相关测试时 `LifeProfile.confidence=Low` 并列出缺失输入。
- 主结果不使用随机数；稳定 hash 只在匹配分完全相同时排序。

## 尚未执行

按用户要求，本轮没有执行手机安装、启动、`adb devices`、Logcat、真实截图、旋转/返回键、TalkBack、reduced-motion 或窄屏溢出验收。上述代码级门禁不能替代真实设备证据；等用户通知后再使用包名 `com.xuanji.app` 做设备复测。

工作树中的既有截图、UI dump、推广图、`.superpowers` 视觉资料和旧交接文档均未清理或覆盖。
