package com.kumadev.kumakeep.presentation.wishlistdetail

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kumadev.kumakeep.domain.model.SearchResult
import com.kumadev.kumakeep.domain.model.WishlistEntry
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistDetailScreen(
    onBack: () -> Unit,
    onGameClick: (Long) -> Unit,
    viewModel: WishlistDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()

    // Naviga indietro quando la wishlist viene cancellata
    LaunchedEffect(uiState) {
        if (uiState is WishlistDetailUiState.Deleted) onBack()
    }

    var showSearchSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (val s = uiState) {
                            is WishlistDetailUiState.Success -> s.wishlistName
                            else -> ""
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rinomina") },
                                onClick = {
                                    menuExpanded = false
                                    showRenameDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Elimina wishlist") },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showSearchSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi gioco")
            }
        }
    ) { padding ->
        when (val state = uiState) {
            is WishlistDetailUiState.Loading, WishlistDetailUiState.Deleted -> Unit
            is WishlistDetailUiState.Success -> {
                WishlistEntryList(
                    entries = state.entries,
                    onReorder = viewModel::onReorder,
                    onRemove = { viewModel.removeEntry(it.bggId, it.primaryName) },
                    onToggleToBuy = viewModel::toggleToBuy,
                    onGameClick = onGameClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
        }
    }

    // Bottom sheet di ricerca per aggiungere giochi
    if (showSearchSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                viewModel.clearSearch()
                showSearchSheet = false
            },
            sheetState = sheetState
        ) {
            SearchAddGameSheet(
                searchState = searchState,
                onQueryChange = viewModel::searchBgg,
                onGameAdd = { bggId, name ->
                    viewModel.addGameToWishlist(bggId, name)
                    showSearchSheet = false
                }
            )
        }
    }

    // Dialog rinomina
    if (showRenameDialog) {
        val currentName = (uiState as? WishlistDetailUiState.Success)?.wishlistName ?: ""
        RenameDialog(
            currentName = currentName,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                viewModel.rename(newName)
                showRenameDialog = false
            }
        )
    }

    // Dialog conferma cancellazione
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Elimina wishlist") },
            text = { Text("Questa azione è irreversibile. Tutti i giochi nella lista verranno rimossi.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteWishlist()
                    showDeleteDialog = false
                }) { Text("Elimina", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Annulla") }
            }
        )
    }
}

@Composable
private fun WishlistEntryList(
    entries: List<WishlistEntry>,
    onReorder: (List<WishlistEntry>) -> Unit,
    onRemove: (WishlistEntry) -> Unit,
    onToggleToBuy: (WishlistEntry) -> Unit,
    onGameClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Lista locale mutabile per il reorder ottimistico
    val localEntries = remember(entries) { mutableStateListOf(*entries.toTypedArray()) }
    LaunchedEffect(entries) {
        if (entries != localEntries.toList()) {
            localEntries.clear()
            localEntries.addAll(entries)
        }
    }

    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        localEntries.apply { add(to.index, removeAt(from.index)) }
        onReorder(localEntries.toList())
    }

    if (localEntries.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                "Nessun gioco in questa wishlist",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(localEntries, key = { it.entryId }) { entry ->
            ReorderableItem(reorderState, key = entry.entryId) { isDragging ->
                WishlistEntryRow(
                    entry = entry,
                    isDragging = isDragging,
                    onRemove = { onRemove(entry) },
                    onToggleToBuy = { onToggleToBuy(entry) },
                    onGameClick = { onGameClick(entry.bggId) },
                    dragHandle = {
                        Icon(
                            Icons.Default.DragHandle,
                            contentDescription = "Riordina",
                            modifier = Modifier.draggableHandle(),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun WishlistEntryRow(
    entry: WishlistEntry,
    isDragging: Boolean,
    onRemove: () -> Unit,
    onToggleToBuy: () -> Unit,
    onGameClick: () -> Unit,
    dragHandle: @Composable () -> Unit
) {
    Card(
        onClick = onGameClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            dragHandle()

            AsyncImage(
                model = entry.thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.primaryName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                entry.yearPublished?.let {
                    Text(
                        text = it.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Toggle toBuy
            IconButton(onClick = onToggleToBuy) {
                Icon(
                    imageVector = if (entry.toBuy) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart,
                    contentDescription = if (entry.toBuy) "Da comprare" else "Non da comprare",
                    tint = if (entry.toBuy) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Rimuovi
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Rimuovi",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun SearchAddGameSheet(
    searchState: SearchUiState,
    onQueryChange: (String) -> Unit,
    onGameAdd: (bggId: Long, name: String) -> Unit
) {
    var query by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            "Aggiungi un gioco",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                onQueryChange(it)
            },
            label = { Text("Cerca su BGG") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        when (searchState) {
            SearchUiState.Idle -> Unit
            SearchUiState.Loading -> {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
            SearchUiState.Empty -> {
                Text(
                    "Nessun risultato",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is SearchUiState.Results -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(searchState.items, key = { it.bggId }) { result ->
                        SearchResultRow(result = result, onAdd = { onGameAdd(result.bggId, result.name) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(result: SearchResult, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(result.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            result.yearPublished?.let {
                Text(it.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = onAdd) { Text("Aggiungi") }
    }
}

@Composable
private fun RenameDialog(currentName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rinomina wishlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank() && name.trim() != currentName
            ) { Text("Salva") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}
