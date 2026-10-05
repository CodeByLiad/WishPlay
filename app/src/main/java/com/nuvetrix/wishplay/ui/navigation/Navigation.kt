package com.nuvetrix.wishplay.ui.navigation

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Main : Screen("main")
    object CustomGame : Screen("custom_game")
    object Calendar : Screen("calendar")
    object Admin : Screen("admin")
    data class Details(val gameId: String) : Screen("details/{gameId}") {
        companion object {
            fun createRoute(gameId: String) = "details/$gameId"
        }
    }
}

enum class NavigationTab(val route: String, val title: String) {
    WISHLIST("wishlist", "Wishlist"),
    SEARCH("search", "Search"),
    ALERTS("alerts", "Alerts"),
    PROFILE("profile", "Profile")
}
