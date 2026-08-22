package com.kumadev.kumakeep.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumadev.kumakeep.data.local.preferences.UserPreferences
import com.kumadev.kumakeep.domain.model.BoardGame
import com.kumadev.kumakeep.domain.model.HotGame
import com.kumadev.kumakeep.domain.repository.BoardGameRepository
import com.kumadev.kumakeep.domain.usecase.GetLibraryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val username: String = "",
    val carouselSize: Int = 10,
    val libraryCount: Int = 0,
    val wishlistCount: Int = 0,
    val playedCount: Int = 0,
    val expansionCount: Int = 0,
    val recentlyAdded: List<BoardGame> = emptyList(),
    val recentlyViewed: List<BoardGame> = emptyList(),
    // Hot list BGG: chiamata di rete singola e indipendente dal resto della Home,
    // stato di caricamento separato apposta (non deve bloccare/essere bloccata
    // dagli altri caroselli, che sono reattivi su DB locale).
    val isHotLoading: Boolean = true,
    val hotGames: List<HotGame> = emptyList()
)

private data class LibrarySnapshot(
    val games: List<BoardGame>,
    val playedCount: Int,
    val wishlistCount: Int,
    val expansionCount: Int,
    val carouselSize: Int
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getLibraryUseCase: GetLibraryUseCase,
    private val repository: BoardGameRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Username
        viewModelScope.launch {
            userPreferences.usernameFlow.collect { name ->
                _uiState.update { it.copy(username = name) }
            }
        }

        // Carousel size (for SettingsScreen to reflect current value)
        viewModelScope.launch {
            userPreferences.carouselSizeFlow.collect { size ->
                _uiState.update { it.copy(carouselSize = size) }
            }
        }

        // Library stats + recently added (reacts to carousel size changes too)
        viewModelScope.launch {
            combine(
                getLibraryUseCase(),
                repository.getPlayedCount(),
                repository.getWishlistGameCount(),
                repository.getOwnedExpansionCount(),
                userPreferences.carouselSizeFlow
            ) { args ->
                @Suppress("UNCHECKED_CAST")
                LibrarySnapshot(
                    games = args[0] as List<BoardGame>,
                    playedCount = args[1] as Int,
                    wishlistCount = args[2] as Int,
                    expansionCount = args[3] as Int,
                    carouselSize = args[4] as Int
                )
            }.collect { snap ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        libraryCount = snap.games.size,
                        playedCount = snap.playedCount,
                        wishlistCount = snap.wishlistCount,
                        expansionCount = snap.expansionCount,
                        recentlyAdded = snap.games
                            .sortedByDescending { g -> g.libraryEntry?.createdAt ?: 0L }
                            .take(snap.carouselSize)
                    )
                }
            }
        }

        // Recently viewed
        viewModelScope.launch {
            userPreferences.recentlyViewedIdsFlow
                .flatMapLatest { ids ->
                    if (ids.isEmpty()) flowOf(emptyList())
                    else repository.getRecentlyViewedGames(ids)
                }
                .collect { games ->
                    _uiState.update { it.copy(recentlyViewed = games) }
                }
        }

        // Hot list BGG — chiamata di rete singola (non reattiva come le altre
        // liste della Home), cache 1h lato repository: non si ripete ad ogni
        // ritorno sulla tab Home nella stessa sessione (il ViewModel sopravvive
        // ai cambi tab, vedi punto 26 del todo).
        viewModelScope.launch {
            repository.getHotGames()
                .onSuccess { games ->
                    _uiState.update { it.copy(hotGames = games, isHotLoading = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(isHotLoading = false) }
                }
        }
    }
}
