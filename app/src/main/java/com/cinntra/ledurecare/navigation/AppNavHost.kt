package com.cinntra.ledurecare.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavDestination.Companion.hasRoute
import com.cinntra.ledurecare.feature.auth.ui.AuthBackdrop
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cinntra.ledurecare.feature.auth.ui.ForgotPasswordRoute
import com.cinntra.ledurecare.feature.auth.ui.LoginRoute
import com.cinntra.ledurecare.feature.auth.ui.SessionExpiredDialog
import com.cinntra.ledurecare.feature.auth.ui.SessionViewModel
import com.cinntra.ledurecare.feature.home.HomeRoute
import kotlinx.serialization.Serializable

@Serializable
data object LoginDestination

@Serializable
data object ForgotPasswordDestination

@Serializable
data object HomeDestination

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val sessionExpired by sessionViewModel.expired.collectAsStateWithLifecycle()

    // One video behind login, OTP and forgot password: only the card in front changes between them.
    val destination by navController.currentBackStackEntryAsState()
    val onAuthScreen = destination?.destination.let {
        it == null || it.hasRoute<LoginDestination>() || it.hasRoute<ForgotPasswordDestination>()
    }

    Box(modifier) {
    if (onAuthScreen) AuthBackdrop()
    NavHost(
        navController = navController,
        startDestination = LoginDestination,
    ) {
        composable<LoginDestination> {
            LoginRoute(
                onSignedIn = {
                    navController.navigate(HomeDestination) {
                        popUpTo(LoginDestination) { inclusive = true }
                    }
                },
                onForgotPassword = { navController.navigate(ForgotPasswordDestination) },
            )
        }
        composable<ForgotPasswordDestination> {
            ForgotPasswordRoute(
                onBack = { navController.popBackStack() },
                onSignedIn = {
                    navController.navigate(HomeDestination) {
                        popUpTo(LoginDestination) { inclusive = true }
                    }
                },
                onSignInAgain = { navController.popBackStack() },
            )
        }
        composable<HomeDestination> {
            HomeRoute(onLoggedOut = {
                navController.navigate(LoginDestination) {
                    popUpTo(HomeDestination) { inclusive = true }
                }
            })
        }
    }
    }

    // Shown over whatever screen the user is on when the server rejects their token.
    if (sessionExpired) {
        SessionExpiredDialog(
            onLoginAgain = {
                sessionViewModel.loginAgain {
                    navController.navigate(LoginDestination) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    }
                }
            },
        )
    }
}
