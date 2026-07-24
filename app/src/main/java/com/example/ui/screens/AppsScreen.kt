package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppPolicy
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentRed
import com.example.ui.theme.PolishEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val apps by viewModel.appPolicies.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var filterSystemApps by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (apps.isEmpty()) {
            viewModel.syncInstalledApps(context)
        }
    }

    val filteredApps = remember(apps, searchQuery, filterSystemApps) {
        apps.filter {
            (searchQuery.isEmpty() || it.appName.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true)) &&
            (!filterSystemApps || !it.isSystemApp)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Per-App Network Rules", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.syncInstalledApps(context) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync Apps")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search application or package...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredApps.size} Applications",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                FilterChip(
                    selected = filterSystemApps,
                    onClick = { filterSystemApps = !filterSystemApps },
                    label = { Text("Hide System Apps") },
                    shape = RoundedCornerShape(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No applications found.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { appPolicy ->
                        AppPolicyCard(
                            policy = appPolicy,
                            onUpdatePolicy = { updated -> viewModel.updateAppPolicy(updated) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppPolicyCard(
    policy: AppPolicy,
    onUpdatePolicy: (AppPolicy) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (policy.isSystemApp) Icons.Default.Android else Icons.Default.Apps,
                    contentDescription = null,
                    tint = if (policy.isSystemApp) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = policy.appName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = policy.packageName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Global Block Toggle
                Switch(
                    checked = !policy.isInternetBlocked,
                    onCheckedChange = { isAllowed ->
                        onUpdatePolicy(policy.copy(isInternetBlocked = !isAllowed))
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Granular controls row with horizontal scroll
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ControlIconButton(
                    icon = Icons.Default.Wifi,
                    label = "Wi-Fi",
                    isBlocked = policy.isWifiBlocked,
                    onClick = { onUpdatePolicy(policy.copy(isWifiBlocked = !policy.isWifiBlocked)) }
                )
                ControlIconButton(
                    icon = Icons.Default.SignalCellular4Bar,
                    label = "Data",
                    isBlocked = policy.isMobileBlocked,
                    onClick = { onUpdatePolicy(policy.copy(isMobileBlocked = !policy.isMobileBlocked)) }
                )
                ControlIconButton(
                    icon = Icons.Default.Speed,
                    label = "QUIC Block",
                    isBlocked = policy.isQuicBlocked,
                    onClick = { onUpdatePolicy(policy.copy(isQuicBlocked = !policy.isQuicBlocked)) }
                )
            }
        }
    }
}

@Composable
fun ControlIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isBlocked: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = !isBlocked,
        onClick = onClick,
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isBlocked) AccentRed else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        },
        label = {
            Text(
                text = if (isBlocked) "Blocked" else label,
                fontSize = 12.sp,
                color = if (isBlocked) AccentRed else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        },
        shape = RoundedCornerShape(20.dp)
    )
}

