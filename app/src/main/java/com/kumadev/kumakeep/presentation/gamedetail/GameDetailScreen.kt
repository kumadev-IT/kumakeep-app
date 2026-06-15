package com.kumadev.kumakeep.presentation.gamedetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kumadev.kumakeep.domain.model.BoardGame
import com.kumadev.kumakeep.domain.model.WishlistWithStatus
import com.kumadev.kumakeep.presentation.theme.AccentGreen
import com.kumadev.kumakeep.presentation.theme.AccentOrange
import com.kumadev.kumakeep.presentation.theme.SurfaceDark
import com.kumadev.kumakeep.presentation.theme.SurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(
    bggId: Long,
    onBack: () -> Unit,
    viewModel: GameDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showWishlistDialog by viewModel.showWishlistDialog.collectAsStateWithLifecycle()
    val wishlistsForGame by viewModel.wishlistsForGame.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
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
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is GameDetailUiState.Success -> {
                GameDetailContent(
                    game = state.game,
                    onToggleLibrary = viewModel::toggleLibrary,
                    onOpenWishlistDialog = viewModel::openWishlistDialog,
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
}

@Composable
private fun GameDetailContent(
    game: BoardGame,
    onToggleLibrary: () -> Unit,
    onOpenWishlistDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Cover image
        AsyncImage(
            model = game.image,
            contentDescription = game.primaryName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )

        Column(modifier = Modifier.padding(16.dp)) {

            // Titolo + anno
            Text(
                text = game.primaryName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            game.yearPublished?.let {
                Text(
                    text = it.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))

            // Stats row
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Giocatori
                if (game.minPlayers != null && game.maxPlayers != null) {
                    StatItem(
                        icon = { Icon(Icons.Default.Groups, null, tint = AccentOrange, modifier = Modifier.size(16.dp)) },
                        label = "${game.minPlayers}–${game.maxPlayers}"
                    )
                }
                // Durata
                game.playingTime?.let {
                    StatItem(
                        icon = { Icon(Icons.Default.Schedule, null, tint = AccentOrange, modifier = Modifier.size(16.dp)) },
                        label = "${it} min"
                    )
                }
                // Voto BGG
                game.bggRating?.let {
                    StatItem(
                        icon = { Icon(Icons.Default.Star, null, tint = AccentOrange, modifier = Modifier.size(16.dp)) },
                        label = String.format("%.1f", it)
                    )
                }
                // Complessità
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

            // Designer + Publisher
            if (game.designers.isNotEmpty()) {
                InfoRow("Designer", game.designers.take(3).joinToString(", "))
            }
            if (game.publishers.isNotEmpty()) {
                InfoRow("Editore", game.publishers.take(2).joinToString(", "))
            }

            // Categorie
            if (game.categories.isNotEmpty()) {
                InfoRow("Categorie", game.categories.take(4).joinToString(", "))
            }

            // Meccaniche
            if (game.mechanics.isNotEmpty()) {
                InfoRow("Meccaniche", game.mechanics.take(4).joinToString(", "))
            }

            Spacer(Modifier.height(24.dp))

            // Bottone aggiungi/rimuovi libreria
            val inLibrary = game.libraryEntry != null
            FilledTonalButton(
                onClick = onToggleLibrary,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (inLibrary) SurfaceVariant else AccentOrange,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = if (inLibrary) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(if (inLibrary) "In libreria" else "Aggiungi alla libreria")
            }

            Spacer(Modifier.height(8.dp))

            // Bottone wishlist
            OutlinedButton(
                onClick = onOpenWishlistDialog,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Wishlist")
            }
        }
    }
}

@Composable
private fun StatItem(icon: @Composable () -> Unit, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface)
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