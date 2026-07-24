package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Overview", Icons.Default.Shield)
    object Apps : Screen("apps", "Apps", Icons.Default.Apps)
    object Rules : Screen("rules", "Firewall", Icons.Default.Rule)
    object Dns : Screen("dns", "DNS", Icons.Default.Dns)
    object LiveConnections : Screen("live", "Live", Icons.Default.ReceiptLong)
    object HttpsInspection : Screen("https", "HTTPS", Icons.Default.Lock)
    object LanServer : Screen("lan", "LAN", Icons.Default.Devices)
    object Analytics : Screen("analytics", "Analytics", Icons.Default.BarChart)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}
