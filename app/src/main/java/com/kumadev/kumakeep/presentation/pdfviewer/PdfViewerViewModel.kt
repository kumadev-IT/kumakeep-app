package com.kumadev.kumakeep.presentation.pdfviewer

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumadev.kumakeep.domain.usecase.GetRulebookUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import javax.inject.Inject

sealed interface PdfViewerUiState {
    data object Loading : PdfViewerUiState
    data class Ready(val filePath: String, val pageCount: Int) : PdfViewerUiState
    data class Error(val message: String) : PdfViewerUiState
}

@HiltViewModel
class PdfViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getRulebookUseCase: GetRulebookUseCase
) : ViewModel() {

    private val gameId: Long = checkNotNull(savedStateHandle["gameId"])

    private val _uiState = MutableStateFlow<PdfViewerUiState>(PdfViewerUiState.Loading)
    val uiState: StateFlow<PdfViewerUiState> = _uiState.asStateFlow()

    private val _pages = MutableStateFlow<Map<Int, Bitmap>>(emptyMap())
    val pages: StateFlow<Map<Int, Bitmap>> = _pages.asStateFlow()

    private var renderer: PdfRenderer? = null
    private var rendererFd: ParcelFileDescriptor? = null
    private val renderMutex = Mutex()

    init {
        viewModelScope.launch {
            getRulebookUseCase(gameId).collect { rulebook ->
                if (rulebook == null) {
                    _uiState.value = PdfViewerUiState.Error("Nessun regolamento importato per questo gioco")
                    return@collect
                }
                openRenderer(rulebook.filePath, rulebook.pageCount)
            }
        }
    }

    private fun openRenderer(filePath: String, pageCount: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                closeRenderer()
                val file = File(filePath)
                if (!file.exists()) error("File non trovato: $filePath")
                val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                rendererFd = fd
                renderer = PdfRenderer(fd)
                _uiState.value = PdfViewerUiState.Ready(filePath, pageCount)
            }.onFailure {
                _uiState.value = PdfViewerUiState.Error(it.message ?: "Errore apertura PDF")
            }
        }
    }

    /**
     * Richiede il rendering della pagina [index] alla larghezza [widthPx].
     * Usa un Mutex per serializzare le chiamate a PdfRenderer (non thread-safe).
     */
    fun renderPage(index: Int, widthPx: Int) {
        if (_pages.value.containsKey(index)) return
        viewModelScope.launch(Dispatchers.IO) {
            renderMutex.withLock {
                if (_pages.value.containsKey(index)) return@withLock
                val r = renderer ?: return@withLock
                if (index >= r.pageCount) return@withLock
                runCatching {
                    val page = r.openPage(index)
                    val pageHeight = (page.height.toFloat() / page.width.toFloat() * widthPx).toInt()
                        .coerceAtLeast(1)
                    val bmp = Bitmap.createBitmap(widthPx, pageHeight, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    _pages.value = _pages.value + (index to bmp)
                }
            }
        }
    }

    private fun closeRenderer() {
        try { renderer?.close() } catch (_: Exception) {}
        try { rendererFd?.close() } catch (_: Exception) {}
        renderer = null
        rendererFd = null
    }

    override fun onCleared() {
        super.onCleared()
        _pages.value.values.forEach { runCatching { it.recycle() } }
        closeRenderer()
    }
}
