package com.example.lifelink.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

class EmergencyAudio(
    private val context: Context
) {
    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private var focusRequest: AudioFocusRequest? = null
    private var activeAudioTrack: AudioTrack? = null
    private var sirenJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest = AudioFocusRequest.Builder(
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
                )
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .build()
            }
        } catch (e: Exception) {
            Log.e("EmergencyAudio", "Failed to build focusRequest", e)
        }
    }

    fun requestEmergencyAudio(): Boolean {
        if (audioManager == null) return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let {
                audioManager.requestAudioFocus(it) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } ?: false
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    fun playEmergencySirenTone(durationMs: Int = 1200) {
        stopSirenTone()
        sirenJob = scope.launch {
            try {
                requestEmergencyAudio()
                playSirenInternal(durationMs)
            } catch (e: Exception) {
                Log.e("EmergencyAudio", "Failed to play emergency siren", e)
            }
        }
    }

    private fun playSirenInternal(durationMs: Int) {
        val sampleRate = 22050
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufferSize, sampleRate / 10)

        val audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } else {
            @Suppress("DEPRECATION")
            AudioTrack(
                AudioManager.STREAM_ALARM,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM
            )
        }

        synchronized(this) {
            activeAudioTrack = audioTrack
        }

        try {
            audioTrack.play()
            val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val shortBuffer = ShortArray(1024)
            var currentSample = 0

            // Alternating two-tone emergency alarm (850Hz and 1150Hz)
            val lowFreq = 850.0
            val highFreq = 1150.0
            val cycleSamples = (sampleRate * 0.25).toInt() // alternate every 250ms

            while (currentSample < totalSamples && sirenJob?.isActive == true) {
                val chunkSize = minOf(shortBuffer.size, totalSamples - currentSample)
                for (i in 0 until chunkSize) {
                    val t = (currentSample + i).toDouble() / sampleRate
                    val freq = if (((currentSample + i) / cycleSamples) % 2 == 0) highFreq else lowFreq
                    val angle = 2.0 * Math.PI * freq * t
                    val sampleValue = (sin(angle) * 32767 * 0.85).toInt().coerceIn(-32768, 32767)
                    shortBuffer[i] = sampleValue.toShort()
                }
                audioTrack.write(shortBuffer, 0, chunkSize)
                currentSample += chunkSize
            }
        } catch (e: Exception) {
            Log.e("EmergencyAudio", "Error generating siren audio", e)
        } finally {
            try {
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {}
            synchronized(this) {
                if (activeAudioTrack === audioTrack) {
                    activeAudioTrack = null
                }
            }
        }
    }

    fun stopSirenTone() {
        sirenJob?.cancel()
        sirenJob = null
        synchronized(this) {
            try {
                activeAudioTrack?.let {
                    it.pause()
                    it.flush()
                    it.stop()
                    it.release()
                }
            } catch (_: Exception) {}
            activeAudioTrack = null
        }
    }

    fun release() {
        try {
            stopSirenTone()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            Log.e("EmergencyAudio", "Error during release", e)
        }
    }
}
