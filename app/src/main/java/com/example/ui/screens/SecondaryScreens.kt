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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.localization.AppStrings
import com.example.ui.UiState
import com.example.ui.components.WeAppEmptyState
import com.example.ui.components.WeAppGradientButton
import com.example.ui.components.WeSecondaryTopBar
import com.example.ui.theme.*

@Composable
fun WalletScreen(
    currentUser: PrivateUser,
    transactionsState: UiState<List<WalletTransaction>>,
    strings: AppStrings,
    onClaimDaily: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val userCountry = remember(currentUser.countryCode) { GlobalCountries.byCode(currentUser.countryCode) }
    val balanceLocal = remember(currentUser.coins, userCountry) {
        GlobalFormatters.formatCoinsValue(currentUser.coins, userCountry)
    }
    val rate100Coins = remember(userCountry) {
        GlobalFormatters.formatCoinsValue(100L, userCountry)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        WeSecondaryTopBar(strings.walletTitle, onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WeBrandGradient, RoundedCornerShape(24.dp))
                        .padding(22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "${strings.currentBalance} • ${userCountry.flag} ${userCountry.currencyCode}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(0.85f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = WeGold, modifier = Modifier.size(34.dp))
                            Column {
                                Text(
                                    text = "${currentUser.coins} WeCoins",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "≈ $balanceLocal  (100 Coins = $rate100Coins)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = WeGold
                                )
                            }
                        }
                        Button(
                            onClick = onClaimDaily,
                            modifier = Modifier.testTag("claim_daily_coins_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = WeGold, contentColor = Color.Black),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(strings.claimDailyBonus, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Text(strings.transactionHistory, style = MaterialTheme.typography.titleLarge)
            }

            when (transactionsState) {
                is UiState.Loading -> item { CircularProgressIndicator(color = WePurple) }
                is UiState.Error -> item { Text(transactionsState.message, color = WeRose) }
                is UiState.Success -> {
                    val txs = transactionsState.data
                    if (txs.isEmpty()) {
                        item {
                            WeAppEmptyState(
                                icon = Icons.Filled.ReceiptLong,
                                title = strings.noTransactionsYet,
                                subtitle = strings.claimDailyBonus
                            )
                        }
                    } else {
                        items(txs, key = { it.id }) { tx ->
                            val dateStr = GlobalFormatters.formatRelativeTime(tx.createdAt)
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(tx.title, style = MaterialTheme.typography.titleMedium)
                                        Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        text = if (tx.txType == "earn") "+${tx.amount}" else "-${tx.amount}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (tx.txType == "earn") WeEmerald else WeRose,
                                        fontWeight = FontWeight.Bold
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

@Composable
fun MarketplaceScreen(
    currentUser: PrivateUser,
    listingsState: UiState<List<MarketplaceListing>>,
    strings: AppStrings,
    onCreateListing: (title: String, desc: String, category: String, priceNgn: Long) -> Unit,
    onMarkSold: (String) -> Unit,
    onMessageSeller: (sellerId: String, sellerName: String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var showSellDialog by remember { mutableStateOf(false) }
    val userCountry = remember(currentUser.countryCode) { GlobalCountries.byCode(currentUser.countryCode) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        WeSecondaryTopBar("${strings.marketplaceTitle} (${userCountry.currencyCode})", onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                WeAppGradientButton(
                    text = strings.sellItem,
                    icon = Icons.Filled.AddBusiness,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "marketplace_sell_button",
                    onClick = { showSellDialog = true }
                )
            }

            when (listingsState) {
                is UiState.Loading -> item { CircularProgressIndicator(color = WePurple) }
                is UiState.Error -> item { Text(listingsState.message, color = WeRose) }
                is UiState.Success -> {
                    val items = listingsState.data
                    if (items.isEmpty()) {
                        item {
                            WeAppEmptyState(
                                icon = Icons.Filled.Storefront,
                                title = strings.noListingsYet,
                                subtitle = "Global Community Marketplace • ${userCountry.currencyCode}"
                            )
                        }
                    } else {
                        items(items, key = { it.id }) { item ->
                            val localPrice = GlobalFormatters.formatNgnInLocalCurrency(item.priceNgn, userCountry)
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
                                        AssistChip(onClick = {}, label = { Text(item.category) })
                                        Text(
                                            text = localPrice,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = WeGold,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(item.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${item.sellerName} • ${item.sellerCountryFlag} ${item.sellerCity} • ${GlobalFormatters.formatRelativeTime(item.createdAt)} • ${item.status.uppercase()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (item.status == "sold") WeRose else WeEmerald
                                    )
                                    if (item.sellerId == currentUser.userId && item.status == "available") {
                                        OutlinedButton(onClick = { onMarkSold(item.id) }) {
                                            Text(strings.markSold)
                                        }
                                    } else if (item.sellerId != currentUser.userId && item.status == "available") {
                                        WeAppGradientButton(
                                            text = strings.messageSeller,
                                            onClick = { onMessageSeller(item.sellerId, item.sellerName) },
                                            modifier = Modifier.fillMaxWidth()
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

    if (showSellDialog) {
        var title by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Electronics") }
        var localPriceInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showSellDialog = false },
            title = { Text(strings.sellItem) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it.take(100) }, label = { Text("Item Title") }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it.take(500) }, label = { Text("Description") }, maxLines = 3)
                    OutlinedTextField(
                        value = localPriceInput,
                        onValueChange = { localPriceInput = it.filter { c -> c.isDigit() }.take(8) },
                        label = { Text("Price (${userCountry.currencySymbol} ${userCountry.currencyCode})") },
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Electronics", "Gaming", "Fashion").forEach { cat ->
                            FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val entered = localPriceInput.toLongOrNull() ?: 0L
                        val normalizedNgn = if (userCountry.code == "NG" || userCountry.ngnConversionRate <= 0.0) entered
                        else (entered / userCountry.ngnConversionRate).toLong().coerceIn(0L, 100_000_000L)
                        if (title.isNotBlank() && desc.isNotBlank()) {
                            onCreateListing(title, desc, category, normalizedNgn)
                            showSellDialog = false
                        }
                    },
                    enabled = title.isNotBlank() && desc.isNotBlank()
                ) { Text(strings.postBtn) }
            },
            dismissButton = { TextButton(onClick = { showSellDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun LiveScreen(
    currentUser: PrivateUser,
    roomsState: UiState<List<LiveRoom>>,
    activeRoomId: String?,
    liveMessages: List<LiveMessage>,
    strings: AppStrings,
    onCreateAndJoinRoom: (title: String, topic: String) -> Unit,
    onJoinRoom: (String?) -> Unit,
    onSendLiveMessage: (roomId: String, text: String) -> Unit,
    onEndRoom: (String) -> Unit,
    onBack: () -> Unit
) {
    val rooms = (roomsState as? UiState.Success)?.data ?: emptyList()
    val activeRoom = rooms.firstOrNull { it.id == activeRoomId }

    if (activeRoomId != null && activeRoom != null) {
        BackHandler { onJoinRoom(null) }
        var draft by remember { mutableStateOf("") }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
        ) {
            WeSecondaryTopBar("${activeRoom.title} • 🔴 ${activeRoom.viewerCount}", onBack = { onJoinRoom(null) })

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Host: ${activeRoom.hostName}", style = MaterialTheme.typography.titleMedium)
                            Text("Topic: ${activeRoom.topic}", style = MaterialTheme.typography.labelSmall, color = WePurpleLight)
                        }
                        if (activeRoom.hostId == currentUser.userId && activeRoom.status == "live") {
                            Button(
                                onClick = { onEndRoom(activeRoom.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = WeRose)
                            ) {
                                Text(strings.endLive, color = Color.White)
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(liveMessages, key = { it.id }) { msg ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${msg.senderName}: ${msg.text}",
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it.take(300) },
                        placeholder = { Text(strings.typeMessage) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    IconButton(
                        onClick = {
                            if (draft.isNotBlank()) {
                                onSendLiveMessage(activeRoom.id, draft)
                                draft = ""
                            }
                        },
                        enabled = draft.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = strings.send, tint = WePurpleLight)
                    }
                }
            }
        }
        return
    }

    BackHandler(onBack = onBack)
    var showGoLiveDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        WeSecondaryTopBar(strings.liveTitle, onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                WeAppGradientButton(
                    text = strings.goLive,
                    icon = Icons.Filled.Podcasts,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "go_live_button",
                    onClick = { showGoLiveDialog = true }
                )
            }

            when (roomsState) {
                is UiState.Loading -> item { CircularProgressIndicator(color = WePurple) }
                is UiState.Error -> item { Text(roomsState.message, color = WeRose) }
                is UiState.Success -> {
                    val activeRooms = rooms.filter { it.status == "live" }
                    if (activeRooms.isEmpty()) {
                        item {
                            WeAppEmptyState(
                                icon = Icons.Filled.LiveTv,
                                title = strings.noLiveRoomsYet,
                                subtitle = strings.goLive
                            )
                        }
                    } else {
                        items(activeRooms, key = { it.id }) { room ->
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("🔴 LIVE • ${room.viewerCount} watching", style = MaterialTheme.typography.labelSmall, color = WeRose)
                                        Text(room.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("${room.hostName} • ${room.topic}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    WeAppGradientButton(
                                        text = strings.joinRoom,
                                        onClick = { onJoinRoom(room.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGoLiveDialog) {
        var title by remember { mutableStateOf("") }
        var topic by remember { mutableStateOf("Hangout") }
        AlertDialog(
            onDismissRequest = { showGoLiveDialog = false },
            title = { Text(strings.goLive) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it.take(100) }, label = { Text("Room Title") }, singleLine = true)
                    OutlinedTextField(value = topic, onValueChange = { topic = it.take(50) }, label = { Text("Topic") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onCreateAndJoinRoom(title, topic)
                            showGoLiveDialog = false
                        }
                    },
                    enabled = title.isNotBlank()
                ) { Text(strings.goLive) }
            },
            dismissButton = { TextButton(onClick = { showGoLiveDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun SubscriptionScreen(
    currentPlanCode: String,
    countryCode: String,
    strings: AppStrings,
    onSelectPlan: (SubscriptionPlan) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val userCountry = remember(countryCode) { GlobalCountries.byCode(countryCode) }
    val perks = mapOf(
        SubscriptionPlan.FREE to listOf("Social feed, Reels & Chat", "Gaming Arena access", "Earn WeCoins daily"),
        SubscriptionPlan.PRO to listOf("PRO Creator Badge", "+300 Bonus WeCoins", "Ad-Free experience & Priority Marketplace"),
        SubscriptionPlan.VIP to listOf("VIP Gold Crown Badge", "+1,000 Bonus WeCoins", "Ad-Free + Zero-fee Marketplace & Tournaments")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        WeSecondaryTopBar("${strings.subscriptionTitle} (${userCountry.currencyCode})", onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(SubscriptionPlan.entries) { plan ->
                val isActive = currentPlanCode == plan.code
                val accent = when (plan) {
                    SubscriptionPlan.VIP -> WeGold
                    SubscriptionPlan.PRO -> WePurpleLight
                    SubscriptionPlan.FREE -> WeBlueLight
                }
                val localPrice = GlobalFormatters.formatNgnInLocalCurrency(plan.priceNgn, userCountry)
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(if (isActive) 2.dp else 1.dp, if (isActive) accent else MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(plan.code, style = MaterialTheme.typography.headlineMedium, color = accent, fontWeight = FontWeight.Bold)
                            Text(
                                text = localPrice,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        perks[plan]?.forEach { perk ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                                Text(perk, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        if (isActive) {
                            OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                                Text(strings.activeLabel)
                            }
                        } else {
                            WeAppGradientButton(
                                text = strings.upgradeBtn,
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "select_plan_${plan.code}",
                                onClick = { onSelectPlan(plan) }
                            )
                        }
                    }
                }
            }
        }
    }
}
