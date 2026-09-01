# 玄星 · XuanXing（Android）

[![Build Debug APK](https://github.com/blueicx/xuanxing-android/actions/workflows/build.yml/badge.svg)](https://github.com/blueicx/xuanxing-android/actions/workflows/build.yml)

一款**离线优先**的玄学与自我探索 App，涵盖东方命理、西方占星、世界多元占卜与性格测试。
所有推算与解读均在**设备本地完成，无需联网**，也不收集任何个人数据。

> 项目早期曾用名「玄机」，包名保留为 `com.xuanji.app`。

## 功能概览

- **综合 / 东方（五行八字）**
  - 由出生时间推算八字四柱（节气采用经验表，结果明确标注为离线近似）
  - 五行分布可视化（金木水火土，缺项高亮）、日主、生肖、喜用神
  - 每日运势：综合 / 事业 / 财运 / 感情 / 健康 五维评分 + 今日干支 + 幸运色 / 方位 + 建议
- **西方（星座）**
  - 由出生月日判定星座、属性、日期范围、性格特质
  - 每日运势：五维评分 + 幸运数字 / 颜色 / 方位 + 当日寄语
- **占卜（世界多元体系）**
  - 顶级板块：亚洲 / 非洲 / 欧洲 / 美洲 / 近现代新兴 / 常用占卜
  - 含紫微斗数（中州 / 北 / 闽三派合一）、奇门、风水、六爻、易经、塔罗、水晶球、灵签抽签等数十个体系
  - 全手绘专属图标 + 摇签 / 翻书 / 贝壳旋转等动态效果
- **测试（性格 · 职业 · 趣味）**
  - MBTI、大五人格、卡特尔 16PF、MMPI、霍兰德 RIASEC、霍格沃兹学院
  - 趣味合集：九型人格 / DISC / 性格色彩 / 菲尔人格 / 颜色心理 / 瑞文智力 / 动物 · 美食 · 影视 · 颜色等
  - 确定性计分，本地保存测试记录
- **历史 · 同月同日生**：按你的出生日期精确匹配历史上的今天诞生的名人
- **人物交流 · 中国象棋**：在综合/东方/西方页的对话面板中与角色真实对弈中国象棋
  - 纯 Kotlin 规则核心：合法走法、吃子、将军/将死/困毙、三次重复与限着判和，全部本地校验
  - 本地 alpha-beta 搜索引擎（难度=2/3/4 层），走子后自动应手；不显示胜率/等级分
  - 角色解说只引用真实棋盘事件（棋子/坐标/吃子/胜负），不结合运势下结论
  - 对话可直接说「换个难度」「观战」「有哪些威胁」「来局残局」「重做」「保存棋局」「继续棋局」「战绩」
  - 续局存档与战绩落 DataStore（按档案指纹分仓），只存局面与棋谱，角色评语不入库
  - 棋盘 9×10 可点选走子，系统动画时长缩放为 0 时自动跳过落子滑动动画，控件 ≥48dp 并带 TalkBack 描述
  - 围棋与国际象棋仅预留接口，明确显示「尚未启用」而不伪造棋盘
- **我的 · 命盘设置**：出生日期 / 时间 / 地点编辑并保存（DataStore 本地存储）
- **每日提醒**：WorkManager 每日推送「今日运势已更新」通知
- 深色神秘主题（紫 + 流金），Material 3，沉浸式状态栏 / 导航栏

## 技术栈

- Kotlin + Jetpack Compose（Material 3）
- MVVM（ViewModel + StateFlow）+ 单向数据流
- DataStore（Preferences）做离线存储与每日运势缓存（按日期 + 命盘指纹缓存，结果稳定）
- WorkManager 每日提醒
- 最低 SDK 26（Android 8.0），目标 / 编译 SDK 34
- Compose BOM 2024.02.00，Kotlin 1.9.22，Compose 编译器 1.5.8

## 如何运行

需要用 **Android Studio（Hedgehog 或更新版本）** 打开本项目并运行。

1. 安装 [Android Studio](https://developer.android.com/studio) 与 Android SDK（API 34）。
2. 打开 Android Studio → `File / Open` → 选择 `xuanji-android` 目录。
3. 首次打开会触发 Gradle 同步（已内置 Gradle Wrapper，无需本机预装 Gradle）。
4. 连接安卓设备或启动模拟器（API 26+）。
5. 点击 ▶ Run，或执行 `./gradlew assembleDebug`（Windows 用 `gradlew.bat`）。

> 应用完全离线运行，无需任何 API Key 或后端服务。

历法与测验数据边界：农历/闰月采用 1900–2100 表驱动换算；Ifá 256 组合、纳迪叶文和心理测验均遵循来源与授权标记，默认离线，不伪造经文、叶文或商业量表常模。

棋局边界：中国象棋规则与本地 alpha-beta 搜索完全离线运行，续局存档与战绩只存本机 DataStore；角色话术只基于棋盘事实。Pikafish 原生引擎、围棋 GTP、国际象棋 UCI 均为预留接口，未打包二进制、不联网；接入前需完成对应开源许可（GPLv3 等）与 source offer（见 `NOTICE-THIRD-PARTY.md` 与 `docs/BOARD_GAME_INTEGRATION.md`）。

## 自动构建（CI）

每次 push 到 `main` 或发起 PR 时，[GitHub Actions](https://github.com/blueicx/xuanxing-android/actions) 会自动执行 `.github/workflows/build.yml`：安装 JDK 17 + Android SDK 34、构建并上传 debug APK。构建产物 `xuanxing-debug-apk` 可在对应 Actions 任务页下载（保留 30 天）。

## 目录结构

```
app/src/main/java/com/xuanji/app/
├── domain/          # 八字 / 星座 / 占卜 / 测试 推算（纯 Kotlin，可单测）
├── data/            # model / DataStore / Repository
├── ui/              # theme / 导航 / 各板块 / 公共组件 / 图标
├── daily/           # WorkManager 每日提醒
├── XuanjiApplication.kt
└── MainActivity.kt
```

## 隐私

本应用**不联网、不上传、不收集**任何用户数据。所有命盘、运势、占卜与测试结果均在本地计算并仅存储于本机 DataStore。

## 许可证

[MIT License](LICENSE) © 2026 吴家希（WJX）
