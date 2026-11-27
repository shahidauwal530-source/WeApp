package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.GoogleAuthHelper
import com.example.data.GlobalCountries
import com.example.data.LocalAppPrefs
import com.example.data.PrivateUser
import com.example.data.UserPreferencesDataStore
import com.example.data.WeAppRepository
import com.example.localization.stringsFor
import com.example.permissions.WeAppPermissions
import com.example.security.PasswordSecurity
import com.example.ui.MainViewModel
import com.example.ui.UiState
import com.example.ui.components.MainTab
import com.example.ui.components.WeAppBottomBar
import com.example.ui.screens.*
import com.example.ui.theme.WeAppTheme
import com.example.ui.theme.WePurple
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class AuthRoute {
    SIGN_IN,
    SIGN_UP
}

private enum class SecondaryRoute {
    NONE,
    WALLET,
    SETTINGS,
    MARKETPLACE,
    LIVE,
    SUBSCRIPTION,
    SEARCH,
    NOTIFICATIONS,
    EDIT_PROFILE,
    PRIVACY_SETTINGS,
    BLOCKED_USERS,
    NOTIFICATION_SETTINGS,
    SECURITY_SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (runCatching { FirebaseApp.getApps(this).isEmpty() }.getOrDefault(false)) {
            runCatching { FirebaseApp.initializeApp(this) }
        }
        runCatching { enableEdgeToEdge() }
        setContent {
            WeAppRoot()
        }
    }
}

@Composable
fun WeAppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsStore = remember(context) { UserPreferencesDataStore(context) }
    val loadedPrefs by prefsStore.prefsFlow.collectAsStateWithLifecycle(initialValue = null)
    val localPrefs = loadedPrefs ?: LocalAppPrefs()
    val strings = remember(localPrefs.language) { stringsFor(localPrefs.language) }

    val hasFirebase = remember(context) {
        runCatching { FirebaseApp.getApps(context).isNotEmpty() }.getOrDefault(false)
    }
    var showSplash by remember { mutableStateOf(true) }
    var currentUser by remember {
        mutableStateOf(
            if (hasFirebase) runCatching { Firebase.auth.currentUser }.getOrNull() else null
        )
    }
    var pendingSignUpDraft by remember { mutableStateOf<PendingSignUpDraft?>(null) }
    var pendingSignInHint by remember { mutableStateOf<PendingSignInHint?>(null) }
    var signInErrorMessage by remember { mutableStateOf<String?>(null) }
    var authRoute by remember { mutableStateOf(AuthRoute.SIGN_IN) }
    var signUpPrefillEmailOrUser by remember { mutableStateOf("") }
    var signUpPrefillPhone by remember { mutableStateOf("") }
    var signUpPrefillDialCountry by remember { mutableStateOf(GlobalCountries.detectDeviceCountry()) }
    var autoSignInChecked by remember { mutableStateOf(false) }

    DisposableEffect(hasFirebase) {
        if (!hasFirebase) return@DisposableEffect onDispose {}
        val auth = runCatching { Firebase.auth }.getOrNull()
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth?.addAuthStateListener(listener)
        onDispose { auth?.removeAuthStateListener(listener) }
    }

    LaunchedEffect(hasFirebase, loadedPrefs, showSplash, currentUser) {
        val prefs = loadedPrefs ?: return@LaunchedEffect
        if (!hasFirebase || showSplash || !prefs.onboardingCompleted || currentUser != null || autoSignInChecked) {
            return@LaunchedEffect
        }
        autoSignInChecked = true
        if (!prefs.rememberSession) return@LaunchedEffect
        delay(300)
        runCatching {
            val credentialManager = CredentialManager.create(context)
            GoogleAuthHelper.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    currentUser = runCatching { Firebase.auth.currentUser }.getOrNull()
                },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    WeAppTheme(
        themeMode = localPrefs.themeMode,
        fontSize = localPrefs.fontSize
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                showSplash || loadedPrefs == null -> {
                    SplashScreen(
                        strings = strings,
                        onSplashFinished = { showSplash = false }
                    )
                }
                !localPrefs.onboardingCompleted -> {
                    OnboardingScreen(
                        strings = strings,
                        currentLanguage = localPrefs.language,
                        onToggleLanguage = { lang -> scope.launch { prefsStore.setLanguage(lang) } },
                        onFinishOnboarding = { scope.launch { prefsStore.setOnboardingCompleted(true) } }
                    )
                }
                currentUser == null -> {
                    AnimatedContent(
                        targetState = authRoute,
                        label = "auth_screen_route"
                    ) { route ->
                        when (route) {
                            AuthRoute.SIGN_IN -> SignInScreen(
                                strings = strings,
                                currentLanguage = localPrefs.language,
                                initialRememberSession = localPrefs.rememberSession,
                                initialSignInHint = localPrefs.lastSignedInHint,
                                initialErrorMessage = signInErrorMessage,
                                onConsumeInitialError = { signInErrorMessage = null },
                                onToggleLanguage = { lang -> scope.launch { prefsStore.setLanguage(lang) } },
                                onRememberSessionChanged = { rememberMe, hint ->
                                    scope.launch { prefsStore.setRememberSession(rememberMe, hint) }
                                },
                                onSignInHintPrepared = { hint ->
                                    signInErrorMessage = null
                                    pendingSignInHint = hint
                                    pendingSignUpDraft = null
                                },
                                onNavigateToSignUp = { prefillId, prefillPhone, dialCountry ->
                                    signInErrorMessage = null
                                    signUpPrefillEmailOrUser = prefillId
                                    signUpPrefillPhone = prefillPhone
                                    signUpPrefillDialCountry = dialCountry
                                    authRoute = AuthRoute.SIGN_UP
                                },
                                onAuthSuccess = {
                                    currentUser = runCatching { Firebase.auth.currentUser }.getOrNull()
                                }
                            )
                            AuthRoute.SIGN_UP -> SignUpScreen(
                                strings = strings,
                                currentLanguage = localPrefs.language,
                                initialEmailOrUsername = signUpPrefillEmailOrUser,
                                initialPhoneDigits = signUpPrefillPhone,
                                initialDialCountry = signUpPrefillDialCountry,
                                onToggleLanguage = { lang -> scope.launch { prefsStore.setLanguage(lang) } },
                                onSignUpDraftPrepared = { draft ->
                                    pendingSignUpDraft = draft
                                    pendingSignInHint = null
                                },
                                onNavigateToSignIn = {
                                    authRoute = AuthRoute.SIGN_IN
                                },
                                onAuthSuccess = {
                                    currentUser = runCatching { Firebase.auth.currentUser }.getOrNull()
                                }
                            )
                        }
                    }
                }
                else -> {
                    val user = currentUser
                    if (user != null) {
                        key(user.uid) {
                            AuthenticatedWeApp(
                                firebaseUser = user,
                                localPrefs = localPrefs,
                                prefsStore = prefsStore,
                                pendingSignUpDraft = pendingSignUpDraft,
                                pendingSignInHint = pendingSignInHint,
                                onClearPendingDrafts = {
                                    pendingSignUpDraft = null
                                    pendingSignInHint = null
                                },
                                onSignOutWithError = { errMsg ->
                                    pendingSignUpDraft = null
                                    pendingSignInHint = null
                                    signInErrorMessage = errMsg
                                    authRoute = AuthRoute.SIGN_IN
                                    runCatching {
                                        val cm = CredentialManager.create(context)
                                        GoogleAuthHelper.signOut(cm, scope) {
                                            currentUser = null
                                        }
                                    }.onFailure {
                                        runCatching { Firebase.auth.signOut() }
                                        currentUser = null
                                    }
                                },
                                onSignOut = {
                                    pendingSignUpDraft = null
                                    pendingSignInHint = null
                                    signInErrorMessage = null
                                    authRoute = AuthRoute.SIGN_IN
                                    signUpPrefillEmailOrUser = ""
                                    signUpPrefillPhone = ""
                                    runCatching {
                                        val cm = CredentialManager.create(context)
                                        GoogleAuthHelper.signOut(cm, scope) {
                                            currentUser = null
                                        }
                                    }.onFailure {
                                        runCatching { Firebase.auth.signOut() }
                                        currentUser = null
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedWeApp(
    firebaseUser: FirebaseUser,
    localPrefs: LocalAppPrefs,
    prefsStore: UserPreferencesDataStore,
    pendingSignUpDraft: PendingSignUpDraft? = null,
    pendingSignInHint: PendingSignInHint? = null,
    onClearPendingDrafts: () -> Unit = {},
    onSignOutWithError: (String) -> Unit = {},
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val uid = firebaseUser.uid
    val firestoreInitResult = remember(context) {
        runCatching {
            val dbId = context.getString(R.string.firestore_database_id)
            FirebaseFirestore.getInstance(dbId)
        }
    }
    val firestore = firestoreInitResult.getOrNull()
    if (firestore == null) {
        val err = firestoreInitResult.exceptionOrNull()?.localizedMessage ?: "Cloud database unavailable."
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Unable to connect to WeApp Cloud: $err")
                Button(onClick = onSignOut) { Text("Back to Sign In") }
            }
        }
        return
    }
    val viewModel: MainViewModel = viewModel(
        key = uid,
        factory = viewModelFactory {
            initializer {
                MainViewModel(WeAppRepository(firestore), uid)
            }
        }
    )

    val privateUserState by viewModel.privateUserState.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val strings = remember(localPrefs.language) { stringsFor(localPrefs.language) }
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    when (val userState = privateUserState) {
        is UiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WePurple)
            }
        }
        is UiState.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(userState.message)
                    Button(onClick = onSignOut) { Text(strings.logoutLabel) }
                }
            }
        }
        is UiState.Success -> {
            val profile = userState.data
            if (profile == null) {
                var autoCreateTriggered by remember(firebaseUser.uid) { mutableStateOf(false) }
                var autoCreateError by remember(firebaseUser.uid) { mutableStateOf<String?>(null) }
                val autoDraft = pendingSignUpDraft?.takeIf { it.autoCompleteProfile }
                LaunchedEffect(firebaseUser.uid, autoDraft) {
                    if (autoDraft != null && !autoCreateTriggered) {
                        autoCreateTriggered = true
                        val digitsOnly = autoDraft.phoneDigits.filter { it.isDigit() }.trimStart('0')
                        val fullPhone = "${autoDraft.dialCountry.dialCode}$digitsOnly"
                        viewModel.completeProfile(
                            username = autoDraft.username,
                            displayName = autoDraft.displayName,
                            email = autoDraft.email,
                            phone = fullPhone,
                            country = autoDraft.selectedCountry,
                            state = autoDraft.state,
                            city = autoDraft.city,
                            bio = autoDraft.bio,
                            dateOfBirth = autoDraft.dateOfBirth,
                            gender = autoDraft.gender,
                            photoUrl = autoDraft.photoUrl,
                            themeMode = localPrefs.themeMode,
                            language = localPrefs.language,
                            passwordHash = autoDraft.passwordHash,
                            passwordSalt = autoDraft.passwordSalt,
                            passwordLockEnabled = autoDraft.passwordLockEnabled,
                            emailVerified = autoDraft.emailVerified,
                            onError = { errMsg -> autoCreateError = errMsg }
                        )
                    }
                }
                if (autoDraft != null && autoCreateError == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = WePurple)
                            Text(
                                text = "Creating your WeApp account...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ProfileSetupScreen(
                            firebaseUser = firebaseUser,
                            cloudinaryCloudName = localPrefs.cloudinaryCloudName,
                            cloudinaryUploadPreset = localPrefs.cloudinaryUploadPreset,
                            strings = strings,
                            currentLanguage = localPrefs.language,
                            isSubmitting = isSubmitting,
                            initialSignUpDraft = pendingSignUpDraft,
                            initialSignInHint = pendingSignInHint,
                            onCheckUsername = { viewModel.checkUsernameAvailable(it) },
                            onSaveCloudinaryConfig = { c, p ->
                                scope.launch { prefsStore.setCloudinaryConfig(c, p) }
                            },
                            onShowMessage = { viewModel.showMessage(it) },
                            onSignOut = onSignOut,
                            onSubmitProfile = { username, displayName, email, phone, country, state, city, bio, dateOfBirth, gender, photoUrl, passwordHash, passwordSalt, passwordLockEnabled, emailVerified ->
                                viewModel.completeProfile(
                                    username = username,
                                    displayName = displayName,
                                    email = email,
                                    phone = phone,
                                    country = country,
                                    state = state,
                                    city = city,
                                    bio = bio,
                                    dateOfBirth = dateOfBirth,
                                    gender = gender,
                                    photoUrl = photoUrl,
                                    themeMode = localPrefs.themeMode,
                                    language = localPrefs.language,
                                    passwordHash = passwordHash,
                                    passwordSalt = passwordSalt,
                                    passwordLockEnabled = passwordLockEnabled,
                                    emailVerified = emailVerified
                                )
                            }
                        )
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                        )
                    }
                }
            } else {
                val strictMismatchError = remember(profile.userId, pendingSignInHint) {
                    val hint = pendingSignInHint
                    if (hint != null && hint.verifyCredentialsStrictly) {
                        PasswordSecurity.verifyStoredCredentials(
                            enteredIdentifier = hint.identifier,
                            enteredPhoneDigits = hint.phoneDigits,
                            dialCode = hint.dialCountry.dialCode,
                            enteredPassword = hint.enteredPassword,
                            storedEmail = profile.email,
                            storedUsername = profile.username,
                            storedPhone = profile.phone,
                            storedPasswordHash = profile.passwordHash,
                            storedPasswordSalt = profile.passwordSalt
                        )
                    } else {
                        null
                    }
                }

                if (strictMismatchError != null) {
                    LaunchedEffect(profile.userId, strictMismatchError) {
                        onSignOutWithError(strictMismatchError)
                    }
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = WePurple)
                    }
                    return
                }

                val hasStoredPassword = profile.passwordHash.isNotBlank() && profile.passwordSalt.isNotBlank()
                var sessionUnlocked by remember(profile.userId) {
                    val hintPwd = pendingSignInHint?.enteredPassword.orEmpty()
                    val shouldGate = hasStoredPassword && (
                        hintPwd.isNotEmpty() ||
                            (profile.passwordLockEnabled && pendingSignUpDraft == null)
                    )
                    val initialOk = if (!shouldGate) {
                        true
                    } else if (hintPwd.isNotEmpty()) {
                        PasswordSecurity.verifyPassword(hintPwd, profile.passwordHash, profile.passwordSalt)
                    } else {
                        false
                    }
                    mutableStateOf(initialOk)
                }
                val initialGateError = remember(profile.userId) {
                    val hintPwd = pendingSignInHint?.enteredPassword.orEmpty()
                    if (
                        hasStoredPassword &&
                        hintPwd.isNotEmpty() &&
                        !PasswordSecurity.verifyPassword(hintPwd, profile.passwordHash, profile.passwordSalt)
                    ) {
                        "Incorrect WeApp password entered on Sign In. Please verify below."
                    } else null
                }

                LaunchedEffect(profile.userId) {
                    onClearPendingDrafts()
                }

                if (!sessionUnlocked) {
                    PasswordUnlockGate(
                        displayName = profile.displayName,
                        email = profile.email,
                        passwordHash = profile.passwordHash,
                        passwordSalt = profile.passwordSalt,
                        strings = strings,
                        initialError = initialGateError,
                        isSubmitting = isSubmitting,
                        onUnlocked = { sessionUnlocked = true },
                        onResetPasswordWithGoogle = { newPwd, confirmPwd, onDone ->
                            viewModel.updateAccountPassword(
                                currentPassword = "",
                                newPassword = newPwd,
                                confirmPassword = confirmPwd,
                                bypassCurrentCheck = true,
                                onDone = onDone
                            )
                        },
                        onSignOut = onSignOut
                    )
                } else {
                    LaunchedEffect(profile.userId) {
                        viewModel.registerSession(profile.city, profile.countryName)
                    }
                    MainWeAppScaffold(
                        profile = profile,
                        viewModel = viewModel,
                        localPrefs = localPrefs,
                        prefsStore = prefsStore,
                        isSubmitting = isSubmitting,
                        snackbarHostState = snackbarHostState,
                        onSignOut = onSignOut
                    )
                }
            }
        }
    }
}

@Composable
private fun MainWeAppScaffold(
    profile: PrivateUser,
    viewModel: MainViewModel,
    localPrefs: LocalAppPrefs,
    prefsStore: UserPreferencesDataStore,
    isSubmitting: Boolean,
    snackbarHostState: SnackbarHostState,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = remember(localPrefs.language) { stringsFor(localPrefs.language) }
    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    var secondaryRoute by remember { mutableStateOf(SecondaryRoute.NONE) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        scope.launch {
            prefsStore.setPermissionsPrompted(true)
            if (results.values.any { granted -> !granted }) {
                snackbarHostState.showSnackbar(
                    "Some permissions were denied. You can enable them later in Settings."
                )
            }
        }
    }

    LaunchedEffect(localPrefs.permissionsPrompted) {
        if (!localPrefs.permissionsPrompted) {
            delay(800)
            val missingPermissions = WeAppPermissions.missingRuntimePermissions(context)
            if (missingPermissions.isEmpty()) {
                prefsStore.setPermissionsPrompted(true)
            } else {
                runCatching {
                    permissionLauncher.launch(missingPermissions.toTypedArray())
                }.onFailure {
                    prefsStore.setPermissionsPrompted(true)
                }
            }
        }
    }

    val publicUsersState by viewModel.publicUsersState.collectAsStateWithLifecycle()
    val storiesState by viewModel.storiesState.collectAsStateWithLifecycle()
    val postsState by viewModel.postsState.collectAsStateWithLifecycle()
    val feedFilter by viewModel.feedFilter.collectAsStateWithLifecycle()
    val likedPostIds by viewModel.likedPostIds.collectAsStateWithLifecycle()
    val hiddenPostIds by viewModel.hiddenPostIds.collectAsStateWithLifecycle()
    val followingIds by viewModel.followingIds.collectAsStateWithLifecycle()
    val safetyList by viewModel.safetyListState.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val sessionsState by viewModel.sessionsState.collectAsStateWithLifecycle()
    val activeCommentPostId by viewModel.activeCommentPostId.collectAsStateWithLifecycle()
    val postComments by viewModel.postComments.collectAsStateWithLifecycle()
    val videosState by viewModel.videosState.collectAsStateWithLifecycle()
    val likedVideoIds by viewModel.likedVideoIds.collectAsStateWithLifecycle()
    val activeCommentVideoId by viewModel.activeCommentVideoId.collectAsStateWithLifecycle()
    val videoComments by viewModel.videoComments.collectAsStateWithLifecycle()
    val selectedGame by viewModel.selectedGame.collectAsStateWithLifecycle()
    val challengesState by viewModel.challengesState.collectAsStateWithLifecycle()
    val chatsState by viewModel.chatsState.collectAsStateWithLifecycle()
    val activeChatId by viewModel.activeChatId.collectAsStateWithLifecycle()
    val activeChatMessages by viewModel.activeChatMessages.collectAsStateWithLifecycle()
    val marketplaceState by viewModel.marketplaceState.collectAsStateWithLifecycle()
    val liveRoomsState by viewModel.liveRoomsState.collectAsStateWithLifecycle()
    val activeLiveRoomId by viewModel.activeLiveRoomId.collectAsStateWithLifecycle()
    val activeLiveMessages by viewModel.activeLiveMessages.collectAsStateWithLifecycle()
    val walletTransactionsState by viewModel.walletTransactionsState.collectAsStateWithLifecycle()

    val unreadNotificationsCount = remember(notificationsState) {
        ((notificationsState as? UiState.Success)?.data ?: emptyList()).count { !it.readFlag }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (secondaryRoute == SecondaryRoute.NONE) {
                WeAppBottomBar(
                    currentTab = currentTab,
                    strings = strings,
                    onSelectTab = { currentTab = it }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            when (secondaryRoute) {
                SecondaryRoute.WALLET -> WalletScreen(
                    currentUser = profile,
                    transactionsState = walletTransactionsState,
                    strings = strings,
                    onClaimDaily = { viewModel.claimDailyBonus() },
                    onBack = { secondaryRoute = SecondaryRoute.NONE }
                )
                SecondaryRoute.SEARCH -> SearchScreen(
                    currentUser = profile,
                    publicUsersState = publicUsersState,
                    followingIds = followingIds,
                    safetyList = safetyList,
                    strings = strings,
                    onToggleFollow = { targetId, targetName -> viewModel.toggleFollow(targetId, targetName) },
                    onStartChat = { peerId, peerName ->
                        viewModel.startChatWith(
                            myName = profile.displayName.ifBlank { profile.username },
                            peerId = peerId,
                            peerName = peerName
                        ) {
                            secondaryRoute = SecondaryRoute.NONE
                            currentTab = MainTab.CHAT
                        }
                    },
                    onSetSafety = { targetId, targetName, type ->
                        viewModel.setSafetyEntry(targetId, targetName, type)
                    },
                    onReportUser = { targetId, reason, details ->
                        viewModel.submitReport("user", targetId, reason, details)
                    },
                    onBack = { secondaryRoute = SecondaryRoute.NONE }
                )
                SecondaryRoute.NOTIFICATIONS -> NotificationsScreen(
                    notificationsState = notificationsState,
                    strings = strings,
                    onMarkAllRead = { viewModel.markAllNotificationsRead() },
                    onBack = { secondaryRoute = SecondaryRoute.NONE }
                )
                SecondaryRoute.EDIT_PROFILE -> EditProfileScreen(
                    currentUser = profile,
                    cloudinaryCloudName = localPrefs.cloudinaryCloudName,
                    cloudinaryUploadPreset = localPrefs.cloudinaryUploadPreset,
                    isSubmitting = isSubmitting,
                    strings = strings,
                    onSaveCloudinaryConfig = { cloud, preset ->
                        scope.launch { prefsStore.setCloudinaryConfig(cloud, preset) }
                    },
                    onSaveAccount = { displayName, username, email, phone, dob, gender, country, state, city, bio, website, work, education, relationshipStatus, photoUrl, coverPhotoUrl, emailVerified ->
                        viewModel.updateAccountDetails(
                            displayName = displayName,
                            username = username,
                            email = email,
                            phone = phone,
                            dateOfBirth = dob,
                            gender = gender,
                            country = country,
                            state = state,
                            city = city,
                            bio = bio,
                            website = website,
                            work = work,
                            education = education,
                            relationshipStatus = relationshipStatus,
                            photoUrl = photoUrl,
                            coverPhotoUrl = coverPhotoUrl,
                            emailVerified = emailVerified,
                            onDone = { secondaryRoute = SecondaryRoute.NONE }
                        )
                    },
                    onShowMessage = { viewModel.showMessage(it) },
                    onBack = { secondaryRoute = SecondaryRoute.NONE }
                )
                SecondaryRoute.PRIVACY_SETTINGS -> PrivacySettingsScreen(
                    currentUser = profile,
                    strings = strings,
                    onSavePrivacy = { pPosts, pFollow, pMessage, pFriends, pTag, pContact, pBirthday, sEmail, sPhone, sUsername ->
                        viewModel.updatePrivacySettings(
                            pPosts, pFollow, pMessage, pFriends, pTag, pContact, pBirthday,
                            sEmail, sPhone, sUsername
                        )
                    },
                    onBack = { secondaryRoute = SecondaryRoute.SETTINGS }
                )
                SecondaryRoute.BLOCKED_USERS -> BlockedUsersScreen(
                    currentUser = profile,
                    safetyList = safetyList,
                    publicUsersState = publicUsersState,
                    strings = strings,
                    onSetSafety = { id, name, type -> viewModel.setSafetyEntry(id, name, type) },
                    onRemoveSafety = { id, name -> viewModel.removeSafetyEntry(id, name) },
                    onBack = { secondaryRoute = SecondaryRoute.SETTINGS }
                )
                SecondaryRoute.NOTIFICATION_SETTINGS -> NotificationSettingsScreen(
                    currentUser = profile,
                    strings = strings,
                    onSaveNotificationSettings = { likes, comments, follows, messages, challenges, live, friendRequests, push, sound, vibration, email ->
                        viewModel.updateNotificationSettings(
                            likes, comments, follows, messages, challenges, live,
                            friendRequests, push, sound, vibration, email
                        )
                    },
                    onBack = { secondaryRoute = SecondaryRoute.SETTINGS }
                )
                SecondaryRoute.SECURITY_SETTINGS -> SecuritySettingsScreen(
                    currentUser = profile,
                    sessionsState = sessionsState,
                    strings = strings,
                    isSubmitting = isSubmitting,
                    onSaveSecurity = { twoFactor, loginAlerts, passwordLock ->
                        viewModel.updateSecuritySettings(twoFactor, loginAlerts, passwordLock)
                    },
                    onUpdatePassword = { currentPwd, newPwd, confirmPwd, onDone ->
                        viewModel.updateAccountPassword(
                            currentPassword = currentPwd,
                            newPassword = newPwd,
                            confirmPassword = confirmPwd,
                            bypassCurrentCheck = false,
                            onDone = onDone
                        )
                    },
                    onEmailVerified = { verifiedEmail ->
                        viewModel.markEmailVerified(verifiedEmail)
                    },
                    onRevokeOtherSessions = { viewModel.revokeOtherSessions() },
                    onDownloadData = { onReady -> viewModel.exportUserData(profile, onReady) },
                    onDeactivateAccount = {
                        viewModel.deactivateAccount { onSignOut() }
                    },
                    onDeleteAccount = {
                        viewModel.deleteAccountData { onSignOut() }
                    },
                    onBack = { secondaryRoute = SecondaryRoute.SETTINGS }
                )
                SecondaryRoute.SETTINGS -> SettingsHubScreen(
                    currentUser = profile,
                    localPrefs = localPrefs,
                    strings = strings,
                    onOpenAccountSettings = { secondaryRoute = SecondaryRoute.EDIT_PROFILE },
                    onOpenPrivacySettings = { secondaryRoute = SecondaryRoute.PRIVACY_SETTINGS },
                    onOpenBlockedUsers = { secondaryRoute = SecondaryRoute.BLOCKED_USERS },
                    onOpenNotificationSettings = { secondaryRoute = SecondaryRoute.NOTIFICATION_SETTINGS },
                    onOpenSecuritySettings = { secondaryRoute = SecondaryRoute.SECURITY_SETTINGS },
                    onUpdateAppearanceAndAds = { themeMode, fontSize, lang, dataSaver, autoPlay, fewerAds ->
                        scope.launch {
                            prefsStore.setThemeMode(themeMode)
                            prefsStore.setFontSize(fontSize)
                            prefsStore.setLanguage(lang)
                            prefsStore.setDataSaver(dataSaver)
                            prefsStore.setAutoPlayVideos(autoPlay)
                            prefsStore.setShowFewerAds(fewerAds)
                        }
                        viewModel.syncAppearanceAndAds(themeMode, fontSize, lang, dataSaver, autoPlay, fewerAds)
                    },
                    onClearCache = {
                        runCatching { context.cacheDir.deleteRecursively() }
                        viewModel.showMessage("App cache cleared")
                    },
                    onOpenSubscription = { secondaryRoute = SecondaryRoute.SUBSCRIPTION },
                    onLogout = onSignOut,
                    onBack = { secondaryRoute = SecondaryRoute.NONE }
                )
                SecondaryRoute.MARKETPLACE -> MarketplaceScreen(
                    currentUser = profile,
                    listingsState = marketplaceState,
                    strings = strings,
                    onCreateListing = { title, desc, cat, price ->
                        viewModel.createListing(profile, title, desc, cat, price)
                    },
                    onMarkSold = { viewModel.markListingSold(it) },
                    onMessageSeller = { sellerId, sellerName ->
                        viewModel.startChatWith(
                            myName = profile.displayName.ifBlank { profile.username },
                            peerId = sellerId,
                            peerName = sellerName
                        ) {
                            secondaryRoute = SecondaryRoute.NONE
                            currentTab = MainTab.CHAT
                        }
                    },
                    onBack = { secondaryRoute = SecondaryRoute.NONE }
                )
                SecondaryRoute.LIVE -> LiveScreen(
                    currentUser = profile,
                    roomsState = liveRoomsState,
                    activeRoomId = activeLiveRoomId,
                    liveMessages = activeLiveMessages,
                    strings = strings,
                    onCreateAndJoinRoom = { title, topic ->
                        viewModel.createAndJoinLiveRoom(profile, title, topic)
                    },
                    onJoinRoom = { viewModel.openLiveRoom(it) },
                    onSendLiveMessage = { roomId, text ->
                        viewModel.sendLiveMessage(roomId, profile.displayName.ifBlank { profile.username }, text)
                    },
                    onEndRoom = { viewModel.endLiveRoom(it) },
                    onBack = {
                        viewModel.openLiveRoom(null)
                        secondaryRoute = SecondaryRoute.NONE
                    }
                )
                SecondaryRoute.SUBSCRIPTION -> SubscriptionScreen(
                    currentPlanCode = profile.plan,
                    countryCode = profile.countryCode,
                    strings = strings,
                    onSelectPlan = { viewModel.upgradePlan(it) },
                    onBack = { secondaryRoute = SecondaryRoute.NONE }
                )
                SecondaryRoute.NONE -> {
                    when (currentTab) {
                        MainTab.HOME -> HomeFeedScreen(
                            currentUser = profile,
                            storiesState = storiesState,
                            postsState = postsState,
                            feedFilter = feedFilter,
                            likedPostIds = likedPostIds,
                            hiddenPostIds = hiddenPostIds,
                            followingIds = followingIds,
                            safetyList = safetyList,
                            unreadNotificationsCount = unreadNotificationsCount,
                            showFewerAds = localPrefs.showFewerAds,
                            activeCommentPostId = activeCommentPostId,
                            postComments = postComments,
                            strings = strings,
                            onSelectFeedFilter = { viewModel.selectFeedFilter(it) },
                            onCreateStory = { caption, style -> viewModel.createStory(profile, caption, style) },
                            onCreatePost = { content, category -> viewModel.createPost(profile, content, category) },
                            onToggleLike = { viewModel.toggleLikePost(it) },
                            onOpenComments = { viewModel.openPostComments(it) },
                            onAddComment = { postId, text ->
                                viewModel.addPostComment(postId, profile.displayName.ifBlank { profile.username }, text)
                            },
                            onSharePost = { viewModel.sharePost(it) },
                            onToggleFollow = { targetId, targetName -> viewModel.toggleFollow(targetId, targetName) },
                            onHidePost = { viewModel.hidePost(it) },
                            onMuteUser = { id, name -> viewModel.setSafetyEntry(id, name, "muted") },
                            onBlockUser = { id, name -> viewModel.setSafetyEntry(id, name, "blocked") },
                            onReportPost = { postId, reason, details ->
                                viewModel.submitReport("post", postId, reason, details)
                            },
                            onOpenSearch = { secondaryRoute = SecondaryRoute.SEARCH },
                            onOpenNotifications = { secondaryRoute = SecondaryRoute.NOTIFICATIONS },
                            onOpenLive = { secondaryRoute = SecondaryRoute.LIVE },
                            onOpenMarketplace = { secondaryRoute = SecondaryRoute.MARKETPLACE },
                            onOpenWallet = { secondaryRoute = SecondaryRoute.WALLET },
                            onOpenSubscription = { secondaryRoute = SecondaryRoute.SUBSCRIPTION },
                            onOpenSettings = { secondaryRoute = SecondaryRoute.SETTINGS }
                        )
                        MainTab.VIDEO -> VideoFeedScreen(
                            currentUser = profile,
                            videosState = videosState,
                            likedVideoIds = likedVideoIds,
                            followingIds = followingIds,
                            safetyList = safetyList,
                            autoPlayVideos = localPrefs.autoPlayVideos,
                            activeCommentVideoId = activeCommentVideoId,
                            videoComments = videoComments,
                            strings = strings,
                            onCreateVideo = { title, caption, cat, dur, theme ->
                                viewModel.createVideoReel(profile, title, caption, cat, dur, theme)
                            },
                            onToggleLikeVideo = { viewModel.toggleLikeVideo(it) },
                            onOpenComments = { viewModel.openVideoComments(it) },
                            onAddComment = { videoId, text ->
                                viewModel.addVideoComment(videoId, profile.displayName.ifBlank { profile.username }, text)
                            },
                            onShareVideo = { viewModel.shareVideo(it) },
                            onToggleFollow = { targetId, targetName -> viewModel.toggleFollow(targetId, targetName) }
                        )
                        MainTab.GAME -> GameArenaScreen(
                            currentUser = profile,
                            selectedGame = selectedGame,
                            publicUsersState = publicUsersState,
                            challengesState = challengesState,
                            strings = strings,
                            onSelectGame = { viewModel.selectGame(it) },
                            onCreateChallenge = { game, mode, note, stake ->
                                viewModel.createGameChallenge(profile, game, mode, note, stake)
                            },
                            onAcceptChallenge = { chId ->
                                viewModel.acceptChallenge(chId, profile.displayName.ifBlank { profile.username })
                            },
                            onCompleteChallenge = { viewModel.completeChallenge(it) }
                        )
                        MainTab.CHAT -> ChatScreen(
                            currentUser = profile,
                            chatsState = chatsState,
                            publicUsersState = publicUsersState,
                            activeChatId = activeChatId,
                            activeMessages = activeChatMessages,
                            strings = strings,
                            onOpenChat = { viewModel.openChat(it) },
                            onStartChatWith = { peerId, peerName ->
                                viewModel.startChatWith(
                                    myName = profile.displayName.ifBlank { profile.username },
                                    peerId = peerId,
                                    peerName = peerName
                                ) {}
                            },
                            onSendMessage = { chatId, participantIds, text ->
                                viewModel.sendChatMessage(
                                    chatId = chatId,
                                    participantIds = participantIds,
                                    senderName = profile.displayName.ifBlank { profile.username },
                                    text = text
                                )
                            },
                            onSetTyping = { chatId, typing -> viewModel.setTyping(chatId, typing) }
                        )
                        MainTab.PROFILE -> ProfileScreen(
                            currentUser = profile,
                            postsState = postsState,
                            strings = strings,
                            onOpenEditProfile = { secondaryRoute = SecondaryRoute.EDIT_PROFILE },
                            onOpenWallet = { secondaryRoute = SecondaryRoute.WALLET },
                            onOpenMarketplace = { secondaryRoute = SecondaryRoute.MARKETPLACE },
                            onOpenLive = { secondaryRoute = SecondaryRoute.LIVE },
                            onOpenSubscription = { secondaryRoute = SecondaryRoute.SUBSCRIPTION },
                            onOpenSettings = { secondaryRoute = SecondaryRoute.SETTINGS }
                        )
                    }
                }
            }
        }
    }
}
