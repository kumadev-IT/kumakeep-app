package com.kumadev.kumakeep.presentation.home

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kumadev.kumakeep.R
import com.kumadev.kumakeep.domain.model.BoardGame
import com.kumadev.kumakeep.presentation.theme.SurfaceDark
import com.kumadev.kumakeep.presentation.theme.TextPrimary
import com.kumadev.kumakeep.presentation.theme.TextSecondary

@Composable
fun HomeScreen(
    onGameClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Background logo watermark ─────────────────────────────────────
            // Inverted-ghost technique (≡ CSS grayscale + invert + brightness).
            // Dark logo body → light steel-blue ghost on dark bg.
            // Internal details (shield/orange) have lower luminance after inversion
            // → appear fractionally darker → contours preserved.
            //
            // L   = 0.2126R + 0.7152G + 0.0722B
            // L'  = 1 − L  (inversion)
            // R_out = 0.70 × L',  G_out = 0.80 × L',  B_out = 1.20 × L'
            Image(
                painter = painterResource(id = R.drawable.kuma_keep_dark_transparent),
                contentDescription = null,
                modifier = Modifier
                    .requiredWidth(700.dp)
                    .align(Alignment.Center)
                    .alpha(0.15f),
                colorFilter = ColorFilter.colorMatrix(
                    ColorMatrix(floatArrayOf(
                        -0.149f, -0.501f, -0.051f, 0f, 0.70f,  // R = 0.70 × (1−L)
                        -0.170f, -0.572f, -0.058f, 0f, 0.80f,  // G = 0.80 × (1−L)
                        -0.255f, -0.858f, -0.087f, 0f, 1.20f,  // B = 1.20 × (1−L)
                         0f,      0f,      0f,     1f, 0f       // A unchanged
                    ))
                )
            )

            // ── Content ───────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 24.dp, bottom = 16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color(0xFF5B8BAD), fontWeight = FontWeight.SemiBold)) {
                                append(uiState.username.ifBlank { "Kuma" })
                            }
                            append(" Library")
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Stats row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(label = "Games", value = uiState.libraryCount, modifier = Modifier.weight(1f))
                    StatCard(label = "Wishlist", value = uiState.wishlistCount, modifier = Modifier.weight(1f))
                    StatCard(label = "Played", value = uiState.playedCount, modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(24.dp))

                // ── Recently Added ────────────────────────────────────────────
                SectionHeader("Recently Added")
                Spacer(Modifier.height(8.dp))
                if (uiState.recentlyAdded.isEmpty()) {
                    EmptyCarouselHint("Add games to your library to see them here")
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.recentlyAdded, key = { it.bggId }) { game ->
                            GameCard(game = game, onClick = { onGameClick(game.bggId) })
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Recently Viewed ───────────────────────────────────────────
                SectionHeader("Recently Viewed")
                Spacer(Modifier.height(8.dp))
                if (uiState.recentlyViewed.isEmpty()) {
                    EmptyCarouselHint("Open a game to see it here")
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.recentlyViewed, key = { it.bggId }) { game ->
                            GameCard(game = game, onClick = { onGameClick(game.bggId) })
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun EmptyCarouselHint(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun GameCard(game: BoardGame, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(96.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            AsyncImage(
                model = game.thumbnail,
                contentDescription = game.primaryName,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.mipmap.ic_launcher_foreground),
                error = painterResource(id = R.mipmap.ic_launcher_foreground)
            )
            Column(modifier = Modifier.padding(6.dp)) {
                Text(
                    text = game.primaryName,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
                if (game.yearPublished != null) {
                    Text(
                        text = game.yearPublished.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
