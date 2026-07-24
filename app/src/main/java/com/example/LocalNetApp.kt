package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.database.AppDatabase
import com.example.data.datastore.SettingsDataStore
import com.example.data.repository.FirewallRepository
import com.example.engine.CertificateManager
import com.example.server.LanWebServer

class LocalNetApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var settingsDataStore: SettingsDataStore
        private set

    lateinit var repository: FirewallRepository
        private set

    lateinit var certificateManager: CertificateManager
        private set

    var webServer: LanWebServer? = null

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getDatabase(this)
        settingsDataStore = SettingsDataStore(this)
        certificateManager = CertificateManager(this)
        repository = FirewallRepository(database, settingsDataStore)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vpnChannel = NotificationChannel(
                VPN_NOTIFICATION_CHANNEL_ID,
                "LocalNetGuard VPN Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active firewall & local VPN status"
            }

            val lanChannel = NotificationChannel(
                LAN_NOTIFICATION_CHANNEL_ID,
                "LAN Management Dashboard",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for remote dashboard pairing and management requests"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(vpnChannel)
            notificationManager.createNotificationChannel(lanChannel)
        }
    }

    companion object {
        const val VPN_NOTIFICATION_CHANNEL_ID = "localnet_vpn_channel"
        const val LAN_NOTIFICATION_CHANNEL_ID = "localnet_lan_channel"

        lateinit var instance: LocalNetApp
            private set
    }
}
