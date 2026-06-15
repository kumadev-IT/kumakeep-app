package com.kumadev.kumakeep.presentation.wishlistdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumadev.kumakeep.data.local.dao.WishlistDao
import com.kumadev.kumakeep.domain.model.SearchResult
import com.kumadev.kumakeep.domain.model.WishlistEntry
import com.kumadev.kumakeep.domain.usecase.AddGameToWishlistUseCase
import com.kumadev.kumakeep.domain.usecase.DeleteWishlistUseCase
import com.kumadev.kumakeep.domain.usecase.GetWishlistEntriesUseCase
import com.kumadev.kumakeep.domain.usecase.RemoveFromWishlistUseCase
import com.kumadev.kumakeep.domain.usecase.RenameWishlistUseCase
import com.kumadev.kumakeep.domain.usecase.ReorderWishlistUseCase
import com.kumadev.kumakeep.domain.usecase.SearchBggUseCase
import com.kumadev.kumakeep.presentation.SnackbarController
import com.kumadev.kumakeep.presentation.SnackbarEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface WishlistDetailUiState {
    data object Loading : WishlistDetailUiState
    data class Success(
        val wishlistName: String,
        val entries: List<WishlistEntry>
    ) : WishlistDetailUiState
    data object Deleted : WishlistDetailUiState
}

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Results(val items: List<SearchResult>) : SearchUiState
    data object Empty : SearchUiState
}

@HiltViewModel
class WishlistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val wishlistDao: WishlistDao,
    private val getWishlistEntriesUseCase: GetWishlistEntriesUseCase,
    private val removeFromWishlistUseCase: RemoveFromWishlistUseCase,
    private val reorderWishlistUseCase: ReorderWishlistUseCase,
    private val renameWishlistUseCase: RenameWishlistUseCase,
    private val deleteWishlistUseCase: DeleteWishlistUseCase,
    private val searchBggUseCase: SearchBggUseCase,
    private val addGameToWishlistUseCase: AddGameToWishlistUseCase,
    private val snackbarController: SnackbarController
) : ViewModel() {

    val wishlistId: Long = checkNotNull(savedStateHandle["wishlistId"])

    private val _uiState = MutableStateFlow<WishlistDetailUiState>(WishlistDetailUiState.Loading)
    val uiState: StateFlow<WishlistDetailUiState> = _uiState.asStateFlow()

    private val _searchState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private var wishlistName: String = ""
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            val wishlist = wishlistDao.getWishlistById(wishlistId)
            wishlistName = wishlist?.name ?: ""
            getWishlistEntriesUseCase(wishlistId).collect { entries ->
                _uiState.value = WishlistDetailUiState.Success(
                    wishlistName = wishlistName,
                    entries = entries
                )
            }
        }
    }

    /** Drag-to-reorder: aggiorna stato locale immediatamente, persiste in background. */
    fun onReorder(newOrder: List<WishlistEntry>) {
        val current = _uiState.value as? WishlistDetailUiState.Success ?: return
        _uiState.value = current.copy(entries = newOrder)
        viewModelScope.launch {
            reorderWishlistUseCase(wishlistId, newOrder)
        }
    }

    fun removeEntry(bggId: Long, gameName: String) {
        viewModelScope.launch {
            removeFromWishlistUseCase(bggId, wishlistId)
            snackbarController.sendEvent(
                SnackbarEvent(
                    message = "\"$gameName\" rimosso dalla wishlist",
                    actionLabel = "Annulla",
                    onAction = {
                        viewModelScope.launch { addGameToWishlistUseCase(bggId, wishlistId) }
                    }
                )
            )
        }
    }

    fun toggleToBuy(entry: WishlistEntry) {
        viewModelScope.launch {
            val entity = wishlistDao.getEntryById(entry.entryId) ?: return@launch
            wishlistDao.updateEntry(entity.copy(toBuy = !entity.toBuy))
        }
    }

    fun rename(newName: String) {
        viewModelScope.launch {
            renameWishlistUseCase(wishlistId, newName).onSuccess {
                wishlistName = newName.trim()
                val current = _uiState.value as? WishlistDetailUiState.Success ?: return@onSuccess
                _uiState.value = current.copy(wishlistName = wishlistName)
            }
        }
    }

    fun deleteWishlist() {
        viewModelScope.launch {
            deleteWishlistUseCase(wishlistId).onSuccess {
                _uiState.value = WishlistDetailUiState.Deleted
            }
        }
    }

    /** Ricerca BGG con debounce 600ms per aggiungere giochi dalla wishlist detail. */
    fun searchBgg(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchState.value = SearchUiState.Idle
            return
        }
        _searchState.value = SearchUiState.Loading
        searchJob = viewModelScope.launch {
            delay(600)
            searchBggUseCase(query)
                .onSuccess { results ->
                    _searchState.value = if (results.isEmpty()) SearchUiState.Empty
                    else SearchUiState.Results(results)
                }
                .onFailure { _searchState.value = SearchUiState.Idle }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _searchState.value = SearchUiState.Idle
    }

    /** Aggiunge un gioco trovato dalla ricerca alla wishlist corrente. */
    fun addGameToWishlist(bggId: Long, gameName: String) {
        viewModelScope.launch {
            addGameToWishlistUseCase(bggId, wishlistId)
            snackbarController.sendEvent(SnackbarEvent(message = "\"$gameName\" aggiunto alla wishlist"))
            clearSearch()
        }
    }
}
