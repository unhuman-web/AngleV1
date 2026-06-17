package com.example.storagecleanup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.storagecleanup.ui.screens.DashboardScreen
import com.example.storagecleanup.ui.screens.DuplicatesScreen
import com.example.storagecleanup.ui.screens.ScanScreen
import com.example.storagecleanup.ui.screens.TrashScreen
import com.example.storagecleanup.ui.theme.StorageCleanupTheme
import com.example.storagecleanup.viewmodel.CleanupViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StorageCleanupTheme {
                StorageCleanupContent()
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Scan : Screen("scan", "Scan", Icons.Default.Folder)
    data object Duplicates : Screen("duplicates", "Duplicates", Icons.Default.DeleteSweep)
    data object Trash : Screen("trash", "Trash", Icons.Default.DeleteSweep)
}

private val screens = listOf(
    Screen.Home,
    Screen.Duplicates,
    Screen.Trash
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageCleanupContent() {
    val navController = rememberNavController()
    val viewModel: CleanupViewModel = viewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Storage Cleanup") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                screens.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
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
                DashboardScreen(
                    viewModel = viewModel,
                    onStartScan = {
                        navController.navigate(Screen.Scan.route)
                    }
                )
            }
            composable(Screen.Scan.route) {
                ScanScreen(
                    viewModel = viewModel,
                    onScanComplete = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.Duplicates.route) {
                DuplicatesScreen(viewModel = viewModel)
            }
            composable(Screen.Trash.route) {
                TrashScreen(viewModel = viewModel)
            }
        }
    }
}
