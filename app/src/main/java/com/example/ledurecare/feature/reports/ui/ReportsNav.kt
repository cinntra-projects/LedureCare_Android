package com.example.ledurecare.feature.reports.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import kotlinx.serialization.Serializable

/** The Reports tab. Its screens live in this nested graph so the shell's header and tab bar stay put. */
@Serializable
data object ReportsGraph

@Serializable
data object ReportsHomeDestination

/** Property names double as argument keys; [DliListViewModel] reads them from its SavedStateHandle. */
@Serializable
data class DliListDestination(val stage: String? = null, val focusSearch: Boolean = false)

@Serializable
data class DliDetailDestination(val id: Int)

fun NavGraphBuilder.reportsGraph(navController: NavHostController) {
    navigation<ReportsGraph>(startDestination = ReportsHomeDestination) {
        composable<ReportsHomeDestination> {
            ReportsRoute(
                onOpenStage = { navController.navigate(DliListDestination(stage = it.code)) },
                onOpenSearch = { navController.navigate(DliListDestination(focusSearch = true)) },
            )
        }
        composable<DliListDestination> {
            DliListRoute(
                onBack = { navController.popBackStack() },
                onOpenDli = { navController.navigate(DliDetailDestination(it)) },
            )
        }
        composable<DliDetailDestination> {
            DliDetailRoute(onBack = { navController.popBackStack() })
        }
    }
}
