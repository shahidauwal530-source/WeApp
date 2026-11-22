package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.localization.AppStrings
import com.example.ui.UiState
import com.example.ui.components.WeAppAvatar
import com.example.ui.components.WeAppEmptyState
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun VideoFeedScreen(
    currentUser: PrivateUser,
    videosState: UiState<List<VideoReel>>,
    likedVideoIds: Set<String>,
    followingIds: Set<String>,
    safetyList: List<SafetyListEntry>,
    autoPlayVideos: Boolean,
    activeCommentVideoId: String?,
    videoComments: List<Comment>,
    strings: AppStrings,
    onCreateVideo: (title: String, caption: String, category: String, duration: String, theme: String) -> Unit,
    onToggleLikeVideo: (String) -> Unit,
    onOpenComments: (String?) -> Unit,
    onAddComment: (videoId: String, text: String) -> Unit,
    onShareVideo: (String) -> Unit,
    onToggleFollow: (targetUserId: String, targetName: String) -> Unit
) {
    val context = LocalContext.current
    var showUploadDialog by remember { mutableStateOf(false) }
    val blockedOrMutedIds = remember(safetyList) {
        safetyList.filter { it.listType == "blocked" || it.listType == "muted" }
            .map { it.targetUserId }
            .toSet()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF06050D))
    ) {
        when (videosState) {
            is UiState.Loading -> {
                CircularProgressIndicator(
                    color = WePurple,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    WeAppEmptyState(
                        icon = Icons.Outlined.VideocamOff,
                        title = "Video Feed Unavailable",
                        subtitle = videosState.message
                    )
                }
            }
            is UiState.Success -> {
                val videos = videosState.data.filter { it.authorId !in blockedOrMutedIds }
                if (videos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        WeAppEmptyState(
                            icon = Icons.Filled.PlayCircleFilled,
                            title = strings.noVideosYet,
                            subtitle = strings.doubleTapHint,
                            actionLabel = strings.uploadReel,
                            onAction = { showUploadDialog = true }
                        )
                    }
                } else {
                    val pagerState = rememberPagerState(pageCount = { videos.size })
                    VerticalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val reel = videos[page]
                        val isLiked = reel.id in likedVideoIds
                        val isFollowing = reel.authorId in followingIds
                        var showHeartBurst by remember(reel.id) { mutableStateOf(false) }
                        var isPlaying by remember(reel.id, autoPlayVideos) { mutableStateOf(autoPlayVideos) }

                        LaunchedEffect(showHeartBurst) {
                            if (showHeartBurst) {
                                delay(650)
                                showHeartBurst = false
                            }
                        }

                        val bgGradient = when (reel.accentTheme) {
                            "neon_cyan" -> listOf(Color(0xFF0A192F), Color(0xFF0E7490), Color(0xFF2563EB))
                            "sunset_fire" -> listOf(Color(0xFF2D0A1B), Color(0xFF9F1239), Color(0xFF7C3AED))
                            "emerald_pulse" -> listOf(Color(0xFF052E16), Color(0xFF047857), Color(0xFF2563EB))
                            else -> listOf(Color(0xFF140B2D), Color(0xFF5B21B6), Color(0xFF1D4ED8))
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(bgGradient))
                                .pointerInput(reel.id, isLiked) {
                                    detectTapGestures(
                                        onTap = { isPlaying = !isPlaying },
                                        onDoubleTap = {
                                            showHeartBurst = true
                                            if (!isLiked) onToggleLikeVideo(reel.id)
                                        }
                                    )
                                }
                                .padding(16.dp)
                                .statusBarsPadding()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Text(
                                        text = "${reel.category} • ${reel.durationLabel} • ${if (isPlaying) "Playing" else "Paused"}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                                Text(
                                    text = strings.doubleTapHint,
                                    color = Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(84.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = reel.title,
                                        tint = Color.White,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                                Text(
                                    text = reel.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            AnimatedVisibility(
                                visible = showHeartBurst,
                                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                                exit = scaleOut() + fadeOut(),
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = null,
                                    tint = WeRose,
                                    modifier = Modifier.size(110.dp)
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(bottom = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                WeAppAvatar(name = reel.authorName, size = 46.dp, highlightBorder = true)

                                VideoActionButton(
                                    icon = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    tint = if (isLiked) WeRose else Color.White,
                                    count = reel.likesCount.toString(),
                                    label = strings.like,
                                    testTag = "video_like_${reel.id}",
                                    onClick = { onToggleLikeVideo(reel.id) }
                                )

                                VideoActionButton(
                                    icon = Icons.Filled.ChatBubble,
                                    tint = Color.White,
                                    count = reel.commentsCount.toString(),
                                    label = strings.comment,
                                    testTag = "video_comment_${reel.id}",
                                    onClick = { onOpenComments(reel.id) }
                                )

                                VideoActionButton(
                                    icon = Icons.Filled.Share,
                                    tint = Color.White,
                                    count = reel.sharesCount.toString(),
                                    label = strings.share,
                                    testTag = "video_share_${reel.id}",
                                    onClick = {
                                        onShareVideo(reel.id)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "Watch '${reel.title}' by ${reel.authorName} on WeApp!")
                                        }
                                        context.startActivity(Intent.createChooser(intent, strings.share))
                                    }
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth(0.75f)
                                    .padding(bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "@${reel.authorName}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (reel.authorId != currentUser.userId) {
                                        OutlinedButton(
                                            onClick = { onToggleFollow(reel.authorId, reel.authorName) },
                                            shape = RoundedCornerShape(999.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f))
                                        ) {
                                            Text(
                                                text = if (isFollowing) strings.following else strings.follow,
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = reel.caption.ifBlank { reel.title },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = "${reel.authorCountryFlag} ${reel.authorCity} • ${GlobalFormatters.formatRelativeTime(reel.createdAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WeCyan
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showUploadDialog = true },
            containerColor = WePurple,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 48.dp, end = 16.dp)
                .testTag("upload_video_fab")
        ) {
            Icon(Icons.Filled.VideoCall, contentDescription = strings.uploadReel)
        }
    }

    if (showUploadDialog) {
        UploadVideoDialog(
            strings = strings,
            onDismiss = { showUploadDialog = false },
            onUpload = { title, caption, category, duration, theme ->
                onCreateVideo(title, caption, category, duration, theme)
                showUploadDialog = false
            }
        )
    }

    if (activeCommentVideoId != null) {
        CommentsBottomSheet(
            title = strings.commentsTitle,
            comments = videoComments,
            placeholder = strings.writeComment,
            sendLabel = strings.send,
            onDismiss = { onOpenComments(null) },
            onSend = { text -> onAddComment(activeCommentVideoId, text) }
        )
    }
}

@Composable
private fun VideoActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    count: String,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .testTag(testTag)
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(2.dp))
        Text(text = count, color = Color.White, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun UploadVideoDialog(
    strings: AppStrings,
    onDismiss: () -> Unit,
    onUpload: (title: String, caption: String, category: String, duration: String, theme: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Trending") }
    var theme by remember { mutableStateOf("purple_blue") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.uploadReel) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(120) },
                    label = { Text("Reel Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it.take(400) },
                    label = { Text("Caption") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Trending", "Comedy", "Gaming", "Music").forEach { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("purple_blue", "neon_cyan", "sunset_fire").forEach { t ->
                        FilterChip(selected = theme == t, onClick = { theme = t }, label = { Text(t.substringBefore("_")) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onUpload(title, caption, category, "0:30", theme) },
                enabled = title.isNotBlank()
            ) {
                Text(strings.postBtn)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
