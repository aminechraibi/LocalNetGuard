package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.LocalNetApp
import com.example.data.model.*
import com.example.engine.CertificateInfo
import com.example.engine.EvaluationResult
import com.example.engine.PacketInfo
import com.example.engine.RuleEngine
import com.example.server.LanWebServer
import com.example.vpn.LocalNetVpnService
import com.example.vpn.VpnStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val repository = LocalNetApp.instance.repository
    private val certificateManager = LocalNetApp.instance.certificateManager
    private val ruleEngine = RuleEngine()

    val vpnStatus: StateFlow<VpnStatus> = LocalNetVpnService.vpnStatus
    val appPolicies: StateFlow<List<AppPolicy>> = repository.appPolicies.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val customRules: StateFlow<List<FirewallRule>> = repository.customRules.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val recentConnections: StateFlow<List<ConnectionLog>> = repository.recentConnections.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val recentDnsLogs: StateFlow<List<DnsLog>> = repository.recentDnsLogs.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val blocklists: StateFlow<List<Blocklist>> = repository.blocklists.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val pairedDevices: StateFlow<List<PairedDevice>> = repository.pairedDevices.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val totalConnections: StateFlow<Long> = repository.totalConnectionsCount.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0L
    )
    val blockedConnections: StateFlow<Long> = repository.blockedConnectionsCount.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0L
    )
    val totalBytes: StateFlow<Long> = repository.totalBytesTransferred.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0L
    )

    val isLanServerEnabled: StateFlow<Boolean> = repository.settings.isLanServerEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )
    val lanServerPort: StateFlow<Int> = repository.settings.lanServerPort.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 8080
    )
    val isHttpsInspectionEnabled: StateFlow<Boolean> = repository.settings.isHttpsInspectionEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )
    val themeMode: StateFlow<String> = repository.settings.themeMode.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM"
    )

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            repository.settings.setThemeMode(mode)
        }
    }

    private val _ruleSimulationResult = MutableStateFlow<EvaluationResult?>(null)
    val ruleSimulationResult: StateFlow<EvaluationResult?> = _ruleSimulationResult.asStateFlow()

    private val _caCertInfo = MutableStateFlow<CertificateInfo?>(null)
    val caCertInfo: StateFlow<CertificateInfo?> = _caCertInfo.asStateFlow()

    init {
        loadCaCertInfo()
    }

    fun syncInstalledApps(context: Context) {
        viewModelScope.launch {
            repository.syncInstalledApps(context)
        }
    }

    fun toggleVpn(context: Context, prepareIntentNeeded: () -> Unit) {
        viewModelScope.launch {
            val current = vpnStatus.value
            if (current == VpnStatus.RUNNING) {
                repository.settings.setVpnEnabled(false)
                val intent = Intent(context, LocalNetVpnService::class.java).apply {
                    action = LocalNetVpnService.ACTION_STOP_VPN
                }
                context.startService(intent)
            } else {
                repository.settings.setVpnEnabled(true)
                val intent = Intent(context, LocalNetVpnService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }
    }

    fun updateAppPolicy(policy: AppPolicy) {
        viewModelScope.launch {
            repository.updateAppPolicy(policy)
        }
    }

    fun addRule(rule: FirewallRule) {
        viewModelScope.launch {
            repository.insertRule(rule)
        }
    }

    fun updateRule(rule: FirewallRule) {
        viewModelScope.launch {
            repository.updateRule(rule)
        }
    }

    fun deleteRule(id: Long) {
        viewModelScope.launch {
            repository.deleteRule(id)
        }
    }

    fun testRuleSimulation(packageName: String, domain: String, ip: String, port: Int) {
        val packet = PacketInfo(
            packageName = packageName,
            sourceIp = "10.1.10.1",
            sourcePort = 54321,
            destinationIp = ip.ifEmpty { "1.1.1.1" },
            destinationPort = port,
            domain = domain,
            protocol = "TCP"
        )
        val result = ruleEngine.evaluate(
            packet = packet,
            appPolicies = appPolicies.value.associateBy { it.packageName },
            customRules = customRules.value,
            blocklistDomains = setOf("ad.doubleclick.net", "telemetry.com")
        )
        _ruleSimulationResult.value = result
    }

    fun toggleLanServer(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            repository.settings.setLanServerEnabled(enabled)
            val app = LocalNetApp.instance
            if (enabled) {
                if (app.webServer == null) {
                    app.webServer = LanWebServer(context, lanServerPort.value)
                    app.webServer?.start()
                }
            } else {
                app.webServer?.stop()
                app.webServer = null
            }
        }
    }

    fun toggleHttpsInspection(enabled: Boolean) {
        viewModelScope.launch {
            repository.settings.setHttpsInspectionEnabled(enabled)
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    fun loadCaCertInfo() {
        _caCertInfo.value = certificateManager.generateOrGetLocalCaCertificate()
    }

    fun getExportablePem(): String {
        return certificateManager.getExportablePemCertificate()
    }
}
