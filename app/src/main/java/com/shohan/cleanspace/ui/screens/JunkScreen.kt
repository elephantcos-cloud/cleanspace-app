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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
fun JunkScreen(viewModel: MainViewModel, navController: NavController) {
    val junkFiles by viewModel.junkFiles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) { viewModel.scanJunk() }

    val totalSelected = junkFiles.filter { it.selected }.sumOf { it.sizeBytes }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Junk Cleaner") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (junkFiles.isNotEmpty()) {
                BottomAppBar {
                    Text(
                        "নির্বাচিত: ${MainViewModel.formatBytes(totalSelected)}",
                        modifier = Modifier.padding(start = 16.dp).weight(1f)
                    )
                    Button(
                        onClick = { viewModel.deleteSelectedJunk() },
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
                junkFiles.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.FolderOff, contentDescription = null, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("কোনো Junk ফাইল পাওয়া যায়নি")
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = { viewModel.scanJunk() }) {
                            Icon(Icons.Filled.Search, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("আবার স্ক্যান করো")
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(junkFiles, key = { it.path }) { file ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = file.selected,
                                        onCheckedChange = { viewModel.toggleJunkSelection(file) }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(file.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                        Text(file.reason, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Text(MainViewModel.formatBytes(file.sizeBytes))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
