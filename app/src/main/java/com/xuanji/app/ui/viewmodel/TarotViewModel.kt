package com.xuanji.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xuanji.app.data.model.DrawnTarot
import com.xuanji.app.data.model.TarotCard
import com.xuanji.app.data.repository.TarotRepository
import com.xuanji.app.domain.divination.DeterministicTarot
import com.xuanji.app.domain.divination.DivinationQuery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class TarotViewModel(private val repository: TarotRepository) : ViewModel() {

    private val _deck = MutableStateFlow<List<TarotCard>>(emptyList())
    val deck: StateFlow<List<TarotCard>> = _deck.asStateFlow()

    private val _drawn = MutableStateFlow<List<DrawnTarot>>(emptyList())
    val drawn: StateFlow<List<DrawnTarot>> = _drawn.asStateFlow()

    private val _spread = MutableStateFlow("single") // single / three
    val spread: StateFlow<String> = _spread.asStateFlow()

    private val _stableSeed = MutableStateFlow<String?>(null)
    val stableSeed: StateFlow<String?> = _stableSeed.asStateFlow()

    private val _stableExplanation = MutableStateFlow<String?>(null)
    val stableExplanation: StateFlow<String?> = _stableExplanation.asStateFlow()

    init {
        viewModelScope.launch { _deck.value = repository.loadDeck() }
    }

    fun setSpread(s: String) {
        _spread.value = s
        _drawn.value = emptyList()
        _stableSeed.value = null
        _stableExplanation.value = null
    }

    fun draw() {
        val d = _deck.value
        if (d.isEmpty()) return
        val count = when (_spread.value) {
            "three" -> 3
            "five" -> 5
            else -> 1
        }
        val positions = when (count) {
            3 -> listOf("过去", "现在", "未来")
            5 -> listOf("主题", "阻力", "资源", "行动", "走向")
            else -> listOf("指引")
        }
        val indices = d.indices.shuffled(Random).take(count)
        _drawn.value = indices.mapIndexed { i, idx ->
            DrawnTarot(d[idx], Random.nextBoolean(), positions[i])
        }
        _stableSeed.value = null
        _stableExplanation.value = null
    }

    fun drawDeterministic(query: DivinationQuery) {
        val d = _deck.value
        if (d.isEmpty()) return
        val reading = DeterministicTarot.read(d, query.copy(spread = _spread.value))
        _drawn.value = reading.cards
        _stableSeed.value = reading.seed
        _stableExplanation.value = reading.explanation
    }

    fun clear() { _drawn.value = emptyList() }
}
