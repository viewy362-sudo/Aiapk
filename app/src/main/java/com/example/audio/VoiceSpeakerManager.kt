package com.example.audio

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.i18n.AppLanguage
import com.example.i18n.FeatureExplanation
import com.example.security.ZeroCrashGuard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/**
 * Multi-language Android TextToSpeech manager.
 *
 * Speaks selected language confirmations and full feature explanations aloud cleanly,
 * with safe fallbacks on devices or tests without an active TTS engine.
 */
object VoiceSpeakerManager {

    private var tts: TextToSpeech? = null
    private var appContext: Context? = null
    private var pendingUtterance: Pair<String, AppLanguage>? = null
    private val utteranceCounter = AtomicInteger(1)

    private val _voiceEnabled = MutableStateFlow(true)
    val voiceEnabled: StateFlow<Boolean> = _voiceEnabled.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _ttsReady = MutableStateFlow(false)
    val ttsReady: StateFlow<Boolean> = _ttsReady.asStateFlow()

    fun init(context: Context, skipEngineInit: Boolean = false) {
        appContext = context.applicationContext
        if (skipEngineInit || isRobolectricEnvironment()) return
        if (tts != null) return

        ZeroCrashGuard.runGuarded("VoiceSpeaker.init", Unit) {
            tts = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    _ttsReady.value = true
                    tts?.setSpeechRate(1.0f)
                    tts?.setPitch(1.0f)
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                        }

                        override fun onDone(utteranceId: String?) {
                            _isSpeaking.value = false
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _isSpeaking.value = false
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            _isSpeaking.value = false
                        }
                    })
                    val queued = pendingUtterance
                    pendingUtterance = null
                    if (queued != null && _voiceEnabled.value) {
                        speakInternal(queued.first, queued.second)
                    }
                } else {
                    _ttsReady.value = false
                }
            }
        }
    }

    fun setVoiceEnabled(enabled: Boolean) {
        _voiceEnabled.value = enabled
        if (!enabled) {
            stop()
        }
    }

    fun toggleVoice(): Boolean {
        val next = !_voiceEnabled.value
        setVoiceEnabled(next)
        return next
    }

    fun candidateLocalesFor(language: AppLanguage): List<Locale> = when (language) {
        AppLanguage.ENGLISH -> listOf(Locale.US, Locale.ENGLISH)
        AppLanguage.BENGALI -> listOf(Locale("bn", "BD"), Locale("bn", "IN"), Locale("bn"))
        AppLanguage.HINDI -> listOf(Locale("hi", "IN"), Locale("hi"))
        AppLanguage.ARABIC -> listOf(Locale("ar", "SA"), Locale("ar"))
        AppLanguage.INDONESIAN -> listOf(Locale("id", "ID"), Locale("in", "ID"), Locale("id"))
        AppLanguage.SPANISH -> listOf(Locale("es", "ES"), Locale("es", "MX"), Locale("es"))
    }

    fun languageSelectedAnnouncement(language: AppLanguage): String = when (language) {
        AppLanguage.BENGALI ->
            "বাংলা ভাষা সিলেক্ট করা হয়েছে। ব্লাডি টিম ভয়েস গাইড এখন চালু। যেকোনো ফাংশনে ক্লিক করলে ভয়েসে বুঝিয়ে দেবে।"
        AppLanguage.HINDI ->
            "हिन्दी भाषा चुनी गई है। ब्लडी टीम वॉइस गाइड चालू है। किसी भी फ़ंक्शन पर टैप करें और उसकी जानकारी सुनें।"
        AppLanguage.ARABIC ->
            "تم اختيار اللغة العربية. الدليل الصوتي لفريق بلادي مفعل الآن. اضغط على أي ميزة لسماع شرحها."
        AppLanguage.INDONESIAN ->
            "Bahasa Indonesia dipilih. Panduan suara Bloody Team aktif. Ketuk fitur apa saja untuk mendengarkan penjelasannya."
        AppLanguage.SPANISH ->
            "Idioma español seleccionado. La guía de voz de Bloody Team está activa. Toca cualquier función para escucharla."
        AppLanguage.ENGLISH ->
            "English selected. Bloody Team voice guide is active. Tap any function to hear what it does and how it works."
    }

    fun speakLanguageGreeting(language: AppLanguage) {
        if (!_voiceEnabled.value) return
        speak(languageSelectedAnnouncement(language), language)
    }

    fun speakFeatureExplanation(explanation: FeatureExplanation, language: AppLanguage, forceSpeak: Boolean = false) {
        if (!forceSpeak && !_voiceEnabled.value) return
        val script = buildString {
            append(explanation.title.trim())
            append(". ")
            append(explanation.whatItDoes.trim())
            append(" ")
            append(explanation.howItWorks.trim())
        }
        speak(script, language, forceSpeak = forceSpeak)
    }

    fun speak(text: String, language: AppLanguage, forceSpeak: Boolean = false) {
        if (text.isBlank()) return
        if (!forceSpeak && !_voiceEnabled.value) return
        val ctx = appContext ?: return
        if (tts == null) {
            pendingUtterance = text to language
            init(ctx)
            return
        }
        if (!_ttsReady.value) {
            pendingUtterance = text to language
            return
        }
        speakInternal(text, language)
    }

    private fun speakInternal(text: String, language: AppLanguage) {
        val engine = tts ?: return
        ZeroCrashGuard.runGuarded("VoiceSpeaker.speak", Unit) {
            selectBestLocale(engine, language)
            val id = "bt_tts_${utteranceCounter.getAndIncrement()}"
            engine.stop()
            _isSpeaking.value = true
            val res = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
            if (res == TextToSpeech.ERROR) {
                _isSpeaking.value = false
            }
        }
    }

    private fun selectBestLocale(engine: TextToSpeech, language: AppLanguage) {
        val candidates = candidateLocalesFor(language)
        for (locale in candidates) {
            val availability = runCatching { engine.isLanguageAvailable(locale) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
            if (availability >= TextToSpeech.LANG_AVAILABLE) {
                engine.language = locale
                return
            }
        }
        // Try setting primary language locale anyway; if unavailable, fall back to US English
        val primary = candidates.first()
        val setPrimary = runCatching { engine.setLanguage(primary) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        if (setPrimary < TextToSpeech.LANG_AVAILABLE) {
            runCatching { engine.language = Locale.US }
        }
    }

    fun stop() {
        pendingUtterance = null
        _isSpeaking.value = false
        ZeroCrashGuard.runGuarded("VoiceSpeaker.stop", Unit) {
            tts?.stop()
        }
    }

    fun shutdown() {
        stop()
        ZeroCrashGuard.runGuarded("VoiceSpeaker.shutdown", Unit) {
            tts?.shutdown()
        }
        tts = null
        _ttsReady.value = false
    }

    private fun isRobolectricEnvironment(): Boolean {
        return Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
    }
}
