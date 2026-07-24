package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "app_policies")
data class AppPolicy(
    @PrimaryKey val packageName: String,
    val appName: String,
    val uid: Int,
    val isSystemApp: Boolean = false,
    val isInternetBlocked: Boolean = false,
    val isWifiBlocked: Boolean = false,
    val isMobileBlocked: Boolean = false,
    val isBackgroundBlocked: Boolean = false,
    val isHttpsInspectionBypassed: Boolean = false,
    val isQuicBlocked: Boolean = false,
    val customNotes: String = ""
)
