package com.xuanji.app.ui.components

import androidx.compose.runtime.compositionLocalOf
import com.xuanji.app.domain.MysticDetailContext

data class MysticReadingMetadata(
    val mode: String = "综合",
    val period: String = "今日",
    val dateKey: String = ""
)

/** Bridges a detail page to the already-mounted companion without replacing its navigation stack. */
val LocalMysticDetailQuestion = compositionLocalOf<((MysticDetailContext, () -> Unit) -> Unit)?> { null }

val LocalMysticReadingMetadata = compositionLocalOf { MysticReadingMetadata() }

const val MYSTIC_DETAIL_QUESTION_ACTION = "__ask_about_current_detail__"
