package com.example

import com.example.data.model.*
import com.example.engine.*
import org.junit.Assert.*
import org.junit.Test

class LocalNetGuardUnitTests {

    private val ruleEngine = RuleEngine()
    private val httpInspector = HttpInspector()
    private val dnsResolver = DnsResolver()

    @Test
    fun ruleEngine_blocksAppWhenInternetAccessRevoked() {
        val appPolicies = mapOf(
            "com.bad.tracker" to AppPolicy(
                packageName = "com.bad.tracker",
                appName = "Bad App",
                uid = 10050,
                isInternetBlocked = true
            )
        )

        val packet = PacketInfo(
            packageName = "com.bad.tracker",
            sourceIp = "10.1.10.1",
            sourcePort = 12345,
            destinationIp = "1.1.1.1",
            destinationPort = 443,
            domain = "example.com"
        )

        val result = ruleEngine.evaluate(packet, appPolicies, emptyList(), emptySet())

        assertEquals(RuleAction.BLOCK, result.action)
        assertTrue(result.reason.contains("Internet Blocked"))
    }

    @Test
    fun ruleEngine_matchesWildcardDomainRule() {
        val rules = listOf(
            FirewallRule(
                name = "Block Adware Wildcard",
                type = RuleType.DOMAIN,
                target = "*.doubleclick.net",
                action = RuleAction.BLOCK,
                priority = 10
            )
        )

        val packet = PacketInfo(
            packageName = "com.browser",
            sourceIp = "10.1.10.1",
            sourcePort = 54321,
            destinationIp = "172.217.1.1",
            destinationPort = 443,
            domain = "ad.doubleclick.net"
        )

        val result = ruleEngine.evaluate(packet, emptyMap(), rules, emptySet())

        assertEquals(RuleAction.BLOCK, result.action)
        assertEquals("Block Adware Wildcard", result.matchedRuleName)
    }

    @Test
    fun ruleEngine_blocksDomainInDnsBlocklist() {
        val blocklists = setOf("malware.badsite.org", "analytics.google.com")

        val packet = PacketInfo(
            packageName = "com.example.app",
            sourceIp = "10.1.10.1",
            sourcePort = 30000,
            destinationIp = "93.184.216.34",
            destinationPort = 80,
            domain = "malware.badsite.org"
        )

        val result = ruleEngine.evaluate(packet, emptyMap(), emptyList(), blocklists)

        assertEquals(RuleAction.BLOCK, result.action)
        assertTrue(result.reason.contains("active blocklist"))
    }

    @Test
    fun httpInspector_redactsSensitiveHeaders() {
        val rawHttpRequest = """
            GET /api/user/profile HTTP/1.1
            Host: api.service.com
            Authorization: Bearer secret_jwt_token_12345
            Cookie: session_id=abcdef987654321
            User-Agent: LocalNetGuardTest
            
        """.trimIndent()

        val info = httpInspector.inspectHttpRequest(rawHttpRequest.toByteArray(Charsets.UTF_8))

        assertNotNull(info)
        assertEquals("GET", info?.method)
        assertEquals("/api/user/profile", info?.path)
        assertEquals("api.service.com", info?.host)
        assertEquals("[REDACTED SECRET]", info?.headers?.get("Authorization"))
        assertEquals("[REDACTED SECRET]", info?.headers?.get("Cookie"))
        assertEquals("LocalNetGuardTest", info?.headers?.get("User-Agent"))
    }

    @Test
    fun dnsResolver_parsesDnsQueryPacket() {
        // Construct basic DNS Query for "example.com" (Type A)
        val dummyQueryBytes = byteArrayOf(
            0x12, 0x34, // ID: 0x1234
            0x01, 0x00, // Standard Query flags
            0x00, 0x01, // QDCOUNT = 1
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x07, 'e'.code.toByte(), 'x'.code.toByte(), 'a'.code.toByte(), 'm'.code.toByte(), 'p'.code.toByte(), 'l'.code.toByte(), 'e'.code.toByte(),
            0x03, 'c'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte(),
            0x00, // Null byte terminating domain name
            0x00, 0x01, // Type A
            0x00, 0x01  // Class IN
        )

        val query = dnsResolver.parseQuery(dummyQueryBytes, dummyQueryBytes.size)

        assertNotNull(query)
        assertEquals("example.com", query?.domain)
        assertEquals("A", query?.queryType)
        assertEquals(0x1234, query?.transactionId)
    }

    @Test
    fun dnsResolver_buildsSinkholeResponse() {
        val dummyQuery = DnsQuery(
            domain = "ad.tracker.com",
            queryType = "A",
            transactionId = 0x5678,
            rawPacket = ByteArray(32)
        )

        val result = dnsResolver.buildSinkholeResponse(dummyQuery, "Adware Blocklist")

        assertTrue(result.isBlocked)
        assertEquals("0.0.0.0 (Sinkhole)", result.resolvedIp)
        assertEquals("Adware Blocklist", result.blockReason)
        assertNotNull(result.responseBytes)
    }
}
