package com.example.ledurecare.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ledurecare.feature.auth.ui.ForgotPasswordRoute
import com.example.ledurecare.feature.auth.ui.LoginRoute
import com.example.ledurecare.feature.auth.ui.SessionExpiredDialog
import com.example.ledurecare.feature.auth.ui.SessionViewModel
import com.example.ledurecare.feature.home.HomeRoute
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

    NavHost(
        navController = navController,
        startDestination = LoginDestination,
        modifier = modifier,
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
