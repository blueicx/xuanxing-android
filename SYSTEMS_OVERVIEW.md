# 玄机 App · 全球玄学体系补全总览

2026-10-19：现实推荐条件、库存优先、主动日记与周月回顾已接入；当前架构与协议见[系统概览](docs/SYSTEMS_OVERVIEW.md)。Debug 版本 1.1.0（2），总计划仍有未完成项；本轮用户已授权自动门禁后安装与推送，以下“未安装”描述是历史状态。

2026-10-16：日常安排/库存、软记忆有效期、备份迁移和构建清单的当前实现与门禁见[日常扩充交接](docs/superpowers/handoffs/2026-10-16-everyday-expansion-handoff.md)。以下体系清单继续作为历史说明。

本文件保留早期体系补全记录，不作为当前构建或设备状态。最新架构以 [系统概览](docs/SYSTEMS_OVERVIEW.md) 和 [会话与工程交接](docs/superpowers/handoffs/2026-10-15-thread-safety-and-lint.md) 为准。待安装 APK 与哈希以最新交接为准；本轮只验证隔离模拟器，没有连接或安装手机。

角色舞台状态：**已统一场景实现，设备视觉待复测**（四角色固定走同一五层舞台；JVM/Compose 编译门禁可执行，真机/TalkBack/Logcat 仍需单独验收）

## 本次新增 / 完成的体系（共 12 项）

| 体系 | 算法要点 | 主要文件 |
|---|---|---|
| 六爻（增强） | 问题输入框 + 用神(六亲)选择 + 现代解说 | LiuYao.kt / LiuYaoViewModel / LiuYaoScreen |
| 梅花易数 | 年月日时起卦、先天卦数、互卦变卦、体用生克；含本命卦 | domain/divination/MeiHua.kt + ViewModel + Screen |
| 奇门遁甲 | 阴阳遁局（节气三元表）、九宫飞盘、值符值使八门九星八神 | domain/divination/QiMen.kt + ViewModel + Screen(九宫格) |
| 七政四余 | 日月五星(复用 ZodiacCalculator) + 罗睺/计都/月孛/紫气，落黄道十二宫 | domain/divination/QiZheng.kt + ViewModel + Screen |
| 风水八宅 | 命卦(东四/西四) + 坐山伏位排八游星吉凶方位 | domain/divination/FengShui.kt + ViewModel + Screen |
| 玛雅卓尔金历 | 260 天 Tzolk'in(1-13×20日名) + Haab；当前日与生日 | domain/divination/MayaTzolkin.kt + ViewModel + Screen |
| 雷诺曼 | 36 张符号牌，抽 3 张(过去/现在/未来) | domain/divination/Lenormand.kt + ViewModel + Screen |
| 脉轮 | 七大能量中心(位置/颜色/意涵/失衡) | domain/divination/Chakra.kt + ViewModel + Screen |
| 古典占星 | 希腊(十度区间·庙旺落陷·幸运点) / 波斯(阿拉伯点) / 巴比伦(星象预兆) 三框架切换 | domain/divination/ClassicalAstrology.kt + ViewModel + Screen |
| 人类图（近似） | 行星黄经→64卦之门→中心定义→类型/权威（明确标注近似） | domain/divination/HumanDesign.kt + ViewModel + Screen |
| 资料卡通用系统 | 19 个不可离线算法化体系，顶部醒目「不可离线算法化」横幅 | res/raw/systems_reference.json + ReferenceRepository/ViewModel/Screen |
| 占卜导航 | DivinationHub 全部条目已接可点击路由（无「规划中」残留） | DivinationHub.kt / XuanjiApp.kt |

## 共用基础
- `GuaCommons.kt`：八卦/六十四卦名/五行生克复用层（梅花易数等使用）
- 复用 `ZodiacCalculator.calculateNatalChart` 的已验证本命星盘（七政四余、古典占星、人类图）
- 需生日的体系统一从 `AppModule.repository.userProfileFlow` 读取；出生信息默认空白，由用户主动填写。

## 明确的近似说明（已写入各屏「说明」）
- 行星为平黄经近似、奇门为简化飞布（未拆补置闰）、人类图为离线简化映射、资料卡体系仅作文化介绍不含测算。全部标注「仅供文化娱乐参考」。

## 待办（设备重连后）
1. `adb install -r app-debug.apk`
2. UIAutomator 逐屏截图：梅花易数 / 奇门 / 七政四余 / 风水 / 玛雅 / 雷诺曼 / 脉轮 / 古典占星 / 人类图 / 资料卡(各一类)
3. 确认无崩溃、各屏正常渲染
