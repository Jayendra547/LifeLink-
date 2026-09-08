package com.example.lifelink.ai

interface SpeechRecognizer {
    suspend fun recognize(audioData: ByteArray): String
}

class DemoSpeechRecognizer(
    private val defaultTranscript: String = "There is a fire on the second floor, need immediate rescue"
) : SpeechRecognizer {

    private var simulatedText: String = defaultTranscript

    fun setSimulatedText(text: String) {
        simulatedText = text
    }

    override suspend fun recognize(audioData: ByteArray): String {
        // Temporary demo response / simulated offline IndicConformer inference
        return simulatedText
    }
}

data class EmergencyVoicePreset(
    val title: String,
    val text: String,
    val language: String,
    val iconEmoji: String
)

object EmergencyVoicePresets {
    val presets = listOf(
        EmergencyVoicePreset(
            title = "Building Fire",
            text = "There is a fire on the second floor, heavy smoke spreading",
            language = "en",
            iconEmoji = "🔥"
        ),
        EmergencyVoicePreset(
            title = "Medical Trauma",
            text = "Two people severely injured and bleeding, need ambulance immediately",
            language = "en",
            iconEmoji = "🚑"
        ),
        EmergencyVoicePreset(
            title = "Trapped Under Debris",
            text = "We are trapped under collapsed ceiling in room 302, send rescue team",
            language = "en",
            iconEmoji = "🏚️"
        ),
        EmergencyVoicePreset(
            title = "Telugu: Fire Emergency",
            text = "భవనం రెండవ అంతస్తులో భారీ మంటలు చెలరేగాయి, వెంటనే కాపాడండి",
            language = "te",
            iconEmoji = "🔥"
        ),
        EmergencyVoicePreset(
            title = "Hindi: Medical SOS",
            text = "यहाँ दो लोग गंभीर रूप से घायल हैं, तुरंत डॉक्टर और एम्बुलेंस चाहिए",
            language = "hi",
            iconEmoji = "🚑"
        ),
        EmergencyVoicePreset(
            title = "Clean Water SOS",
            text = "Critical shortage of drinking water and baby food for 15 people",
            language = "en",
            iconEmoji = "💧"
        ),
        EmergencyVoicePreset(
            title = "Evacuated & Safe",
            text = "Our sector is evacuated, everyone is safe and accounted for",
            language = "en",
            iconEmoji = "✅"
        )
    )
}
