package edu.gascnagercoil.kaalakolam.speech

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import java.util.Locale

object SpeechPronunciation {
    val tamilMap: LinkedHashMap<String, String> = linkedMapOf(
        "SR1.5" to "எஸ் ஆர் ஒன்று புள்ளி ஐந்து",
        "IPCC" to "ஐ பி சி சி",
        "MoES" to "எம் ஓ ஈ எஸ்",
        "NOAA" to "நோஆ",
        "WMO" to "டபிள்யூ எம் ஓ",
        "UNEP" to "யூ நெப்",
        "ENSO" to "என் சோ",
        "ITCZ" to "ஐ டி சி இசட்",
        "SPEI" to "எஸ் பி ஈ ஐ",
        "SPI" to "எஸ் பி ஐ",
        "ONI" to "ஓ என் ஐ",
        "IMD" to "ஐ எம் டி",
        "AR6" to "ஏ ஆர் ஆறு",
        "W/m²" to "வாட் ஒரு சதுர மீட்டருக்கு",
        "CO₂" to "கார்பன் டை ஆக்சைடு",
        "ppm" to "பி பி எம்",
        "°C" to "டிகிரி செல்சியஸ்",
        "µm" to "மைக்ரோமீட்டர்",
        "km" to "கிலோமீட்டர்",
    )

    fun forTamil(displayText: String): String =
        tamilMap.entries.fold(displayText) { text, (token, spoken) -> text.replace(token, spoken) }
}

class SpeechController(
    context: Context,
    private val onMissingTamilVoice: () -> Unit,
) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private var missingShown = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
    }

    fun speak(displayText: String, language: String) {
        if (!ready || displayText.isBlank()) return
        val locale = if (language == "ta") Locale("ta", "IN") else Locale("en", "IN")
        if (language == "ta" && !hasTamilVoice(locale)) {
            if (!missingShown) {
                missingShown = true
                onMissingTamilVoice()
            }
            return
        }
        tts.language = locale
        val spoken = if (language == "ta") SpeechPronunciation.forTamil(displayText) else displayText
        tts.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, "kaala-kolam")
    }

    private fun hasTamilVoice(locale: Locale): Boolean {
        val languageStatus = tts.isLanguageAvailable(locale)
        if (languageStatus < TextToSpeech.LANG_AVAILABLE) return false
        val voices = tts.voices
        return voices.isNullOrEmpty() || voices.any { it.locale.language == "ta" }
    }

    fun stop() = tts.stop()

    fun shutdown() = tts.shutdown()

    companion object {
        fun installVoiceDataIntent(): Intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
    }
}
