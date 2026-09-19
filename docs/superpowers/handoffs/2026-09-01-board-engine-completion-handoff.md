# 交接文档：棋局剩余任务收尾（2026-09-01）

> 本文覆盖 `2026-09-01-board-engine-handoff.md` §3 里那五项「未完成/显式暂停」的处置结果。
> 那份文档的 §3 清单已过期，以本文为准。**凭据红线不变：对话中出现过的 GitHub 凭据一律 [REDACTED]，严禁写入任何文件、命令或提交。**

## 1. 提交链（分支 codex/system-consistency）

| Commit | 内容 |
|---|---|
| `9e3f267` | Bridge 按会话解析 `SmartBoardEngine`（难度作为会话参数）+ 暴露 `awaitEngine` 驱动异步搜索 + 威胁/难度/观战/残局命令入口 + `Draw` 判和文案（对话与棋盘 UI 同步） |
| `bce4bfb` | 棋盘 UI：难度选择、回放控制（逐手/回到当前）、落子平移动画（reduced-motion 豁免）、自动应手驱动、皮肤联动配色 |
| `aeed6ac` | 续局存档与战绩持久化：`GameArchive` UCI 编解码、按 profileKey 分仓、回放逐手过 `XiangqiRules`、只写棋盘数据 |
| `13535af` | 契约与文档同步：`dialogue_contract.json` 扩至 22 条 golden，README / SYSTEMS_OVERVIEW / TECHNICAL_DEBT / BOARD_GAME_INTEGRATION 补搜索引擎、Draw、威胁、残局、禁吃将 |
| `c094967` | androidTest 交互测试：`app/src/androidTest` 源码集建立、棋盘 testTag、12 项 Compose 用例 + JVM 侧前提锚定 + `board_ui` 契约段 |

§3 逐项结论：Bridge 未接 → 已接；B 组 UI → 已完成（**音效除外**）；存档/战绩 → 已完成；契约/文档 → 已完成；androidTest → 已建立并编译通过，**未在设备执行**；设备实机验证 → 仍按用户指示暂停。

## 2. 仍未做（不是遗漏，是显式边界）

- **落子音效**：仓库无任何音频资源，未接 `SoundPool`，不引入在线素材。需要设计确认音源与许可后再补，当前在 `TECHNICAL_DEBT.md` 与 `BOARD_GAME_INTEGRATION.md` 排障表都写明「未实现」。
- **棋子字体**：系统字体渲染 `XiangqiPieceGlyphs.glyph` 单字，未捆绑字体，**没有缺字可视回退**（`description` 只作 TalkBack 内容描述，不参与绘制）。此前文档里「缺字退回 description」的说法是错的，已在两处更正。
- **设备证据**：`adb devices`、安装、启动、截图、Logcat、TalkBack 焦点顺序、reduced-motion 实机表现全部**未验证**。恢复时重新采集，不要拿工作树里 8 月 31 日的 `device-*.xml` / `ui*.xml` 当新证据。
- **引擎强度上限**：深度 4 层、无置换表/quiescence/迭代加深，不宣称任何等级分。

## 3. 本轮沉淀的坑（下次开工前先看）

1. **Gradle 退出码不能被管道吃掉**：`gradlew ... | tail` 会报 exit 0 而构建实际失败。先 `EXIT=$?` 再打印，或读 `build/` 报告。
2. **UI 断言的前提必须在 JVM 侧可证**：本轮两个「看起来对」的棋盘前提被证伪——初始局面**没有**无路可走的红子（每子至少 1 步），(1,8) 是**另一门炮**的合法落点。设备专属用例的几何/着法前提一律先落到 `BoardUiFixtureTest`，跑不动设备也能在 JVM 失败。
3. **中文记法不要手写**：同 file 双车歧义 + 红黑纵线编号方向相反。测试夹具一律给坐标，让 `XiangqiNotation.format` 生成 record。
4. **Compose 定位用 testTag 不用像素**：`square-$file-$rank` / `difficulty-$level` 直接承载坐标，避免随屏宽漂移；断言不要求可见但点击要求，所以卡片按 `fillMaxWidth(0.6f)` 挂载。
5. **不要把带脉冲动画的局面送进 `waitForIdle()`**：将军的 `rememberInfiniteRepeatable` 光晕会让 idle 永不返回。
6. **`ui-test-manifest` 放 `androidTestImplementation`**：这样不动 app 自己的 debug manifest。

## 4. 快速上手

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
node _dev\dialogue_contract_test.js
# 有设备后（当前未执行）：
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
```

本机最后一次真实执行结果：201 项单测 / 0 失败（棋局相关 153 项）、lint 0 error（77 warning）、两个 APK 均产出、契约 PASS。**以上都只是 JVM/编译证据。**

—— 交接完毕。分支 `codex/system-consistency`，最新 `c094967`。
