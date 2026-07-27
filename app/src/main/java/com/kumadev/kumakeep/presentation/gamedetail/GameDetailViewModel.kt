package com.kumadev.kumakeep.presentation.gamedetail

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumadev.kumakeep.domain.model.BaseGameRef
import com.kumadev.kumakeep.domain.model.BoardGame
import com.kumadev.kumakeep.domain.model.Rulebook
import com.kumadev.kumakeep.domain.model.Tag
import com.kumadev.kumakeep.domain.model.WishlistWithStatus
import com.kumadev.kumakeep.domain.model.NumPlays
import com.kumadev.kumakeep.domain.model.UserRate
import com.kumadev.kumakeep.domain.usecase.AddOwnedExpansionUseCase
import com.kumadev.kumakeep.domain.usecase.AddToLibraryUseCase
import com.kumadev.kumakeep.domain.usecase.AddToWishlistsUseCase
import com.kumadev.kumakeep.domain.usecase.AssignTagToGameUseCase
import com.kumadev.kumakeep.domain.usecase.CreateTagUseCase
import com.kumadev.kumakeep.domain.usecase.DeleteRulebookUseCase
import com.kumadev.kumakeep.domain.usecase.GetAllTagsUseCase
import com.kumadev.kumakeep.domain.usecase.GetExpansionBaseLinksUseCase
import com.kumadev.kumakeep.domain.usecase.GetGameDetailUseCase
import com.kumadev.kumakeep.domain.usecase.GetOwnedBaseCandidatesUseCase
import com.kumadev.kumakeep.domain.usecase.GetOwnedExpansionsUseCase
import com.kumadev.kumakeep.domain.usecase.GetRulebookUseCase
import com.kumadev.kumakeep.domain.usecase.GetWishlistsForGameUseCase
import com.kumadev.kumakeep.domain.usecase.ImportRulebookUseCase
import com.kumadev.kumakeep.domain.usecase.RemoveFromLibraryUseCase
import com.kumadev.kumakeep.domain.usecase.RemoveOwnedExpansionUseCase
import com.kumadev.kumakeep.domain.usecase.RemoveTagFromGameUseCase
import com.kumadev.kumakeep.domain.usecase.UpdateLibraryEntryUseCase
import com.kumadev.kumakeep.data.local.preferences.UserPreferences
import com.kumadev.kumakeep.presentation.SnackbarController
import com.kumadev.kumakeep.presentation.SnackbarEvent
import com.kumadev.kumakeep.util.PendingPdfHolder
import com.kumadev.rulesreader.RulesReader
import com.kumadev.rulesreader.model.ProcessingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
    private val updateLibraryEntryUseCase: UpdateLibraryEntryUseCase,
    private val getWishlistsForGameUseCase: GetWishlistsForGameUseCase,
    private val addToWishlistsUseCase: AddToWishlistsUseCase,
    private val getOwnedExpansionsUseCase: GetOwnedExpansionsUseCase,
    private val getExpansionBaseLinksUseCase: GetExpansionBaseLinksUseCase,
    private val addOwnedExpansionUseCase: AddOwnedExpansionUseCase,
    private val removeOwnedExpansionUseCase: RemoveOwnedExpansionUseCase,
    private val getOwnedBaseCandidatesUseCase: GetOwnedBaseCandidatesUseCase,
    private val getRulebookUseCase: GetRulebookUseCase,
    private val importRulebookUseCase: ImportRulebookUseCase,
    private val deleteRulebookUseCase: DeleteRulebookUseCase,
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val createTagUseCase: CreateTagUseCase,
    private val assignTagToGameUseCase: AssignTagToGameUseCase,
    private val removeTagFromGameUseCase: RemoveTagFromGameUseCase,
    private val snackbarController: SnackbarController,
    private val userPreferences: UserPreferences,
    private val rulesReader: RulesReader
) : ViewModel() {

    val bggId: Long = checkNotNull(savedStateHandle["bggId"])

    private val _uiState = MutableStateFlow<GameDetailUiState>(GameDetailUiState.Loading)
    val uiState: StateFlow<GameDetailUiState> = _uiState.asStateFlow()

    private val _libraryAction = MutableStateFlow<LibraryAction?>(null)
    val libraryAction: StateFlow<LibraryAction?> = _libraryAction

    val wishlistsForGame: StateFlow<List<WishlistWithStatus>> =
        getWishlistsForGameUseCase(bggId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** Espansioni possedute collegate a questo gioco (rilevante se è un base). */
    val ownedExpansions: StateFlow<List<BoardGame>> =
        getOwnedExpansionsUseCase(bggId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** Giochi base a cui questa espansione è collegata (vuoto = non posseduta). */
    val expansionBaseLinks: StateFlow<List<Long>> =
        getExpansionBaseLinksUseCase(bggId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** Basi possedute tra cui scegliere quando un'espansione ne estende più d'una. */
    private val _baseChooser = MutableStateFlow<List<BaseGameRef>?>(null)
    val baseChooser: StateFlow<List<BaseGameRef>?> = _baseChooser.asStateFlow()

    val rulebook: StateFlow<Rulebook?> =
        getRulebookUseCase(bggId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val rulebookProcessingState: StateFlow<ProcessingState> =
        rulesReader.getProcessingState(bggId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProcessingState.Idle
        )

    private val _showWishlistDialog = MutableStateFlow(false)
    val showWishlistDialog: StateFlow<Boolean> = _showWishlistDialog.asStateFlow()

    private val _showRatingSheet = MutableStateFlow(false)
    val showRatingSheet: StateFlow<Boolean> = _showRatingSheet.asStateFlow()

    private val _showNumPlaysSheet = MutableStateFlow(false)
    val showNumPlaysSheet: StateFlow<Boolean> = _showNumPlaysSheet.asStateFlow()

    private val _showDeleteRulebookDialog = MutableStateFlow(false)
    val showDeleteRulebookDialog: StateFlow<Boolean> = _showDeleteRulebookDialog.asStateFlow()

    /** Tutti i tag definiti dall'utente (per il picker). */
    val allTags: StateFlow<List<Tag>> =
        getAllTagsUseCase().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _showTagSheet = MutableStateFlow(false)
    val showTagSheet: StateFlow<Boolean> = _showTagSheet.asStateFlow()

    private val _showCreateTagDialog = MutableStateFlow(false)
    val showCreateTagDialog: StateFlow<Boolean> = _showCreateTagDialog.asStateFlow()

    /** URI ricevuto via ACTION_SEND, in attesa di conferma utente */
    private val _pendingImport = MutableStateFlow<Pair<Uri, String>?>(null)
    val pendingImport: StateFlow<Pair<Uri, String>?> = _pendingImport.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    init {
        userPreferences.addRecentlyViewed(bggId)
        loadGame()
        // Controlla se c'è un PDF in attesa (condivisione via ACTION_SEND)
        PendingPdfHolder.consume()?.let { _pendingImport.value = it }
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
                        snackbarController.sendEvent(
                            SnackbarEvent(
                                message = "\"${state.game.primaryName}\" rimosso dalla libreria",
                                actionLabel = "Annulla",
                                onAction = {
                                    viewModelScope.launch { addToLibraryUseCase(bggId).onSuccess { loadGame() } }
                                }
                            )
                        )
                    }
                    .onFailure {
                        snackbarController.sendEvent(SnackbarEvent(message = "Errore durante la rimozione"))
                    }
            } else {
                addToLibraryUseCase(bggId)
                    .onSuccess {
                        _libraryAction.value = LibraryAction.Added
                        loadGame()
                        snackbarController.sendEvent(
                            SnackbarEvent(message = "\"${state.game.primaryName}\" aggiunto alla libreria")
                        )
                    }
                    .onFailure {
                        snackbarController.sendEvent(SnackbarEvent(message = "Errore durante l'aggiunta"))
                    }
            }
        }
    }

    // ─── Espansione posseduta ────────────────────────────────────────────────────

    /**
     * Toggle "possiedo questa espansione". Se già posseduta la scollega da tutte
     * le basi; altrimenti la aggancia a una base posseduta (scelta se >1, avviso
     * se nessuna base è in libreria).
     */
    fun toggleOwnedExpansion() {
        val state = _uiState.value as? GameDetailUiState.Success ?: return
        val game = state.game
        if (!game.isExpansion) return

        viewModelScope.launch {
            if (expansionBaseLinks.value.isNotEmpty()) {
                removeOwnedExpansionUseCase(bggId)
                    .onSuccess {
                        snackbarController.sendEvent(
                            SnackbarEvent(message = "\"${game.primaryName}\" rimossa dalle espansioni")
                        )
                    }
                    .onFailure { snackbarController.sendEvent(SnackbarEvent("Errore durante la rimozione")) }
                return@launch
            }

            val candidates = getOwnedBaseCandidatesUseCase(game.baseGames)
            when {
                candidates.isEmpty() -> {
                    val names = game.baseGames.joinToString(", ") { it.name }
                    snackbarController.sendEvent(
                        SnackbarEvent(
                            message = if (names.isBlank()) "Aggiungi prima il gioco base alla libreria"
                            else "Aggiungi prima alla libreria: $names"
                        )
                    )
                }
                candidates.size == 1 -> linkExpansionTo(candidates.first(), game.primaryName)
                else -> _baseChooser.value = candidates
            }
        }
    }

    /** Conferma la scelta della base nel picker (caso espansione con più basi). */
    fun confirmBaseChoice(base: BaseGameRef) {
        val state = _uiState.value as? GameDetailUiState.Success ?: return
        _baseChooser.value = null
        viewModelScope.launch { linkExpansionTo(base, state.game.primaryName) }
    }

    fun dismissBaseChooser() { _baseChooser.value = null }

    private suspend fun linkExpansionTo(base: BaseGameRef, expansionName: String) {
        addOwnedExpansionUseCase(bggId, base.bggId)
            .onSuccess {
                snackbarController.sendEvent(
                    SnackbarEvent(message = "\"$expansionName\" aggiunta a ${base.name}")
                )
            }
            .onFailure { snackbarController.sendEvent(SnackbarEvent("Errore durante l'aggiunta")) }
    }

    // ─── Rulebook ────────────────────────────────────────────────────────────────

    /** Avvia la pipeline di estrazione+chunking+embedding per il regolamento corrente. */
    fun startRulebookProcessing() {
        val rb = rulebook.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            rulesReader.process(bggId, rb.filePath)
        }
    }

    /** Resetta la pipeline per permettere una ri-elaborazione da zero. */
    fun resetRulebookProcessing() {
        viewModelScope.launch(Dispatchers.IO) {
            rulesReader.reset(bggId)
        }
    }

    fun importRulebook(uri: Uri, fileName: String) {
        viewModelScope.launch {
            _isImporting.value = true
            importRulebookUseCase(uri, bggId, fileName)
                .onSuccess {
                    snackbarController.sendEvent(SnackbarEvent("Regolamento importato (${it.pageCount} pagine)"))
                }
                .onFailure {
                    snackbarController.sendEvent(SnackbarEvent("Errore import: ${it.message}"))
                }
            _isImporting.value = false
        }
    }

    /** Chiamato quando l'utente conferma l'import da ACTION_SEND */
    fun confirmPendingImport() {
        val pending = _pendingImport.value ?: return
        _pendingImport.value = null
        importRulebook(pending.first, pending.second)
    }

    fun dismissPendingImport() {
        _pendingImport.value = null
    }

    fun openDeleteRulebookDialog() { _showDeleteRulebookDialog.value = true }
    fun dismissDeleteRulebookDialog() { _showDeleteRulebookDialog.value = false }

    fun deleteRulebook() {
        viewModelScope.launch {
            _showDeleteRulebookDialog.value = false
            deleteRulebookUseCase(bggId)
                .onSuccess { snackbarController.sendEvent(SnackbarEvent("Regolamento rimosso")) }
                .onFailure { snackbarController.sendEvent(SnackbarEvent("Errore durante la rimozione")) }
        }
    }

    // ─── Wishlist / Rating / NumPlays ─────────────────────────────────────────

    fun retry() { loadGame() }

    fun openWishlistDialog() { _showWishlistDialog.value = true }
    fun dismissWishlistDialog() { _showWishlistDialog.value = false }

    fun openRatingSheet() { _showRatingSheet.value = true }
    fun dismissRatingSheet() { _showRatingSheet.value = false }

    fun openNumPlaysSheet() { _showNumPlaysSheet.value = true }
    fun dismissNumPlaysSheet() { _showNumPlaysSheet.value = false }

    fun updateRate(rate: UserRate) {
        viewModelScope.launch {
            _showRatingSheet.value = false
            updateLibraryEntryUseCase(bggId, rate = rate)
                .onSuccess { loadGame() }
                .onFailure { snackbarController.sendEvent(SnackbarEvent("Errore aggiornamento valutazione")) }
        }
    }

    fun updateNumPlays(numPlays: NumPlays) {
        viewModelScope.launch {
            _showNumPlaysSheet.value = false
            updateLibraryEntryUseCase(bggId, numPlays = numPlays)
                .onSuccess { loadGame() }
                .onFailure { snackbarController.sendEvent(SnackbarEvent("Errore aggiornamento partite")) }
        }
    }

    // ─── Tag utente ─────────────────────────────────────────────────────────

    fun openTagSheet() { _showTagSheet.value = true }
    fun dismissTagSheet() { _showTagSheet.value = false }

    fun openCreateTagDialog() { _showCreateTagDialog.value = true }
    fun dismissCreateTagDialog() { _showCreateTagDialog.value = false }

    /** Assegna o rimuove un tag già esistente dal gioco corrente. */
    fun toggleGameTag(tagId: Long) {
        val state = _uiState.value as? GameDetailUiState.Success ?: return
        val alreadyAssigned = state.game.tags.any { it.id == tagId }
        viewModelScope.launch {
            val result = if (alreadyAssigned) {
                removeTagFromGameUseCase(bggId, tagId)
            } else {
                assignTagToGameUseCase(bggId, tagId)
            }
            result
                .onSuccess { loadGame() }
                .onFailure { snackbarController.sendEvent(SnackbarEvent("Errore aggiornamento tag")) }
        }
    }

    /** Crea un nuovo tag e lo assegna subito al gioco corrente. */
    fun createAndAssignTag(name: String, colorHex: String) {
        viewModelScope.launch {
            createTagUseCase(name, colorHex)
                .onSuccess { tag ->
                    _showCreateTagDialog.value = false
                    assignTagToGameUseCase(bggId, tag.id)
                    loadGame()
                }
                .onFailure {
                    snackbarController.sendEvent(SnackbarEvent(it.message ?: "Errore creazione tag"))
                }
        }
    }

    fun saveWishlistSelections(selectedIds: Set<Long>) {
        val previousIds = wishlistsForGame.value
            .filter { it.isSelected }
            .map { it.id }
            .toSet()
        viewModelScope.launch {
            addToWishlistsUseCase(bggId, selectedIds, previousIds)
            _showWishlistDialog.value = false
            val added = selectedIds.size - (selectedIds intersect previousIds).size
            val removed = (previousIds - selectedIds).size
            val message = when {
                added > 0 && removed > 0 -> "Wishlist aggiornate"
                added > 0 -> "Aggiunto alle wishlist"
                removed > 0 -> "Rimosso dalle wishlist"
                else -> return@launch
            }
            snackbarController.sendEvent(SnackbarEvent(message = message))
        }
    }
}

enum class LibraryAction { Added, Removed }
