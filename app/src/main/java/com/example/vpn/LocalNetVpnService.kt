package com.example.vpn

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.LocalNetApp
import com.example.MainActivity
import com.example.R
import com.example.data.model.*
import com.example.engine.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

class LocalNetVpnService : VpnService() {

    private var tunFd: ParcelFileDescriptor? = null
    private var serviceJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val ruleEngine = RuleEngine()
    private val dnsResolver = DnsResolver()
    private val httpInspector = HttpInspector()

    private var appPoliciesMap = mapOf<String, AppPolicy>()
    private var customRulesList = listOf<FirewallRule>()
    private var blocklistsSet = setOf<String>("ad.doubleclick.net", "telemetry.com", "analytics.google.com")

    override fun onCreate() {
        super.onCreate()
        updateNotification("LocalNetGuard active - Filtering local traffic")

        scope.launch {
            LocalNetApp.instance.repository.appPolicies.collect { policies ->
                appPoliciesMap = policies.associateBy { it.packageName }
            }
        }
        scope.launch {
            LocalNetApp.instance.repository.customRules.collect { rules ->
                customRulesList = rules
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_VPN) {
            stopVpn()
            return START_NOT_STICKY
        }

        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        if (_vpnStatus.value == VpnStatus.RUNNING) return
        _vpnStatus.value = VpnStatus.STARTING

        try {
            val builder = Builder()
                .addAddress("10.1.10.1", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("10.1.10.1")
                .setMtu(1500)
                .setSession("LocalNetGuard Firewall")

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }

            tunFd = builder.establish()
            if (tunFd == null) {
                _vpnStatus.value = VpnStatus.ERROR
                return
            }

            _vpnStatus.value = VpnStatus.RUNNING
            startPacketLoop()
            updateNotification("Firewall active - Protecting network traffic")
        } catch (e: Exception) {
            e.printStackTrace()
            _vpnStatus.value = VpnStatus.ERROR
        }
    }

    private fun startPacketLoop() {
        serviceJob?.cancel()
        serviceJob = scope.launch {
            val fileDescriptor = tunFd?.fileDescriptor ?: return@launch
            val inputStream = FileInputStream(fileDescriptor)
            val outputStream = FileOutputStream(fileDescriptor)
            val buffer = ByteArray(32767)

            while (isActive && _vpnStatus.value == VpnStatus.RUNNING) {
                try {
                    val length = inputStream.read(buffer)
                    if (length > 0) {
                        processOutgoingPacket(buffer, length, outputStream)
                    }
                } catch (e: Exception) {
                    if (!isActive) break
                }
            }
        }
    }

    private suspend fun processOutgoingPacket(buffer: ByteArray, length: Int, tunOutput: FileOutputStream) {
        if (length < 20) return
        val version = (buffer[0].toInt() and 0xF0) shr 4
        if (version != 4) return // IPv4 for now

        val protocol = buffer[9].toInt() and 0xFF
        val srcIp = "${buffer[12].toInt() and 0xFF}.${buffer[13].toInt() and 0xFF}.${buffer[14].toInt() and 0xFF}.${buffer[15].toInt() and 0xFF}"
        val dstIp = "${buffer[16].toInt() and 0xFF}.${buffer[17].toInt() and 0xFF}.${buffer[18].toInt() and 0xFF}.${buffer[19].toInt() and 0xFF}"

        val ihl = (buffer[0].toInt() and 0x0F) * 4
        if (ihl >= length) return

        var srcPort = 0
        var dstPort = 0
        var protocolStr = "OTHER"

        if (protocol == 6) { // TCP
            protocolStr = "TCP"
            if (ihl + 4 <= length) {
                srcPort = ((buffer[ihl].toInt() and 0xFF) shl 8) or (buffer[ihl + 1].toInt() and 0xFF)
                dstPort = ((buffer[ihl + 2].toInt() and 0xFF) shl 8) or (buffer[ihl + 3].toInt() and 0xFF)
            }
        } else if (protocol == 17) { // UDP
            protocolStr = "UDP"
            if (ihl + 4 <= length) {
                srcPort = ((buffer[ihl].toInt() and 0xFF) shl 8) or (buffer[ihl + 1].toInt() and 0xFF)
                dstPort = ((buffer[ihl + 2].toInt() and 0xFF) shl 8) or (buffer[ihl + 3].toInt() and 0xFF)
            }
        } else if (protocol == 1) {
            protocolStr = "ICMP"
        }

        // Check if DNS query (UDP port 53 to local DNS 10.1.10.1 or external)
        var domainName = ""
        if (dstPort == 53 && protocolStr == "UDP") {
            val udpDataOffset = ihl + 8
            if (udpDataOffset < length) {
                val dnsData = ByteArray(length - udpDataOffset)
                System.arraycopy(buffer, udpDataOffset, dnsData, 0, dnsData.size)
                val dnsQuery = dnsResolver.parseQuery(dnsData, dnsData.size)
                if (dnsQuery != null) {
                    domainName = dnsQuery.domain
                }
            }
        }

        // SNI extraction for TLS ClientHello on port 443
        if (dstPort == 443 && protocolStr == "TCP") {
            val tcpDataOffset = ihl + 20
            if (tcpDataOffset < length) {
                val tlsData = ByteArray(length - tcpDataOffset)
                System.arraycopy(buffer, tcpDataOffset, tlsData, 0, tlsData.size)
                val extractedSni = httpInspector.extractSniFromTlsClientHello(tlsData)
                if (extractedSni != null) {
                    domainName = extractedSni
                }
            }
        }

        val packetInfo = PacketInfo(
            packageName = "App",
            sourceIp = srcIp,
            sourcePort = srcPort,
            destinationIp = dstIp,
            destinationPort = dstPort,
            domain = domainName,
            protocol = protocolStr,
            isWifi = true
        )

        val eval = ruleEngine.evaluate(
            packet = packetInfo,
            appPolicies = appPoliciesMap,
            customRules = customRulesList,
            blocklistDomains = blocklistsSet
        )

        val connectionStatus = if (eval.action == RuleAction.BLOCK) ConnectionStatus.BLOCKED else ConnectionStatus.ALLOWED

        // Log connection event
        LocalNetApp.instance.repository.logConnection(
            ConnectionLog(
                packageName = "System/App",
                appName = "App Network Activity",
                uid = 1000,
                sourceIp = srcIp,
                sourcePort = srcPort,
                destinationIp = dstIp,
                destinationPort = dstPort,
                domain = domainName,
                protocol = protocolStr,
                status = connectionStatus,
                matchedRule = eval.reason,
                bytesSent = length.toLong()
            )
        )

        if (domainName.isNotEmpty()) {
            LocalNetApp.instance.repository.logDnsQuery(
                DnsLog(
                    domain = domainName,
                    resolvedIp = dstIp,
                    isBlocked = connectionStatus == ConnectionStatus.BLOCKED,
                    blockReason = eval.reason
                )
            )
        }
    }

    private fun stopVpn() {
        _vpnStatus.value = VpnStatus.STOPPING
        serviceJob?.cancel()
        try {
            tunFd?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        tunFd = null
        _vpnStatus.value = VpnStatus.STOPPED
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateNotification(content: String) {
        val stopIntent = Intent(this, LocalNetVpnService::class.java).apply {
            action = ACTION_STOP_VPN
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, LocalNetApp.VPN_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("LocalNetGuard Firewall")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(mainPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Stop Firewall", stopPendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob?.cancel()
        scope.cancel()
    }

    companion object {
        const val ACTION_STOP_VPN = "com.example.action.STOP_VPN"
        const val NOTIFICATION_ID = 1001

        private val _vpnStatus = MutableStateFlow(VpnStatus.STOPPED)
        val vpnStatus: StateFlow<VpnStatus> = _vpnStatus.asStateFlow()

        private val _throughput = MutableStateFlow(VpnThroughput())
        val throughput: StateFlow<VpnThroughput> = _throughput.asStateFlow()
    }
}
