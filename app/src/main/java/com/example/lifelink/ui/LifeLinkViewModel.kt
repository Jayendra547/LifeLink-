package com.example.lifelink.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifelink.LifeLinkEngine
import com.example.lifelink.ai.AndroidTtsEngine
import com.example.lifelink.ai.DemoSpeechRecognizer
import com.example.lifelink.ai.EmergencyVoicePreset
import com.example.lifelink.ai.EmergencyVoicePresets
import com.example.lifelink.ai.IndicTranslator
import com.example.lifelink.ai.IntentClassifier
import com.example.lifelink.ai.SupportedLanguages
import com.example.lifelink.audio.EmergencyAudio
import com.example.lifelink.data.EmergencyIntent
import com.example.lifelink.data.LifeLinkMessage
import com.example.lifelink.data.MessageRepository
import com.example.lifelink.data.local.LifeLinkDatabase
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
    CRITICAL_P5,
    HIGH_P4,
    SAFE
}

class LifeLinkViewModel(application: Application) : AndroidViewModel(application) {

    private val db = LifeLinkDatabase.getInstance(application)
    private val repository = MessageRepository(db.messageDao())

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

    // Room DB Flow combined with filter
    val messagesList: StateFlow<List<LifeLinkMessage>> = combine(
        repository.allMessages,
        _filter
    ) { list, currentFilter ->
        when (currentFilter) {
            MessageFilter.ALL -> list
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
                        translatedText = localizedText,
                        targetLanguage = _targetLanguage.value
                    )

                    // Persist to Room
                    repository.saveMessage(processedMessage)

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

            // Save outgoing to local Room
            repository.saveMessage(packet)

            // Broadcast to mesh transport
            val encoded = MessageCodec.encode(packet)
            meshTransport.broadcast(encoded)
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

            // Save outgoing to local Room
            repository.saveMessage(packet)

            // Broadcast through mesh
            val encoded = MessageCodec.encode(packet)
            meshTransport.broadcast(encoded)
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
