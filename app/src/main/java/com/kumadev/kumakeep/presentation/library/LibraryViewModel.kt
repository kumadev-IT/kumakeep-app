package com.kumadev.kumakeep.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumadev.kumakeep.domain.model.BoardGame
import com.kumadev.kumakeep.domain.usecase.GetLibraryUseCase
import com.kumadev.kumakeep.domain.usecase.RemoveFromLibraryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data object Empty : LibraryUiState
    data class Success(val games: List<BoardGame>) : LibraryUiState
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getLibraryUseCase: GetLibraryUseCase,
    private val removeFromLibraryUseCase: RemoveFromLibraryUseCase
) : ViewModel() {

    val uiState: StateFlow<LibraryUiState> = getLibraryUseCase()
        .map { games ->
            if (games.isEmpty()) LibraryUiState.Empty
            else LibraryUiState.Success(games)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LibraryUiState.Loading
        )

    fun removeFromLibrary(bggId: Long) {
        viewModelScope.launch {
            removeFromLibraryUseCase(bggId)
        }
    }
}