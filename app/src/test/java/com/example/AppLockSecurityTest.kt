package com.example

import com.example.security.CryptoManager
import com.example.security.HashUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class AppLockSecurityTest {

    @Test
    fun `test salt generation uniqueness`() {
        val salt1 = HashUtils.generateSalt()
        val salt2 = HashUtils.generateSalt()
        
        assertNotEquals(salt1, salt2)
        assertTrue(salt1.isNotEmpty())
        assertTrue(salt2.isNotEmpty())
    }

    @Test
    fun `test password hashing with salt`() {
        val salt = HashUtils.generateSalt()
        val pin = "1234"
        
        val hash1 = HashUtils.hashPassword(pin, salt)
        val hash2 = HashUtils.hashPassword(pin, salt)
        
        // Hashes of the same input with the same salt must match
        assertEquals(hash1, hash2)
        
        // Hashes of different inputs must not match
        val hashDifferent = HashUtils.hashPassword("5678", salt)
        assertNotEquals(hash1, hashDifferent)
    }

    @Test
    fun `test AES GCM encryption and decryption`() {
        val cryptoManager = CryptoManager()
        val rawMessage = "Aegis_Secure_Lock_Payload_Text"
        val rawBytes = rawMessage.toByteArray(Charsets.UTF_8)
        
        // Encrypt
        val (ciphertext, iv) = cryptoManager.encrypt(rawBytes)
        assertNotEquals(rawBytes, ciphertext)
        assertTrue(ciphertext.isNotEmpty())
        assertTrue(iv.isNotEmpty())
        
        // Decrypt
        val decryptedBytes = cryptoManager.decrypt(ciphertext, iv)
        val decryptedMessage = String(decryptedBytes, Charsets.UTF_8)
        
        assertEquals(rawMessage, decryptedMessage)
    }
}
