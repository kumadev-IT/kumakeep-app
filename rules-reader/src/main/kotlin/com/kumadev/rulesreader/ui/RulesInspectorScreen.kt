package com.kumadev.rulesreader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kumadev.rulesreader.db.entity.ExtractedPageEntity
import com.kumadev.rulesreader.db.entity.RulebookChunkEntity

/**
 * Schermata di ispezione del testo estratto e dei chunk generati.
 * Accessibile solo dopo che la pipeline è completata (stato Done).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesInspectorScreen(
    onBack: () -> Unit,
    viewModel: RulesInspectorViewModel = hiltViewModel()
) {
    val pages by viewModel.pages.collectAsStateWithLifecycle()
    val chunks by viewModel.chunks.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pagine (${pages.size})", "Chunk (${chunks.size})")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ispettore regolamento",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Indietro",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> PagesTab(pages = pages)
                1 -> ChunksTab(chunks = chunks)
            }
        }
    }
}

// ─── Tab Pagine ───────────────────────────────────────────────────────────────

@Composable
private fun PagesTab(pages: List<ExtractedPageEntity>) {
    if (pages.isEmpty()) {
        EmptyState("Nessuna pagina estratta")
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(pages, key = { it.id }) { page ->
            PageCard(page)
        }
    }
}

@Composable
private fun PageCard(page: ExtractedPageEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Pagina ${page.pageNum}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${page.rawText.length} caratteri",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (page.rawText.isBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "⚠ Testo non estratto (pagina immagine senza OCR)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Text(
                    text = page.rawText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 12,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ─── Tab Chunk ────────────────────────────────────────────────────────────────

@Composable
private fun ChunksTab(chunks: List<RulebookChunkEntity>) {
    if (chunks.isEmpty()) {
        EmptyState("Nessun chunk generato")
        return
    }

    val chunksWithEmbedding = chunks.count { (it.embeddingBlob?.size ?: 0) > 0 }
    val embeddingDimension = chunks.firstOrNull()
        ?.embeddingBlob
        ?.let { it.size / 4 }
        ?: 0

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Header statistiche
        item {
            ChunksSummaryHeader(
                total = chunks.size,
                withEmbedding = chunksWithEmbedding,
                embeddingDimension = embeddingDimension
            )
            Spacer(Modifier.height(4.dp))
        }

        items(chunks, key = { it.id }) { chunk ->
            ChunkCard(chunk)
        }
    }
}

@Composable
private fun ChunksSummaryHeader(total: Int, withEmbedding: Int, embeddingDimension: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "$total chunk totali",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = if (embeddingDimension > 0)
                        "$withEmbedding con embedding (dim $embeddingDimension)"
                    else
                        "Modello embedding non disponibile",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            if (withEmbedding < total) {
                CircularProgressIndicator(
                    progress = { withEmbedding.toFloat() / total },
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
                )
            } else if (withEmbedding == total && total > 0) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ChunkCard(chunk: RulebookChunkEntity) {
    val hasEmbedding = (chunk.embeddingBlob?.size ?: 0) > 0
    val embeddingDim = if (hasEmbedding) (chunk.embeddingBlob!!.size / 4) else 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "#${chunk.chunkIndex}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "p.${chunk.pageNum}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                // Badge embedding
                EmbeddingBadge(hasEmbedding = hasEmbedding, dimension = embeddingDim)
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Text(
                text = chunk.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))
            Text(
                text = "v${chunk.rulesReaderVersion} · ${chunk.text.split(" ").size} parole",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun EmbeddingBadge(hasEmbedding: Boolean, dimension: Int) {
    val (bgColor, icon, label) = if (hasEmbedding) {
        Triple(
            Color(0xFF1B5E20),
            Icons.Default.Check,
            "emb $dimension"
        )
    } else {
        Triple(
            MaterialTheme.colorScheme.errorContainer,
            Icons.Default.HourglassEmpty,
            "no emb"
        )
    }

    Row(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(10.dp)
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}

// ─── Helper ──────────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
