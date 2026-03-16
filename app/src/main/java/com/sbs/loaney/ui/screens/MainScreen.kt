package com.sbs.loaney.ui.screens

import com.sbs.loaney.ui.screens.ShopScreen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import com.sbs.loaney.ui.components.bounceClick
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sbs.loaney.data.model.UserProfile
import com.sbs.loaney.ui.components.ProfileSidebarContent
import com.sbs.loaney.ui.navigation.Screen
import androidx.compose.ui.res.stringResource
import com.sbs.loaney.R
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbs.loaney.ui.theme.*
import com.sbs.loaney.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    startDestination: String = Screen.Home.route,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val topLevelRoutes = listOf(Screen.Home.route, Screen.ManageLoans.route, Screen.Shop.route)
    val isTopLevel = currentDestination?.route in topLevelRoutes

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val settingsState by settingsViewModel.uiState.collectAsState()
    val userProfile = UserProfile(name = settingsState.userName)

    // Bottom nav items (excluding center FAB)
    val navItems = listOf(
        BkashNavItem(stringResource(R.string.nav_home), Screen.Home.route, Icons.Default.Home),
        BkashNavItem(stringResource(R.string.nav_history), Screen.ManageLoans.route, Icons.AutoMirrored.Filled.List)
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isTopLevel,
        drawerContent = {
            ProfileSidebarContent(
                profile = userProfile,
                onNavigateToSettings = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToHistory = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.ManageLoans.createRoute()) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToShop = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.Shop.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (isTopLevel) {
                    BkashBottomNavBar(
                        navItems = navItems,
                        currentDestination = currentDestination,
                        onNavItemClick = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onCenterFabClick = { navController.navigate(Screen.AddLoan.createRoute("LEND")) },
                        onProfileClick = { scope.launch { drawerState.open() } },
                        onShopClick = { 
                            navController.navigate(Screen.Shop.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding),
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(350, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(350))
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(350, easing = FastOutSlowInEasing),
                        targetOffset = { it / 4 }
                    ) + fadeOut(animationSpec = tween(350))
                },
                popEnterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(350, easing = FastOutSlowInEasing),
                        initialOffset = { it / 4 }
                    ) + fadeIn(animationSpec = tween(250, delayMillis = 100))
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(350, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(150))
                }
            ) {
                composable(Screen.Onboarding.route) {
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
                composable(Screen.Shop.route) {
                    ShopScreen(onNavigateBack = { navController.popBackStack() })
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
}

// ── bKash-style Bottom Navigation Bar ────────────────────────────────────────
@Composable
fun BkashBottomNavBar(
    navItems: List<BkashNavItem>,
    currentDestination: androidx.navigation.NavDestination?,
    onNavItemClick: (String) -> Unit,
    onCenterFabClick: () -> Unit,
    onProfileClick: () -> Unit,
    onShopClick: () -> Unit
) {
    val pink = MaterialTheme.colorScheme.primary
    val gray = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Top divider line
        HorizontalDivider(
            modifier = Modifier.align(Alignment.TopCenter),
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 0.8.dp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home
            val homeSelected = currentDestination?.hierarchy?.any { it.route == Screen.Home.route } == true
            BkashNavBarItem(
                icon = Icons.Default.Home,
                label = stringResource(R.string.nav_home),
                selected = homeSelected,
                onClick = { onNavItemClick(Screen.Home.route) }
            )

            // History
            val historySelected = currentDestination?.hierarchy?.any { it.route == Screen.ManageLoans.route } == true
            BkashNavBarItem(
                icon = Icons.AutoMirrored.Filled.List,
                label = stringResource(R.string.nav_history),
                selected = historySelected,
                onClick = { onNavItemClick(Screen.ManageLoans.route) }
            )

            // Center FAB — Add button
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .then(Modifier.shadow(elevation = 12.dp, shape = CircleShape))
                    .clip(CircleShape)
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(pink, pink.copy(alpha = 0.8f))
                        )
                    )
                    .bounceClick { onCenterFabClick() }
                    .then(Modifier.wrapContentSize()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            // Shop
            val shopSelected = currentDestination?.hierarchy?.any { it.route == Screen.Shop.route } == true
            BkashNavBarItem(
                icon = Icons.Default.ShoppingBag,
                label = "Shop",
                selected = shopSelected,
                onClick = onShopClick
            )

            // Profile
            BkashNavBarItem(
                icon = Icons.Default.AccountCircle,
                label = "Profile",
                selected = false,
                onClick = onProfileClick
            )
        }
    }
}

@Composable
private fun BkashNavBarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val pink = MaterialTheme.colorScheme.primary
    val gray = MaterialTheme.colorScheme.onSurfaceVariant
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    
    val color by animateColorAsState(
        targetValue = if (selected) pink else gray,
        animationSpec = tween(300),
        label = "item_color"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "item_scale"
    )

    Box(
        modifier = Modifier
            .width(72.dp)
            .height(80.dp)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.then(Modifier.graphicsLayer(scaleX = scale, scaleY = scale))
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (selected) pink.copy(alpha = 0.1f) else Color.Transparent)
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = label,
                color = color,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

data class BkashNavItem(val label: String, val route: String, val icon: ImageVector)
data class NavigationItem(val label: String, val route: String, val icon: ImageVector)
