package com.xuanji.app.domain

import com.xuanji.app.data.model.UserProfile
import java.time.LocalDate

enum class ProfileInputAvailability { Available, Missing }

data class ProfileInputReadiness(
    val key: String,
    val label: String,
    val availability: ProfileInputAvailability,
    val optional: Boolean,
    val impact: String
)

data class BirthProfileReadiness(val fields: List<ProfileInputReadiness>) {
    val missingFields: List<ProfileInputReadiness>
        get() = fields.filter { it.availability == ProfileInputAvailability.Missing }
}

/** Reports saved input coverage only; it does not alter chart confidence or infer missing values. */
object BirthProfileReadinessEvaluator {
    fun evaluate(profile: UserProfile?): BirthProfileReadiness {
        val validDate = profile?.let {
            runCatching { LocalDate.of(it.birthYear, it.birthMonth, it.birthDay) }.isSuccess
        } == true
        val validTime = profile?.let { it.birthHour in 0..23 && it.birthMinute in 0..59 } == true
        val hasPlace = !profile?.locationName.isNullOrBlank()
        val hasCoordinates = profile?.let { value ->
            val latitude = value.locationLat
            val longitude = value.locationLng
            latitude != null && longitude != null && latitude.isFinite() && longitude.isFinite() &&
                latitude in -90.0..90.0 && longitude in -180.0..180.0
        } == true
        val hasGender = profile?.gender in setOf("男", "女")
        return BirthProfileReadiness(
            fields = listOf(
                ProfileInputReadiness("date", "出生日期", available(validDate), optional = false,
                    "用于本命与周期计算；未保存时相关盘面不会生成。"),
                ProfileInputReadiness("time", "出生时间", available(validTime), optional = false,
                    "用于时柱及依赖出生时刻的计算；系统不替你猜测时辰。"),
                ProfileInputReadiness("place", "出生地点", available(hasPlace), optional = true,
                    "用于地点相关展示与地区匹配；缺少时不会编造城市。"),
                ProfileInputReadiness("coordinates", "出生地坐标", available(hasCoordinates), optional = true,
                    if (hasCoordinates) "档案已保存经纬度。" else "未保存经纬度；只使用已选地点名称，不推测精确坐标。"),
                ProfileInputReadiness("gender", "性别资料", available(hasGender), optional = true,
                    "仅影响大运顺逆等明确依赖性别的规则；其他计算不根据此项作人格推断。")
            )
        )
    }

    private fun available(value: Boolean) =
        if (value) ProfileInputAvailability.Available else ProfileInputAvailability.Missing
}
