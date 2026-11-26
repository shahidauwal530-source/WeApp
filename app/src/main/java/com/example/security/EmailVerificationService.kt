package com.example.security

import com.example.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

sealed class EmailSendResult {
    data class OtpCodeSent(
        val email: String,
        val expiresMinutes: Int = 10,
        val cooldownSeconds: Int = 60
    ) : EmailSendResult()

    data class FirebaseEmailSent(
        val email: String,
        val isEmailUpdate: Boolean
    ) : EmailSendResult()

    data class AlreadyVerified(val email: String) : EmailSendResult()

    data class NeedsGoogleSignInOrResendKey(val email: String) : EmailSendResult()

    data class Error(val message: String, val cooldownRemainingSeconds: Int = 0) : EmailSendResult()
}

sealed class EmailVerifyResult {
    data class Verified(val email: String, val method: String) : EmailVerifyResult()
    data class Invalid(val message: String, val attemptsRemaining: Int? = null) : EmailVerifyResult()
}

data class EmailOtpChallenge(
    val normalizedEmail: String,
    val saltedHash: SaltedPasswordHash,
    val expiresAtMillis: Long,
    val lastSentAtMillis: Long,
    val attemptsRemaining: Int
)

class EmailOtpCoordinator(
    private val resendCooldownMs: Long = 60_000L,
    private val expiryMs: Long = 10 * 60_000L,
    private val maxAttempts: Int = 5
) {
    private val random = SecureRandom()
    private val challenges = ConcurrentHashMap<String, EmailOtpChallenge>()

    fun generateSixDigitCode(): String = (random.nextInt(900_000) + 100_000).toString()

    fun cooldownRemainingSeconds(email: String, nowMillis: Long = System.currentTimeMillis()): Int {
        val normalized = normalizeEmail(email)
        val existing = challenges[normalized] ?: return 0
        val elapsed = nowMillis - existing.lastSentAtMillis
        return if (elapsed in 0 until resendCooldownMs) {
            ((resendCooldownMs - elapsed + 999L) / 1000L).toInt()
        } else {
            0
        }
    }

    fun prepareChallenge(
        email: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Result<String> {
        val normalized = normalizeEmail(email)
        if (!EmailVerificationService.isValidEmail(normalized)) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        val remaining = cooldownRemainingSeconds(normalized, nowMillis)
        if (remaining > 0) {
            return Result.failure(
                CooldownException(
                    "Please wait ${remaining}s before requesting a new verification code.",
                    remaining
                )
            )
        }
        val code = generateSixDigitCode()
        val salted = PasswordSecurity.hashPassword("$normalized:$code")
        challenges[normalized] = EmailOtpChallenge(
            normalizedEmail = normalized,
            saltedHash = salted,
            expiresAtMillis = nowMillis + expiryMs,
            lastSentAtMillis = nowMillis,
            attemptsRemaining = maxAttempts
        )
        return Result.success(code)
    }

    fun rollbackChallenge(email: String) {
        challenges.remove(normalizeEmail(email))
    }

    fun verifyCode(
        email: String,
        enteredCode: String,
        nowMillis: Long = System.currentTimeMillis()
    ): EmailVerifyResult {
        val normalized = normalizeEmail(email)
        val cleanCode = enteredCode.trim()
        if (!cleanCode.matches(Regex("^\\d{6}$"))) {
            return EmailVerifyResult.Invalid("Enter the 6-digit verification code sent to your email.")
        }
        val current = challenges[normalized]
            ?: return EmailVerifyResult.Invalid("No active verification code for $normalized. Request a new code.")

        if (nowMillis > current.expiresAtMillis) {
            challenges.remove(normalized)
            return EmailVerifyResult.Invalid("Verification code expired. Please request a new code.")
        }
        if (current.attemptsRemaining <= 0) {
            challenges.remove(normalized)
            return EmailVerifyResult.Invalid("Too many incorrect attempts. Please request a new code.")
        }

        val matches = PasswordSecurity.verifyPassword(
            "$normalized:$cleanCode",
            current.saltedHash.hash,
            current.saltedHash.salt
        )
        return if (matches) {
            challenges.remove(normalized)
            EmailVerifyResult.Verified(email = normalized, method = "otp")
        } else {
            val nextAttempts = current.attemptsRemaining - 1
            if (nextAttempts <= 0) {
                challenges.remove(normalized)
                EmailVerifyResult.Invalid(
                    "Incorrect code. Maximum attempts reached; please request a new code.",
                    attemptsRemaining = 0
                )
            } else {
                challenges[normalized] = current.copy(attemptsRemaining = nextAttempts)
                EmailVerifyResult.Invalid(
                    "Incorrect verification code. $nextAttempts attempt(s) left.",
                    attemptsRemaining = nextAttempts
                )
            }
        }
    }

    fun normalizeEmail(email: String): String = email.trim().lowercase()

    class CooldownException(message: String, val remainingSeconds: Int) : IllegalStateException(message)
}

object EmailVerificationService {
    private val EMAIL_REGEX = Regex(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    )

    val otpCoordinator = EmailOtpCoordinator()

    fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        return trimmed.length in 5..120 && EMAIL_REGEX.matches(trimmed)
    }

    fun isOtpEmailApiConfigured(): Boolean {
        val key = runCatching { BuildConfig.RESEND_API_KEY.trim() }.getOrDefault("")
        if (key.isBlank()) return false
        val upper = key.uppercase()
        return !upper.startsWith("YOUR_") && !upper.startsWith("MY_") && key != "null"
    }

    private fun resolveFromEmail(): String {
        val configured = runCatching { BuildConfig.RESEND_FROM_EMAIL.trim() }.getOrDefault("")
        return if (configured.isNotBlank() && isValidEmail(configured)) {
            configured
        } else {
            "onboarding@resend.dev"
        }
    }

    suspend fun sendVerification(
        email: String,
        language: String = "en"
    ): EmailSendResult {
        val normalized = email.trim().lowercase()
        if (!isValidEmail(normalized)) {
            val msg = if (language == "ha") {
                "Shigar da ingantaccen adireshin imel tukuna."
            } else {
                "Please enter a valid email address first."
            }
            return EmailSendResult.Error(msg)
        }

        if (isOtpEmailApiConfigured()) {
            val codeResult = otpCoordinator.prepareChallenge(normalized)
            val code = codeResult.getOrElse { err ->
                val cooldown = (err as? EmailOtpCoordinator.CooldownException)?.remainingSeconds ?: 0
                return EmailSendResult.Error(err.message ?: "Unable to prepare code", cooldown)
            }
            val sendRes = sendViaResendApi(
                apiKey = BuildConfig.RESEND_API_KEY.trim(),
                fromEmail = resolveFromEmail(),
                toEmail = normalized,
                code = code,
                language = language
            )
            return sendRes.fold(
                onSuccess = {
                    EmailSendResult.OtpCodeSent(email = normalized, expiresMinutes = 10, cooldownSeconds = 60)
                },
                onFailure = { err ->
                    otpCoordinator.rollbackChallenge(normalized)
                    EmailSendResult.Error(err.localizedMessage ?: "Failed to send verification code email.")
                }
            )
        }

        val currentUser = runCatching { Firebase.auth.currentUser }.getOrNull()
        if (currentUser != null) {
            return sendViaFirebaseAuth(normalized, language)
        }

        return EmailSendResult.NeedsGoogleSignInOrResendKey(normalized)
    }

    suspend fun sendViaFirebaseAuth(
        email: String,
        language: String = "en"
    ): EmailSendResult {
        val normalized = email.trim().lowercase()
        val user = runCatching { Firebase.auth.currentUser }.getOrNull()
            ?: return EmailSendResult.NeedsGoogleSignInOrResendKey(normalized)
        return try {
            runCatching { user.reload().await() }
            val currentEmail = user.email?.trim()?.lowercase().orEmpty()
            if (currentEmail == normalized) {
                if (user.isEmailVerified) {
                    EmailSendResult.AlreadyVerified(normalized)
                } else {
                    user.sendEmailVerification().await()
                    EmailSendResult.FirebaseEmailSent(normalized, isEmailUpdate = false)
                }
            } else {
                user.verifyBeforeUpdateEmail(normalized).await()
                EmailSendResult.FirebaseEmailSent(normalized, isEmailUpdate = true)
            }
        } catch (e: Exception) {
            val fallbackMsg = if (language == "ha") {
                "An kasa tura saƙon tabbatar da imel: ${e.localizedMessage ?: "Sake gwadawa"}"
            } else {
                e.localizedMessage ?: "Unable to send Firebase verification email."
            }
            EmailSendResult.Error(fallbackMsg)
        }
    }

    suspend fun checkFirebaseEmailVerified(targetEmail: String): EmailVerifyResult {
        val normalized = targetEmail.trim().lowercase()
        val user = runCatching { Firebase.auth.currentUser }.getOrNull()
            ?: return EmailVerifyResult.Invalid("Sign in with Google first to check email verification.")
        return try {
            user.reload().await()
            val refreshed = Firebase.auth.currentUser
            val currentEmail = refreshed?.email?.trim()?.lowercase().orEmpty()
            if (refreshed != null && refreshed.isEmailVerified && (currentEmail == normalized || normalized.isBlank())) {
                EmailVerifyResult.Verified(email = currentEmail.ifBlank { normalized }, method = "firebase")
            } else if (refreshed != null && !refreshed.isEmailVerified) {
                EmailVerifyResult.Invalid("Email is not verified yet. Open the verification email in your inbox, then tap Check Status.")
            } else {
                EmailVerifyResult.Invalid("Open the link sent to $normalized to finish verifying this email address.")
            }
        } catch (e: Exception) {
            EmailVerifyResult.Invalid(e.localizedMessage ?: "Unable to refresh verification status.")
        }
    }

    fun verifyOtpCode(email: String, code: String): EmailVerifyResult =
        otpCoordinator.verifyCode(email = email, enteredCode = code)

    private suspend fun sendViaResendApi(
        apiKey: String,
        fromEmail: String,
        toEmail: String,
        code: String,
        language: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("https://api.resend.com/emails")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 15_000
                doOutput = true
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }

            val subject = if (language == "ha") {
                "Lambar Tabbatarwa ta WeApp: $code"
            } else {
                "Your WeApp Verification Code: $code"
            }
            val htmlBody = """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:24px;background:#121026;color:#ffffff;border-radius:16px;">
                  <h2 style="margin:0 0 12px;color:#A78BFA;">WeApp Email Verification</h2>
                  <p style="margin:0 0 16px;color:#D1D5DB;">Use the 6-digit verification code below to verify your email address:</p>
                  <div style="font-size:32px;font-weight:bold;letter-spacing:8px;padding:16px 20px;background:#1F1B3D;border-radius:12px;text-align:center;color:#34D399;">$code</div>
                  <p style="margin:16px 0 0;font-size:13px;color:#9CA3AF;">This code expires in 10 minutes. Never share this code with anyone.</p>
                </div>
            """.trimIndent()
            val textBody = "Your WeApp verification code is $code. It expires in 10 minutes. Never share this code with anyone."

            val payload = JSONObject().apply {
                put("from", "WeApp Security <$fromEmail>")
                put("to", JSONArray().put(toEmail))
                put("subject", subject)
                put("html", htmlBody)
                put("text", textBody)
            }.toString()

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload)
                writer.flush()
            }

            val status = connection.responseCode
            if (status in 200..299) {
                Result.success(Unit)
            } else {
                val errText = runCatching {
                    connection.errorStream?.bufferedReader()?.use { it.readText() }
                }.getOrNull().orEmpty()
                Result.failure(
                    IllegalStateException(
                        "Email delivery failed (HTTP $status). ${errText.take(180)}"
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }
}
