package edu.gascnagercoil.kaalakolam

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.webkit.WebView
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject
import java.util.Locale

/**
 * Origin-restricted Android TextToSpeech fallback for the page's read-aloud controls.
 *
 * WebMessageListener is used instead of addJavascriptInterface because AndroidX WebKit can bind
 * the bridge explicitly to the appassets origin. The page-side polyfill activates only when the
 * WebView speech API is absent or reports no voices.
 */
class TtsBridge(
    private val activity: Activity,
    private val webView: WebView,
    private val onTamilMissing: () -> Unit
) {
    private val main = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var ready = false
    private var queued: Runnable? = null
    private var queuedId: String? = null
    private var reply: JavaScriptReplyProxy? = null
    private var tamilWarned = false

    private val messageListener = WebViewCompat.WebMessageListener {
            _: WebView, message: WebMessageCompat, _, _: Boolean, proxy: JavaScriptReplyProxy ->
        reply = proxy
        val raw = message.data ?: return@WebMessageListener
        try {
            val item = JSONObject(raw)
            when (item.optString("op")) {
                "speak" -> speak(
                    item.optString("id"),
                    item.optString("text"),
                    item.optString("lang", "en-IN"),
                    item.optDouble("rate", 1.0).toFloat()
                )
                "stop" -> stop()
            }
        } catch (_: Exception) {
            // Ignore malformed messages from the page; the origin restriction remains the boundary.
        }
    }

    private val utteranceListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = emit(utteranceId, "start", null)
        override fun onDone(utteranceId: String?) = emit(utteranceId, "end", null)
        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) = emit(utteranceId, "error", "synthesis-failed")
        override fun onError(utteranceId: String?, errorCode: Int) =
            emit(utteranceId, "error", "synthesis-failed")
        override fun onStop(utteranceId: String?, interrupted: Boolean) = Unit
    }

    fun attach() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) ||
            !WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
        ) return

        val polyfill = activity.assets.open("tts_polyfill.js").bufferedReader().use { it.readText() }
        val origins = setOf(Constants.ORIGIN)
        WebViewCompat.addWebMessageListener(webView, "AndroidTTS", origins, messageListener)
        WebViewCompat.addDocumentStartJavaScript(webView, polyfill, origins)
    }

    private fun ensureEngine() {
        if (engine != null) return
        engine = TextToSpeech(activity.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                engine?.setOnUtteranceProgressListener(utteranceListener)
                queued?.run()
            } else {
                ready = false
                emit(queuedId, "error", "engine-unavailable")
            }
            queued = null
            queuedId = null
        }
    }

    private fun speak(id: String, text: String, langTag: String, rate: Float) {
        if (text.isBlank()) {
            emit(id, "error", "blank-text")
            return
        }
        ensureEngine()
        val job = Runnable { doSpeak(id, text, langTag, rate) }
        if (ready) job.run() else {
            queued = job
            queuedId = id
        }
    }

    private fun doSpeak(id: String, text: String, langTag: String, rate: Float) {
        val currentEngine = engine ?: return
        val locale = Locale.forLanguageTag(if (langTag.startsWith("ta")) "ta-IN" else "en-IN")
        val languageResult = currentEngine.setLanguage(locale)
        val offlineVoice = currentEngine.voices
            ?.filter { voice ->
                voice.locale.language == locale.language && !voice.isNetworkConnectionRequired
            }
            ?.minByOrNull { voice -> if (voice.locale.country == locale.country) 0 else 1 }

        if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
            languageResult == TextToSpeech.LANG_NOT_SUPPORTED ||
            offlineVoice == null
        ) {
            emit(id, "error", "language-unavailable")
            if (locale.language == "ta" && !tamilWarned) {
                tamilWarned = true
                main.post { onTamilMissing() }
            }
            return
        }

        currentEngine.voice = offlineVoice
        currentEngine.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
        val limit = (TextToSpeech.getMaxSpeechInputLength() - 1).coerceAtLeast(100)
        currentEngine.speak(text.take(limit), TextToSpeech.QUEUE_FLUSH, Bundle(), id)
    }

    fun stop() {
        queued = null
        queuedId = null
        engine?.stop()
    }

    fun shutdown() {
        stop()
        engine?.shutdown()
        engine = null
        ready = false
    }

    private fun emit(id: String?, event: String, reason: String?) {
        if (id.isNullOrEmpty()) return
        val payload = JSONObject()
            .put("id", id)
            .put("ev", event)
            .put("reason", reason ?: "")
            .toString()
        main.post { reply?.postMessage(payload) }
    }
}
