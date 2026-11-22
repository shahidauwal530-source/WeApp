package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class PrivateUser(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val email: String = "",
    val phone: String = "",
    val countryCode: String = "US",
    val countryName: String = "United States",
    val countryFlag: String = "🇺🇸",
    val city: String = "New York",
    val currencyCode: String = "USD",
    val bio: String = "",
    val dateOfBirth: String = "",
    val gender: String = "",
    val website: String = "",
    val work: String = "",
    val education: String = "",
    val relationshipStatus: String = "",
    val photoUrl: String = "",
    val coverPhotoUrl: String = "",
    val plan: String = "FREE",
    val coins: Long = 0L,
    val rankPoints: Long = 0L,
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val postsCount: Long = 0L,
    val darkMode: Boolean = true,
    val themeMode: String = "dark",
    val fontSize: String = "medium",
    val language: String = "en",
    val dataSaver: Boolean = false,
    val autoPlayVideos: Boolean = true,
    val showFewerAds: Boolean = false,
    val twoFactorEnabled: Boolean = false,
    val loginAlertsEnabled: Boolean = true,
    val accountStatus: String = "active",
    val privacyPosts: String = "Everyone",
    val privacyFollow: String = "Everyone",
    val privacyMessage: String = "Everyone",
    val privacyFriends: String = "Everyone",
    val privacyTag: String = "Everyone",
    val privacyContact: String = "Only Me",
    val privacyBirthday: String = "Friends",
    val searchableByEmail: Boolean = true,
    val searchableByPhone: Boolean = true,
    val searchableByUsername: Boolean = true,
    val notifLikes: Boolean = true,
    val notifComments: Boolean = true,
    val notifFollows: Boolean = true,
    val notifMessages: Boolean = true,
    val notifChallenges: Boolean = true,
    val notifLive: Boolean = true,
    val notifFriendRequests: Boolean = true,
    val notifPush: Boolean = true,
    val notifSound: Boolean = true,
    val notifVibration: Boolean = true,
    val notifEmail: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PublicUser(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val countryCode: String = "US",
    val countryName: String = "United States",
    val countryFlag: String = "🇺🇸",
    val city: String = "New York",
    val bio: String = "",
    val website: String = "",
    val work: String = "",
    val education: String = "",
    val relationshipStatus: String = "",
    val photoUrl: String = "",
    val coverPhotoUrl: String = "",
    val plan: String = "FREE",
    val coins: Long = 0L,
    val rankPoints: Long = 0L,
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val postsCount: Long = 0L,
    val privacyMessage: String = "Everyone",
    val privacyFollow: String = "Everyone",
    val searchableByUsername: Boolean = true,
    val accountStatus: String = "active",
    val visibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Story(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorCity: String = "",
    val caption: String = "",
    val accentStyle: String = "purple",
    val visibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Post(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorCity: String = "",
    val authorCountryCode: String = "US",
    val authorCountryFlag: String = "🇺🇸",
    val authorPlan: String = "FREE",
    val content: String = "",
    val category: String = "Social",
    val postPrivacy: String = "Everyone",
    val visibility: String = "public",
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val sharesCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Comment(
    @DocumentId val id: String = "",
    val parentId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val text: String = "",
    val visibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VideoReel(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorCity: String = "",
    val authorCountryCode: String = "US",
    val authorCountryFlag: String = "🇺🇸",
    val title: String = "",
    val caption: String = "",
    val category: String = "Trending",
    val durationLabel: String = "0:30",
    val videoUrl: String? = null,
    val accentTheme: String = "purple_blue",
    val visibility: String = "public",
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val sharesCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GameChallenge(
    @DocumentId val id: String = "",
    val gameName: String = "Free Fire",
    val mode: String = "Challenge",
    val creatorId: String = "",
    val creatorName: String = "",
    val city: String = "",
    val note: String = "",
    val stakeCoins: Long = 0L,
    val status: String = "open",
    val opponentId: String? = null,
    val opponentName: String? = null,
    val winnerId: String? = null,
    val visibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatThread(
    @DocumentId val id: String = "",
    val participantIds: List<String> = emptyList(),
    val creatorId: String = "",
    val creatorName: String = "",
    val peerId: String = "",
    val peerName: String = "",
    val lastMessage: String = "",
    val lastSenderId: String = "",
    val typingUserIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val participantIds: List<String> = emptyList(),
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MarketplaceListing(
    @DocumentId val id: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val sellerCity: String = "",
    val sellerCountryCode: String = "US",
    val sellerCountryFlag: String = "🇺🇸",
    val currencyCode: String = "USD",
    val title: String = "",
    val description: String = "",
    val category: String = "Electronics",
    val priceNgn: Long = 0L,
    val status: String = "available",
    val visibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LiveRoom(
    @DocumentId val id: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val title: String = "",
    val topic: String = "General",
    val status: String = "live",
    val viewerCount: Long = 1L,
    val visibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LiveMessage(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val visibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WalletTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val txType: String = "earn",
    val amount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SafetyListEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val targetUserId: String = "",
    val targetName: String = "",
    val listType: String = "blocked",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val notifType: String = "system",
    val title: String = "",
    val body: String = "",
    val readFlag: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LoginSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val deviceName: String = "",
    val city: String = "",
    val countryName: String = "",
    val activeFlag: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ContentReport(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetType: String = "post",
    val targetId: String = "",
    val reason: String = "Spam",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserLike(
    @DocumentId val id: String = "",
    val userId: String = "",
    val targetId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserFollow(
    @DocumentId val id: String = "",
    val userId: String = "",
    val targetUserId: String = "",
    val targetName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class SubscriptionPlan(val code: String, val priceNgn: Long, val bonusCoins: Long) {
    FREE("FREE", 0L, 0L),
    PRO("PRO", 1500L, 300L),
    VIP("VIP", 3000L, 1000L);

    companion object {
        fun fromCode(raw: String): SubscriptionPlan =
            entries.firstOrNull { it.code == raw } ?: FREE
    }
}

object SupportedGames {
    val all = listOf("Free Fire", "PUBG", "EA FC", "COD", "eFootball")
}

object ReportReasons {
    val all = listOf("Spam", "Harassment", "Fake", "Violence")
}

enum class GlobalFeedFilter {
    FOR_YOU, FOLLOWING, NEAR_ME, TRENDING
}
