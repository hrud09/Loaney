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
import com.sbs.loaney.ui.theme.NeonLime
import com.sbs.loaney.ui.theme.SurfaceDark

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val topLevelRoutes = listOf(Screen.Home.route, Screen.ManageLoans.route, "profile_screen")
    val isTopLevel = currentDestination?.route in topLevelRoutes

    val items = listOf(
        NavigationItem("Home", Screen.Home.route, Icons.Default.Home),
        NavigationItem("Manage", Screen.ManageLoans.route, Icons.AutoMirrored.Filled.List),
        NavigationItem("Profile", "profile_screen", Icons.Default.Person) 
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (isTopLevel) {
                NavigationBar(
                    containerColor = SurfaceDark,
                    contentColor = NeonLime
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
                                selectedIconColor = Color.Black,
                                selectedTextColor = NeonLime,
                                indicatorColor = NeonLime,
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToManage = { navController.navigate(Screen.ManageLoans.route) },
                    onNavigateToAddLoan = { navController.navigate(Screen.AddLoan.route) },
                    onNavigateToDetail = { loanId ->
                        navController.navigate(Screen.LoanDetail.createRoute(loanId))
                    }
                )
            }
            composable(Screen.ManageLoans.route) {
                ManageLoansScreen(
                    onNavigateToAddLoan = { navController.navigate(Screen.AddLoan.route) },
                    onNavigateToDetail = { loanId ->
                        navController.navigate(Screen.LoanDetail.createRoute(loanId))
                    }
                )
            }
            composable(Screen.AddLoan.route) {
                AddLoanScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable("profile_screen") { ProfileScreen() }
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
