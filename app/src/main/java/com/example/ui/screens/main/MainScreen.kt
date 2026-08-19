package com.example.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.ui.navigation.Routes
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.wallet.WalletScreen
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.BackgroundDark
import kotlinx.coroutines.launch

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Block

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavItem(Routes.HOME, "Home", Icons.Filled.Home)
    object Earn : BottomNavItem(Routes.EARN, "Earn", Icons.Filled.MonetizationOn)
    object Wallet : BottomNavItem(Routes.WALLET, "Wallet", Icons.Filled.Wallet)
    object History : BottomNavItem(Routes.HISTORY, "History", Icons.Filled.History)
}

@Composable
fun BlockedScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Block, contentDescription = "Blocked", tint = Color.Red, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Account Blocked", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Your account has been suspended.", color = Color.Gray)
            Text("Contact support for more info.", color = Color.Gray)
        }
    }
}

@Composable
fun MainScreen(
    onNavigateTo: (String) -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    if (user?.isBlocked == true) {
        BlockedScreen()
        return
    }

    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Handle result if needed
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Earn,
        BottomNavItem.Wallet,
        BottomNavItem.History
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF151528),
                modifier = Modifier.width(280.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    if (user?.photoUrl?.isNotEmpty() == true) {
                        AsyncImage(
                            model = user?.photoUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF6D00)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user?.name?.firstOrNull()?.toString()?.uppercase() ?: "U",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = user?.name?.takeIf { it.isNotBlank() } ?: "Guest User",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = user?.email?.takeIf { it.isNotBlank() } ?: "guest@example.com",
                        color = Color(0xFFAAAAB4),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    DrawerItem(icon = Icons.Filled.Home, title = "Home") {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate(Routes.HOME) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    }
                    DrawerItem(icon = Icons.Filled.MonetizationOn, title = "Earn Coins") {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate(Routes.EARN) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    }
                    DrawerItem(icon = Icons.Filled.Wallet, title = "Wallet") {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate(Routes.WALLET) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    }
                    DrawerItem(icon = Icons.Filled.History, title = "History") {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate(Routes.HISTORY) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    }
                    DrawerItem(icon = Icons.Filled.GroupAdd, title = "Invite Friends") {
                        coroutineScope.launch { drawerState.close() }
                        onNavigateTo(Routes.REFER_EARN)
                    }

                    DrawerItem(icon = Icons.Filled.Settings, title = "Settings") {
                        coroutineScope.launch { drawerState.close() }
                        onNavigateTo(Routes.SETTINGS)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    DrawerItem(icon = Icons.Filled.Logout, title = "Logout", color = Color.Red) {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.signOut()
                        onNavigateTo(Routes.AUTH)
                    }
                }
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF0B0B16),
                    contentColor = Color.White,
                    tonalElevation = 0.dp
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title, fontSize = 11.sp) },
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFFD500F9),
                                selectedTextColor = Color(0xFFD500F9),
                                unselectedIconColor = Color(0xFFAAAAB4),
                                unselectedTextColor = Color(0xFFAAAAB4),
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(innerPadding).background(Color(0xFF0B0B16))
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onNavigateTo = { route -> 
                            if (route == Routes.WALLET) {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            } else {
                                onNavigateTo(route)
                            }
                        },
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                        user = user,
                        appConfig = viewModel.appConfig.collectAsStateWithLifecycle().value
                    )
                }
                composable(Routes.EARN) {
                    com.example.ui.screens.earn.EarnScreen(
                        onNavigateTo = { route -> 
                            if (route == Routes.WALLET) {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            } else {
                                onNavigateTo(route)
                            }
                        }
                    )
                }
                composable(Routes.WALLET) {
                    WalletScreen(
                        onBack = { navController.navigate(Routes.HOME) },
                        onHistoryClick = { navController.navigate(Routes.HISTORY) }
                    )
                }
                composable(Routes.HISTORY) {
                    com.example.ui.screens.history.HistoryScreen(
                        onBack = { navController.navigate(Routes.HOME) }
                    )
                }
                composable(Routes.LEADERBOARD) {
                    com.example.ui.screens.leaderboard.LeaderboardScreen(
                        onBack = { navController.navigate(Routes.HOME) }
                    )
                }
            }
        }
    }
}

@Composable
fun DrawerItem(icon: ImageVector, title: String, color: Color = Color.White, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = color)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
