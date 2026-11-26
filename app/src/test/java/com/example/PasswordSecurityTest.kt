package com.example

import com.example.security.PasswordSecurity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordSecurityTest {

    @Test
    fun `validateNewPassword enforces length complexity and confirmation match`() {
        assertNotNull(PasswordSecurity.validateNewPassword("short1", "short1"))
        assertNotNull(PasswordSecurity.validateNewPassword("onlyletters", "onlyletters"))
        assertNotNull(PasswordSecurity.validateNewPassword("12345678", "12345678"))
        assertNotNull(PasswordSecurity.validateNewPassword("WeApp2026", "Mismatch2026"))
        assertNull(PasswordSecurity.validateNewPassword("WeApp2026!", "WeApp2026!"))
    }

    @Test
    fun `hashPassword produces unique salted PBKDF2 hashes and verifies accurately`() {
        val secret = "WeAppPass2026!"
        val first = PasswordSecurity.hashPassword(secret)
        val second = PasswordSecurity.hashPassword(secret)

        assertTrue(first.hash.startsWith("pbkdf2_"))
        assertNotEquals(first.salt, second.salt)
        assertNotEquals(first.hash, second.hash)
        assertFalse(first.hash.contains(secret))

        assertTrue(PasswordSecurity.verifyPassword(secret, first.hash, first.salt))
        assertTrue(PasswordSecurity.verifyPassword(secret, second.hash, second.salt))
        assertFalse(PasswordSecurity.verifyPassword("WrongPass2026!", first.hash, first.salt))
        assertFalse(PasswordSecurity.verifyPassword(secret, first.hash, second.salt))
    }

    @Test
    fun `evaluateStrength distinguishes weak fair good and strong passwords`() {
        assertEquals("Weak", PasswordSecurity.evaluateStrength("abc").label)
        assertEquals("Good", PasswordSecurity.evaluateStrength("weapp123").label)
        assertEquals("Strong", PasswordSecurity.evaluateStrength("WeApp2026!").label)
    }
}
