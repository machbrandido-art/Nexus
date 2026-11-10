package com.nexus.app.data.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CryptoManagerTest {

    private class JvmCipherStub : SecretCipher {
        private val key: SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

        override fun encrypt(plaintext: String): String {
            if (plaintext.isEmpty()) return ""
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.ENCRYPT_MODE, key)
            val iv = c.iv
            val ct = c.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            return (iv + ct).joinToString(",") { it.toString() }
        }

        override fun decrypt(ciphertext: String): String {
            if (ciphertext.isEmpty()) return ""
            val bytes = ciphertext.split(",").map { it.toByte() }.toByteArray()
            require(bytes.size > 12) { "Ciphertext too short" }
            val iv = bytes.copyOfRange(0, 12)
            val payload = bytes.copyOfRange(12, bytes.size)
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            return String(c.doFinal(payload), Charsets.UTF_8)
        }
    }

    @Test
    fun `round trip preserves value`() {
        val cipher = JvmCipherStub()
        val secret = "sk-12345-abcdefghijklmnop"
        val encrypted = cipher.encrypt(secret)
        assertNotEquals(secret, encrypted)
        assertEquals(secret, cipher.decrypt(encrypted))
    }

    @Test
    fun `empty string short circuits`() {
        val cipher = JvmCipherStub()
        assertEquals("", cipher.encrypt(""))
        assertEquals("", cipher.decrypt(""))
    }

    @Test
    fun `non deterministic encryption`() {
        val cipher = JvmCipherStub()
        val secret = "same-secret"
        val a = cipher.encrypt(secret)
        val b = cipher.encrypt(secret)
        assertNotEquals(a, b)
        assertEquals(secret, cipher.decrypt(a))
        assertEquals(secret, cipher.decrypt(b))
    }

    @Test
    fun `invalid ciphertext fails`() {
        val cipher = JvmCipherStub()
        assertThrows(Throwable::class.java) { cipher.decrypt("1,2,3") }
    }

    @Test
    fun `unicode survives round trip`() {
        val cipher = JvmCipherStub()
        val secret = "مفتاح-سري-🔐-ключ-鍵"
        assertEquals(secret, cipher.decrypt(cipher.encrypt(secret)))
    }
}