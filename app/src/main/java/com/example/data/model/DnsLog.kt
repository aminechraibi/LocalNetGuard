package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "dns_logs")
data class DnsLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val queryType: String = "A", // A, AAAA, CNAME, HTTPS
    val resolvedIp: String = "",
    val isBlocked: Boolean = false,
    val blockReason: String = "", // e.g. "Adware Blocklist", "Custom Domain Rule"
    val upstream: String = "System",
    val latencyMs: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)
