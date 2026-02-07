package com.sbs.loaney.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sbs.loaney.ui.navigation.Screen

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val topLevelRoutes = listOf(Screen.Home.route, Screen.ManageLoans.route)
    val isTopLevel = currentDestination?.route in topLevelRoutes
    val isHome = currentDestination?.route == Screen.Home.route

    val items = listOf(
        NavigationItem("Home", Screen.Home.route, Icons.Default.Home),
        NavigationItem("Manage", Screen.ManageLoans.route, Icons.AutoMirrored.Filled.List)
    )

    Scaffold(
        bottomBar = {}, // We will overlay the custom bottom bar in the content using Box
        floatingActionButton = {
            if (isTopLevel) {
                 // Custom Floating Bottom Navigation
                 Surface(
                     color = MaterialTheme.colorScheme.surfaceVariant,
                     shape = CircleShape,
                     modifier = Modifier
                         .padding(bottom = 16.dp)
                         .height(72.dp)
                         .wrapContentWidth(),
                     shadowElevation = 8.dp
                 ) {
                     Row(
                         modifier = Modifier.padding(horizontal = 8.dp),
                         verticalAlignment = Alignment.CenterVertically,
                         horizontalArrangement = Arrangement.spacedBy(8.dp)
                     ) {
                         items.forEach { item ->
                             val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                             
                             IconButton(
                                 onClick = {
                                     navController.navigate(item.route) {
                                         popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                         launchSingleTop = true
                                         restoreState = true
                                     }
                                 },
                                 modifier = Modifier
                                     .clip(CircleShape)
                                     .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                             ) {
                                 Icon(
                                     imageVector = item.icon,
                                     contentDescription = item.label,
                                     tint = if (selected) Color.Black else Color.Gray,
                                     modifier = Modifier.size(24.dp)
                                 )
                             }
                         }

                         // Add Loan FAB inside the bar - only shown when NOT on Home
                         if (!isHome) {
                             Spacer(modifier = Modifier.width(8.dp))
                             IconButton(
                                 onClick = { navController.navigate(Screen.AddLoan.route) },
                                 modifier = Modifier
                                     .clip(CircleShape)
                                     .background(MaterialTheme.colorScheme.secondary)
                                     .size(48.dp)
                             ) {
                                 Icon(
                                     Icons.Default.Add,
                                     contentDescription = "Add Loan",
                                     tint = Color.Black
                                 )
                             }
                         }
                     }
                 }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
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
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAddLoan = { navController.navigate(Screen.AddLoan.route) },
                    onNavigateToDetail = { loanId ->
                        navController.navigate(Screen.LoanDetail.createRoute(loanId))
                    }
                )
            }
            composable(Screen.AddLoan.route) {
                AddLoanScreen(onNavigateBack = { navController.popBackStack() })
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
