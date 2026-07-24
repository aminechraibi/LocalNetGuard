package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.*

@Database(
    entities = [
        AppPolicy::class,
        FirewallRule::class,
        ConnectionLog::class,
        DnsLog::class,
        Blocklist::class,
        PairedDevice::class,
        AuditLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appPolicyDao(): AppPolicyDao
    abstract fun firewallRuleDao(): FirewallRuleDao
    abstract fun connectionLogDao(): ConnectionLogDao
    abstract fun dnsLogDao(): DnsLogDao
    abstract fun blocklistDao(): BlocklistDao
    abstract fun pairedDeviceDao(): PairedDeviceDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "localnet_guard.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
