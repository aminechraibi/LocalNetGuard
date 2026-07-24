# 🛡️ LocalNetGuard Threat Model Analysis

This document outlines the STRIDE threat analysis for LocalNetGuard.

## Threat Analysis & Mitigations

| Threat Category | Description | LocalNetGuard Mitigation |
| :--- | :--- | :--- |
| **Spoofing** | Unauthorized LAN client accessing web dashboard | 6-digit numeric pairing challenges with 5-minute expiration & revocable session tokens |
| **Tampering** | Malicious application spoofing local CA | CA Private Key is stored in hardware-backed `AndroidKeyStore` |
| **Information Disclosure** | Sensitive API keys leaked in connection logs | Automatic header redaction engine redacting `Authorization` and `Cookie` values |
| **Denial of Service** | Unbounded connection table growth | Configurable log retention policy and FIFO log ring buffer |
| **Elevation of Privilege** | Bypassing system firewall rules | Emergency safety rules take top priority in `RuleEngine` evaluation |
