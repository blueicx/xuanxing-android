package com.xuanji.app.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileSectionOrderTest {
    @Test
    fun profile_sections_keep_identity_and_settings_after_birth_profile() {
        assertEquals(
            listOf(
                ProfileSection.BirthInfo,
                ProfileSection.CurrentProfile,
                ProfileSection.LifeProfile,
                ProfileSection.FoodPreference,
                ProfileSection.MysticGuide,
                ProfileSection.About,
                ProfileSection.Privacy
            ),
            profileSectionOrder
        )
    }
}
