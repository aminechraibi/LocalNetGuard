# 🛡️ LocalNetGuard

> **Android Local VPN Firewall, HTTP/HTTPS Filter, DNS Sinkhole, and Embedded LAN Management Dashboard**

**LocalNetGuard** is an open-source, production-ready Android firewall and local network security system powered by Android `VpnService`, Jetpack Compose, Room, DataStore, and an embedded Ktor LAN administration web server.

---

## 🌟 Key Features

* **🛡️ Local VPN Firewall Engine (`VpnService`)**:
  - Full IPv4 local traffic routing and filtering.
  - Granular per-application network toggles (Internet, Wi-Fi, Mobile Data, Background, QUIC block).
  - Socket protection (`protect()`) preventing loopbacks.

* **🚫 DNS Proxy & Sinkhole**:
  - Local DNS listener intercepting UDP/TCP DNS queries.
  - Multi-category blocklists (StevenBlack Hosts, AdGuard DNS Filter, Malware Domain List).
  - Upstream DoH (DNS over HTTPS) and DoT (DNS over TLS) resolution.

* **⚖️ Deterministic Firewall Rule Engine**:
  - Domain wildcards (`*.tracker.com`), IP/CIDR subnets, Port rules, Application rules.
  - Priority hierarchy: System Safety -> Per-App Policies -> Custom Rules -> DNS Blocklists -> Global Policy.
  - Interactive **Rule Evaluation Simulator** to test and verify traffic policies.

* **🔒 Plain HTTP & Optional HTTPS Inspection**:
  - Real-time HTTP header and TLS ClientHello SNI domain extraction.
  - Automatic secret redaction for `Authorization`, `Cookie`, and `X-Api-Key` headers.
  - KeyStore-backed Root CA generation and PEM export for optional HTTPS MITM inspection.
  - Automatic fail-safe bypass for banking and certificate-pinned applications.

* **🌐 Embedded LAN Remote Management Web Dashboard**:
  - Embedded Ktor HTTP server serving static dashboard assets and REST APIs (`/api/v1/...`).
  - Secure 6-digit numeric pairing challenges and revocable session tokens.
  - Real-time socket connection monitor and control panel accessible on your local Wi-Fi.

* **📊 Traffic Analytics & Configuration Backup**:
  - Detailed traffic metrics and domain/app usage breakdowns.
  - Versioned JSON configuration import and export for rule backups.

---

## 🏗️ Architecture & Module Layout

```text
LocalNetGuard/
├── app/
│   ├── src/main/java/com/example/
│   │   ├── LocalNetApp.kt             # Application class & Notification channels
│   │   ├── MainActivity.kt            # Jetpack Compose UI & Navigation Compose
│   │   ├── data/
│   │   │   ├── database/              # Room AppDatabase & DAOs
│   │   │   ├── datastore/             # DataStore Preferences
│   │   │   ├── model/                 # AppPolicy, FirewallRule, ConnectionLog, DnsLog
│   │   │   └── repository/            # FirewallRepository single source of truth
│   │   ├── engine/
│   │   │   ├── RuleEngine.kt          # High-performance rule matching
│   │   │   ├── DnsResolver.kt         # DNS proxy & sinkhole resolver
│   │   │   ├── HttpInspector.kt       # HTTP & TLS ClientHello inspector
│   │   │   └── CertificateManager.kt # Local CA KeyStore & PEM exporter
│   │   ├── server/
│   │   │   ├── LanWebServer.kt        # Embedded Ktor HTTP server
│   │   │   └── PairingManager.kt      # Short-lived numeric pairing challenges
│   │   ├── vpn/
│   │   │   ├── LocalNetVpnService.kt  # Android VpnService & packet loop
│   │   │   ├── VpnState.kt            # VPN status & throughput flows
│   │   │   └── BootReceiver.kt        # Boot receiver for autostart
│   │   └── ui/
│   │       ├── navigation/            # Bottom navigation routes
│   │       ├── screens/               # Compose screens (Dashboard, Apps, Rules, DNS, Live, LAN)
│   │       └── theme/                 # Cyber Navy Material 3 palette
│   └── src/main/assets/dashboard/     # Embedded LAN Web Dashboard assets (HTML5/JS/CSS)
```

---

## ⚡ Quick Start & Build Instructions

### Prerequisites
* JDK 17 or higher
* Android SDK 36 (Android 14/15/16 ready)
* Gradle 8.x

### Build Debug APK
```bash
./gradlew :app:assembleDebug
```

### Run Local Unit Tests
```bash
./gradlew :app:testDebugUnitTest
```

---

## 🛡️ Security & Privacy Disclosures

1. **100% Local Execution**: All packet processing, rule evaluation, and log storage happen strictly on the device.
2. **Zero Telemetry**: No user data, URLs, or IP addresses leave your network.
3. **Certificate Pinning Compliance**: Banking apps and security-critical services with hardcoded pins fail-safe and bypass HTTPS inspection.

---

## 📜 License
Released under the MIT License.
