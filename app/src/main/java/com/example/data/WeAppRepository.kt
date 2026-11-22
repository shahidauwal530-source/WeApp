package com.example.data

import android.content.Context
import android.os.Build
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class WeAppRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> Flow<T>.guardFirestoreErrors(
        operationType: OperationType,
        path: String,
        expectedUid: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    // --- Real-Time Observations ---

    fun observePrivateUser(userId: String): Flow<PrivateUser?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user private profile read." }
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(PrivateUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .guardFirestoreErrors(OperationType.GET, docRef.path, uid)
        )
    }

    fun observePublicUsers(): Flow<List<PublicUser>> = flow {
        val uid = requireUserId()
        val path = "users_public"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snap ->
                    snap.toObjects(PublicUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.accountStatus != "deactivated" }
                        .sortedByDescending { it.rankPoints }
                }
                .guardFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeStories(): Flow<List<Story>> = flow {
        val uid = requireUserId()
        val path = "stories"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Story::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .guardFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observePosts(): Flow<List<Post>> = flow {
        val uid = requireUserId()
        val path = "posts"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .guardFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observePostComments(postId: String): Flow<List<Comment>> = flow {
        val uid = requireUserId()
        val path = "posts/$postId/comments"
        emitAll(
            db.collection("posts").document(postId).collection("comments")
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Comment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .guardFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeLikedPostIds(userId: String): Flow<Set<String>> = flow {
        val uid = requireUserId()
        val path = "users/$userId/liked_posts"
        emitAll(
            db.collection("users").document(userId).collection("liked_posts")
                .snapshots()
                .map { snap -> snap.documents.map { it.id }.toSet() }
                .guardFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeHiddenPostIds(userId: String): Flow<Set<String>> = flow {
        val uid = requireUserId()
        val path = "users/$userId/hidden_posts"
        emitAll(
            db.collection("users").document(userId).collection("hidden_posts")
                .snapshots()
                .map { snap -> snap.documents.map { it.id }.toSet() }
                .guardFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeFollowingIds(userId: String): Flow<Set<String>> = flow {
        val uid = requireUserId()
        val path = "users/$userId/following"
        emitAll(
            db.collection("users").document(userId).collection("following")
                .snapshots()
                .map { snap -> snap.documents.map { it.id }.toSet() }
                .guardFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeSafetyList(userId: String): Flow<List<SafetyListEntry>> = flow {
        val uid = requireUserId()
        val path = "users/$userId/safety_lists"
        emitAll(
            db.collection("users").document(userId).collection("safety_lists")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SafetyListEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .guardFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeNotifications(userId: String): Flow<List<UserNotification>> = flow {
        val uid = requireUserId()
        val path = "users/$userId/notifications"
        emitAll(
            db.collection("users").document(userId).collection("notifications")
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .guardFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeSessions(userId: String): Flow<List<LoginSession>> = flow {
        val uid = requireUserId()
        val path = "users/$userId/sessions"
        emitAll(
            db.collection("users").document(userId).collection("sessions")
                .snapshots()
                .map { snap ->
                    snap.toObjects(LoginSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .guardFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeVideos(): Flow<List<VideoReel>> = flow {
        val uid = requireUserId()
        val path = "videos"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snap ->
                    snap.toObjects(VideoReel::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .guardFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeVideoComments(videoId: String): Flow<List<Comment>> = flow {
        val uid = requireUserId()
        val path = "videos/$videoId/comments"
        emitAll(
            db.collection("videos").document(videoId).collection("comments")
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Comment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .guardFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeLikedVideoIds(userId: String): Flow<Set<String>> = flow {
        val uid = requireUserId()
        val path = "users/$userId/liked_videos"
        emitAll(
            db.collection("users").document(userId).collection("liked_videos")
                .snapshots()
                .map { snap -> snap.documents.map { it.id }.toSet() }
                .guardFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeGameChallenges(): Flow<List<GameChallenge>> = flow {
        val uid = requireUserId()
        val path = "game_challenges"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snap ->
                    snap.toObjects(GameChallenge::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .guardFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeChats(userId: String): Flow<List<ChatThread>> = flow {
        val uid = requireUserId()
        val path = "chats"
        emitAll(
            db.collection(path)
                .whereArrayContains("participantIds", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatThread::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .guardFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeChatMessages(chatId: String, userId: String): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        val path = "chats/$chatId/messages"
        emitAll(
            db.collection("chats").document(chatId).collection("messages")
                .whereArrayContains("participantIds", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .guardFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeMarketplace(): Flow<List<MarketplaceListing>> = flow {
        val uid = requireUserId()
        val path = "marketplace"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snap ->
                    snap.toObjects(MarketplaceListing::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .guardFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeLiveRooms(): Flow<List<LiveRoom>> = flow {
        val uid = requireUserId()
        val path = "live_rooms"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snap ->
                    snap.toObjects(LiveRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .guardFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeLiveMessages(roomId: String): Flow<List<LiveMessage>> = flow {
        val uid = requireUserId()
        val path = "live_rooms/$roomId/messages"
        emitAll(
            db.collection("live_rooms").document(roomId).collection("messages")
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snap ->
                    snap.toObjects(LiveMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .guardFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeWalletTransactions(userId: String): Flow<List<WalletTransaction>> = flow {
        val uid = requireUserId()
        val path = "users/$userId/wallet_transactions"
        emitAll(
            db.collection("users").document(userId).collection("wallet_transactions")
                .snapshots()
                .map { snap ->
                    snap.toObjects(WalletTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .guardFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    // --- Profile, Account & Global Settings ---

    suspend fun isUsernameAvailable(username: String): Boolean {
        val clean = username.trim()
        if (clean.length < 3 || !clean.matches(Regex("^[a-zA-Z0-9_]+$"))) return false
        return try {
            requireUserId()
            val snap = db.collection("users_public")
                .whereEqualTo("visibility", "public")
                .get()
                .await()
            val users = snap.toObjects(PublicUser::class.java)
            users.none { it.username.equals(clean, ignoreCase = true) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    suspend fun createUserProfile(
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
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val cleanUsername = username.trim().take(30)
            val cleanName = displayName.trim().take(50)
            val cleanEmail = email.trim().take(120)
            val cleanPhone = phone.trim().take(25)
            val cleanCity = city.trim().ifBlank { country.defaultCity }.take(60)
            val cleanBio = bio.trim().take(200)
            val cleanDob = dateOfBirth.trim().take(30)
            val cleanGender = gender.trim().take(30)
            val safeLang = if (GlobalLanguages.supported.any { it.first == language }) language else "en"
            val safeTheme = if (themeMode in listOf("dark", "light", "system")) themeMode else "dark"
            val resolvedPhoto = photoUrl.trim().ifBlank {
                auth.currentUser?.photoUrl?.toString() ?: ""
            }.take(500)

            require(cleanUsername.length >= 3) { "Username must be at least 3 characters" }
            require(cleanName.isNotEmpty()) { "Display name is required" }
            require(cleanEmail.contains("@")) { "Valid email is required" }
            require(cleanPhone.length >= 7) { "Valid phone number is required" }

            val existingUsers = db.collection("users_public")
                .whereEqualTo("visibility", "public")
                .get()
                .await()
                .toObjects(PublicUser::class.java)
            require(existingUsers.none { it.userId != uid && it.username.equals(cleanUsername, ignoreCase = true) }) {
                "Username @$cleanUsername is already taken. Choose another one."
            }

            val privatePayload = mapOf(
                "userId" to uid,
                "username" to cleanUsername,
                "displayName" to cleanName,
                "email" to cleanEmail,
                "phone" to cleanPhone,
                "countryCode" to country.code,
                "countryName" to country.name,
                "countryFlag" to country.flag,
                "city" to cleanCity,
                "currencyCode" to country.currencyCode,
                "bio" to cleanBio,
                "dateOfBirth" to cleanDob,
                "gender" to cleanGender,
                "photoUrl" to resolvedPhoto,
                "coverPhotoUrl" to "",
                "plan" to "FREE",
                "coins" to 100L,
                "rankPoints" to 10L,
                "followersCount" to 0L,
                "followingCount" to 0L,
                "postsCount" to 0L,
                "darkMode" to (safeTheme == "dark"),
                "themeMode" to safeTheme,
                "fontSize" to "medium",
                "language" to safeLang,
                "accountStatus" to "active",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val publicPayload = mapOf(
                "userId" to uid,
                "username" to cleanUsername,
                "displayName" to cleanName,
                "countryCode" to country.code,
                "countryName" to country.name,
                "countryFlag" to country.flag,
                "city" to cleanCity,
                "bio" to cleanBio,
                "photoUrl" to resolvedPhoto,
                "coverPhotoUrl" to "",
                "plan" to "FREE",
                "coins" to 100L,
                "rankPoints" to 10L,
                "followersCount" to 0L,
                "followingCount" to 0L,
                "postsCount" to 0L,
                "privacyMessage" to "Everyone",
                "privacyFollow" to "Everyone",
                "searchableByUsername" to true,
                "accountStatus" to "active",
                "visibility" to "public",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val txRef = db.collection("users").document(uid).collection("wallet_transactions").document()
            val txPayload = mapOf(
                "userId" to uid,
                "title" to "Welcome to WeApp Bonus",
                "txType" to "earn",
                "amount" to 100L,
                "createdAt" to FieldValue.serverTimestamp()
            )
            val notifRef = db.collection("users").document(uid).collection("notifications").document()
            val notifPayload = mapOf(
                "userId" to uid,
                "actorId" to uid,
                "actorName" to "WeApp Global",
                "notifType" to "system",
                "title" to "Welcome to WeApp ${country.flag}",
                "body" to "Your global account is live in $cleanCity, ${country.name}. +100 WeCoins added!",
                "readFlag" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
            val sessionRef = db.collection("users").document(uid).collection("sessions").document("current_device")
            val deviceLabel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}".take(80)
            val sessionPayload = mapOf(
                "userId" to uid,
                "deviceName" to deviceLabel,
                "city" to cleanCity,
                "countryName" to country.name,
                "activeFlag" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            path = "users/$uid"
            val batch = db.batch()
            batch.set(db.collection("users").document(uid), privatePayload)
            batch.set(db.collection("users_public").document(uid), publicPayload)
            batch.set(txRef, txPayload)
            batch.set(notifRef, notifPayload)
            batch.set(sessionRef, sessionPayload)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateAccountDetails(
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
        coverPhotoUrl: String
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val cleanName = displayName.trim().take(50)
            val cleanUsername = username.trim().take(30)
            val cleanEmail = email.trim().take(120)
            val cleanPhone = phone.trim().take(25)
            val cleanCity = city.trim().ifBlank { country.defaultCity }.take(60)
            require(cleanName.isNotEmpty()) { "Full name is required" }
            require(cleanUsername.length >= 3) { "Username must be at least 3 characters" }
            require(cleanEmail.contains("@")) { "Valid email is required" }
            require(cleanPhone.length >= 7) { "Valid phone is required" }

            path = "users/$uid"
            val privateUpdates = mapOf(
                "displayName" to cleanName,
                "username" to cleanUsername,
                "email" to cleanEmail,
                "phone" to cleanPhone,
                "dateOfBirth" to dateOfBirth.trim().take(30),
                "gender" to gender.trim().take(30),
                "countryCode" to country.code,
                "countryName" to country.name,
                "countryFlag" to country.flag,
                "currencyCode" to country.currencyCode,
                "city" to cleanCity,
                "bio" to bio.trim().take(200),
                "website" to website.trim().take(150),
                "work" to work.trim().take(100),
                "education" to education.trim().take(100),
                "relationshipStatus" to relationshipStatus.trim().take(40),
                "photoUrl" to photoUrl.trim().take(500),
                "coverPhotoUrl" to coverPhotoUrl.trim().take(500),
                "accountStatus" to "active",
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val publicUpdates = mapOf(
                "displayName" to cleanName,
                "username" to cleanUsername,
                "countryCode" to country.code,
                "countryName" to country.name,
                "countryFlag" to country.flag,
                "city" to cleanCity,
                "bio" to bio.trim().take(200),
                "website" to website.trim().take(150),
                "work" to work.trim().take(100),
                "education" to education.trim().take(100),
                "relationshipStatus" to relationshipStatus.trim().take(40),
                "photoUrl" to photoUrl.trim().take(500),
                "coverPhotoUrl" to coverPhotoUrl.trim().take(500),
                "accountStatus" to "active",
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.update(db.collection("users").document(uid), privateUpdates)
            batch.update(db.collection("users_public").document(uid), publicUpdates)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePrivacySettings(
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
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val batch = db.batch()
            batch.update(
                db.collection("users").document(uid),
                mapOf(
                    "privacyPosts" to privacyPosts,
                    "privacyFollow" to privacyFollow,
                    "privacyMessage" to privacyMessage,
                    "privacyFriends" to privacyFriends,
                    "privacyTag" to privacyTag,
                    "privacyContact" to privacyContact,
                    "privacyBirthday" to privacyBirthday,
                    "searchableByEmail" to searchableByEmail,
                    "searchableByPhone" to searchableByPhone,
                    "searchableByUsername" to searchableByUsername,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("users_public").document(uid),
                mapOf(
                    "privacyFollow" to privacyFollow,
                    "privacyMessage" to privacyMessage,
                    "searchableByUsername" to searchableByUsername,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateNotificationSettings(
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
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            db.collection("users").document(uid).update(
                mapOf(
                    "notifLikes" to notifLikes,
                    "notifComments" to notifComments,
                    "notifFollows" to notifFollows,
                    "notifMessages" to notifMessages,
                    "notifChallenges" to notifChallenges,
                    "notifLive" to notifLive,
                    "notifFriendRequests" to notifFriendRequests,
                    "notifPush" to notifPush,
                    "notifSound" to notifSound,
                    "notifVibration" to notifVibration,
                    "notifEmail" to notifEmail,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateSecuritySettings(
        twoFactorEnabled: Boolean,
        loginAlertsEnabled: Boolean
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            db.collection("users").document(uid).update(
                mapOf(
                    "twoFactorEnabled" to twoFactorEnabled,
                    "loginAlertsEnabled" to loginAlertsEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateAppearanceAndAdsSettings(
        themeMode: String,
        fontSize: String,
        language: String,
        dataSaver: Boolean,
        autoPlayVideos: Boolean,
        showFewerAds: Boolean
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val safeLang = if (GlobalLanguages.supported.any { it.first == language }) language else "en"
            val safeTheme = if (themeMode in listOf("dark", "light", "system")) themeMode else "dark"
            val safeFont = if (fontSize in listOf("small", "medium", "large")) fontSize else "medium"
            db.collection("users").document(uid).update(
                mapOf(
                    "darkMode" to (safeTheme == "dark"),
                    "themeMode" to safeTheme,
                    "fontSize" to safeFont,
                    "language" to safeLang,
                    "dataSaver" to dataSaver,
                    "autoPlayVideos" to autoPlayVideos,
                    "showFewerAds" to showFewerAds,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun registerCurrentSession(city: String, countryName: String): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            path = "users/$uid/sessions/current_device"
            val deviceLabel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}".take(80)
            val sessionRef = db.collection("users").document(uid).collection("sessions").document("current_device")
            val existing = sessionRef.get().await()
            if (existing.exists()) {
                sessionRef.update(
                    mapOf(
                        "deviceName" to deviceLabel,
                        "city" to city.ifBlank { "Global" }.take(60),
                        "countryName" to countryName.ifBlank { "Global" }.take(60),
                        "activeFlag" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                sessionRef.set(
                    mapOf(
                        "userId" to uid,
                        "deviceName" to deviceLabel,
                        "city" to city.ifBlank { "Global" }.take(60),
                        "countryName" to countryName.ifBlank { "Global" }.take(60),
                        "activeFlag" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun revokeOtherSessions(): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            path = "users/$uid/sessions"
            val snap = db.collection("users").document(uid).collection("sessions").get().await()
            val batch = db.batch()
            snap.documents.forEach { doc ->
                if (doc.id != "current_device") {
                    batch.delete(doc.reference)
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun deactivateAccount(): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val batch = db.batch()
            batch.update(
                db.collection("users").document(uid),
                mapOf("accountStatus" to "deactivated", "updatedAt" to FieldValue.serverTimestamp())
            )
            batch.update(
                db.collection("users_public").document(uid),
                mapOf("accountStatus" to "deactivated", "updatedAt" to FieldValue.serverTimestamp())
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAccountData(): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val myPosts = db.collection("posts").whereEqualTo("visibility", "public").get().await()
                .documents.filter { it.getString("authorId") == uid }
            val myStories = db.collection("stories").whereEqualTo("visibility", "public").get().await()
                .documents.filter { it.getString("authorId") == uid }
            val myVideos = db.collection("videos").whereEqualTo("visibility", "public").get().await()
                .documents.filter { it.getString("authorId") == uid }
            val myMarket = db.collection("marketplace").whereEqualTo("visibility", "public").get().await()
                .documents.filter { it.getString("sellerId") == uid }

            val batch = db.batch()
            myPosts.take(150).forEach { batch.delete(it.reference) }
            myStories.take(50).forEach { batch.delete(it.reference) }
            myVideos.take(100).forEach { batch.delete(it.reference) }
            myMarket.take(100).forEach { batch.delete(it.reference) }
            batch.delete(db.collection("users").document(uid))
            batch.delete(db.collection("users_public").document(uid))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun buildUserDataExportJson(user: PrivateUser): String {
        val uid = requireUserId()
        val myPosts = db.collection("posts")
            .whereEqualTo("visibility", "public")
            .get().await()
            .toObjects(Post::class.java)
            .filter { it.authorId == uid }
        val myTxs = db.collection("users").document(uid).collection("wallet_transactions")
            .get().await()
            .toObjects(WalletTransaction::class.java)

        val root = JSONObject().apply {
            put("app", "WeApp")
            put("exportedAtMillis", System.currentTimeMillis())
            put("profile", JSONObject().apply {
                put("userId", user.userId)
                put("username", user.username)
                put("displayName", user.displayName)
                put("email", user.email)
                put("phone", user.phone)
                put("country", "${user.countryFlag} ${user.countryName} (${user.countryCode})")
                put("city", user.city)
                put("currency", user.currencyCode)
                put("plan", user.plan)
                put("coins", user.coins)
                put("bio", user.bio)
                put("work", user.work)
                put("education", user.education)
                put("website", user.website)
                put("relationshipStatus", user.relationshipStatus)
            })
            put("posts", JSONArray().apply {
                myPosts.forEach { p ->
                    put(JSONObject().apply {
                        put("id", p.id)
                        put("category", p.category)
                        put("content", p.content)
                        put("likesCount", p.likesCount)
                    })
                }
            })
            put("walletTransactions", JSONArray().apply {
                myTxs.forEach { tx ->
                    put(JSONObject().apply {
                        put("title", tx.title)
                        put("type", tx.txType)
                        put("amount", tx.amount)
                    })
                }
            })
        }
        return root.toString(2)
    }

    // --- Safety, Blocking, Muting, Reporting & Notifications ---

    suspend fun setSafetyEntry(targetUserId: String, targetName: String, listType: String): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            if (uid == targetUserId) return Result.success(Unit)
            val safeType = if (listType in listOf("blocked", "muted", "restricted")) listType else "blocked"
            val docRef = db.collection("users").document(uid).collection("safety_lists").document(targetUserId)
            path = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "targetUserId" to targetUserId,
                    "targetName" to targetName.trim().ifBlank { "WeApp User" }.take(50),
                    "listType" to safeType,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun removeSafetyEntry(targetUserId: String): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("safety_lists").document(targetUserId)
            path = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun hidePost(postId: String): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("hidden_posts").document(postId)
            path = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "targetId" to postId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitReport(
        targetType: String,
        targetId: String,
        reason: String,
        details: String
    ): Result<Unit> {
        val docRef = db.collection("reports").document()
        return try {
            val uid = requireUserId()
            val safeType = if (targetType in listOf("post", "user", "comment")) targetType else "post"
            val safeReason = if (reason in ReportReasons.all) reason else "Spam"
            docRef.set(
                mapOf(
                    "reporterId" to uid,
                    "targetType" to safeType,
                    "targetId" to targetId,
                    "reason" to safeReason,
                    "details" to details.trim().take(500),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsRead(): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            path = "users/$uid/notifications"
            val snap = db.collection("users").document(uid).collection("notifications").get().await()
            val batch = db.batch()
            snap.documents.forEach { doc ->
                if (doc.getBoolean("readFlag") == false) {
                    batch.update(doc.reference, "readFlag", true)
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    private suspend fun createSelfNotification(
        uid: String,
        actorName: String,
        notifType: String,
        title: String,
        body: String
    ) {
        val ref = db.collection("users").document(uid).collection("notifications").document()
        ref.set(
            mapOf(
                "userId" to uid,
                "actorId" to uid,
                "actorName" to actorName.take(50),
                "notifType" to notifType.take(30),
                "title" to title.take(120),
                "body" to body.take(300),
                "readFlag" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    // --- Wallet & Subscriptions ---

    private suspend fun awardCoins(uid: String, amount: Long, title: String, rankDelta: Long = 0L) {
        val txRef = db.collection("users").document(uid).collection("wallet_transactions").document()
        val batch = db.batch()
        val userUpdates = mutableMapOf<String, Any>(
            "coins" to FieldValue.increment(amount),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (rankDelta != 0L) {
            userUpdates["rankPoints"] = FieldValue.increment(rankDelta)
        }
        batch.update(db.collection("users").document(uid), userUpdates)
        batch.update(db.collection("users_public").document(uid), userUpdates)
        batch.set(
            txRef,
            mapOf(
                "userId" to uid,
                "title" to title.take(100),
                "txType" to "earn",
                "amount" to amount,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
    }

    suspend fun claimDailyCoins(): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            path = "users/$uid/wallet_transactions"
            awardCoins(uid, 25L, "Daily Check-In Reward", rankDelta = 5L)
            createSelfNotification(uid, "WeCoins", "wallet", "+25 WeCoins Earned", "Daily check-in reward credited to your wallet.")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun upgradeSubscription(plan: SubscriptionPlan): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val batch = db.batch()
            val updateMap = mutableMapOf<String, Any>(
                "plan" to plan.code,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (plan.bonusCoins > 0L) {
                updateMap["coins"] = FieldValue.increment(plan.bonusCoins)
            }
            batch.update(db.collection("users").document(uid), updateMap)
            batch.update(db.collection("users_public").document(uid), updateMap)
            if (plan.bonusCoins > 0L) {
                val txRef = db.collection("users").document(uid).collection("wallet_transactions").document()
                batch.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "title" to "${plan.code} Plan Bonus",
                        "txType" to "earn",
                        "amount" to plan.bonusCoins,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            createSelfNotification(uid, "WeApp ${plan.code}", "plan", "Plan Updated: ${plan.code}", "Your WeApp plan is now ${plan.code}.")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- Stories & Posts ---

    suspend fun createStory(user: PrivateUser, caption: String, accentStyle: String): Result<Unit> {
        val docRef = db.collection("stories").document()
        return try {
            val uid = requireUserId()
            val cleanCaption = caption.trim().take(120)
            require(cleanCaption.isNotEmpty()) { "Story caption cannot be empty" }
            val safeStyle = if (accentStyle in listOf("purple", "blue", "sunset", "emerald")) accentStyle else "purple"
            docRef.set(
                mapOf(
                    "authorId" to uid,
                    "authorName" to user.displayName.ifBlank { user.username }.take(50),
                    "authorCity" to user.city.ifBlank { "Global" }.take(60),
                    "caption" to cleanCaption,
                    "accentStyle" to safeStyle,
                    "visibility" to "public",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createPost(user: PrivateUser, content: String, category: String): Result<Unit> {
        val docRef = db.collection("posts").document()
        return try {
            val uid = requireUserId()
            val cleanContent = content.trim().take(1000)
            val cleanCat = category.trim().ifBlank { "Social" }.take(40)
            require(cleanContent.isNotEmpty()) { "Post content cannot be empty" }

            val batch = db.batch()
            batch.set(
                docRef,
                mapOf(
                    "authorId" to uid,
                    "authorName" to user.displayName.ifBlank { user.username }.take(50),
                    "authorCity" to user.city.ifBlank { "Global" }.take(60),
                    "authorCountryCode" to user.countryCode.ifBlank { "US" }.take(5),
                    "authorCountryFlag" to user.countryFlag.ifBlank { "🌐" }.take(12),
                    "authorPlan" to user.plan,
                    "content" to cleanContent,
                    "category" to cleanCat,
                    "postPrivacy" to user.privacyPosts.ifBlank { "Everyone" }.take(30),
                    "visibility" to "public",
                    "likesCount" to 0L,
                    "commentsCount" to 0L,
                    "sharesCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("users").document(uid),
                mapOf(
                    "postsCount" to FieldValue.increment(1L),
                    "coins" to FieldValue.increment(10L),
                    "rankPoints" to FieldValue.increment(5L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("users_public").document(uid),
                mapOf(
                    "postsCount" to FieldValue.increment(1L),
                    "coins" to FieldValue.increment(10L),
                    "rankPoints" to FieldValue.increment(5L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val txRef = db.collection("users").document(uid).collection("wallet_transactions").document()
            batch.set(
                txRef,
                mapOf(
                    "userId" to uid,
                    "title" to "Created a Post (+10)",
                    "txType" to "earn",
                    "amount" to 10L,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleLikePost(postId: String, currentlyLiked: Boolean): Result<Unit> {
        val postRef = db.collection("posts").document(postId)
        return try {
            val uid = requireUserId()
            val likeRef = db.collection("users").document(uid).collection("liked_posts").document(postId)
            val batch = db.batch()
            if (currentlyLiked) {
                batch.delete(likeRef)
                batch.update(
                    postRef,
                    mapOf(
                        "likesCount" to FieldValue.increment(-1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    likeRef,
                    mapOf(
                        "userId" to uid,
                        "targetId" to postId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    postRef,
                    mapOf(
                        "likesCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, postRef.path)
            Result.failure(e)
        }
    }

    suspend fun addPostComment(postId: String, authorName: String, text: String): Result<Unit> {
        val postRef = db.collection("posts").document(postId)
        val commentRef = postRef.collection("comments").document()
        return try {
            val uid = requireUserId()
            val cleanText = text.trim().take(400)
            require(cleanText.isNotEmpty()) { "Comment cannot be empty" }
            val batch = db.batch()
            batch.set(
                commentRef,
                mapOf(
                    "parentId" to postId,
                    "authorId" to uid,
                    "authorName" to authorName.take(50),
                    "text" to cleanText,
                    "visibility" to "public",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                postRef,
                mapOf(
                    "commentsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, commentRef.path)
            Result.failure(e)
        }
    }

    suspend fun sharePost(postId: String): Result<Unit> {
        val postRef = db.collection("posts").document(postId)
        return try {
            requireUserId()
            postRef.update(
                mapOf(
                    "sharesCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, postRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleFollowUser(
        targetUserId: String,
        targetName: String,
        currentlyFollowing: Boolean
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            if (uid == targetUserId) return Result.success(Unit)
            val followRef = db.collection("users").document(uid).collection("following").document(targetUserId)
            path = followRef.path
            val delta = if (currentlyFollowing) -1L else 1L
            val batch = db.batch()
            if (currentlyFollowing) {
                batch.delete(followRef)
            } else {
                batch.set(
                    followRef,
                    mapOf(
                        "userId" to uid,
                        "targetUserId" to targetUserId,
                        "targetName" to targetName.take(50),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.update(
                db.collection("users").document(uid),
                mapOf(
                    "followingCount" to FieldValue.increment(delta),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("users_public").document(uid),
                mapOf(
                    "followingCount" to FieldValue.increment(delta),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("users_public").document(targetUserId),
                mapOf(
                    "followersCount" to FieldValue.increment(delta),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            if (!currentlyFollowing) {
                createSelfNotification(uid, targetName, "follow", "Followed $targetName", "You are now following $targetName on WeApp.")
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- Videos ---

    suspend fun createVideoReel(
        user: PrivateUser,
        title: String,
        caption: String,
        category: String,
        durationLabel: String,
        accentTheme: String
    ): Result<Unit> {
        val docRef = db.collection("videos").document()
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(120)
            require(cleanTitle.isNotEmpty()) { "Reel title required" }

            val batch = db.batch()
            batch.set(
                docRef,
                mapOf(
                    "authorId" to uid,
                    "authorName" to user.displayName.ifBlank { user.username }.take(50),
                    "authorCity" to user.city.ifBlank { "Global" }.take(60),
                    "authorCountryCode" to user.countryCode.ifBlank { "US" }.take(5),
                    "authorCountryFlag" to user.countryFlag.ifBlank { "🌐" }.take(12),
                    "title" to cleanTitle,
                    "caption" to caption.trim().take(400),
                    "category" to category.trim().ifBlank { "Trending" }.take(40),
                    "durationLabel" to durationLabel.trim().ifBlank { "0:30" }.take(15),
                    "accentTheme" to accentTheme.ifBlank { "purple_blue" }.take(30),
                    "visibility" to "public",
                    "likesCount" to 0L,
                    "commentsCount" to 0L,
                    "sharesCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("users").document(uid),
                mapOf(
                    "coins" to FieldValue.increment(15L),
                    "rankPoints" to FieldValue.increment(10L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("users_public").document(uid),
                mapOf(
                    "coins" to FieldValue.increment(15L),
                    "rankPoints" to FieldValue.increment(10L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val txRef = db.collection("users").document(uid).collection("wallet_transactions").document()
            batch.set(
                txRef,
                mapOf(
                    "userId" to uid,
                    "title" to "Uploaded Video Reel (+15)",
                    "txType" to "earn",
                    "amount" to 15L,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleLikeVideo(videoId: String, currentlyLiked: Boolean): Result<Unit> {
        val videoRef = db.collection("videos").document(videoId)
        return try {
            val uid = requireUserId()
            val likeRef = db.collection("users").document(uid).collection("liked_videos").document(videoId)
            val batch = db.batch()
            if (currentlyLiked) {
                batch.delete(likeRef)
                batch.update(
                    videoRef,
                    mapOf(
                        "likesCount" to FieldValue.increment(-1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    likeRef,
                    mapOf(
                        "userId" to uid,
                        "targetId" to videoId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    videoRef,
                    mapOf(
                        "likesCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, videoRef.path)
            Result.failure(e)
        }
    }

    suspend fun addVideoComment(videoId: String, authorName: String, text: String): Result<Unit> {
        val videoRef = db.collection("videos").document(videoId)
        val commentRef = videoRef.collection("comments").document()
        return try {
            val uid = requireUserId()
            val cleanText = text.trim().take(400)
            require(cleanText.isNotEmpty()) { "Comment cannot be empty" }
            val batch = db.batch()
            batch.set(
                commentRef,
                mapOf(
                    "parentId" to videoId,
                    "authorId" to uid,
                    "authorName" to authorName.take(50),
                    "text" to cleanText,
                    "visibility" to "public",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                videoRef,
                mapOf(
                    "commentsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, commentRef.path)
            Result.failure(e)
        }
    }

    suspend fun shareVideo(videoId: String): Result<Unit> {
        val videoRef = db.collection("videos").document(videoId)
        return try {
            requireUserId()
            videoRef.update(
                mapOf(
                    "sharesCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, videoRef.path)
            Result.failure(e)
        }
    }

    // --- Gaming Arena ---

    suspend fun createGameChallenge(
        user: PrivateUser,
        gameName: String,
        mode: String,
        note: String,
        stakeCoins: Long
    ): Result<Unit> {
        val docRef = db.collection("game_challenges").document()
        return try {
            val uid = requireUserId()
            val safeGame = if (gameName in SupportedGames.all) gameName else "Free Fire"
            val safeMode = if (mode in listOf("Challenge", "Find Team")) mode else "Challenge"
            val safeStake = stakeCoins.coerceIn(0L, 50000L)
            docRef.set(
                mapOf(
                    "gameName" to safeGame,
                    "mode" to safeMode,
                    "creatorId" to uid,
                    "creatorName" to user.displayName.ifBlank { user.username }.take(50),
                    "city" to "${user.countryFlag} ${user.city.ifBlank { "Global" }}".take(60),
                    "note" to note.trim().take(200),
                    "stakeCoins" to safeStake,
                    "status" to "open",
                    "visibility" to "public",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun acceptGameChallenge(challengeId: String, opponentName: String): Result<Unit> {
        val docRef = db.collection("game_challenges").document(challengeId)
        return try {
            val uid = requireUserId()
            docRef.update(
                mapOf(
                    "status" to "accepted",
                    "opponentId" to uid,
                    "opponentName" to opponentName.take(50),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun completeGameChallenge(challengeId: String): Result<Unit> {
        val docRef = db.collection("game_challenges").document(challengeId)
        return try {
            val uid = requireUserId()
            docRef.update(
                mapOf(
                    "status" to "completed",
                    "winnerId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            awardCoins(uid, 50L, "Arena Victory Reward (+50)", rankDelta = 25L)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Chat ---

    suspend fun startOrGetChat(myName: String, peerId: String, peerName: String): Result<String> {
        return try {
            val uid = requireUserId()
            require(peerId.isNotBlank() && peerId != uid) { "Select another registered user" }
            val ids = listOf(uid, peerId).sorted()
            val chatId = "${ids[0]}_${ids[1]}"
            val chatRef = db.collection("chats").document(chatId)
            val snap = chatRef.get().await()
            if (!snap.exists()) {
                chatRef.set(
                    mapOf(
                        "participantIds" to ids,
                        "creatorId" to uid,
                        "creatorName" to myName.take(50),
                        "peerId" to peerId,
                        "peerName" to peerName.take(50),
                        "lastMessage" to "",
                        "lastSenderId" to uid,
                        "typingUserIds" to emptyList<String>(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(chatId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "chats")
            Result.failure(e)
        }
    }

    suspend fun sendChatMessage(
        chatId: String,
        participantIds: List<String>,
        senderName: String,
        text: String
    ): Result<Unit> {
        val chatRef = db.collection("chats").document(chatId)
        val msgRef = chatRef.collection("messages").document()
        return try {
            val uid = requireUserId()
            val cleanText = text.trim().take(1000)
            require(cleanText.isNotEmpty()) { "Message cannot be empty" }
            val batch = db.batch()
            batch.set(
                msgRef,
                mapOf(
                    "chatId" to chatId,
                    "participantIds" to participantIds,
                    "senderId" to uid,
                    "senderName" to senderName.take(50),
                    "text" to cleanText,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                chatRef,
                mapOf(
                    "lastMessage" to cleanText.take(500),
                    "lastSenderId" to uid,
                    "typingUserIds" to emptyList<String>(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            Result.failure(e)
        }
    }

    suspend fun setTypingStatus(chatId: String, typing: Boolean): Result<Unit> {
        val chatRef = db.collection("chats").document(chatId)
        return try {
            val uid = requireUserId()
            val update = if (typing) FieldValue.arrayUnion(uid) else FieldValue.arrayRemove(uid)
            chatRef.update(
                mapOf(
                    "typingUserIds" to update,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, chatRef.path)
            Result.failure(e)
        }
    }

    // --- Marketplace ---

    suspend fun createMarketplaceListing(
        user: PrivateUser,
        title: String,
        description: String,
        category: String,
        priceNgn: Long
    ): Result<Unit> {
        val docRef = db.collection("marketplace").document()
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(100)
            val cleanDesc = description.trim().take(500)
            require(cleanTitle.isNotEmpty() && cleanDesc.isNotEmpty()) { "Title and description required" }
            docRef.set(
                mapOf(
                    "sellerId" to uid,
                    "sellerName" to user.displayName.ifBlank { user.username }.take(50),
                    "sellerCity" to user.city.ifBlank { "Global" }.take(60),
                    "sellerCountryCode" to user.countryCode.ifBlank { "US" }.take(5),
                    "sellerCountryFlag" to user.countryFlag.ifBlank { "🌐" }.take(12),
                    "currencyCode" to user.currencyCode.ifBlank { "USD" }.take(6),
                    "title" to cleanTitle,
                    "description" to cleanDesc,
                    "category" to category.trim().ifBlank { "General" }.take(40),
                    "priceNgn" to priceNgn.coerceIn(0L, 100_000_000L),
                    "status" to "available",
                    "visibility" to "public",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun markListingSold(itemId: String): Result<Unit> {
        val docRef = db.collection("marketplace").document(itemId)
        return try {
            requireUserId()
            docRef.update(
                mapOf(
                    "status" to "sold",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Live Rooms ---

    suspend fun createLiveRoom(user: PrivateUser, title: String, topic: String): Result<String> {
        val docRef = db.collection("live_rooms").document()
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(100)
            require(cleanTitle.isNotEmpty()) { "Live room title required" }
            docRef.set(
                mapOf(
                    "hostId" to uid,
                    "hostName" to user.displayName.ifBlank { user.username }.take(50),
                    "title" to cleanTitle,
                    "topic" to topic.trim().ifBlank { "Hangout" }.take(50),
                    "status" to "live",
                    "viewerCount" to 1L,
                    "visibility" to "public",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun adjustLiveViewerCount(roomId: String, delta: Long): Result<Unit> {
        val docRef = db.collection("live_rooms").document(roomId)
        return try {
            requireUserId()
            docRef.update(
                mapOf(
                    "viewerCount" to FieldValue.increment(delta),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun sendLiveMessage(roomId: String, senderName: String, text: String): Result<Unit> {
        val msgRef = db.collection("live_rooms").document(roomId).collection("messages").document()
        return try {
            val uid = requireUserId()
            val cleanText = text.trim().take(300)
            require(cleanText.isNotEmpty()) { "Message required" }
            msgRef.set(
                mapOf(
                    "roomId" to roomId,
                    "senderId" to uid,
                    "senderName" to senderName.take(50),
                    "text" to cleanText,
                    "visibility" to "public",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            Result.failure(e)
        }
    }

    suspend fun endLiveRoom(roomId: String): Result<Unit> {
        val docRef = db.collection("live_rooms").document(roomId)
        return try {
            requireUserId()
            docRef.update(
                mapOf(
                    "status" to "ended",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }
}
