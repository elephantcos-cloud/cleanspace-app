package com.shohan.cleanspace.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.shohan.cleanspace.data.models.DuplicateFile
import com.shohan.cleanspace.viewmodel.MainViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatesScreen(viewModel: MainViewModel, navController: NavController) {
    val groups by viewModel.duplicateGroups.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) { viewModel.scanDuplicates() }

    val totalWasted = groups.sumOf { it.wastedBytes }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Duplicate Finder") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (groups.isNotEmpty()) {
                BottomAppBar {
                    Text(
                        "নষ্ট হচ্ছে: ${MainViewModel.formatBytes(totalWasted)}",
                        modifier = Modifier.padding(start = 16.dp).weight(1f)
                    )
                    Button(
                        onClick = { viewModel.deleteUnselectedDuplicates() },
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Delete copies")
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
                        Text("ফাইল মিলিয়ে দেখা হচ্ছে...")
                    }
                }
                groups.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("কোনো নকল ফাইল পাওয়া যায়নি")
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(groups) { groupIndex, group ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "${group.files.size}টা একই ফাইল  •  ${MainViewModel.formatBytes(group.sizeBytes)} প্রতিটা",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    group.files.forEach { file ->
                                        DuplicateFileRow(
                                            file = file,
                                            onToggle = {
                                                viewModel.toggleDuplicateKeep(groupIndex, file.path)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val previewableExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic")

@Composable
private fun DuplicateFileRow(file: DuplicateFile, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val extension = file.name.substringAfterLast('.', "").lowercase()
        if (extension in previewableExtensions) {
            AsyncImage(
                model = File(file.path),
                contentDescription = file.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.InsertDriveFile, contentDescription = file.name)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(file.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(
                if (file.keepThisOne) "রাখা হবে" else "মুছে ফেলা হবে",
                style = MaterialTheme.typography.bodyMedium,
                color = if (file.keepThisOne) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
            )
        }
        IconButton(onClick = onToggle) {
            Icon(
                if (file.keepThisOne) Icons.Filled.CheckCircle else Icons.Filled.ThumbUp,
                contentDescription = "Toggle keep",
                tint = if (file.keepThisOne) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline
            )
        }
    }
}
