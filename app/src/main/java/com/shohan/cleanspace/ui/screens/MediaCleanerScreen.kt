package com.shohan.cleanspace.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.shohan.cleanspace.data.models.MediaAppInfo
import com.shohan.cleanspace.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaCleanerScreen(viewModel: MainViewModel, navController: NavController) {
    val mediaApps by viewModel.mediaApps.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) { viewModel.scanMediaApps() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Media Cleaner") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("স্ক্যান করা হচ্ছে...")
                    }
                }
                mediaApps.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.ChatBubble, contentDescription = null)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "WhatsApp/Telegram/Messenger পাওয়া যায়নি, অথবা এদের কোনো মিডিয়া নেই",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(mediaApps, key = { it.packageName }) { app ->
                            MediaAppCard(
                                app = app,
                                onDeleteCategory = { categoryName ->
                                    viewModel.deleteMediaCategory(app.packageName, categoryName)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaAppCard(app: MediaAppInfo, onDeleteCategory: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ChatBubble, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "মোট: ${MainViewModel.formatBytes(app.totalBytes)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            app.categories.forEach { category ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(category.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${category.fileCount}টা ফাইল  •  ${MainViewModel.formatBytes(category.bytes)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { onDeleteCategory(category.name) }) {
                        Text("Clean")
                    }
                }
            }
        }
    }
}
