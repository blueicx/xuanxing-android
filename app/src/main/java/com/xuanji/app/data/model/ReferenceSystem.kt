package com.xuanji.app.data.model

data class ReferenceSystem(
    val key: String,
    val name: String,
    val region: String,
    val summary: String,
    val concepts: List<String>,
    val canCompute: Boolean,
    val note: String
)
