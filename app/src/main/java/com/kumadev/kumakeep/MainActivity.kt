package com.kumadev.kumakeep

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kumadev.kumakeep.util.PendingPdfHolder
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kumadev.kumakeep.presentation.SnackbarController
import com.kumadev.kumakeep.presentation.navigation.KumaKeepNavGraph
import com.kumadev.kumakeep.presentation.navigation.Screen
import com.kumadev.kumakeep.presentation.theme.AccentOrange
import com.kumadev.kumakeep.presentation.theme.DevBannerRed
import com.kumadev.kumakeep.presentation.theme.KumaKeepTheme
import com.kumadev.kumakeep.presentation.theme.SurfaceDark
import com.kumadev.kumakeep.presentation.theme.TextSecondary
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var snackbarController: SnackbarController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            KumaKeepTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(Unit) {
                    snackbarController.events.collect { event ->
                        val result = snackbarHostState.showSnackbar(
                            message = event.message,
                            actionLabel = event.actionLabel,
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            event.onAction?.invoke()
                        }
                    }
                }

                // nasconde la bottom bar nel Game Detail, WishlistDetail e PDF Viewer
                val showBottomBar = currentRoute != Screen.GameDetail.route
                        && currentRoute != Screen.WishlistDetail.route
                        && currentRoute != Screen.PdfViewer.route
                        && currentRoute != null

                // Pattern standard Navigation Compose per bottom bar: riusa l'istanza
                // (ViewModel + stato) della tab già visitata invece di ricrearla ogni
                // volta da zero, ed evita che il back stack cresca a ogni tap.
                val navigateToTab: (String) -> Unit = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.Home.route) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        if (BuildConfig.IS_DEV_BUILD) {
                            DevModeBanner()
                        }
                    },
                    snackbarHost = {
                        SnackbarHost(snackbarHostState) { data ->
                            Snackbar(
                                snackbarData = data,
                                containerColor = MaterialTheme.colorScheme.inverseSurface,
                                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                                actionColor = AccentOrange
                            )
                        }
                    },
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(containerColor = SurfaceDark) {
                                NavigationBarItem(
                                    selected = currentRoute == Screen.Home.route,
                                    onClick = { navigateToTab(Screen.Home.route) },
                                    icon = { Icon(Icons.Default.Home, null) },
                                    label = { Text("Home") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AccentOrange,
                                        selectedTextColor = AccentOrange,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary,
                                        indicatorColor = SurfaceDark
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentRoute == Screen.Library.route,
                                    onClick = { navigateToTab(Screen.Library.route) },
                                    icon = { Icon(Icons.Default.CollectionsBookmark, null) },
                                    label = { Text("Library") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AccentOrange,
                                        selectedTextColor = AccentOrange,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary,
                                        indicatorColor = SurfaceDark
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentRoute == Screen.Search.route,
                                    onClick = { navigateToTab(Screen.Search.route) },
                                    icon = { Icon(Icons.Default.Search, null) },
                                    label = { Text("Search") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AccentOrange,
                                        selectedTextColor = AccentOrange,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary,
                                        indicatorColor = SurfaceDark
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentRoute == Screen.Wishlist.route,
                                    onClick = { navigateToTab(Screen.Wishlist.route) },
                                    icon = { Icon(Icons.Default.FavoriteBorder, null) },
                                    label = { Text("Wishlist") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AccentOrange,
                                        selectedTextColor = AccentOrange,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary,
                                        indicatorColor = SurfaceDark
                                    )
                                )
                            }
                        }
                    }
                ) { padding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        KumaKeepNavGraph(navController = navController)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND && intent.type == "application/pdf") {
            val uri: android.net.Uri? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            uri ?: return
            val fileName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                if (nameIndex >= 0) cursor.getString(nameIndex) else null
            } ?: "regolamento.pdf"
            PendingPdfHolder.uri = uri
            PendingPdfHolder.fileName = fileName
        }
    }
}

/**
 * Striscia sempre visibile, su tutte le schermate, solo nella build "dev".
 * Serve al tester per non confondere mai questa build con quella "reale" (debug).
 */
@Composable
private fun DevModeBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DevBannerRed
    ) {
        Text(
            text = "DEV MODE  •  v${BuildConfig.VERSION_NAME}",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )
    }
}