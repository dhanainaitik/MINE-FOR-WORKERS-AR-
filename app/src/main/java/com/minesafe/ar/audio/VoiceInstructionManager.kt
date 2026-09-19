package com.minesafe.ar.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class VoiceInstructionManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isReady = false
    private var currentLanguage = Locale.ENGLISH
    var isMuted: Boolean = false
    var currentLanguageCode: String = "en"
        private set

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    fun setLanguage(languageCode: String) {
        currentLanguageCode = languageCode
        currentLanguage = if (languageCode == "hi") {
            Locale.Builder().setLanguage("hi").setRegion("IN").build()
        } else {
            Locale.ENGLISH
        }
        
        if (isReady) {
            val result = tts?.setLanguage(currentLanguage)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("VoiceManager", "Language $languageCode is not supported on this device's TTS. Falling back to English.")
                tts?.setLanguage(Locale.ENGLISH)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isReady = true
            tts?.setSpeechRate(0.92f) // Clear, measured safety trainer pace
            tts?.setPitch(0.98f)
            setLanguage(currentLanguageCode)
        } else {
            Log.e("VoiceManager", "TTS Initialization Failed!")
        }
    }

    fun speak(instruction: String) {
        if (isMuted) return
        if (isReady && instruction.isNotBlank()) {
            tts?.speak(instruction, TextToSpeech.QUEUE_FLUSH, null, "MINE_SAFE_VOICE")
        }
    }

    fun stop() {
        if (isReady) {
            tts?.stop()
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
