package com.xuanji.app.domain.external

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class OpenMeteoParserTest {
    @Test
    fun parses_geocoding_and_current_weather_without_network() {
        val cities = OpenMeteoParser.parseCities(
            """{"results":[{"id":1,"name":"上海","country":"中国","admin1":"上海","latitude":31.23,"longitude":121.47}]}"""
        )
        assertEquals(1, cities.size)
        assertEquals("上海", cities.single().name)
        val weather = OpenMeteoParser.parseWeather(
            """{"current":{"temperature_2m":25.5,"apparent_temperature":26.0,"precipitation_probability":20,"weather_code":2,"wind_speed_10m":8.4}}""",
            cities.single()
        )
        assertEquals(25.5, weather.temperatureC!!, 0.01)
        assertEquals(20, weather.precipitationProbability)
        assertNotNull(weather.fetchedAtEpochMs)
    }
}
