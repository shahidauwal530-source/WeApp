package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.GoogleAuthHelper
import com.example.data.CloudinaryUploader
import com.example.data.CountryInfo
import com.example.data.GlobalCountries
import com.example.data.GlobalLanguages
import com.example.localization.AppStrings
import com.example.ui.components.WeAppAvatar
import com.example.ui.components.WeAppGradientButton
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    strings: AppStrings,
    currentLanguage: String,
    onToggleLanguage: (String) -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isSignUpMode by remember { mutableStateOf(false) }
    var rememberSession by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF090814), Color(0xFF1D1042), Color(0xFF0D1B44))
                )
            )
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 540.dp)
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
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

            Spacer(Modifier.height(16.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Brand "W" Emblem + Logo Badge
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(WeBrandGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_weapp_foreground),
                        contentDescription = strings.appName,
                        modifier = Modifier.fillMaxSize().padding(10.dp),
                        contentScale = ContentScale.Fit
                    )
                    Surface(
                        color = Color.Black.copy(alpha = 0.25f),
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "W",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = if (isSignUpMode) "Join WeApp Global" else strings.signInTitle,
                    style = MaterialTheme.typography.headlineLarge,
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
                    text = if (isSignUpMode) {
                        "Create your global WeApp profile with Google and customize your @username, country, currency, and language."
                    } else {
                        strings.signInSubtitle
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkOnSurfaceVariant,
                    textAlign = TextAlign.Center
                )

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
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    isSignUpMode = false
                                    errorMessage = null
                                }
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
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    isSignUpMode = true
                                    errorMessage = null
                                }
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

                if (errorMessage != null) {
                    Surface(
                        color = WeRose.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, WeRose.copy(alpha = 0.45f)),
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
                                color = WeRose,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Remember Session Toggle Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { rememberSession = !rememberSession }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = rememberSession,
                        onCheckedChange = { rememberSession = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = WePurple,
                            uncheckedColor = DarkOnSurfaceVariant,
                            checkmarkColor = Color.White
                        ),
                        modifier = Modifier.testTag("remember_me_checkbox")
                    )
                    Text(
                        text = "Keep me signed in on this device",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.88f)
                    )
                }

                // Crisp White Google Button ([G] Sign In / Sign Up with Google)
                Button(
                    onClick = {
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
                                errorMessage = "Sign-in was cancelled. Please select an account to continue."
                            }
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
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
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (isSignUpMode) "Sign Up with Google" else strings.signInWithGoogle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "Existing accounts sign straight in without duplicates. New accounts proceed to Complete Profile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkOnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ProfileSetupScreen(
    firebaseUser: FirebaseUser,
    cloudinaryCloudName: String,
    cloudinaryUploadPreset: String,
    strings: AppStrings,
    isSubmitting: Boolean,
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
        city: String,
        bio: String,
        dateOfBirth: String,
        gender: String,
        photoUrl: String
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val defaultEmail = firebaseUser.email ?: ""
    val defaultName = firebaseUser.displayName ?: ""
    val defaultPhoto = firebaseUser.photoUrl?.toString() ?: ""
    val detectedCountry = remember { GlobalCountries.detectDeviceCountry() }
    val suggestedUsername = remember(defaultEmail, defaultName) {
        val base = (defaultEmail.substringBefore("@").ifBlank { defaultName })
            .lowercase()
            .replace(Regex("[^a-z0-9_]"), "_")
            .trim('_')
        if (base.length >= 3) base.take(20) else "weapp_user"
    }

    var username by remember { mutableStateOf(suggestedUsername) }
    var isCheckingUsername by remember { mutableStateOf(false) }
    var isUsernameAvailable by remember { mutableStateOf<Boolean?>(null) }

    var displayName by remember { mutableStateOf(defaultName) }
    var email by remember { mutableStateOf(defaultEmail) }
    var dialCountry by remember { mutableStateOf(detectedCountry) }
    var phoneDigits by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Prefer not to say") }
    var selectedCountry by remember { mutableStateOf(detectedCountry) }
    var city by remember { mutableStateOf(detectedCountry.defaultCity) }
    var bio by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf(defaultPhoto) }

    var showCloudinaryPanel by remember { mutableStateOf(false) }
    var cloudNameInput by remember(cloudinaryCloudName) { mutableStateOf(cloudinaryCloudName) }
    var uploadPresetInput by remember(cloudinaryUploadPreset) { mutableStateOf(cloudinaryUploadPreset) }
    var uploadingPhoto by remember { mutableStateOf(false) }

    var acceptedTerms by remember { mutableStateOf(false) }
    var showCountryPicker by remember { mutableStateOf(false) }
    var showDialPicker by remember { mutableStateOf(false) }
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

    fun validateAndSubmit() {
        val u = username.trim().lowercase()
        val n = displayName.trim()
        val e = email.trim()
        val digits = phoneDigits.filter { it.isDigit() }
        val dob = dateOfBirth.trim()
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
            !GlobalCountries.isAtLeast13YearsOld(dob) ->
                "Enter a valid Date of Birth (YYYY-MM-DD). You must be at least 13 years old."
            c.isEmpty() -> "City is required."
            !acceptedTerms -> "Please accept the Terms of Service & Privacy Policy to continue."
            else -> null
        }
        if (validationError == null) {
            val fullPhone = "${dialCountry.dialCode} ${phoneDigits.trim()}"
            onSubmitProfile(
                u, n, e, fullPhone, selectedCountry, c, bio.trim(),
                dob, gender, photoUrl
            )
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
                onValueChange = { dateOfBirth = it.take(10); validationError = null },
                label = { Text("Date of Birth (YYYY-MM-DD)") },
                placeholder = { Text("1999-05-12") },
                leadingIcon = { Icon(Icons.Filled.Cake, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = dateOfBirth.length == 10 && !dobValid,
                supportingText = {
                    if (dateOfBirth.length == 10 && !dobValid) {
                        Text("Must be a valid date (YYYY-MM-DD) and at least 13 years old", color = WeRose)
                    } else {
                        Text("Format: YYYY-MM-DD (13+ required)")
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
                Text("Gender", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                city = info.defaultCity
                if (phoneDigits.isBlank()) {
                    dialCountry = info
                }
                showCountryPicker = false
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
