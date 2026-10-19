package com.xuanji.app.domain.game

/** Fixed, self-authored resource puzzles. Not historical travel itineraries. */
data class SilkRoadMission(
    val id: String,
    val title: String,
    val target: String,
    val days: Int,
    val supplies: Int,
    val solution: List<String>,
    val explanation: String
)

object SilkRoadMissionCatalog {
    private val fullPath = listOf("长安", "敦煌", "喀什", "撒马尔罕")
    val all = listOf(
        SilkRoadMission("silk-01", "首次西行", "撒马尔罕", 8, 12, fullPath, "恰好用完初始资源，逐段确认，不预支收益。"),
        SilkRoadMission("silk-02", "敦煌送信", "敦煌", 3, 4, fullPath.take(2), "只需抵达第一站，比较出发和剩余资源。"),
        SilkRoadMission("silk-03", "喀什交接", "喀什", 6, 8, fullPath.take(3), "两段累计五天、七点补给，保留返程外的缓冲。"),
        SilkRoadMission("silk-04", "准时抵达", "撒马尔罕", 8, 16, fullPath, "时间紧、补给宽裕；回头行进也会消耗天数。"),
        SilkRoadMission("silk-05", "节省补给", "撒马尔罕", 12, 12, fullPath, "补给仅够单程；解释为何不能反复返回驿站。"),
        SilkRoadMission("silk-06", "从容西行", "撒马尔罕", 12, 18, fullPath, "留下四天、六点补给，练习逐段复核。"),
        SilkRoadMission("silk-07", "短程核算", "敦煌", 2, 3, fullPath.take(2), "第一段恰好两天、三点补给。"),
        SilkRoadMission("silk-08", "两站紧预算", "喀什", 5, 7, fullPath.take(3), "两段累计恰好耗尽，撤销后资源按路径重算。"),
        SilkRoadMission("silk-09", "喀什留余粮", "喀什", 7, 12, fullPath.take(3), "完成后核对剩余两天、五点补给。"),
        SilkRoadMission("silk-10", "敦煌慢行", "敦煌", 6, 8, fullPath.take(2), "区分可用资源与实际消耗，余量不会增加耗时。"),
        SilkRoadMission("silk-11", "长途留缓冲", "撒马尔罕", 10, 15, fullPath, "预计留下两天、三点补给；比较逐段与累计数。"),
        SilkRoadMission("silk-12", "完成路线复盘", "撒马尔罕", 9, 14, fullPath, "完成后逐段复盘八天、十二点补给的结算。")
    )
    fun find(id: String): SilkRoadMission? = all.firstOrNull { it.id == id }
}
