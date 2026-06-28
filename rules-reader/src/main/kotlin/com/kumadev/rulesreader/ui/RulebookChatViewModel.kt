package com.kumadev.rulesreader.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumadev.rulesreader.llm.LlmClient
import com.kumadev.rulesreader.rag.RagChatEngine
import com.kumadev.rulesreader.rag.RagChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel per la chat RAG sul regolamento.
 *
 * La history dei messaggi è mantenuta SOLO in-memory (non persistita in Room).
 * [LlmClient] viene iniettato tramite Hilt — il binding è fornito da :app (GeminiModule).
 */
@HiltViewModel
class RulebookChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val ragChatEngine: RagChatEngine,
    private val llmClient: LlmClient
) : ViewModel() {

    val gameId: Long = checkNotNull(savedStateHandle["gameId"])

    private val _messages = MutableStateFlow<List<RagChatMessage>>(emptyList())
    val messages: StateFlow<List<RagChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isLoading.value) return

        val userMsg = RagChatMessage(role = RagChatMessage.Role.User, text = trimmed)
        val currentHistory = _messages.value
        _messages.value = currentHistory + userMsg

        viewModelScope.launch {
            _isLoading.value = true
            val response = ragChatEngine.chat(
                gameId = gameId,
                userMessage = trimmed,
                llmClient = llmClient,
                history = currentHistory   // history prima del messaggio corrente
            )
            _messages.value = _messages.value + response
            _isLoading.value = false
        }
    }
}
