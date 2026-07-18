package com.kumadev.kumakeep.presentation.search

import com.kumadev.kumakeep.domain.model.SearchResult

/**
 * Stato della ricerca BGG, condiviso tra la tab Search e la ricerca "aggiungi gioco"
 * nel dettaglio wishlist.
 */
sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val results: List<SearchResult>) : SearchUiState
    data object Empty : SearchUiState
    data class Error(val message: String) : SearchUiState
}
