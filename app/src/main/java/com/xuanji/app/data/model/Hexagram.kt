package com.xuanji.app.data.model

/** 周易一卦（由 hexagrams.json 载入，京房八宫推导） */
data class Hexagram(
    val kingWen: Int,        // 文王序 1-64
    val name: String,       // 乾
    val binary: String,     // 6 位 0/1，自下而上（0=阴 1=阳）
    val lower: String,      // 下卦 key（QIAN...）
    val upper: String,      // 上卦 key
    val palace: String,     // 卦宫五行（金/木/水/火/土）
    val world: Int,         // 世爻 1-6（1=初爻）
    val judgment: String    // 卦辞
)
