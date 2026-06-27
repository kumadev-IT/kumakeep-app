package com.kumadev.kumakeep.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumadev.kumakeep.data.local.preferences.DEFAULT_CAROUSEL_SIZE
import com.kumadev.kumakeep.data.local.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val username: String = "",
    val carouselSize: Int = DEFAULT_CAROUSEL_SIZE
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferences.usernameFlow.collect { name ->
                _uiState.update { it.copy(username = name) }
            }
        }
        viewModelScope.launch {
            userPreferences.carouselSizeFlow.collect { size ->
                _uiState.update { it.copy(carouselSize = size) }
            }
        }
    }

    fun saveUsername(name: String) {
        userPreferences.setUsername(name.trim())
    }

    fun saveCarouselSize(size: Int) {
        val clamped = size.coerceIn(1, 20)
        userPreferences.setCarouselSize(clamped)
    }
}
