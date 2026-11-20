package com.cinntra.ledurecare.feature.home

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.ui.graphics.vector.ImageVector
import com.cinntra.ledurecare.R
import com.cinntra.ledurecare.feature.reports.ui.ReportsGraph
import kotlinx.serialization.Serializable

/** The My Plan tab: the plan itself plus the audit form opened from it. */
@Serializable
data object PlanGraph

@Serializable
data object PlanHomeDestination

@Serializable
data object DealersTab

/** One entry of the bottom bar. */
data class HomeTab(
    val route: Any,
    @StringRes val label: Int,
    val icon: ImageVector,
)

val HomeTabs = listOf(
    HomeTab(PlanGraph, R.string.nav_my_plan, Icons.Outlined.CalendarMonth),
    HomeTab(DealersTab, R.string.nav_my_dealers, Icons.Outlined.Storefront),
    HomeTab(ReportsGraph, R.string.nav_reports, Icons.Outlined.BarChart),
)
