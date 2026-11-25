package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.localization.AppStrings
import com.example.ui.UiState
import com.example.ui.components.PlanBadge
import com.example.ui.components.WeAppAvatar
import com.example.ui.components.WeAppEmptyState
import com.example.ui.components.WeSecondaryTopBar
import com.example.ui.theme.*

@Composable
fun SearchScreen(
    currentUser: PrivateUser,
    publicUsersState: UiState<List<PublicUser>>,
    followingIds: Set<String>,
    safetyList: List<SafetyListEntry>,
    strings: AppStrings,
    onToggleFollow: (targetUserId: String, targetName: String) -> Unit,
    onStartChat: (peerId: String, peerName: String) -> Unit,
    onSetSafety: (targetUserId: String, targetName: String, listType: String) -> Unit,
    onReportUser: (targetUserId: String, reason: String, details: String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var query by remember { mutableStateOf("") }
    var selectedCountryCode by remember { mutableStateOf("ALL") }
    var cityFilter by remember { mutableStateOf("") }
    var inspectedUser by remember { mutableStateOf<PublicUser?>(null) }
    var reportingUser by remember { mutableStateOf<PublicUser?>(null) }

    val blockedIds = remember(safetyList) {
        safetyList.filter { it.listType == "blocked" }.map { it.targetUserId }.toSet()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        WeSecondaryTopBar(strings.searchTitle, onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(60) },
                placeholder = { Text("Search people worldwide by name or @username...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("global_search_input")
            )

            OutlinedTextField(
                value = cityFilter,
                onValueChange = { cityFilter = it.take(40) },
                placeholder = { Text("Filter by city (optional, e.g. Lagos, London, Mumbai)") },
                leadingIcon = { Icon(Icons.Filled.LocationCity, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCountryCode == "ALL",
                        onClick = { selectedCountryCode = "ALL" },
                        label = { Text("🌐 All Countries") }
                    )
                }
                items(GlobalCountries.all, key = { it.code }) { c ->
                    FilterChip(
                        selected = selectedCountryCode == c.code,
                        onClick = { selectedCountryCode = c.code },
                        label = { Text("${c.flag} ${c.name}") }
                    )
                }
            }
        }

        when (publicUsersState) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WePurple)
            }
            is UiState.Error -> Box(Modifier.fillMaxSize().padding(16.dp)) {
                WeAppEmptyState(Icons.Outlined.SearchOff, "Search Unavailable", publicUsersState.message)
            }
            is UiState.Success -> {
                val results = remember(publicUsersState.data, query, selectedCountryCode, cityFilter, blockedIds, currentUser.userId) {
                    publicUsersState.data.filter { u ->
                        val isSelf = u.userId == currentUser.userId
                        val notBlocked = u.userId !in blockedIds
                        val searchable = isSelf || u.searchableByUsername
                        val matchesQuery = query.isBlank() ||
                            u.displayName.contains(query, ignoreCase = true) ||
                            u.username.contains(query, ignoreCase = true)
                        val matchesCountry = selectedCountryCode == "ALL" ||
                            u.countryCode.equals(selectedCountryCode, ignoreCase = true)
                        val matchesCity = cityFilter.isBlank() ||
                            u.city.contains(cityFilter.trim(), ignoreCase = true)
                        notBlocked && searchable && matchesQuery && matchesCountry && matchesCity
                    }
                }

                if (results.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(16.dp)) {
                        WeAppEmptyState(
                            icon = Icons.Outlined.PersonSearch,
                            title = "No matching people found",
                            subtitle = "Try another country, city, or username."
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(results, key = { it.userId }) { person ->
                            val isSelf = person.userId == currentUser.userId
                            val isFollowing = person.userId in followingIds
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { inspectedUser = person },
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        WeAppAvatar(
                                            name = person.displayName.ifBlank { person.username },
                                            size = 46.dp,
                                            photoUrl = person.photoUrl
                                        )
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(person.displayName.ifBlank { person.username }, style = MaterialTheme.typography.titleMedium)
                                                PlanBadge(person.plan)
                                            }
                                            Text(
                                                "@${person.username} • ${person.countryFlag} ${person.city}, ${person.countryName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (!isSelf) {
                                        OutlinedButton(
                                            onClick = { onToggleFollow(person.userId, person.displayName.ifBlank { person.username }) },
                                            shape = RoundedCornerShape(999.dp)
                                        ) {
                                            Text(if (isFollowing) strings.following else strings.follow)
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

    inspectedUser?.let { u ->
        val isSelf = u.userId == currentUser.userId
        val isFollowing = u.userId in followingIds
        val canFollow = when (u.privacyFollow) {
            "Friends of Friends" -> isFollowing || u.followersCount > 0
            else -> true
        }
        val canMessage = when (u.privacyMessage) {
            "No One" -> false
            "Friends" -> isFollowing
            else -> true
        }

        AlertDialog(
            onDismissRequest = { inspectedUser = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WeAppAvatar(name = u.displayName.ifBlank { u.username }, size = 44.dp, photoUrl = u.photoUrl)
                    Column {
                        Text(u.displayName.ifBlank { u.username }, style = MaterialTheme.typography.titleMedium)
                        Text("@${u.username} • ${u.countryFlag} ${u.city}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (u.bio.isNotBlank()) Text(u.bio, style = MaterialTheme.typography.bodyMedium)
                    if (u.work.isNotBlank()) Text("💼 ${u.work}", style = MaterialTheme.typography.labelMedium)
                    if (u.education.isNotBlank()) Text("🎓 ${u.education}", style = MaterialTheme.typography.labelMedium)
                    Text("${u.followersCount} ${strings.followersLabel} • ${u.rankPoints} Rank Pts", style = MaterialTheme.typography.labelMedium, color = WePurpleLight)
                    if (!isSelf) {
                        HorizontalDivider()
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (canFollow) {
                                Button(
                                    onClick = { onToggleFollow(u.userId, u.displayName.ifBlank { u.username }) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isFollowing) strings.following else strings.follow)
                                }
                            }
                            if (canMessage) {
                                OutlinedButton(
                                    onClick = {
                                        inspectedUser = null
                                        onStartChat(u.userId, u.displayName.ifBlank { u.username })
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Message")
                                }
                            } else {
                                Text("Messages restricted by user privacy", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = {
                                onSetSafety(u.userId, u.displayName.ifBlank { u.username }, "muted")
                                inspectedUser = null
                            }) { Text("Mute") }
                            TextButton(onClick = {
                                onSetSafety(u.userId, u.displayName.ifBlank { u.username }, "restricted")
                                inspectedUser = null
                            }) { Text("Restrict") }
                            TextButton(onClick = {
                                onSetSafety(u.userId, u.displayName.ifBlank { u.username }, "blocked")
                                inspectedUser = null
                            }) { Text("Block", color = WeRose) }
                            TextButton(onClick = {
                                reportingUser = u
                                inspectedUser = null
                            }) { Text("Report", color = WeRose) }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { inspectedUser = null }) { Text("Close") }
            }
        )
    }

    reportingUser?.let { targetUser ->
        ReportContentDialog(
            title = "Report ${targetUser.displayName}",
            onDismiss = { reportingUser = null },
            onSubmit = { reason, details ->
                onReportUser(targetUser.userId, reason, details)
                reportingUser = null
            }
        )
    }
}
