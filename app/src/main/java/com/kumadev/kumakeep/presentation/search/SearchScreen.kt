package com.kumadev.kumakeep.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kumadev.kumakeep.domain.model.BaseGameRef
import com.kumadev.kumakeep.domain.model.SearchResult
import com.kumadev.kumakeep.presentation.theme.AccentGreen
import com.kumadev.kumakeep.presentation.theme.AccentOrange
import com.kumadev.kumakeep.presentation.theme.SurfaceVariant

@Composable
fun SearchScreen(
    onGameClick: (Long) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val expansionChooser by viewModel.expansionChooser.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            TextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                placeholder = {
                    Text(
                        "Cerca su BoardGameGeek...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cancella",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SurfaceVariant,
                    unfocusedContainerColor = SurfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp)
            )

            when (val state = uiState) {
                is SearchUiState.Idle -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Cerca un gioco da tavolo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is SearchUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                is SearchUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                is SearchUiState.Empty -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Nessun risultato",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is SearchUiState.Success -> {
                    LazyColumn {
                        items(state.results, key = { it.bggId }) { result ->
                            SearchResultItem(
                                result = result,
                                onGameClick = { onGameClick(result.bggId) },
                                onAdd = { viewModel.onAddResult(result) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }
            }
        }
    }

    // Scelta del gioco base quando l'espansione ne estende più d'uno posseduto
    expansionChooser?.let { chooser ->
        ExpansionBaseChooserSheet(
            chooser = chooser,
            onSelect = viewModel::confirmExpansionBase,
            onDismiss = viewModel::dismissExpansionChooser
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpansionBaseChooserSheet(
    chooser: ExpansionChooser,
    onSelect: (BaseGameRef) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "\"${chooser.expansionName}\" — aggiungi come espansione di",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(12.dp))
            chooser.candidates.forEach { base ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(base) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Extension,
                        contentDescription = null,
                        tint = AccentOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = base.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    result: SearchResult,
    onGameClick: () -> Unit,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onGameClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = result.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (result.isExpansion) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "EXP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
                if (result.isOwned) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "OWNED",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier
                            .background(AccentGreen, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
            result.yearPublished?.let {
                Text(
                    text = it.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        // Stesso "+" per giochi ed espansioni: aggiunge. Cosa aggiunge (libreria
        // o collegamento come espansione) è deciso nel ViewModel. Nascosto se già
        // posseduto: non si può aggiungere due volte lo stesso gioco.
        if (!result.isOwned) {
            IconButton(onClick = onAdd) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = if (result.isExpansion) "Aggiungi come espansione"
                    else "Aggiungi alla libreria",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
