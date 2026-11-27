package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.auth.GoogleAuthHelper
import com.example.data.CountryInfo
import com.example.data.GlobalCountries
import com.example.data.GlobalLanguages
import com.example.localization.AppStrings
import com.example.security.EmailVerificationService
import com.example.ui.components.EmailVerificationCard
import com.example.ui.theme.*

@Composable
fun SignInScreen(
    strings: AppStrings,
    currentLanguage: String,
    initialRememberSession: Boolean = true,
    initialSignInHint: String = "",
    initialErrorMessage: String? = null,
    onConsumeInitialError: () -> Unit = {},
    onToggleLanguage: (String) -> Unit,
    onRememberSessionChanged: (Boolean, String) -> Unit = { _, _ -> },
    onSignInHintPrepared: (PendingSignInHint) -> Unit = {},
    onNavigateToSignUp: (prefillEmailOrUser: String, prefillPhone: String, dialCountry: CountryInfo) -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf(initialErrorMessage) }

    LaunchedEffect(initialErrorMessage) {
        if (!initialErrorMessage.isNullOrBlank()) {
            errorMessage = initialErrorMessage
            onConsumeInitialError()
        }
    }

    val detectedCountry = remember { GlobalCountries.detectDeviceCountry() }
    var signInIdentifier by remember(initialSignInHint) { mutableStateOf(initialSignInHint) }
    var signInDialCountry by remember { mutableStateOf(detectedCountry) }
    var signInPhoneDigits by remember { mutableStateOf("") }
    var signInPassword by remember { mutableStateOf("") }
    var signInPasswordVisible by remember { mutableStateOf(false) }
    var verifiedEmailAddress by remember { mutableStateOf<String?>(null) }
    var rememberSession by remember(initialRememberSession) { mutableStateOf(initialRememberSession) }
    var usePhoneSignIn by remember { mutableStateOf(false) }
    var hasTappedSignIn by remember { mutableStateOf(false) }
    var showSignInDialPicker by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    BackHandler(enabled = usePhoneSignIn) {
        usePhoneSignIn = false
        errorMessage = null
    }

    val darkFieldColors = weAppDarkAuthFieldColors()

    fun validateSignIn(): String? {
        if (!usePhoneSignIn) {
            val id = signInIdentifier.trim()
            if (id.isNotEmpty()) {
                val cleanUser = id.removePrefix("@")
                val validEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(id).matches()
                val validUsername = cleanUser.length >= 3 && cleanUser.matches(Regex("^[a-zA-Z0-9_]+$"))
                if (!validEmail && !validUsername) {
                    return "Enter a valid email address or @username (at least 3 characters)."
                }
            }
        } else {
            val phone = signInPhoneDigits.filter { it.isDigit() }
            if (signInPhoneDigits.isNotBlank() && phone.length < 7) {
                return "Enter a valid phone number (at least 7 digits) or leave it blank."
            }
        }
        if (signInPassword.isNotEmpty() && signInPassword.length < 8) {
            return "Password must be at least 8 characters (or leave blank to verify directly with Google)."
        }
        return null
    }

    fun launchGoogleAuth() {
        isLoading = true
        errorMessage = null
        GoogleAuthHelper.onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isLoading = false
                onAuthSuccess()
            },
            onAuthError = { msg ->
                isLoading = false
                errorMessage = msg
            },
            scope = scope,
            onAuthCancelled = {
                isLoading = false
                errorMessage = "Sign-in was cancelled. Please select a Google account to continue."
            }
        )
    }

    fun validateStrictCredentialsSignIn(): String? {
        if (!usePhoneSignIn) {
            val id = signInIdentifier.trim()
            if (id.isEmpty()) {
                return "Please enter your email address."
            }
            val cleanUser = id.removePrefix("@")
            val validEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(id).matches()
            val validUsername = cleanUser.length >= 3 && cleanUser.matches(Regex("^[a-zA-Z0-9_]+$"))
            if (!validEmail && !validUsername) {
                return "Enter a valid email address or @username (at least 3 characters)."
            }
        } else {
            val phone = signInPhoneDigits.filter { it.isDigit() }
            if (phone.isEmpty()) {
                return "Please enter your phone number."
            }
            if (phone.length < 7) {
                return "Enter a valid phone number (at least 7 digits)."
            }
        }
        if (signInPassword.isEmpty()) {
            return "Please enter your password."
        }
        if (signInPassword.length < 8) {
            return "Password must be at least 8 characters."
        }
        return null
    }

    fun submitCredentialsSignIn() {
        hasTappedSignIn = true
        val validation = validateStrictCredentialsSignIn()
        if (validation != null) {
            errorMessage = validation
        } else {
            val activeIdentifier = if (!usePhoneSignIn) signInIdentifier.trim() else ""
            val activePhone = if (usePhoneSignIn) signInPhoneDigits.trim() else ""
            onRememberSessionChanged(rememberSession, activeIdentifier)
            onSignInHintPrepared(
                PendingSignInHint(
                    identifier = activeIdentifier,
                    dialCountry = signInDialCountry,
                    phoneDigits = activePhone,
                    enteredPassword = signInPassword,
                    rememberSession = rememberSession,
                    verifyCredentialsStrictly = true
                )
            )
            signInPassword = ""
            launchGoogleAuth()
        }
    }

    fun launchGoogleSignInDirect() {
        val activeIdentifier = if (!usePhoneSignIn) signInIdentifier.trim() else ""
        val activePhone = if (usePhoneSignIn) signInPhoneDigits.trim() else ""
        onRememberSessionChanged(rememberSession, activeIdentifier)
        onSignInHintPrepared(
            PendingSignInHint(
                identifier = activeIdentifier,
                dialCountry = signInDialCountry,
                phoneDigits = activePhone,
                enteredPassword = "",
                rememberSession = rememberSession,
                verifyCredentialsStrictly = false
            )
        )
        launchGoogleAuth()
    }

    fun submitSignIn() {
        hasTappedSignIn = true
        val validation = validateSignIn()
        if (validation != null) {
            errorMessage = validation
        } else {
            val activeIdentifier = if (!usePhoneSignIn) signInIdentifier.trim() else ""
            val activePhone = if (usePhoneSignIn) signInPhoneDigits.trim() else ""
            onRememberSessionChanged(rememberSession, activeIdentifier)
            onSignInHintPrepared(
                PendingSignInHint(
                    identifier = activeIdentifier,
                    dialCountry = signInDialCountry,
                    phoneDigits = activePhone,
                    enteredPassword = signInPassword,
                    rememberSession = rememberSession,
                    verifyCredentialsStrictly = false
                )
            )
            signInPassword = ""
            launchGoogleAuth()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF090814), Color(0xFF1D1042), Color(0xFF0D1B44))
                )
            )
            .statusBarsPadding()
            .imePadding()
            .testTag("sign_in_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 560.dp)
                    .align(Alignment.CenterHorizontally)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(GlobalLanguages.supported) { (code, label) ->
                        FilterChip(
                            selected = currentLanguage == code,
                            onClick = { onToggleLanguage(code) },
                            label = { Text(label) }
                        )
                    }
                }

                // Brand Header
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(WeBrandGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(R.drawable.ic_weapp_foreground)
                                .crossfade(true)
                                .build(),
                            contentDescription = strings.appName,
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            contentScale = ContentScale.Fit
                        )
                        Surface(
                            color = Color.Black.copy(alpha = 0.25f),
                            shape = CircleShape,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                        ) {
                            Text(
                                text = "W",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Text(
                        text = strings.signInTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = strings.slogan + " • " + strings.tagline,
                        style = MaterialTheme.typography.labelLarge,
                        color = WePurpleLight,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = strings.signInSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkOnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // Sign In Details Form Card
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF14122B).copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, WePurple.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(18.dp)
                            .animateContentSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Sign In Details",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        if (!usePhoneSignIn) {
                            OutlinedTextField(
                                value = signInIdentifier,
                                onValueChange = {
                                    signInIdentifier = it
                                    hasTappedSignIn = false
                                    errorMessage = null
                                },
                                label = { Text(strings.emailLabel) },
                                placeholder = { Text("you@gmail.com") },
                                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                singleLine = true,
                                colors = darkFieldColors,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signin_identifier_input")
                            )

                            val signInTrimmedId = signInIdentifier.trim()
                            val isEmailVerified = verifiedEmailAddress?.equals(signInTrimmedId, ignoreCase = true) == true
                            if ((hasTappedSignIn || isEmailVerified) && EmailVerificationService.isValidEmail(signInTrimmedId)) {
                                EmailVerificationCard(
                                    email = signInTrimmedId,
                                    isVerified = isEmailVerified,
                                    strings = strings,
                                    currentLanguage = currentLanguage,
                                    darkSurface = true,
                                    onVerified = { verifiedEmail -> verifiedEmailAddress = verifiedEmail },
                                    onNeedGoogleSignIn = ::submitSignIn
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedCard(
                                    onClick = { showSignInDialPicker = true },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = Color.White.copy(alpha = 0.05f),
                                        contentColor = Color.White
                                    ),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
                                    modifier = Modifier
                                        .height(56.dp)
                                        .testTag("signin_dial_code_picker")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            "${signInDialCountry.flag} ${signInDialCountry.dialCode}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Icon(
                                            Icons.Filled.ArrowDropDown,
                                            contentDescription = "Select dial code",
                                            tint = Color.White
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = signInPhoneDigits,
                                    onValueChange = {
                                        signInPhoneDigits = it
                                        errorMessage = null
                                    },
                                    label = { Text(strings.phoneLabel) },
                                    placeholder = { Text("801 234 5678") },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Phone,
                                        imeAction = ImeAction.Next
                                    ),
                                    singleLine = true,
                                    colors = darkFieldColors,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("signin_phone_input")
                                )
                            }
                        }

                        OutlinedTextField(
                            value = signInPassword,
                            onValueChange = {
                                signInPassword = it.take(128)
                                errorMessage = null
                            },
                            label = { Text("${strings.passwordLabel} (Optional)") },
                            placeholder = { Text("Enter your WeApp password") },
                            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(
                                    onClick = { signInPasswordVisible = !signInPasswordVisible },
                                    modifier = Modifier.testTag("signin_password_visibility")
                                ) {
                                    Icon(
                                        imageVector = if (signInPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (signInPasswordVisible) "Hide password" else "Show password",
                                        tint = DarkOnSurfaceVariant
                                    )
                                }
                            },
                            visualTransformation = if (signInPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true,
                            colors = darkFieldColors,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signin_password_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            if (!usePhoneSignIn) {
                                TextButton(
                                    onClick = {
                                        usePhoneSignIn = true
                                        errorMessage = null
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("other_ways_to_sign_in_button")
                                ) {
                                    Text(
                                        text = "Other ways to sign in",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = WeBlueLight,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else {
                                TextButton(
                                    onClick = {
                                        usePhoneSignIn = false
                                        errorMessage = null
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("use_email_instead_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        tint = WeBlueLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Use Email instead",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = WeBlueLight,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    rememberSession = !rememberSession
                                    onRememberSessionChanged(rememberSession, signInIdentifier)
                                }
                                .padding(horizontal = 2.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = rememberSession,
                                onCheckedChange = { checked ->
                                    rememberSession = checked
                                    onRememberSessionChanged(checked, signInIdentifier)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = WePurple,
                                    uncheckedColor = DarkOnSurfaceVariant,
                                    checkmarkColor = Color.White
                                ),
                                modifier = Modifier.testTag("remember_me_checkbox")
                            )
                            Text(
                                text = strings.keepSignedInLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                Button(
                    onClick = ::submitCredentialsSignIn,
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WePurple,
                        contentColor = Color.White,
                        disabledContainerColor = WePurple.copy(alpha = 0.6f),
                        disabledContentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("inline_signin_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = "sign in",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Pinned Bottom Sign In Action Bar + Link to separate Sign Up Screen
            Surface(
                color = Color(0xFF0E0C22).copy(alpha = 0.96f),
                tonalElevation = 8.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp)
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (errorMessage != null) {
                        Surface(
                            color = Color(0xFFDC2626).copy(alpha = 0.25f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, Color(0xFFEF4444)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_error_box")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Filled.ErrorOutline,
                                    contentDescription = "Sign-in error",
                                    tint = Color(0xFFFCA5A5)
                                )
                                Text(
                                    text = errorMessage ?: "",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Button(
                        onClick = ::launchGoogleSignInDirect,
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_sign_in_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1A1A2E),
                            disabledContainerColor = Color.White.copy(alpha = 0.7f),
                            disabledContentColor = Color(0xFF1A1A2E)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = WePurple,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF3F4F6),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "G",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = WeBlue
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = strings.signInWithGoogle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = ::submitSignIn,
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WePurple,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("bottom_signin_button")
                        ) {
                            Icon(Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Sign In", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                onNavigateToSignUp(
                                    if (!usePhoneSignIn) signInIdentifier.trim() else "",
                                    if (usePhoneSignIn) signInPhoneDigits.trim() else "",
                                    signInDialCountry
                                )
                            },
                            enabled = !isLoading,
                            border = BorderStroke(1.dp, WeBlueLight.copy(alpha = 0.65f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("go_to_signup_button")
                        ) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Sign Up", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Text(
                        text = if (currentLanguage == "ha") {
                            "Sabon shiga ne? Danna Sign Up don buɗe sabon shafin rajista."
                        } else {
                            "New to WeApp? Tap Sign Up to open the registration screen."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = DarkOnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showSignInDialPicker) {
        CountryPickerDialog(
            title = "Country Dial Code",
            selectedCode = signInDialCountry.code,
            onDismiss = { showSignInDialPicker = false },
            onSelect = { info ->
                signInDialCountry = info
                showSignInDialPicker = false
            }
        )
    }
}
