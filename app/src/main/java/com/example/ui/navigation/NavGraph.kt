package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.earn.dailybonus.DailyBonusScreen
import com.example.ui.screens.earn.moretasks.MoreTasksScreen
import com.example.ui.screens.earn.referearn.ReferEarnScreen
import com.example.ui.screens.earn.scratchcard.ScratchCardScreen
import com.example.ui.screens.earn.spinwheel.SpinWheelScreen
import com.example.ui.screens.earn.watchearn.WatchEarnScreen
import com.example.ui.screens.leaderboard.LeaderboardScreen
import com.example.ui.screens.main.MainScreen
import com.example.ui.screens.notifications.NotificationsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.splash.SplashScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val startDestination = if (auth.currentUser != null) Routes.MAIN else Routes.AUTH

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.AUTH) {
            AuthScreen(
                onNavigateToHome = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.MAIN) {
            // MainScreen manages its own bottom nav, but we can pass navController 
            // if we need to navigate out to top-level screens.
            MainScreen(
                onNavigateTo = { route ->
                    navController.navigate(route)
                }
            )
        }
        composable(Routes.DAILY_BONUS) {
            DailyBonusScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SPIN_WHEEL) {
            SpinWheelScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.WATCH_EARN) {
            WatchEarnScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SCRATCH_CARD) {
            ScratchCardScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.REFER_EARN) {
            ReferEarnScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.MORE_TASKS) {
            MoreTasksScreen(
                onBack = { navController.popBackStack() },
                onOfferClick = { offer ->
                    navController.navigate("${Routes.OFFER_WEB}?url=${offer.url}&timer=${offer.timer}&reward=${offer.reward}")
                }
            )
        }
        
        composable(
            route = "${Routes.OFFER_WEB}?url={url}&timer={timer}&reward={reward}",
            arguments = listOf(
                androidx.navigation.navArgument("url") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("timer") { type = androidx.navigation.NavType.IntType; defaultValue = 0 },
                androidx.navigation.navArgument("reward") { type = androidx.navigation.NavType.IntType; defaultValue = 0 }
            )
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            val timer = backStackEntry.arguments?.getInt("timer") ?: 0
            val reward = backStackEntry.arguments?.getInt("reward") ?: 0
            com.example.ui.screens.earn.moretasks.OfferWebScreen(
                url = url,
                timer = timer,
                reward = reward,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.LEADERBOARD) {
            LeaderboardScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
