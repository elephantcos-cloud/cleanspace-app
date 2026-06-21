package com.shohan.cleanspace.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shohan.cleanspace.data.PermissionHelper
import com.shohan.cleanspace.data.models.AppPermissions
import com.shohan.cleanspace.ui.theme.GreenAccent

@Composable
fun PermissionScreen(permissions: AppPermissions, onRefresh: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Text("CleanSpace", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "শুরু করতে নিচের দুটো permission দিতে হবে",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        PermissionCard(
            icon = Icons.Filled.Folder,
            title = "All Files Access",
            description = "Junk ও Large file স্ক্যান করার জন্য প্রয়োজন",
            granted = permissions.allFilesAccess,
            onClick = { context.startActivity(PermissionHelper.allFilesAccessIntent(context)) }
        )
        Spacer(Modifier.height(16.dp))
        PermissionCard(
            icon = Icons.Filled.Insights,
            title = "Usage Access",
            description = "App-wise storage তথ্য দেখার জন্য প্রয়োজন",
            granted = permissions.usageAccess,
            onClick = { context.startActivity(PermissionHelper.usageAccessIntent()) }
        )

        Spacer(Modifier.height(32.dp))
        Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
            Text("Permission চেক করো")
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "টিপস: System Settings পেজে CleanSpace-এর জন্য permission \"Allow\" করে ফিরে এসে \"চেক করো\" বাটনে ট্যাপ করো।",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    onClick: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(
                if (granted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (granted) GreenAccent else MaterialTheme.colorScheme.outline
            )
        }
    }
}
