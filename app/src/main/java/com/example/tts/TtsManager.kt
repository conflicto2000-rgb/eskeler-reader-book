package com.example.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class TtsVoiceInfo(
    val id: String,
    val name: String,
    val displayName: String,
    val locale: Locale,
    val isNetworkConnectionRequired: Boolean,
    val isSpanish: Boolean,
    val genderGuess: String
)

class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val TAG = "TtsManager"
    private var tts: TextToSpeech? = null

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<TtsVoiceInfo>>(emptyList())
    val availableVoices: StateFlow<List<TtsVoiceInfo>> = _availableVoices.asStateFlow()

    private val _selectedVoice = MutableStateFlow<TtsVoiceInfo?>(null)
    val selectedVoice: StateFlow<TtsVoiceInfo?> = _selectedVoice.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _speechPitch = MutableStateFlow(1.0f)
    val speechPitch: StateFlow<Float> = _speechPitch.asStateFlow()

    var onParagraphCompleted: (() -> Unit)? = null
    var onParagraphStarted: ((String) -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engine = tts ?: return
            val spanishLocale = Locale("es", "ES")
            val langResult = engine.setLanguage(spanishLocale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.language = Locale.getDefault()
            }

            engine.setSpeechRate(_speechRate.value)
            engine.setPitch(_speechPitch.value)

            // Setup utterance listener
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isPlaying.value = true
                    utteranceId?.let { onParagraphStarted?.invoke(it) }
                }

                override fun onDone(utteranceId: String?) {
                    if (utteranceId?.startsWith("sample_") == true) {
                        _isPlaying.value = false
                        return
                    }
                    // Inform sequence that paragraph completed
                    onParagraphCompleted?.invoke()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    Log.e(TAG, "TTS Utterance error on: $utteranceId")
                    _isPlaying.value = false
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    Log.e(TAG, "TTS Utterance error: $errorCode on $utteranceId")
                    _isPlaying.value = false
                }
            })

            loadAvailableVoices()
            _isInitialized.value = true
        } else {
            Log.e(TAG, "Failed to initialize TextToSpeech: $status")
        }
    }

    private fun loadAvailableVoices() {
        val engine = tts ?: return
        val list = mutableListOf<TtsVoiceInfo>()
        try {
            val systemVoices = engine.voices ?: emptySet()
            for (v in systemVoices) {
                val loc = v.locale
                val isEs = loc.language.equals("es", ignoreCase = true)
                val nameLower = v.name.lowercase()
                val gender = when {
                    nameLower.contains("female") || nameLower.contains("fem") || nameLower.contains("-f-") || nameLower.contains("mujer") -> "Femenina"
                    nameLower.contains("male") || nameLower.contains("-m-") || nameLower.contains("hombre") -> "Masculina"
                    else -> "Voz Natural"
                }

                val displayName = buildVoiceDisplayName(loc, v.name, gender)
                list.add(
                    TtsVoiceInfo(
                        id = v.name,
                        name = v.name,
                        displayName = displayName,
                        locale = loc,
                        isNetworkConnectionRequired = v.isNetworkConnectionRequired,
                        isSpanish = isEs,
                        genderGuess = gender
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying TTS voices: ${e.message}")
        }

        // Sort: Spanish voices first, then alphabetical
        val sorted = list.sortedWith(
            compareByDescending<TtsVoiceInfo> { it.isSpanish }
                .thenBy { it.displayName }
        )
        _availableVoices.value = sorted

        // Select primary Spanish voice by default
        val defaultEs = sorted.firstOrNull { it.isSpanish } ?: sorted.firstOrNull()
        defaultEs?.let { setVoice(it) }
    }

    private fun buildVoiceDisplayName(locale: Locale, rawName: String, gender: String): String {
        val country = locale.displayCountry.ifBlank { locale.country }
        val lang = locale.displayLanguage
        val shortName = rawName.substringAfterLast("-").take(8)
        return "$lang ($country) - $gender [$shortName]"
    }

    fun setVoice(voiceInfo: TtsVoiceInfo) {
        _selectedVoice.value = voiceInfo
        val engine = tts ?: return
        try {
            val targetVoice = engine.voices?.find { it.name == voiceInfo.name }
            if (targetVoice != null) {
                engine.voice = targetVoice
            } else {
                engine.language = voiceInfo.locale
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting voice: ${e.message}")
        }
    }

    fun setSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(0.5f, 3.0f)
        _speechRate.value = clamped
        tts?.setSpeechRate(clamped)
    }

    fun setPitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.5f, 2.0f)
        _speechPitch.value = clamped
        tts?.setPitch(clamped)
    }

    fun speakParagraph(text: String, utteranceId: String) {
        if (!_isInitialized.value) return
        val engine = tts ?: return
        val params = Bundle()
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        _isPlaying.value = true
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun playSampleVoice(voiceInfo: TtsVoiceInfo) {
        val engine = tts ?: return
        setVoice(voiceInfo)
        val samplePhrase = when (voiceInfo.locale.language) {
            "es" -> "Hola, esta es una muestra de mi voz en español para tus audiolibros."
            "en" -> "Hello, this is a sample of my voice for your audiobooks."
            else -> "Hola, esta es una muestra de audio."
        }
        val params = Bundle()
        engine.speak(samplePhrase, TextToSpeech.QUEUE_FLUSH, params, "sample_${voiceInfo.id}")
    }

    fun stop() {
        tts?.stop()
        _isPlaying.value = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
