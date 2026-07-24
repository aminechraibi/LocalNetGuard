package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class ConnectionStatus {
    ALLOWED,
    BLOCKED,
    INSPECTED,
    BYPASSED,
    FAILED
}

@Serializable
@Entity(tableName = "connection_logs")
data class ConnectionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val uid: Int,
    val sourceIp: String,
    val sourcePort: Int,
    val destinationIp: String,
    val destinationPort: Int,
    val domain: String = "",
    val protocol: String, // TCP, UDP, ICMP, DNS
    val status: ConnectionStatus,
    val matchedRule: String = "",
    val bytesSent: Long = 0,
    val bytesReceived: Long = 0,
    val httpMethod: String = "",
    val httpPath: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
