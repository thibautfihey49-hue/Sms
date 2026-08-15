package com.calculatrice.messenger
import javax.crypto.*
import javax.crypto.spec.*
import java.security.*
import java.util.*

object CryptoEngine {
    fun generateAesKey(): ByteArray {
        val key = ByteArray(32)
        SecureRandom().nextBytes(key)
        return key
    }

    fun encrypt(message: String, key: ByteArray): String {
        val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }
        val secretKey = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(message.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(iv + ciphertext)
    }

    fun decrypt(encryptedB64: String, key: ByteArray): String {
        return try {
            val packet = Base64.getDecoder().decode(encryptedB64)
            val iv = packet.take(12).toByteArray()
            val secretKey = SecretKeySpec(key, "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            val ciphertext = packet.drop(12).toByteArray()
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            "⚠️ Message illisible"
        }
    }
}
