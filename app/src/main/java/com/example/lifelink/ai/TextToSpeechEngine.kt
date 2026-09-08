package com.example.lifelink.ai

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

interface TextToSpeechEngine {
    suspend fun speak(
        text: String,
        language: String,
        emergency: Boolean
    )

    fun stop()
    fun release()
}

class AndroidTtsEngine(
    context: Context
) : TextToSpeechEngine, TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("LifeLinkTTS", "Failed to init TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.setSpeechRate(0.92f) // slightly slower for high clarity during disaster alarms
            // Set audio attributes to emergency speech/alarm
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            tts?.setAudioAttributes(audioAttributes)
        } else {
            Log.w("LifeLinkTTS", "TextToSpeech init returned code $status")
        }
    }

    override suspend fun speak(
        text: String,
        language: String,
        emergency: Boolean
    ) {
        if (!isInitialized || tts == null) {
            Log.w("LifeLinkTTS", "TTS not initialized yet")
            return
        }

        val locale = when (language.lowercase()) {
            "te" -> Locale("te", "IN")
            "hi" -> Locale("hi", "IN")
            "ta" -> Locale("ta", "IN")
            "kn" -> Locale("kn", "IN")
            "ml" -> Locale("ml", "IN")
            "bn" -> Locale("bn", "IN")
            else -> Locale.ENGLISH
        }

        val langResult = tts?.setLanguage(locale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback to English if the specific regional voice pack is missing on device
            tts?.language = Locale.ENGLISH
        }

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        val queueMode = if (emergency) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts?.speak(text, queueMode, params, "lifelink_${System.currentTimeMillis()}")
    }

    override fun stop() {
        tts?.stop()
    }

    override fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            Log.e("LifeLinkTTS", "Error shutting down TTS", e)
        }
    }
}
