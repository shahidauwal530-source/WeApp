package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.localization.AppStrings
import com.example.ui.UiState
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeedScreen(
    currentUser: PrivateUser,
    storiesState: UiState<List<Story>>,
    postsState: UiState<List<Post>>,
    feedFilter: GlobalFeedFilter,
    likedPostIds: Set<String>,
    hiddenPostIds: Set<String>,
    followingIds: Set<String>,
    safetyList: List<SafetyListEntry>,
    unreadNotificationsCount: Int,
    showFewerAds: Boolean,
    activeCommentPostId: String?,
    postComments: List<Comment>,
    strings: AppStrings,
    onSelectFeedFilter: (GlobalFeedFilter) -> Unit,
    onCreateStory: (caption: String, accentStyle: String) -> Unit,
    onCreatePost: (content: String, category: String) -> Unit,
    onToggleLike: (String) -> Unit,
    onOpenComments: (String?) -> Unit,
    onAddComment: (postId: String, text: String) -> Unit,
    onSharePost: (String) -> Unit,
    onToggleFollow: (targetUserId: String, targetName: String) -> Unit,
    onHidePost: (String) -> Unit,
    onMuteUser: (targetUserId: String, targetName: String) -> Unit,
    onBlockUser: (targetUserId: String, targetName: String) -> Unit,
    onReportPost: (postId: String, reason: String, details: String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenLive: () -> Unit,
    onOpenMarketplace: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenSubscription: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    var postText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Social") }
    var showStoryDialog by remember { mutableStateOf(false) }
    var viewingStory by remember { mutableStateOf<Story?>(null) }
    var reportingPostId by remember { mutableStateOf<String?>(null) }

    val categories = listOf("Social", "Gaming", "Music", "Tech", "Global")
    val blockedOrMutedIds = remember(safetyList) {
        safetyList.filter { it.listType == "blocked" || it.listType == "muted" }
            .map { it.targetUserId }
            .toSet()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header Bar
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = strings.appName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = WePurpleLight,
                            fontWeight = FontWeight.Bold
                        )
                        Text(currentUser.countryFlag, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = strings.tagline,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onOpenSearch,
                        modifier = Modifier.testTag("home_search_button")
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = strings.searchTitle)
                    }
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier.testTag("home_notifications_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationsCount > 0) {
                                    Badge(containerColor = WeRose) {
                                        Text(
                                            text = unreadNotificationsCount.coerceAtMost(99).toString(),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.Notifications, contentDescription = strings.notificationsTitle)
                        }
                    }
                    IconButton(
                        onClick = onOpenLive,
                        modifier = Modifier.testTag("home_live_button")
                    ) {
                        Icon(Icons.Filled.LiveTv, contentDescription = strings.liveTitle, tint = WeRose)
                    }
                    Surface(
                        color = WeGold.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .testTag("home_wallet_chip")
                            .clickable(onClick = onOpenWallet)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.MonetizationOn, contentDescription = strings.walletTitle, tint = WeGold, modifier = Modifier.size(16.dp))
                            Text(
                                text = currentUser.coins.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = WeGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("home_settings_button")
                    ) {
                        Icon(Icons.Outlined.Settings, contentDescription = strings.settingsTitle)
                    }
                }
            }
        }

        // Global Feed Filter Pills (For You, Following, Near Me, Trending)
        Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filterItems = listOf(
                    GlobalFeedFilter.FOR_YOU to strings.feedForYou,
                    GlobalFeedFilter.FOLLOWING to strings.feedFollowing,
                    GlobalFeedFilter.NEAR_ME to "${currentUser.countryFlag} ${strings.feedNearMe}",
                    GlobalFeedFilter.TRENDING to "🔥 ${strings.feedTrending}"
                )
                items(filterItems) { (filter, label) ->
                    FilterChip(
                        selected = feedFilter == filter,
                        onClick = { onSelectFeedFilter(filter) },
                        label = { Text(label) },
                        modifier = Modifier.testTag("feed_filter_${filter.name.lowercase()}")
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Stories Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.storiesTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = onOpenMarketplace) {
                        Icon(Icons.Filled.Storefront, contentDescription = null, modifier = Modifier.size(16.dp), tint = WeBlueLight)
                        Spacer(Modifier.width(4.dp))
                        Text(strings.marketplaceTitle, color = WeBlueLight)
                    }
                }
                val stories = ((storiesState as? UiState.Success)?.data ?: emptyList())
                    .filter { it.authorId !in blockedOrMutedIds }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .testTag("add_story_button")
                                .width(72.dp)
                                .clickable { showStoryDialog = true }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(2.dp, WeBrandGradient, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = strings.addStory, tint = WePurpleLight)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = strings.addStory,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    items(stories, key = { it.id }) { story ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(72.dp)
                                .clickable { viewingStory = story }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .border(2.5.dp, WeBrandGradient, CircleShape)
                                    .padding(3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                WeAppAvatar(name = story.authorName, size = 54.dp)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = story.authorName,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Create Post Box
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            WeAppAvatar(
                                name = currentUser.displayName.ifBlank { currentUser.username },
                                size = 40.dp,
                                photoUrl = currentUser.photoUrl
                            )
                            OutlinedTextField(
                                value = postText,
                                onValueChange = { postText = it.take(1000) },
                                placeholder = { Text(strings.whatsOnYourMind) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("create_post_input"),
                                shape = RoundedCornerShape(16.dp),
                                maxLines = 4
                            )
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(categories) { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat) }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${currentUser.countryFlag} ${currentUser.city} • ${currentUser.privacyPosts}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            WeAppGradientButton(
                                text = strings.postBtn,
                                icon = Icons.Filled.Send,
                                enabled = postText.isNotBlank(),
                                testTag = "publish_post_button",
                                onClick = {
                                    onCreatePost(postText, selectedCategory)
                                    postText = ""
                                }
                            )
                        }
                    }
                }
            }

            // Subtle Free-Tier Promo Banner (Hidden automatically for PRO/VIP or Show Fewer Ads)
            if (currentUser.plan == "FREE" && !showFewerAds) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = WePurple.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, WePurple.copy(alpha = 0.35f)),
                        modifier = Modifier.clickable(onClick = onOpenSubscription)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("WeApp PRO / VIP • Ad-Free", style = MaterialTheme.typography.titleSmall, color = WePurpleLight, fontWeight = FontWeight.Bold)
                                Text("Unlock bonus WeCoins, zero ads & global priority badge.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            AssistChip(onClick = onOpenSubscription, label = { Text(strings.upgradeBtn) })
                        }
                    }
                }
            }

            // Posts Feed
            when (postsState) {
                is UiState.Loading -> {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = WePurple)
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        WeAppEmptyState(
                            icon = Icons.Outlined.CloudOff,
                            title = "Feed Unavailable",
                            subtitle = postsState.message
                        )
                    }
                }
                is UiState.Success -> {
                    val basePosts = postsState.data.filter { post ->
                        val isSelf = post.authorId == currentUser.userId
                        val allowedByPrivacy = when (post.postPrivacy) {
                            "Only Me" -> isSelf
                            "Friends" -> isSelf || post.authorId in followingIds
                            else -> true
                        }
                        post.id !in hiddenPostIds &&
                            post.authorId !in blockedOrMutedIds &&
                            allowedByPrivacy
                    }
                    val posts = when (feedFilter) {
                        GlobalFeedFilter.FOR_YOU -> basePosts
                        GlobalFeedFilter.FOLLOWING -> basePosts.filter { it.authorId in followingIds || it.authorId == currentUser.userId }
                        GlobalFeedFilter.NEAR_ME -> basePosts.filter {
                            it.authorCountryCode.equals(currentUser.countryCode, ignoreCase = true) ||
                                it.authorCity.equals(currentUser.city, ignoreCase = true)
                        }
                        GlobalFeedFilter.TRENDING -> basePosts.sortedByDescending {
                            it.likesCount + (it.commentsCount * 2L) + (it.sharesCount * 3L)
                        }
                    }
                    if (posts.isEmpty()) {
                        item {
                            WeAppEmptyState(
                                icon = Icons.Outlined.DynamicFeed,
                                title = strings.noPostsYet,
                                subtitle = strings.whatsOnYourMind
                            )
                        }
                    } else {
                        items(posts, key = { it.id }) { post ->
                            PostCard(
                                post = post,
                                isSelf = post.authorId == currentUser.userId,
                                isLiked = post.id in likedPostIds,
                                isFollowing = post.authorId in followingIds,
                                strings = strings,
                                onLike = { onToggleLike(post.id) },
                                onComment = { onOpenComments(post.id) },
                                onShare = {
                                    onSharePost(post.id)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "${post.authorName} on WeApp: ${post.content}")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, strings.share))
                                },
                                onFollow = { onToggleFollow(post.authorId, post.authorName) },
                                onHide = { onHidePost(post.id) },
                                onMuteAuthor = { onMuteUser(post.authorId, post.authorName) },
                                onBlockAuthor = { onBlockUser(post.authorId, post.authorName) },
                                onReport = { reportingPostId = post.id }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showStoryDialog) {
        CreateStoryDialog(
            strings = strings,
            onDismiss = { showStoryDialog = false },
            onPublish = { caption, style ->
                onCreateStory(caption, style)
                showStoryDialog = false
            }
        )
    }

    if (viewingStory != null) {
        val story = viewingStory!!
        AlertDialog(
            onDismissRequest = { viewingStory = null },
            confirmButton = {
                TextButton(onClick = { viewingStory = null }) { Text("Close") }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WeAppAvatar(name = story.authorName, size = 36.dp)
                    Column {
                        Text(story.authorName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${story.authorCity} • ${GlobalFormatters.formatRelativeTime(story.createdAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            text = {
                val bgColors = when (story.accentStyle) {
                    "blue" -> listOf(WeBlue, WeCyan)
                    "sunset" -> listOf(WeRose, WeGold)
                    "emerald" -> listOf(WeEmerald, WeBlue)
                    else -> listOf(WePurple, WeBlue)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(bgColors))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = story.caption,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                }
            }
        )
    }

    if (reportingPostId != null) {
        ReportContentDialog(
            title = "Report Post",
            onDismiss = { reportingPostId = null },
            onSubmit = { reason, details ->
                onReportPost(reportingPostId!!, reason, details)
                reportingPostId = null
            }
        )
    }

    if (activeCommentPostId != null) {
        CommentsBottomSheet(
            title = strings.commentsTitle,
            comments = postComments,
            placeholder = strings.writeComment,
            sendLabel = strings.send,
            onDismiss = { onOpenComments(null) },
            onSend = { text -> onAddComment(activeCommentPostId, text) }
        )
    }
}

@Composable
fun PostCard(
    post: Post,
    isSelf: Boolean,
    isLiked: Boolean,
    isFollowing: Boolean,
    strings: AppStrings,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onFollow: () -> Unit,
    onHide: () -> Unit,
    onMuteAuthor: () -> Unit,
    onBlockAuthor: () -> Unit,
    onReport: () -> Unit
) {
    val timeLabel = remember(post.createdAt) { GlobalFormatters.formatRelativeTime(post.createdAt) }
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("post_card_${post.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    WeAppAvatar(name = post.authorName, size = 42.dp)
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = post.authorName,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            PlanBadge(post.authorPlan)
                        }
                        Text(
                            text = "${post.authorCountryFlag} ${post.authorCity} • $timeLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isSelf) {
                        OutlinedButton(
                            onClick = onFollow,
                            modifier = Modifier.testTag("follow_button_${post.authorId}"),
                            shape = RoundedCornerShape(999.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, if (isFollowing) WeEmerald else WePurple)
                        ) {
                            Text(
                                text = if (isFollowing) strings.following else strings.follow,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isFollowing) WeEmerald else WePurpleLight
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Post options")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Hide Post") },
                                leadingIcon = { Icon(Icons.Outlined.VisibilityOff, null) },
                                onClick = {
                                    menuExpanded = false
                                    onHide()
                                }
                            )
                            if (!isSelf) {
                                DropdownMenuItem(
                                    text = { Text("Mute ${post.authorName}") },
                                    leadingIcon = { Icon(Icons.Outlined.VolumeOff, null) },
                                    onClick = {
                                        menuExpanded = false
                                        onMuteAuthor()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Block ${post.authorName}") },
                                    leadingIcon = { Icon(Icons.Outlined.Block, null, tint = WeRose) },
                                    onClick = {
                                        menuExpanded = false
                                        onBlockAuthor()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Report Post") },
                                    leadingIcon = { Icon(Icons.Outlined.Flag, null, tint = WeRose) },
                                    onClick = {
                                        menuExpanded = false
                                        onReport()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            AssistChip(
                onClick = {},
                label = { Text(post.category, style = MaterialTheme.typography.labelSmall) }
            )

            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = onLike,
                    modifier = Modifier.testTag("like_post_${post.id}")
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = strings.like,
                        tint = if (isLiked) WeRose else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${post.likesCount}",
                        color = if (isLiked) WeRose else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(
                    onClick = onComment,
                    modifier = Modifier.testTag("comment_post_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = strings.comment,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${post.commentsCount}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(
                    onClick = onShare,
                    modifier = Modifier.testTag("share_post_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = strings.share,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${post.sharesCount}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ReportContentDialog(
    title: String,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, details: String) -> Unit
) {
    var selectedReason by remember { mutableStateOf(ReportReasons.all.first()) }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select reason:", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ReportReasons.all) { reason ->
                        FilterChip(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            label = { Text(reason) }
                        )
                    }
                }
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it.take(500) },
                    label = { Text("Additional details (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedReason, details) },
                colors = ButtonDefaults.buttonColors(containerColor = WeRose)
            ) {
                Text("Submit Report", color = Color.White)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CreateStoryDialog(
    strings: AppStrings,
    onDismiss: () -> Unit,
    onPublish: (caption: String, style: String) -> Unit
) {
    var caption by remember { mutableStateOf("") }
    var style by remember { mutableStateOf("purple") }
    val styles = listOf("purple", "blue", "sunset", "emerald")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addStory) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it.take(120) },
                    placeholder = { Text(strings.whatsOnYourMind) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    styles.forEach { s ->
                        FilterChip(
                            selected = style == s,
                            onClick = { style = s },
                            label = { Text(s.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (caption.isNotBlank()) onPublish(caption, style) },
                enabled = caption.isNotBlank()
            ) {
                Text(strings.postBtn)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    title: String,
    comments: List<Comment>,
    placeholder: String,
    sendLabel: String,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (comments.isEmpty()) {
                Text(
                    text = "No comments yet. Start the conversation!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(comments, key = { it.id }) { comment ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            WeAppAvatar(name = comment.authorName, size = 32.dp)
                            Column {
                                Text(comment.authorName, style = MaterialTheme.typography.labelLarge)
                                Text(comment.text, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(400) },
                    placeholder = { Text(placeholder) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                )
                IconButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            onSend(text)
                            text = ""
                        }
                    },
                    enabled = text.isNotBlank()
                ) {
                    Icon(Icons.Filled.Send, contentDescription = sendLabel, tint = WePurpleLight)
                }
            }
        }
    }
}
