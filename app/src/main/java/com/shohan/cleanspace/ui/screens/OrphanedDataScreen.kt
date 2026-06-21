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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.shohan.cleanspace.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrphanedDataScreen(viewModel: MainViewModel, navController: NavController) {
    val orphanedList by viewModel.orphanedItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val permissions by viewModel.permissions.collectAsState()

    val shizukuReady = permissions.shizukuRunning && permissions.shizukuPermission

    LaunchedEffect(shizukuReady) {
        if (shizukuReady) viewModel.scanOrphanedData()
    }

    val totalSelected = orphanedList.filter { it.selected }.sumOf { it.sizeBytes }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Orphaned Data") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (orphanedList.isNotEmpty()) {
                BottomAppBar {
                    Text(
                        "নির্বাচিত: ${MainViewModel.formatBytes(totalSelected)}",
                        modifier = Modifier.padding(start = 16.dp).weight(1f)
                    )
                    Button(
                        onClick = { viewModel.deleteSelectedOrphaned() },
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                !shizukuReady -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.FolderOff, contentDescription = null, modifier = Modifier.padding(bottom = 12.dp))
                        Text(
                            "এই ফিচারের জন্য Shizuku চালু থাকা দরকার",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Android 11+ এ uninstall করা App-এর leftover ফোল্ডার দেখতে hলে shell-level access লাগে — এটা শুধু Shizuku দিয়েই সম্ভব। README.md দেখো setup করার উপায়।",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
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
                orphanedList.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("কোনো Orphaned Data পাওয়া যায়নি — সব পরিষ্কার আছে!")
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(orphanedList, key = { it.path }) { item ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = item.selected,
                                        onCheckedChange = { viewModel.toggleOrphanedSelection(item) }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.packageName, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                        Text(item.location, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Text(MainViewModel.formatBytes(item.sizeBytes))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
