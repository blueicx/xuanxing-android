# X3 文化皮肤与象棋入口设计规格

## 目标

把玄师舞台从“同一套 Canvas 人形换色”升级为三套真正可辨识、可切换、离线可用的文化视觉：江南水墨、星象学院、沙海守望者。同时让已接入的真实中国象棋在对话面板中有明确可发现的入口。

## 用户体验

- 舞台左上角继续保留服饰/文化切换控件；切换后立即更新人物轮廓、脸部风格、姿态、服饰、道具与背景。
- 玄学家/半仙两个 persona 仍是独立维度，不因文化皮肤改名；舞台标题仍只显示“玄学家”或“半仙”。
- 江南水墨使用老年玄学家、折扇、手杖和水榭；星象学院使用现代中性二次元人物、星象符和观测台；沙海守望者使用低多边形面具人物、几何斗篷、星盘和沙漠观测台。
- 皮肤切换不改变命盘、运势分数、对话 seed、记忆或棋局状态。
- 对话快捷区新增“来一盘象棋”；点击后直接走现有 `GameDialogueBridge`，出现真实 `GameBoardCard`。围棋和国际象棋继续明确显示为未启用，不伪造棋盘。

## 资源与渲染架构

1. `MysticSkin` 增加稳定的 `visualStyleId`，只允许 `ink_scholar`、`cel_astrologer`、`lowpoly_guardian` 三个值。现有八个皮肤 ID 保留，按文化归入三种视觉风格。
2. 新增 `MysticFigureAsset` UI 映射，将三个视觉风格映射到本地透明 WebP 角色资源；资源使用 `drawable-nodpi`，不联网、不依赖设备字体。
3. `MysticFigureCanvas` 改为渲染分发器：三种资源存在时用 `Image` 以 `ContentScale.Fit` 显示；资源读取失败时回退到现有 Canvas 人形，保证旧 APK/资源异常时舞台仍可用。
4. `MysticStageLayout` 增大人物展示区域并保持 safe-area；动画只作用于舞台微光/轻微透明度，角色资源本身不做位移。reduced-motion 下静态显示。
5. `MysticCultureBackdrop` 保留并按皮肤继续切换场景；角色资源与背景分离，避免把整张概念图硬裁成舞台人物。

## 皮肤映射

| 既有 skin ID | 文化表现 | visualStyleId |
| --- | --- | --- |
| `jiangnan-robe`、`cloud-daoist` | 江南/道观水墨系 | `ink_scholar` |
| `academy-gown`、`street-jacket`、`festival-costume` | 星象学院/城市节庆图形系 | `cel_astrologer` |
| `silkroad-robe`、`northland-mantle`、`desert-traveler` | 丝路/北境/沙海几何系 | `lowpoly_guardian` |

## 数据与状态边界

- `visualStyleId` 只参与渲染选择，不进入 `MysticDialogueEngine` 的 deterministic seed。
- 皮肤切换仍通过现有 `stageCostumeRequest`；旧异步对话 token 规则不变。
- 象棋快捷入口只发送已有游戏命令文本，游戏状态仍由 `GameSessionState` reducer 管理，角色文案不能改变盘面事实。

## 验收与测试

- Node 契约测试钉住三个视觉 ID、每个皮肤映射、三份 drawable 资源和“来一盘象棋”快捷入口。
- JVM 测试验证皮肤视觉 ID 只取三值、不同文化 ID 映射到不同风格、游戏快捷文本分类为 `MysticIntent.Game`。
- `:app:testDebugUnitTest`、`:app:lintDebug`、`:app:assembleDebug` 和三个 Node contract test 全部通过。
- 设备验收在用户通知后执行：打开舞台、逐一切换三种文化、关闭舞台、点击“来一盘象棋”，记录截图与 Logcat；本轮不把未执行的 TalkBack/旋转测试写成已完成。

