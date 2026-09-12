package com.xuanji.app.data.model

/**
 * 用户出生信息。时间按出生地当地时区（中国默认 Asia/Shanghai）解释。
 * gender 为可空（兼容旧数据），"男" / "女"，用于排大运顺逆。
 */
data class UserProfile(
    val birthYear: Int,
    val birthMonth: Int,   // 1-12
    val birthDay: Int,     // 1-31
    val birthHour: Int,    // 0-23
    val birthMinute: Int,  // 0-59
    val locationName: String,
    val locationCode: String? = null,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val gender: String? = null
)
