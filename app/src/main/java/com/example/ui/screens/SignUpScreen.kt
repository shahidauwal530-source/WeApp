package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.auth.GoogleAuthHelper
import com.example.data.CountryInfo
import com.example.data.GlobalCountries
import com.example.data.GlobalLanguages
import com.example.localization.AppStrings
import com.example.security.EmailVerificationService
import com.example.security.PasswordSecurity
import com.example.ui.components.EmailVerificationCard
import com.example.ui.components.WeAppAvatar
import com.example.ui.theme.*

@Composable
fun SignUpScreen(
    strings: AppStrings,
    currentLanguage: String,
    initialEmailOrUsername: String = "",
    initialPhoneDigits: String = "",
    initialDialCountry: CountryInfo = GlobalCountries.detectDeviceCountry(),
    onToggleLanguage: (String) -> Unit,
    onSignUpDraftPrepared: (PendingSignUpDraft) -> Unit = {},
    onNavigateToSignIn: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    BackHandler(onBack = onNavigateToSignIn)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val detectedCountry = remember { GlobalCountries.detectDeviceCountry() }
    val prefillEmail = remember(initialEmailOrUsername) {
        val id = initialEmailOrUsername.trim()
        if (id.contains("@") && !id.startsWith("@")) id else ""
    }
    val prefillUser = remember(initialEmailOrUsername) {
        val id = initialEmailOrUsername.trim()
        if (id.isNotEmpty() && (!id.contains("@") || id.startsWith("@"))) id.removePrefix("@") else ""
    }

    var signUpUsername by remember(prefillUser) { mutableStateOf(prefillUser) }
    var signUpDisplayName by remember { mutableStateOf("") }
    var signUpEmail by remember(prefillEmail) { mutableStateOf(prefillEmail) }
    var verifiedEmailAddress by remember { mutableStateOf<String?>(null) }
    var signUpDialCountry by remember(initialDialCountry) { mutableStateOf(initialDialCountry) }
    var signUpPhoneDigits by remember(initialPhoneDigits) { mutableStateOf(initialPhoneDigits) }
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

    val signUpPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            signUpPhotoUrl = uri.toString()
        }
    }

    val darkFieldColors = weAppDarkAuthFieldColors()

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
                errorMessage = "Sign-up verification was cancelled. Please select a Google account to continue."
            }
        )
    }

    fun validateQuickSignUp(): String? {
        val n = signUpDisplayName.trim()
        val e = signUpEmail.trim()
        val digits = signUpPhoneDigits.filter { it.isDigit() }
        val confirmCandidate = if (signUpConfirmPassword.isNotBlank()) signUpConfirmPassword else signUpPassword
        val passwordErr = PasswordSecurity.validateNewPassword(signUpPassword, confirmCandidate)
        val rawUser = signUpUsername.trim().lowercase().removePrefix("@")
        val dob = signUpDob.trim()
        return when {
            n.isEmpty() -> "Full name is required."
            !android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches() ->
                "Please enter a valid email address."
            digits.length < 7 ->
                "Please enter a valid phone number (at least 7 digits)."
            passwordErr != null -> passwordErr
            rawUser.isNotEmpty() && (rawUser.length < 3 || !rawUser.matches(Regex("^[a-z0-9_]+$"))) ->
                "Username must be at least 3 characters (letters, numbers, underscores)."
            dob.isNotEmpty() && !GlobalCountries.isAtLeast13YearsOld(dob) ->
                "Enter a valid Date of Birth (YYYY-MM-DD). You must be at least 13 years old."
            else -> null
        }
    }

    fun submitQuickSignUp() {
        val validation = validateQuickSignUp()
        if (validation != null) {
            errorMessage = validation
        } else {
            val cleanUser = signUpUsername.trim().lowercase().removePrefix("@").ifBlank {
                val base = signUpEmail.trim().substringBefore("@")
                    .lowercase()
                    .replace(Regex("[^a-z0-9_]"), "_")
                    .trim('_')
                    .take(15)
                val prefix = if (base.length >= 3) base else "weapp"
                "${prefix}_${(1000..9999).random()}"
            }
            val hashed = PasswordSecurity.hashPassword(signUpPassword)
            onSignUpDraftPrepared(
                PendingSignUpDraft(
                    username = cleanUser,
                    displayName = signUpDisplayName.trim(),
                    email = signUpEmail.trim(),
                    dialCountry = signUpDialCountry,
                    phoneDigits = signUpPhoneDigits.trim(),
                    dateOfBirth = signUpDob.trim().ifBlank { "2000-01-01" },
                    gender = signUpGender,
                    selectedCountry = signUpCountry,
                    state = signUpState.trim().ifBlank { signUpCountry.defaultState },
                    city = signUpCity.trim().ifBlank { signUpCountry.defaultCity },
                    bio = signUpBio.trim(),
                    photoUrl = signUpPhotoUrl,
                    passwordHash = hashed.hash,
                    passwordSalt = hashed.salt,
                    passwordLockEnabled = signUpPasswordLock,
                    emailVerified = verifiedEmailAddress?.equals(signUpEmail.trim(), ignoreCase = true) == true,
                    acceptedTerms = true,
                    autoCompleteProfile = true
                )
            )
            signUpPassword = ""
            signUpConfirmPassword = ""
            launchGoogleAuth()
        }
    }

    fun submitSignUp() {
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
                    acceptedTerms = signUpAcceptedTerms,
                    autoCompleteProfile = false
                )
            )
            signUpPassword = ""
            signUpConfirmPassword = ""
            launchGoogleAuth()
        }
    }

    val genderOptions = listOf("Male", "Female", "Other", "Prefer not to say")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A0D1E), Color(0xFF13224A), Color(0xFF0B1535))
                )
            )
            .statusBarsPadding()
            .imePadding()
            .testTag("sign_up_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Dedicated Sign Up Screen Top Navigation Header
            Surface(
                color = Color(0xFF0D1128).copy(alpha = 0.92f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onNavigateToSignIn,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("signup_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Sign In",
                                tint = Color.White
                            )
                        }
                        Column {
                            Text(
                                text = strings.signUpTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = strings.appName + " • " + strings.slogan,
                                style = MaterialTheme.typography.labelSmall,
                                color = WeBlueLight
                            )
                        }
                    }

                    TextButton(
                        onClick = onNavigateToSignIn,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("signup_top_signin_link")
                    ) {
                        Text("Sign In", color = WePurpleLight, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 560.dp)
                    .align(Alignment.CenterHorizontally)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
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

                Text(
                    text = strings.signUpSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkOnSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF14122B).copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, WeBlue.copy(alpha = 0.45f)),
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
                            onNeedGoogleSignIn = ::submitSignUp
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
                                    Icon(Icons.Filled.Map, contentDescription = null, tint = WePurpleLight)
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

                Button(
                    onClick = ::submitQuickSignUp,
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
                        .testTag("inline_signup_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = "Sign Up",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Pinned Bottom Sign Up Action Bar + Link back to Sign In Screen
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
                                    contentDescription = "Sign-up error",
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
                        onClick = ::submitSignUp,
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("bottom_signup_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WeBlue,
                            contentColor = Color.White,
                            disabledContainerColor = WeBlue.copy(alpha = 0.65f),
                            disabledContentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
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
                                text = strings.signUpWithGoogle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (currentLanguage == "ha") "Kana da asusu tuni?" else "Already have an account?",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkOnSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        TextButton(
                            onClick = onNavigateToSignIn,
                            enabled = !isLoading,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("go_to_signin_button")
                        ) {
                            Icon(Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(16.dp), tint = WePurpleLight)
                            Spacer(Modifier.width(4.dp))
                            Text("Sign In", color = WePurpleLight, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
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
