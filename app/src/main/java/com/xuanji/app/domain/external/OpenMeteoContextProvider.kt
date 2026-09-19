package com.xuanji.app.domain.external

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/** Optional network provider. It is never constructed as the default app provider. */
class OpenMeteoContextProvider(
    private val connectTimeoutMs: Int = 8_000,
    private val readTimeoutMs: Int = 8_000,
    private val userAgent: String = "XuanjiAndroid/1.0 (manual city search)"
) : ExternalContextProvider {
    private data class Cached<T>(val value: T, val expiresAt: Long)
    private val cityCache = ConcurrentHashMap<String, Cached<List<CitySearchResult>>>()
    private val weatherCache = ConcurrentHashMap<Long, Cached<WeatherSnapshot>>()

    override suspend fun searchCities(query: String, language: String): ExternalResult<List<CitySearchResult>> =
        withContext(Dispatchers.IO) {
            val term = query.trim()
            if (term.isEmpty()) return@withContext ExternalResult.Failure(ExternalFailureReason.InvalidQuery, retryable = false)
            val cacheKey = "$language|${term.lowercase()}"
            cityCache[cacheKey]?.takeIf { it.expiresAt > System.currentTimeMillis() }?.let {
                return@withContext ExternalResult.Success(it.value, fromCache = true)
            }
            val encoded = URLEncoder.encode(term, StandardCharsets.UTF_8.toString())
            val lang = URLEncoder.encode(language.ifBlank { "en" }, StandardCharsets.UTF_8.toString())
            val url = URL("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=8&language=$lang&format=json")
            request(url)?.let { body ->
                runCatching { OpenMeteoParser.parseCities(body) }
                    .fold(
                        onSuccess = { values ->
                            if (values.isEmpty()) ExternalResult.Failure(ExternalFailureReason.NotFound, false)
                            else {
                                cityCache[cacheKey] = Cached(values, System.currentTimeMillis() + CITY_CACHE_TTL_MS)
                                ExternalResult.Success(values)
                            }
                        },
                        onFailure = { cachedCityFailure(cacheKey, ExternalFailureReason.Parse) }
                    )
            } ?: cachedCityFailure(cacheKey, ExternalFailureReason.Network)
        }

    override suspend fun weather(city: CitySearchResult): ExternalResult<WeatherSnapshot> =
        withContext(Dispatchers.IO) {
            weatherCache[city.id]?.takeIf { it.expiresAt > System.currentTimeMillis() }?.let {
                return@withContext ExternalResult.Success(it.value, fromCache = true)
            }
            val url = URL(
                "https://api.open-meteo.com/v1/forecast?latitude=${city.latitude}&longitude=${city.longitude}" +
                    "&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m" +
                    "&hourly=precipitation_probability&forecast_days=1"
            )
            request(url)?.let { body ->
                runCatching { OpenMeteoParser.parseWeather(body, city) }
                    .fold(
                        onSuccess = {
                            weatherCache[city.id] = Cached(it, System.currentTimeMillis() + WEATHER_CACHE_TTL_MS)
                            ExternalResult.Success(it)
                        },
                        onFailure = { weatherCache[city.id]?.let { cached -> ExternalResult.Success(cached.value, true) } ?: ExternalResult.Failure(ExternalFailureReason.Parse, false) }
                    )
            } ?: (weatherCache[city.id]?.let { ExternalResult.Success(it.value, true) } ?: ExternalResult.Failure(ExternalFailureReason.Network))
        }

    private fun cachedCityFailure(key: String, reason: ExternalFailureReason): ExternalResult<List<CitySearchResult>> =
        cityCache[key]?.let { ExternalResult.Success(it.value, fromCache = true) }
            ?: ExternalResult.Failure(reason)

    private companion object {
        const val CITY_CACHE_TTL_MS = 24 * 60 * 60 * 1000L
        const val WEATHER_CACHE_TTL_MS = 15 * 60 * 1000L
    }

    private fun request(url: URL): String? {
        val connection = (url.openConnection() as? HttpURLConnection) ?: return null
        return try {
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", userAgent)
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.bufferedReader().use { it.readText().take(256_000) }
        } finally {
            connection.disconnect()
        }
    }
}

internal object OpenMeteoParser {
    fun parseCities(json: String): List<CitySearchResult> {
        val root = JsonParser.parseString(json).asJsonObject
        return root.getAsJsonArray("results")?.mapNotNull { node ->
            val value = node.asJsonObject
            val name = value.stringOrNull("name") ?: return@mapNotNull null
            CitySearchResult(
                id = value.longOrNull("id") ?: name.hashCode().toLong(),
                name = name,
                country = value.stringOrNull("country") ?: "",
                admin1 = value.stringOrNull("admin1"),
                latitude = value.doubleOrNull("latitude") ?: return@mapNotNull null,
                longitude = value.doubleOrNull("longitude") ?: return@mapNotNull null
            )
        }.orEmpty()
    }

    fun parseWeather(json: String, city: CitySearchResult): WeatherSnapshot {
        val root = JsonParser.parseString(json).asJsonObject
        val current = root.getAsJsonObject("current") ?: error("missing current")
        return WeatherSnapshot(
            city = city,
            temperatureC = current.doubleOrNull("temperature_2m"),
            apparentTemperatureC = current.doubleOrNull("apparent_temperature"),
            precipitationProbability = current.intOrNull("precipitation_probability")
                ?: root.getAsJsonObject("hourly")?.getAsJsonArray("precipitation_probability")?.firstOrNull()?.asInt,
            weatherCode = current.intOrNull("weather_code"),
            windSpeedKmh = current.doubleOrNull("wind_speed_10m"),
            fetchedAtEpochMs = Instant.now().toEpochMilli(),
            source = "Open-Meteo"
        )
    }

    private fun JsonObject.stringOrNull(key: String): String? = get(key)?.takeUnless { it.isJsonNull }?.asString
    private fun JsonObject.doubleOrNull(key: String): Double? = get(key)?.takeUnless { it.isJsonNull }?.asDouble
    private fun JsonObject.longOrNull(key: String): Long? = get(key)?.takeUnless { it.isJsonNull }?.asLong
    private fun JsonObject.intOrNull(key: String): Int? = get(key)?.takeUnless { it.isJsonNull }?.asInt
}
