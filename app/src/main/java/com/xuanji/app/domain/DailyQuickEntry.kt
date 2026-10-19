package com.xuanji.app.domain

enum class DailyQuickEntry(val tab: String) {
    Agenda("history"), Resume("companion");
    companion object {
        fun from(value: String?): DailyQuickEntry? = entries.firstOrNull { it.tab == value }
        fun resolveDate(action: String?, recordedDate: String?, today: java.time.LocalDate, packageName: String): String? =
            if (action == "$packageName.widget.Agenda") today.toString() else recordedDate
    }
}
