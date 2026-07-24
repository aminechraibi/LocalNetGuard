package com.example.server

import java.security.SecureRandom
import java.util.UUID

data class PairingChallenge(
    val code: String,
    val expiresAt: Long
)

class PairingManager {

    private val random = SecureRandom()
    @Volatile
    private var currentChallenge: PairingChallenge? = null

    fun generateNewPairingCode(): PairingChallenge {
        val codeNum = random.nextInt(900000) + 100000
        val codeStr = codeNum.toString()
        val challenge = PairingChallenge(
            code = codeStr,
            expiresAt = System.currentTimeMillis() + 300_000 // 5 minutes
        )
        currentChallenge = challenge
        return challenge
    }

    fun validateCode(inputCode: String): String? {
        val active = currentChallenge ?: return null
        if (System.currentTimeMillis() > active.expiresAt) {
            currentChallenge = null
            return null
        }
        if (active.code == inputCode.trim()) {
            currentChallenge = null
            return UUID.randomUUID().toString() // Granted session token
        }
        return null
    }

    fun getCurrentCode(): String {
        val active = currentChallenge
        if (active != null && System.currentTimeMillis() <= active.expiresAt) {
            return active.code
        }
        return generateNewPairingCode().code
    }
}
