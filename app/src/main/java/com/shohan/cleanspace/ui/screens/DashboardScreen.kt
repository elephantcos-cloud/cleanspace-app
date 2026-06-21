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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.shohan.cleanspace.ui.components.DonutChart
import com.shohan.cleanspace.ui.components.DonutSlice
import com.shohan.cleanspace.ui.components.ToolCard
import com.shohan.cleanspace.ui.theme.BluePrimary
import com.shohan.cleanspace.ui.theme.CategoryColors
import com.shohan.cleanspace.ui.theme.GreenAccent
import com.shohan.cleanspace.ui.theme.OrangeAccent
import com.shohan.cleanspace.ui.theme.PurpleAccent
import com.shohan.cleanspace.ui.theme.RedAccent
import com.shohan.cleanspace.ui.theme.TealAccent
import com.shohan.cleanspace.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel, navController: NavController) {
    val overview by viewModel.storageOverview.collectAsState()
    val breakdown by viewModel.categoryBreakdown.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadDashboard() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CleanSpace") },
                actions = {
                    IconButton(onClick = { viewModel.loadDashboard() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isLoading && breakdown.isEmpty()) {
                        CircularProgressIndicator()
                    } else {
                        val usedPercent = if (overview.totalBytes > 0)
                            (overview.usedBytes * 100 / overview.totalBytes).toInt() else 0
                        val slices = if (breakdown.isNotEmpty()) {
                            breakdown.mapIndexed { index, c ->
                                DonutSlice(
                                    value = c.bytes.toFloat(),
                                    color = CategoryColors[index % CategoryColors.size],
                                    label = c.name
                                )
                            }
                        } else {
                            listOf(DonutSlice(1f, Color.LightGray, "—"))
                        }
                        DonutChart(
                            slices = slices,
                            centerLabel = "$usedPercent%",
                            centerSubLabel = "ব্যবহৃত"
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "${MainViewModel.formatBytes(overview.usedBytes)} / ${MainViewModel.formatBytes(overview.totalBytes)}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "${MainViewModel.formatBytes(overview.freeBytes)} খালি আছে",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (breakdown.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            breakdown.forEachIndexed { index, category ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(CategoryColors[index % CategoryColors.size], CircleShape)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        category.name,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        MainViewModel.formatBytes(category.bytes),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Text(
                "টুলস",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(toolItems) { tool ->
                    ToolCard(
                        icon = tool.icon,
                        title = tool.title,
                        subtitle = tool.subtitle,
                        accentColor = tool.color,
                        onClick = { navController.navigate(tool.route) }
                    )
                }
            }
        }
    }
}

private data class ToolItem(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

private val toolItems = listOf(
    ToolItem("junk", "Junk Cleaner", "অপ্রয়োজনীয় ফাইল", Icons.Filled.DeleteSweep, RedAccent),
    ToolItem("large_files", "Large Files", "বড় ফাইল খুঁজুন", Icons.Filled.FolderOpen, OrangeAccent),
    ToolItem("duplicates", "Duplicate Finder", "নকল ফাইল খুঁজুন", Icons.Filled.ContentCopy, PurpleAccent),
    ToolItem("orphaned", "Orphaned Data", "Leftover App data", Icons.Filled.FolderOff, TealAccent),
    ToolItem("media_cleaner", "Media Cleaner", "WhatsApp/Telegram", Icons.Filled.ChatBubble, GreenAccent),
    ToolItem("apps", "App Manager", "App-wise storage", Icons.Filled.Apps, BluePrimary)
)
