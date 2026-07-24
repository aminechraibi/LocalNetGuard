# 🔑 Release Signing Guide

This guide describes how to configure GitHub Actions Secrets for automated Android release builds.

## 1. Generate Release Key
```bash
keytool -genkey -v -keystore localnetguard-release.jks \
  -alias localnetguard \
  -keyalg RSA -keysize 2048 -validity 10000
```

## 2. Base64 Encode Key
```bash
base64 -w 0 localnetguard-release.jks > keystore_base64.txt
```

## 3. GitHub Repository Secrets
Add the following secrets to GitHub:
- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`
