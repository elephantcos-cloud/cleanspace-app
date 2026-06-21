package com.shohan.cleanspace.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.shohan.cleanspace.data.models.ThemeMode
import com.shohan.cleanspace.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, navController: NavController) {
    val themeMode by viewModel.themeMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Theme", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            ThemeOption("Light", ThemeMode.LIGHT, themeMode) { viewModel.setThemeMode(it) }
            ThemeOption("Dark", ThemeMode.DARK, themeMode) { viewModel.setThemeMode(it) }
            ThemeOption("System Default", ThemeMode.SYSTEM, themeMode) { viewModel.setThemeMode(it) }

            Spacer(Modifier.height(24.dp))
            Divider()
            Spacer(Modifier.height(16.dp))
            Text("CleanSpace v1.0", style = MaterialTheme.typography.bodyMedium)
            Text("তৈরি করেছে শোহান", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ThemeOption(
    label: String,
    mode: ThemeMode,
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(mode) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = current == mode, onClick = { onSelect(mode) })
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}
