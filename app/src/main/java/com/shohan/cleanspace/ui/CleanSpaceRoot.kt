package com.shohan.cleanspace.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shohan.cleanspace.ui.screens.AppsScreen
import com.shohan.cleanspace.ui.screens.DashboardScreen
import com.shohan.cleanspace.ui.screens.DuplicatesScreen
import com.shohan.cleanspace.ui.screens.JunkScreen
import com.shohan.cleanspace.ui.screens.LargeFilesScreen
import com.shohan.cleanspace.ui.screens.MediaCleanerScreen
import com.shohan.cleanspace.ui.screens.OrphanedDataScreen
import com.shohan.cleanspace.ui.screens.PermissionScreen
import com.shohan.cleanspace.ui.screens.SettingsScreen
import com.shohan.cleanspace.viewmodel.MainViewModel

@Composable
fun CleanSpaceRoot(viewModel: MainViewModel) {
    val permissions by viewModel.permissions.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    if (!permissions.allFilesAccess || !permissions.usageAccess) {
        PermissionScreen(permissions = permissions, onRefresh = { viewModel.refreshPermissions() })
        return
    }

    val navController = rememberNavController()
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") { DashboardScreen(viewModel, navController) }
            composable("junk") { JunkScreen(viewModel, navController) }
            composable("large_files") { LargeFilesScreen(viewModel, navController) }
            composable("duplicates") { DuplicatesScreen(viewModel, navController) }
            composable("orphaned") { OrphanedDataScreen(viewModel, navController) }
            composable("media_cleaner") { MediaCleanerScreen(viewModel, navController) }
            composable("apps") { AppsScreen(viewModel, navController) }
            composable("settings") { SettingsScreen(viewModel, navController) }
        }
    }
}
