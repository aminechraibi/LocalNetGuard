# 🔒 LocalNetGuard Privacy Policy

LocalNetGuard is committed to absolute user privacy.

## Core Privacy Principles

1. **Local Packet Processing**: All network traffic analysis, DNS sinkholing, and rule evaluation occur locally inside the device's Android `VpnService` loopback environment.
2. **No Remote Telemetry**: LocalNetGuard contains zero analytics SDKs, zero ad networks, and zero tracking code.
3. **Secret Redaction**: Request headers containing authorization tokens (`Authorization`, `Cookie`, `X-Api-Key`) are automatically redacted before saving to local Room logs.
4. **LAN Web Server Security**: The embedded Ktor web server is disabled by default and requires short-lived numeric pairing code authorization.
