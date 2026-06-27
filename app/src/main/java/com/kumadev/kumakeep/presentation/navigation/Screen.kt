package com.kumadev.kumakeep.presentation.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Library : Screen("library")
    data object Search : Screen("search")
    data object GameDetail : Screen("game_detail/{bggId}") {
        fun createRoute(bggId: Long) = "game_detail/$bggId"
    }
    data object Wishlist : Screen("wishlist")
    data object WishlistDetail : Screen("wishlist_detail/{wishlistId}") {
        fun createRoute(wishlistId: Long) = "wishlist_detail/$wishlistId"
    }
    data object Settings : Screen("settings")
    data object PdfViewer : Screen("pdf_viewer/{gameId}") {
        fun createRoute(gameId: Long) = "pdf_viewer/$gameId"
    }
}
