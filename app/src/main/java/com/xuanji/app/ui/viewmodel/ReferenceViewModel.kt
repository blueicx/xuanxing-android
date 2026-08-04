package com.xuanji.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.xuanji.app.data.model.ReferenceSystem
import com.xuanji.app.data.repository.ReferenceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReferenceViewModel(private val repository: ReferenceRepository, private val key: String) : ViewModel() {

    private val _entry = MutableStateFlow<ReferenceSystem?>(null)
    val entry: StateFlow<ReferenceSystem?> = _entry.asStateFlow()

    init {
        _entry.value = repository.getByKey(key)
    }
}
