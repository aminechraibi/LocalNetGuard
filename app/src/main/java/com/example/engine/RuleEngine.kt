package com.example.engine

import com.example.data.model.*

data class PacketInfo(
    val packageName: String = "",
    val uid: Int = -1,
    val sourceIp: String,
    val sourcePort: Int,
    val destinationIp: String,
    val destinationPort: Int,
    val domain: String = "",
    val protocol: String = "TCP",
    val isWifi: Boolean = true
)

data class EvaluationResult(
    val action: RuleAction,
    val matchedRuleName: String,
    val reason: String,
    val matchedRuleId: Long? = null
)

class RuleEngine {

    fun evaluate(
        packet: PacketInfo,
        appPolicies: Map<String, AppPolicy>,
        customRules: List<FirewallRule>,
        blocklistDomains: Set<String>,
        globalDefaultAction: RuleAction = RuleAction.ALLOW
    ): EvaluationResult {

        // 1. Emergency Safety / Local Loop Protection
        if (packet.packageName == "com.aistudio.localnetguard.app" || packet.packageName == "com.example") {
            return EvaluationResult(RuleAction.ALLOW, "System Safety", "Self traffic allowed to prevent loops")
        }

        // 2. Per-App Policy Evaluation
        val appPolicy = appPolicies[packet.packageName]
        if (appPolicy != null) {
            if (appPolicy.isInternetBlocked) {
                return EvaluationResult(RuleAction.BLOCK, "App Policy: Internet Blocked", "Internet Blocked: Entire internet access revoked for ${appPolicy.appName}")
            }
            if (packet.isWifi && appPolicy.isWifiBlocked) {
                return EvaluationResult(RuleAction.BLOCK, "App Policy: Wi-Fi Blocked", "Wi-Fi access disabled for ${appPolicy.appName}")
            }
            if (!packet.isWifi && appPolicy.isMobileBlocked) {
                return EvaluationResult(RuleAction.BLOCK, "App Policy: Mobile Blocked", "Mobile data access disabled for ${appPolicy.appName}")
            }
            if (appPolicy.isQuicBlocked && packet.protocol == "UDP" && packet.destinationPort == 443) {
                return EvaluationResult(RuleAction.BLOCK, "App Policy: QUIC Blocked", "UDP port 443 blocked to encourage TCP fallback")
            }
        }

        // 3. Custom Firewall Rules Evaluation (Sorted by Priority)
        for (rule in customRules) {
            if (!rule.isEnabled) continue

            // Network filter
            if (rule.isWifiOnly && !packet.isWifi) continue
            if (rule.isMobileOnly && packet.isWifi) continue

            val isMatch = when (rule.type) {
                RuleType.APPLICATION -> {
                    rule.target.equals(packet.packageName, ignoreCase = true)
                }
                RuleType.DOMAIN -> {
                    matchesDomainPattern(packet.domain, rule.target)
                }
                RuleType.IP_CIDR -> {
                    matchesIpCidr(packet.destinationIp, rule.target)
                }
                RuleType.PORT -> {
                    rule.target.toIntOrNull() == packet.destinationPort
                }
            }

            if (isMatch) {
                return EvaluationResult(
                    action = rule.action,
                    matchedRuleName = rule.name,
                    reason = "Matched rule '${rule.name}' [Target: ${rule.target}, Type: ${rule.type}]",
                    matchedRuleId = rule.id
                )
            }
        }

        // 4. DNS Blocklists Evaluation
        if (packet.domain.isNotEmpty()) {
            val normalizedDomain = packet.domain.lowercase().trim()
            if (blocklistDomains.contains(normalizedDomain)) {
                return EvaluationResult(RuleAction.BLOCK, "DNS Blocklist Sinkhole", "Domain '$normalizedDomain' is in active blocklist")
            }
            // Check domain suffixes
            val parts = normalizedDomain.split(".")
            for (i in 1 until parts.size - 1) {
                val parentDomain = parts.subList(i, parts.size).joinToString(".")
                if (blocklistDomains.contains(parentDomain)) {
                    return EvaluationResult(RuleAction.BLOCK, "DNS Blocklist Subdomain", "Parent domain '$parentDomain' in active blocklist")
                }
            }
        }

        // 5. Global Default Fallback
        return EvaluationResult(globalDefaultAction, "Global Default Policy", "Default policy applied ($globalDefaultAction)")
    }

    private fun matchesDomainPattern(domain: String, pattern: String): Boolean {
        if (domain.isEmpty() || pattern.isEmpty()) return false
        val cleanDomain = domain.lowercase().trim()
        val cleanPattern = pattern.lowercase().trim()

        if (cleanPattern.startsWith("*.")) {
            val suffix = cleanPattern.substring(2)
            return cleanDomain.endsWith(suffix) || cleanDomain == suffix
        }
        return cleanDomain == cleanPattern
    }

    private fun matchesIpCidr(ip: String, cidrTarget: String): Boolean {
        if (ip.isEmpty() || cidrTarget.isEmpty()) return false
        if (cidrTarget == ip) return true
        // Basic CIDR prefix match
        if (cidrTarget.contains("/")) {
            val parts = cidrTarget.split("/")
            val prefix = parts[0].trim()
            val maskBits = parts[1].toIntOrNull() ?: 32
            // Compare first N octets or characters
            if (maskBits >= 24) {
                val ipOctets = ip.split(".")
                val prefixOctets = prefix.split(".")
                if (ipOctets.size >= 3 && prefixOctets.size >= 3) {
                    return ipOctets[0] == prefixOctets[0] && ipOctets[1] == prefixOctets[1] && ipOctets[2] == prefixOctets[2]
                }
            } else if (maskBits >= 16) {
                val ipOctets = ip.split(".")
                val prefixOctets = prefix.split(".")
                if (ipOctets.size >= 2 && prefixOctets.size >= 2) {
                    return ipOctets[0] == prefixOctets[0] && ipOctets[1] == prefixOctets[1]
                }
            }
        }
        return false
    }
}
