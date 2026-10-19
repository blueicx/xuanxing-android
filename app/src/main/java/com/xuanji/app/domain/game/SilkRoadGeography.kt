package com.xuanji.app.domain.game

import kotlin.math.*

data class SilkRoadPoint(val x: Double, val y: Double)
data class SilkRoadGeoCity(val id: String, val latitude: Double, val longitude: Double, val source: String)
data class SilkRoadEdge(val from: String, val to: String, val days: Int, val supplies: Int)
data class SilkRoadRoutePlan(val id: String, val title: String, val path: List<String>, val days: Int, val supplies: Int, val distanceKm: Int, val feasible: Boolean)

/** Geographic positions are real reference points; resource costs and corridors are self-authored game rules. */
object SilkRoadGeography {
    const val ATTRIBUTION = "城市坐标：GeoNames，CC BY 4.0；坐标四舍五入。底图/廊道自绘，非历史路网或现实导航。"
    val cities = listOf(
        SilkRoadGeoCity("长安", 34.25833, 108.92861, "https://www.geonames.org/1790630/xi-an.html"),
        SilkRoadGeoCity("兰州", 36.05701, 103.83987, "https://www.geonames.org/1804430/lanzhou.html"),
        SilkRoadGeoCity("张掖", 38.93417, 100.45167, "https://www.geonames.org/1785036/zhangye.html"),
        SilkRoadGeoCity("敦煌", 40.16667, 94.68333, "https://www.geonames.org/advanced-search.html?q=gansu&startRow=0"),
        SilkRoadGeoCity("吐鲁番", 42.94769, 89.17886, "https://www.geonames.org/search.html?country=CN&q=Turpan"),
        SilkRoadGeoCity("喀什", 39.46718, 75.98675, "https://www.geonames.org/1280849/kashgar.html"),
        SilkRoadGeoCity("塔什干", 41.265, 69.216, "https://www.geonames.org/UZ/largest-cities-in-uzbekistan.html"),
        SilkRoadGeoCity("撒马尔罕", 39.655, 66.964, "https://www.geonames.org/UZ/largest-cities-in-uzbekistan.html"),
        SilkRoadGeoCity("布哈拉", 39.770, 64.431, "https://www.geonames.org/UZ/largest-cities-in-uzbekistan.html")
    )
    private val byId = cities.associateBy { it.id }
    private val legacyCosts = mapOf("长安" to (0 to 0), "敦煌" to (2 to 3), "喀什" to (3 to 4), "撒马尔罕" to (3 to 5))
    private val legacyPairs = listOf("长安" to "敦煌", "敦煌" to "喀什", "喀什" to "撒马尔罕")
    val edges = buildList {
        legacyPairs.forEach { (a, b) ->
            add(SilkRoadEdge(a, b, legacyCosts.getValue(b).first, legacyCosts.getValue(b).second))
            add(SilkRoadEdge(b, a, legacyCosts.getValue(a).first, legacyCosts.getValue(a).second))
        }
        listOf(SilkRoadEdge("长安", "兰州", 1, 1), SilkRoadEdge("兰州", "张掖", 1, 1),
            SilkRoadEdge("张掖", "敦煌", 1, 1), SilkRoadEdge("敦煌", "吐鲁番", 4, 1),
            SilkRoadEdge("吐鲁番", "喀什", 2, 1), SilkRoadEdge("喀什", "塔什干", 3, 1),
            SilkRoadEdge("塔什干", "撒马尔罕", 1, 1), SilkRoadEdge("撒马尔罕", "布哈拉", 1, 1),
            SilkRoadEdge("塔什干", "布哈拉", 2, 1)).forEach { edge -> add(edge); add(edge.copy(from = edge.to, to = edge.from)) }
    }
    fun leg(from: String, to: String): SilkRoadEdge? = edges.firstOrNull { it.from == from && it.to == to }
    fun destinations(current: String): List<RouteCity> = edges.filter { it.from == current }.sortedBy { it.to }
        .map { RouteCity(it.to, it.to, it.days, it.supplies) }
    fun contains(city: String): Boolean = city in byId
    fun project(city: String): SilkRoadPoint {
        val value = byId.getValue(city)
        return SilkRoadPoint((value.longitude - 62.0) / 49.0, .5 - (value.latitude - 39.0) / (49.0 * cos(Math.toRadians(39.0))))
    }
    fun hit(point: SilkRoadPoint, camera: SilkRoadCamera, width: Double, height: Double, radius: Double): String? = cities
        .map { it.id to camera.toScreen(project(it.id), width, height) }
        .minByOrNull { (_, p) -> hypot(point.x - p.x, point.y - p.y) }
        ?.takeIf { (_, p) -> hypot(point.x - p.x, point.y - p.y) <= radius }?.first

    fun distanceKm(from: String, to: String): Int {
        val a = byId.getValue(from); val b = byId.getValue(to)
        val latA = Math.toRadians(a.latitude); val latB = Math.toRadians(b.latitude)
        val deltaLat = latB - latA; val deltaLon = Math.toRadians(b.longitude - a.longitude)
        val haversine = sin(deltaLat / 2).pow(2) + cos(latA) * cos(latB) * sin(deltaLon / 2).pow(2)
        return (6371 * 2 * asin(sqrt(haversine.coerceIn(0.0, 1.0)))).roundToInt()
    }
    fun validPlan(path: List<String>, current: String, target: String): Boolean = path.size in 1..cities.size &&
        path.first() == current && path.last() == target && path.distinct().size == path.size && path.all(::contains) &&
        path.zipWithNext().all { (a, b) -> leg(a, b) != null }

    fun compareRoutes(state: SilkRoadState): List<SilkRoadRoutePlan> {
        if (state.completed || state.failed || !contains(state.current) || !contains(state.target)) return emptyList()
        val candidates = mutableListOf<SilkRoadRoutePlan>()
        fun visit(path: List<String>) {
            if (path.last() == state.target) {
                val legs = path.zipWithNext().map { (a, b) -> leg(a, b)!! }
                val days = legs.sumOf { it.days }; val supplies = legs.sumOf { it.supplies }
                candidates += SilkRoadRoutePlan("", "", path, days, supplies,
                    legs.sumOf { distanceKm(it.from, it.to) }, days <= state.daysLeft && supplies <= state.budgetLeft)
                return
            }
            if (path.size >= cities.size) return
            destinations(path.last()).filterNot { it.id in path }.forEach { visit(path + it.id) }
        }
        visit(listOf(state.current))
        val stable = compareBy<SilkRoadRoutePlan> { it.path.joinToString("|") }
        val rankings = listOf(
            Triple("fast", "耗时较短", compareBy<SilkRoadRoutePlan> { it.days }.thenBy { it.supplies }.then(stable)),
            Triple("cheap", "补给较省", compareBy<SilkRoadRoutePlan> { it.supplies }.thenBy { it.days }.then(stable)),
            Triple("balanced", "综合均衡", compareBy<SilkRoadRoutePlan> { it.days + it.supplies }.thenBy { it.days }.then(stable)))
        // Compare feasible routes first. Only show resource-shortfall alternatives if
        // no complete route can be paid for; never spend prospective event rewards.
        val available = candidates.filter { it.feasible }.ifEmpty { candidates }
        return rankings.mapNotNull { (id, title, order) -> available.minWithOrNull(order)?.copy(id = id, title = title) }
    }
}

data class SilkRoadCamera(val zoom: Double = 1.0, val panX: Double = 0.0, val panY: Double = 0.0) {
    private fun scale(width: Double, height: Double) = min(width.coerceAtLeast(1.0), height.coerceAtLeast(1.0) * 3) * zoom.coerceIn(1.0, 4.0)
    fun toScreen(point: SilkRoadPoint, width: Double, height: Double) = SilkRoadPoint(
        (point.x - .5) * scale(width, height) + width / 2 + panX * width,
        (point.y - .5) * scale(width, height) + height / 2 + panY * height)
    fun fromScreen(point: SilkRoadPoint, width: Double, height: Double) = SilkRoadPoint(
        (point.x - width / 2 - panX * width) / scale(width, height) + .5,
        (point.y - height / 2 - panY * height) / scale(width, height) + .5)
    fun adjusted(factor: Double = 1.0, dx: Double = 0.0, dy: Double = 0.0) = copy(
        zoom = (zoom * factor).coerceIn(1.0, 4.0), panX = (panX + dx).coerceIn(-.6, .6), panY = (panY + dy).coerceIn(-.6, .6))
}
