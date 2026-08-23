package com.xuanji.app.daily

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.xuanji.app.data.local.dataStore
import com.xuanji.app.MainActivity
import com.xuanji.app.R
import kotlinx.coroutines.flow.first

class DailyReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (hasProfile()) {
            showNotification()
        }
        // 无命盘（例如用户已一键清除）则静默跳过，不再打扰
        return ListenableWorker.Result.success()
    }

    private suspend fun hasProfile(): Boolean =
        applicationContext.dataStore.data
            .first()[stringPreferencesKey("user_profile")] != null

    private fun showNotification() {
        val ctx = applicationContext
        val channelId = "fortune_daily"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "每日运势",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            ctx.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
        val intent = Intent(ctx, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            ctx,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(ctx, channelId)
            .setContentTitle(ctx.getString(R.string.notification_title))
            .setContentText(ctx.getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(1001, notification)
    }
}
