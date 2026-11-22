package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: WeAppRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _selectedGame = MutableStateFlow(SupportedGames.all.first())
    val selectedGame: StateFlow<String> = _selectedGame.asStateFlow()

    private val _feedFilter = MutableStateFlow(GlobalFeedFilter.FOR_YOU)
    val feedFilter: StateFlow<GlobalFeedFilter> = _feedFilter.asStateFlow()

    private val _activeCommentPostId = MutableStateFlow<String?>(null)
    val activeCommentPostId: StateFlow<String?> = _activeCommentPostId.asStateFlow()

    private val _activeCommentVideoId = MutableStateFlow<String?>(null)
    val activeCommentVideoId: StateFlow<String?> = _activeCommentVideoId.asStateFlow()

    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    private val _activeLiveRoomId = MutableStateFlow<String?>(null)
    val activeLiveRoomId: StateFlow<String?> = _activeLiveRoomId.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private fun <T> Flow<T>.toUiStateFlow(tagLabel: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $tagLabel", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Unable to load $tagLabel"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val privateUserState: StateFlow<UiState<PrivateUser?>> =
        repository.observePrivateUser(currentUserId).toUiStateFlow("profile")

    val publicUsersState: StateFlow<UiState<List<PublicUser>>> =
        repository.observePublicUsers().toUiStateFlow("creators")

    val storiesState: StateFlow<UiState<List<Story>>> =
        repository.observeStories().toUiStateFlow("stories")

    val postsState: StateFlow<UiState<List<Post>>> =
        repository.observePosts().toUiStateFlow("posts")

    val likedPostIds: StateFlow<Set<String>> =
        repository.observeLikedPostIds(currentUserId)
            .catch { emit(emptySet()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptySet())

    val hiddenPostIds: StateFlow<Set<String>> =
        repository.observeHiddenPostIds(currentUserId)
            .catch { emit(emptySet()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptySet())

    val followingIds: StateFlow<Set<String>> =
        repository.observeFollowingIds(currentUserId)
            .catch { emit(emptySet()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptySet())

    val safetyListState: StateFlow<List<SafetyListEntry>> =
        repository.observeSafetyList(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val notificationsState: StateFlow<UiState<List<UserNotification>>> =
        repository.observeNotifications(currentUserId).toUiStateFlow("notifications")

    val sessionsState: StateFlow<UiState<List<LoginSession>>> =
        repository.observeSessions(currentUserId).toUiStateFlow("sessions")

    val videosState: StateFlow<UiState<List<VideoReel>>> =
        repository.observeVideos().toUiStateFlow("videos")

    val likedVideoIds: StateFlow<Set<String>> =
        repository.observeLikedVideoIds(currentUserId)
            .catch { emit(emptySet()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptySet())

    val challengesState: StateFlow<UiState<List<GameChallenge>>> =
        repository.observeGameChallenges().toUiStateFlow("challenges")

    val chatsState: StateFlow<UiState<List<ChatThread>>> =
        repository.observeChats(currentUserId).toUiStateFlow("chats")

    val marketplaceState: StateFlow<UiState<List<MarketplaceListing>>> =
        repository.observeMarketplace().toUiStateFlow("marketplace")

    val liveRoomsState: StateFlow<UiState<List<LiveRoom>>> =
        repository.observeLiveRooms().toUiStateFlow("live_rooms")

    val walletTransactionsState: StateFlow<UiState<List<WalletTransaction>>> =
        repository.observeWalletTransactions(currentUserId).toUiStateFlow("wallet")

    val postComments: StateFlow<List<Comment>> = _activeCommentPostId
        .flatMapLatest { postId ->
            if (postId == null) flowOf(emptyList())
            else repository.observePostComments(postId).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val videoComments: StateFlow<List<Comment>> = _activeCommentVideoId
        .flatMapLatest { videoId ->
            if (videoId == null) flowOf(emptyList())
            else repository.observeVideoComments(videoId).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val activeChatMessages: StateFlow<List<ChatMessage>> = _activeChatId
        .flatMapLatest { chatId ->
            if (chatId == null) flowOf(emptyList())
            else repository.observeChatMessages(chatId, currentUserId).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val activeLiveMessages: StateFlow<List<LiveMessage>> = _activeLiveRoomId
        .flatMapLatest { roomId ->
            if (roomId == null) flowOf(emptyList())
            else repository.observeLiveMessages(roomId).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun showMessage(message: String) {
        _statusBanner.value = message
    }

    fun selectGame(game: String) {
        _selectedGame.value = game
    }

    fun selectFeedFilter(filter: GlobalFeedFilter) {
        _feedFilter.value = filter
    }

    fun openPostComments(postId: String?) {
        _activeCommentPostId.value = postId
    }

    fun openVideoComments(videoId: String?) {
        _activeCommentVideoId.value = videoId
    }

    fun openChat(chatId: String?) {
        _activeChatId.value = chatId
    }

    fun openLiveRoom(roomId: String?) {
        val previous = _activeLiveRoomId.value
        if (previous != null && previous != roomId) {
            viewModelScope.launch { repository.adjustLiveViewerCount(previous, -1L) }
        }
        _activeLiveRoomId.value = roomId
        if (roomId != null && roomId != previous) {
            viewModelScope.launch { repository.adjustLiveViewerCount(roomId, 1L) }
        }
    }

    private fun launchMutation(successMessage: String? = null, block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = block()
            _isSubmitting.value = false
            result.fold(
                onSuccess = {
                    if (successMessage != null) _statusBanner.value = successMessage
                },
                onFailure = { error ->
                    _statusBanner.value = error.localizedMessage ?: "Action failed"
                }
            )
        }
    }

    suspend fun checkUsernameAvailable(username: String): Boolean =
        repository.isUsernameAvailable(username)

    fun completeProfile(
        username: String,
        displayName: String,
        email: String,
        phone: String,
        country: CountryInfo,
        city: String,
        bio: String,
        dateOfBirth: String,
        gender: String,
        photoUrl: String,
        themeMode: String,
        language: String
    ) = launchMutation("Welcome to WeApp! +100 Coins awarded") {
        repository.createUserProfile(
            username, displayName, email, phone, country, city, bio,
            dateOfBirth, gender, photoUrl, themeMode, language
        )
    }

    fun updateAccountDetails(
        displayName: String,
        username: String,
        email: String,
        phone: String,
        dateOfBirth: String,
        gender: String,
        country: CountryInfo,
        city: String,
        bio: String,
        website: String,
        work: String,
        education: String,
        relationshipStatus: String,
        photoUrl: String,
        coverPhotoUrl: String,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.updateAccountDetails(
                displayName = displayName,
                username = username,
                email = email,
                phone = phone,
                dateOfBirth = dateOfBirth,
                gender = gender,
                country = country,
                city = city,
                bio = bio,
                website = website,
                work = work,
                education = education,
                relationshipStatus = relationshipStatus,
                photoUrl = photoUrl,
                coverPhotoUrl = coverPhotoUrl
            )
            _isSubmitting.value = false
            res.fold(
                onSuccess = {
                    _statusBanner.value = "Account settings saved"
                    onDone()
                },
                onFailure = { e -> _statusBanner.value = e.localizedMessage ?: "Failed to save" }
            )
        }
    }

    fun updatePrivacySettings(
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
    ) = launchMutation("Privacy settings saved") {
        repository.updatePrivacySettings(
            privacyPosts, privacyFollow, privacyMessage, privacyFriends, privacyTag,
            privacyContact, privacyBirthday, searchableByEmail, searchableByPhone, searchableByUsername
        )
    }

    fun updateNotificationSettings(
        notifLikes: Boolean,
        notifComments: Boolean,
        notifFollows: Boolean,
        notifMessages: Boolean,
        notifChallenges: Boolean,
        notifLive: Boolean,
        notifFriendRequests: Boolean,
        notifPush: Boolean,
        notifSound: Boolean,
        notifVibration: Boolean,
        notifEmail: Boolean
    ) = launchMutation("Notification preferences updated") {
        repository.updateNotificationSettings(
            notifLikes, notifComments, notifFollows, notifMessages, notifChallenges,
            notifLive, notifFriendRequests, notifPush, notifSound, notifVibration, notifEmail
        )
    }

    fun updateSecuritySettings(twoFactorEnabled: Boolean, loginAlertsEnabled: Boolean) =
        launchMutation("Security preferences updated") {
            repository.updateSecuritySettings(twoFactorEnabled, loginAlertsEnabled)
        }

    fun syncAppearanceAndAds(
        themeMode: String,
        fontSize: String,
        language: String,
        dataSaver: Boolean,
        autoPlayVideos: Boolean,
        showFewerAds: Boolean
    ) {
        viewModelScope.launch {
            repository.updateAppearanceAndAdsSettings(
                themeMode, fontSize, language, dataSaver, autoPlayVideos, showFewerAds
            )
        }
    }

    fun registerSession(city: String, countryName: String) {
        viewModelScope.launch { repository.registerCurrentSession(city, countryName) }
    }

    fun revokeOtherSessions() = launchMutation("Logged out of other active devices") {
        repository.revokeOtherSessions()
    }

    fun deactivateAccount(onDone: () -> Unit) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.deactivateAccount()
            _isSubmitting.value = false
            res.fold(
                onSuccess = {
                    _statusBanner.value = "Account deactivated temporarily"
                    onDone()
                },
                onFailure = { e -> _statusBanner.value = e.localizedMessage ?: "Failed to deactivate" }
            )
        }
    }

    fun deleteAccountData(onDone: () -> Unit) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.deleteAccountData()
            _isSubmitting.value = false
            res.fold(
                onSuccess = {
                    _statusBanner.value = "Account data deleted permanently"
                    onDone()
                },
                onFailure = { e -> _statusBanner.value = e.localizedMessage ?: "Failed to delete account" }
            )
        }
    }

    fun exportUserData(user: PrivateUser, onExportReady: (String) -> Unit) {
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val json = repository.buildUserDataExportJson(user)
                _isSubmitting.value = false
                onExportReady(json)
            } catch (e: Exception) {
                _isSubmitting.value = false
                _statusBanner.value = e.localizedMessage ?: "Export failed"
            }
        }
    }

    fun setSafetyEntry(targetUserId: String, targetName: String, listType: String) =
        launchMutation("Updated $listType list for $targetName") {
            repository.setSafetyEntry(targetUserId, targetName, listType)
        }

    fun removeSafetyEntry(targetUserId: String, targetName: String) =
        launchMutation("Removed $targetName from safety list") {
            repository.removeSafetyEntry(targetUserId)
        }

    fun hidePost(postId: String) = launchMutation("Post hidden from your feed") {
        repository.hidePost(postId)
    }

    fun submitReport(targetType: String, targetId: String, reason: String, details: String) =
        launchMutation("Report submitted ($reason). Thank you for keeping WeApp safe.") {
            repository.submitReport(targetType, targetId, reason, details)
        }

    fun markAllNotificationsRead() {
        viewModelScope.launch { repository.markAllNotificationsRead() }
    }

    fun createStory(user: PrivateUser, caption: String, accentStyle: String) =
        launchMutation("Story posted!") {
            repository.createStory(user, caption, accentStyle)
        }

    fun createPost(user: PrivateUser, content: String, category: String) =
        launchMutation("Post published! +10 Coins") {
            repository.createPost(user, content, category)
        }

    fun toggleLikePost(postId: String) {
        val liked = postId in likedPostIds.value
        launchMutation { repository.toggleLikePost(postId, liked) }
    }

    fun addPostComment(postId: String, authorName: String, text: String) =
        launchMutation { repository.addPostComment(postId, authorName, text) }

    fun sharePost(postId: String) =
        launchMutation { repository.sharePost(postId) }

    fun toggleFollow(targetUserId: String, targetName: String) {
        val following = targetUserId in followingIds.value
        launchMutation { repository.toggleFollowUser(targetUserId, targetName, following) }
    }

    fun createVideoReel(
        user: PrivateUser,
        title: String,
        caption: String,
        category: String,
        durationLabel: String,
        accentTheme: String
    ) = launchMutation("Reel uploaded! +15 Coins") {
        repository.createVideoReel(user, title, caption, category, durationLabel, accentTheme)
    }

    fun toggleLikeVideo(videoId: String) {
        val liked = videoId in likedVideoIds.value
        launchMutation { repository.toggleLikeVideo(videoId, liked) }
    }

    fun addVideoComment(videoId: String, authorName: String, text: String) =
        launchMutation { repository.addVideoComment(videoId, authorName, text) }

    fun shareVideo(videoId: String) =
        launchMutation { repository.shareVideo(videoId) }

    fun createGameChallenge(
        user: PrivateUser,
        gameName: String,
        mode: String,
        note: String,
        stakeCoins: Long
    ) = launchMutation("$mode created for $gameName!") {
        repository.createGameChallenge(user, gameName, mode, note, stakeCoins)
    }

    fun acceptChallenge(challengeId: String, myName: String) =
        launchMutation("Challenge accepted!") {
            repository.acceptGameChallenge(challengeId, myName)
        }

    fun completeChallenge(challengeId: String) =
        launchMutation("Match completed! +50 Coins & +25 Rank Points") {
            repository.completeGameChallenge(challengeId)
        }

    fun startChatWith(myName: String, peerId: String, peerName: String, onReady: (String) -> Unit) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.startOrGetChat(myName, peerId, peerName)
            _isSubmitting.value = false
            res.fold(
                onSuccess = { chatId ->
                    _activeChatId.value = chatId
                    onReady(chatId)
                },
                onFailure = { err ->
                    _statusBanner.value = err.localizedMessage ?: "Unable to start chat"
                }
            )
        }
    }

    fun sendChatMessage(chatId: String, participantIds: List<String>, senderName: String, text: String) =
        launchMutation { repository.sendChatMessage(chatId, participantIds, senderName, text) }

    fun setTyping(chatId: String, typing: Boolean) {
        viewModelScope.launch { repository.setTypingStatus(chatId, typing) }
    }

    fun claimDailyBonus() = launchMutation("Claimed +25 Daily WeCoins!") {
        repository.claimDailyCoins()
    }

    fun upgradePlan(plan: SubscriptionPlan) = launchMutation("Upgraded to ${plan.code}!") {
        repository.upgradeSubscription(plan)
    }

    fun createListing(
        user: PrivateUser,
        title: String,
        description: String,
        category: String,
        priceNgn: Long
    ) = launchMutation("Item listed on Marketplace!") {
        repository.createMarketplaceListing(user, title, description, category, priceNgn)
    }

    fun markListingSold(itemId: String) = launchMutation("Marked as sold!") {
        repository.markListingSold(itemId)
    }

    fun createAndJoinLiveRoom(user: PrivateUser, title: String, topic: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.createLiveRoom(user, title, topic)
            _isSubmitting.value = false
            res.fold(
                onSuccess = { roomId -> _activeLiveRoomId.value = roomId },
                onFailure = { err -> _statusBanner.value = err.localizedMessage ?: "Failed to go live" }
            )
        }
    }

    fun sendLiveMessage(roomId: String, senderName: String, text: String) =
        launchMutation { repository.sendLiveMessage(roomId, senderName, text) }

    fun endLiveRoom(roomId: String) = launchMutation("Live stream ended") {
        _activeLiveRoomId.value = null
        repository.endLiveRoom(roomId)
    }

    private companion object {
        const val TAG = "MainViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
