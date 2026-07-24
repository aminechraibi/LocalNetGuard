package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val event: String,
    val actor: String, // e.g. "Local UI", "LAN Dashboard (192.168.1.15)"
    val status: String, // "SUCCESS", "FAILURE", "WARNING"
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
