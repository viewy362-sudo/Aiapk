package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.security.ZeroCrashGuard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Synthesized PCM Tactical Sound FX Engine.
 * Generates crisp zero-asset click, switch, and completion chimes via AudioTrack.
 */
object SoundFxManager {
    private const val SAMPLE_RATE = 22050
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + ZeroCrashGuard.coroutineExceptionHandler)

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    fun toggleSound() {
        val next = !_soundEnabled.value
        _soundEnabled.value = next
        if (next) playToggle(true)
    }

    fun setSoundEnabled(enabled: Boolean) {
        _soundEnabled.value = enabled
    }

    fun playClick() {
        if (!_soundEnabled.value) return
        scope.launch {
            playToneBurst(startFreqHz = 1450.0, endFreqHz = 780.0, durationMs = 20, amplitude = 0.28)
        }
    }

    fun playToggle(active: Boolean) {
        if (!_soundEnabled.value) return
        scope.launch {
            if (active) {
                playToneBurst(startFreqHz = 720.0, endFreqHz = 1280.0, durationMs = 36, amplitude = 0.25)
            } else {
                playToneBurst(startFreqHz = 960.0, endFreqHz = 520.0, durationMs = 34, amplitude = 0.22)
            }
        }
    }

    fun playSuccessChime() {
        if (!_soundEnabled.value) return
        scope.launch {
            playDualChime()
        }
    }

    private fun playToneBurst(startFreqHz: Double, endFreqHz: Double, durationMs: Int, amplitude: Double) {
        ZeroCrashGuard.runGuarded("SoundFxManager.playToneBurst", Unit) {
            val count = (SAMPLE_RATE * durationMs) / 1000
            val samples = ShortArray(count)
            var phase = 0.0
            for (i in 0 until count) {
                val t = i.toDouble() / count
                val freq = startFreqHz + (endFreqHz - startFreqHz) * t
                val envelope = exp(-5.5 * t)
                phase += 2.0 * PI * freq / SAMPLE_RATE
                val v = (sin(phase) * envelope * amplitude * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                samples[i] = v.toShort()
            }
            playPcmShorts(samples)
        }
    }

    private fun playDualChime() {
        ZeroCrashGuard.runGuarded("SoundFxManager.playDualChime", Unit) {
            val totalMs = 150
            val count = (SAMPLE_RATE * totalMs) / 1000
            val half = count / 2
            val samples = ShortArray(count)
            var phase = 0.0
            for (i in 0 until count) {
                val freq = if (i < half) 880.0 else 1318.5
                val localT = if (i < half) i.toDouble() / half else (i - half).toDouble() / (count - half)
                val env = exp(-4.0 * localT)
                phase += 2.0 * PI * freq / SAMPLE_RATE
                val v = (sin(phase) * env * 0.30 * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                samples[i] = v.toShort()
            }
            playPcmShorts(samples)
        }
    }

    private fun playPcmShorts(samples: ShortArray) {
        val byteLen = samples.size * 2
        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufSize = maxOf(byteLen, minBuf.coerceAtLeast(byteLen))
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val format = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()

        val track = AudioTrack.Builder()
            .setAudioAttributes(attrs)
            .setAudioFormat(format)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(bufSize)
            .build()

        try {
            track.write(samples, 0, samples.size)
            track.play()
            Thread.sleep(((samples.size * 1000L) / SAMPLE_RATE) + 25L)
        } finally {
            runCatching { track.stop() }
            runCatching { track.release() }
        }
    }
}
