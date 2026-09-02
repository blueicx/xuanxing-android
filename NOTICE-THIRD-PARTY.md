# 第三方组件与许可声明（THIRD-PARTY NOTICES）

更新日期：2026-09-01

## 当前随应用分发的第三方组件

| 组件 | 许可 | 说明 |
| --- | --- | --- |
| Kotlin/JVM 标准库 | Apache-2.0 | 通过 Gradle 依赖引入 |
| AndroidX / Jetpack Compose | Apache-2.0 | 通过 Gradle 依赖引入 |
| kotlinx-coroutines | Apache-2.0 | 通过 Gradle 依赖引入 |
| Gson | Apache-2.0 | 通过 Gradle 依赖引入 |
| JUnit 4 | EPL-1.0 | 仅测试 |

以上均为标准 Gradle/Maven 依赖，源码获取方式即对应 Maven Central 坐标。

## 未随当前版本分发的组件（预留 seam）

- **Pikafish（中国象棋 UCI 引擎）**
  - 许可：GPLv3
  - 当前状态：**未打包、未修改、未分发**。仓库内仅有自研 UCI 行协议解析器（`UciProtocolParser`）与降级逻辑（`PikafishEngine` → `OfflineBoardEngine`），不含任何 Pikafish 源码或二进制。
  - 若未来打包：必须随应用提供 GPLv3 完整文本、版本号、上游地址（https://github.com/official-pikafish/pikafish）、修改说明，并按 GPLv3 第 6 条提供对应完整源码（source offer）。

- **Stockfish（国际象棋，预留）**：GPLv3；接入前按 Pikafish 同一流程补全许可与 source offer。

- **KataGo（围棋，预留）**：许可随版本不同（部分版本为 GPL 兼容、部分为 CC BY-NC 等非商用条款）；接入前逐版本核查，非商用许可版本不得进入公开分发渠道。

- **在线大模型 provider**：产品边界决定不接入；棋盘事实、命盘事实、健康与财务边界不交由外部模型。

## 检查清单（release 前）

1. `app/src/main/cpp/` 若存在，确认对应 NOTICE 条目、许可文本与 source offer 齐全。
2. APK 内不得出现无许可声明的 `.so` 或引擎数据文件。
3. 本文件与 `docs/BOARD_GAME_INTEGRATION.md` 的许可章节保持一致。
