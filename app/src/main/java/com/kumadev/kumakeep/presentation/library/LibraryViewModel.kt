package com.kumadev.kumakeep.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumadev.kumakeep.domain.model.BoardGame
import com.kumadev.kumakeep.domain.model.Tag
import com.kumadev.kumakeep.domain.model.UserRate
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

/** Gioco in attesa di conferma di rimozione dalla libreria. */
data class PendingLibraryRemoval(val bggId: Long, val gameName: String)

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

    /**
     * Voti selezionati per il filtro. A differenza dei tag la logica è OR: "Wow + Top"
     * significa "i miei preferiti", non "giochi votati sia Wow sia Top" (impossibile).
     * Set vuoto = nessun filtro sul voto.
     */
    private val _selectedRates = MutableStateFlow<Set<UserRate>>(emptySet())
    val selectedRates: StateFlow<Set<UserRate>> = _selectedRates.asStateFlow()

    /** Gioco per cui è stata richiesta la rimozione, in attesa di conferma dell'utente. */
    private val _pendingRemoval = MutableStateFlow<PendingLibraryRemoval?>(null)
    val pendingRemoval: StateFlow<PendingLibraryRemoval?> = _pendingRemoval.asStateFlow()

    /** Tutti i tag esistenti, per la riga di filtro chip. */
    val allTags: StateFlow<List<Tag>> = getAllTagsUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val uiState: StateFlow<LibraryUiState> = combine(
        getLibraryUseCase(),
        _searchQuery,
        _selectedTagIds,
        _selectedRates
    ) { games, query, tagIds, rates ->
        val byQuery = if (query.isBlank()) games
        else games.filter { it.primaryName.contains(query, ignoreCase = true) }
        val byTags = if (tagIds.isEmpty()) byQuery
        else byQuery.filter { game -> tagIds.all { id -> game.tags.any { it.id == id } } }
        val filtered = if (rates.isEmpty()) byTags
        else byTags.filter { (it.libraryEntry?.rate ?: UserRate.NOT_RATED) in rates }
        // L'ordine di default della libreria (inserimento) resta intatto: si passa a
        // "dal migliore al peggiore" solo quando il filtro voto è attivo, dove serve
        // davvero (es. Wow + Top insieme).
        val sorted = if (rates.isEmpty()) filtered
        else filtered.sortedWith(
            compareByDescending<BoardGame> { it.libraryEntry?.rate?.rank ?: 0 }
                .thenBy { it.primaryName.lowercase() }
        )
        val isFiltered = query.isNotBlank() || tagIds.isNotEmpty() || rates.isNotEmpty()
        when {
            games.isEmpty() -> LibraryUiState.Empty
            sorted.isEmpty() -> LibraryUiState.Success(emptyList(), isFiltered = true)
            else -> LibraryUiState.Success(sorted, isFiltered = isFiltered)
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

    fun toggleRateFilter(rate: UserRate) {
        _selectedRates.value = _selectedRates.value.let { current ->
            if (rate in current) current - rate else current + rate
        }
    }

    fun clearRateFilters() {
        _selectedRates.value = emptySet()
    }

    /** Richiede la rimozione: apre il dialog di conferma invece di rimuovere subito. */
    fun requestRemoveFromLibrary(bggId: Long, gameName: String) {
        _pendingRemoval.value = PendingLibraryRemoval(bggId, gameName)
    }

    fun cancelRemoveFromLibrary() {
        _pendingRemoval.value = null
    }

    fun confirmRemoveFromLibrary() {
        val pending = _pendingRemoval.value ?: return
        _pendingRemoval.value = null
        removeFromLibrary(pending.bggId, pending.gameName)
    }

    private fun removeFromLibrary(bggId: Long, gameName: String) {
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