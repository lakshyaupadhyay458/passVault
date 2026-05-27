package com.example.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CryptoHelper {
    private const val PROVIDER = "AndroidKeyStore"
    private const val ALIAS = "PassVaultSecretKeyAlias"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    init {
        try {
            getOrCreateSecretKey()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var cachedSecretKey: SecretKey? = null

    @Synchronized
    private fun getOrCreateSecretKey(): SecretKey {
        cachedSecretKey?.let { return it }
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        val existingKey = keyStore.getKey(ALIAS, null) as? SecretKey
        if (existingKey != null) {
            cachedSecretKey = existingKey
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        val newKey = keyGenerator.generateKey()
        cachedSecretKey = newKey
        return newKey
    }

    fun encrypt(plainText: String): EncryptedPayload {
        if (plainText.isEmpty()) return EncryptedPayload("", "")
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            EncryptedPayload(
                base64Cipher = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP),
                base64Iv = Base64.encodeToString(iv, Base64.NO_WRAP)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            EncryptedPayload("", "")
        }
    }

    fun decrypt(encryptedPayload: EncryptedPayload): String {
        if (encryptedPayload.base64Cipher.isEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val ivBytes = Base64.decode(encryptedPayload.base64Iv, Base64.NO_WRAP)
            val cipherBytes = Base64.decode(encryptedPayload.base64Cipher, Base64.NO_WRAP)
            val spec = GCMParameterSpec(128, ivBytes)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
            val decryptedBytes = cipher.doFinal(cipherBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            "[Decryption Error]"
        }
    }

    fun decrypt(base64Cipher: String, base64Iv: String): String {
        return decrypt(EncryptedPayload(base64Cipher, base64Iv))
    }
}

data class EncryptedPayload(
    val base64Cipher: String,
    val base64Iv: String
)
