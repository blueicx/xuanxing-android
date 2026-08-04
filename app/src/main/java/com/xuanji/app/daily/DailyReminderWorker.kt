package com.xuanji.app.daily

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.xuanji.app.MainActivity
import com.xuanji.app.R

class DailyReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        showNotification()
        return ListenableWorker.Result.success()
    }

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
