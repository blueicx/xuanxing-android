package com.xuanji.app.domain.action

enum class ClimateDataKind { StationNormal, GriddedReanalysis }

/** Frozen excerpts, not live weather. Ranking thresholds are project rules, not agency advice. */
data class CityClimateNormal(val cityKey: String, val stationId: Int?, val stationName: String,
    val monthlyMeanC: List<Double>,
    val sourceName: String = "JMA ClimatView",
    val sourceUrl: String = "https://www.data.jma.go.jp/tcc/tcc/products/climate/climatview/graph_mkhtml_nrm.php?n=$stationId",
    val dataKind: ClimateDataKind = ClimateDataKind.StationNormal,
    val spatialDescription: String = "具名测站常年值，不代表全市") {
    init {
        require(monthlyMeanC.size == 12 && monthlyMeanC.all { it.isFinite() && it in -90.0..65.0 }) {
            "Climate reference requires twelve valid monthly temperatures"
        }
    }
    val annualMonthMeanC get() = monthlyMeanC.average()
    val period get() = "1991–2020"
}

object CityClimateNormals {
    const val VERSION = "climate-excerpts-2026-10-15-v2"
    val all = listOf(
        CityClimateNormal("shanghai",58362,"上海宝山",listOf(5.0,6.5,10.3,15.7,20.9,24.4,28.8,28.5,24.8,19.7,13.9,7.5)),
        CityClimateNormal("beijing",54511,"北京",listOf(-2.8,0.6,7.5,15.1,21.3,25.3,27.2,26.0,21.2,13.8,5.2,-1.0)),
        CityClimateNormal("hangzhou",58457,"杭州",listOf(5.4,7.5,11.6,17.4,22.4,25.4,29.7,29.3,25.0,19.6,13.8,7.7)),
        CityClimateNormal("qingdao",54857,"青岛",listOf(0.2,2.1,6.3,11.6,17.1,20.8,24.7,25.6,22.3,16.7,9.5,2.7)),
        CityClimateNormal("kunming",56778,"昆明",listOf(9.3,11.6,14.9,17.8,19.7,20.7,20.5,20.3,18.8,16.2,12.5,9.5)),
        CityClimateNormal("nanjing",58238,"南京",listOf(3.2,5.6,10.2,16.2,21.5,25.0,28.4,27.9,23.7,18.0,11.6,5.4)),
        CityClimateNormal("guangzhou",59287,"广州",listOf(13.8,15.5,18.3,22.5,26.0,27.9,28.9,28.6,27.4,24.5,20.1,15.5)),
        CityClimateNormal("seoul",47108,"首尔",listOf(-1.9,0.7,6.1,12.6,18.3,22.7,25.3,26.1,21.7,15.1,7.5,0.2)),
        CityClimateNormal("tokyo",47662,"东京",listOf(5.4,6.1,9.4,14.3,18.8,21.9,25.7,26.9,23.3,18.0,12.5,7.7)),
        CityClimateNormal("kyoto",47759,"京都",listOf(4.8,5.4,8.8,14.4,19.5,23.3,27.3,28.5,24.4,18.4,12.5,7.2)),
        CityClimateNormal("helsinki",2978,"赫尔辛基 Kaisaniemi",listOf(-3.5,-3.5,-0.5,5.0,11.1,15.3,18.4,17.2,12.9,7.1,3.3,0.1)),
        CityClimateNormal("lisbon",8535,"Lisboa/Geof",listOf(11.6,12.3,14.5,15.8,18.3,20.9,22.6,23.2,21.5,18.6,14.7,12.4)),
        CityClimateNormal("vienna",11035,"Wien/Hohe Warte",listOf(0.8,2.3,6.1,11.3,15.8,19.6,21.4,20.9,15.7,10.5,5.8,1.5)),
        CityClimateNormal("prague",11520,"Praha-Libus",listOf(0.1,1.2,4.8,9.9,14.3,17.8,19.7,19.4,14.6,9.4,4.5,1.1)),
        CityClimateNormal("vancouver",71892,"温哥华国际机场",listOf(3.9,5.2,7.1,9.7,12.9,15.5,17.7,18.1,15.0,10.3,6.1,3.9)),
        CityClimateNormal("singapore",48698,"新加坡樟宜机场",listOf(26.8,27.3,27.9,28.2,28.6,28.5,28.2,28.1,28.0,27.9,27.2,26.8)),
        CityClimateNormal("wellington",93439,"惠灵顿机场 AWS",listOf(17.6,17.8,16.5,14.6,12.9,10.8,9.9,10.4,11.7,12.9,14.3,16.4)),
        CityClimateNormal("melbourne",94866,"墨尔本机场",listOf(20.2,20.2,18.3,15.1,12.3,10.1,9.5,10.2,12.0,14.0,16.3,18.2)),
        CityClimateNormal("chengdu",56187,"成都温江 WENJIANG",listOf(5.7,8.0,12.6,17.6,21.4,24.1,25.6,25.4,21.7,17.3,12.3,7.0)),
        CityClimateNormal("xian",57036,"西安 XI'AN",listOf(0.5,4.0,9.4,15.9,20.9,25.8,27.7,25.4,20.7,14.2,7.4,1.7)),
        CityClimateNormal("amsterdam",240,"阿姆斯特丹地区·史基浦机场 Schiphol",listOf(3.8,4.1,6.5,9.8,13.3,16.0,18.1,18.0,15.1,11.3,7.4,4.6),
            sourceName = "KNMI", sourceUrl = "https://cdn.knmi.nl/knmi/map/page/klimatologie/klimaatatlas/tabel/stationsdata/klimtab_9120_240.pdf",
            spatialDescription = "史基浦机场测站，非阿姆斯特丹市中心或全市平均"),
        nasaReference("suzhou", "苏州", "120.58", "31.30", listOf(3.29,5.32,9.49,15.42,21.08,24.87,28.30,28.13,23.93,18.33,12.24,5.59)),
        nasaReference("shenzhen", "深圳", "114.06", "22.55", listOf(16.12,17.52,20.06,23.15,26.19,27.95,28.50,28.50,27.64,25.14,21.64,17.35)),
        nasaReference("xiamen", "厦门", "118.08", "24.48", listOf(13.08,13.98,16.50,20.35,23.81,26.44,27.74,27.65,26.37,22.98,19.30,14.89))
    )

    private fun nasaReference(key: String, name: String, longitude: String, latitude: String, months: List<Double>) =
        CityClimateNormal(key, null, "$name·参考点 $latitude°N $longitude°E", months,
            sourceName = "NASA POWER / MERRA-2",
            sourceUrl = "https://power.larc.nasa.gov/api/temporal/climatology/point?parameters=T2M&community=SB&longitude=$longitude&latitude=$latitude&format=JSON&start=1991&end=2020",
            dataKind = ClimateDataKind.GriddedReanalysis,
            spatialDescription = "0.5°×0.625°网格再分析；地面以上2米气温，非当地测站实测或全市平均")
}

data class RegionPreferenceComparison(val city: CityProfile, val climate: CityClimateNormal?,
    val climateMatch: Boolean?, val explanation: String)

object OfflineRegionComparison {
    fun compare(preference: RegionPreference): List<RegionPreferenceComparison> = CityProfileCatalog.profiles.map { city ->
        val normal = CityClimateNormals.all.firstOrNull { it.cityKey == city.key }
        val match = normal?.let { when(preference.climate) {
            "温暖" -> it.annualMonthMeanC >= 18.0
            "凉爽" -> it.annualMonthMeanC <= 12.0
            else -> null
        } }
        val description = if (normal == null) "气候资料未收录，不计分。" else buildString {
            append("${normal.stationName}，${normal.period}；12个月均温的简单平均 %.1f°C，月均范围 %.1f–%.1f°C。".format(java.util.Locale.ROOT,
                normal.annualMonthMeanC, normal.monthlyMeanC.minOrNull(), normal.monthlyMeanC.maxOrNull()))
            append("项目规则：温暖≥18°C、凉爽≤12°C；${when(match) { true -> "满足所选阈值"; false -> "不满足所选阈值"; null -> "未指定气候阈值" }}。")
            append("根据 ${normal.sourceName} 数据整理；${normal.spatialDescription}。非实时天气，不代表未来气候。来源：${normal.sourceUrl}。")
        }
        RegionPreferenceComparison(city, normal, match, description + "节奏${preference.rhythm}、预算${preference.budget}：缺少可比现实资料，不计分；不生成开销或迁居结论。")
    }.sortedWith(compareBy<RegionPreferenceComparison> { when(it.climateMatch) { true -> 0; false -> 1; null -> 2 } }.thenBy { it.city.key })
}
