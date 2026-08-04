# 贡献指南 · Contributing to 玄星 XuanXing

感谢你有兴趣为「玄星」做贡献！这是一个**完全离线、零联网、零数据收集**的玄学与自我探索 App，欢迎提交 Bug 修复、新占卜/测试体系、UI 改进或文档完善。

## 行为准则

请保持友善、尊重。我们不接受任何形式的骚扰、歧视或攻击性言论。有异议就就事论事地讨论。

## 开发环境

- **Android Studio**：Hedgehog（2023.1）或更新版本
- **JDK**：17（项目 `jvmTarget = "17"`）
- **Android SDK**：API 34（平台 34 + build-tools 34.0.0）
- **Gradle**：无需预装，仓库已内置 Gradle Wrapper（8.9）
- **Kotlin**：1.9.22 / Compose 编译器 1.5.10 / Compose BOM 2024.02.00

首次用 Android Studio 打开 `xuanji-android` 目录，会自动触发 Gradle 同步。

## 本地构建

```bash
# 调试包
./gradlew assembleDebug        # macOS / Linux
gradlew.bat assembleDebug      # Windows

# 安装到已连接设备
./gradlew installDebug
```

也可以在 Android Studio 直接点 ▶ Run。

## 提交 Issue

提 Issue 前请先搜索是否已有相同或相关的问题。一个高质量的 Issue 应包含：

- **复现步骤**（能稳定触发的最小操作路径）
- **期望行为** 与 **实际行为**
- **设备 / 系统版本 / App 版本**
- 相关截图或日志（如有）

## 提交 Pull Request

1. Fork 本仓库，从 `main` 切出特性分支：`git checkout -b feat/你的改动`
2. 保持改动**小而聚焦**——一个 PR 解决一件事。
3. 确保你的代码能通过本地构建：`./gradlew assembleDebug`
4. 推送分支并发起 PR 到 `main`，在描述里说明**为什么**这么改。

### 提交信息规范

采用约定式提交（Conventional Commits）风格，方便以后生成 changelog：

```
<type>(<scope>): <简短中文/英文描述>

# type: feat / fix / docs / style / refactor / test / chore
# scope 可选：bazi / western / divination / test / ui / ci ...
```

示例：

- `feat(divination): 新增玛雅卓尔金历解读`
- `fix(ui): 修复健康人体图骨/皮肤坐标偏移`
- `docs: 补充 README 目录结构说明`

### 代码风格

- Kotlin 遵循 [Kotlin 官方编码规范](https://kotlinlang.org/docs/coding-conventions.html)。
- Compose 组件采用 **MVVM + StateFlow** 单向数据流，业务逻辑尽量下沉到 `domain/`（纯 Kotlin，便于单测）。
- 新增占卜/测试体系时，请**配套手绘专属图标**（参考 `ui/components/DivinationIcons*.kt` / `TestIcons.kt`），并补充六维解读文本。

## 项目核心原则（请务必遵守）

1. **离线优先**：所有推算、运势、占卜、测试都必须在设备本地完成，不调用任何后端、不联网、不依赖外部 API。
2. **隐私零收集**：不得引入任何会读取、上传用户数据的 SDK（如统计、广告、崩溃上报需联网的）。
3. **确定性**：占卜/测试结果必须可复现（按日期+命盘做种子），禁止用 `Random` 产生不同结果。
4. **不要在仓库里提交**：APK、`build/` 产物、`local.properties`（含本地 SDK 路径）、截图、个人文件（如微信收款码 `donate_qrcode.jpg`）——这些已被 `.gitignore` 排除。

## 许可证

贡献即表示你同意你的代码以 **MIT License** 发布。
