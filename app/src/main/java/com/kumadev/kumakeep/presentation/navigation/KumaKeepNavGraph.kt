package com.kumadev.kumakeep.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kumadev.kumakeep.presentation.gamedetail.GameDetailScreen
import com.kumadev.kumakeep.presentation.library.LibraryScreen
import com.kumadev.kumakeep.presentation.search.SearchScreen
import com.kumadev.kumakeep.presentation.wishlist.WishlistScreen
import com.kumadev.kumakeep.presentation.wishlistdetail.WishlistDetailScreen

@Composable
fun KumaKeepNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Search.route
    ) {
        composable(Screen.Search.route) {
            SearchScreen(
                onGameClick = { bggId ->
                    navController.navigate(Screen.GameDetail.createRoute(bggId))
                }
            )
        }

        composable(
            route = Screen.GameDetail.route,
            arguments = listOf(navArgument("bggId") { type = NavType.LongType })
        ) { backStackEntry ->
            val bggId = backStackEntry.arguments?.getLong("bggId") ?: return@composable
            GameDetailScreen(
                bggId = bggId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Library.route) {
            LibraryScreen(
                onGameClick = { bggId ->
                    navController.navigate(Screen.GameDetail.createRoute(bggId))
                }
            )
        }

        composable(Screen.Wishlist.route) {
            WishlistScreen(
                onWishlistClick = { wishlistId ->
                    navController.navigate(Screen.WishlistDetail.createRoute(wishlistId))
                }
            )
        }

        composable(
            route = Screen.WishlistDetail.route,
            arguments = listOf(navArgument("wishlistId") { type = NavType.LongType })
        ) { backStackEntry ->
            val wishlistId = backStackEntry.arguments?.getLong("wishlistId") ?: return@composable
            WishlistDetailScreen(
                onBack = { navController.popBackStack() },
                onGameClick = { bggId ->
                    navController.navigate(Screen.GameDetail.createRoute(bggId))
                }
            )
        }
    }
}
