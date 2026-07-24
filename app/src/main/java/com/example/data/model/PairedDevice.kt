package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "paired_devices")
data class PairedDevice(
    @PrimaryKey val id: String, // Session Token or Device UUID
    val clientName: String,
    val clientIp: String,
    val pairedAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis(),
    val isApproved: Boolean = true
)
