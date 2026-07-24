package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "blocklists")
data class Blocklist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String, // Adware, Trackers, Telemetry, Malware, Custom
    val url: String,
    val isEnabled: Boolean = true,
    val rulesCount: Int = 0,
    val lastUpdated: Long = 0,
    val isBuiltIn: Boolean = false
)
