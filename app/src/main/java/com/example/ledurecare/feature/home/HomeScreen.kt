package com.example.ledurecare.feature.home

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.ledurecare.R
import com.example.ledurecare.core.ui.LedureCareLogo
import com.example.ledurecare.feature.dealers.ui.DealersRoute
import com.example.ledurecare.feature.audit.ui.AuditDestination
import com.example.ledurecare.feature.audit.ui.AuditFileDestination
import com.example.ledurecare.feature.audit.ui.AuditFileRoute
import com.example.ledurecare.feature.audit.ui.AuditRoute
import com.example.ledurecare.feature.audit.ui.AuditSubmittedDestination
import com.example.ledurecare.feature.audit.ui.AuditSubmittedRoute
import com.example.ledurecare.feature.plan.ui.PlanRoute
import com.example.ledurecare.feature.reports.ui.DliDetailDestination
import com.example.ledurecare.feature.reports.ui.reportsGraph
import com.example.ledurecare.ui.theme.GreenMint
import com.example.ledurecare.ui.theme.Green600
import com.example.ledurecare.ui.theme.Ink
import com.example.ledurecare.ui.theme.Muted
import com.example.ledurecare.ui.theme.PageBg

@Composable
fun HomeRoute(
    onLoggedOut: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val tabController = rememberNavController()
    ExitOnDoubleBack(tabController)

    Scaffold(
        containerColor = PageBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LedureTopBar(
                subtitle = listOfNotNull(user?.name, user?.roleName?.takeIf { it.isNotBlank() }).joinToString(" · "),
                onSignOut = { viewModel.onLogout(onLoggedOut) },
            )
        },
        bottomBar = { LedureBottomBar(tabController) },
    ) { padding ->
        NavHost(
            navController = tabController,
            startDestination = PlanGraph,
            modifier = Modifier.padding(padding),
        ) {
            navigation<PlanGraph>(startDestination = PlanHomeDestination) {
                composable<PlanHomeDestination> {
                    PlanRoute(
                        onStartAudit = { tabController.navigate(AuditDestination(it)) },
                        onViewDli = { tabController.navigate(DliDetailDestination(it)) },
                    )
                }
                composable<AuditDestination> {
                    AuditRoute(
                        onClose = { tabController.popBackStack() },
                        // The form is replaced by the confirmation, so Back goes to the plan, not into a spent form.
                        onSubmitted = { dliNo ->
                            tabController.navigate(AuditSubmittedDestination(dliNo)) {
                                popUpTo<AuditDestination> { inclusive = true }
                            }
                        },
                    )
                }
                composable<AuditSubmittedDestination> {
                    AuditSubmittedRoute(
                        onBackToPlan = { tabController.popBackStack(PlanHomeDestination, inclusive = false) },
                        onOpenFile = { dliNo, index -> tabController.navigate(AuditFileDestination(dliNo, index)) },
                    )
                }
                composable<AuditFileDestination> {
                    AuditFileRoute(onBack = { tabController.popBackStack() })
                }
            }
            composable<DealersTab> { DealersRoute() }
            reportsGraph(tabController, onEditDli = { tabController.navigate(AuditDestination(returnId = it)) })
        }
    }
}

/**
 * On the landing screen (nothing behind it to go back to) the first Back only shows a hint; a second
 * Back within two seconds leaves the app. Everywhere else Back keeps its normal job.
 */
@Composable
private fun ExitOnDoubleBack(navController: androidx.navigation.NavHostController) {
    val context = LocalContext.current
    val current by navController.currentBackStackEntryAsState()
    val atLanding = current != null && navController.previousBackStackEntry == null
    var lastBackAt by remember { mutableStateOf(0L) }
    val hint = stringResource(R.string.home_press_back_again)
    BackHandler(enabled = atLanding) {
        val now = System.currentTimeMillis()
        if (now - lastBackAt <= EXIT_WINDOW_MS) {
            (context as? android.app.Activity)?.finish()
        } else {
            lastBackAt = now
            Toast.makeText(context, hint, Toast.LENGTH_SHORT).show()
        }
    }
}

private const val EXIT_WINDOW_MS = 2_000L

@Composable
private fun LedureTopBar(subtitle: String, onSignOut: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Green600)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            LedureCareLogo(height = 16.dp)
            if (subtitle.isNotBlank()) {
                Text(subtitle, fontSize = 10.sp, color = Color.White.copy(alpha = 0.9f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        IconButton(onClick = {}) {
            Icon(Icons.Outlined.Notifications, contentDescription = stringResource(R.string.home_notifications), tint = Color.White)
        }
        Box {
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier.size(34.dp).clip(CircleShape).background(Color.White),
            ) {
                Icon(Icons.Outlined.Person, contentDescription = stringResource(R.string.home_account), tint = Ink)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.home_sign_out)) },
                    onClick = {
                        menuOpen = false
                        onSignOut()
                    },
                )
            }
        }
    }
}

@Composable
private fun LedureBottomBar(navController: androidx.navigation.NavHostController) {
    val destination = navController.currentBackStackEntryAsState().value?.destination
    NavigationBar(containerColor = Color.White) {
        HomeTabs.forEach { tab ->
            val selected = destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.label), fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Ink,
                    selectedTextColor = Ink,
                    indicatorColor = GreenMint,
                    unselectedIconColor = Muted,
                    unselectedTextColor = Muted,
                ),
            )
        }
    }
}
