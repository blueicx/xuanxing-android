package com.xuanji.app.domain

import com.xuanji.app.data.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileDataReadinessTest {
    @Test
    fun missing_profile_reports_every_birth_input_as_missing() {
        val readiness = BirthProfileReadinessEvaluator.evaluate(null)

        assertTrue(readiness.fields.all { it.availability == ProfileInputAvailability.Missing })
        assertEquals(5, readiness.fields.size)
    }

    @Test
    fun saved_profile_distinguishes_selected_place_from_optional_coordinates_and_gender() {
        val readiness = BirthProfileReadinessEvaluator.evaluate(
            UserProfile(1990, 5, 3, 8, 20, locationName = "浙江省/杭州市/西湖区")
        )

        assertEquals(ProfileInputAvailability.Available, readiness.fields.single { it.key == "date" }.availability)
        assertEquals(ProfileInputAvailability.Available, readiness.fields.single { it.key == "time" }.availability)
        assertEquals(ProfileInputAvailability.Available, readiness.fields.single { it.key == "place" }.availability)
        assertEquals(ProfileInputAvailability.Missing, readiness.fields.single { it.key == "coordinates" }.availability)
        assertEquals(ProfileInputAvailability.Missing, readiness.fields.single { it.key == "gender" }.availability)
        assertTrue(readiness.fields.single { it.key == "gender" }.impact.contains("大运"))
    }
}
