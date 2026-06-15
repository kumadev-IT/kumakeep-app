package com.kumadev.kumakeep.presentation.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumadev.kumakeep.domain.model.Wishlist
import com.kumadev.kumakeep.domain.usecase.CreateWishlistUseCase
import com.kumadev.kumakeep.domain.usecase.GetWishlistsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface WishlistUiState {
    data object Loading : WishlistUiState
    data class Success(val wishlists: List<Wishlist>) : WishlistUiState
    data object Empty : WishlistUiState
}

@HiltViewModel
class WishlistViewModel @Inject constructor(
    private val getWishlistsUseCase: GetWishlistsUseCase,
    private val createWishlistUseCase: CreateWishlistUseCase
) : ViewModel() {

    val uiState: StateFlow<WishlistUiState> =
        getWishlistsUseCase().let { flow ->
            MutableStateFlow<WishlistUiState>(WishlistUiState.Loading).also { state ->
                viewModelScope.launch {
                    flow.collect { wishlists ->
                        state.value = if (wishlists.isEmpty()) WishlistUiState.Empty
                        else WishlistUiState.Success(wishlists)
                    }
                }
            }
        }

    fun createWishlist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            createWishlistUseCase(name)
        }
    }
}
