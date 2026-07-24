package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val totalConnections by viewModel.totalConnections.collectAsState()
    val blockedConnections by viewModel.blockedConnections.collectAsState()
    val totalBytes by viewModel.totalBytes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Network Traffic Analytics", fontWeight = FontWeight.Bold) })
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
            // Analytics Summary Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Processed",
                        value = "$totalConnections",
                        icon = Icons.Default.SwapVert,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Blocked",
                        value = "$blockedConnections",
                        icon = Icons.Default.Block,
                        color = AccentRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Visual Traffic Breakdown Bar Chart
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
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "Traffic Ratio Breakdown",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        val total = (totalConnections).coerceAtLeast(1)
                        val allowedRatio = ((totalConnections - blockedConnections).toFloat() / total).coerceIn(0f, 1f)
                        val blockedRatio = (blockedConnections.toFloat() / total).coerceIn(0f, 1f)

                        // Dual Bar Indicator
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp)
                                )
                        ) {
                            if (allowedRatio > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(allowedRatio)
                                        .background(PolishEmerald, shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                                )
                            }
                            if (blockedRatio > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(blockedRatio)
                                        .background(AccentRed, shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🟢 Allowed: ${"%.1f".format(allowedRatio * 100)}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "🔴 Blocked: ${"%.1f".format(blockedRatio * 100)}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Export Actions
            item {
                Button(
                    onClick = {
                        Toast.makeText(context, "Exported Network Logs to /sdcard/Download/LocalNetGuard_logs.json", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("EXPORT AUDIT LOGS (JSON / CSV)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

