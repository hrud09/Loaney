package com.sbs.loaney.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sbs.loaney.ui.navigation.Screen
import androidx.compose.ui.res.stringResource
import com.sbs.loaney.R

@Composable
fun MainScreen(startDestination: String = Screen.Home.route) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val topLevelRoutes = listOf(Screen.Home.route, Screen.ManageLoans.route)
    val isTopLevel = currentDestination?.route in topLevelRoutes

    val items = listOf(
        NavigationItem(stringResource(id = R.string.nav_home), Screen.Home.route, Icons.Default.Home),
        NavigationItem(stringResource(id = R.string.nav_history), Screen.ManageLoans.route, Icons.AutoMirrored.Filled.List)
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (isTopLevel) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    items.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = 0.6f, // Heavy elastic
                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                    )
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = 0.6f,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                    )
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = 0.6f,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                    )
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = 0.6f,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                    )
                )
            }
        ) {
            composable(Screen.Onboarding.route) {
                val coroutineScope = rememberCoroutineScope()
                // You can inject SettingsRepository here via Hilt/ViewModel, but for simplicity we'll assume it's done elsewhere or pass a lambda.
                // It's better to pass an onFinish lambda to keep MainScreen decoupled exactly from DataStore where possible.
                // We'll update MainActivity to handle this
                OnboardingScreen(
                    onFinish = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToAddLoan = { type -> navController.navigate(Screen.AddLoan.createRoute(type)) },
                    onNavigateToDetail = { loanId ->
                        navController.navigate(Screen.LoanDetail.createRoute(loanId))
                    },
                    onNavigateToHistory = { type -> 
                        navController.navigate(Screen.ManageLoans.createRoute(type)) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(
                route = Screen.ManageLoans.route,
                arguments = listOf(navArgument("type") {
                    type = NavType.StringType
                    nullable = true
                })
            ) { backStackEntry ->
                val initialType = backStackEntry.arguments?.getString("type")
                ManageLoansScreen(
                    initialType = initialType,
                    onNavigateToAddLoan = { type -> navController.navigate(Screen.AddLoan.createRoute(type)) },
                    onNavigateToDetail = { loanId ->
                        navController.navigate(Screen.LoanDetail.createRoute(loanId))
                    }
                )
            }
            composable(
                route = Screen.AddLoan.route,
                arguments = listOf(navArgument("type") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = "LEND"
                })
            ) { backStackEntry ->
                val typeStr = backStackEntry.arguments?.getString("type") ?: "LEND"
                val initialType = try {
                    com.sbs.loaney.data.model.LoanType.valueOf(typeStr)
                } catch (e: Exception) {
                    com.sbs.loaney.data.model.LoanType.LEND
                }
                AddLoanScreen(
                    initialType = initialType,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.LoanDetail.route,
                arguments = listOf(navArgument("loanId") { type = NavType.LongType })
            ) { backStackEntry ->
                val loanId = backStackEntry.arguments?.getLong("loanId") ?: return@composable
                LoanTrackerScreen(
                    loanId = loanId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

data class NavigationItem(val label: String, val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
