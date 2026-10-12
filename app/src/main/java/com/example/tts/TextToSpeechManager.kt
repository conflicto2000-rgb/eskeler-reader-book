package com.example.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val TAG = "TTSManager"
    private var tts: TextToSpeech? = null

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<Voice>>(emptyList())
    val availableVoices: StateFlow<List<Voice>> = _availableVoices.asStateFlow()

    private val _selectedVoice = MutableStateFlow<Voice?>(null)
    val selectedVoice: StateFlow<Voice?> = _selectedVoice.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _speechPitch = MutableStateFlow(1.0f)
    val speechPitch: StateFlow<Float> = _speechPitch.asStateFlow()

    var onParagraphCompleted: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("es", "ES"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }

            tts?.let { engine ->
                try {
                    val voices = engine.voices?.toList() ?: emptyList()
                    val spanishOrLatinVoices = voices.filter {
                        val lang = it.locale.language
                        lang == "es" || lang == "en" || lang == "fr" || lang == "de"
                    }
                    _availableVoices.value = spanishOrLatinVoices.ifEmpty { voices }
                    _selectedVoice.value = engine.voice ?: spanishOrLatinVoices.firstOrNull()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load voices: ${e.message}")
                }
            }

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isPlaying.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isPlaying.value = false
                    onParagraphCompleted?.invoke()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isPlaying.value = false
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isPlaying.value = false
                }
            })

            _isInitialized.value = true
        }
    }

    fun speak(text: String, utteranceId: String = "paragraph_${System.currentTimeMillis()}") {
        if (!_isInitialized.value || text.isBlank()) return
        tts?.setSpeechRate(_speechRate.value)
        tts?.setPitch(_speechPitch.value)
        _selectedVoice.value?.let { tts?.voice = it }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
        _isPlaying.value = false
    }

    fun setSpeechRate(rate: Float) {
        _speechRate.value = rate.coerceIn(0.5f, 2.5f)
        tts?.setSpeechRate(_speechRate.value)
    }

    fun setSpeechPitch(pitch: Float) {
        _speechPitch.value = pitch.coerceIn(0.5f, 2.0f)
        tts?.setPitch(_speechPitch.value)
    }

    fun setVoice(voice: Voice) {
        _selectedVoice.value = voice
        tts?.voice = voice
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
