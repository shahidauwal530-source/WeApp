package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.GlobalFormatters
import com.example.data.UserNotification
import com.example.localization.AppStrings
import com.example.ui.UiState
import com.example.ui.components.WeAppAvatar
import com.example.ui.components.WeAppEmptyState
import com.example.ui.components.WeSecondaryTopBar
import com.example.ui.theme.*

@Composable
fun NotificationsScreen(
    notificationsState: UiState<List<UserNotification>>,
    strings: AppStrings,
    onMarkAllRead: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        WeSecondaryTopBar(
            title = strings.notificationsTitle,
            onBack = onBack,
            actions = {
                TextButton(
                    onClick = onMarkAllRead,
                    modifier = Modifier.testTag("notifications_mark_read_button")
                ) {
                    Text("Mark all read", color = WePurpleLight)
                }
            }
        )

        when (notificationsState) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WePurple)
            }
            is UiState.Error -> Box(Modifier.fillMaxSize().padding(16.dp)) {
                WeAppEmptyState(Icons.Outlined.NotificationsNone, "Unavailable", notificationsState.message)
            }
            is UiState.Success -> {
                val items = notificationsState.data
                if (items.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(16.dp)) {
                        WeAppEmptyState(
                            icon = Icons.Outlined.NotificationsNone,
                            title = "No notifications yet",
                            subtitle = "Likes, follows, coin rewards, and security alerts appear here."
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(items, key = { it.id }) { item ->
                            val unread = !item.readFlag
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (unread) WePurple.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (unread) WePurpleLight.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    WeAppAvatar(name = item.actorName, size = 42.dp)
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(item.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            GlobalFormatters.formatRelativeTime(item.createdAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = WePurpleLight
                                        )
                                    }
                                    if (unread) {
                                        Icon(Icons.Filled.Circle, contentDescription = "Unread", tint = WePurpleLight, modifier = Modifier.size(10.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
