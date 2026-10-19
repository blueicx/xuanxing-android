package com.xuanji.app.daily

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.xuanji.app.MainActivity
import com.xuanji.app.R
import com.xuanji.app.domain.DailyQuickEntry

/** Static local shortcuts; never publish private agenda or chat text onto the launcher. */
class DailyQuickEntryWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.daily_quick_entry_widget)
            listOf(R.id.widget_agenda to DailyQuickEntry.Agenda, R.id.widget_resume to DailyQuickEntry.Resume).forEach { (view, entry) ->
                val intent = entryIntent(context, entry)
                val pending = PendingIntent.getActivity(context, 20_100 + entry.ordinal, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(view, pending)
            }
            manager.updateAppWidget(id, views)
        }
    }
    companion object {
        fun entryIntent(context: Context, entry: DailyQuickEntry): Intent = Intent(context, MainActivity::class.java)
            .setAction("${context.packageName}.widget.${entry.name}")
            .putExtra("open_tab", entry.tab)
            .apply { if (entry == DailyQuickEntry.Agenda) putExtra("record_date", java.time.LocalDate.now().toString()) }
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
