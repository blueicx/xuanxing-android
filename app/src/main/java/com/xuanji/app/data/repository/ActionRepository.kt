package com.xuanji.app.data.repository

import com.xuanji.app.data.local.FoodPreferenceStore
import com.xuanji.app.data.model.UserProfile
import com.xuanji.app.domain.action.CityProfileCatalog
import com.xuanji.app.domain.action.DailyActionInput
import com.xuanji.app.domain.action.DailyActionPlan
import com.xuanji.app.domain.action.DailyActionPlanner
import com.xuanji.app.domain.action.FoodPreference
import com.xuanji.app.domain.action.LifeProfile
import com.xuanji.app.domain.action.LifeProfileInput
import com.xuanji.app.domain.action.LifeProfilePlanner
import com.xuanji.app.data.model.Element
import com.xuanji.app.domain.ZodiacCalculator
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import java.time.LocalDate

class ActionRepository(
    private val fortuneRepository: FortuneRepository,
    private val testRecords: TestRecordRepository,
    private val preferences: FoodPreferenceStore,
    private val dailyPlanner: DailyActionPlanner = DailyActionPlanner(),
    private val lifePlanner: LifeProfilePlanner = LifeProfilePlanner()
) {
    suspend fun loadToday(date: LocalDate = LocalDate.now()): DailyActionPlan? {
        val profile = fortuneRepository.userProfileState.value ?: return null
        val bazi = fortuneRepository.baziFullFlow.value ?: return null
        val natal = fortuneRepository.natalChartFlow.value ?: return null
        val western = ZodiacCalculator.detailFromChart(natal).sun
        val fortune = fortuneRepository.getCompositeFortune(
            bazi.chart,
            western,
            date,
            "day"
        )
        val profileKey = profileKey(profile)
        val preference = preferences.read(profileKey)
        return dailyPlanner.plan(
            DailyActionInput(
                profileKey = profileKey,
                date = date,
                chart = bazi.chart,
                zodiacKey = western.sign,
                zodiacElement = western.element.toElement(),
                fortune = fortune,
                city = cityFrom(profile),
                preference = preference
            )
        )
    }

    suspend fun loadLifeProfile(): LifeProfile? {
        val profile = fortuneRepository.userProfileState.value ?: return null
        val bazi = fortuneRepository.baziFullFlow.value ?: return null
        val natal = fortuneRepository.natalChartFlow.value ?: return null
        val western = ZodiacCalculator.detailFromChart(natal).sun
        val key = profileKey(profile)
        return lifePlanner.plan(
            LifeProfileInput(
                profileKey = key,
                chart = bazi.chart,
                zodiacKey = western.sign,
                zodiacElement = western.element.toElement(),
                tests = testRecords.records.first(),
                preferredCityKey = cityFrom(profile)?.key
            )
        )
    }

    suspend fun readFoodPreference(profileKey: String): FoodPreference = preferences.read(profileKey)

    suspend fun saveFoodPreference(profileKey: String, preference: FoodPreference) {
        preferences.save(profileKey, preference)
    }

    suspend fun clearFoodPreference(profileKey: String) {
        preferences.clear(profileKey)
    }

    fun profileKey(profile: UserProfile): String = Companion.profileKey(profile)

    private fun cityFrom(profile: UserProfile) =
        CityProfileCatalog.find(profile.locationName.split('/').getOrNull(1)?.trim())
            ?: CityProfileCatalog.find(profile.locationName)

    companion object {
        fun profileKey(profile: UserProfile): String {
            val raw = listOf(
                profile.birthYear, profile.birthMonth, profile.birthDay,
                profile.birthHour, profile.birthMinute, profile.locationCode.orEmpty()
            ).joinToString("|")
            return MessageDigest.getInstance("SHA-256")
                .digest(raw.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
        }
    }
}

private fun String.toElement(): Element = when (this) {
    "木" -> Element.WOOD
    "火" -> Element.FIRE
    "土" -> Element.EARTH
    "金" -> Element.METAL
    else -> Element.WATER
}
