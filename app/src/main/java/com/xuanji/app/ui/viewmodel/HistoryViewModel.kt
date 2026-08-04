package com.xuanji.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xuanji.app.data.model.HistoryEvent
import com.xuanji.app.data.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: HistoryRepository) : ViewModel() {
    private val _events = MutableStateFlow<List<HistoryEvent>>(emptyList())
    val events: StateFlow<List<HistoryEvent>> = _events.asStateFlow()

    init {
        viewModelScope.launch {
            _events.value = repository.eventsForToday()
        }
    }
}
