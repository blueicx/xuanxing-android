package com.xuanji.app.daily

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.*
import com.xuanji.app.MainActivity
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.ReminderPreferences
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object AgendaReminderScheduler {
    private const val NAME = "explicit_agenda_reminder"
    internal const val SNOOZE_TAG = "explicit_agenda_snooze"

    fun cancelSnoozes(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(SNOOZE_TAG)
        val notifications = context.getSystemService(NotificationManager::class.java)
        notifications.activeNotifications.filter { it.notification.channelId == "local_agenda" }
            .forEach { notifications.cancel(it.id) }
    }

    fun schedule(context: Context, profileKey: String?, preferences: ReminderPreferences, append: Boolean = false) {
        val manager = WorkManager.getInstance(context)
        val now = LocalDateTime.now()
        val next = preferences.nextAfter(now)
        if (profileKey == null || next == null) {
            manager.cancelUniqueWork(NAME)
            cancelSnoozes(context)
            return
        }
        val request = OneTimeWorkRequestBuilder<AgendaReminderWorker>()
            .setInitialDelay(Duration.between(now, next).toMillis().coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("profile_key" to profileKey)).build()
        manager.enqueueUniqueWork(NAME, if (append) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE, request)
    }
}

class AgendaReminderWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val expected = inputData.getString("profile_key") ?: return Result.success()
        val profile = AppModule.repository.userProfileFlow.first() ?: return Result.success()
        val key = AppModule.actionRepository.profileKey(profile)
        if (key != expected) return Result.success()
        val store = AppModule.dailyLifeStore ?: return Result.success()
        val document = store.load(key)
        if (!document.readable || !document.reminders.enabled) return Result.success()
        val date = LocalDate.now().toString()
        val pending = document.agenda.filter { it.dateKey == date && !it.completed }.sortedWith(compareBy({ it.time.isBlank() }, { it.time }))
        val saved = document.snapshots.filter { it.dateKey == date }
        val pantry = com.xuanji.app.domain.PantryRules.reminderItems(document, LocalDate.now())
        if (LocalDate.now().dayOfWeek.value in document.reminders.weekdays &&
            ((document.reminders.agenda && pending.isNotEmpty()) || (document.reminders.savedResults && saved.isNotEmpty()) || pantry.isNotEmpty())) {
            val allowed = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(applicationContext,
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (allowed) {
                val manager = applicationContext.getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(NotificationChannel("local_agenda", "安排与结果", NotificationManager.IMPORTANCE_DEFAULT))
                val intent = Intent(applicationContext, MainActivity::class.java).putExtra("open_tab", "history")
                    .putExtra("record_date", date).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                val target = PendingIntent.getActivity(applicationContext, 1002, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                val first = pending.firstOrNull().takeIf { document.reminders.agenda }
                val text = buildString {
                    if (first != null) {
                        append(first.time.ifBlank { "时间未定" })
                        if (pending.size > 1) append(" · 还有 ${pending.size - 1} 项待办")
                    } else if (document.reminders.savedResults && saved.isNotEmpty()) append("查看今天主动保存的 ${saved.size} 份结果")
                    if (pantry.isNotEmpty()) {
                        if (isNotEmpty()) append("；")
                        append("${pantry.size} 种库存临近或超过手填期限，请自行核对")
                    }
                }
                val notification = NotificationCompat.Builder(applicationContext, "local_agenda")
                    .setContentTitle(first?.let { "安排提醒 · ${it.title}" } ?: "今天的安排与记录")
                    .setContentText(text).setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentIntent(target).setAutoCancel(true)
                if (first != null) {
                    notification.addAction(0, "标记完成", AgendaReminderActionReceiver.pendingIntent(
                        applicationContext, first, key, AgendaReminderActionReceiver.COMPLETE, 1002
                    ))
                    notification.addAction(0, "1 小时后提醒", AgendaReminderActionReceiver.pendingIntent(
                        applicationContext, first, key, AgendaReminderActionReceiver.SNOOZE, 1002
                    ))
                }
                manager.notify(1002, notification.build())
            }
        }
        AgendaReminderScheduler.schedule(applicationContext, key, document.reminders, append = true)
        return Result.success()
    }
}

/** Explicit notification actions mutate only the referenced agenda item and profile. */
class AgendaReminderActionReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val profileKey = intent.getStringExtra(EXTRA_PROFILE) ?: return
        val itemId = intent.getStringExtra(EXTRA_ITEM) ?: return
        val action = intent.getStringExtra(EXTRA_ACTION) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION, 1002)
        val pendingResult = goAsync()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val profile = AppModule.repository.userProfileFlow.first() ?: return@launch
                if (AppModule.actionRepository.profileKey(profile) != profileKey) return@launch
                val store = AppModule.dailyLifeStore ?: return@launch
                val document = store.load(profileKey)
                if (!document.readable || !document.reminders.enabled || !document.reminders.agenda) return@launch
                val item = document.agenda.firstOrNull {
                    it.id == itemId && !it.completed && it.dateKey == intent.getStringExtra("agenda_date") &&
                        it.time == intent.getStringExtra("agenda_time")
                } ?: return@launch
                when (action) {
                    COMPLETE -> {
                        store.setCompleted(profileKey, itemId, true)
                        androidx.work.WorkManager.getInstance(context).cancelUniqueWork(snoozeWorkName(profileKey, itemId))
                        context.getSystemService(NotificationManager::class.java).cancel(notificationId)
                    }
                    SNOOZE -> {
                        val request = OneTimeWorkRequestBuilder<AgendaSnoozeWorker>()
                            .setInitialDelay(1, TimeUnit.HOURS)
                            .setInputData(Data.Builder().putString(EXTRA_PROFILE, profileKey).putString(EXTRA_ITEM, itemId)
                                .putString("agenda_date", item.dateKey).putString("agenda_time", item.time).build())
                            .addTag(AgendaReminderScheduler.SNOOZE_TAG)
                            .build()
                        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                            snoozeWorkName(profileKey, itemId), ExistingWorkPolicy.REPLACE, request
                        )
                        context.getSystemService(NotificationManager::class.java).cancel(notificationId)
                    }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Preserve the notification when an action could not be saved.
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val COMPLETE = "complete"
        const val SNOOZE = "snooze"
        private const val EXTRA_PROFILE = "profile_key"
        private const val EXTRA_ITEM = "agenda_item_id"
        private const val EXTRA_ACTION = "agenda_action"
        private const val EXTRA_NOTIFICATION = "notification_id"

        fun pendingIntent(context: Context, item: com.xuanji.app.domain.DailyAgendaItem, profileKey: String, action: String, notificationId: Int): PendingIntent {
            val intent = Intent(context, AgendaReminderActionReceiver::class.java)
                .setData(android.net.Uri.parse("xuanji://agenda-action/${com.xuanji.app.domain.action.StableScore.sha256("$profileKey:${item.id}:$action:${item.dateKey}:${item.time}")}"))
                .putExtra(EXTRA_PROFILE, profileKey).putExtra(EXTRA_ITEM, item.id)
                .putExtra(EXTRA_ACTION, action).putExtra(EXTRA_NOTIFICATION, notificationId)
                .putExtra("agenda_date", item.dateKey).putExtra("agenda_time", item.time)
            val requestCode = ("${item.id}:$action".hashCode() and 0x7fffffff)
            return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        fun snoozeWorkName(profileKey: String, itemId: String): String =
            "agenda-snooze-${com.xuanji.app.domain.action.StableScore.sha256("$profileKey:$itemId").take(24)}"
    }
}

class AgendaSnoozeWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val profileKey = inputData.getString("profile_key") ?: return Result.success()
        val itemId = inputData.getString("agenda_item_id") ?: return Result.success()
        val profile = AppModule.repository.userProfileFlow.first() ?: return Result.success()
        if (AppModule.actionRepository.profileKey(profile) != profileKey) return Result.success()
        val store = AppModule.dailyLifeStore ?: return Result.success()
        val document = store.load(profileKey)
        if (!document.readable || !document.reminders.enabled || !document.reminders.agenda) return Result.success()
        val item = document.agenda.firstOrNull {
            it.id == itemId && !it.completed && it.dateKey == inputData.getString("agenda_date") &&
                it.time == inputData.getString("agenda_time")
        } ?: return Result.success()
        val allowed = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(applicationContext,
            Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!allowed) return Result.success()
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("local_agenda", "安排与结果", NotificationManager.IMPORTANCE_DEFAULT))
        val open = Intent(applicationContext, MainActivity::class.java).putExtra("open_tab", "history")
            .putExtra("record_date", item.dateKey).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val content = PendingIntent.getActivity(applicationContext, 1002, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notificationId = 20_000 + (item.id.hashCode() and 0x3fff)
        val notification = NotificationCompat.Builder(applicationContext, "local_agenda")
            .setContentTitle("稍后提醒 · ${item.title}").setContentText("${item.dateKey} · ${item.time.ifBlank { "时间未定" }}")
            .setSmallIcon(android.R.drawable.ic_dialog_info).setContentIntent(content).setAutoCancel(true)
            .addAction(0, "标记完成", AgendaReminderActionReceiver.pendingIntent(applicationContext, item, profileKey, AgendaReminderActionReceiver.COMPLETE, notificationId))
            .addAction(0, "再等 1 小时", AgendaReminderActionReceiver.pendingIntent(applicationContext, item, profileKey, AgendaReminderActionReceiver.SNOOZE, notificationId))
            .build()
        manager.notify(notificationId, notification)
        return Result.success()
    }
}
