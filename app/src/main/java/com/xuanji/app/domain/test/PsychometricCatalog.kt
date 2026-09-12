package com.xuanji.app.domain.test

enum class InstrumentAvailability { Open, LicensedOnly, NotConfigured }

data class InstrumentDescriptor(
    val id: String,
    val displayName: String,
    val availability: InstrumentAvailability,
    val claims: String
)

object PsychometricCatalog {
    val instruments = listOf(
        InstrumentDescriptor("ipip-big5-50", "IPIP Big Five 50（开放题库）", InstrumentAvailability.Open, "开放题库；结果为人格维度自我探索，不是临床诊断"),
        InstrumentDescriptor("mmpi", "MMPI / MMPI-3", InstrumentAvailability.LicensedOnly, "需版权题目、手册与授权常模"),
        InstrumentDescriptor("raven", "Raven", InstrumentAvailability.LicensedOnly, "需授权题目与年龄/地区常模"),
        InstrumentDescriptor("16pf", "16PF", InstrumentAvailability.LicensedOnly, "需版权题目、手册与授权常模"),
        InstrumentDescriptor("mbti", "MBTI", InstrumentAvailability.LicensedOnly, "需认证/授权题目、报告与常模")
    )
}
