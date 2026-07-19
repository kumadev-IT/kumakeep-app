package com.kumadev.kumakeep.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumadev.kumakeep.domain.model.BaseGameRef
import com.kumadev.kumakeep.domain.model.SearchResult
import com.kumadev.kumakeep.domain.usecase.AddOwnedExpansionUseCase
import com.kumadev.kumakeep.domain.usecase.AddToLibraryUseCase
import com.kumadev.kumakeep.domain.usecase.GetOwnedBaseCandidatesForExpansionUseCase
import com.kumadev.kumakeep.domain.usecase.SearchBggUseCase
import com.kumadev.kumakeep.presentation.SnackbarController
import com.kumadev.kumakeep.presentation.SnackbarEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchBggUseCase: SearchBggUseCase,
    private val addToLibraryUseCase: AddToLibraryUseCase,
    private val getOwnedBaseCandidatesForExpansionUseCase: GetOwnedBaseCandidatesForExpansionUseCase,
    private val addOwnedExpansionUseCase: AddOwnedExpansionUseCase,
    private val snackbarController: SnackbarController
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState

    private val _addedToLibrary = MutableStateFlow<Long?>(null)
    val addedToLibrary: StateFlow<Long?> = _addedToLibrary

    /** Scelta del gioco base quando un'espansione ne estende più d'uno posseduto. */
    private val _expansionChooser = MutableStateFlow<ExpansionChooser?>(null)
    val expansionChooser: StateFlow<ExpansionChooser?> = _expansionChooser.asStateFlow()

    init {
        _query
            .debounce(600)
            .distinctUntilChanged()
            .filter { it.length >= 2 }
            .onEach { query -> performSearch(query) }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        if (newQuery.isBlank()) _uiState.value = SearchUiState.Idle
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            searchBggUseCase(query)
                .onSuccess {
                    _uiState.value = if (it.isEmpty()) SearchUiState.Empty
                    else SearchUiState.Success(it)
                }
                .onFailure { _uiState.value = SearchUiState.Error("Errore di rete. Controlla la connessione.") }
        }
    }

    /**
     * Azione del tasto "+": aggiunge il risultato. Un gioco va in libreria; un'
     * espansione viene collegata al gioco base posseduto (stesso flusso del toggle
     * "Possiedo" nel dettaglio): diretta se una sola base, picker se più d'una,
     * avviso se nessuna base è in libreria.
     */
    fun onAddResult(result: SearchResult) {
        if (result.isExpansion) addExpansion(result)
        else addToLibrary(result.bggId, result.name)
    }

    private fun addToLibrary(bggId: Long, gameName: String) {
        viewModelScope.launch {
            addToLibraryUseCase(bggId)
                .onSuccess {
                    _addedToLibrary.value = bggId
                    snackbarController.sendEvent(SnackbarEvent(message = "\"$gameName\" aggiunto alla libreria"))
                }
                .onFailure {
                    snackbarController.sendEvent(SnackbarEvent(message = "Errore durante l'aggiunta"))
                }
        }
    }

    private fun addExpansion(result: SearchResult) {
        viewModelScope.launch {
            getOwnedBaseCandidatesForExpansionUseCase(result.bggId)
                .onSuccess { candidates ->
                    when {
                        candidates.isEmpty() -> snackbarController.sendEvent(
                            SnackbarEvent(message = "Aggiungi prima il gioco base alla libreria")
                        )
                        candidates.size == 1 -> linkExpansion(result.bggId, result.name, candidates.first())
                        else -> _expansionChooser.value =
                            ExpansionChooser(result.bggId, result.name, candidates)
                    }
                }
                .onFailure {
                    snackbarController.sendEvent(SnackbarEvent(message = "Errore durante l'aggiunta"))
                }
        }
    }

    fun confirmExpansionBase(base: BaseGameRef) {
        val chooser = _expansionChooser.value ?: return
        _expansionChooser.value = null
        viewModelScope.launch { linkExpansion(chooser.expansionBggId, chooser.expansionName, base) }
    }

    fun dismissExpansionChooser() { _expansionChooser.value = null }

    private suspend fun linkExpansion(expansionBggId: Long, expansionName: String, base: BaseGameRef) {
        addOwnedExpansionUseCase(expansionBggId, base.bggId)
            .onSuccess {
                snackbarController.sendEvent(
                    SnackbarEvent(message = "\"$expansionName\" aggiunta a ${base.name}")
                )
            }
            .onFailure { snackbarController.sendEvent(SnackbarEvent(message = "Errore durante l'aggiunta")) }
    }
}

/** Stato per il picker della base nella ricerca (espansione con più basi possedute). */
data class ExpansionChooser(
    val expansionBggId: Long,
    val expansionName: String,
    val candidates: List<BaseGameRef>
)