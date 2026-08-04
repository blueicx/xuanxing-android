package com.xuanji.app.data.model

/** 塔罗牌 */
data class TarotCard(
    val nameCn: String,
    val nameEn: String,
    val arcana: String,   // major / minor
    val suit: String,     // 权杖/圣杯/宝剑/星币/""(大阿尔卡纳)
    val upright: String,
    val reversed: String
)

/** 已抽取的牌（含正逆位） */
data class DrawnTarot(
    val card: TarotCard,
    val reversed: Boolean,
    val position: String  // 单张/过去/现在/未来
)
