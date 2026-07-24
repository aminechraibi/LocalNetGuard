package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConnectionLog
import com.example.data.model.ConnectionStatus
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveConnectionsScreen(viewModel: MainViewModel) {
    val connections by viewModel.recentConnections.collectAsState()
    var filterType by remember { mutableStateOf("ALL") } // ALL, ALLOWED, BLOCKED, DNS
    var searchQuery by remember { mutableStateOf("") }

    val filteredList = remember(connections, filterType, searchQuery) {
        connections.filter { log ->
            val matchesFilter = when (filterType) {
                "ALLOWED" -> log.status == ConnectionStatus.ALLOWED
                "BLOCKED" -> log.status == ConnectionStatus.BLOCKED
                "DNS" -> log.protocol == "DNS" || log.destinationPort == 53
                else -> true
            }
            val matchesSearch = searchQuery.isEmpty() ||
                    log.packageName.contains(searchQuery, ignoreCase = true) ||
                    log.domain.contains(searchQuery, ignoreCase = true) ||
                    log.destinationIp.contains(searchQuery)
            matchesFilter && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Socket Connection Feed", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.clearLogs() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Logs")
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Filter domain, IP, or package...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = filterType == "ALL",
                    onClick = { filterType = "ALL" },
                    label = { Text("All (${connections.size})") },
                    shape = RoundedCornerShape(20.dp)
                )
                FilterChip(
                    selected = filterType == "ALLOWED",
                    onClick = { filterType = "ALLOWED" },
                    label = { Text("Allowed") },
                    shape = RoundedCornerShape(20.dp)
                )
                FilterChip(
                    selected = filterType == "BLOCKED",
                    onClick = { filterType = "BLOCKED" },
                    label = { Text("Blocked") },
                    shape = RoundedCornerShape(20.dp)
                )
                FilterChip(
                    selected = filterType == "DNS",
                    onClick = { filterType = "DNS" },
                    label = { Text("DNS") },
                    shape = RoundedCornerShape(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No connections logged matching filter.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        ConnectionLogRow(log = item)
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionLogRow(log: ConnectionLog) {
    val isBlocked = log.status == ConnectionStatus.BLOCKED

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            if (isBlocked) AccentRed.copy(alpha = 0.15f) else PolishEmerald.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = log.status.name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isBlocked) AccentRed else PolishEmerald
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = log.protocol,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "${log.bytesSent} bytes",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (log.domain.isNotEmpty()) log.domain else "${log.destinationIp}:${log.destinationPort}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Src: ${log.sourceIp}:${log.sourcePort} -> Dst: ${log.destinationIp}:${log.destinationPort}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Matched: ${log.matchedRule}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

