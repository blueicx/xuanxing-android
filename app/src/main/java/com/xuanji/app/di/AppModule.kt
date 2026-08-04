package com.xuanji.app.di

import com.xuanji.app.data.repository.FortuneRepository
import com.xuanji.app.data.repository.HistoryRepository
import com.xuanji.app.data.repository.LiuYaoRepository
import com.xuanji.app.data.repository.ReferenceRepository
import com.xuanji.app.data.repository.TarotRepository
import com.xuanji.app.data.repository.TestRecordRepository

/** 轻量级依赖容器（单例 Repository） */
object AppModule {
    lateinit var repository: FortuneRepository
        private set
    lateinit var historyRepository: HistoryRepository
        private set
    lateinit var tarotRepository: TarotRepository
        private set
    lateinit var liuYaoRepository: LiuYaoRepository
        private set
    lateinit var referenceRepository: ReferenceRepository
        private set
    lateinit var testRecordRepository: TestRecordRepository
        private set

    fun init(
        repository: FortuneRepository,
        historyRepository: HistoryRepository,
        tarotRepository: TarotRepository,
        liuYaoRepository: LiuYaoRepository,
        referenceRepository: ReferenceRepository,
        testRecordRepository: TestRecordRepository
    ) {
        this.repository = repository
        this.historyRepository = historyRepository
        this.tarotRepository = tarotRepository
        this.liuYaoRepository = liuYaoRepository
        this.referenceRepository = referenceRepository
        this.testRecordRepository = testRecordRepository
    }
}
