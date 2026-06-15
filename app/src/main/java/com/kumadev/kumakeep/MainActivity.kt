package com.kumadev.kumakeep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kumadev.kumakeep.presentation.navigation.KumaKeepNavGraph
import com.kumadev.kumakeep.presentation.navigation.Screen
import com.kumadev.kumakeep.presentation.theme.AccentOrange
import com.kumadev.kumakeep.presentation.theme.KumaKeepTheme
import com.kumadev.kumakeep.presentation.theme.SurfaceDark
import com.kumadev.kumakeep.presentation.theme.TextSecondary
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KumaKeepTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // nasconde la bottom bar nel Game Detail
                val showBottomBar = currentRoute != Screen.GameDetail.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(containerColor = SurfaceDark) {
                                NavigationBarItem(
                                    selected = currentRoute == Screen.Library.route,
                                    onClick = { navController.navigate(Screen.Library.route) },
                                    icon = { Icon(Icons.Default.CollectionsBookmark, null) },
                                    label = { Text("Libreria") },
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
                                    onClick = { navController.navigate(Screen.Search.route) },
                                    icon = { Icon(Icons.Default.Search, null) },
                                    label = { Text("Cerca") },
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
                                    onClick = { navController.navigate(Screen.Wishlist.route) },
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
}