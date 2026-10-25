package com.jinatra.wishplay.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jinatra.wishplay.data.local.prefs.UserPreferences
import com.jinatra.wishplay.ui.custom.CustomGameScreen
import com.jinatra.wishplay.ui.custom.CustomGameViewModel
import com.jinatra.wishplay.ui.details.GameDetailsScreen
import com.jinatra.wishplay.ui.details.GameDetailsViewModel
import com.jinatra.wishplay.ui.main.MainScreen
import com.jinatra.wishplay.ui.navigation.Screen
import com.jinatra.wishplay.ui.theme.AccentColor
import com.jinatra.wishplay.ui.theme.WishPlayTheme
import com.jinatra.wishplay.ui.welcome.WelcomeScreen
import com.jinatra.wishplay.ui.welcome.WelcomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val themeMode by userPreferences.themeMode.collectAsState(initial = "system")
            val accentName by userPreferences.accentColor.collectAsState(initial = "gold")
            val isOnboardingDone by userPreferences.isOnboardingDone.collectAsState(initial = null)

            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            val accentColor = AccentColor.fromKey(accentName)

            WishPlayTheme(
                darkTheme = darkTheme,
                accentColor = accentColor
            ) {
                val extraGameId = intent?.getStringExtra("extra_game_id")
                Surface(modifier = Modifier.fillMaxSize()) {
                    val onboardingDone = isOnboardingDone
                    if (onboardingDone == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    } else {
                    WishPlayApp(
                        isOnboardingDone = onboardingDone,
                        extraGameId = extraGameId
                    )
                    }
                }
            }
        }
    }
}

@Composable
fun WishPlayApp(
    isOnboardingDone: Boolean,
    extraGameId: String? = null
) {
    val navController = rememberNavController()
    val startDestination = if (isOnboardingDone) Screen.Main.route else Screen.Welcome.route

    androidx.compose.runtime.LaunchedEffect(extraGameId) {
        if (!extraGameId.isNullOrBlank()) {
            navController.navigate(Screen.Details.createRoute(extraGameId))
        }
    }

    NavHost(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding(),
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Welcome.route) {
            val welcomeViewModel: WelcomeViewModel = hiltViewModel()
            WelcomeScreen(
                viewModel = welcomeViewModel,
                onNavigateToWishlist = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                onNavigateToDetails = { gameId ->
                    navController.navigate(Screen.Details.createRoute(gameId))
                },
                onNavigateToCustom = {
                    navController.navigate(Screen.CustomGame.route)
                },
                onNavigateToCalendar = {
                    navController.navigate(Screen.Calendar.route)
                },
                onNavigateToAdmin = {
                    navController.navigate(Screen.Admin.route)
                }
            )
        }

        composable(Screen.CustomGame.route) {
            val customViewModel: CustomGameViewModel = hiltViewModel()
            CustomGameScreen(
                viewModel = customViewModel,
                onBack = { navController.popBackStack() },
                onGameSaved = { gameId ->
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Details("").route,
            arguments = listOf(navArgument("gameId") { type = NavType.StringType })
        ) {
            val detailsViewModel: GameDetailsViewModel = hiltViewModel()
            GameDetailsScreen(
                viewModel = detailsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Calendar.route) {
            val alertsViewModel: com.jinatra.wishplay.ui.alerts.AlertsViewModel = hiltViewModel()
            val wishlistViewModel: com.jinatra.wishplay.ui.wishlist.WishlistViewModel = hiltViewModel()
            val wishlistState by wishlistViewModel.uiState.collectAsState()

            com.jinatra.wishplay.ui.alerts.CalendarScreen(
                viewModel = alertsViewModel,
                wishlistGames = wishlistState.games,
                onBack = { navController.popBackStack() },
                onNavigateToDetails = { gameId ->
                    navController.navigate(Screen.Details.createRoute(gameId))
                }
            )
        }

        composable(Screen.Admin.route) {
            val adminViewModel: com.jinatra.wishplay.ui.admin.AdminViewModel = hiltViewModel()
            com.jinatra.wishplay.ui.admin.AdminScreen(
                viewModel = adminViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
