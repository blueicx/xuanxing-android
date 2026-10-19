# 离线城市温度参考 v2

核对日期：2026-10-15（Asia/Shanghai）。应用运行时不联网。

目录包含全部 24 个城市：20 个 JMA 测站、1 个 KNMI 测站和 3 个 NASA POWER 网格参考点，均使用 1991–2020 基准期。应用运行时不请求这些服务。版本：`climate-excerpts-2026-10-15-v2`。

JMA 来源为 [ClimatView](https://www.data.jma.go.jp/tcc/tcc/products/climate/climatview/frame.php) 月温度常年值；[产品说明](https://www.data.jma.go.jp/tcc/tcc/products/climate/climatview/outline.html) 指明基准期与 CLIMAT 来源。上海为宝山，成都为温江，温哥华/墨尔本/惠灵顿/新加坡为具名机场，不宣称代表全市。温江与成都市的行政关系另核对[成都市数据门户区县目录](https://www.chengdu.gov.cn/data/oportal/index)。只摘录数值，不复制图片、段落、GPV 或标志。

阿姆斯特丹地区使用 KNMI 的 [Schiphol 240 常年值表](https://cdn.knmi.nl/knmi/map/page/klimatologie/klimaatatlas/tabel/stationsdata/klimtab_9120_240.pdf)，取 Temperatuur / gemiddeld 第一列，不取平均最低或最高温。机场不是市中心，页面明确说明这一限制。

苏州、深圳和厦门使用 [NASA POWER 自定义气候统计 API](https://power.larc.nasa.gov/docs/services/api/temporal/climatology/)，参数 `T2M`、`start=1991&end=2020`。实际响应核对 `header.range` 为 January 1991–December 2020、`sources` 为 MERRA2、单位 C、12 个月均有效，未采用默认统计期。按[官方方法说明](https://power.larc.nasa.gov/docs/methodology/meteorology/)，这是 0.5°×0.625° 网格的再分析估计、地面以上 2 米气温，不是当地测站实测。坐标是本项目选择的静态城市参考点，非用户位置、非精确中心或全市平均；请求地址随记录显示。

| 城市 | 来源/标识 | 参考点或测站 | 1月至12月的月均温 °C |
|---|---|---|---|
| 成都 | JMA 56187 | WENJIANG | 5.7, 8.0, 12.6, 17.6, 21.4, 24.1, 25.6, 25.4, 21.7, 17.3, 12.3, 7.0 |
| 西安 | JMA 57036 | XI'AN | 0.5, 4.0, 9.4, 15.9, 20.9, 25.8, 27.7, 25.4, 20.7, 14.2, 7.4, 1.7 |
| 阿姆斯特丹地区 | KNMI 240 | Schiphol | 3.8, 4.1, 6.5, 9.8, 13.3, 16.0, 18.1, 18.0, 15.1, 11.3, 7.4, 4.6 |
| 苏州 | NASA POWER | 31.30°N 120.58°E | 3.29, 5.32, 9.49, 15.42, 21.08, 24.87, 28.30, 28.13, 23.93, 18.33, 12.24, 5.59 |
| 深圳 | NASA POWER | 22.55°N 114.06°E | 16.12, 17.52, 20.06, 23.15, 26.19, 27.95, 28.50, 28.50, 27.64, 25.14, 21.64, 17.35 |
| 厦门 | NASA POWER | 24.48°N 118.08°E | 13.08, 13.98, 16.50, 20.35, 23.81, 26.44, 27.74, 27.65, 26.37, 22.98, 19.30, 14.89 |

全部来源、数值和数据类型见 `app/src/main/java/com/xuanji/app/domain/action/OfflineRegionComparison.kt`。不要只验证 HTTP 200 或月份数量：JMA 某些无效 n 参数会返回 WAKKANAI 默认站，应核对实际标题、国家和坐标，不能把默认站冒作请求城市。本次六城先有缺失回归失败，再补录与验证。

## 自编处理规则

- 12 个已发布月均温的简单平均用于对比，明确不是按各月天数加权的年均温。
- “温暖”定义为该简单平均 ≥18°C，“凉爽”定义为 ≤12°C；这是项目筛选阈值，不是 JMA 气候分类。
- 气候匹配、未匹配、缺失分别显示；相同输入按 cityKey 稳定排序。
- 同基准期不等于同测量方法：测站与网格只作粗略阈值参考，不据小数差异产生精确城市优劣名次。页面明确显示类型，全部城市可展开，单城依据按需展开。
- 不改变或累计五行/星座文化匹配分。生活节奏及城市预算缺少可比事实数据，标为资料不足，不参与排序，不编造物价。
- 不显示实时天气、未来气候、旅游安全、收入或迁居结论。历史测站常年值不是个体体感保证。

## 归属与条款

归属：根据 JMA ClimatView 数值整理，比较规则由本项目编写，不代表 JMA 认可。

已核对 [JMA 使用条款](https://www.jma.go.jp/jma/en/copyright.html) 的来源/编辑标注要求，及 [TCC 版权和免责](https://ds.data.jma.go.jp/tcc/tcc/notice.html)。TCC 单独说明 GPV 访问权限；本功能不获取 GPV。保留来源与修改说明，不使用机构名称制造官方背书。新增数据前须继续核对对应来源和适用限制。

KNMI：按其[版权说明](https://www.knmi.nl/copyright)署名 Source: KNMI；本项目摘录与阈值比较不是 KNMI 产品或背书，未复制机构标志。

NASA：[官方数据目录](https://catalog.data.gov/dataset/prediction-of-worldwide-energy-resources-power)标明公开数据及致谢要求。归属：These data were obtained from the NASA Langley Research Center (LaRC) POWER Project funded through the NASA Earth Science/Applied Science Program. 本项目只保存离线数值摘录与来源链接，不使用机构标志或宣称官方推荐。
