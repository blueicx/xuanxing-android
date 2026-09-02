package com.xuanji.app.daily

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_BOOT_COMPLETED
import com.xuanji.app.di.AppModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 设备重启后重新安排每日提醒 */
class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_BOOT_COMPLETED) {
            val result = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (AppModule.repository.isDailyReminderOn()) {
                        ReminderScheduler.schedule(context)
                    }
                } finally {
                    result.finish()
                }
            }
        }
    }
}
