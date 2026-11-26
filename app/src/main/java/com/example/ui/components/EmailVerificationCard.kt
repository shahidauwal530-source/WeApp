package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.localization.AppStrings
import com.example.security.EmailSendResult
import com.example.security.EmailVerificationService
import com.example.security.EmailVerifyResult
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EmailVerificationCard(
    email: String,
    isVerified: Boolean,
    strings: AppStrings,
    currentLanguage: String = "en",
    darkSurface: Boolean = true,
    onVerified: (String) -> Unit,
    onNeedGoogleSignIn: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val normalizedEmail = email.trim().lowercase()
    val validEmail = EmailVerificationService.isValidEmail(normalizedEmail)
    if (!validEmail && !isVerified) return

    val scope = rememberCoroutineScope()
    var isSending by remember(normalizedEmail) { mutableStateOf(false) }
    var isChecking by remember(normalizedEmail) { mutableStateOf(false) }
    var activeFlow by remember(normalizedEmail) { mutableStateOf<String?>(null) } // "otp", "firebase", "needs_auth"
    var codeInput by remember(normalizedEmail) { mutableStateOf("") }
    var cooldownSeconds by remember(normalizedEmail) {
        mutableIntStateOf(EmailVerificationService.otpCoordinator.cooldownRemainingSeconds(normalizedEmail))
    }
    var statusMessage by remember(normalizedEmail) { mutableStateOf<String?>(null) }
    var statusIsError by remember(normalizedEmail) { mutableStateOf(false) }

    LaunchedEffect(cooldownSeconds) {
        if (cooldownSeconds > 0) {
            delay(1000)
            cooldownSeconds -= 1
        }
    }

    val cardColor = when {
        isVerified -> WeEmerald.copy(alpha = if (darkSurface) 0.14f else 0.10f)
        darkSurface -> Color.White.copy(alpha = 0.06f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val borderColor = when {
        isVerified -> WeEmerald.copy(alpha = 0.55f)
        darkSurface -> WeBlueLight.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.outline
    }
    val primaryTextColor = if (darkSurface) Color.White else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (darkSurface) DarkOnSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant

    val fieldColors = if (darkSurface) {
        OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = WeBlueLight,
            unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
            focusedLabelColor = WeBlueLight,
            unfocusedLabelColor = DarkOnSurfaceVariant,
            focusedLeadingIconColor = WeBlueLight,
            unfocusedLeadingIconColor = DarkOnSurfaceVariant,
            cursorColor = WeBlueLight
        )
    } else {
        OutlinedTextFieldDefaults.colors()
    }

    fun triggerSendCode() {
        if (isSending || cooldownSeconds > 0) return
        scope.launch {
            isSending = true
            statusMessage = null
            statusIsError = false
            when (val res = EmailVerificationService.sendVerification(normalizedEmail, currentLanguage)) {
                is EmailSendResult.OtpCodeSent -> {
                    activeFlow = "otp"
                    cooldownSeconds = res.cooldownSeconds
                    statusIsError = false
                    statusMessage = if (currentLanguage == "ha") {
                        "An tura lambar tabbatarwa ta lambobi 6 zuwa ${res.email}. Duba imel ɗinka."
                    } else {
                        "A 6-digit verification code was sent to ${res.email}. Expires in ${res.expiresMinutes} min."
                    }
                }
                is EmailSendResult.FirebaseEmailSent -> {
                    activeFlow = "firebase"
                    cooldownSeconds = 30
                    statusIsError = false
                    statusMessage = if (currentLanguage == "ha") {
                        "An tura saƙon tabbatar da imel zuwa ${res.email}. Duba akwatin imel sannan ka danna Bincika Matsayi."
                    } else {
                        "Verification email sent to ${res.email}. Open your inbox link, then tap Check Status."
                    }
                }
                is EmailSendResult.AlreadyVerified -> {
                    activeFlow = null
                    statusIsError = false
                    statusMessage = null
                    onVerified(res.email)
                }
                is EmailSendResult.NeedsGoogleSignInOrResendKey -> {
                    activeFlow = "needs_auth"
                    statusIsError = false
                    statusMessage = if (currentLanguage == "ha") {
                        "Don tura lambar OTP kafin shiga Google, saita RESEND_API_KEY a AI Studio Secrets — ko ci gaba da Google don tura saƙon tabbatarwa ta Firebase zuwa ${res.email}."
                    } else {
                        "Add RESEND_API_KEY in AI Studio Secrets to email a 6-digit code before Google Sign-In, or verify with Google below to send a Firebase verification email to ${res.email}."
                    }
                }
                is EmailSendResult.Error -> {
                    if (res.cooldownRemainingSeconds > 0) {
                        cooldownSeconds = res.cooldownRemainingSeconds
                    }
                    statusIsError = true
                    statusMessage = res.message
                }
            }
            isSending = false
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = cardColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isVerified) Icons.Filled.MarkEmailRead else Icons.Filled.MarkEmailUnread,
                        contentDescription = null,
                        tint = if (isVerified) WeEmerald else WeBlueLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = if (isVerified) strings.emailVerifiedLabel else strings.emailPendingLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isVerified) WeEmerald else primaryTextColor
                        )
                        Text(
                            text = normalizedEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryTextColor
                        )
                    }
                }

                if (!isVerified) {
                    FilledTonalButton(
                        onClick = ::triggerSendCode,
                        enabled = !isSending && cooldownSeconds == 0,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = WeBlue.copy(alpha = 0.22f),
                            contentColor = if (darkSurface) Color.White else WeBlue
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .testTag("send_email_code_button")
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = if (darkSurface) Color.White else WeBlue
                            )
                        } else {
                            Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = when {
                                    cooldownSeconds > 0 -> "${strings.resendCodeBtn} (${cooldownSeconds}s)"
                                    activeFlow != null -> strings.resendCodeBtn
                                    else -> strings.sendCodeBtn
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = !isVerified && activeFlow == "otp") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = codeInput,
                            onValueChange = { raw ->
                                codeInput = raw.filter { it.isDigit() }.take(6)
                                statusIsError = false
                            },
                            label = { Text(strings.verificationCodeLabel) },
                            placeholder = { Text("123456") },
                            leadingIcon = { Icon(Icons.Filled.Pin, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true,
                            colors = fieldColors,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("email_otp_code_input")
                        )
                        Button(
                            onClick = {
                                when (val check = EmailVerificationService.verifyOtpCode(normalizedEmail, codeInput)) {
                                    is EmailVerifyResult.Verified -> {
                                        statusIsError = false
                                        statusMessage = null
                                        codeInput = ""
                                        onVerified(check.email)
                                    }
                                    is EmailVerifyResult.Invalid -> {
                                        statusIsError = true
                                        statusMessage = check.message
                                    }
                                }
                            },
                            enabled = codeInput.length == 6,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WeEmerald,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("verify_email_code_button")
                        ) {
                            Text(strings.verifyCodeBtn, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            AnimatedVisibility(visible = !isVerified && activeFlow == "firebase") {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isChecking = true
                            when (val res = EmailVerificationService.checkFirebaseEmailVerified(normalizedEmail)) {
                                is EmailVerifyResult.Verified -> {
                                    statusIsError = false
                                    statusMessage = null
                                    onVerified(res.email)
                                }
                                is EmailVerifyResult.Invalid -> {
                                    statusIsError = true
                                    statusMessage = res.message
                                }
                            }
                            isChecking = false
                        }
                    },
                    enabled = !isChecking,
                    border = BorderStroke(1.dp, WeEmerald.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WeEmerald),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .testTag("check_email_verified_button")
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = WeEmerald)
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (currentLanguage == "ha") "Bincika Matsayin Imel" else "I Verified My Email (Check Status)",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            AnimatedVisibility(visible = !isVerified && activeFlow == "needs_auth" && onNeedGoogleSignIn != null) {
                OutlinedButton(
                    onClick = { onNeedGoogleSignIn?.invoke() },
                    border = BorderStroke(1.dp, WePurpleLight.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (darkSurface) Color.White else WePurple
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .testTag("verify_email_google_button")
                ) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (currentLanguage == "ha") {
                            "Tabbatar da Google & Tura Imel"
                        } else {
                            "Verify with Google & Send Email"
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (statusMessage != null) {
                Text(
                    text = statusMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (statusIsError) WeRose else secondaryTextColor
                )
            }
        }
    }
}
