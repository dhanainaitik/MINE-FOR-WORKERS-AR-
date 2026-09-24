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
        currentLanguage = when (languageCode) {
            "hi" -> Locale.Builder().setLanguage("hi").setRegion("IN").build()
            "sat" -> Locale.Builder().setLanguage("sat").setRegion("IN").build()
            else -> Locale.ENGLISH
        }
        
        if (isReady) {
            val result = tts?.setLanguage(currentLanguage)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                if (languageCode == "sat") {
                    Log.w("VoiceManager", "Santali TTS voice not installed on device. Falling back to Hindi voice engine for Devanagari Santali phonetics.")
                    tts?.setLanguage(Locale.Builder().setLanguage("hi").setRegion("IN").build())
                } else {
                    Log.w("VoiceManager", "Language $languageCode is not supported on this device's TTS. Falling back to English.")
                    tts?.setLanguage(Locale.ENGLISH)
                }
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

    private val hindiVoiceMap = mapOf(
        // Common / Entrance
        "Scan the floor to place the mine entrance." to "खदान का प्रवेश द्वार स्थापित करने के लिए फर्श को स्कैन करें।",
        "Physically walk toward the doorway to enter the mine." to "खदान में प्रवेश करने के लिए द्वार की ओर बढ़ें।",

        // Module 1: Electrical Fire Safety (Sequential PASS)
        "Warning. Electrical fire detected ahead. Move toward the fire extinguisher." to "चेतावनी। आगे बिजली की आग लगी है। अग्निशामक यंत्र की ओर बढ़ें।",
        "Please open the extinguisher nozzle." to "कृपया अग्निशामक का नोज़ल खोलें।",
        "Pinch your thumb and index finger to pick up the fire extinguisher." to "अग्निशामक यंत्र उठाने के लिए अपने अंगूठे और तर्जनी को मिलाएं।",
        "Extinguisher acquired! Approach the electrical fire." to "अग्निशामक प्राप्त हुआ! बिजली की आग के पास जाएं।",
        "Pull the safety pin." to "सुरक्षा पिन निकालें।",
        "Safety pin removed! Now aim the nozzle at the base of the fire." to "सुरक्षा पिन निकल गई! अब नोज़ल को आग के आधार की ओर निशाना लगाएँ।",
        "Pull the safety pin first." to "पहले सुरक्षा पिन निकालें।",
        "Aim the nozzle at the base of the fire." to "नोज़ल को आग के आधार की ओर निशाना लगाएँ।",
        "Aim correct! Now squeeze the lever." to "निशाना सही है! अब लीवर दबाएँ।",
        "Aim the nozzle at the base of the fire first." to "पहले नोज़ल को आग के आधार पर केंद्रित करें।",
        "Now squeeze the lever." to "अब लीवर दबाएँ।",
        "Keep squeezing and sweep the nozzle from side to side." to "लीवर दबाए रखें और नोज़ल को एक तरफ से दूसरी तरफ घुमाएँ।",
        "Keep squeezing and sweep side to side." to "लीवर दबाए रखें और नोज़ल को एक तरफ से दूसरी तरफ घुमाएँ।",
        "Sweep the nozzle from side to side across the base of the fire." to "नोज़ल को आग के आधार पर एक तरफ से दूसरी तरफ घुमाएँ।",
        "Keep the lever squeezed while sweeping." to "घुमाते समय लीवर को दबाकर रखें।",
        "Fire extinguished. Good job following safety procedures." to "आग बुझ गई है। सुरक्षा नियमों का पालन करने के लिए बहुत अच्छा।",
        "Fire extinguished! Excellent job following safety procedures." to "आग बुझ गई! सुरक्षा नियमों का पालन करने के लिए शाबाश।",

        // Module 2: Chemical Hazard Safety
        "Chemical hazard detected. Do not approach the leak." to "रासायनिक खतरे का पता चला है। रिसाव के पास न जाएं।",
        "Move away from the hazard zone." to "खतरे के क्षेत्र से दूर हटें।",
        "Warning! Too close to chemical hazard. Step back immediately!" to "चेतावनी! रासायनिक खतरे के बहुत करीब हैं। तुरंत पीछे हटें!",
        "Locate the emergency response equipment." to "आपातकालीन सुरक्षा उपकरण खोजें।",
        "Do not touch the leaking container. Follow the emergency procedure." to "रिसाव वाले कंटेनर को न छुएं। आपातकालीन प्रक्रिया का पालन करें।",
        "Activate the emergency isolation valve to seal the line." to "लाइन सील करने के लिए आपातकालीन आइसोलेशन वाल्व चालू करें।",
        "Hazard controlled. Emergency isolation confirmed." to "खतरा नियंत्रित हो गया है। आपातकालीन आइसोलेशन सफल।"
    )

    private val santaliVoiceMap = mapOf(
        // Common / Entrance
        "Scan the floor to place the mine entrance." to "खदान बोलोन दुआर बयसाव ला़गिद ओत स्कॅन मे।",
        "Physically walk toward the doorway to enter the mine." to "खदान भितरी बोलोन ला़गिद दुआर सेद ताड़ाम मे।",

        // Module 1: Electrical Fire Safety (Sequential PASS)
        "Warning. Electrical fire detected ahead. Move toward the fire extinguisher." to "हुसियार। माढ़ंग रे बिजली सेंगॆल लागाव आकाना। सेंगॆल इरिज सामान सेद चालाव मे।",
        "Please open the extinguisher nozzle." to "दया काते सेंगॆल इरिज नोज़ल झिज मे।",
        "Pinch your thumb and index finger to pick up the fire extinguisher." to "सेंगॆल इरिज सामान तुल ला़गिद अंगूठा आर इतुरिसी कतूब चिमटा मे।",
        "Extinguisher acquired! Approach the electrical fire." to "सेंगॆल इरिज सामान ञाम एना! बिजली सेंगॆल सूर ते चालाव मे।",
        "Pull the safety pin." to "सेफ्टी पिन ओर ओडोक मे।",
        "Safety pin removed! Now aim the nozzle at the base of the fire." to "सेफ्टी पिन ओडोक एना! नितोः नोज़ल सेंगॆल फेद सेद निशानाय मे।",
        "Pull the safety pin first." to "माढ़ंग ते सेफ्टी पिन ओर ओडोक मे।",
        "Aim the nozzle at the base of the fire." to "नोज़ल दो सेंगॆल फेद सेद निशानाय मे।",
        "Aim correct! Now squeeze the lever." to "निशाना ठीक गेया! नितोः लीवर ओताय मे।",
        "Aim the nozzle at the base of the fire first." to "माढ़ंग ते नोज़ल सेंगॆल फेद सेद निशानाय मे।",
        "Now squeeze the lever." to "नितोः लीवर ओताय मे।",
        "Keep squeezing and sweep the nozzle from side to side." to "लीवर ओता काते दोहोय मे आर नोज़ल हाना-नोवा सेद बिहुर मे।",
        "Keep squeezing and sweep side to side." to "लीवर ओता काते दोहोय मे आर हाना-नोवा सेद बिहुर मे।",
        "Sweep the nozzle from side to side across the base of the fire." to "नोज़ल दो सेंगॆल फेद रे हाना-नोवा सेद बिहुर मे।",
        "Keep the lever squeezed while sweeping." to "बिहुर जोखोः लीवर ओता काते दोहोय मे।",
        "Fire extinguished. Good job following safety procedures." to "सेंगॆल इरिज एना। सुरक्षा नियम पांजा ला़गिद आडी नापाय कामि।",
        "Fire extinguished! Excellent job following safety procedures." to "सेंगॆल इरिज एना! सुरक्षा नियम पांजा ला़गिद आडी सरेश कामि।",

        // Module 2: Chemical Hazard Safety
        "Chemical hazard detected. Do not approach the leak." to "केमिकल खतरा ञाम एना। लिक सूर ते आलोम चालावा।",
        "Move away from the hazard zone." to "खतरा जायगा खोन साहाः मे।",
        "Warning! Too close to chemical hazard. Step back immediately!" to "हुसियार! केमिकल खतरा सूर रे मेनामा। ला़गे तायोम सेद साहाः मे!",
        "Locate the emergency response equipment." to "इमरजेंसी सुरक्षा सामान पांजा ओडोक मे।",
        "Do not touch the leaking container. Follow the emergency procedure." to "लिकोः कान डब्बा आलोम जोटोदा। इमरजेंसी नियम पांजा मे।",
        "Activate the emergency isolation valve to seal the line." to "लाइन सील ला़गिद इमरजेंसी वाल्व चालुय मे।",
        "Hazard controlled. Emergency isolation confirmed." to "खतरा सम्भड़ाव एना। इमरजेंसी आइसोलेशन सफल एना।"
    )

    fun speak(instruction: String) {
        if (isMuted) return
        if (isReady && instruction.isNotBlank()) {
            val textToSpeak = when (currentLanguageCode) {
                "hi" -> hindiVoiceMap[instruction.trim()] ?: instruction
                "sat" -> santaliVoiceMap[instruction.trim()] ?: instruction
                else -> instruction
            }
            tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "MINE_SAFE_VOICE")
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
