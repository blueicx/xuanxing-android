package com.xuanji.app

import android.app.Application
import android.util.Log
import com.xuanji.app.data.repository.FortuneRepository
import com.xuanji.app.data.repository.HistoryRepository
import com.xuanji.app.data.repository.LiuYaoRepository
import com.xuanji.app.data.repository.ReferenceRepository
import com.xuanji.app.data.repository.TarotRepository
import com.xuanji.app.data.repository.TestRecordRepository
import com.xuanji.app.daily.ReminderScheduler
import com.xuanji.app.di.AppModule
import java.io.File
import java.time.LocalDateTime

class XuanjiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        installCrashLogger()
        val repository = FortuneRepository(this)
        AppModule.init(
            repository,
            HistoryRepository(this),
            TarotRepository(this),
            LiuYaoRepository(this),
            ReferenceRepository(this),
            TestRecordRepository(this)
        )

        // 出生信息默认空白（不预填任何生日），由用户自行填写
        // 安排每日运势提醒（默认上午 9 点）
        ReminderScheduler.schedule(this)
    }

    /** 崩溃时把堆栈写入内部文件，便于在 logcat 受限的设备上用 run-as 抓取 */
    private fun installCrashLogger() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val file = File(filesDir, "crash.log")
                file.appendText("=== ${LocalDateTime.now()} thread=${thread.name} ===\n")
                file.appendText(Log.getStackTraceString(throwable))
                file.appendText("\n")
            } catch (_: Throwable) {
                // 忽略日志写入失败，不掩盖原始崩溃
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
