package com.xuanji.app.domain

import android.content.Context
import com.google.gson.Gson
import com.xuanji.app.R

data class LocationDistrict(
    val code: String,
    val name: String,
    val lat: Double,
    val lng: Double
)

data class LocationCity(
    val code: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val districts: List<LocationDistrict>
)

data class LocationProvince(
    val code: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val cities: List<LocationCity>
)

data class LocationCatalogData(
    val provinces: List<LocationProvince>
)

data class SelectedLocation(
    val provinceIndex: Int,
    val cityIndex: Int,
    val districtIndex: Int
) {
    val province: LocationProvince get() = ChinaLocations.catalog.provinces[provinceIndex]
    val city: LocationCity get() = province.cities[cityIndex]
    val district: LocationDistrict get() = city.districts[districtIndex]
}

object ChinaLocations {
    @Volatile
    private var cached: LocationCatalogData? = null

    val catalog: LocationCatalogData
        get() = requireNotNull(cached) { "Location catalog is not loaded" }

    fun load(context: Context): LocationCatalogData {
        cached?.let { return it }
        return synchronized(this) {
            cached?.let { return it }
            context.resources.openRawResource(R.raw.china_locations).bufferedReader().use { reader ->
                Gson().fromJson(reader, LocationCatalogData::class.java)
            }.also { cached = it }
        }
    }

    fun find(code: String?): SelectedLocation? {
        if (code.isNullOrBlank()) return null
        catalog.provinces.forEachIndexed { pi, province ->
            province.cities.forEachIndexed { ci, city ->
                val di = city.districts.indexOfFirst { it.code == code }
                if (di >= 0) return SelectedLocation(pi, ci, di)
            }
        }
        return null
    }

    /** 兼容旧版自由文本中的常用城市；新档案一律保存区县编码。 */
    fun findLegacyCity(name: String?): SelectedLocation? {
        if (name.isNullOrBlank()) return null
        val normalized = name.trim().removePrefix("市").removeSuffix("市")
        catalog.provinces.forEachIndexed { pi, province ->
            province.cities.forEachIndexed { ci, city ->
                if (city.name.removeSuffix("市") == normalized || province.name.removeSuffix("市") == normalized) {
                    return SelectedLocation(pi, ci, 0)
                }
            }
        }
        return null
    }
}
