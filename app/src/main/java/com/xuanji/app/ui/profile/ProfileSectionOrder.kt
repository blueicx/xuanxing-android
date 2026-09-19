package com.xuanji.app.ui.profile

/**
 * The stable reading order for the profile page.
 *
 * Birth data and the saved snapshot are intentionally kept together at the top;
 * preference and app settings follow after the personal reading sections.
 */
internal enum class ProfileSection {
    BirthInfo,
    CurrentProfile,
    LifeProfile,
    FoodPreference,
    MysticGuide,
    About,
    Privacy
}

internal val profileSectionOrder: List<ProfileSection> = listOf(
    ProfileSection.BirthInfo,
    ProfileSection.CurrentProfile,
    ProfileSection.LifeProfile,
    ProfileSection.FoodPreference,
    ProfileSection.MysticGuide,
    ProfileSection.About,
    ProfileSection.Privacy
)
