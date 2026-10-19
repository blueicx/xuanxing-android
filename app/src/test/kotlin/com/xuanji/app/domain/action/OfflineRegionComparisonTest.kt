package com.xuanji.app.domain.action

import org.junit.Assert.*
import org.junit.Test

class OfflineRegionComparisonTest {
    @Test fun all_24_cities_have_named_sources_and_the_same_baseline() {
        assertEquals(24, CityClimateNormals.all.size)
        assertEquals(CityProfileCatalog.profiles.map { it.key }.toSet(), CityClimateNormals.all.map { it.cityKey }.toSet())
        CityClimateNormals.all.forEach {
            assertEquals(12, it.monthlyMeanC.size)
            assertEquals("1991–2020", it.period)
            assertTrue(it.sourceUrl.startsWith("https://"))
        }
        assertTrue(OfflineRegionComparison.compare(RegionPreference("温暖")).all { it.climateMatch != null })
    }
    @Test fun new_city_values_are_verified_not_an_unrelated_station_default() {
        val records = CityClimateNormals.all.associateBy { it.cityKey }
        assertEquals(5.7, records.getValue("chengdu").monthlyMeanC.first(), 0.0)
        assertEquals(0.5, records.getValue("xian").monthlyMeanC.first(), 0.0)
        assertEquals(3.8, records.getValue("amsterdam").monthlyMeanC.first(), 0.0)
        assertEquals(3.29, records.getValue("suzhou").monthlyMeanC.first(), 0.0)
        assertEquals(16.12, records.getValue("shenzhen").monthlyMeanC.first(), 0.0)
        assertEquals(13.08, records.getValue("xiamen").monthlyMeanC.first(), 0.0)
    }
    @Test fun gridded_data_is_never_labelled_as_a_station_observation() {
        val grids = CityClimateNormals.all.filter { it.dataKind == ClimateDataKind.GriddedReanalysis }
        assertEquals(setOf("suzhou", "shenzhen", "xiamen"), grids.map { it.cityKey }.toSet())
        grids.forEach {
            assertNull(it.stationId)
            assertTrue(it.sourceUrl.contains("start=1991&end=2020"))
            assertTrue(it.sourceUrl.contains("latitude=") && it.sourceUrl.contains("longitude="))
            assertTrue(it.spatialDescription.contains("非当地测站实测"))
        }
        val comparison = OfflineRegionComparison.compare(RegionPreference("温暖")).first { it.city.key == "shenzhen" }
        assertTrue(comparison.climateMatch == true)
        assertTrue(comparison.explanation.contains("NASA POWER / MERRA-2"))
        assertFalse(comparison.explanation.contains("JMA"))
        assertTrue(CityClimateNormals.all.single { it.cityKey == "amsterdam" }.sourceUrl.contains("knmi.nl"))
    }
    @Test fun incomplete_or_non_finite_monthly_data_is_rejected() {
        listOf(List(11) { 10.0 }, List(12) { Double.NaN }, List(12) { -999.0 }).forEach { values ->
            assertThrows(IllegalArgumentException::class.java) { CityClimateNormal("invalid", 1, "invalid", values) }
        }
    }
    @Test fun climate_preferences_change_only_factual_comparison_not_cultural_scores() {
        val warm = OfflineRegionComparison.compare(RegionPreference("温暖"))
        assertTrue(warm.first { it.city.key == "singapore" }.climateMatch == true)
        assertFalse(warm.first { it.city.key == "helsinki" }.climateMatch == true)
        val cool = OfflineRegionComparison.compare(RegionPreference("凉爽", "舒缓", "低预算"))
        assertTrue(cool.first { it.city.key == "helsinki" }.climateMatch == true)
        assertTrue(cool.all { it.explanation.contains("不计分") && !it.explanation.contains("最适合") })
        assertEquals(cool, OfflineRegionComparison.compare(RegionPreference("凉爽", "舒缓", "低预算")))
    }
}
