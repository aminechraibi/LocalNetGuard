package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import com.example.vpn.VpnStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToLanServer: () -> Unit,
    onNavigateToHttps: () -> Unit,
    onNavigateToRules: () -> Unit
) {
    val context = LocalContext.current
    val vpnStatus by viewModel.vpnStatus.collectAsState()
    val totalConnections by viewModel.totalConnections.collectAsState()
    val blockedConnections by viewModel.blockedConnections.collectAsState()
    val totalBytes by viewModel.totalBytes.collectAsState()
    val isLanEnabled by viewModel.isLanServerEnabled.collectAsState()
    val isHttpsEnabled by viewModel.isHttpsInspectionEnabled.collectAsState()

    val isRunning = vpnStatus == VpnStatus.RUNNING

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "App Logo",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "LocalNetGuard",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Master VPN Switch Hero Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vpn_master_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRunning) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (isRunning) PolishEmerald else MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isRunning) AccentGreen.copy(alpha = 0.2f)
                                    else AccentRed.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.CheckCircle else Icons.Default.PowerSettingsNew,
                                contentDescription = "VPN Power",
                                tint = if (isRunning) AccentGreen else AccentRed,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (isRunning) "Local Firewall Active" else "Firewall Service Stopped",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isRunning) PolishEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = if (isRunning) "All device packets & DNS requests are filtered locally" else "Tap below to enable local packet protection",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.toggleVpn(context) {} },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("toggle_vpn_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRunning) AccentRed else PrimaryBlue
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRunning) "STOP FIREWALL" else "START FIREWALL VPN",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Real-Time Network Metrics Grid
            item {
                Text(
                    text = "Live Protection Metrics",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Processed",
                        value = "$totalConnections",
                        icon = Icons.Default.SwapVert,
                        color = PrimaryBlue,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Blocked",
                        value = "$blockedConnections",
                        icon = Icons.Default.Block,
                        color = AccentRed,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Transferred",
                        value = "${totalBytes / (1024 * 1024)} MB",
                        icon = Icons.Default.DataUsage,
                        color = AccentGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Module Action Cards
            item {
                Text(
                    text = "Security Subsystems",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextSecondaryDark,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                SubsystemCard(
                    title = "Remote LAN Dashboard",
                    subtitle = if (isLanEnabled) "Server running on port 8080" else "Disabled - Local LAN remote access off",
                    isActive = isLanEnabled,
                    icon = Icons.Default.Lan,
                    onClick = onNavigateToLanServer
                )

                Spacer(modifier = Modifier.height(10.dp))

                SubsystemCard(
                    title = "HTTPS MITM Inspection",
                    subtitle = if (isHttpsEnabled) "Active - Inspecting compatible HTTPS ClientHello" else "Disabled - Metadata SNI filter mode",
                    isActive = isHttpsEnabled,
                    icon = Icons.Default.Lock,
                    onClick = onNavigateToHttps
                )

                Spacer(modifier = Modifier.height(10.dp))

                SubsystemCard(
                    title = "Custom Firewall Rules & Tester",
                    subtitle = "Evaluate domain, IP, and port policies in real-time",
                    isActive = true,
                    icon = Icons.Default.Rule,
                    onClick = onNavigateToRules
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SubsystemCard(
    title: String,
    subtitle: String,
    isActive: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
