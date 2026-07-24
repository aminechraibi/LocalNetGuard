package com.example.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.data.database.AppDatabase
import com.example.data.datastore.SettingsDataStore
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FirewallRepository(
    private val database: AppDatabase,
    val settings: SettingsDataStore
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    val appPolicies: Flow<List<AppPolicy>> = database.appPolicyDao().getAllAppPolicies()
    val customRules: Flow<List<FirewallRule>> = database.firewallRuleDao().getAllRules()
    val recentConnections: Flow<List<ConnectionLog>> = database.connectionLogDao().getRecentConnectionLogs(100)
    val recentDnsLogs: Flow<List<DnsLog>> = database.dnsLogDao().getRecentDnsLogs(100)
    val blocklists: Flow<List<Blocklist>> = database.blocklistDao().getAllBlocklists()
    val pairedDevices: Flow<List<PairedDevice>> = database.pairedDeviceDao().getAllPairedDevices()
    val auditLogs: Flow<List<AuditLog>> = database.auditLogDao().getRecentAuditLogs(100)

    val totalConnectionsCount: Flow<Long> = database.connectionLogDao().getTotalConnectionsCount()
    val blockedConnectionsCount: Flow<Long> = database.connectionLogDao().getBlockedConnectionsCount()
    val totalBytesTransferred: Flow<Long> = database.connectionLogDao().getTotalBytesTransferred().map { it ?: 0L }

    val totalDnsCount: Flow<Long> = database.dnsLogDao().getTotalDnsQueriesCount()
    val blockedDnsCount: Flow<Long> = database.dnsLogDao().getBlockedDnsQueriesCount()

    init {
        scope.launch {
            seedDefaultBlocklistsIfEmpty()
            seedSampleCustomRulesIfEmpty()
        }
    }

    suspend fun syncInstalledApps(context: Context) {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
        val policies = mutableListOf<AppPolicy>()

        for (pkg in packages) {
            val appInfo = pkg.applicationInfo ?: continue
            val appName = pm.getApplicationLabel(appInfo).toString()
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            policies.add(
                AppPolicy(
                    packageName = pkg.packageName,
                    appName = appName,
                    uid = appInfo.uid,
                    isSystemApp = isSystem
                )
            )
        }
        database.appPolicyDao().upsertPolicies(policies)
    }

    suspend fun updateAppPolicy(policy: AppPolicy) {
        database.appPolicyDao().upsertPolicy(policy)
        database.auditLogDao().insertAuditLog(
            AuditLog(
                event = "App Policy Updated",
                actor = "Local User",
                status = "SUCCESS",
                details = "Updated policy for ${policy.appName} (${policy.packageName})"
            )
        )
    }

    suspend fun insertRule(rule: FirewallRule) = database.firewallRuleDao().insertRule(rule)
    suspend fun updateRule(rule: FirewallRule) = database.firewallRuleDao().updateRule(rule)
    suspend fun deleteRule(id: Long) = database.firewallRuleDao().deleteRuleById(id)

    suspend fun logConnection(log: ConnectionLog) = database.connectionLogDao().insertLog(log)
    suspend fun logDnsQuery(log: DnsLog) = database.dnsLogDao().insertDnsLog(log)

    suspend fun clearLogs() {
        database.connectionLogDao().clearAllLogs()
        database.dnsLogDao().clearAllDnsLogs()
    }

    suspend fun insertBlocklist(blocklist: Blocklist) = database.blocklistDao().insertBlocklist(blocklist)
    suspend fun updateBlocklist(blocklist: Blocklist) = database.blocklistDao().updateBlocklist(blocklist)
    suspend fun deleteBlocklist(id: Long) = database.blocklistDao().deleteBlocklist(id)

    suspend fun addPairedDevice(device: PairedDevice) = database.pairedDeviceDao().upsertDevice(device)
    suspend fun removePairedDevice(id: String) = database.pairedDeviceDao().deleteDevice(id)

    private suspend fun seedDefaultBlocklistsIfEmpty() {
        val existing = database.blocklistDao().getEnabledBlocklists()
        if (existing.isEmpty()) {
            val defaults = listOf(
                Blocklist(
                    name = "StevenBlack Unified Hosts",
                    category = "Adware & Trackers",
                    url = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts",
                    isEnabled = true,
                    rulesCount = 125430,
                    isBuiltIn = true
                ),
                Blocklist(
                    name = "AdGuard DNS Filter",
                    category = "Privacy & Tracking",
                    url = "https://adguardteam.github.io/AdGuardSDNSFilter/Filters/filter.txt",
                    isEnabled = true,
                    rulesCount = 48200,
                    isBuiltIn = true
                ),
                Blocklist(
                    name = "Malware Domain List",
                    category = "Malware & Phishing",
                    url = "https://www.malwaredomainlist.com/hostslist/hosts.txt",
                    isEnabled = true,
                    rulesCount = 15300,
                    isBuiltIn = true
                )
            )
            for (b in defaults) {
                database.blocklistDao().insertBlocklist(b)
            }
        }
    }

    private suspend fun seedSampleCustomRulesIfEmpty() {
        val rules = database.firewallRuleDao().getEnabledRules()
        if (rules.isEmpty()) {
            database.firewallRuleDao().insertRule(
                FirewallRule(
                    name = "Block Known Telemetry",
                    type = RuleType.DOMAIN,
                    target = "*.telemetry.com",
                    action = RuleAction.BLOCK,
                    priority = 10,
                    isEnabled = true,
                    notes = "Blocks common background telemetry endpoints"
                )
            )
            database.firewallRuleDao().insertRule(
                FirewallRule(
                    name = "Block Unencrypted DNS (Port 53)",
                    type = RuleType.PORT,
                    target = "53",
                    action = RuleAction.LOG_ONLY,
                    priority = 50,
                    isEnabled = false,
                    notes = "Logs unencrypted UDP DNS traffic"
                )
            )
        }
    }
}
