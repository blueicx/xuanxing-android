package com.xuanji.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xuanji.app.data.model.DrawnTarot
import com.xuanji.app.data.model.TarotCard
import com.xuanji.app.data.repository.TarotRepository
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

    init {
        viewModelScope.launch { _deck.value = repository.loadDeck() }
    }

    fun setSpread(s: String) {
        _spread.value = s
        _drawn.value = emptyList()
    }

    fun draw() {
        val d = _deck.value
        if (d.isEmpty()) return
        val count = if (_spread.value == "three") 3 else 1
        val positions = if (count == 3) listOf("过去", "现在", "未来") else listOf("指引")
        val indices = d.indices.shuffled(Random).take(count)
        _drawn.value = indices.mapIndexed { i, idx ->
            DrawnTarot(d[idx], Random.nextBoolean(), positions[i])
        }
    }

    fun clear() { _drawn.value = emptyList() }
}
