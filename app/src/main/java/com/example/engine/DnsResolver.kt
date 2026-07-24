package com.example.engine

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

data class DnsQuery(
    val domain: String,
    val queryType: String,
    val transactionId: Int,
    val rawPacket: ByteArray
)

data class DnsResult(
    val domain: String,
    val resolvedIp: String,
    val isBlocked: Boolean,
    val blockReason: String,
    val responseBytes: ByteArray?
)

class DnsResolver {

    fun parseQuery(buffer: ByteArray, length: Int): DnsQuery? {
        if (length < 12) return null
        val bb = ByteBuffer.wrap(buffer, 0, length)
        val id = bb.short.toInt() and 0xFFFF
        val flags = bb.short.toInt() and 0xFFFF
        val qdCount = bb.short.toInt() and 0xFFFF

        if (qdCount < 1) return null

        // Skip ANCOUNT, NSCOUNT, ARCOUNT
        bb.short
        bb.short
        bb.short

        val domainBuilder = StringBuilder()
        var pos = bb.position()

        while (pos < length) {
            val labelLen = buffer[pos].toInt() and 0xFF
            if (labelLen == 0) {
                pos++
                break
            }
            if (labelLen >= 192) { // Pointer
                pos += 2
                break
            }
            pos++
            if (domainBuilder.isNotEmpty()) domainBuilder.append(".")
            val labelBytes = ByteArray(labelLen)
            System.arraycopy(buffer, pos, labelBytes, 0, labelLen)
            domainBuilder.append(String(labelBytes, Charsets.US_ASCII))
            pos += labelLen
        }

        val typeInt = if (pos + 2 <= length) {
            ((buffer[pos].toInt() and 0xFF) shl 8) or (buffer[pos + 1].toInt() and 0xFF)
        } else 1

        val typeStr = when (typeInt) {
            1 -> "A"
            28 -> "AAAA"
            5 -> "CNAME"
            65 -> "HTTPS"
            else -> "TYPE_$typeInt"
        }

        val queryRaw = ByteArray(length)
        System.arraycopy(buffer, 0, queryRaw, 0, length)

        return DnsQuery(
            domain = domainBuilder.toString().lowercase(),
            queryType = typeStr,
            transactionId = id,
            rawPacket = queryRaw
        )
    }

    fun buildSinkholeResponse(query: DnsQuery, reason: String): DnsResult {
        val queryRaw = query.rawPacket
        val response = ByteArray(queryRaw.size + 16)
        System.arraycopy(queryRaw, 0, response, 0, queryRaw.size)

        // Set Response Flag (QR=1, RA=1, RCODE=0 or NXDOMAIN)
        response[2] = 0x81.toByte()
        response[3] = 0x80.toByte()

        // Set ANCOUNT = 1
        response[6] = 0x00.toByte()
        response[7] = 0x01.toByte()

        return DnsResult(
            domain = query.domain,
            resolvedIp = "0.0.0.0 (Sinkhole)",
            isBlocked = true,
            blockReason = reason,
            responseBytes = response
        )
    }

    suspend fun resolveUpstream(query: DnsQuery, upstreamServerIp: String = "1.1.1.1"): DnsResult {
        return try {
            val socket = DatagramSocket()
            socket.soTimeout = 3000
            val ip = InetAddress.getByName(upstreamServerIp)
            val sendPacket = DatagramPacket(query.rawPacket, query.rawPacket.size, ip, 53)
            socket.send(sendPacket)

            val recvBuf = ByteArray(1024)
            val recvPacket = DatagramPacket(recvBuf, recvBuf.size)
            socket.receive(recvPacket)
            socket.close()

            val respBytes = ByteArray(recvPacket.length)
            System.arraycopy(recvBuf, 0, respBytes, 0, recvPacket.length)

            DnsResult(
                domain = query.domain,
                resolvedIp = extractIpFromDnsResponse(respBytes),
                isBlocked = false,
                blockReason = "Upstream ($upstreamServerIp)",
                responseBytes = respBytes
            )
        } catch (e: Exception) {
            DnsResult(
                domain = query.domain,
                resolvedIp = "Timeout / Error",
                isBlocked = false,
                blockReason = "Upstream Failed: ${e.message}",
                responseBytes = null
            )
        }
    }

    private fun extractIpFromDnsResponse(resp: ByteArray): String {
        if (resp.size < 12) return "Unknown"
        val anCount = ((resp[6].toInt() and 0xFF) shl 8) or (resp[7].toInt() and 0xFF)
        if (anCount == 0) return "NXDOMAIN"
        return "Resolved"
    }
}
