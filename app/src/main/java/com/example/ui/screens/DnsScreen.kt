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
import com.example.data.model.Blocklist
import com.example.data.model.DnsLog
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsScreen(viewModel: MainViewModel) {
    val blocklists by viewModel.blocklists.collectAsState()
    val dnsLogs by viewModel.recentDnsLogs.collectAsState()

    var selectedUpstream by remember { mutableStateOf("Cloudflare DoH (1.1.1.1)") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DNS & Sinkhole Subsystem", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.clearLogs() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear DNS Logs")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Upstream Resolver Config Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Upstream Encrypted DNS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Upstream Provider: $selectedUpstream",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                selected = selectedUpstream.contains("Cloudflare"),
                                onClick = { selectedUpstream = "Cloudflare DoH (1.1.1.1)" },
                                label = { Text("Cloudflare DoH") },
                                shape = RoundedCornerShape(20.dp)
                            )
                            FilterChip(
                                selected = selectedUpstream.contains("Google"),
                                onClick = { selectedUpstream = "Google DNS (8.8.8.8)" },
                                label = { Text("Google 8.8.8.8") },
                                shape = RoundedCornerShape(20.dp)
                            )
                            FilterChip(
                                selected = selectedUpstream.contains("Quad9"),
                                onClick = { selectedUpstream = "Quad9 Secure (9.9.9.9)" },
                                label = { Text("Quad9") },
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }
                }
            }

            // Blocklists Section
            item {
                Text(
                    text = "Active DNS Sinkhole Blocklists",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            items(blocklists, key = { it.id }) { list ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = list.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${list.category} • ${list.rulesCount} rules",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = list.isEnabled,
                            onCheckedChange = {}
                        )
                    }
                }
            }

            // Live DNS Queries feed
            item {
                Text(
                    text = "Recent DNS Sinkhole Log (${dnsLogs.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            if (dnsLogs.isEmpty()) {
                item {
                    Text(
                        text = "No DNS queries logged yet. Active DNS queries will appear here.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(dnsLogs.take(20), key = { it.id }) { log ->
                    DnsLogRow(log = log)
                }
            }
        }
    }
}

@Composable
fun DnsLogRow(log: DnsLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(
                        if (log.isBlocked) AccentRed.copy(alpha = 0.15f) else PolishEmerald.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (log.isBlocked) "BLOCKED" else "RESOLVED",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (log.isBlocked) AccentRed else PolishEmerald
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.domain,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${log.queryType} -> ${log.resolvedIp} (${log.blockReason})",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

