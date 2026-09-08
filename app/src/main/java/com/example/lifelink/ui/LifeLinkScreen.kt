package com.example.lifelink.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifelink.ai.EmergencyVoicePreset
import com.example.lifelink.ai.EmergencyVoicePresets
import com.example.lifelink.ai.SupportedLanguage
import com.example.lifelink.ai.SupportedLanguages
import com.example.lifelink.data.EmergencyIntent
import com.example.lifelink.data.LifeLinkMessage
import com.example.lifelink.network.MeshNode
import com.example.lifelink.network.MessageCodec
import com.example.lifelink.network.PacketHopEvent
import com.example.ui.theme.EmergencyAmber
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.MeshBlue
import com.example.ui.theme.SafetyGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeLinkScreen(
    viewModel: LifeLinkViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messagesList.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val micLevel by viewModel.micDecibelLevel.collectAsState()
    val currentPerspective by viewModel.currentPerspective.collectAsState()
    val sourceLang by viewModel.sourceLanguage.collectAsState()
    val targetLang by viewModel.targetLanguage.collectAsState()
    val lastRecognizedText by viewModel.lastRecognizedText.collectAsState()
    val lastIntent by viewModel.lastDetectedIntent.collectAsState()
    val isAlarmActive by viewModel.isAlarmActive.collectAsState()
    val alarmMessage by viewModel.activeAlarmMessage.collectAsState()
    val selectedDetailMessage by viewModel.selectedMessageForDetails.collectAsState()
    val filter by viewModel.filter.collectAsState()

    val meshNodes by viewModel.meshTransport.activeNodes.collectAsState()
    val currentHopEvent by viewModel.meshTransport.currentHopEvent.collectAsState()
    val isSimulatingTransit by viewModel.meshTransport.isSimulatingTransit.collectAsState()

    var showCustomInputDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (isAlarmActive) EmergencyRed else SafetyGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "LifeLink",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Offline Emergency Mesh & Translation",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.clearAllHistory() },
                        modifier = Modifier.testTag("clear_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Message History"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Emergency Alarm Siren Banner (When Priority 5 active)
            AnimatedVisibility(
                visible = isAlarmActive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                alarmMessage?.let { alarm ->
                    EmergencyAlarmBanner(
                        message = alarm,
                        onDismiss = { viewModel.dismissEmergencyAlarm() },
                        onReplayTts = { viewModel.speakMessageManually(alarm) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Mesh Perspective & Network Status
                item {
                    MeshStatusBar(
                        currentPerspective = currentPerspective,
                        onSelectPerspective = { viewModel.setPerspective(it) },
                        meshNodes = meshNodes
                    )
                }

                // Section 2: Multi-Hop Mesh Topology Live Visualizer
                item {
                    MeshTopologyCard(
                        nodes = meshNodes,
                        hopEvent = currentHopEvent,
                        isTransmitting = isSimulatingTransit
                    )
                }

                // Section 3: Dual-Language Cross-Lingual Selector
                item {
                    LanguageConfigCard(
                        sourceLang = sourceLang,
                        targetLang = targetLang,
                        onSelectSource = { viewModel.setSourceLanguage(it) },
                        onSelectTarget = { viewModel.setTargetLanguage(it) }
                    )
                }

                // Section 4: Voice Capture & Hold-To-Speak Engine
                item {
                    VoiceCapturePanel(
                        isListening = isListening,
                        micLevel = micLevel,
                        lastRecognizedText = lastRecognizedText,
                        lastIntent = lastIntent,
                        sourceLang = sourceLang,
                        targetLang = targetLang,
                        onStartListening = { viewModel.startListening() },
                        onStopListening = { viewModel.stopListeningAndSend() },
                        onOpenCustomInput = { showCustomInputDialog = true }
                    )
                }

                // Section 5: Instant Emergency Voice Presets (Field Drills)
                item {
                    EmergencyPresetsRow(
                        presets = EmergencyVoicePresets.presets,
                        onSelectPreset = { preset ->
                            viewModel.sendVoicePreset(preset)
                        }
                    )
                }

                // Section 6: Filter Chips & History Header
                item {
                    MessageHistoryHeader(
                        currentFilter = filter,
                        onFilterSelected = { viewModel.setFilter(it) },
                        messageCount = messages.size
                    )
                }

                // Section 7: Message Feed
                if (messages.isEmpty()) {
                    item {
                        EmptyHistoryPlaceholder()
                    }
                } else {
                    items(messages, key = { it.id }) { msg ->
                        MessageItemCard(
                            message = msg,
                            onPlayTts = { viewModel.speakMessageManually(msg) },
                            onInspect = { viewModel.selectMessageForDetails(msg) },
                            onDelete = { viewModel.deleteMessage(msg.id) }
                        )
                    }
                }
            }
        }
    }

    // Packet Inspection Dialog (Byte-for-byte JSON viewer)
    selectedDetailMessage?.let { msg ->
        PacketInspectorDialog(
            message = msg,
            onDismiss = { viewModel.selectMessageForDetails(null) }
        )
    }

    // Custom incident text entry dialog
    if (showCustomInputDialog) {
        CustomIncidentDialog(
            sourceLang = sourceLang,
            onDismiss = { showCustomInputDialog = false },
            onSend = { text ->
                viewModel.stopListeningAndSend(customPromptText = text)
                showCustomInputDialog = false
            }
        )
    }
}

@Composable
fun MeshStatusBar(
    currentPerspective: String,
    onSelectPerspective: (String) -> Unit,
    meshNodes: List<MeshNode>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = null,
                        tint = MeshBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Perspective Node:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(SafetyGreen)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Mesh Online (${meshNodes.size} Nodes)",
                        style = MaterialTheme.typography.labelSmall,
                        color = SafetyGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                meshNodes.forEach { node ->
                    val isSelected = currentPerspective == node.name
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectPerspective(node.name) },
                        label = {
                            Text(
                                text = node.name,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MeshBlue,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("node_chip_${node.id}")
                    )
                }
            }
        }
    }
}

@Composable
fun MeshTopologyCard(
    nodes: List<MeshNode>,
    hopEvent: PacketHopEvent?,
    isTransmitting: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CellTower,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Multi-Hop Mesh Routing (SIH 26173)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                if (isTransmitting) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EmergencyAmber.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "⚡ Relaying Packet...",
                            style = MaterialTheme.typography.labelSmall.copy(color = EmergencyAmber),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3-Node Interactive Diagram
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                nodes.forEachIndexed { index, node ->
                    val isActiveHop = hopEvent?.toNode == node.name || hopEvent?.fromNode == node.name
                    val nodeColor = if (isActiveHop && isTransmitting) EmergencyAmber else MeshBlue

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(nodeColor.copy(alpha = 0.15f))
                                .border(
                                    width = if (isActiveHop && isTransmitting) 2.dp else 1.dp,
                                    color = nodeColor,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (index) {
                                    0 -> "A"
                                    1 -> "B"
                                    else -> "C"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = nodeColor
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = node.name,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = when (index) {
                                0 -> "Origin/Victim"
                                1 -> "Mesh Relay"
                                else -> "Command HQ"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (index < nodes.size - 1) {
                        Box(
                            modifier = Modifier
                                .weight(0.6f)
                                .height(2.dp)
                                .background(
                                    if (isTransmitting) EmergencyAmber else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Real-time hop telemetry
            hopEvent?.let { hop ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Last Hop: ${hop.fromNode} ➔ ${hop.toNode}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = "Delay: ${hop.latencyMs}ms | Hop: #${hop.hopNumber}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageConfigCard(
    sourceLang: String,
    targetLang: String,
    onSelectSource: (String) -> Unit,
    onSelectTarget: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Cross-Lingual Disaster Translation",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Speaks in Sender Language ➔ Receives in Responders Language (Offline)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Source Language Selector
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Voice Input Language",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LanguageDropdown(
                        selectedCode = sourceLang,
                        onSelect = onSelectSource,
                        testTag = "source_lang_selector"
                    )
                }

                // Target Language Selector
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Receiver / TTS Language",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LanguageDropdown(
                        selectedCode = targetLang,
                        onSelect = onSelectTarget,
                        testTag = "target_lang_selector"
                    )
                }
            }
        }
    }
}

@Composable
fun LanguageDropdown(
    selectedCode: String,
    onSelect: (String) -> Unit,
    testTag: String
) {
    var expanded by remember { mutableStateOf(false) }
    val current = SupportedLanguages.getByCode(selectedCode)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${current.name} (${current.nativeName})",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                maxLines = 1
            )
            Text(text = "▾", style = MaterialTheme.typography.labelLarge)
        }
    }

    if (expanded) {
        AlertDialog(
            onDismissRequest = { expanded = false },
            title = { Text("Select Language") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SupportedLanguages.list.forEach { lang ->
                        val isSelected = lang.code.equals(selectedCode, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(lang.code)
                                    expanded = false
                                }
                                .padding(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(lang.flagEmoji, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = lang.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = lang.nativeName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { expanded = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun VoiceCapturePanel(
    isListening: Boolean,
    micLevel: Float,
    lastRecognizedText: String,
    lastIntent: EmergencyIntent?,
    sourceLang: String,
    targetLang: String,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onOpenCustomInput: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Offline Voice Capture",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Hold button to record or tap to broadcast offline emergency",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Concentric Ring & Hold-To-Speak Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(170.dp)
            ) {
                // Outer Pulse Ring when listening
                if (isListening) {
                    Box(
                        modifier = Modifier
                            .size(170.dp * pulseScale)
                            .clip(CircleShape)
                            .background(EmergencyRed.copy(alpha = 0.2f))
                    )
                    Box(
                        modifier = Modifier
                            .size(145.dp * (1.0f + micLevel * 0.3f))
                            .clip(CircleShape)
                            .background(EmergencyRed.copy(alpha = 0.35f))
                    )
                }

                // Core Push-To-Talk Button
                val buttonGradient = if (isListening) {
                    Brush.verticalGradient(listOf(EmergencyRed, Color(0xFFB91C1C)))
                } else {
                    Brush.verticalGradient(listOf(Color(0xFFDC2626), Color(0xFF991B1B)))
                }

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(buttonGradient)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    onStartListening()
                                    tryAwaitRelease()
                                    onStopListening()
                                }
                            )
                        }
                        .testTag("hold_to_speak_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                            contentDescription = "Hold to Speak",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isListening) "LISTENING" else "HOLD TO SPEAK",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // State & Waveform Status
            Text(
                text = if (isListening) "🔴 Capturing offline audio wave..." else "Ready to capture",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (isListening) EmergencyRed else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Live Recognized Text Preview & Intent Tag
            if (lastRecognizedText.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recognized Transcript:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            lastIntent?.let { intent ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = getIntentColor(intent).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${intent.getIconEmoji()} ${intent.name} (P${getIntentPriority(intent)})",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = getIntentColor(intent)
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\"$lastRecognizedText\"",
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onOpenCustomInput,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("type_incident_button"),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Type Incident Message / Custom Voice Drill")
            }
        }
    }
}

@Composable
fun EmergencyPresetsRow(
    presets: List<EmergencyVoicePreset>,
    onSelectPreset: (EmergencyVoicePreset) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        PaddingValues(horizontal = 16.dp).let {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Emergency Drills (SIH Scenarios)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Tap to Broadcast",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(presets) { preset ->
                Card(
                    modifier = Modifier
                        .width(190.dp)
                        .clickable { onSelectPreset(preset) }
                        .testTag("preset_${preset.title.replace(" ", "_")}"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(preset.iconEmoji, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = preset.title,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = preset.text,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Broadcast ➔",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessageHistoryHeader(
    currentFilter: MessageFilter,
    onFilterSelected: (MessageFilter) -> Unit,
    messageCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mesh Message Log ($messageCount)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = currentFilter == MessageFilter.ALL,
                onClick = { onFilterSelected(MessageFilter.ALL) },
                label = { Text("All Packets") },
                modifier = Modifier.testTag("filter_all")
            )
            FilterChip(
                selected = currentFilter == MessageFilter.CRITICAL_P5,
                onClick = { onFilterSelected(MessageFilter.CRITICAL_P5) },
                label = { Text("🔥 Critical P5") },
                modifier = Modifier.testTag("filter_p5")
            )
            FilterChip(
                selected = currentFilter == MessageFilter.HIGH_P4,
                onClick = { onFilterSelected(MessageFilter.HIGH_P4) },
                label = { Text("⚠️ High P4+") },
                modifier = Modifier.testTag("filter_p4")
            )
            FilterChip(
                selected = currentFilter == MessageFilter.SAFE,
                onClick = { onFilterSelected(MessageFilter.SAFE) },
                label = { Text("✅ Safe Status") },
                modifier = Modifier.testTag("filter_safe")
            )
        }
    }
}

@Composable
fun MessageItemCard(
    message: LifeLinkMessage,
    onPlayTts: () -> Unit,
    onInspect: () -> Unit,
    onDelete: () -> Unit
) {
    val priorityColor = getIntentColor(message.intent)
    val timeFormatted = remember(message.timestamp) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(message.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("message_card_${message.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header with priority badge, sender, time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = priorityColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "${message.intent.getIconEmoji()} P${message.priority} ${message.intent.name}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = priorityColor
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "from ${message.senderId}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Original Message Text
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )

            // Indic Translated Text (if present)
            message.translatedText?.let { translated ->
                if (translated != message.text) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "Translated (${message.targetLanguage?.uppercase() ?: "RESCUE"}):",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            )
                            Text(
                                text = translated,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Multi-Hop Path and Telemetry
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val pathString = if (message.path.isNotEmpty()) {
                    message.path.joinToString(" ➔ ")
                } else {
                    message.senderId
                }

                Text(
                    text = "Path: $pathString | Hops: ${message.hopCount} | TTL: ${message.ttl}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPlayTts,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("play_tts_${message.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Speak Alert",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onInspect,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("inspect_packet_${message.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Inspect Packet JSON",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_message_${message.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete Message",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmergencyAlarmBanner(
    message: LifeLinkMessage,
    onDismiss: () -> Unit,
    onReplayTts: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("emergency_alarm_banner"),
        colors = CardDefaults.cardColors(
            containerColor = EmergencyRed
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CRITICAL EMERGENCY ALARM",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = "PRIORITY 5",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${message.intent.getIconEmoji()} ${message.intent.getDisplayName()}",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = message.translatedText ?: message.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.95f)
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = EmergencyRed
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dismiss_alarm_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Mute / Stop Siren",
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onReplayTts,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("replay_tts_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Replay Audio")
                }
            }
        }
    }
}

@Composable
fun PacketInspectorDialog(
    message: LifeLinkMessage,
    onDismiss: () -> Unit
) {
    val prettyJson = remember(message) {
        MessageCodec.prettyPrint(message)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Packet Inspector",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "ID: ${message.id}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "SIH 26173 Compact JSON Payload (${prettyJson.toByteArray().size} bytes):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = prettyJson,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun CustomIncidentDialog(
    sourceLang: String,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Emergency Broadcast Input",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Enter emergency text to classify, translate, and transmit through the offline mesh:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("e.g. Building collapsed, trapped on 3rd floor") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_incident_input"),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onSend(textInput)
                    }
                },
                enabled = textInput.isNotBlank(),
                modifier = Modifier.testTag("send_custom_incident_button")
            ) {
                Text("Broadcast SOS")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EmptyHistoryPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Sensors,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(54.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No Packets Broadcasted Yet",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Use Hold to Speak or select an Emergency Drill scenario above to broadcast across the mesh.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

fun getIntentColor(intent: EmergencyIntent): Color {
    return when (intent) {
        EmergencyIntent.FIRE,
        EmergencyIntent.MEDICAL,
        EmergencyIntent.TRAPPED -> EmergencyRed
        EmergencyIntent.HELP -> EmergencyAmber
        EmergencyIntent.FOOD_WATER -> MeshBlue
        EmergencyIntent.SAFE -> SafetyGreen
        EmergencyIntent.GENERAL -> Color(0xFF64748B)
    }
}

fun getIntentPriority(intent: EmergencyIntent): Int {
    return when (intent) {
        EmergencyIntent.FIRE,
        EmergencyIntent.MEDICAL,
        EmergencyIntent.TRAPPED -> 5
        EmergencyIntent.HELP -> 4
        EmergencyIntent.FOOD_WATER -> 2
        EmergencyIntent.SAFE,
        EmergencyIntent.GENERAL -> 1
    }
}
