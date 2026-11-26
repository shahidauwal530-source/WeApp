package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.example.data.CloudinaryUploader
import com.example.data.CountryInfo
import com.example.data.GlobalCountries
import com.example.data.GlobalLanguages
import com.example.localization.AppStrings
import com.example.security.EmailVerificationService
import com.example.security.PasswordSecurity
import com.example.ui.components.EmailVerificationCard
import com.example.ui.components.WeAppAvatar
import com.example.ui.components.WeAppGradientButton
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun formatDobInput(raw: String): String {
    val digits = raw.filter { it.isDigit() }.take(8)
    return buildString {
        digits.forEachIndexed { index, ch ->
            if (index == 4 || index == 6) append('-')
            append(ch)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DobDatePickerDialog(
    initialDob: String,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val utc = remember { TimeZone.getTimeZone("UTC") }
    val initialMillis = remember(initialDob) {
        runCatching {
            if (initialDob.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = utc }.parse(initialDob)?.time
            } else {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = utc }.parse("2000-01-01")?.time
            }
        }.getOrNull()
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val formatted = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            .apply { timeZone = utc }
                            .format(Date(millis))
                        onDateSelected(formatted)
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

data class PendingSignInHint(
    val identifier: String = "",
    val dialCountry: CountryInfo = GlobalCountries.detectDeviceCountry(),
    val phoneDigits: String = "",
    val enteredPassword: String = "",
    val rememberSession: Boolean = true
)

data class PendingSignUpDraft(
    val username: String = "",
    val displayName: String = "",
    val email: String = "",
    val dialCountry: CountryInfo = GlobalCountries.detectDeviceCountry(),
    val phoneDigits: String = "",
    val dateOfBirth: String = "",
    val gender: String = "Prefer not to say",
    val selectedCountry: CountryInfo = GlobalCountries.detectDeviceCountry(),
    val state: String = GlobalCountries.detectDeviceCountry().defaultState,
    val city: String = GlobalCountries.detectDeviceCountry().defaultCity,
    val bio: String = "",
    val photoUrl: String = "",
    val passwordHash: String = "",
    val passwordSalt: String = "",
    val passwordLockEnabled: Boolean = true,
    val emailVerified: Boolean = false,
    val acceptedTerms: Boolean = false
)

@Composable
fun AuthScreen(
    strings: AppStrings,
    currentLanguage: String,
    initialRememberSession: Boolean = true,
    initialSignInHint: String = "",
    onToggleLanguage: (String) -> Unit,
    onRememberSessionChanged: (Boolean, String) -> Unit = { _, _ -> },
    onSignInHintPrepared: (PendingSignInHint) -> Unit = {},
    onSignUpDraftPrepared: (PendingSignUpDraft) -> Unit = {},
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isSignUpMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // --- Sign In Inputs ---
    val detectedCountry = remember { GlobalCountries.detectDeviceCountry() }
    var signInIdentifier by remember(initialSignInHint) { mutableStateOf(initialSignInHint) }
    var signInDialCountry by remember { mutableStateOf(detectedCountry) }
    var signInPhoneDigits by remember { mutableStateOf("") }
    var signInPassword by remember { mutableStateOf("") }
    var signInPasswordVisible by remember { mutableStateOf(false) }
    var rememberSession by remember(initialRememberSession) { mutableStateOf(initialRememberSession) }
    var showSignInDialPicker by remember { mutableStateOf(false) }

    // --- Sign Up Inputs (all fields required by WeApp) ---
    var signUpUsername by remember { mutableStateOf("") }
    var signUpDisplayName by remember { mutableStateOf("") }
    var signUpEmail by remember { mutableStateOf("") }
    var verifiedEmailAddress by remember { mutableStateOf<String?>(null) }
    var signUpDialCountry by remember { mutableStateOf(detectedCountry) }
    var signUpPhoneDigits by remember { mutableStateOf("") }
    var signUpPassword by remember { mutableStateOf("") }
    var signUpConfirmPassword by remember { mutableStateOf("") }
    var signUpPasswordVisible by remember { mutableStateOf(false) }
    var signUpConfirmPasswordVisible by remember { mutableStateOf(false) }
    var signUpPasswordLock by remember { mutableStateOf(true) }
    var signUpDob by remember { mutableStateOf("") }
    var signUpGender by remember { mutableStateOf("Prefer not to say") }
    var signUpCountry by remember { mutableStateOf(detectedCountry) }
    var signUpState by remember { mutableStateOf(detectedCountry.defaultState) }
    var signUpCity by remember { mutableStateOf(detectedCountry.defaultCity) }
    var signUpBio by remember { mutableStateOf("") }
    var signUpPhotoUrl by remember { mutableStateOf("") }
    var signUpAcceptedTerms by remember { mutableStateOf(false) }
    var showSignUpCountryPicker by remember { mutableStateOf(false) }
    var showSignUpStatePicker by remember { mutableStateOf(false) }
    var showSignUpDialPicker by remember { mutableStateOf(false) }
    var showSignUpDobPicker by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    fun switchAuthMode(targetSignUp: Boolean) {
        if (targetSignUp && !isSignUpMode) {
            val id = signInIdentifier.trim()
            if (id.contains("@") && !id.startsWith("@") && signUpEmail.isBlank()) {
                signUpEmail = id
            } else if (id.isNotBlank() && signUpUsername.isBlank()) {
                signUpUsername = id.removePrefix("@")
            }
            if (signInPhoneDigits.isNotBlank() && signUpPhoneDigits.isBlank()) {
                signUpPhoneDigits = signInPhoneDigits
                signUpDialCountry = signInDialCountry
            }
        } else if (!targetSignUp && isSignUpMode) {
            if (signInIdentifier.isBlank()) {
                signInIdentifier = signUpEmail.ifBlank { signUpUsername }
            }
            if (signInPhoneDigits.isBlank() && signUpPhoneDigits.isNotBlank()) {
                signInPhoneDigits = signUpPhoneDigits
                signInDialCountry = signUpDialCountry
            }
        }
        isSignUpMode = targetSignUp
        errorMessage = null
        scope.launch { scrollState.animateScrollTo(0) }
    }

    val signUpPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            signUpPhotoUrl = uri.toString()
        }
    }

    val darkFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = WePurpleLight,
        unfocusedBorderColor = Color.White.copy(alpha = 0.24f),
        focusedLabelColor = WePurpleLight,
        unfocusedLabelColor = DarkOnSurfaceVariant,
        focusedLeadingIconColor = WePurpleLight,
        unfocusedLeadingIconColor = DarkOnSurfaceVariant,
        focusedPlaceholderColor = DarkOnSurfaceVariant.copy(alpha = 0.7f),
        unfocusedPlaceholderColor = DarkOnSurfaceVariant.copy(alpha = 0.7f),
        cursorColor = WePurpleLight
    )

    fun validateSignIn(): String? {
        val id = signInIdentifier.trim()
        val phone = signInPhoneDigits.filter { it.isDigit() }
        if (id.isNotEmpty()) {
            val cleanUser = id.removePrefix("@")
            val validEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(id).matches()
            val validUsername = cleanUser.length >= 3 && cleanUser.matches(Regex("^[a-zA-Z0-9_]+$"))
            if (!validEmail && !validUsername) {
                return "Enter a valid email address or @username (at least 3 characters)."
            }
        }
        if (signInPhoneDigits.isNotBlank() && phone.length < 7) {
            return "Enter a valid phone number (at least 7 digits) or leave it blank."
        }
        if (signInPassword.isNotEmpty() && signInPassword.length < 8) {
            return "Password must be at least 8 characters (or leave blank to verify directly with Google)."
        }
        return null
    }

    fun validateSignUp(): String? {
        val u = signUpUsername.trim().lowercase().removePrefix("@")
        val n = signUpDisplayName.trim()
        val e = signUpEmail.trim()
        val digits = signUpPhoneDigits.filter { it.isDigit() }
        val passwordErr = PasswordSecurity.validateNewPassword(signUpPassword, signUpConfirmPassword)
        val dob = signUpDob.trim()
        val st = signUpState.trim()
        val c = signUpCity.trim()
        val emailOtpRequired = EmailVerificationService.isOtpEmailApiConfigured()
        val isEmailVerified = verifiedEmailAddress?.equals(e, ignoreCase = true) == true
        return when {
            u.length < 3 || !u.matches(Regex("^[a-z0-9_]+$")) ->
                "Username must be at least 3 characters (letters, numbers, underscores)."
            n.isEmpty() -> "Full name is required."
            !android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches() ->
                "Please enter a valid email address."
            emailOtpRequired && !isEmailVerified ->
                "Please tap Send Code and enter the 6-digit verification code sent to $e."
            digits.length < 7 ->
                "Please enter a valid phone number (at least 7 digits)."
            passwordErr != null -> passwordErr
            !GlobalCountries.isAtLeast13YearsOld(dob) ->
                "Enter a valid Date of Birth (YYYY-MM-DD). You must be at least 13 years old."
            st.isEmpty() -> "Please select a state or region in ${signUpCountry.name}."
            c.isEmpty() -> "City is required."
            !signUpAcceptedTerms ->
                "Please accept the Terms of Service & Privacy Policy to continue."
            else -> null
        }
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

    fun submitAuth() {
        if (isSignUpMode) {
            val validation = validateSignUp()
            if (validation != null) {
                errorMessage = validation
            } else {
                val hashed = PasswordSecurity.hashPassword(signUpPassword)
                onSignUpDraftPrepared(
                    PendingSignUpDraft(
                        username = signUpUsername.trim().lowercase().removePrefix("@"),
                        displayName = signUpDisplayName.trim(),
                        email = signUpEmail.trim(),
                        dialCountry = signUpDialCountry,
                        phoneDigits = signUpPhoneDigits.trim(),
                        dateOfBirth = signUpDob.trim(),
                        gender = signUpGender,
                        selectedCountry = signUpCountry,
                        state = signUpState.trim(),
                        city = signUpCity.trim(),
                        bio = signUpBio.trim(),
                        photoUrl = signUpPhotoUrl,
                        passwordHash = hashed.hash,
                        passwordSalt = hashed.salt,
                        passwordLockEnabled = signUpPasswordLock,
                        emailVerified = verifiedEmailAddress?.equals(signUpEmail.trim(), ignoreCase = true) == true,
                        acceptedTerms = signUpAcceptedTerms
                    )
                )
                signUpPassword = ""
                signUpConfirmPassword = ""
                launchGoogleAuth()
            }
        } else {
            val validation = validateSignIn()
            if (validation != null) {
                errorMessage = validation
            } else {
                onRememberSessionChanged(rememberSession, signInIdentifier)
                onSignInHintPrepared(
                    PendingSignInHint(
                        identifier = signInIdentifier.trim(),
                        dialCountry = signInDialCountry,
                        phoneDigits = signInPhoneDigits.trim(),
                        enteredPassword = signInPassword,
                        rememberSession = rememberSession
                    )
                )
                signInPassword = ""
                launchGoogleAuth()
            }
        }
    }

    val genderOptions = listOf("Male", "Female", "Other", "Prefer not to say")

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
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
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

            // Compact Brand Header
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
                    text = if (isSignUpMode) strings.signUpTitle else strings.signInTitle,
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
                    text = if (isSignUpMode) strings.signUpSubtitle else strings.signInSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkOnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Sign In / Sign Up Mode Toggle
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val signInSelected = !isSignUpMode
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (signInSelected) WePurple else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { switchAuthMode(false) }
                            .testTag("auth_mode_signin_tab")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Sign In",
                                color = Color.White,
                                fontWeight = if (signInSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSignUpMode) WeBlue else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { switchAuthMode(true) }
                            .testTag("auth_mode_signup_tab")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Sign Up",
                                color = Color.White,
                                fontWeight = if (isSignUpMode) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Distinct Sign In vs Sign Up Form Cards
            AnimatedContent(
                targetState = isSignUpMode,
                label = "auth_mode_form"
            ) { signUp ->
                if (!signUp) {
                    // --- SIGN IN SCREEN FORM ---
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFF14122B).copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, WePurple.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Sign In Details",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = signInIdentifier,
                                onValueChange = {
                                    signInIdentifier = it
                                    errorMessage = null
                                },
                                label = { Text(strings.emailOrUsernameLabel) },
                                placeholder = { Text("you@gmail.com or @username") },
                                leadingIcon = { Icon(Icons.Filled.AlternateEmail, contentDescription = null) },
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
                            if (EmailVerificationService.isValidEmail(signInTrimmedId)) {
                                EmailVerificationCard(
                                    email = signInTrimmedId,
                                    isVerified = verifiedEmailAddress?.equals(signInTrimmedId, ignoreCase = true) == true,
                                    strings = strings,
                                    currentLanguage = currentLanguage,
                                    darkSurface = true,
                                    onVerified = { verifiedEmail -> verifiedEmailAddress = verifiedEmail },
                                    onNeedGoogleSignIn = ::submitAuth
                                )
                            }

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
                                    label = { Text("${strings.phoneLabel} (Optional)") },
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
                } else {
                    // --- SIGN UP SCREEN FORM (All required WeApp user inputs) ---
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFF14122B).copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, WeBlue.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "New Account Information",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )

                            // Profile Photo Picker
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                WeAppAvatar(
                                    name = signUpDisplayName.ifBlank { signUpUsername },
                                    size = 56.dp,
                                    photoUrl = signUpPhotoUrl
                                )
                                OutlinedButton(
                                    onClick = {
                                        signUpPhotoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("signup_pick_photo_button")
                                ) {
                                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (signUpPhotoUrl.isNotBlank()) "Change Photo" else "Pick Photo (Optional)")
                                }
                            }

                            val cleanUser = signUpUsername.trim().lowercase().removePrefix("@")
                            val validUserFormat = cleanUser.length >= 3 && cleanUser.matches(Regex("^[a-z0-9_]+$"))
                            OutlinedTextField(
                                value = signUpUsername,
                                onValueChange = { signUpUsername = it; errorMessage = null },
                                label = { Text(strings.usernameLabel) },
                                placeholder = { Text("musa_global") },
                                leadingIcon = { Icon(Icons.Filled.AlternateEmail, contentDescription = null) },
                                supportingText = {
                                    Text(
                                        text = when {
                                            signUpUsername.isBlank() -> "Letters, numbers, underscores (3+ chars)"
                                            !validUserFormat -> "Use 3+ letters, numbers, or underscores only"
                                            else -> "✓ @$cleanUser looks good"
                                        },
                                        color = when {
                                            signUpUsername.isBlank() -> DarkOnSurfaceVariant
                                            !validUserFormat -> WeRose
                                            else -> WeEmerald
                                        }
                                    )
                                },
                                singleLine = true,
                                colors = darkFieldColors,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_username_input")
                            )

                            OutlinedTextField(
                                value = signUpDisplayName,
                                onValueChange = { signUpDisplayName = it; errorMessage = null },
                                label = { Text(strings.displayNameLabel) },
                                placeholder = { Text("e.g. Amina Bello") },
                                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                singleLine = true,
                                colors = darkFieldColors,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_fullname_input")
                            )

                            OutlinedTextField(
                                value = signUpEmail,
                                onValueChange = { signUpEmail = it; errorMessage = null },
                                label = { Text(strings.emailLabel) },
                                placeholder = { Text("name@gmail.com") },
                                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                colors = darkFieldColors,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_email_input")
                            )

                            EmailVerificationCard(
                                email = signUpEmail,
                                isVerified = verifiedEmailAddress?.equals(signUpEmail.trim(), ignoreCase = true) == true,
                                strings = strings,
                                currentLanguage = currentLanguage,
                                darkSurface = true,
                                onVerified = { verifiedEmail -> verifiedEmailAddress = verifiedEmail },
                                onNeedGoogleSignIn = ::submitAuth
                            )

                            val signUpStrength = remember(signUpPassword) { PasswordSecurity.evaluateStrength(signUpPassword) }
                            OutlinedTextField(
                                value = signUpPassword,
                                onValueChange = { signUpPassword = it.take(128); errorMessage = null },
                                label = { Text(strings.passwordLabel) },
                                placeholder = { Text("Minimum 8 chars (letters & numbers)") },
                                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { signUpPasswordVisible = !signUpPasswordVisible },
                                        modifier = Modifier.testTag("signup_password_visibility")
                                    ) {
                                        Icon(
                                            imageVector = if (signUpPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = if (signUpPasswordVisible) "Hide password" else "Show password",
                                            tint = DarkOnSurfaceVariant
                                        )
                                    }
                                },
                                visualTransformation = if (signUpPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Next
                                ),
                                supportingText = {
                                    if (signUpPassword.isNotEmpty()) {
                                        val barColor = when (signUpStrength.score) {
                                            0, 1 -> WeRose
                                            2 -> WeGold
                                            3 -> WeBlue
                                            else -> WeEmerald
                                        }
                                        Text(
                                            text = "Strength: ${signUpStrength.label} • 8+ chars, letters & numbers",
                                            color = barColor
                                        )
                                    } else {
                                        Text("Minimum 8 characters with letters & numbers", color = DarkOnSurfaceVariant)
                                    }
                                },
                                singleLine = true,
                                colors = darkFieldColors,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_password_input")
                            )

                            val passwordsMatch = signUpConfirmPassword.isNotEmpty() && signUpPassword == signUpConfirmPassword
                            val showMismatch = signUpConfirmPassword.isNotEmpty() && signUpPassword != signUpConfirmPassword
                            OutlinedTextField(
                                value = signUpConfirmPassword,
                                onValueChange = { signUpConfirmPassword = it.take(128); errorMessage = null },
                                label = { Text(strings.confirmPasswordLabel) },
                                placeholder = { Text("Re-enter password") },
                                leadingIcon = { Icon(Icons.Filled.VerifiedUser, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { signUpConfirmPasswordVisible = !signUpConfirmPasswordVisible },
                                        modifier = Modifier.testTag("signup_confirm_password_visibility")
                                    ) {
                                        Icon(
                                            imageVector = if (signUpConfirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = if (signUpConfirmPasswordVisible) "Hide password" else "Show password",
                                            tint = DarkOnSurfaceVariant
                                        )
                                    }
                                },
                                visualTransformation = if (signUpConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Next
                                ),
                                isError = showMismatch,
                                supportingText = {
                                    when {
                                        passwordsMatch -> Text("✓ Passwords match", color = WeEmerald)
                                        showMismatch -> Text("Passwords do not match", color = WeRose)
                                        else -> Text("Confirm your WeApp password", color = DarkOnSurfaceVariant)
                                    }
                                },
                                singleLine = true,
                                colors = darkFieldColors,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_confirm_password_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedCard(
                                    onClick = { showSignUpDialPicker = true },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = Color.White.copy(alpha = 0.05f),
                                        contentColor = Color.White
                                    ),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
                                    modifier = Modifier
                                        .height(56.dp)
                                        .testTag("signup_dial_code_picker")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            "${signUpDialCountry.flag} ${signUpDialCountry.dialCode}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Country dial code", tint = Color.White)
                                    }
                                }
                                OutlinedTextField(
                                    value = signUpPhoneDigits,
                                    onValueChange = { signUpPhoneDigits = it; errorMessage = null },
                                    label = { Text(strings.phoneLabel) },
                                    placeholder = { Text("801 234 5678") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    colors = darkFieldColors,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("signup_phone_input")
                                )
                            }

                            val dobValid = signUpDob.isBlank() || GlobalCountries.isAtLeast13YearsOld(signUpDob)
                            OutlinedTextField(
                                value = signUpDob,
                                onValueChange = { signUpDob = formatDobInput(it); errorMessage = null },
                                label = { Text(strings.dobLabel) },
                                placeholder = { Text("1999-05-12") },
                                leadingIcon = { Icon(Icons.Filled.Cake, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { showSignUpDobPicker = true },
                                        modifier = Modifier.testTag("signup_dob_calendar_button")
                                    ) {
                                        Icon(
                                            Icons.Filled.CalendarMonth,
                                            contentDescription = "Pick date of birth",
                                            tint = WePurpleLight
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = signUpDob.length == 10 && !dobValid,
                                supportingText = {
                                    Text(
                                        text = if (signUpDob.length == 10 && !dobValid) {
                                            "Must be a valid date (YYYY-MM-DD) and at least 13 years old"
                                        } else {
                                            "Type YYYYMMDD or tap calendar (13+ required)"
                                        },
                                        color = if (signUpDob.length == 10 && !dobValid) WeRose else DarkOnSurfaceVariant
                                    )
                                },
                                singleLine = true,
                                colors = darkFieldColors,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_dob_input")
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = strings.genderLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = DarkOnSurfaceVariant
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(genderOptions) { option ->
                                        FilterChip(
                                            selected = signUpGender == option,
                                            onClick = { signUpGender = option },
                                            label = { Text(option) },
                                            modifier = Modifier.testTag("signup_gender_${option.lowercase().replace(" ", "_")}")
                                        )
                                    }
                                }
                            }

                            OutlinedCard(
                                onClick = { showSignUpCountryPicker = true },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = Color.White.copy(alpha = 0.05f),
                                    contentColor = Color.White
                                ),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_country_picker")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(signUpCountry.flag, style = MaterialTheme.typography.headlineSmall)
                                        Column {
                                            Text(
                                                strings.countryLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = DarkOnSurfaceVariant
                                            )
                                            Text(
                                                "${signUpCountry.name} (${signUpCountry.currencyCode})",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = strings.countryLabel, tint = Color.White)
                                }
                            }

                            OutlinedCard(
                                onClick = { showSignUpStatePicker = true },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = Color.White.copy(alpha = 0.05f),
                                    contentColor = Color.White
                                ),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_state_picker")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Filled.Map,
                                            contentDescription = null,
                                            tint = WePurpleLight
                                        )
                                        Column {
                                            Text(
                                                "${strings.stateLabel} • ${signUpCountry.name}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = DarkOnSurfaceVariant
                                            )
                                            Text(
                                                signUpState.ifBlank { strings.selectStateTitle },
                                                style = MaterialTheme.typography.titleMedium,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = strings.selectStateTitle, tint = Color.White)
                                }
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(signUpCountry.states.take(8), key = { it }) { stateOption ->
                                    FilterChip(
                                        selected = signUpState.equals(stateOption, ignoreCase = true),
                                        onClick = {
                                            signUpState = stateOption
                                            errorMessage = null
                                        },
                                        label = { Text(stateOption) },
                                        modifier = Modifier.testTag("signup_state_chip_${stateOption.lowercase().replace(" ", "_")}")
                                    )
                                }
                                if (signUpCountry.states.size > 8) {
                                    item(key = "all_states_chip") {
                                        AssistChip(
                                            onClick = { showSignUpStatePicker = true },
                                            label = { Text("All (${signUpCountry.states.size})") }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = signUpCity,
                                onValueChange = { signUpCity = it; errorMessage = null },
                                label = { Text(strings.cityLabel) },
                                leadingIcon = { Icon(Icons.Filled.LocationCity, contentDescription = null) },
                                singleLine = true,
                                colors = darkFieldColors,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_city_input")
                            )

                            OutlinedTextField(
                                value = signUpBio,
                                onValueChange = { signUpBio = it.take(200) },
                                label = { Text("${strings.bioLabel} (Optional)") },
                                leadingIcon = { Icon(Icons.Filled.EditNote, contentDescription = null) },
                                maxLines = 2,
                                colors = darkFieldColors,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_bio_input")
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { signUpPasswordLock = !signUpPasswordLock }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = signUpPasswordLock,
                                    onCheckedChange = { signUpPasswordLock = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = WePurple,
                                        uncheckedColor = DarkOnSurfaceVariant,
                                        checkmarkColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("signup_password_lock_checkbox")
                                )
                                Text(
                                    text = strings.requirePasswordLoginLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        signUpAcceptedTerms = !signUpAcceptedTerms
                                        errorMessage = null
                                    }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = signUpAcceptedTerms,
                                    onCheckedChange = {
                                        signUpAcceptedTerms = it
                                        errorMessage = null
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = WeBlue,
                                        uncheckedColor = DarkOnSurfaceVariant,
                                        checkmarkColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("signup_terms_checkbox")
                                )
                                Text(
                                    text = "I agree to the WeApp Terms of Service & Privacy Policy and confirm I am 13+.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Surface(
                    color = WeRose.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, WeRose.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = WeRose)
                        Text(
                            text = errorMessage ?: "",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            } // End scrollable form Column

            // Pinned Bottom Sign In & Sign Up Action Bar
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
                    Button(
                        onClick = ::submitAuth,
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
                                text = if (isSignUpMode) strings.signUpWithGoogle else strings.signInWithGoogle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val signInActive = !isSignUpMode
                        if (signInActive) {
                            Button(
                                onClick = { submitAuth() },
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
                        } else {
                            OutlinedButton(
                                onClick = { switchAuthMode(false) },
                                enabled = !isLoading,
                                border = BorderStroke(1.dp, WePurpleLight.copy(alpha = 0.65f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("switch_to_signin_button")
                            ) {
                                Icon(Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Sign In", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (isSignUpMode) {
                            Button(
                                onClick = { submitAuth() },
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WeBlue,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("bottom_signup_button")
                            ) {
                                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Sign Up", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { switchAuthMode(true) },
                                enabled = !isLoading,
                                border = BorderStroke(1.dp, WeBlueLight.copy(alpha = 0.65f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("switch_to_signup_button")
                            ) {
                                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Sign Up", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Text(
                        text = "Protected with PBKDF2 hashing & Google verification before saving to database.",
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

    if (showSignUpCountryPicker) {
        CountryPickerDialog(
            title = strings.countryLabel,
            selectedCode = signUpCountry.code,
            onDismiss = { showSignUpCountryPicker = false },
            onSelect = { info ->
                signUpCountry = info
                signUpState = info.defaultState
                signUpCity = info.defaultCity
                if (signUpPhoneDigits.isBlank()) {
                    signUpDialCountry = info
                }
                showSignUpCountryPicker = false
            }
        )
    }

    if (showSignUpStatePicker) {
        StatePickerDialog(
            title = strings.selectStateTitle,
            country = signUpCountry,
            selectedState = signUpState,
            onDismiss = { showSignUpStatePicker = false },
            onSelect = { st ->
                signUpState = st
                errorMessage = null
                showSignUpStatePicker = false
            }
        )
    }

    if (showSignUpDialPicker) {
        CountryPickerDialog(
            title = "Country Dial Code",
            selectedCode = signUpDialCountry.code,
            onDismiss = { showSignUpDialPicker = false },
            onSelect = { info ->
                signUpDialCountry = info
                showSignUpDialPicker = false
            }
        )
    }

    if (showSignUpDobPicker) {
        DobDatePickerDialog(
            initialDob = signUpDob,
            onDismiss = { showSignUpDobPicker = false },
            onDateSelected = { selected ->
                signUpDob = selected
                errorMessage = null
                showSignUpDobPicker = false
            }
        )
    }
}

@Composable
fun ProfileSetupScreen(
    firebaseUser: FirebaseUser,
    cloudinaryCloudName: String,
    cloudinaryUploadPreset: String,
    strings: AppStrings,
    currentLanguage: String = "en",
    isSubmitting: Boolean,
    initialSignUpDraft: PendingSignUpDraft? = null,
    initialSignInHint: PendingSignInHint? = null,
    onCheckUsername: suspend (String) -> Boolean,
    onSaveCloudinaryConfig: (String, String) -> Unit,
    onShowMessage: (String) -> Unit,
    onSignOut: () -> Unit,
    onSubmitProfile: (
        username: String,
        displayName: String,
        email: String,
        phone: String,
        country: CountryInfo,
        state: String,
        city: String,
        bio: String,
        dateOfBirth: String,
        gender: String,
        photoUrl: String,
        passwordHash: String,
        passwordSalt: String,
        passwordLockEnabled: Boolean,
        emailVerified: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val defaultEmail = initialSignUpDraft?.email?.ifBlank { null }
        ?: initialSignInHint?.identifier?.takeIf { it.contains("@") && !it.startsWith("@") }
        ?: firebaseUser.email
        ?: ""
    val defaultName = initialSignUpDraft?.displayName?.ifBlank { null }
        ?: firebaseUser.displayName
        ?: ""
    val defaultPhoto = initialSignUpDraft?.photoUrl?.ifBlank { null }
        ?: firebaseUser.photoUrl?.toString()
        ?: ""
    val detectedCountry = remember(initialSignUpDraft) {
        initialSignUpDraft?.selectedCountry ?: GlobalCountries.detectDeviceCountry()
    }
    val initialDialCountry = remember(initialSignUpDraft, initialSignInHint) {
        initialSignUpDraft?.dialCountry ?: initialSignInHint?.dialCountry ?: detectedCountry
    }
    val suggestedUsername = remember(initialSignUpDraft, initialSignInHint, defaultEmail, defaultName) {
        val explicit = initialSignUpDraft?.username?.ifBlank { null }
            ?: initialSignInHint?.identifier?.takeIf { !it.contains("@") || it.startsWith("@") }
                ?.removePrefix("@")
        val source = explicit ?: defaultEmail.substringBefore("@").ifBlank { defaultName }
        val base = source
            .lowercase()
            .replace(Regex("[^a-z0-9_]"), "_")
            .trim('_')
        if (base.length >= 3) base.take(20) else "weapp_user"
    }

    var username by remember(suggestedUsername) { mutableStateOf(suggestedUsername) }
    var isCheckingUsername by remember { mutableStateOf(false) }
    var isUsernameAvailable by remember { mutableStateOf<Boolean?>(null) }

    var displayName by remember(defaultName) { mutableStateOf(defaultName) }
    var email by remember(defaultEmail) { mutableStateOf(defaultEmail) }
    var verifiedEmailAddress by remember(initialSignUpDraft, defaultEmail) {
        val preVerified = when {
            initialSignUpDraft?.emailVerified == true && initialSignUpDraft.email.isNotBlank() ->
                initialSignUpDraft.email.trim().lowercase()
            firebaseUser.isEmailVerified && !firebaseUser.email.isNullOrBlank() ->
                firebaseUser.email!!.trim().lowercase()
            else -> null
        }
        mutableStateOf(preVerified)
    }
    var dialCountry by remember(initialDialCountry) { mutableStateOf(initialDialCountry) }
    var phoneDigits by remember(initialSignUpDraft, initialSignInHint) {
        mutableStateOf(
            initialSignUpDraft?.phoneDigits?.ifBlank { null }
                ?: initialSignInHint?.phoneDigits
                ?: ""
        )
    }
    val hasPreHashedPassword = remember(initialSignUpDraft) {
        !initialSignUpDraft?.passwordHash.isNullOrBlank() && !initialSignUpDraft?.passwordSalt.isNullOrBlank()
    }
    var setupPassword by remember(initialSignInHint) {
        mutableStateOf(initialSignInHint?.enteredPassword ?: "")
    }
    var setupConfirmPassword by remember { mutableStateOf("") }
    var setupPasswordVisible by remember { mutableStateOf(false) }
    var setupConfirmPasswordVisible by remember { mutableStateOf(false) }
    var setupPasswordLock by remember(initialSignUpDraft) {
        mutableStateOf(initialSignUpDraft?.passwordLockEnabled ?: true)
    }
    var dateOfBirth by remember(initialSignUpDraft) {
        mutableStateOf(initialSignUpDraft?.dateOfBirth ?: "")
    }
    var gender by remember(initialSignUpDraft) {
        mutableStateOf(initialSignUpDraft?.gender ?: "Prefer not to say")
    }
    var selectedCountry by remember(detectedCountry) { mutableStateOf(detectedCountry) }
    var selectedState by remember(initialSignUpDraft, detectedCountry) {
        mutableStateOf(initialSignUpDraft?.state?.ifBlank { null } ?: detectedCountry.defaultState)
    }
    var city by remember(initialSignUpDraft, detectedCountry) {
        mutableStateOf(initialSignUpDraft?.city?.ifBlank { null } ?: detectedCountry.defaultCity)
    }
    var bio by remember(initialSignUpDraft) { mutableStateOf(initialSignUpDraft?.bio ?: "") }
    var photoUrl by remember(defaultPhoto) { mutableStateOf(defaultPhoto) }

    var showCloudinaryPanel by remember { mutableStateOf(false) }
    var cloudNameInput by remember(cloudinaryCloudName) { mutableStateOf(cloudinaryCloudName) }
    var uploadPresetInput by remember(cloudinaryUploadPreset) { mutableStateOf(cloudinaryUploadPreset) }
    var uploadingPhoto by remember { mutableStateOf(false) }

    var acceptedTerms by remember(initialSignUpDraft) {
        mutableStateOf(initialSignUpDraft?.acceptedTerms ?: false)
    }
    var showCountryPicker by remember { mutableStateOf(false) }
    var showStatePicker by remember { mutableStateOf(false) }
    var showDialPicker by remember { mutableStateOf(false) }
    var showDobPicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Live username availability check
    LaunchedEffect(username) {
        val clean = username.trim().lowercase()
        if (clean.length < 3 || !clean.matches(Regex("^[a-z0-9_]+$"))) {
            isCheckingUsername = false
            isUsernameAvailable = null
            return@LaunchedEffect
        }
        isCheckingUsername = true
        delay(350)
        isUsernameAvailable = onCheckUsername(clean)
        isCheckingUsername = false
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onSaveCloudinaryConfig(cloudNameInput, uploadPresetInput)
            if (cloudNameInput.isNotBlank() && uploadPresetInput.isNotBlank()) {
                uploadingPhoto = true
                scope.launch {
                    val res = CloudinaryUploader.uploadImage(context, uri, cloudNameInput, uploadPresetInput)
                    uploadingPhoto = false
                    res.fold(
                        onSuccess = { url ->
                            photoUrl = url
                            onShowMessage("Profile photo uploaded to Cloudinary!")
                        },
                        onFailure = { e ->
                            onShowMessage(e.localizedMessage ?: "Cloudinary upload failed")
                        }
                    )
                }
            } else {
                photoUrl = uri.toString()
                onShowMessage("Photo selected locally. Add Cloudinary preset below if you want a cloud URL.")
            }
        }
    }

    var autoSubmittedDraft by remember(initialSignUpDraft) { mutableStateOf(false) }

    fun validateAndSubmit() {
        val u = username.trim().lowercase()
        val n = displayName.trim()
        val e = email.trim()
        val digits = phoneDigits.filter { it.isDigit() }
        val passwordErr = if (!hasPreHashedPassword) {
            PasswordSecurity.validateNewPassword(setupPassword, setupConfirmPassword)
        } else null
        val dob = dateOfBirth.trim()
        val st = selectedState.trim()
        val c = city.trim()
        validationError = when {
            u.length < 3 || !u.matches(Regex("^[a-z0-9_]+$")) ->
                "Username must be at least 3 characters (letters, numbers, underscores)."
            isCheckingUsername ->
                "Checking username availability, please wait..."
            isUsernameAvailable == false ->
                "@$u is already taken. Please pick another username."
            n.isEmpty() -> "Full name is required."
            !android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches() ->
                "Please enter a valid email address."
            digits.length < 7 ->
                "Please enter a valid phone number (at least 7 digits)."
            passwordErr != null -> passwordErr
            !GlobalCountries.isAtLeast13YearsOld(dob) ->
                "Enter a valid Date of Birth (YYYY-MM-DD). You must be at least 13 years old."
            st.isEmpty() -> "Please select a state or region in ${selectedCountry.name}."
            c.isEmpty() -> "City is required."
            !acceptedTerms -> "Please accept the Terms of Service & Privacy Policy to continue."
            else -> null
        }
        if (validationError == null) {
            val fullPhone = "${dialCountry.dialCode} ${phoneDigits.trim()}"
            val finalHash: String
            val finalSalt: String
            if (hasPreHashedPassword) {
                finalHash = initialSignUpDraft?.passwordHash.orEmpty()
                finalSalt = initialSignUpDraft?.passwordSalt.orEmpty()
            } else {
                val hashed = PasswordSecurity.hashPassword(setupPassword)
                finalHash = hashed.hash
                finalSalt = hashed.salt
                setupPassword = ""
                setupConfirmPassword = ""
            }
            val isEmailVerified = verifiedEmailAddress?.equals(e, ignoreCase = true) == true ||
                (firebaseUser.isEmailVerified && firebaseUser.email.equals(e, ignoreCase = true))
            onSubmitProfile(
                u, n, e, fullPhone, selectedCountry, st, c, bio.trim(),
                dob, gender, photoUrl, finalHash, finalSalt, setupPasswordLock, isEmailVerified
            )
        }
    }

    LaunchedEffect(initialSignUpDraft, isUsernameAvailable, isCheckingUsername, uploadingPhoto, isSubmitting) {
        if (
            initialSignUpDraft != null &&
            !autoSubmittedDraft &&
            isUsernameAvailable == true &&
            !isCheckingUsername &&
            !uploadingPhoto &&
            !isSubmitting
        ) {
            autoSubmittedDraft = true
            validateAndSubmit()
        }
    }

    val genderOptions = listOf("Male", "Female", "Other", "Prefer not to say")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.completeProfileTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onSignOut) {
                    Text(strings.logoutLabel, color = WeRose)
                }
            }

            Text(
                text = strings.completeProfileSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Profile Photo Preview + Photo Picker (+ Optional Cloudinary)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        WeAppAvatar(
                            name = displayName.ifBlank { username },
                            size = 64.dp,
                            photoUrl = photoUrl
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                enabled = !uploadingPhoto,
                                modifier = Modifier.fillMaxWidth().testTag("setup_pick_photo_button")
                            ) {
                                Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (photoUrl.isNotBlank()) "Change Profile Photo" else "Pick Profile Photo (Optional)")
                            }
                            TextButton(
                                onClick = { showCloudinaryPanel = !showCloudinaryPanel },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    if (showCloudinaryPanel) "Hide Cloudinary Config" else "Cloudinary Upload Config (Optional)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = WePurpleLight
                                )
                            }
                        }
                    }
                    if (uploadingPhoto) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = WePurple)
                    }
                    if (showCloudinaryPanel) {
                        OutlinedTextField(
                            value = cloudNameInput,
                            onValueChange = { cloudNameInput = it },
                            label = { Text("Cloudinary Cloud Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uploadPresetInput,
                            onValueChange = { uploadPresetInput = it },
                            label = { Text("Cloudinary Unsigned Upload Preset") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Username with Live Availability Check
            val cleanUser = username.trim().lowercase()
            val validFormat = cleanUser.length >= 3 && cleanUser.matches(Regex("^[a-z0-9_]+$"))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it; validationError = null },
                label = { Text(strings.usernameLabel) },
                leadingIcon = { Icon(Icons.Filled.AlternateEmail, null) },
                trailingIcon = {
                    when {
                        isCheckingUsername -> CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = WePurple
                        )
                        validFormat && isUsernameAvailable == true -> Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Username available",
                            tint = WeEmerald
                        )
                        validFormat && isUsernameAvailable == false -> Icon(
                            Icons.Filled.Cancel,
                            contentDescription = "Username taken",
                            tint = WeRose
                        )
                    }
                },
                supportingText = {
                    when {
                        username.isBlank() -> Text("Letters, numbers, underscores (3+ chars)")
                        !validFormat -> Text("Use 3+ letters, numbers, or underscores only", color = WeRose)
                        isCheckingUsername -> Text("Checking availability...")
                        isUsernameAvailable == true -> Text("✓ @$cleanUser is available", color = WeEmerald)
                        isUsernameAvailable == false -> Text("✕ @$cleanUser is already taken", color = WeRose)
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_username_input"),
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it; validationError = null },
                label = { Text(strings.displayNameLabel) },
                leadingIcon = { Icon(Icons.Filled.Person, null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_name_input"),
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; validationError = null },
                label = { Text(strings.emailLabel) },
                leadingIcon = { Icon(Icons.Filled.Email, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_email_input"),
                shape = RoundedCornerShape(14.dp)
            )

            EmailVerificationCard(
                email = email,
                isVerified = verifiedEmailAddress?.equals(email.trim(), ignoreCase = true) == true ||
                    (firebaseUser.isEmailVerified && firebaseUser.email.equals(email.trim(), ignoreCase = true)),
                strings = strings,
                currentLanguage = currentLanguage,
                darkSurface = false,
                onVerified = { verifiedEmail -> verifiedEmailAddress = verifiedEmail }
            )

            if (hasPreHashedPassword) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = WeEmerald.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, WeEmerald.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = WeEmerald)
                        Text(
                            text = "Security password hashed & ready to sync to database.",
                            style = MaterialTheme.typography.bodySmall,
                            color = WeEmerald,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                val setupStrength = remember(setupPassword) { PasswordSecurity.evaluateStrength(setupPassword) }
                OutlinedTextField(
                    value = setupPassword,
                    onValueChange = { setupPassword = it.take(128); validationError = null },
                    label = { Text(strings.passwordLabel) },
                    placeholder = { Text("Minimum 8 chars (letters & numbers)") },
                    leadingIcon = { Icon(Icons.Filled.Lock, null) },
                    trailingIcon = {
                        IconButton(
                            onClick = { setupPasswordVisible = !setupPasswordVisible },
                            modifier = Modifier.testTag("setup_password_visibility")
                        ) {
                            Icon(
                                imageVector = if (setupPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (setupPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (setupPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    supportingText = {
                        if (setupPassword.isNotEmpty()) {
                            val color = when (setupStrength.score) {
                                0, 1 -> WeRose
                                2 -> WeGold
                                3 -> WeBlue
                                else -> WeEmerald
                            }
                            Text("Strength: ${setupStrength.label} • 8+ chars, letters & numbers", color = color)
                        } else {
                            Text("Minimum 8 characters with letters & numbers")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_password_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                val setupMatch = setupConfirmPassword.isNotEmpty() && setupPassword == setupConfirmPassword
                val setupMismatch = setupConfirmPassword.isNotEmpty() && setupPassword != setupConfirmPassword
                OutlinedTextField(
                    value = setupConfirmPassword,
                    onValueChange = { setupConfirmPassword = it.take(128); validationError = null },
                    label = { Text(strings.confirmPasswordLabel) },
                    placeholder = { Text("Re-enter password") },
                    leadingIcon = { Icon(Icons.Filled.VerifiedUser, null) },
                    trailingIcon = {
                        IconButton(
                            onClick = { setupConfirmPasswordVisible = !setupConfirmPasswordVisible },
                            modifier = Modifier.testTag("setup_confirm_password_visibility")
                        ) {
                            Icon(
                                imageVector = if (setupConfirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (setupConfirmPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (setupConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    isError = setupMismatch,
                    supportingText = {
                        when {
                            setupMatch -> Text("✓ Passwords match", color = WeEmerald)
                            setupMismatch -> Text("Passwords do not match", color = WeRose)
                            else -> Text("Confirm your WeApp password")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_confirm_password_input"),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            // Phone with Country Dial Code Picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedCard(
                    onClick = { showDialPicker = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(56.dp)
                        .testTag("setup_dial_code_picker")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("${dialCountry.flag} ${dialCountry.dialCode}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Country dial code")
                    }
                }
                OutlinedTextField(
                    value = phoneDigits,
                    onValueChange = { phoneDigits = it; validationError = null },
                    label = { Text(strings.phoneLabel) },
                    placeholder = { Text("801 234 5678") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("setup_phone_input"),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            // Date of Birth (13+ validation)
            val dobValid = dateOfBirth.isBlank() || GlobalCountries.isAtLeast13YearsOld(dateOfBirth)
            OutlinedTextField(
                value = dateOfBirth,
                onValueChange = { dateOfBirth = formatDobInput(it); validationError = null },
                label = { Text(strings.dobLabel) },
                placeholder = { Text("1999-05-12") },
                leadingIcon = { Icon(Icons.Filled.Cake, null) },
                trailingIcon = {
                    IconButton(
                        onClick = { showDobPicker = true },
                        modifier = Modifier.testTag("setup_dob_calendar_button")
                    ) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = "Pick date of birth")
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = dateOfBirth.length == 10 && !dobValid,
                supportingText = {
                    if (dateOfBirth.length == 10 && !dobValid) {
                        Text("Must be a valid date (YYYY-MM-DD) and at least 13 years old", color = WeRose)
                    } else {
                        Text("Type YYYYMMDD or tap calendar (13+ required)")
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_dob_input"),
                shape = RoundedCornerShape(14.dp)
            )

            // Gender Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(strings.genderLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(genderOptions) { option ->
                        FilterChip(
                            selected = gender == option,
                            onClick = { gender = option },
                            label = { Text(option) },
                            modifier = Modifier.testTag("setup_gender_${option.lowercase().replace(" ", "_")}")
                        )
                    }
                }
            }

            // Country Picker (Flag + Currency)
            OutlinedCard(
                onClick = { showCountryPicker = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_country_picker")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(selectedCountry.flag, style = MaterialTheme.typography.headlineSmall)
                        Column {
                            Text(strings.countryLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${selectedCountry.name} (${selectedCountry.currencyCode})",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = strings.countryLabel)
                }
            }

            // State / Region Picker (Scoped to selected country)
            OutlinedCard(
                onClick = { showStatePicker = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_state_picker")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Map, contentDescription = null, tint = WePurpleLight)
                        Column {
                            Text(
                                "${strings.stateLabel} • ${selectedCountry.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                selectedState.ifBlank { strings.selectStateTitle },
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = strings.selectStateTitle)
                }
            }

            OutlinedTextField(
                value = city,
                onValueChange = { city = it; validationError = null },
                label = { Text(strings.cityLabel) },
                leadingIcon = { Icon(Icons.Filled.LocationCity, null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_city_input"),
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it.take(200) },
                label = { Text(strings.bioLabel) },
                leadingIcon = { Icon(Icons.Filled.EditNote, null) },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_bio_input"),
                shape = RoundedCornerShape(14.dp)
            )

            // Terms & Privacy Checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        acceptedTerms = !acceptedTerms
                        validationError = null
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = acceptedTerms,
                    onCheckedChange = {
                        acceptedTerms = it
                        validationError = null
                    },
                    modifier = Modifier.testTag("setup_terms_checkbox")
                )
                Text(
                    text = "I agree to the WeApp Terms of Service & Privacy Policy and confirm I am 13+.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (validationError != null) {
                Text(
                    text = validationError ?: "",
                    color = WeRose,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(6.dp))

            WeAppGradientButton(
                text = if (isSubmitting) "..." else strings.createAccountBtn,
                enabled = !isSubmitting && !uploadingPhoto && !isCheckingUsername,
                onClick = ::validateAndSubmit,
                modifier = Modifier.fillMaxWidth(),
                testTag = "complete_signup_button"
            )
        }
    }

    if (showCountryPicker) {
        CountryPickerDialog(
            title = strings.countryLabel,
            selectedCode = selectedCountry.code,
            onDismiss = { showCountryPicker = false },
            onSelect = { info ->
                selectedCountry = info
                selectedState = info.defaultState
                city = info.defaultCity
                if (phoneDigits.isBlank()) {
                    dialCountry = info
                }
                showCountryPicker = false
            }
        )
    }

    if (showStatePicker) {
        StatePickerDialog(
            title = strings.selectStateTitle,
            country = selectedCountry,
            selectedState = selectedState,
            onDismiss = { showStatePicker = false },
            onSelect = { st ->
                selectedState = st
                validationError = null
                showStatePicker = false
            }
        )
    }

    if (showDialPicker) {
        CountryPickerDialog(
            title = "Country Dial Code",
            selectedCode = dialCountry.code,
            onDismiss = { showDialPicker = false },
            onSelect = { info ->
                dialCountry = info
                showDialPicker = false
            }
        )
    }

    if (showDobPicker) {
        DobDatePickerDialog(
            initialDob = dateOfBirth,
            onDismiss = { showDobPicker = false },
            onDateSelected = { selected ->
                dateOfBirth = selected
                validationError = null
                showDobPicker = false
            }
        )
    }
}

@Composable
fun CountryPickerDialog(
    title: String,
    selectedCode: String,
    onDismiss: () -> Unit,
    onSelect: (CountryInfo) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(GlobalCountries.all, key = { it.code }) { c ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(c) }
                            .background(
                                if (c.code == selectedCode) WePurple.copy(alpha = 0.16f)
                                else Color.Transparent
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(c.flag, style = MaterialTheme.typography.titleLarge)
                            Column {
                                Text(c.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${c.dialCode} • ${c.defaultCity} • ${c.currencyCode} (${c.currencySymbol})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (c.code == selectedCode) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = WePurpleLight)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun StatePickerDialog(
    title: String,
    country: CountryInfo,
    selectedState: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var searchQuery by remember(country.code) { mutableStateOf("") }
    val filteredStates = remember(country.code, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) country.states
        else country.states.filter { it.contains(q, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("${country.flag} $title (${country.name})")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search state in ${country.name}...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("state_search_input")
                )

                LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredStates, key = { it }) { stateName ->
                        val isSelected = stateName.equals(selectedState, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelect(stateName) }
                                .background(
                                    if (isSelected) WePurple.copy(alpha = 0.16f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stateName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (isSelected) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = WePurpleLight)
                            }
                        }
                    }

                    val trimmed = searchQuery.trim()
                    if (trimmed.isNotEmpty() && filteredStates.none { it.equals(trimmed, ignoreCase = true) }) {
                        item(key = "custom_state_option") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelect(trimmed) }
                                    .background(WeBlue.copy(alpha = 0.14f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Use \"$trimmed\"",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = WeBlueLight
                                )
                                Icon(Icons.Filled.AddCircleOutline, contentDescription = null, tint = WeBlueLight)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun PasswordUnlockGate(
    displayName: String,
    email: String,
    passwordHash: String,
    passwordSalt: String,
    strings: AppStrings,
    initialError: String? = null,
    isSubmitting: Boolean = false,
    onUnlocked: () -> Unit,
    onResetPasswordWithGoogle: (newPassword: String, confirmPassword: String, onDone: () -> Unit) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var passwordInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMsg by remember(initialError) { mutableStateOf(initialError) }
    var isResetMode by remember { mutableStateOf(false) }
    var isReauthenticating by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showNewPwd by remember { mutableStateOf(false) }
    var showConfirmPwd by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF090814), Color(0xFF1D1042), Color(0xFF0D1B44))
                )
            )
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF14122B).copy(alpha = 0.94f),
            border = BorderStroke(1.dp, WePurple.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = WePurple.copy(alpha = 0.18f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isResetMode) Icons.Filled.LockReset else Icons.Filled.Lock,
                            contentDescription = null,
                            tint = WePurpleLight,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Text(
                    text = if (isResetMode) strings.updatePasswordTitle else strings.unlockAccountTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (isResetMode) {
                        "Google identity verified for $email. Set a new WeApp password below."
                    } else {
                        "${strings.unlockAccountSubtitle}\n${displayName.ifBlank { email }}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkOnSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = WePurpleLight,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.24f),
                    focusedLabelColor = WePurpleLight,
                    unfocusedLabelColor = DarkOnSurfaceVariant,
                    focusedLeadingIconColor = WePurpleLight,
                    unfocusedLeadingIconColor = DarkOnSurfaceVariant,
                    cursorColor = WePurpleLight
                )

                if (!isResetMode) {
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it.take(128); errorMsg = null },
                        label = { Text(strings.passwordLabel) },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (showPassword) "Hide password" else "Show password",
                                    tint = DarkOnSurfaceVariant
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("unlock_password_input")
                    )

                    if (errorMsg != null) {
                        Text(errorMsg ?: "", color = WeRose, style = MaterialTheme.typography.bodySmall)
                    }

                    WeAppGradientButton(
                        text = "Unlock Account",
                        testTag = "unlock_account_button",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (passwordInput.isEmpty()) {
                                errorMsg = "Enter your WeApp password."
                            } else if (PasswordSecurity.verifyPassword(passwordInput, passwordHash, passwordSalt)) {
                                passwordInput = ""
                                onUnlocked()
                            } else {
                                errorMsg = "Incorrect WeApp password. Try again or reset via Google."
                            }
                        }
                    )

                    TextButton(
                        onClick = {
                            isReauthenticating = true
                            errorMsg = null
                            GoogleAuthHelper.onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isReauthenticating = false
                                    isResetMode = true
                                },
                                onAuthError = { msg ->
                                    isReauthenticating = false
                                    errorMsg = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isReauthenticating = false
                                    errorMsg = "Google verification cancelled."
                                }
                            )
                        },
                        enabled = !isReauthenticating
                    ) {
                        Text(
                            text = if (isReauthenticating) "Verifying Google..." else "Forgot Password? Reset with Google",
                            color = WePurpleLight
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it.take(128); errorMsg = null },
                        label = { Text(strings.newPasswordLabel) },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showNewPwd = !showNewPwd }) {
                                Icon(
                                    if (showNewPwd) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (showNewPwd) "Hide password" else "Show password",
                                    tint = DarkOnSurfaceVariant
                                )
                            }
                        },
                        visualTransformation = if (showNewPwd) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_new_password_input")
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it.take(128); errorMsg = null },
                        label = { Text(strings.confirmPasswordLabel) },
                        leadingIcon = { Icon(Icons.Filled.VerifiedUser, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showConfirmPwd = !showConfirmPwd }) {
                                Icon(
                                    if (showConfirmPwd) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (showConfirmPwd) "Hide password" else "Show password",
                                    tint = DarkOnSurfaceVariant
                                )
                            }
                        },
                        visualTransformation = if (showConfirmPwd) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_confirm_password_input")
                    )

                    if (errorMsg != null) {
                        Text(errorMsg ?: "", color = WeRose, style = MaterialTheme.typography.bodySmall)
                    }

                    WeAppGradientButton(
                        text = if (isSubmitting) "..." else strings.updatePasswordBtn,
                        enabled = !isSubmitting,
                        testTag = "reset_password_submit_button",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val err = PasswordSecurity.validateNewPassword(newPassword, confirmPassword)
                            if (err != null) {
                                errorMsg = err
                            } else {
                                onResetPasswordWithGoogle(newPassword, confirmPassword) {
                                    newPassword = ""
                                    confirmPassword = ""
                                    onUnlocked()
                                }
                            }
                        }
                    )
                }

                TextButton(onClick = onSignOut) {
                    Text(strings.logoutLabel, color = WeRose)
                }
            }
        }
    }
}
