package com.example.lifelink

import com.example.lifelink.ai.IntentClassifier
import com.example.lifelink.ai.SpeechRecognizer
import com.example.lifelink.ai.Translator
import com.example.lifelink.data.LifeLinkMessage
import com.example.lifelink.network.MessageCodec
import java.util.UUID

class LifeLinkEngine(
    private val recognizer: SpeechRecognizer,
    private val classifier: IntentClassifier,
    private val translator: Translator
) {

    suspend fun processVoice(
        audio: ByteArray,
        senderId: String,
        language: String,
        targetLanguage: String? = null
    ): LifeLinkMessage {
        val text = recognizer.recognize(audio)
        return processText(text, senderId, language, targetLanguage)
    }

    suspend fun processText(
        text: String,
        senderId: String,
        language: String,
        targetLanguage: String? = null
    ): LifeLinkMessage {
        val intent = classifier.classify(text)
        val priority = classifier.priority(intent)

        val translated = if (!targetLanguage.isNullOrBlank() && targetLanguage != language) {
            translator.translate(text, language, targetLanguage)
        } else {
            null
        }

        return LifeLinkMessage(
            id = UUID.randomUUID().toString().substring(0, 8),
            senderId = senderId,
            text = text,
            language = language,
            intent = intent,
            priority = priority,
            timestamp = System.currentTimeMillis(),
            hopCount = 0,
            ttl = 5,
            path = listOf(senderId),
            translatedText = translated,
            targetLanguage = targetLanguage
        )
    }

    fun encode(message: LifeLinkMessage): ByteArray {
        return MessageCodec.encode(message)
    }

    fun decode(data: ByteArray): LifeLinkMessage {
        return MessageCodec.decode(data)
    }
}
