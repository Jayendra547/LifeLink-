package com.example.lifelink.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularConnectedNoInternet0Bar
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifelink.data.ConnectivityZone
import com.example.lifelink.data.DeliveryStatus
import com.example.lifelink.data.EmergencyIntent
import com.example.lifelink.data.LifeLinkMessage
import com.example.lifelink.data.MessageDirection
import com.example.ui.theme.EmergencyAmber
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.MeshBlue
import com.example.ui.theme.SafetyGreen
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class RoomSortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST,
    HIGHEST_PRIORITY
}

/**
 * Explicit offline storage delivery states for stored LifeLink messages.
 */
enum class OfflineMessageStatus(
    val id: String,
    val title: String,
    val sublabel: String
) {
    SENT(
        id = "sent",
        title = "SENT",
        sublabel = "MESH DELIVERED"
    ),
    PENDING(
        id = "pending",
        title = "PENDING",
        sublabel = "ROOM QUEUED"
    ),
    RECEIVED(
        id = "received",
        title = "RECEIVED",
        sublabel = "ROOM SAVED"
    );

    fun getBadgeText(): String = "$title • $sublabel"
}

fun LifeLinkMessage.getOfflineStatus(): OfflineMessageStatus {
    return when {
        direction == MessageDirection.INCOMING || deliveryStatus == DeliveryStatus.RECEIVED_OFFLINE -> OfflineMessageStatus.RECEIVED
        deliveryStatus == DeliveryStatus.DELIVERED_MESH -> OfflineMessageStatus.SENT
        else -> OfflineMessageStatus.PENDING
    }
}

/**
 * Prominent visual badge indicating 'sent', 'pending', or 'received' status
 * with dedicated Material icons and color coding.
 */
@Composable
fun OfflineStatusBadge(
    status: OfflineMessageStatus,
    messageId: String,
    modifier: Modifier = Modifier
) {
    val icon = when (status) {
        OfflineMessageStatus.SENT -> Icons.Default.DoneAll
        OfflineMessageStatus.PENDING -> Icons.Default.Schedule
        OfflineMessageStatus.RECEIVED -> Icons.Default.CallReceived
    }
    val tint = when (status) {
        OfflineMessageStatus.SENT -> SafetyGreen
        OfflineMessageStatus.PENDING -> EmergencyAmber
        OfflineMessageStatus.RECEIVED -> MeshBlue
    }
    val bgColor = when (status) {
        OfflineMessageStatus.SENT -> SafetyGreen.copy(alpha = 0.18f)
        OfflineMessageStatus.PENDING -> EmergencyAmber.copy(alpha = 0.18f)
        OfflineMessageStatus.RECEIVED -> MeshBlue.copy(alpha = 0.18f)
    }

    Surface(
        modifier = modifier.testTag("status_indicator_${status.id}_$messageId"),
        shape = RoundedCornerShape(4.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Status: ${status.name.lowercase()}",
                tint = tint,
                modifier = Modifier
                    .size(13.dp)
                    .testTag("status_icon_${status.id}")
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = status.title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp,
                    color = tint
                ),
                modifier = Modifier.testTag("status_title_${status.id}")
            )
            Text(
                text = " • ",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = tint.copy(alpha = 0.7f)
                )
            )
            Text(
                text = status.sublabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = tint
                ),
                modifier = Modifier.testTag("status_sublabel_${status.id}")
            )
        }
    }
}

/**
 * Interactive summary pill displaying count and visual status icon for offline messages.
 */
@Composable
fun StatusSummaryPill(
    icon: ImageVector,
    count: Int,
    label: String,
    tint: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) tint.copy(alpha = 0.24f) else tint.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) tint else tint.copy(alpha = 0.35f)
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "$label count: $count",
                tint = tint,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = tint
                )
            )
        }
    }
}

/**
 * A Composable list UI that observes a Room database Flow in real-time.
 * Features live reactive updates, query search, direction/priority filtering,
 * visual storage badges, playback, inspection, and record deletion.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoomStoredMessagesList(
    messagesFlow: Flow<List<LifeLinkMessage>>,
    modifier: Modifier = Modifier,
    isScrollable: Boolean = true,
    onPlayTts: (LifeLinkMessage) -> Unit = {},
    onInspect: (LifeLinkMessage) -> Unit = {},
    onDelete: (String) -> Unit = {},
    onClearAll: (() -> Unit)? = null
) {
    // Observe Room database reactive Flow in real-time
    val storedMessages by messagesFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(MessageFilter.ALL) }
    var sortOrder by remember { mutableStateOf(RoomSortOrder.NEWEST_FIRST) }
    var showClearConfirmation by remember { mutableStateOf(false) }

    val pendingCount = remember(storedMessages) {
        storedMessages.count { it.getOfflineStatus() == OfflineMessageStatus.PENDING }
    }
    val sentCount = remember(storedMessages) {
        storedMessages.count { it.getOfflineStatus() == OfflineMessageStatus.SENT }
    }
    val receivedCount = remember(storedMessages) {
        storedMessages.count { it.getOfflineStatus() == OfflineMessageStatus.RECEIVED }
    }

    // Filter & sort logic in memory on top of Room DB emission
    val filteredMessages = remember(storedMessages, searchQuery, selectedFilter, sortOrder) {
        var result = storedMessages.filter { msg ->
            when (selectedFilter) {
                MessageFilter.ALL -> true
                MessageFilter.PENDING_ONLY -> msg.getOfflineStatus() == OfflineMessageStatus.PENDING
                MessageFilter.SENT_ONLY -> msg.getOfflineStatus() == OfflineMessageStatus.SENT
                MessageFilter.RECEIVED_ONLY -> msg.getOfflineStatus() == OfflineMessageStatus.RECEIVED
                MessageFilter.OUTGOING_OFFLINE -> msg.direction == MessageDirection.OUTGOING
                MessageFilter.INCOMING_MESH -> msg.direction == MessageDirection.INCOMING
                MessageFilter.CRITICAL_P5 -> msg.priority == 5
                MessageFilter.HIGH_P4 -> msg.priority >= 4
                MessageFilter.SAFE -> msg.intent == EmergencyIntent.SAFE
            }
        }

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            result = result.filter { msg ->
                msg.text.lowercase(Locale.ROOT).contains(q) ||
                    (msg.translatedText?.lowercase(Locale.ROOT)?.contains(q) == true) ||
                    msg.senderId.lowercase(Locale.ROOT).contains(q) ||
                    msg.intent.name.lowercase(Locale.ROOT).contains(q) ||
                    msg.id.lowercase(Locale.ROOT).contains(q)
            }
        }

        when (sortOrder) {
            RoomSortOrder.NEWEST_FIRST -> result.sortedByDescending { it.timestamp }
            RoomSortOrder.OLDEST_FIRST -> result.sortedBy { it.timestamp }
            RoomSortOrder.HIGHEST_PRIORITY -> result.sortedWith(
                compareByDescending<LifeLinkMessage> { it.priority }.thenByDescending { it.timestamp }
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("room_stored_messages_list")
    ) {
        // Room Stream Header Card
        RoomStreamHeader(
            totalCount = storedMessages.size,
            filteredCount = filteredMessages.size,
            sentCount = sentCount,
            pendingCount = pendingCount,
            receivedCount = receivedCount,
            searchQuery = searchQuery,
            onSearchQueryChanged = { searchQuery = it },
            currentFilter = selectedFilter,
            onFilterChanged = { selectedFilter = it },
            sortOrder = sortOrder,
            onSortOrderChanged = { sortOrder = it },
            onClearAllRequest = if (onClearAll != null && storedMessages.isNotEmpty()) {
                { showClearConfirmation = true }
            } else null
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Messages List Content
        if (storedMessages.isEmpty()) {
            RoomEmptyState(
                title = "No Messages in Local Room DB",
                description = "Zero records found in SQLite storage. Outgoing voice SOS packets or incoming relay alerts will stream here instantly."
            )
        } else if (filteredMessages.isEmpty()) {
            RoomEmptyState(
                title = "No Matching Records",
                description = "No stored messages match your active filter or search query '$searchQuery'."
            )
        } else {
            if (isScrollable) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = filteredMessages,
                        key = { it.id }
                    ) { message ->
                        RoomMessageCard(
                            message = message,
                            onPlayTts = { onPlayTts(message) },
                            onInspect = { onInspect(message) },
                            onDelete = { onDelete(message.id) }
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    filteredMessages.forEach { message ->
                        RoomMessageCard(
                            message = message,
                            onPlayTts = { onPlayTts(message) },
                            onInspect = { onInspect(message) },
                            onDelete = { onDelete(message.id) }
                        )
                    }
                }
            }
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear All Room DB Messages?") },
            text = {
                Text("This will purge all ${storedMessages.size} stored messages from local SQLite database storage. This operation cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                        onClearAll?.invoke()
                    }
                ) {
                    Text("Clear All", color = EmergencyRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoomStreamHeader(
    totalCount: Int,
    filteredCount: Int,
    sentCount: Int = 0,
    pendingCount: Int = 0,
    receivedCount: Int = 0,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    currentFilter: MessageFilter,
    onFilterChanged: (MessageFilter) -> Unit,
    sortOrder: RoomSortOrder,
    onSortOrderChanged: (RoomSortOrder) -> Unit,
    onClearAllRequest: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Live Pulse Indicator & Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pulsing green dot indicating live Room flow observation
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseAlpha"
                    )

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(SafetyGreen.copy(alpha = pulseAlpha))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ROOM DATABASE STREAM",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = if (filteredCount == totalCount) "$totalCount stored" else "$filteredCount / $totalCount",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (onClearAllRequest != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onClearAllRequest,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("btn_clear_all_room_db")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear All Messages",
                                tint = EmergencyRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status Summary Row (Sent, Pending, Received) with visual icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusSummaryPill(
                    icon = Icons.Default.DoneAll,
                    count = sentCount,
                    label = "Sent",
                    tint = SafetyGreen,
                    isSelected = currentFilter == MessageFilter.SENT_ONLY,
                    onClick = {
                        onFilterChanged(if (currentFilter == MessageFilter.SENT_ONLY) MessageFilter.ALL else MessageFilter.SENT_ONLY)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("status_summary_sent")
                )
                StatusSummaryPill(
                    icon = Icons.Default.Schedule,
                    count = pendingCount,
                    label = "Pending",
                    tint = EmergencyAmber,
                    isSelected = currentFilter == MessageFilter.PENDING_ONLY,
                    onClick = {
                        onFilterChanged(if (currentFilter == MessageFilter.PENDING_ONLY) MessageFilter.ALL else MessageFilter.PENDING_ONLY)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("status_summary_pending")
                )
                StatusSummaryPill(
                    icon = Icons.Default.CallReceived,
                    count = receivedCount,
                    label = "Received",
                    tint = MeshBlue,
                    isSelected = currentFilter == MessageFilter.RECEIVED_ONLY,
                    onClick = {
                        onFilterChanged(if (currentFilter == MessageFilter.RECEIVED_ONLY) MessageFilter.ALL else MessageFilter.RECEIVED_ONLY)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("status_summary_received")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search input field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("room_db_search_input"),
                placeholder = {
                    Text(
                        "Search messages, intents, nodes...",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Search",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips FlowRow
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = currentFilter == MessageFilter.ALL,
                    onClick = { onFilterChanged(MessageFilter.ALL) },
                    label = { Text("All", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("room_filter_all")
                )
                FilterChip(
                    selected = currentFilter == MessageFilter.PENDING_ONLY,
                    onClick = { onFilterChanged(MessageFilter.PENDING_ONLY) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = EmergencyAmber,
                            modifier = Modifier.size(13.dp)
                        )
                    },
                    label = { Text("Pending", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("room_filter_pending")
                )
                FilterChip(
                    selected = currentFilter == MessageFilter.SENT_ONLY,
                    onClick = { onFilterChanged(MessageFilter.SENT_ONLY) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                            tint = SafetyGreen,
                            modifier = Modifier.size(13.dp)
                        )
                    },
                    label = { Text("Sent", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("room_filter_sent")
                )
                FilterChip(
                    selected = currentFilter == MessageFilter.RECEIVED_ONLY,
                    onClick = { onFilterChanged(MessageFilter.RECEIVED_ONLY) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CallReceived,
                            contentDescription = null,
                            tint = MeshBlue,
                            modifier = Modifier.size(13.dp)
                        )
                    },
                    label = { Text("Received", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("room_filter_received")
                )
                FilterChip(
                    selected = currentFilter == MessageFilter.CRITICAL_P5,
                    onClick = { onFilterChanged(MessageFilter.CRITICAL_P5) },
                    label = { Text("🔥 P5 Critical", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("room_filter_p5")
                )
                FilterChip(
                    selected = currentFilter == MessageFilter.SAFE,
                    onClick = { onFilterChanged(MessageFilter.SAFE) },
                    label = { Text("✅ Safe", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("room_filter_safe")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sorting row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sort Order:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val isNewest = sortOrder == RoomSortOrder.NEWEST_FIRST
                    val isOldest = sortOrder == RoomSortOrder.OLDEST_FIRST
                    val isPriority = sortOrder == RoomSortOrder.HIGHEST_PRIORITY

                    FilterChip(
                        selected = isNewest,
                        onClick = { onSortOrderChanged(RoomSortOrder.NEWEST_FIRST) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        },
                        label = { Text("Newest", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) }
                    )
                    FilterChip(
                        selected = isPriority,
                        onClick = { onSortOrderChanged(RoomSortOrder.HIGHEST_PRIORITY) },
                        label = { Text("Priority", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) }
                    )
                    FilterChip(
                        selected = isOldest,
                        onClick = { onSortOrderChanged(RoomSortOrder.OLDEST_FIRST) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        },
                        label = { Text("Oldest", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) }
                    )
                }
            }
        }
    }
}

@Composable
fun RoomMessageCard(
    message: LifeLinkMessage,
    onPlayTts: () -> Unit,
    onInspect: () -> Unit,
    onDelete: () -> Unit
) {
    val status = message.getOfflineStatus()
    val isCritical = message.priority >= 5
    val isQueued = status == OfflineMessageStatus.PENDING

    val borderColor by animateColorAsState(
        targetValue = when {
            isCritical -> EmergencyRed.copy(alpha = 0.6f)
            isQueued -> EmergencyAmber.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        },
        label = "cardBorderColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("room_message_card_${message.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isCritical) {
                EmergencyRed.copy(alpha = 0.07f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top Row: Status badge, Zone tag, Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Room status badge with visual status icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OfflineStatusBadge(
                        status = status,
                        messageId = message.id
                    )

                    // Zone tag
                    val zoneIcon = when (message.connectivityZone) {
                        ConnectivityZone.OFFLINE_ZERO_BARS -> Icons.Default.SignalCellularConnectedNoInternet0Bar
                        ConnectivityZone.LOW_CONNECTIVITY_EDGE -> Icons.Default.SignalCellularAlt
                        ConnectivityZone.MESH_HOP_CONNECTED -> Icons.Default.Wifi
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = zoneIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = message.connectivityZone.getShortLabel(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }
                }

                // Time & Status indicator icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formatRoomTimestamp(message.timestamp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = when (status) {
                            OfflineMessageStatus.SENT -> Icons.Default.DoneAll
                            OfflineMessageStatus.PENDING -> Icons.Default.Schedule
                            OfflineMessageStatus.RECEIVED -> Icons.Default.CallReceived
                        },
                        contentDescription = "Status: ${status.name.lowercase()}",
                        tint = when (status) {
                            OfflineMessageStatus.SENT -> SafetyGreen
                            OfflineMessageStatus.PENDING -> EmergencyAmber
                            OfflineMessageStatus.RECEIVED -> MeshBlue
                        },
                        modifier = Modifier
                            .size(14.dp)
                            .testTag("timestamp_status_icon_${status.id}_${message.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sender & Priority & Intent Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Priority Pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (message.priority) {
                            5 -> EmergencyRed
                            4 -> EmergencyAmber
                            3 -> MeshBlue
                            else -> SafetyGreen
                        }
                    ) {
                        Text(
                            text = "P${message.priority}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Intent Pill with Emoji
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = message.intent.getIconEmoji(), fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = message.intent.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }

                // Sender ID
                Text(
                    text = "Node: ${message.senderId}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary Message Text
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Translated Text if present
            if (!message.translatedText.isNullOrBlank() && message.translatedText != message.text) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "🌐 Translated (${message.targetLanguage?.uppercase(Locale.ROOT) ?: "RECEIVER"}):",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = message.translatedText,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Routing details & Quick Actions footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Route metadata
                Column {
                    Text(
                        text = "Hops: ${message.hopCount} | TTL: ${message.ttl} | Retry: ${message.retryCount}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (message.path.isNotEmpty()) {
                        Text(
                            text = "Path: ${message.path.joinToString(" → ")}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onPlayTts,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Speak via TTS",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onInspect,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Inspect Raw Payload",
                            tint = MeshBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete from Room DB",
                            tint = EmergencyRed.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RoomEmptyState(
    title: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("room_empty_state_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Storage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

private fun formatRoomTimestamp(epochMs: Long): String {
    val diff = System.currentTimeMillis() - epochMs
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        else -> SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(epochMs))
    }
}
