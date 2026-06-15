package com.kumadev.kumakeep.presentation.gamedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumadev.kumakeep.domain.model.BoardGame
import com.kumadev.kumakeep.domain.model.WishlistWithStatus
import com.kumadev.kumakeep.domain.usecase.AddToLibraryUseCase
import com.kumadev.kumakeep.domain.usecase.AddToWishlistsUseCase
import com.kumadev.kumakeep.domain.usecase.GetGameDetailUseCase
import com.kumadev.kumakeep.domain.usecase.GetWishlistsForGameUseCase
import com.kumadev.kumakeep.domain.usecase.RemoveFromLibraryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface GameDetailUiState {
    data object Loading : GameDetailUiState
    data class Success(val game: BoardGame) : GameDetailUiState
    data class Error(val message: String) : GameDetailUiState
}

@HiltViewModel
class GameDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getGameDetailUseCase: GetGameDetailUseCase,
    private val addToLibraryUseCase: AddToLibraryUseCase,
    private val removeFromLibraryUseCase: RemoveFromLibraryUseCase,
    private val getWishlistsForGameUseCase: GetWishlistsForGameUseCase,
    private val addToWishlistsUseCase: AddToWishlistsUseCase
) : ViewModel() {

    private val bggId: Long = checkNotNull(savedStateHandle["bggId"])

    private val _uiState = MutableStateFlow<GameDetailUiState>(GameDetailUiState.Loading)
    val uiState: StateFlow<GameDetailUiState> = _uiState.asStateFlow()

    private val _libraryAction = MutableStateFlow<LibraryAction?>(null)
    val libraryAction: StateFlow<LibraryAction?> = _libraryAction

    // Stato wishlist per il dialog multi-select
    val wishlistsForGame: StateFlow<List<WishlistWithStatus>> =
        getWishlistsForGameUseCase(bggId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _showWishlistDialog = MutableStateFlow(false)
    val showWishlistDialog: StateFlow<Boolean> = _showWishlistDialog.asStateFlow()

    init {
        loadGame()
    }

    private fun loadGame() {
        viewModelScope.launch {
            _uiState.value = GameDetailUiState.Loading
            getGameDetailUseCase(bggId)
                .onSuccess { _uiState.value = GameDetailUiState.Success(it) }
                .onFailure { _uiState.value = GameDetailUiState.Error(it.message ?: "Errore") }
        }
    }

    fun toggleLibrary() {
        val state = _uiState.value as? GameDetailUiState.Success ?: return
        viewModelScope.launch {
            if (state.game.libraryEntry != null) {
                removeFromLibraryUseCase(bggId)
                    .onSuccess {
                        _libraryAction.value = LibraryAction.Removed
                        loadGame()
                    }
            } else {
                addToLibraryUseCase(bggId)
                    .onSuccess {
                        _libraryAction.value = LibraryAction.Added
                        loadGame()
                    }
            }
        }
    }

    fun openWishlistDialog() { _showWishlistDialog.value = true }
    fun dismissWishlistDialog() { _showWishlistDialog.value = false }

    fun saveWishlistSelections(selectedIds: Set<Long>) {
        val previousIds = wishlistsForGame.value
            .filter { it.isSelected }
            .map { it.id }
            .toSet()
        viewModelScope.launch {
            addToWishlistsUseCase(bggId, selectedIds, previousIds)
            _showWishlistDialog.value = false
        }
    }
}

enum class LibraryAction { Added, Removed }
