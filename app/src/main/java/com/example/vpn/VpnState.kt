package com.example.vpn

enum class VpnStatus {
    STOPPED,
    PREPARING,
    STARTING,
    RUNNING,
    PAUSED,
    STOPPING,
    ERROR
}

data class VpnThroughput(
    val uploadSpeedBytesPerSec: Long = 0,
    val downloadSpeedBytesPerSec: Long = 0,
    val totalUploadBytes: Long = 0,
    val totalDownloadBytes: Long = 0,
    val activeConnectionsCount: Int = 0,
    val totalBlockedCount: Long = 0
)
