package com.xuanji.app.domain.external

import com.xuanji.app.domain.action.CityProfile

data class CitySearchResult(
    val id: Long,
    val name: String,
    val country: String,
    val admin1: String?,
    val latitude: Double,
    val longitude: Double
)

data class WeatherSnapshot(
    val city: CitySearchResult,
    val temperatureC: Double?,
    val apparentTemperatureC: Double?,
    val precipitationProbability: Int?,
    val weatherCode: Int?,
    val windSpeedKmh: Double?,
    val fetchedAtEpochMs: Long,
    val source: String
)

sealed interface ExternalResult<out T> {
    data class Success<T>(val value: T, val fromCache: Boolean = false) : ExternalResult<T>
    data class Failure(val reason: ExternalFailureReason, val retryable: Boolean = true) : ExternalResult<Nothing>
}

enum class ExternalFailureReason { Disabled, InvalidQuery, Network, RateLimited, Parse, NotFound }

data class ExternalContextConsent(
    val networkEnabled: Boolean = false,
    val manualCityKey: String? = null,
    val cacheEnabled: Boolean = true
)

interface ExternalContextProvider {
    suspend fun searchCities(query: String, language: String = "zh"): ExternalResult<List<CitySearchResult>>
    suspend fun weather(city: CitySearchResult): ExternalResult<WeatherSnapshot>
}

/** No-network fallback. It intentionally returns labels only and never invents weather. */
class OfflineContextProvider(
    private val cities: List<CitySearchResult> = listOf(
        CitySearchResult(1816670, "上海", "中国", "上海", 31.2304, 121.4737),
        CitySearchResult(1816671, "北京", "中国", "北京", 39.9042, 116.4074),
        CitySearchResult(1816672, "成都", "中国", "四川", 30.5728, 104.0668),
        CitySearchResult(1816673, "杭州", "中国", "浙江", 30.2741, 120.1551),
        CitySearchResult(1857910, "京都", "日本", "京都府", 35.0116, 135.7681),
        CitySearchResult(2267057, "里斯本", "葡萄牙", "里斯本", 38.7223, -9.1393),
        CitySearchResult(658225, "赫尔辛基", "芬兰", "新地", 60.1699, 24.9384)
    )
) : ExternalContextProvider {
    override suspend fun searchCities(query: String, language: String): ExternalResult<List<CitySearchResult>> {
        val term = query.trim()
        if (term.isEmpty()) return ExternalResult.Failure(ExternalFailureReason.InvalidQuery, retryable = false)
        val matches = cities.filter { city ->
            city.name.contains(term, ignoreCase = true) ||
                city.country.contains(term, ignoreCase = true) ||
                city.admin1.orEmpty().contains(term, ignoreCase = true)
        }
        return if (matches.isEmpty()) ExternalResult.Failure(ExternalFailureReason.NotFound, retryable = false)
        else ExternalResult.Success(matches)
    }

    override suspend fun weather(city: CitySearchResult): ExternalResult<WeatherSnapshot> =
        ExternalResult.Failure(ExternalFailureReason.Disabled, retryable = false)
}

/** Maps a directory city to the existing offline action tags without network access. */
fun CitySearchResult.toCityProfile(): CityProfile = CityProfile(
    key = "external-$id",
    label = listOfNotNull(country, name).joinToString(" · "),
    tags = emptySet(),
    country = country,
    city = name
)
