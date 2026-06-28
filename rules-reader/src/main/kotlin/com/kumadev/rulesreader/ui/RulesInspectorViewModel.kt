package com.kumadev.rulesreader.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumadev.rulesreader.db.RulesReaderDatabase
import com.kumadev.rulesreader.db.entity.ExtractedPageEntity
import com.kumadev.rulesreader.db.entity.RulebookChunkEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RulesInspectorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    database: RulesReaderDatabase
) : ViewModel() {

    val gameId: Long = checkNotNull(savedStateHandle["gameId"])

    val pages: StateFlow<List<ExtractedPageEntity>> =
        database.extractedPageDao().observeByRulebookId(gameId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val chunks: StateFlow<List<RulebookChunkEntity>> =
        database.rulebookChunkDao().observeByRulebookId(gameId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )
}
