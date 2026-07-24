package com.example.engine

data class HttpRequestInfo(
    val method: String,
    val path: String,
    val host: String,
    val headers: Map<String, String>,
    val isRedacted: Boolean = true
)

class HttpInspector {

    private val sensitiveHeaders = setOf(
        "authorization", "proxy-authorization", "cookie", "set-cookie",
        "x-api-key", "api-key", "token", "sec-websocket-key"
    )

    fun inspectHttpRequest(data: ByteArray): HttpRequestInfo? {
        val text = String(data, Charsets.UTF_8)
        val lines = text.split("\r\n", "\n")
        if (lines.isEmpty()) return null

        val requestLineParts = lines[0].split(" ")
        if (requestLineParts.size < 2) return null

        val method = requestLineParts[0]
        val path = requestLineParts[1]
        var host = ""
        val headers = mutableMapOf<String, String>()

        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.isBlank()) break
            val colonIdx = line.indexOf(":")
            if (colonIdx > 0) {
                val name = line.substring(0, colonIdx).trim()
                val value = line.substring(colonIdx + 1).trim()
                val lowerName = name.lowercase()

                if (lowerName == "host") {
                    host = value
                }

                if (sensitiveHeaders.contains(lowerName)) {
                    headers[name] = "[REDACTED SECRET]"
                } else {
                    headers[name] = value
                }
            }
        }

        return HttpRequestInfo(
            method = method,
            path = path,
            host = host,
            headers = headers,
            isRedacted = true
        )
    }

    fun extractSniFromTlsClientHello(data: ByteArray): String? {
        if (data.size < 43) return null
        if (data[0].toInt() != 0x16) return null // TLS Handshake record type

        try {
            var pos = 5 // Skip TLS Record Header
            if (data[pos].toInt() != 0x01) return null // ClientHello
            
            // Skip Handshake Header (4 bytes)
            pos += 4
            // Skip Version (2) + Random (32)
            pos += 34

            // Skip Session ID
            if (pos >= data.size) return null
            val sessionIdLen = data[pos].toInt() and 0xFF
            pos += 1 + sessionIdLen

            // Skip Cipher Suites
            if (pos + 1 >= data.size) return null
            val cipherSuitesLen = ((data[pos].toInt() and 0xFF) shl 8) or (data[pos + 1].toInt() and 0xFF)
            pos += 2 + cipherSuitesLen

            // Skip Compression Methods
            if (pos >= data.size) return null
            val compMethodsLen = data[pos].toInt() and 0xFF
            pos += 1 + compMethodsLen

            // Extensions length
            if (pos + 1 >= data.size) return null
            val extensionsLen = ((data[pos].toInt() and 0xFF) shl 8) or (data[pos + 1].toInt() and 0xFF)
            pos += 2

            val extensionsEnd = pos + extensionsLen
            while (pos + 4 <= extensionsEnd && pos + 4 <= data.size) {
                val extType = ((data[pos].toInt() and 0xFF) shl 8) or (data[pos + 1].toInt() and 0xFF)
                val extLen = ((data[pos + 2].toInt() and 0xFF) shl 8) or (data[pos + 3].toInt() and 0xFF)
                pos += 4

                if (extType == 0) { // server_name extension
                    if (pos + 5 <= data.size) {
                        // skip server_name_list_length (2) + server_name_type (1)
                        val nameLen = ((data[pos + 3].toInt() and 0xFF) shl 8) or (data[pos + 4].toInt() and 0xFF)
                        pos += 5
                        if (pos + nameLen <= data.size) {
                            val sniBytes = ByteArray(nameLen)
                            System.arraycopy(data, pos, sniBytes, 0, nameLen)
                            return String(sniBytes, Charsets.US_ASCII)
                        }
                    }
                }
                pos += extLen
            }
        } catch (e: Exception) {
            return null
        }
        return null
    }
}
