package com.example.lifelink.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifelink.LifeLinkEngine
import com.example.lifelink.ai.AndroidTtsEngine
import com.example.lifelink.ai.DemoSpeechRecognizer
import com.example.lifelink.ai.EmergencyVoicePreset
import com.example.lifelink.ai.EmergencyVoicePresets
import com.example.lifelink.ai.IndicTranslator
import com.example.lifelink.ai.IntentClassifier
import com.example.lifelink.audio.EmergencyAudio
import com.example.lifelink.data.ConnectivityZone
import com.example.lifelink.data.DeliveryStatus
import com.example.lifelink.data.EmergencyIntent
import com.example.lifelink.data.LifeLinkMessage
import com.example.lifelink.data.MessageDirection
import com.example.lifelink.data.MessageRepository
import com.example.lifelink.data.local.LifeLinkDatabase
import com.example.lifelink.data.local.OfflineStorageStats
import com.example.lifelink.network.MeshNetworkSimulator
import com.example.lifelink.network.MessageCodec
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class MessageFilter {
    ALL,
    OUTGOING_OFFLINE,
    INCOMING_MESH,
    CRITICAL_P5,
    HIGH_P4,
    SAFE
}

class LifeLinkViewModel(application: Application) : AndroidViewModel(application) {

    private val db = LifeLinkDatabase.getInstance(application)
    private val repository = MessageRepository(db.messageDao())
    val roomDatabaseManager = repository.dbManager

    private val speechRecognizer = DemoSpeechRecognizer()
    private val intentClassifier = IntentClassifier()
    private val translator = IndicTranslator()
    private val ttsEngine = AndroidTtsEngine(application)
    private val emergencyAudio = EmergencyAudio(application)

    val meshTransport = MeshNetworkSimulator(viewModelScope)

    private val engine = LifeLinkEngine(
        recognizer = speechRecognizer,
        classifier = intentClassifier,
        translator = translator
    )

    // Current simulated connectivity zone (Offline / Low Edge / Connected)
    private val _connectivityZone = MutableStateFlow(ConnectivityZone.OFFLINE_ZERO_BARS)
    val connectivityZone: StateFlow<ConnectivityZone> = _connectivityZone.asStateFlow()

    // Real-time Room DB storage statistics
    val storageStats: StateFlow<OfflineStorageStats> = repository.storageStats.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OfflineStorageStats()
    )

    private val _isSyncingQueue = MutableStateFlow(false)
    val isSyncingQueue: StateFlow<Boolean> = _isSyncingQueue.asStateFlow()

    private val _syncStatusMessage = MutableStateFlow<String?>(null)
    val syncStatusMessage: StateFlow<String?> = _syncStatusMessage.asStateFlow()

    // Perspectives: "Phone A" (Victim/Sender), "Phone B" (Mesh Relay), "Phone C" (Rescue Receiver)
    private val _currentPerspective = MutableStateFlow("Phone A")
    val currentPerspective: StateFlow<String> = _currentPerspective.asStateFlow()

    private val _sourceLanguage = MutableStateFlow("te") // Telugu default for SIH milestone
    val sourceLanguage: StateFlow<String> = _sourceLanguage.asStateFlow()

    private val _targetLanguage = MutableStateFlow("hi") // Hindi default receiver
    val targetLanguage: StateFlow<String> = _targetLanguage.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _micDecibelLevel = MutableStateFlow(0.15f)
    val micDecibelLevel: StateFlow<Float> = _micDecibelLevel.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow("")
    val lastRecognizedText: StateFlow<String> = _lastRecognizedText.asStateFlow()

    private val _lastDetectedIntent = MutableStateFlow<EmergencyIntent?>(null)
    val lastDetectedIntent: StateFlow<EmergencyIntent?> = _lastDetectedIntent.asStateFlow()

    private val _lastPriority = MutableStateFlow(0)
    val lastPriority: StateFlow<Int> = _lastPriority.asStateFlow()

    private val _activeAlarmMessage = MutableStateFlow<LifeLinkMessage?>(null)
    val activeAlarmMessage: StateFlow<LifeLinkMessage?> = _activeAlarmMessage.asStateFlow()

    private val _isAlarmActive = MutableStateFlow(false)
    val isAlarmActive: StateFlow<Boolean> = _isAlarmActive.asStateFlow()

    private val _selectedMessageForDetails = MutableStateFlow<LifeLinkMessage?>(null)
    val selectedMessageForDetails: StateFlow<LifeLinkMessage?> = _selectedMessageForDetails.asStateFlow()

    private val _filter = MutableStateFlow(MessageFilter.ALL)
    val filter: StateFlow<MessageFilter> = _filter.asStateFlow()

    private var waveformJob: Job? = null

    // Direct Room Database Flow for reactive observation
    val allMessagesFlow: kotlinx.coroutines.flow.Flow<List<LifeLinkMessage>> = repository.allMessages

    // Room DB Flow combined with filter
    val messagesList: StateFlow<List<LifeLinkMessage>> = combine(
        repository.allMessages,
        _filter
    ) { list, currentFilter ->
        when (currentFilter) {
            MessageFilter.ALL -> list
            MessageFilter.OUTGOING_OFFLINE -> list.filter { it.direction == MessageDirection.OUTGOING }
            MessageFilter.INCOMING_MESH -> list.filter { it.direction == MessageDirection.INCOMING }
            MessageFilter.CRITICAL_P5 -> list.filter { it.priority == 5 }
            MessageFilter.HIGH_P4 -> list.filter { it.priority >= 4 }
            MessageFilter.SAFE -> list.filter { it.intent == EmergencyIntent.SAFE }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        meshTransport.start()

        // Incoming message handler from mesh transport
        meshTransport.onMessageReceived { rawBytes ->
            viewModelScope.launch {
                try {
                    val incoming = MessageCodec.decode(rawBytes)

                    // Check if message needs translation to our receiver target language
                    val localizedText = if (incoming.language != _targetLanguage.value) {
                        translator.translate(incoming.text, incoming.language, _targetLanguage.value)
                    } else {
                        incoming.text
                    }

                    val processedMessage = incoming.copy(
                        direction = MessageDirection.INCOMING,
                        deliveryStatus = DeliveryStatus.RECEIVED_OFFLINE,
                        translatedText = localizedText,
                        targetLanguage = _targetLanguage.value,
                        connectivityZone = _connectivityZone.value
                    )

                    // Persist to Room local database manager
                    repository.storeIncomingMessage(processedMessage, _connectivityZone.value)

                    // If priority 5 (Fire, Medical, Trapped), trigger Emergency Alarm & TTS!
                    if (processedMessage.priority >= 5) {
                        triggerEmergencyAlarm(processedMessage)
                    } else {
                        // Play normal priority announcement
                        ttsEngine.speak(
                            text = localizedText,
                            language = _targetLanguage.value,
                            emergency = false
                        )
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun setConnectivityZone(zone: ConnectivityZone) {
        val previous = _connectivityZone.value
        _connectivityZone.value = zone
        if (previous == ConnectivityZone.OFFLINE_ZERO_BARS && zone != ConnectivityZone.OFFLINE_ZERO_BARS) {
            _syncStatusMessage.value = "Connectivity restored: Flushing offline Room queue over mesh..."
            syncOfflineQueue()
        } else {
            _syncStatusMessage.value = "Switched to ${zone.getDisplayName()}"
        }
    }

    fun syncOfflineQueue() {
        if (_isSyncingQueue.value) return
        viewModelScope.launch {
            _isSyncingQueue.value = true
            _syncStatusMessage.value = "Scanning Room database for pending offline messages..."
            val count = repository.syncPendingOutgoingQueue { pendingMsg ->
                val encoded = MessageCodec.encode(pendingMsg)
                meshTransport.broadcast(encoded)
                delay(350) // Simulate mesh packet propagation
                true
            }
            _isSyncingQueue.value = false
            _syncStatusMessage.value = if (count > 0) {
                "Synced $count offline messages from Room database over mesh"
            } else {
                "Offline outbox is empty — all messages synced"
            }
        }
    }

    fun setPerspective(nodeId: String) {
        _currentPerspective.value = nodeId
    }

    fun setSourceLanguage(langCode: String) {
        _sourceLanguage.value = langCode
    }

    fun setTargetLanguage(langCode: String) {
        _targetLanguage.value = langCode
    }

    fun setFilter(newFilter: MessageFilter) {
        _filter.value = newFilter
    }

    fun selectMessageForDetails(message: LifeLinkMessage?) {
        _selectedMessageForDetails.value = message
    }

    fun startListening() {
        _isListening.value = true
        waveformJob?.cancel()
        waveformJob = viewModelScope.launch {
            while (_isListening.value) {
                // Simulate acoustic amplitude variation
                _micDecibelLevel.value = 0.25f + Random.nextFloat() * 0.7f
                delay(90)
            }
            _micDecibelLevel.value = 0.1f
        }
    }

    fun stopListeningAndSend(customPromptText: String? = null) {
        if (!_isListening.value && customPromptText == null) return
        _isListening.value = false
        waveformJob?.cancel()
        _micDecibelLevel.value = 0.1f

        viewModelScope.launch {
            val textToSend = if (!customPromptText.isNullOrBlank()) {
                customPromptText
            } else {
                // Check language default
                if (_sourceLanguage.value == "te") {
                    "భవనం రెండవ అంతస్తులో భారీ మంటలు, వెంటనే సహాయం కావాలి"
                } else if (_sourceLanguage.value == "hi") {
                    "दूसरी मंजिल पर आग लगी है, तत्काल सहायता की आवश्यकता है"
                } else {
                    "There is a severe fire on the second floor, need urgent rescue"
                }
            }

            speechRecognizer.setSimulatedText(textToSend)
            val recognized = speechRecognizer.recognize(ByteArray(0))
            _lastRecognizedText.value = recognized

            val packet = engine.processText(
                text = recognized,
                senderId = _currentPerspective.value,
                language = _sourceLanguage.value,
                targetLanguage = _targetLanguage.value
            )

            _lastDetectedIntent.value = packet.intent
            _lastPriority.value = packet.priority

            // Store outgoing message into local Room Database
            val savedPacket = repository.storeOutgoingMessage(packet, _connectivityZone.value)

            // If connected or in low-connectivity edge, broadcast immediately via mesh
            if (_connectivityZone.value != ConnectivityZone.OFFLINE_ZERO_BARS) {
                val encoded = MessageCodec.encode(savedPacket)
                meshTransport.broadcast(encoded)
            } else {
                _syncStatusMessage.value = "Message safely saved in offline Room database (Queued for mesh sync)"
            }
        }
    }

    fun sendVoicePreset(preset: EmergencyVoicePreset) {
        viewModelScope.launch {
            _sourceLanguage.value = preset.language
            _lastRecognizedText.value = preset.text

            val packet = engine.processText(
                text = preset.text,
                senderId = _currentPerspective.value,
                language = preset.language,
                targetLanguage = _targetLanguage.value
            )

            _lastDetectedIntent.value = packet.intent
            _lastPriority.value = packet.priority

            // Store outgoing message into local Room Database
            val savedPacket = repository.storeOutgoingMessage(packet, _connectivityZone.value)

            // If connected or edge, broadcast to mesh
            if (_connectivityZone.value != ConnectivityZone.OFFLINE_ZERO_BARS) {
                val encoded = MessageCodec.encode(savedPacket)
                meshTransport.broadcast(encoded)
            } else {
                _syncStatusMessage.value = "Preset alert stored in offline Room database (Outbox queued)"
            }
        }
    }

    fun triggerEmergencyAlarm(message: LifeLinkMessage) {
        _activeAlarmMessage.value = message
        _isAlarmActive.value = true

        // 1. Request Exclusive Alarm Audio Focus
        emergencyAudio.requestEmergencyAudio()

        // 2. Play Siren beeps
        emergencyAudio.playEmergencySirenTone(1400)

        // 3. Speak high-priority TTS in target regional language
        viewModelScope.launch {
            delay(1500)
            val speakText = message.translatedText ?: message.text
            ttsEngine.speak(
                text = "Emergency Alert: $speakText",
                language = message.targetLanguage ?: _targetLanguage.value,
                emergency = true
            )
        }
    }

    fun dismissEmergencyAlarm() {
        _isAlarmActive.value = false
        _activeAlarmMessage.value = null
        emergencyAudio.stopSirenTone()
        ttsEngine.stop()
        emergencyAudio.release()
    }

    fun speakMessageManually(message: LifeLinkMessage) {
        viewModelScope.launch {
            val text = message.translatedText ?: message.text
            val lang = message.targetLanguage ?: message.language
            ttsEngine.speak(text, lang, message.priority >= 5)
        }
    }

    fun deleteMessage(id: String) {
        viewModelScope.launch {
            repository.deleteMessage(id)
            if (_selectedMessageForDetails.value?.id == id) {
                _selectedMessageForDetails.value = null
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _selectedMessageForDetails.value = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        waveformJob?.cancel()
        meshTransport.stop()
        ttsEngine.release()
        emergencyAudio.release()
    }
}
