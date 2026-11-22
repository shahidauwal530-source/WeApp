package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.localization.AppStrings
import com.example.ui.UiState
import com.example.ui.components.WeAppAvatar
import com.example.ui.components.WeAppEmptyState
import com.example.ui.components.WeAppGradientButton
import com.example.ui.components.WeSecondaryTopBar
import com.example.ui.theme.*

@Composable
fun SettingsHubScreen(
    currentUser: PrivateUser,
    localPrefs: LocalAppPrefs,
    strings: AppStrings,
    onOpenAccountSettings: () -> Unit,
    onOpenPrivacySettings: () -> Unit,
    onOpenBlockedUsers: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenSecuritySettings: () -> Unit,
    onUpdateAppearanceAndAds: (
        themeMode: String,
        fontSize: String,
        language: String,
        dataSaver: Boolean,
        autoPlayVideos: Boolean,
        showFewerAds: Boolean
    ) -> Unit,
    onClearCache: () -> Unit,
    onOpenSubscription: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        WeSecondaryTopBar(strings.settingsTitle, onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Facebook-Style Navigation Sections (1 to 5)
            SettingsNavRow(
                icon = Icons.Filled.ManageAccounts,
                title = "1. Account Settings",
                subtitle = "Name, Username, Email, Phone, Photos, Country (${currentUser.countryFlag}), Work, Bio",
                tint = WePurpleLight,
                testTag = "settings_account_nav",
                onClick = onOpenAccountSettings
            )
            SettingsNavRow(
                icon = Icons.Filled.Lock,
                title = "2. ${strings.privacyTitle}",
                subtitle = "Posts (${currentUser.privacyPosts}), Follow, Messages, Searchability",
                tint = WeBlueLight,
                testTag = "settings_privacy_nav",
                onClick = onOpenPrivacySettings
            )
            SettingsNavRow(
                icon = Icons.Filled.Block,
                title = "3. ${strings.blockedUsersTitle}",
                subtitle = "Blocked Users, Muted Users, Restricted List",
                tint = WeRose,
                testTag = "settings_safety_nav",
                onClick = onOpenBlockedUsers
            )
            SettingsNavRow(
                icon = Icons.Filled.NotificationsActive,
                title = "4. Notification Settings",
                subtitle = "Likes, Comments, Follows, Messages, Push, Sound, Vibration, Email",
                tint = WeGold,
                testTag = "settings_notifications_nav",
                onClick = onOpenNotificationSettings
            )
            SettingsNavRow(
                icon = Icons.Filled.Security,
                title = "5. ${strings.securityTitle}",
                subtitle = "2FA, Login Alerts, Active Sessions, Download Data, Deactivate / Delete",
                tint = WeEmerald,
                testTag = "settings_security_nav",
                onClick = onOpenSecuritySettings
            )

            // 6. APPEARANCE & APP (Facebook style)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("6. Appearance & App", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Text("Theme", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("dark" to "Dark", "light" to "Light", "system" to "System Default").forEach { (mode, label) ->
                            FilterChip(
                                selected = localPrefs.themeMode == mode,
                                onClick = {
                                    onUpdateAppearanceAndAds(
                                        mode, localPrefs.fontSize, localPrefs.language,
                                        localPrefs.dataSaver, localPrefs.autoPlayVideos, localPrefs.showFewerAds
                                    )
                                },
                                label = { Text(label) }
                            )
                        }
                    }

                    Text("Font Size", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("small" to "Small", "medium" to "Medium", "large" to "Large").forEach { (sz, label) ->
                            FilterChip(
                                selected = localPrefs.fontSize == sz,
                                onClick = {
                                    onUpdateAppearanceAndAds(
                                        localPrefs.themeMode, sz, localPrefs.language,
                                        localPrefs.dataSaver, localPrefs.autoPlayVideos, localPrefs.showFewerAds
                                    )
                                },
                                label = { Text(label) }
                            )
                        }
                    }

                    Text("App Language", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(GlobalLanguages.supported) { (code, label) ->
                            FilterChip(
                                selected = localPrefs.language == code,
                                onClick = {
                                    onUpdateAppearanceAndAds(
                                        localPrefs.themeMode, localPrefs.fontSize, code,
                                        localPrefs.dataSaver, localPrefs.autoPlayVideos, localPrefs.showFewerAds
                                    )
                                },
                                label = { Text(label) }
                            )
                        }
                    }

                    SettingsToggleRow(
                        title = "Data Saver Mode",
                        subtitle = "Reduce image & reel bandwidth usage",
                        checked = localPrefs.dataSaver,
                        onCheckedChange = {
                            onUpdateAppearanceAndAds(
                                localPrefs.themeMode, localPrefs.fontSize, localPrefs.language,
                                it, localPrefs.autoPlayVideos, localPrefs.showFewerAds
                            )
                        }
                    )
                    SettingsToggleRow(
                        title = "Auto-Play Videos",
                        subtitle = "Start Video Reels automatically when swiping",
                        checked = localPrefs.autoPlayVideos,
                        onCheckedChange = {
                            onUpdateAppearanceAndAds(
                                localPrefs.themeMode, localPrefs.fontSize, localPrefs.language,
                                localPrefs.dataSaver, it, localPrefs.showFewerAds
                            )
                        }
                    )

                    OutlinedButton(
                        onClick = onClearCache,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.CleaningServices, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Clear Cache")
                    }
                }
            }

            // 7. ADS & MONETIZATION
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("7. Ads & Monetization", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    SettingsToggleRow(
                        title = "Show Fewer Ads",
                        subtitle = if (currentUser.plan == "FREE") "Minimize sponsored promotions in feed" else "Ads automatically hidden for ${currentUser.plan} plan",
                        checked = localPrefs.showFewerAds || currentUser.plan != "FREE",
                        onCheckedChange = {
                            onUpdateAppearanceAndAds(
                                localPrefs.themeMode, localPrefs.fontSize, localPrefs.language,
                                localPrefs.dataSaver, localPrefs.autoPlayVideos, it
                            )
                        }
                    )
                    if (currentUser.plan == "FREE") {
                        OutlinedButton(onClick = onOpenSubscription, modifier = Modifier.fillMaxWidth()) {
                            Text("Upgrade to PRO / VIP to Hide Ads Automatically")
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("settings_logout_button"),
                colors = ButtonDefaults.buttonColors(containerColor = WeRose),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Logout, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(strings.logoutLabel, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun PrivacySettingsScreen(
    currentUser: PrivateUser,
    strings: AppStrings,
    onSavePrivacy: (
        privacyPosts: String,
        privacyFollow: String,
        privacyMessage: String,
        privacyFriends: String,
        privacyTag: String,
        privacyContact: String,
        privacyBirthday: String,
        searchableByEmail: Boolean,
        searchableByPhone: Boolean,
        searchableByUsername: Boolean
    ) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var pPosts by remember(currentUser.userId) { mutableStateOf(currentUser.privacyPosts) }
    var pFollow by remember(currentUser.userId) { mutableStateOf(currentUser.privacyFollow) }
    var pMessage by remember(currentUser.userId) { mutableStateOf(currentUser.privacyMessage) }
    var pFriends by remember(currentUser.userId) { mutableStateOf(currentUser.privacyFriends) }
    var pTag by remember(currentUser.userId) { mutableStateOf(currentUser.privacyTag) }
    var pContact by remember(currentUser.userId) { mutableStateOf(currentUser.privacyContact) }
    var pBirthday by remember(currentUser.userId) { mutableStateOf(currentUser.privacyBirthday) }
    var sEmail by remember(currentUser.userId) { mutableStateOf(currentUser.searchableByEmail) }
    var sPhone by remember(currentUser.userId) { mutableStateOf(currentUser.searchableByPhone) }
    var sUsername by remember(currentUser.userId) { mutableStateOf(currentUser.searchableByUsername) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        WeSecondaryTopBar(strings.privacyTitle, onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrivacyChoiceCard("Who can see my posts?", listOf("Everyone", "Friends", "Friends of Friends", "Only Me"), pPosts) { pPosts = it }
            PrivacyChoiceCard("Who can send friend request / follow me?", listOf("Everyone", "Friends of Friends"), pFollow) { pFollow = it }
            PrivacyChoiceCard("Who can message me?", listOf("Everyone", "Friends", "No One"), pMessage) { pMessage = it }
            PrivacyChoiceCard("Who can see my friends list?", listOf("Everyone", "Friends", "Only Me"), pFriends) { pFriends = it }
            PrivacyChoiceCard("Who can tag me in posts?", listOf("Everyone", "Friends", "No One"), pTag) { pTag = it }
            PrivacyChoiceCard("Who can see my phone number / email?", listOf("Only Me", "Friends"), pContact) { pContact = it }
            PrivacyChoiceCard("Who can see my birthday?", listOf("Everyone", "Friends", "Only Me"), pBirthday) { pBirthday = it }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Profile Search", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    SettingsToggleRow("Allow people to find me by Email", "", sEmail) { sEmail = it }
                    SettingsToggleRow("Allow people to find me by Phone", "", sPhone) { sPhone = it }
                    SettingsToggleRow("Allow people to find me by Username", "", sUsername) { sUsername = it }
                }
            }

            WeAppGradientButton(
                text = strings.saveChanges,
                modifier = Modifier.fillMaxWidth(),
                testTag = "save_privacy_button",
                onClick = {
                    onSavePrivacy(pPosts, pFollow, pMessage, pFriends, pTag, pContact, pBirthday, sEmail, sPhone, sUsername)
                    onBack()
                }
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun BlockedUsersScreen(
    currentUser: PrivateUser,
    safetyList: List<SafetyListEntry>,
    publicUsersState: UiState<List<PublicUser>>,
    strings: AppStrings,
    onSetSafety: (targetUserId: String, targetName: String, listType: String) -> Unit,
    onRemoveSafety: (targetUserId: String, targetName: String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var activeTab by remember { mutableStateOf("blocked") }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        WeSecondaryTopBar(
            title = strings.blockedUsersTitle,
            onBack = onBack,
            actions = {
                TextButton(onClick = { showAddDialog = true }) {
                    Text("+ Add", color = WePurpleLight)
                }
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("blocked" to "Blocked", "muted" to "Muted", "restricted" to "Restricted").forEach { (key, label) ->
                FilterChip(
                    selected = activeTab == key,
                    onClick = { activeTab = key },
                    label = { Text(label) }
                )
            }
        }

        val entries = safetyList.filter { it.listType == activeTab }
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(16.dp)) {
                WeAppEmptyState(
                    icon = Icons.Outlined.Security,
                    title = "No $activeTab users",
                    subtitle = "Users you add to your $activeTab list appear here. You can remove them anytime.",
                    actionLabel = "Add User",
                    onAction = { showAddDialog = true }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(entries, key = { it.targetUserId }) { entry ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                WeAppAvatar(name = entry.targetName, size = 40.dp)
                                Column {
                                    Text(entry.targetName, style = MaterialTheme.typography.titleMedium)
                                    Text(entry.listType.uppercase(), style = MaterialTheme.typography.labelSmall, color = WeRose)
                                }
                            }
                            OutlinedButton(onClick = { onRemoveSafety(entry.targetUserId, entry.targetName) }) {
                                Text(if (activeTab == "blocked") "Unblock" else if (activeTab == "muted") "Unmute" else "Unrestrict")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        val users = ((publicUsersState as? UiState.Success)?.data ?: emptyList())
            .filter { it.userId != currentUser.userId }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add to $activeTab list") },
            text = {
                if (users.isEmpty()) {
                    Text("No other registered users available.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(users, key = { it.userId }) { u ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSetSafety(u.userId, u.displayName.ifBlank { u.username }, activeTab)
                                        showAddDialog = false
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                WeAppAvatar(name = u.displayName.ifBlank { u.username }, size = 36.dp)
                                Text("${u.displayName} (${u.countryFlag} ${u.city})")
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAddDialog = false }) { Text("Close") } }
        )
    }
}

@Composable
fun NotificationSettingsScreen(
    currentUser: PrivateUser,
    strings: AppStrings,
    onSaveNotificationSettings: (
        likes: Boolean, comments: Boolean, follows: Boolean, messages: Boolean,
        challenges: Boolean, live: Boolean, friendRequests: Boolean,
        push: Boolean, sound: Boolean, vibration: Boolean, email: Boolean
    ) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var likes by remember(currentUser.userId) { mutableStateOf(currentUser.notifLikes) }
    var comments by remember(currentUser.userId) { mutableStateOf(currentUser.notifComments) }
    var follows by remember(currentUser.userId) { mutableStateOf(currentUser.notifFollows) }
    var messages by remember(currentUser.userId) { mutableStateOf(currentUser.notifMessages) }
    var challenges by remember(currentUser.userId) { mutableStateOf(currentUser.notifChallenges) }
    var live by remember(currentUser.userId) { mutableStateOf(currentUser.notifLive) }
    var friendRequests by remember(currentUser.userId) { mutableStateOf(currentUser.notifFriendRequests) }
    var push by remember(currentUser.userId) { mutableStateOf(currentUser.notifPush) }
    var sound by remember(currentUser.userId) { mutableStateOf(currentUser.notifSound) }
    var vibration by remember(currentUser.userId) { mutableStateOf(currentUser.notifVibration) }
    var email by remember(currentUser.userId) { mutableStateOf(currentUser.notifEmail) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        WeSecondaryTopBar("Notification Settings", onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Activity Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    SettingsToggleRow("Likes", "", likes) { likes = it }
                    SettingsToggleRow("Comments", "", comments) { comments = it }
                    SettingsToggleRow("Follows", "", follows) { follows = it }
                    SettingsToggleRow("Messages", "", messages) { messages = it }
                    SettingsToggleRow("Game Challenges", "", challenges) { challenges = it }
                    SettingsToggleRow("Live Streams", "", live) { live = it }
                    SettingsToggleRow("Friend Requests", "", friendRequests) { friendRequests = it }
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Delivery Channels", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    SettingsToggleRow("Push Notifications", "", push) { push = it }
                    SettingsToggleRow("Sound", "", sound) { sound = it }
                    SettingsToggleRow("Vibration", "", vibration) { vibration = it }
                    SettingsToggleRow("Email Notifications", "", email) { email = it }
                }
            }

            WeAppGradientButton(
                text = strings.saveChanges,
                modifier = Modifier.fillMaxWidth(),
                testTag = "save_notif_settings_button",
                onClick = {
                    onSaveNotificationSettings(
                        likes, comments, follows, messages, challenges, live,
                        friendRequests, push, sound, vibration, email
                    )
                    onBack()
                }
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun SecuritySettingsScreen(
    currentUser: PrivateUser,
    sessionsState: UiState<List<LoginSession>>,
    strings: AppStrings,
    onSaveSecurity: (twoFactor: Boolean, loginAlerts: Boolean) -> Unit,
    onRevokeOtherSessions: () -> Unit,
    onDownloadData: (onReady: (String) -> Unit) -> Unit,
    onDeactivateAccount: () -> Unit,
    onDeleteAccount: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    var twoFactor by remember(currentUser.userId) { mutableStateOf(currentUser.twoFactorEnabled) }
    var loginAlerts by remember(currentUser.userId) { mutableStateOf(currentUser.loginAlertsEnabled) }
    var showDeactivateDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        WeSecondaryTopBar(strings.securityTitle, onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Password & Google Sign-In Security Notice
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Account Authentication & Password", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Your WeApp account is secured via Google Identity (${currentUser.email}). Manage your Google password and passkeys in your Google Account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider()
                    SettingsToggleRow("Two-Factor Authentication (2FA)", "Require extra verification on unrecognized devices", twoFactor) {
                        twoFactor = it
                        onSaveSecurity(twoFactor, loginAlerts)
                    }
                    SettingsToggleRow("Login Alerts", "Notify me immediately when a new session signs in", loginAlerts) {
                        loginAlerts = it
                        onSaveSecurity(twoFactor, loginAlerts)
                    }
                }
            }

            // Active Sessions
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Active Sessions (Where You're Logged In)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    val sessions = (sessionsState as? UiState.Success)?.data ?: emptyList()
                    if (sessions.isEmpty()) {
                        Text("This device (${currentUser.city}, ${currentUser.countryName})", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        sessions.forEach { session ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(Icons.Filled.Smartphone, null, tint = WeEmerald)
                                    Column {
                                        Text(session.deviceName, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "${session.city}, ${session.countryName} • ${GlobalFormatters.formatRelativeTime(session.updatedAt)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text("Active", style = MaterialTheme.typography.labelSmall, color = WeEmerald, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    OutlinedButton(onClick = onRevokeOtherSessions, modifier = Modifier.fillMaxWidth()) {
                        Text("Log Out Other Devices")
                    }
                }
            }

            // Download My Data
            OutlinedButton(
                onClick = {
                    onDownloadData { jsonPayload ->
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_SUBJECT, "WeApp_My_Data.json")
                            putExtra(Intent.EXTRA_TEXT, jsonPayload)
                        }
                        context.startActivity(Intent.createChooser(intent, "Download My WeApp Data"))
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Download, null)
                Spacer(Modifier.width(8.dp))
                Text("Download My Data (JSON Export)")
            }

            // Deactivate / Delete Account
            OutlinedButton(
                onClick = { showDeactivateDialog = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, WeGold)
            ) {
                Text("Deactivate Account (Temporary)", color = WeGold)
            }

            Button(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WeRose),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Delete Account Permanently", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDeactivateDialog) {
        AlertDialog(
            onDismissRequest = { showDeactivateDialog = false },
            title = { Text("Deactivate Account?") },
            text = { Text("Your profile will be hidden temporarily until you sign back in and save account details.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeactivateDialog = false
                        onDeactivateAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WeGold, contentColor = Color.Black)
                ) { Text("Deactivate") }
            },
            dismissButton = { TextButton(onClick = { showDeactivateDialog = false }) { Text("Cancel") } }
        )
    }

    if (showDeleteDialog) {
        var confirmText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account Permanently?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Type DELETE below to permanently remove your WeApp account and sign out.")
                    OutlinedTextField(
                        value = confirmText,
                        onValueChange = { confirmText = it },
                        placeholder = { Text("DELETE") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteAccount()
                    },
                    enabled = confirmText.trim() == "DELETE",
                    colors = ButtonDefaults.buttonColors(containerColor = WeRose)
                ) { Text("Delete Forever", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PrivacyChoiceCard(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(options) { opt ->
                    FilterChip(
                        selected = selected == opt,
                        onClick = { onSelect(opt) },
                        label = { Text(opt) }
                    )
                }
            }
        }
    }
}
