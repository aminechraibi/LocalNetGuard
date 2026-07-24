package com.example.data.database

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppPolicyDao {
    @Query("SELECT * FROM app_policies ORDER BY appName ASC")
    fun getAllAppPolicies(): Flow<List<AppPolicy>>

    @Query("SELECT * FROM app_policies WHERE packageName = :packageName LIMIT 1")
    suspend fun getPolicyForPackage(packageName: String): AppPolicy?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPolicy(policy: AppPolicy)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPolicies(policies: List<AppPolicy>)

    @Query("DELETE FROM app_policies WHERE packageName = :packageName")
    suspend fun deletePolicy(packageName: String)
}

@Dao
interface FirewallRuleDao {
    @Query("SELECT * FROM firewall_rules ORDER BY priority ASC, id DESC")
    fun getAllRules(): Flow<List<FirewallRule>>

    @Query("SELECT * FROM firewall_rules WHERE isEnabled = 1 ORDER BY priority ASC")
    suspend fun getEnabledRules(): List<FirewallRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: FirewallRule): Long

    @Update
    suspend fun updateRule(rule: FirewallRule)

    @Query("DELETE FROM firewall_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)

    @Query("DELETE FROM firewall_rules")
    suspend fun deleteAllRules()
}

@Dao
interface ConnectionLogDao {
    @Query("SELECT * FROM connection_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentConnectionLogs(limit: Int = 100): Flow<List<ConnectionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ConnectionLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<ConnectionLog>)

    @Query("SELECT COUNT(*) FROM connection_logs")
    fun getTotalConnectionsCount(): Flow<Long>

    @Query("SELECT COUNT(*) FROM connection_logs WHERE status = 'BLOCKED'")
    fun getBlockedConnectionsCount(): Flow<Long>

    @Query("SELECT SUM(bytesSent + bytesReceived) FROM connection_logs")
    fun getTotalBytesTransferred(): Flow<Long?>

    @Query("DELETE FROM connection_logs WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteLogsOlderThan(cutoffTimestamp: Long)

    @Query("DELETE FROM connection_logs")
    suspend fun clearAllLogs()
}

@Dao
interface DnsLogDao {
    @Query("SELECT * FROM dns_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentDnsLogs(limit: Int = 100): Flow<List<DnsLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDnsLog(log: DnsLog)

    @Query("SELECT COUNT(*) FROM dns_logs")
    fun getTotalDnsQueriesCount(): Flow<Long>

    @Query("SELECT COUNT(*) FROM dns_logs WHERE isBlocked = 1")
    fun getBlockedDnsQueriesCount(): Flow<Long>

    @Query("DELETE FROM dns_logs WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteDnsLogsOlderThan(cutoffTimestamp: Long)

    @Query("DELETE FROM dns_logs")
    suspend fun clearAllDnsLogs()
}

@Dao
interface BlocklistDao {
    @Query("SELECT * FROM blocklists ORDER BY isBuiltIn DESC, name ASC")
    fun getAllBlocklists(): Flow<List<Blocklist>>

    @Query("SELECT * FROM blocklists WHERE isEnabled = 1")
    suspend fun getEnabledBlocklists(): List<Blocklist>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlocklist(blocklist: Blocklist): Long

    @Update
    suspend fun updateBlocklist(blocklist: Blocklist)

    @Query("DELETE FROM blocklists WHERE id = :id")
    suspend fun deleteBlocklist(id: Long)
}

@Dao
interface PairedDeviceDao {
    @Query("SELECT * FROM paired_devices ORDER BY lastActiveAt DESC")
    fun getAllPairedDevices(): Flow<List<PairedDevice>>

    @Query("SELECT * FROM paired_devices WHERE id = :sessionId LIMIT 1")
    suspend fun getDeviceById(sessionId: String): PairedDevice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDevice(device: PairedDevice)

    @Query("DELETE FROM paired_devices WHERE id = :sessionId")
    suspend fun deleteDevice(sessionId: String)

    @Query("DELETE FROM paired_devices")
    suspend fun deleteAllDevices()
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentAuditLogs(limit: Int = 100): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog)

    @Query("DELETE FROM audit_logs")
    suspend fun clearAllAuditLogs()
}
