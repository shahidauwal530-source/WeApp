package com.example.security

import android.os.Build
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class SaltedPasswordHash(
    val hash: String,
    val salt: String
)

data class PasswordStrength(
    val score: Int,
    val label: String,
    val hasMinLength: Boolean,
    val hasLetter: Boolean,
    val hasDigit: Boolean,
    val hasSpecialOrUpper: Boolean
)

object PasswordSecurity {
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    private const val ALG_SHA256 = "PBKDF2WithHmacSHA256"
    private const val ALG_SHA1 = "PBKDF2WithHmacSHA1"
    private const val PREFIX_SHA256 = "pbkdf2_sha256"
    private const val PREFIX_SHA1 = "pbkdf2_sha1"

    private val secureRandom = SecureRandom()

    fun evaluateStrength(password: String): PasswordStrength {
        val hasMinLength = password.length >= 8
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialOrUpper = password.any { !it.isLetterOrDigit() } ||
            (password.any { it.isUpperCase() } && password.any { it.isLowerCase() })

        val score = listOf(hasMinLength, hasLetter, hasDigit, hasSpecialOrUpper).count { it }
        val label = when {
            password.isEmpty() -> ""
            score <= 1 -> "Weak"
            score == 2 -> "Fair"
            score == 3 -> "Good"
            else -> "Strong"
        }
        return PasswordStrength(
            score = score,
            label = label,
            hasMinLength = hasMinLength,
            hasLetter = hasLetter,
            hasDigit = hasDigit,
            hasSpecialOrUpper = hasSpecialOrUpper
        )
    }

    fun validateNewPassword(password: String, confirmPassword: String): String? {
        val strength = evaluateStrength(password)
        return when {
            password.length < 8 -> "Password must be at least 8 characters long."
            password.length > 128 -> "Password must be 128 characters or fewer."
            !strength.hasLetter || !strength.hasDigit ->
                "Password must include at least one letter and one number."
            password != confirmPassword -> "Password and Confirm Password do not match."
            else -> null
        }
    }

    fun hashPassword(password: String): SaltedPasswordHash {
        val saltBytes = ByteArray(SALT_BYTES)
        secureRandom.nextBytes(saltBytes)
        val saltBase64 = encodeBase64(saltBytes)

        val useSha256 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        val algorithm = if (useSha256) ALG_SHA256 else ALG_SHA1
        val prefix = if (useSha256) PREFIX_SHA256 else PREFIX_SHA1

        val derived = deriveKey(password, saltBytes, algorithm, ITERATIONS)
        val derivedBase64 = encodeBase64(derived)
        derived.fill(0)
        return SaltedPasswordHash(
            hash = "$prefix:$ITERATIONS:$derivedBase64",
            salt = saltBase64
        )
    }

    fun verifyPassword(password: String, storedHash: String, storedSalt: String): Boolean {
        if (password.isEmpty() || storedHash.isBlank() || storedSalt.isBlank()) return false
        return try {
            val parts = storedHash.split(":")
            if (parts.size != 3) return false
            val prefix = parts[0]
            val iterations = parts[1].toIntOrNull()?.takeIf { it in 10_000..500_000 } ?: return false
            val expectedBytes = decodeBase64(parts[2])
            val saltBytes = decodeBase64(storedSalt)
            val algorithm = when (prefix) {
                PREFIX_SHA256 -> ALG_SHA256
                PREFIX_SHA1 -> ALG_SHA1
                else -> return false
            }
            val candidateBytes = deriveKey(password, saltBytes, algorithm, iterations)
            val matches = MessageDigest.isEqual(
                expectedBytes.toHexFallback(),
                candidateBytes.toHexFallback()
            )
            candidateBytes.fill(0)
            expectedBytes.fill(0)
            matches
        } catch (_: Throwable) {
            false
        }
    }

    private fun deriveKey(
        password: String,
        saltBytes: ByteArray,
        algorithm: String,
        iterations: Int
    ): ByteArray {
        val chars = password.toCharArray()
        val spec = PBEKeySpec(chars, saltBytes, iterations, KEY_LENGTH_BITS)
        return try {
            val factory = try {
                SecretKeyFactory.getInstance(algorithm)
            } catch (_: Throwable) {
                SecretKeyFactory.getInstance(ALG_SHA1)
            }
            factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            chars.fill('\u0000')
        }
    }

    private fun ByteArray.toHexFallback(): ByteArray =
        joinToString("") { "%02x".format(it) }.toByteArray(StandardCharsets.US_ASCII)

    private const val BASE64_ALPHABET =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    private fun encodeBase64(bytes: ByteArray): String =
        try {
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (_: Throwable) {
            buildString((bytes.size + 2) / 3 * 4) {
                var i = 0
                while (i < bytes.size) {
                    val b0 = bytes[i].toInt() and 0xFF
                    val has1 = i + 1 < bytes.size
                    val has2 = i + 2 < bytes.size
                    val b1 = if (has1) bytes[i + 1].toInt() and 0xFF else 0
                    val b2 = if (has2) bytes[i + 2].toInt() and 0xFF else 0
                    val triple = (b0 shl 16) or (b1 shl 8) or b2
                    append(BASE64_ALPHABET[(triple ushr 18) and 0x3F])
                    append(BASE64_ALPHABET[(triple ushr 12) and 0x3F])
                    append(if (has1) BASE64_ALPHABET[(triple ushr 6) and 0x3F] else '=')
                    append(if (has2) BASE64_ALPHABET[triple and 0x3F] else '=')
                    i += 3
                }
            }
        }

    private fun decodeBase64(encoded: String): ByteArray =
        try {
            Base64.decode(encoded, Base64.NO_WRAP)
        } catch (_: Throwable) {
            val clean = encoded.trim()
            require(clean.length % 4 == 0) { "Invalid Base64 length" }
            if (clean.isEmpty()) ByteArray(0) else {
                val out = ArrayList<Byte>(clean.length * 3 / 4)
                var i = 0
                while (i < clean.length) {
                    val c0 = clean[i]
                    val c1 = clean[i + 1]
                    val c2 = clean[i + 2]
                    val c3 = clean[i + 3]
                    val v0 = BASE64_ALPHABET.indexOf(c0)
                    val v1 = BASE64_ALPHABET.indexOf(c1)
                    val v2 = if (c2 == '=') 0 else BASE64_ALPHABET.indexOf(c2)
                    val v3 = if (c3 == '=') 0 else BASE64_ALPHABET.indexOf(c3)
                    require(v0 >= 0 && v1 >= 0 && v2 >= 0 && v3 >= 0) { "Invalid Base64 character" }
                    val triple = (v0 shl 18) or (v1 shl 12) or (v2 shl 6) or v3
                    out.add(((triple ushr 16) and 0xFF).toByte())
                    if (c2 != '=') out.add(((triple ushr 8) and 0xFF).toByte())
                    if (c3 != '=') out.add((triple and 0xFF).toByte())
                    i += 4
                }
                out.toByteArray()
            }
        }
}
