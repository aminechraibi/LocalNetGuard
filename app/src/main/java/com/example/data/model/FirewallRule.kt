package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class RuleType {
    APPLICATION,
    DOMAIN,
    IP_CIDR,
    PORT
}

enum class RuleAction {
    ALLOW,
    BLOCK,
    LOG_ONLY
}

@Serializable
@Entity(tableName = "firewall_rules")
data class FirewallRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: RuleType,
    val target: String, // Package name, domain pattern (e.g., *.tracker.com), IP/CIDR (e.g. 192.168.1.0/24), or Port (e.g. 443)
    val action: RuleAction,
    val priority: Int = 100, // Lower number = higher priority
    val isEnabled: Boolean = true,
    val isWifiOnly: Boolean = false,
    val isMobileOnly: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
