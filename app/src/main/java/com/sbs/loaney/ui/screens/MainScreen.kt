package com.sbs.loaney.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
    
    val topLevelRoutes = listOf(Screen.Home.route, Screen.ManageLoans.route, "profile_screen")
    val isTopLevel = currentDestination?.route in topLevelRoutes

    val items = listOf(
        NavigationItem("Home", Screen.Home.route, Icons.Default.Home),
        NavigationItem("Manage", Screen.ManageLoans.route, Icons.AutoMirrored.Filled.List),
        NavigationItem("Profile", "profile_screen", Icons.Default.Person) 
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {},
        floatingActionButton = {
            if (isTopLevel) {
                 Box(
                     modifier = Modifier
                         .padding(bottom = 16.dp)
                         .height(64.dp)
                         .wrapContentWidth()
                         .clip(CircleShape)
                         .background(Color.Black.copy(alpha = 0.8f))
                         .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                     contentAlignment = Alignment.Center
                 ) {
                     Row(
                         modifier = Modifier.padding(horizontal = 24.dp),
                         verticalAlignment = Alignment.CenterVertically,
                         horizontalArrangement = Arrangement.spacedBy(32.dp)
                     ) {
                         items.forEach { item ->
                             val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                             
                             val scale by animateFloatAsState(
                                 targetValue = if (selected) 1.1f else 1.0f,
                                 label = "iconScale"
                             )
                             
                             Column(
                                 horizontalAlignment = Alignment.CenterHorizontally,
                                 modifier = Modifier
                                     .clickable(
                                         interactionSource = remember { MutableInteractionSource() },
                                         indication = null
                                     ) {
                                         navController.navigate(item.route) {
                                             popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                             launchSingleTop = true
                                             restoreState = true
                                         }
                                     }
                             ) {
                                 Icon(
                                     imageVector = item.icon,
                                     contentDescription = item.label,
                                     tint = if (selected) MaterialTheme.colorScheme.primary else Color.Gray,
                                     modifier = Modifier
                                         .size(24.dp)
                                         .graphicsLayer(scaleX = scale, scaleY = scale)
                                 )
                                 
                                 AnimatedVisibility(
                                     visible = selected,
                                     enter = fadeIn() + expandVertically(),
                                     exit = fadeOut() + shrinkVertically()
                                 ) {
                                     Box(
                                         contentAlignment = Alignment.Center,
                                         modifier = Modifier.padding(top = 4.dp)
                                     ) {
                                         Box(
                                             modifier = Modifier
                                                 .size(8.dp)
                                                 .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape)
                                         )
                                         Box(
                                             modifier = Modifier
                                                 .size(4.dp)
                                                 .background(MaterialTheme.colorScheme.primary, CircleShape)
                                         )
                                     }
                                 }
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
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
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
            composable("profile_screen") { Text("Profile Screen") } // Placeholder
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
