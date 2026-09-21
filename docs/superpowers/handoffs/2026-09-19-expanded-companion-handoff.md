# X3 深陪伴扩展交接（2026-09-19）

## 本轮已落地

- 塔罗：随机抽牌保留；新增按档案摘要、日期、问题和牌阵稳定复现的单张/三张/五张抽牌。
- 卢恩：新增稳定单符/三符牌阵，随机抽签与今日符文仍保留。
- 数字命理：新增个人年、月、日周期和计算步骤，保留 11/22/33 大师数。
- 今日行动：饮食增加过敏原、单餐预算、准备时长过滤；活动增加能量/时长约束；卡片增加换一个、采纳、不合适反馈入口。
- 天气/地点：默认离线；隐私页加入显式联网开关；手动城市搜索、Open-Meteo 天气请求和 `geo:` 地图深链；不读取 GPS。
- 对话：增加塔罗、卢恩、数字命理、天气、附近地点、人格测验意图；安全护栏统一拦截医疗/投资结论，禁止凭聊天隐式推断人格。
- 数据：新增按档案哈希隔离的占卜历史和行动反馈 DataStore seam，支持清除；联网同意状态单独保存。
- 文档：更新 README、SYSTEMS_OVERVIEW、设计规格与实施计划。

## 验证证据

- `node _dev/authentic_systems_contract_test.js`：PASS
- `node _dev/dialogue_contract_test.js`：PASS（45 golden entries）
- `node _dev/action_profile_contract_test.js`：PASS
- `:app:testDebugUnitTest`：PASS
- `:app:lintDebug`：PASS
- `:app:assembleDebug`：PASS
- 当前 APK：`app/build/outputs/apk/debug/app-debug.apk`
- 当前 SHA-256：`EE528BEC05B669D87B76350F0D4C88442C7E1FE336DEDC804E25C4A94C0BF606`
- 手机安装：无线调试配对 `192.168.101.38:41003` 成功，设备 `SM-S9310`；`adb install -r` 返回 `Success`。
- 启动核验：`com.xuanji.app/.MainActivity` 获得焦点；最近 300 行 Logcat 未观察到 `FATAL EXCEPTION` 或 `AndroidRuntime`。
- 悬浮球崩溃根因：Xperia XZ2 上点击浮球后，舞台外壳的 `verticalScroll` 与沉浸式 `MysticGuideCard` 的滚动容器嵌套，触发 Compose 的无限高度约束异常。
- 修复验证：移除舞台外壳滚动，仅保留沉浸式对话卡滚动；Xperia XZ2（Android 15，`adb-QV7017NH1F-yD13DI._adb-tls-connect._tcp`）安装时间 `2026-09-22 01:56:24`，点击浮球后舞台正常显示，应用保持前台，未观察到 `com.xuanji.app` 崩溃。

## 暂缓项

- 视觉截图、TalkBack、reduced-motion 和旋转/返回路径的完整手机复测仍需单独执行。
- 在线 provider 仅实现 Open-Meteo seam，不接入大模型、地图瓦片、GPS 或密钥。
- 健康/投资仍只提供安全边界与一般信息入口，不做诊断、处方、买卖和收益结论。

## 联网政策备注

- Open-Meteo forecast/geocoding 使用公开 HTTP API；UI 只在用户点击时请求并展示来源。
- 若未来使用 OSM/Nominatim，必须继续遵守手动触发、限速、缓存、User-Agent、署名和禁止自动补全等政策。
