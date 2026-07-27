package com.kumadev.kumakeep.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumadev.kumakeep.domain.model.BoardGame
import com.kumadev.kumakeep.domain.model.Tag
import com.kumadev.kumakeep.domain.usecase.AddToLibraryUseCase
import com.kumadev.kumakeep.domain.usecase.GetAllTagsUseCase
import com.kumadev.kumakeep.domain.usecase.GetLibraryUseCase
import com.kumadev.kumakeep.domain.usecase.RemoveFromLibraryUseCase
import com.kumadev.kumakeep.presentation.SnackbarController
import com.kumadev.kumakeep.presentation.SnackbarEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data object Empty : LibraryUiState
    data class Success(val games: List<BoardGame>, val isFiltered: Boolean = false) : LibraryUiState
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getLibraryUseCase: GetLibraryUseCase,
    private val addToLibraryUseCase: AddToLibraryUseCase,
    private val removeFromLibraryUseCase: RemoveFromLibraryUseCase,
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val snackbarController: SnackbarController
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** Tag selezionati per il filtro. Un gioco deve averli tutti (AND) per comparire. */
    private val _selectedTagIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTagIds: StateFlow<Set<Long>> = _selectedTagIds.asStateFlow()

    /** Tutti i tag esistenti, per la riga di filtro chip. */
    val allTags: StateFlow<List<Tag>> = getAllTagsUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val uiState: StateFlow<LibraryUiState> = combine(
        getLibraryUseCase(),
        _searchQuery,
        _selectedTagIds
    ) { games, query, tagIds ->
        val byQuery = if (query.isBlank()) games
        else games.filter { it.primaryName.contains(query, ignoreCase = true) }
        val filtered = if (tagIds.isEmpty()) byQuery
        else byQuery.filter { game -> tagIds.all { id -> game.tags.any { it.id == id } } }
        val isFiltered = query.isNotBlank() || tagIds.isNotEmpty()
        when {
            games.isEmpty() -> LibraryUiState.Empty
            filtered.isEmpty() -> LibraryUiState.Success(emptyList(), isFiltered = true)
            else -> LibraryUiState.Success(filtered, isFiltered = isFiltered)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState.Loading
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleTagFilter(tagId: Long) {
        _selectedTagIds.value = _selectedTagIds.value.let { current ->
            if (tagId in current) current - tagId else current + tagId
        }
    }

    fun clearTagFilters() {
        _selectedTagIds.value = emptySet()
    }

    fun removeFromLibrary(bggId: Long, gameName: String) {
        viewModelScope.launch {
            removeFromLibraryUseCase(bggId)
                .onSuccess {
                    snackbarController.sendEvent(
                        SnackbarEvent(
                            message = "\"$gameName\" rimosso dalla libreria",
                            actionLabel = "Annulla",
                            onAction = {
                                viewModelScope.launch { addToLibraryUseCase(bggId) }
                            }
                        )
                    )
                }
                .onFailure {
                    snackbarController.sendEvent(SnackbarEvent(message = "Errore durante la rimozione"))
                }
        }
    }
}