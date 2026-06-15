package com.kumadev.kumakeep.presentation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

data class SnackbarEvent(
    val message: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

@Singleton
class SnackbarController @Inject constructor() {
    private val _events = MutableSharedFlow<SnackbarEvent>()
    val events = _events.asSharedFlow()

    suspend fun sendEvent(event: SnackbarEvent) {
        _events.emit(event)
    }
}
