package com.example.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CryptoManager {

    private var keyStore: KeyStore? = null
    private var testFallbackKey: SecretKey? = null

    init {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
                load(null)
            }
        } catch (e: Exception) {
            // JVM fallback (useful for local Unit/Robolectric testing)
            try {
                val generator = KeyGenerator.getInstance("AES")
                generator.init(256)
                testFallbackKey = generator.generateKey()
            } catch (ex: Exception) {
                // Ignore fallback creation errors
            }
        }
    }

    private fun getKey(): SecretKey {
        val ks = keyStore
        if (ks == null) {
            return testFallbackKey ?: throw IllegalStateException("CryptoManager key not initialized")
        }

        return try {
            val existingKey = ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            existingKey?.secretKey ?: generateKey()
        } catch (e: Exception) {
            // Fallback if Keystore operation fails inside JVM tests
            testFallbackKey ?: run {
                val generator = KeyGenerator.getInstance("AES")
                generator.init(256)
                val key = generator.generateKey()
                testFallbackKey = key
                key
            }
        }
    }

    private fun generateKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false) // Allow offline usage without biometric prompt for settings
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun encrypt(bytes: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        val encryptedBytes = cipher.doFinal(bytes)
        return Pair(encryptedBytes, cipher.iv)
    }

    fun decrypt(encryptedBytes: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)
        return cipher.doFinal(encryptedBytes)
    }

    companion object {
        private const val KEY_ALIAS = "AegisAppLockMasterKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
