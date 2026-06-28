package com.kumadev.kumakeep.presentation.gamedetail

import android.os.Build
import android.text.Html
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kumadev.kumakeep.domain.model.BoardGame
import com.kumadev.kumakeep.domain.model.LibraryEntry
import com.kumadev.kumakeep.domain.model.Rulebook
import com.kumadev.kumakeep.domain.model.WishlistWithStatus
import com.kumadev.kumakeep.presentation.theme.AccentGreen
import com.kumadev.kumakeep.presentation.theme.AccentOrange
import com.kumadev.kumakeep.presentation.theme.SurfaceDark
import com.kumadev.kumakeep.presentation.theme.SurfaceVariant
import com.kumadev.rulesreader.model.ProcessingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(
    bggId: Long,
    onBack: () -> Unit,
    onOpenRulebook: (gameId: Long) -> Unit,
    onInspectRulebook: (gameId: Long) -> Unit,
    viewModel: GameDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showWishlistDialog by viewModel.showWishlistDialog.collectAsStateWithLifecycle()
    val wishlistsForGame by viewModel.wishlistsForGame.collectAsStateWithLifecycle()
    val showRatingSheet by viewModel.showRatingSheet.collectAsStateWithLifecycle()
    val showNumPlaysSheet by viewModel.showNumPlaysSheet.collectAsStateWithLifecycle()
    val rulebook by viewModel.rulebook.collectAsStateWithLifecycle()
    val showDeleteRulebookDialog by viewModel.showDeleteRulebookDialog.collectAsStateWithLifecycle()
    val pendingImport by viewModel.pendingImport.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()
    val processingState by viewModel.rulebookProcessingState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Launcher SAF per scegliere un PDF
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            if (nameIndex >= 0) cursor.getString(nameIndex) else null
        } ?: "regolamento.pdf"
        viewModel.importRulebook(uri, fileName)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    if (uiState is GameDetailUiState.Success) {
                        Text(
                            text = (uiState as GameDetailUiState.Success).game.primaryName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro",
                            tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        bottomBar = {
            val state = uiState
            if (state is GameDetailUiState.Success) {
                GameDetailBottomBar(
                    game = state.game,
                    rulebook = rulebook,
                    onLibraryClick = viewModel::toggleLibrary,
                    onWishlistClick = viewModel::openWishlistDialog,
                    onRatingClick = viewModel::openRatingSheet,
                    onNumPlaysClick = viewModel::openNumPlaysSheet,
                    onRulebookClick = {
                        if (rulebook != null) onOpenRulebook(bggId)
                        else pdfPickerLauncher.launch("application/pdf")
                    }
                )
            }
        }
    ) { padding ->
        when (val state = uiState) {
            is GameDetailUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentOrange)
                }
            }
            is GameDetailUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Impossibile caricare il gioco",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.retry() }) {
                            Text("Riprova", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            is GameDetailUiState.Success -> {
                GameDetailContent(
                    game = state.game,
                    rulebook = rulebook,
                    isImporting = isImporting,
                    processingState = processingState,
                    onImportClick = { pdfPickerLauncher.launch("application/pdf") },
                    onOpenRulebookClick = { onOpenRulebook(bggId) },
                    onInspectRulebookClick = { onInspectRulebook(bggId) },
                    onStartProcessingClick = viewModel::startRulebookProcessing,
                    onResetProcessingClick = viewModel::resetRulebookProcessing,
                    onDeleteRulebookClick = viewModel::openDeleteRulebookDialog,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }

    if (showWishlistDialog) {
        WishlistSelectionSheet(
            wishlists = wishlistsForGame,
            onDismiss = viewModel::dismissWishlistDialog,
            onConfirm = viewModel::saveWishlistSelections
        )
    }

    val currentState = uiState
    if (showRatingSheet && currentState is GameDetailUiState.Success) {
        RatingSheet(
            currentRate = currentState.game.libraryEntry?.rate ?: "NOT_RATED",
            onSelect = viewModel::updateRate,
            onDismiss = viewModel::dismissRatingSheet
        )
    }

    if (showNumPlaysSheet && currentState is GameDetailUiState.Success) {
        NumPlaysSheet(
            currentNumPlays = currentState.game.libraryEntry?.numPlays ?: "NOT_CLASSIFIED",
            onSelect = viewModel::updateNumPlays,
            onDismiss = viewModel::dismissNumPlaysSheet
        )
    }

    if (showDeleteRulebookDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteRulebookDialog,
            title = { Text("Rimuovi regolamento") },
            text = { Text("Il file verrà eliminato dall'app. Puoi reimportarlo in qualsiasi momento.") },
            confirmButton = {
                TextButton(onClick = viewModel::deleteRulebook) {
                    Text("Rimuovi", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteRulebookDialog) { Text("Annulla") }
            }
        )
    }

    // Dialog per import via ACTION_SEND
    val pending = pendingImport
    if (pending != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissPendingImport,
            title = { Text("Importa regolamento") },
            text = {
                Text(
                    "Vuoi importare \"${pending.second}\" come regolamento di questo gioco?" +
                            if (rulebook != null) "\n\nSostituirà il regolamento già importato." else ""
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmPendingImport) {
                    Text("Importa", color = AccentOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissPendingImport) { Text("Annulla") }
            }
        )
    }
}

// ─── Bottom action bar ────────────────────────────────────────────────────────

@Composable
private fun GameDetailBottomBar(
    game: BoardGame,
    rulebook: Rulebook?,
    onLibraryClick: () -> Unit,
    onWishlistClick: () -> Unit,
    onRatingClick: () -> Unit,
    onNumPlaysClick: () -> Unit,
    onRulebookClick: () -> Unit
) {
    val inLibrary = game.libraryEntry != null
    val rateLabel = game.libraryEntry?.rate?.toRateLabel()
    val numPlaysLabel = game.libraryEntry?.numPlays?.toNumPlaysLabel()

    BottomAppBar(
        containerColor = SurfaceDark,
        tonalElevation = 0.dp,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        windowInsets = WindowInsets(0),
        modifier = Modifier.height(60.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomBarItem(
                icon = if (inLibrary) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                label = "Libreria",
                tint = if (inLibrary) AccentOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onLibraryClick
            )
            BottomBarItem(
                icon = Icons.Outlined.Favorite,
                label = "Wishlist",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onWishlistClick
            )
            BottomBarItem(
                icon = Icons.Default.Star,
                label = rateLabel ?: "Voto",
                tint = if (rateLabel != null) AccentOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                enabled = inLibrary,
                onClick = onRatingClick
            )
            BottomBarItem(
                icon = Icons.Default.Replay,
                label = numPlaysLabel ?: "Partite",
                tint = if (numPlaysLabel != null) AccentGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                enabled = inLibrary,
                onClick = onNumPlaysClick
            )
            BottomBarItem(
                icon = Icons.Default.MenuBook,
                label = "Regole",
                tint = if (rulebook != null) AccentOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onRulebookClick
            )
        }
    }
}

@Composable
private fun BottomBarItem(
    icon: ImageVector,
    label: String,
    tint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val effectiveTint = if (enabled) tint else tint.copy(alpha = 0.35f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(32.dp)) {
            Icon(icon, contentDescription = label, tint = effectiveTint, modifier = Modifier.size(20.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = effectiveTint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─── Main content ─────────────────────────────────────────────────────────────

@Composable
private fun GameDetailContent(
    game: BoardGame,
    rulebook: Rulebook?,
    isImporting: Boolean,
    processingState: ProcessingState,
    onImportClick: () -> Unit,
    onOpenRulebookClick: () -> Unit,
    onInspectRulebookClick: () -> Unit,
    onStartProcessingClick: () -> Unit,
    onResetProcessingClick: () -> Unit,
    onDeleteRulebookClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Hero image con titolo in overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            AsyncImage(
                model = game.image,
                contentDescription = game.primaryName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .align(Alignment.BottomStart)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xCC000000))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = game.primaryName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                game.yearPublished?.let {
                    Text(
                        text = it.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {

            Spacer(Modifier.height(16.dp))

            // Stats row (dati BGG)
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                if (game.minPlayers != null && game.maxPlayers != null) {
                    StatItem(
                        icon = { Icon(Icons.Default.Groups, null, tint = AccentOrange, modifier = Modifier.size(16.dp)) },
                        label = "${game.minPlayers}–${game.maxPlayers}"
                    )
                }
                game.playingTime?.let {
                    StatItem(
                        icon = { Icon(Icons.Default.Schedule, null, tint = AccentOrange, modifier = Modifier.size(16.dp)) },
                        label = "$it min"
                    )
                }
                game.bggRating?.let {
                    StatItem(
                        icon = { Icon(Icons.Default.Star, null, tint = AccentOrange, modifier = Modifier.size(16.dp)) },
                        label = String.format("%.1f", it)
                    )
                }
                game.complexity?.let {
                    StatItem(
                        icon = { Text("⚙", style = MaterialTheme.typography.labelSmall) },
                        label = String.format("%.1f", it)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = SurfaceVariant)
            Spacer(Modifier.height(16.dp))

            // Designer + Publisher + Categorie + Meccaniche
            if (game.designers.isNotEmpty()) {
                InfoRow("Designer", game.designers.take(3).joinToString(", "))
            }
            if (game.publishers.isNotEmpty()) {
                InfoRow("Editore", game.publishers.take(2).joinToString(", "))
            }
            if (game.categories.isNotEmpty()) {
                InfoRow("Categorie", game.categories.take(4).joinToString(", "))
            }
            if (game.mechanics.isNotEmpty()) {
                InfoRow("Meccaniche", game.mechanics.take(4).joinToString(", "))
            }

            // Badge utente (rating + numPlays): visibili solo se impostati
            game.libraryEntry?.let { entry ->
                UserBadgesSection(entry)
            }

            // Sezione Regolamento
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = SurfaceVariant)
            Spacer(Modifier.height(12.dp))

            RulebookSection(
                rulebook = rulebook,
                isImporting = isImporting,
                processingState = processingState,
                onImportClick = onImportClick,
                onOpenClick = onOpenRulebookClick,
                onInspectClick = onInspectRulebookClick,
                onStartProcessingClick = onStartProcessingClick,
                onResetProcessingClick = onResetProcessingClick,
                onDeleteClick = onDeleteRulebookClick
            )

            // Descrizione
            if (!game.description.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = SurfaceVariant)
                Spacer(Modifier.height(12.dp))

                var descriptionExpanded by remember { mutableStateOf(false) }
                val cleanDescription = remember(game.description) { game.description.parseHtml() }

                Text(
                    text = cleanDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Justify,
                    maxLines = if (descriptionExpanded) Int.MAX_VALUE else 5,
                    overflow = if (descriptionExpanded) TextOverflow.Visible else TextOverflow.Ellipsis
                )
                TextButton(
                    onClick = { descriptionExpanded = !descriptionExpanded },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (descriptionExpanded) "Mostra meno" else "Mostra tutto",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── Sezione Regolamento ──────────────────────────────────────────────────────

@Composable
private fun RulebookSection(
    rulebook: Rulebook?,
    isImporting: Boolean,
    processingState: ProcessingState,
    onImportClick: () -> Unit,
    onOpenClick: () -> Unit,
    onInspectClick: () -> Unit,
    onStartProcessingClick: () -> Unit,
    onResetProcessingClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.MenuBook,
                contentDescription = null,
                tint = AccentOrange,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Regolamento",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(8.dp))

        when {
            isImporting -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = AccentOrange
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Importazione in corso…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            rulebook != null -> {
                Text(
                    text = rulebook.fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${rulebook.pageCount} pagine · ${rulebook.sizeBytes.toReadableSize()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onOpenClick) {
                        Text("Apri", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(onClick = onImportClick) {
                        Text("Sostituisci", style = MaterialTheme.typography.labelMedium)
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Rimuovi regolamento",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // ── Sezione analisi RAG ────────────────────────────────────────
                Spacer(Modifier.height(12.dp))
                RulebookProcessingSection(
                    state = processingState,
                    onStartClick = onStartProcessingClick,
                    onInspectClick = onInspectClick,
                    onResetClick = onResetProcessingClick
                )
            }

            else -> {
                Text(
                    text = "Nessun regolamento importato.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(onClick = onImportClick) {
                    Text("Importa PDF", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun RulebookProcessingSection(
    state: ProcessingState,
    onStartClick: () -> Unit,
    onInspectClick: () -> Unit,
    onResetClick: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Analisi RAG",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            when (state) {
                ProcessingState.Done -> {
                    TextButton(
                        onClick = onResetClick,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text(
                            "Rianalizza",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is ProcessingState.Error -> {
                    TextButton(
                        onClick = onStartClick,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text(
                            "Riprova",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                else -> {}
            }
        }

        Spacer(Modifier.height(6.dp))

        when (state) {
            ProcessingState.Idle -> {
                FilledTonalButton(onClick = onStartClick) {
                    Text("Analizza regolamento", style = MaterialTheme.typography.labelMedium)
                }
            }

            ProcessingState.Extracting -> {
                ProcessingProgressRow(label = "Estrazione testo…")
            }

            is ProcessingState.Chunking -> {
                ProcessingProgressRow(
                    label = "Segmentazione chunk…",
                    progress = if (state.total > 0) state.current.toFloat() / state.total else null
                )
            }

            is ProcessingState.Embedding -> {
                ProcessingProgressRow(
                    label = "Calcolo embedding ${state.current + 1}/${state.total}…",
                    progress = if (state.total > 0) (state.current + 1).toFloat() / state.total else null
                )
            }

            ProcessingState.Done -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "✓ Analisi completata",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentGreen
                    )
                }
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(onClick = onInspectClick) {
                    Text("Ispeziona", style = MaterialTheme.typography.labelMedium)
                }
            }

            is ProcessingState.Error -> {
                Text(
                    text = "Errore: ${state.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ProcessingProgressRow(label: String, progress: Float? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (progress != null) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = AccentOrange
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = AccentOrange
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Badge sezione utente ─────────────────────────────────────────────────────

@Composable
private fun UserBadgesSection(entry: LibraryEntry) {
    val rateLabel = entry.rate.toRateLabel()
    val numPlaysLabel = entry.numPlays.toNumPlaysLabel()

    if (rateLabel == null && numPlaysLabel == null) return

    Spacer(Modifier.height(16.dp))
    HorizontalDivider(color = SurfaceVariant)
    Spacer(Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        rateLabel?.let { label ->
            AssistChip(
                onClick = {},
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = AccentOrange,
                        modifier = Modifier.size(14.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(containerColor = SurfaceVariant),
                border = null
            )
        }
        numPlaysLabel?.let { label ->
            AssistChip(
                onClick = {},
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Replay,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(14.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(containerColor = SurfaceVariant),
                border = null
            )
        }
    }
}

// ─── Sheet valutazione ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RatingSheet(
    currentRate: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf("BAH" to "Meh", "MID" to "Ok", "YES" to "Sì!", "TOP" to "Top", "WOW" to "Wow")
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("La tua valutazione", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (value, label) ->
                    val selected = currentRate == value
                    FilterChip(
                        selected = selected,
                        onClick = { onSelect(if (selected) "NOT_RATED" else value) },
                        label = { Text(label) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─── Sheet partite giocate ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NumPlaysSheet(
    currentNumPlays: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf("ZERO" to "0", "ONE" to "1", "MANY" to "Alcune", "PLENTY" to "Tante")
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("Partite giocate", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (value, label) ->
                    val selected = currentNumPlays == value
                    FilterChip(
                        selected = selected,
                        onClick = { onSelect(if (selected) "NOT_CLASSIFIED" else value) },
                        label = { Text(label) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─── Componenti riutilizzabili ────────────────────────────────────────────────

@Composable
private fun StatItem(icon: @Composable () -> Unit, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = AccentGreen
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ─── Wishlist bottom sheet ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WishlistSelectionSheet(
    wishlists: List<WishlistWithStatus>,
    onDismiss: () -> Unit,
    onConfirm: (Set<Long>) -> Unit
) {
    val selected = remember(wishlists) {
        mutableStateOf(wishlists.filter { it.isSelected }.map { it.id }.toMutableSet())
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                "Aggiungi a wishlist",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (wishlists.isEmpty()) {
                Text(
                    "Nessuna wishlist disponibile",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn {
                    items(wishlists, key = { it.id }) { item ->
                        val isChecked = item.id in selected.value
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                val newSet = selected.value.toMutableSet()
                                if (isChecked) newSet.remove(item.id) else newSet.add(item.id)
                                selected.value = newSet
                            }) {
                                Icon(
                                    imageVector = if (isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = null,
                                    tint = if (isChecked) AccentOrange else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text("Annulla") }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = { onConfirm(selected.value.toSet()) }) {
                    Text("Salva")
                }
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun String.toRateLabel(): String? = when (this) {
    "BAH" -> "Meh"
    "MID" -> "Ok"
    "YES" -> "Sì!"
    "TOP" -> "Top"
    "WOW" -> "Wow"
    else -> null
}

private fun String.toNumPlaysLabel(): String? = when (this) {
    "ZERO" -> "0 partite"
    "ONE" -> "1 partita"
    "MANY" -> "Alcune"
    "PLENTY" -> "Tante"
    else -> null
}

private fun Long.toReadableSize(): String {
    return when {
        this < 1_024 -> "$this B"
        this < 1_048_576 -> "${this / 1_024} KB"
        else -> String.format("%.1f MB", this / 1_048_576.0)
    }
}

@Suppress("DEPRECATION")
private fun String.parseHtml(): String {
    val normalized = this
        .replace("&#10;", "<br>")
        .replace("\n", "<br>")

    val parsed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(normalized, Html.FROM_HTML_MODE_LEGACY)
    } else {
        Html.fromHtml(normalized)
    }.toString()

    return parsed
        .lines()
        .joinToString("\n") { it.trimEnd() }
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}
