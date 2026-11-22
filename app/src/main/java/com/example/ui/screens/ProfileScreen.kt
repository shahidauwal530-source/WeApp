package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.*
import com.example.localization.AppStrings
import com.example.ui.UiState
import com.example.ui.components.PlanBadge
import com.example.ui.components.WeAppAvatar
import com.example.ui.components.WeAppEmptyState
import com.example.ui.theme.*

@Composable
fun ProfileScreen(
    currentUser: PrivateUser,
    postsState: UiState<List<Post>>,
    strings: AppStrings,
    onOpenEditProfile: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenMarketplace: () -> Unit,
    onOpenLive: () -> Unit,
    onOpenSubscription: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val myPosts = remember(postsState, currentUser.userId) {
        ((postsState as? UiState.Success)?.data ?: emptyList())
            .filter { it.authorId == currentUser.userId }
    }
    val country = remember(currentUser.countryCode) { GlobalCountries.byCode(currentUser.countryCode) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Cover Photo + Profile Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WeBrandGradient)
            ) {
                if (currentUser.coverPhotoUrl.isNotBlank()) {
                    AsyncImage(
                        model = currentUser.coverPhotoUrl,
                        contentDescription = "Cover photo",
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(0.35f), Color.Black.copy(0.75f))
                                )
                            )
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = onOpenEditProfile,
                            modifier = Modifier.testTag("profile_edit_button")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = strings.editProfile, tint = Color.White)
                        }
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.testTag("profile_settings_button")
                        ) {
                            Icon(Icons.Filled.Settings, contentDescription = strings.settingsTitle, tint = Color.White)
                        }
                    }

                    WeAppAvatar(
                        name = currentUser.displayName.ifBlank { currentUser.username },
                        size = 86.dp,
                        highlightBorder = true,
                        photoUrl = currentUser.photoUrl
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = currentUser.displayName.ifBlank { currentUser.username },
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        PlanBadge(currentUser.plan)
                    }

                    Text(
                        text = "@${currentUser.username} • ${currentUser.countryFlag} ${currentUser.city}, ${currentUser.countryName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    if (currentUser.bio.isNotBlank()) {
                        Text(
                            text = currentUser.bio,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.92f)
                        )
                    }

                    // Work, Education, Website, Relationship Status details
                    val metaLines = listOfNotNull(
                        currentUser.work.takeIf { it.isNotBlank() }?.let { "💼 $it" },
                        currentUser.education.takeIf { it.isNotBlank() }?.let { "🎓 $it" },
                        currentUser.relationshipStatus.takeIf { it.isNotBlank() }?.let { "❤️ $it" },
                        currentUser.website.takeIf { it.isNotBlank() }?.let { "🔗 $it" }
                    )
                    if (metaLines.isNotEmpty()) {
                        Text(
                            text = metaLines.joinToString("  •  "),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Surface(
                        color = Color.Black.copy(alpha = 0.30f),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ProfileStat(value = currentUser.followersCount.toString(), label = strings.followersLabel)
                            ProfileStat(value = currentUser.postsCount.toString(), label = strings.postsLabel)
                            ProfileStat(value = currentUser.coins.toString(), label = strings.coinsLabel)
                        }
                    }
                }
            }
        }

        // Hub Shortcuts (Wallet, Marketplace, Live, Subscription)
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProfileActionCard(
                        icon = Icons.Filled.AccountBalanceWallet,
                        title = strings.walletTitle,
                        subtitle = GlobalFormatters.formatCoinsValue(currentUser.coins, country),
                        accent = WeGold,
                        modifier = Modifier.weight(1f),
                        testTag = "profile_wallet_card",
                        onClick = onOpenWallet
                    )
                    ProfileActionCard(
                        icon = Icons.Filled.WorkspacePremium,
                        title = strings.subscriptionTitle,
                        subtitle = currentUser.plan,
                        accent = WePurpleLight,
                        modifier = Modifier.weight(1f),
                        testTag = "profile_subscription_card",
                        onClick = onOpenSubscription
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProfileActionCard(
                        icon = Icons.Filled.Storefront,
                        title = strings.marketplaceTitle,
                        subtitle = country.currencyCode,
                        accent = WeBlueLight,
                        modifier = Modifier.weight(1f),
                        testTag = "profile_marketplace_card",
                        onClick = onOpenMarketplace
                    )
                    ProfileActionCard(
                        icon = Icons.Filled.LiveTv,
                        title = strings.liveTitle,
                        subtitle = "Global Live",
                        accent = WeRose,
                        modifier = Modifier.weight(1f),
                        testTag = "profile_live_card",
                        onClick = onOpenLive
                    )
                }
            }
        }

        item {
            Text(
                text = strings.postsLabel,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (myPosts.isEmpty()) {
            item {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    WeAppEmptyState(
                        icon = Icons.Filled.Article,
                        title = strings.noPostsYet,
                        subtitle = strings.whatsOnYourMind
                    )
                }
            }
        } else {
            items(myPosts, key = { it.id }) { post ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "${post.category} • ${GlobalFormatters.formatRelativeTime(post.createdAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = WePurpleLight
                        )
                        Text(post.content, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "❤️ ${post.likesCount}  •  💬 ${post.commentsCount}  •  ↗ ${post.sharesCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
    }
}

@Composable
private fun ProfileActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(24.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = accent, maxLines = 1)
            }
        }
    }
}
