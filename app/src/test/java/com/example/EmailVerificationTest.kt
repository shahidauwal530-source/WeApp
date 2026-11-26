package com.example

import com.example.security.EmailOtpCoordinator
import com.example.security.EmailVerificationService
import com.example.security.EmailVerifyResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailVerificationTest {

    @Test
    fun emailValidation_acceptsValidAndRejectsInvalid() {
        assertTrue(EmailVerificationService.isValidEmail("amina.bello@gmail.com"))
        assertTrue(EmailVerificationService.isValidEmail("user+tag@weapp.co.uk"))
        assertFalse(EmailVerificationService.isValidEmail("not-an-email"))
        assertFalse(EmailVerificationService.isValidEmail("a@b"))
        assertFalse(EmailVerificationService.isValidEmail("   "))
    }

    @Test
    fun otpCoordinator_generatesSixDigitCodeAndVerifiesSingleUse() {
        val coordinator = EmailOtpCoordinator(resendCooldownMs = 60_000L, expiryMs = 600_000L, maxAttempts = 5)
        val now = 1_000_000L

        val code = coordinator.prepareChallenge("Amina@Example.COM", nowMillis = now).getOrThrow()
        assertTrue(code.matches(Regex("^\\d{6}$")))

        // Cooldown blocks immediate second request
        val secondReq = coordinator.prepareChallenge("amina@example.com", nowMillis = now + 10_000L)
        assertTrue(secondReq.isFailure)
        assertTrue(coordinator.cooldownRemainingSeconds("amina@example.com", now + 10_000L) in 49..51)

        // Wrong code fails and decrements attempts
        val wrong = coordinator.verifyCode("amina@example.com", "000000", nowMillis = now + 20_000L)
        assertTrue(wrong is EmailVerifyResult.Invalid)
        assertEquals(4, (wrong as EmailVerifyResult.Invalid).attemptsRemaining)

        // Correct code succeeds
        val verified = coordinator.verifyCode("amina@example.com", code, nowMillis = now + 30_000L)
        assertTrue(verified is EmailVerifyResult.Verified)
        assertEquals("amina@example.com", (verified as EmailVerifyResult.Verified).email)

        // Single-use: cannot reuse the same code again
        val replay = coordinator.verifyCode("amina@example.com", code, nowMillis = now + 35_000L)
        assertTrue(replay is EmailVerifyResult.Invalid)
    }

    @Test
    fun otpCoordinator_expiresAfterTimeoutAndLocksAfterMaxAttempts() {
        val coordinator = EmailOtpCoordinator(resendCooldownMs = 60_000L, expiryMs = 120_000L, maxAttempts = 2)
        val now = 5_000_000L

        val code1 = coordinator.prepareChallenge("user1@example.com", nowMillis = now).getOrThrow()
        assertTrue(coordinator.verifyCode("user1@example.com", code1, nowMillis = now + 125_000L) is EmailVerifyResult.Invalid)

        val code2 = coordinator.prepareChallenge("user2@example.com", nowMillis = now).getOrThrow()
        assertTrue(coordinator.verifyCode("user2@example.com", "111111", nowMillis = now + 1_000L) is EmailVerifyResult.Invalid)
        assertTrue(coordinator.verifyCode("user2@example.com", "222222", nowMillis = now + 2_000L) is EmailVerifyResult.Invalid)
        // Even correct code is rejected after max attempts reached
        assertTrue(coordinator.verifyCode("user2@example.com", code2, nowMillis = now + 3_000L) is EmailVerifyResult.Invalid)
    }
}
