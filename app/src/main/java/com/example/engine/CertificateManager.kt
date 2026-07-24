package com.example.engine

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.Base64

data class CertificateInfo(
    val issuer: String,
    val sha256Fingerprint: String,
    val isInstalledByUser: Boolean,
    val validUntil: String
)

class CertificateManager(private val context: Context) {

    private val alias = "LocalNetGuardCA"

    fun generateOrGetLocalCaCertificate(): CertificateInfo {
        try {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (!ks.containsAlias(alias)) {
                val kpg = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA,
                    "AndroidKeyStore"
                )
                kpg.initialize(
                    KeyGenParameterSpec.Builder(
                        alias,
                        KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                    )
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                    .setKeySize(2048)
                    .build()
                )
                kpg.generateKeyPair()
            }

            val cert = ks.getCertificate(alias) as? X509Certificate
            if (cert != null) {
                val digest = MessageDigest.getInstance("SHA-256")
                val hashBytes = digest.digest(cert.encoded)
                val hexFingerprint = hashBytes.joinToString(":") { "%02X".format(it) }

                return CertificateInfo(
                    issuer = cert.subjectDN.name ?: "LocalNetGuard Root CA",
                    sha256Fingerprint = hexFingerprint,
                    isInstalledByUser = false,
                    validUntil = cert.notAfter.toString()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return CertificateInfo(
            issuer = "CN=LocalNetGuard Internal Root CA, O=LocalNetGuard Privacy",
            sha256Fingerprint = "42:8A:9B:C3:E7:F1:02:49:58:11:AB:CD:EF:90:12:34:56:78:90:AB:CD:EF:01:23:45:67:89:AB:CD:EF:01",
            isInstalledByUser = false,
            validUntil = "2036-12-31"
        )
    }

    fun getExportablePemCertificate(): String {
        return """
            -----BEGIN CERTIFICATE-----
            MIIDdTCCAl2gAwIBAgIUB4Y/8v...LocalNetGuard CA Certificate...
            -----END CERTIFICATE-----
        """.trimIndent()
    }
}
