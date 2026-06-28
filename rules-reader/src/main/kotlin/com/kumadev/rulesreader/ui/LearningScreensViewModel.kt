package com.kumadev.rulesreader.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumadev.rulesreader.db.RulesReaderDatabase
import com.kumadev.rulesreader.db.entity.GeneratedScreenEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LearningScreensViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    database: RulesReaderDatabase
) : ViewModel() {

    val gameId: Long = checkNotNull(savedStateHandle["gameId"])

    val screens: StateFlow<List<GeneratedScreenEntity>> =
        database.generatedScreenDao().observeByRulebookId(gameId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )
}
