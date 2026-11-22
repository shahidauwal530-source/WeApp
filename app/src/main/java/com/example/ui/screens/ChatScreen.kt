package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ChatMessage
import com.example.data.ChatThread
import com.example.data.PrivateUser
import com.example.data.PublicUser
import com.example.localization.AppStrings
import com.example.ui.UiState
import com.example.ui.components.WeAppAvatar
import com.example.ui.components.WeAppEmptyState
import com.example.ui.components.WeAppGradientButton
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    currentUser: PrivateUser,
    chatsState: UiState<List<ChatThread>>,
    publicUsersState: UiState<List<PublicUser>>,
    activeChatId: String?,
    activeMessages: List<ChatMessage>,
    strings: AppStrings,
    onOpenChat: (String?) -> Unit,
    onStartChatWith: (peerId: String, peerName: String) -> Unit,
    onSendMessage: (chatId: String, participantIds: List<String>, text: String) -> Unit,
    onSetTyping: (chatId: String, typing: Boolean) -> Unit
) {
    val chats = (chatsState as? UiState.Success)?.data ?: emptyList()
    val activeThread = chats.firstOrNull { it.id == activeChatId }

    if (activeChatId != null && activeThread != null) {
        BackHandler {
            onSetTyping(activeChatId, false)
            onOpenChat(null)
        }
        ChatDetailView(
            currentUserId = currentUser.userId,
            thread = activeThread,
            messages = activeMessages,
            strings = strings,
            onBack = {
                onSetTyping(activeChatId, false)
                onOpenChat(null)
            },
            onTypingChanged = { typing -> onSetTyping(activeChatId, typing) },
            onSend = { text -> onSendMessage(activeThread.id, activeThread.participantIds, text) }
        )
        return
    }

    var showNewChatDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(strings.chatsTitle, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            WeAppGradientButton(
                text = strings.newChat,
                icon = Icons.Filled.AddComment,
                testTag = "new_chat_button",
                onClick = { showNewChatDialog = true }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (chatsState) {
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
                            icon = Icons.Outlined.ChatBubbleOutline,
                            title = "Chats Unavailable",
                            subtitle = chatsState.message
                        )
                    }
                }
                is UiState.Success -> {
                    if (chats.isEmpty()) {
                        item {
                            WeAppEmptyState(
                                icon = Icons.Outlined.ChatBubbleOutline,
                                title = strings.noChatsYet,
                                subtitle = strings.newChat,
                                actionLabel = strings.newChat,
                                onAction = { showNewChatDialog = true }
                            )
                        }
                    } else {
                        items(chats, key = { it.id }) { thread ->
                            val peerDisplay = if (thread.creatorId == currentUser.userId) thread.peerName else thread.creatorName
                            val isOtherTyping = thread.typingUserIds.any { it != currentUser.userId }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenChat(thread.id) },
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    WeAppAvatar(name = peerDisplay, size = 48.dp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = peerDisplay,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isOtherTyping) "$peerDisplay ${strings.typingIndicator}" else thread.lastMessage.ifBlank { "Tap to chat" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isOtherTyping) WeEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewChatDialog) {
        val creators = ((publicUsersState as? UiState.Success)?.data ?: emptyList())
            .filter { it.userId != currentUser.userId }

        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = { Text(strings.newChat) },
            text = {
                if (creators.isEmpty()) {
                    Text("No other registered WeApp users found yet.")
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(creators, key = { it.userId }) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        showNewChatDialog = false
                                        onStartChatWith(user.userId, user.displayName.ifBlank { user.username })
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                WeAppAvatar(name = user.displayName.ifBlank { user.username }, size = 38.dp)
                                Column {
                                    Text(user.displayName.ifBlank { user.username }, style = MaterialTheme.typography.titleMedium)
                                    Text("@${user.username} • ${user.city}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNewChatDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun ChatDetailView(
    currentUserId: String,
    thread: ChatThread,
    messages: List<ChatMessage>,
    strings: AppStrings,
    onBack: () -> Unit,
    onTypingChanged: (Boolean) -> Unit,
    onSend: (String) -> Unit
) {
    val peerName = if (thread.creatorId == currentUserId) thread.peerName else thread.creatorName
    val isPeerTyping = thread.typingUserIds.any { it != currentUserId }
    var draft by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                WeAppAvatar(name = peerName, size = 38.dp)
                Column {
                    Text(peerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (isPeerTyping) {
                        Text(
                            text = strings.typingIndicator,
                            style = MaterialTheme.typography.labelSmall,
                            color = WeEmerald
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isMine = msg.senderId == currentUserId
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 18.dp,
                                    bottomStart = if (isMine) 18.dp else 4.dp,
                                    bottomEnd = if (isMine) 4.dp else 18.dp
                                )
                            )
                            .background(
                                if (isMine) WeBrandGradient
                                else androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.surfaceVariant)
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = msg.text,
                            color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }

        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { newText ->
                        val wasBlank = draft.isBlank()
                        draft = newText.take(1000)
                        if (wasBlank && draft.isNotBlank()) onTypingChanged(true)
                        else if (!wasBlank && draft.isBlank()) onTypingChanged(false)
                    },
                    placeholder = { Text(strings.typeMessage) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_message_input"),
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 3
                )
                IconButton(
                    onClick = {
                        if (draft.isNotBlank()) {
                            onSend(draft)
                            draft = ""
                        }
                    },
                    enabled = draft.isNotBlank(),
                    modifier = Modifier.testTag("chat_send_button")
                ) {
                    Icon(Icons.Filled.Send, contentDescription = strings.send, tint = WePurpleLight)
                }
            }
        }
    }
}
