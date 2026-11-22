package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.*
import com.example.localization.AppStrings
import com.example.ui.UiState
import com.example.ui.components.WeAppAvatar
import com.example.ui.components.WeAppEmptyState
import com.example.ui.components.WeAppGradientButton
import com.example.ui.theme.*

@Composable
fun GameArenaScreen(
    currentUser: PrivateUser,
    selectedGame: String,
    publicUsersState: UiState<List<PublicUser>>,
    challengesState: UiState<List<GameChallenge>>,
    strings: AppStrings,
    onSelectGame: (String) -> Unit,
    onCreateChallenge: (gameName: String, mode: String, note: String, stakeCoins: Long) -> Unit,
    onAcceptChallenge: (String) -> Unit,
    onCompleteChallenge: (String) -> Unit
) {
    var dialogMode by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Arena Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_arena_banner),
                    contentDescription = strings.gameArenaTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(0.25f), Color.Black.copy(0.82f))
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = strings.gameArenaTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Rank Points: ${currentUser.rankPoints} • WeCoins: ${currentUser.coins}",
                            style = MaterialTheme.typography.labelLarge,
                            color = WeGold
                        )
                    }
                }
            }
        }

        // Game Chips: Free Fire, PUBG, EA FC, COD, eFootball
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(SupportedGames.all) { game ->
                    val selected = selectedGame == game
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectGame(game) },
                        label = { Text(game, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.SportsEsports,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("game_chip_${game.replace(" ", "_")}")
                    )
                }
            }
        }

        // Find Team & Challenge Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { dialogMode = "Find Team" },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("find_team_button"),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, WeBlueLight)
                ) {
                    Icon(Icons.Filled.Groups, contentDescription = null, tint = WeBlueLight)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.findTeam, color = WeBlueLight, fontWeight = FontWeight.Bold)
                }

                WeAppGradientButton(
                    text = strings.challengeBtn,
                    icon = Icons.Filled.Bolt,
                    modifier = Modifier.weight(1f),
                    testTag = "challenge_button",
                    onClick = { dialogMode = "Challenge" }
                )
            }
        }

        // Leaderboard Rank 1, 2, 3
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = WeGold)
                        Text(
                            text = strings.leaderboardTitle,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    val leaders = ((publicUsersState as? UiState.Success)?.data ?: emptyList())
                        .sortedByDescending { it.rankPoints }
                        .take(5)

                    if (leaders.isEmpty()) {
                        Text(
                            text = "No ranked players yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        leaders.forEachIndexed { idx, player ->
                            val rank = idx + 1
                            val rankColor = when (rank) {
                                1 -> WeGold
                                2 -> Color(0xFFC0C0C0)
                                3 -> Color(0xFFCD7F32)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(rankColor.copy(alpha = 0.22f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "#$rank",
                                            color = rankColor,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                    WeAppAvatar(name = player.displayName.ifBlank { player.username }, size = 36.dp)
                                    Column {
                                        Text(
                                            text = player.displayName.ifBlank { player.username },
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = player.city,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = "${player.rankPoints} pts",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = WePurpleLight,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Open Challenges & Team Requests for Selected Game
        item {
            Text(
                text = "${strings.openChallenges} • $selectedGame",
                style = MaterialTheme.typography.titleLarge
            )
        }

        when (challengesState) {
            is UiState.Loading -> {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = WePurple)
                    }
                }
            }
            is UiState.Error -> {
                item {
                    WeAppEmptyState(
                        icon = Icons.Outlined.SportsEsports,
                        title = "Arena Unavailable",
                        subtitle = challengesState.message
                    )
                }
            }
            is UiState.Success -> {
                val filtered = challengesState.data.filter { it.gameName == selectedGame }
                if (filtered.isEmpty()) {
                    item {
                        WeAppEmptyState(
                            icon = Icons.Outlined.SportsEsports,
                            title = strings.noChallengesYet,
                            subtitle = "$selectedGame • ${strings.findTeam} / ${strings.challengeBtn}"
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { ch ->
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        AssistChip(onClick = {}, label = { Text(ch.mode) })
                                        AssistChip(onClick = {}, label = { Text("${ch.stakeCoins} Coins") })
                                    }
                                    Text(
                                        text = ch.status.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when (ch.status) {
                                            "completed" -> WeEmerald
                                            "accepted" -> WeGold
                                            else -> WeCyan
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "${ch.creatorName} (${ch.city})",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (ch.note.isNotBlank()) {
                                    Text(
                                        text = ch.note,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (ch.opponentName != null) {
                                    Text(
                                        text = "vs ${ch.opponentName}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = WePurpleLight
                                    )
                                }
                                if (ch.status == "open" && ch.creatorId != currentUser.userId) {
                                    WeAppGradientButton(
                                        text = strings.acceptChallenge,
                                        onClick = { onAcceptChallenge(ch.id) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else if (ch.status == "accepted" &&
                                    (ch.creatorId == currentUser.userId || ch.opponentId == currentUser.userId)
                                ) {
                                    Button(
                                        onClick = { onCompleteChallenge(ch.id) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = WeEmerald)
                                    ) {
                                        Text(strings.claimWin, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (dialogMode != null) {
        CreateChallengeDialog(
            mode = dialogMode!!,
            defaultGame = selectedGame,
            onDismiss = { dialogMode = null },
            onSubmit = { game, note, stake ->
                onCreateChallenge(game, dialogMode!!, note, stake)
                dialogMode = null
            }
        )
    }
}

@Composable
private fun CreateChallengeDialog(
    mode: String,
    defaultGame: String,
    onDismiss: () -> Unit,
    onSubmit: (game: String, note: String, stakeCoins: Long) -> Unit
) {
    var game by remember { mutableStateOf(defaultGame) }
    var note by remember { mutableStateOf("") }
    var stakeText by remember { mutableStateOf("50") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$mode • $game") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(SupportedGames.all) { g ->
                        FilterChip(selected = game == g, onClick = { game = g }, label = { Text(g) })
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(200) },
                    label = { Text("Match / Squad Note") },
                    placeholder = { Text("1v1 room or 4-player squad") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = stakeText,
                    onValueChange = { stakeText = it.filter { ch -> ch.isDigit() }.take(5) },
                    label = { Text("Stake WeCoins") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val stake = stakeText.toLongOrNull() ?: 0L
                    onSubmit(game, note, stake)
                }
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
