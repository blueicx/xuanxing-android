package com.xuanji.app.domain.divination

import kotlin.math.atan2
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 瓦斯图（Vastu Shastra）建筑能量分析，确定性计算。
 *
 * 基于用户的 Python 基础实现移植并完善：
 *  - 八方向（东/东南/南/西南/西/西北/北/东北）及其元素、理想功能；
 *  - Vastu Purusha Mandala（VPM）3×3 网格能量分区（中心 Brahmasthan 神圣）；
 *  - 房间合规检查：房间中心所在方向 × 该房间推荐方向；
 *  - 综合评分（满分 100，违规按严重性扣分）与等级、详细解读、改进建议。
 * 全部离线、确定性，无随机，仅供文化娱乐参考。
 */

// ======================== 数据 ========================

/** 八方向名称（索引 0=东 顺时针） */
val VASTU_DIRECTIONS = listOf(
    "东 (East)", "东南 (South-East / Agneya)", "南 (South / Yama)", "西南 (South-West / Nairutya)",
    "西 (West / Varuna)", "西北 (North-West / Vayavya)", "北 (North / Kubera)", "东北 (North-East / Ishanya)"
)

/** 方向元素（中文） */
private val DIRECTION_ELEMENT = mapOf(
    "东" to "风", "东南" to "火", "南" to "火", "西南" to "土",
    "西" to "水", "西北" to "风", "北" to "水", "东北" to "空"
)

/** 方向元素（展示长名） */
private val DIRECTION_ELEMENT_FULL = mapOf(
    "东" to "风 (Air/Vayu)", "东南" to "火 (Fire/Agni)", "南" to "火 (Fire/Agni)", "西南" to "土 (Earth/Prithvi)",
    "西" to "水 (Water/Jala)", "西北" to "风 (Air/Vayu)", "北" to "水 (Water/Jala)", "东北" to "空 (Space/Akasha)"
)

/** 方向理想功能 */
private val DIRECTION_FUNCTION = mapOf(
    "东" to "主入口、客厅、祈祷室（晨光最佳）",
    "东南" to "厨房、火源、电器设备、电力房",
    "南" to "卧室、主卧（西南更佳）、储藏室",
    "西南" to "主卧室、重型家具、保险柜",
    "西" to "餐厅、书房、儿童房、楼梯",
    "西北" to "浴室、卫生间、客房、储藏室",
    "北" to "客厅、书房、冥想室、水元素（水箱）",
    "东北" to "祈祷室（Puja Room）、水井/水箱、冥想空间"
)

/** 房间类型 → 推荐方向 */
private val ROOM_PLACEMENT = mapOf(
    "主入口" to listOf("东", "北", "东北"),
    "厨房" to listOf("东南", "西北"),
    "主卧室" to listOf("西南", "南"),
    "客厅" to listOf("东", "北", "东北"),
    "祈祷室" to listOf("东北", "东"),
    "浴室" to listOf("西北", "西"),
    "餐厅" to listOf("西", "东南"),
    "书房" to listOf("西", "北"),
    "楼梯" to listOf("西", "南", "西北"),
    "水箱" to listOf("东北", "北"),
    "保险柜" to listOf("西南", "南")
)

/** VPM 3×3 网格（row: 0=北侧(西南角为原点则 y 大), col: 0=西侧） */
private data class ZoneInfo(val direction: String, val element: String, val luck: String)

private val VPM_ZONES: Map<Pair<Int, Int>, ZoneInfo> = mapOf(
    (0 to 0) to ZoneInfo("西北", "风", "中"),
    (0 to 1) to ZoneInfo("北", "水", "高"),
    (0 to 2) to ZoneInfo("东北", "空", "极高"),
    (1 to 0) to ZoneInfo("西", "水", "中"),
    (1 to 1) to ZoneInfo("中心 (Brahmasthan)", "空", "神圣"),
    (1 to 2) to ZoneInfo("东", "风", "高"),
    (2 to 0) to ZoneInfo("西南", "土", "中"),
    (2 to 1) to ZoneInfo("南", "火", "低"),
    (2 to 2) to ZoneInfo("东南", "火", "中")
)

// ======================== 结果模型 ========================

data class VastuRoom(
    val name: String,
    val x: Float, val y: Float, val width: Float, val height: Float
)

data class VastuRoomResult(
    val name: String,
    val centerX: Float, val centerY: Float,
    val gridRow: Int, val gridCol: Int,
    val direction: String,
    val zoneDirection: String,
    val zoneElement: String,
    val zoneLuck: String,
    val recommended: List<String>,
    val compliant: Boolean?,     // true/false/null(中性)
    val advice: String
)

data class VastuAnalysis(
    val plotWidth: Float, val plotDepth: Float, val facing: String,
    val roomResults: List<VastuRoomResult>,
    val violations: List<Pair<String, String>>,   // (房间, 问题)
    val score: Int,
    val grade: String,
    val interpretation: String,
    val recommendations: List<String>
)

// ======================== 核心计算 ========================

object Vastu {

    /** 坐标 → VPM 网格 (row, col)（西南角为原点，y 向上为北） */
    fun zoneOf(plotWidth: Float, plotDepth: Float, x: Float, y: Float): Pair<Int, Int> {
        val nx = x / plotWidth
        val ny = y / plotDepth
        val col = when {
            nx < 1f / 3f -> 0
            nx < 2f / 3f -> 1
            else -> 2
        }
        val row = when {
            ny < 1f / 3f -> 2
            ny < 2f / 3f -> 1
            else -> 0
        }
        return row to col
    }

    /** 中心偏移角度（度，北=0 顺时针）→ 方向简称 */
    fun directionOf(plotWidth: Float, plotDepth: Float, cx: Float, cy: Float): String {
        val ox = cx - plotWidth / 2f
        val oy = cy - plotDepth / 2f
        // atan2(ox, oy)：y 轴为北，x 轴为东；角度 0=北，顺时针
        var deg = Math.toDegrees(atan2(ox.toDouble(), oy.toDouble()))
        if (deg < 0) deg += 360.0
        val angle = deg
        return when {
            angle >= 337.5 || angle < 22.5 -> "北"
            angle < 67.5 -> "东北"
            angle < 112.5 -> "东"
            angle < 157.5 -> "东南"
            angle < 202.5 -> "南"
            angle < 247.5 -> "西南"
            angle < 292.5 -> "西"
            else -> "西北"
        }
    }

    /** 房间合规检查 */
    fun checkRoom(
        plotWidth: Float, plotDepth: Float, room: VastuRoom
    ): VastuRoomResult {
        val cx = room.x + room.width / 2f
        val cy = room.y + room.height / 2f
        val (row, col) = zoneOf(plotWidth, plotDepth, cx, cy)
        val zone = VPM_ZONES[row to col]
        val direction = directionOf(plotWidth, plotDepth, cx, cy)
        val recommended = ROOM_PLACEMENT[room.name] ?: emptyList()
        val compliant = if (recommended.isEmpty()) null else direction in recommended
        val advice = when {
            recommended.isEmpty() -> "ℹ️ '${room.name}' 无特定方向要求。"
            compliant == true -> "✅ '${room.name}' 在 $direction 方向，符合瓦斯图原则。"
            else -> "⚠️ '${room.name}' 在 $direction 方向，建议移至 ${recommended.joinToString("、")}。"
        }
        return VastuRoomResult(
            name = room.name,
            centerX = (cx * 100).roundToInt() / 100f,
            centerY = (cy * 100).roundToInt() / 100f,
            gridRow = row, gridCol = col,
            direction = direction,
            zoneDirection = zone?.direction ?: "?",
            zoneElement = zone?.element ?: "?",
            zoneLuck = zone?.luck ?: "?",
            recommended = recommended,
            compliant = compliant,
            advice = advice
        )
    }

    /** 完整分析 */
    fun analyze(plotWidth: Float, plotDepth: Float, facing: String, rooms: List<VastuRoom>): VastuAnalysis {
        val results = rooms.map { checkRoom(plotWidth, plotDepth, it) }
        val violations = mutableListOf<Pair<String, String>>()
        results.forEach { r ->
            if (r.compliant == false) {
                violations.add(r.name to "位于 ${r.direction}，但推荐 ${r.recommended.joinToString("、")}")
            }
        }
        var deduction = 0
        violations.forEach { (name, _) ->
            deduction += if (name == "厨房" || name == "主卧室") 7 else 5
        }
        val score = (100 - deduction).coerceIn(0, 100)
        val grade = when {
            score >= 90 -> "极佳 (Excellent) — 高度符合瓦斯图原则"
            score >= 75 -> "良好 (Good) — 基本符合，有少量改进空间"
            score >= 60 -> "一般 (Fair) — 部分符合，建议进行调整"
            else -> "需改进 (Needs Improvement) — 建议重新审视布局"
        }
        return VastuAnalysis(
            plotWidth = plotWidth,
            plotDepth = plotDepth,
            facing = facing,
            roomResults = results,
            violations = violations,
            score = score,
            grade = grade,
            interpretation = interpretation(facing, results, violations, score),
            recommendations = if (violations.isEmpty()) listOf("🎉 所有房间布局均符合瓦斯图原则！")
                             else violations.map { "• ${it.first}: ${it.second}" }
        )
    }

    /** 详细解读（六维：总评/事业/财运/感情/健康/建议，贴合瓦斯图空间能量主题） */
    private fun interpretation(
        facing: String,
        results: List<VastuRoomResult>,
        violations: List<Pair<String, String>>,
        score: Int
    ): String {
        val sb = StringBuilder()
        sb.append("「总评」此宅朝向$facing，")
        sb.append(when {
            score >= 90 -> "整体格局上乘，能量流通顺畅，家宅气象昌明，可安居乐业。"
            score >= 75 -> "整体格局良好，仅个别方位有待微调，稍作优化即佳。"
            score >= 60 -> "整体格局尚可，主要方位存在偏失，宜按建议逐项调整。"
            else -> "整体格局偏弱，能量流通受阻，须从关键方位入手重整。"
        })
        sb.append("\n")

        // 事业：以书房、客厅为考察点
        sb.append("「事业」")
        val study = results.firstOrNull { it.name == "书房" }
        val living = results.firstOrNull { it.name == "客厅" }
        when {
            study?.compliant == true -> sb.append("书房坐落得宜，文昌气聚，利于读书进取、事业钻研。")
            study?.compliant == false -> sb.append("书房方位不合，思路易受阻，宜移至${study.recommended.joinToString("、")}，以利事业精进。")
            living?.compliant == true -> sb.append("客厅方位合宜，明堂开阔，利于交际应酬与事业人脉的拓展。")
            else -> sb.append("宜设书房于西、北或客厅于东、北，以聚文昌之气，助事业有成。")
        }
        sb.append("\n")

        // 财运：北为财位（Kubera），考察客厅/书房/保险柜/厨房
        sb.append("「财运」")
        val northRoom = results.firstOrNull { it.direction == "北" }
        when {
            northRoom?.name in listOf("客厅", "书房") -> sb.append("北侧财位设有开明之室，财气可入，正财偏财皆有所旺。")
            violations.any { it.first == "保险柜" } -> sb.append("保险柜方位有误，财库不稳，宜移至西南，以固财守业。")
            violations.any { it.first == "厨房" } -> sb.append("厨房方位有失，火神失位，须防开销失控、破财之象，宜迁至东南。")
            else -> sb.append("财库尚稳，宜保持北侧开阔明亮，聚财守业自然顺遂。")
        }
        sb.append("\n")

        // 感情：以主卧室为考察点
        sb.append("「感情」")
        val master = results.firstOrNull { it.name == "主卧室" }
        when {
            master?.compliant == true -> sb.append("主卧室落西南，卧龙得位，夫妻情笃、家宅和顺。")
            master?.compliant == false -> sb.append("主卧室方位不妥，易生口角与疏离，宜移至${master.recommended.joinToString("、")}以安枕促情。")
            else -> sb.append("寝居方位尚可，宜保持卧室宁静整洁，感情自有滋养。")
        }
        sb.append("\n")

        // 健康：考察中央 Brahmasthan、厨房火位
        sb.append("「健康」")
        val centerOccupied = results.any { it.gridRow == 1 && it.gridCol == 1 }
        val kitchen = results.firstOrNull { it.name == "厨房" }
        when {
            centerOccupied -> sb.append("中央（Brahmasthan）被占用，气机受阻，易致全家疲惫烦闷，宜清空以畅气血。")
            kitchen?.direction == "东北" -> sb.append("厨房压于东北，火入空位，须防家人头面与神经之疾，宜速调整。")
            kitchen?.compliant == true -> sb.append("火位得正，家人脾胃康健、气色红润，健康根基稳固。")
            else -> sb.append("总体水火有序，宜保持通风与清洁，健康自有保障。")
        }
        sb.append("\n")

        // 建议
        sb.append("「建议」")
        if (violations.isEmpty()) {
            sb.append("当前布局已合瓦斯图要旨，宜保持东北明净、中央开阔，家运自然蒸蒸日上。")
        } else {
            sb.append(violations.joinToString("；") { "${it.first}：${it.second}" })
            sb.append("。按上述次序调整，即可显著改善家宅气场。")
        }
        return sb.toString().trimEnd()
    }

    /** 八方向属性一览（供展示） */
    fun directionTable(): List<Triple<String, String, String>> =
        listOf("东", "东南", "南", "西南", "西", "西北", "北", "东北").map { d ->
            Triple(d, DIRECTION_ELEMENT_FULL[d] ?: "", DIRECTION_FUNCTION[d] ?: "")
        }
}
