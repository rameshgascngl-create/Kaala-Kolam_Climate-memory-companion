package edu.gascnagercoil.kaalakolam

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.google.android.material.appbar.MaterialToolbar

/**
 * Single-activity shell for the finished offline HTML application.
 *
 * The page is served only from the stable appassets HTTPS origin so DOM storage survives updates.
 * Network access is not requested and external links are handed to the operating system.
 */
class MainActivity : AppCompatActivity() {

    private var webView: WebView? = null
    private var ttsBridge: TtsBridge? = null

    private val assetLoader: WebViewAssetLoader by lazy {
        WebViewAssetLoader.Builder()
            .setDomain(Constants.HOST)
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        applySystemBars(dark = true)
        setContentView(R.layout.activity_main)
        applyInsets()
        configureToolbar()

        val wv = findViewById<WebView>(R.id.webview)
        webView = wv
        setupWebView(wv)
        installBackHandling()

        if (savedInstanceState == null || wv.url == null) {
            wv.loadUrl(Constants.START_URL)
        }
    }

    private fun applyInsets() {
        val root = findViewById<View>(R.id.root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun configureToolbar() {
        findViewById<MaterialToolbar>(R.id.toolbar).apply {
            inflateMenu(R.menu.main_menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_about -> startActivity(Intent(this@MainActivity, AboutActivity::class.java))
                    R.id.action_share -> shareApp()
                    R.id.action_tts_settings -> openTtsSettings()
                    R.id.action_privacy -> openExternalUri(Uri.parse(getString(R.string.privacy_url)))
                    else -> return@setOnMenuItemClickListener false
                }
                true
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(wv: WebView) {
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        wv.setBackgroundColor(getColor(R.color.ground))

        with(wv.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            setSupportMultipleWindows(false)
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            textZoom = 100
            setGeolocationEnabled(false)
            // mediaPlaybackRequiresUserGesture remains at its secure default: the page has no audio/video media.
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(wv.settings, false)
        }

        attachThemeBridge(wv)
        ttsBridge = TtsBridge(this, wv) { showTamilVoiceMissingDialog() }.also { it.attach() }

        wv.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val uri = request.url
                if (uri.scheme == "https" && uri.host == Constants.HOST) return false
                openExternalUri(uri)
                return true
            }

            override fun onRenderProcessGone(
                view: WebView,
                detail: RenderProcessGoneDetail
            ): Boolean {
                (view.parent as? ViewGroup)?.removeView(view)
                view.destroy()
                webView = null
                runOnUiThread { recreate() }
                return true
            }
        }

        wv.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                if (BuildConfig.DEBUG) {
                    Log.d(
                        "KaalaKolamJS",
                        "${message.messageLevel()}: ${message.message()} (${message.lineNumber()})"
                    )
                }
                return true
            }
        }
    }

    private fun attachThemeBridge(wv: WebView) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) ||
            !WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
        ) return

        val origins = setOf(Constants.ORIGIN)
        WebViewCompat.addWebMessageListener(wv, "AndroidTheme", origins) { _, message, _, _, _ ->
            val dark = message.data != "light"
            runOnUiThread { applySystemBars(dark) }
        }
        WebViewCompat.addDocumentStartJavaScript(
            wv,
            """
            (function(){
              if(typeof AndroidTheme==='undefined'||window.__kkThemeWatch)return;
              window.__kkThemeWatch=true;
              function send(){try{AndroidTheme.postMessage(document.documentElement.dataset.theme||'dark')}catch(e){}}
              new MutationObserver(send).observe(document.documentElement,{attributes:true,attributeFilter:['data-theme']});
              document.addEventListener('DOMContentLoaded',send,{once:true});
              setTimeout(send,0);
            })();
            """.trimIndent(),
            origins
        )
    }

    private fun applySystemBars(dark: Boolean) {
        val darkGround = Color.parseColor("#10263A")
        val lightGround = Color.parseColor("#EEF3F2")
        if (dark) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(darkGround),
                navigationBarStyle = SystemBarStyle.dark(darkGround)
            )
        } else {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.light(lightGround, lightGround),
                navigationBarStyle = SystemBarStyle.light(lightGround, lightGround)
            )
        }
    }

    private fun installBackHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val current = webView ?: run {
                    finish()
                    return
                }
                current.evaluateJavascript(
                    """
                    (function(){
                      try {
                        if (window.__appBack) return window.__appBack() ? 'handled' : 'exit';
                        if (window.__appIsHome && window.__appGoHome) {
                          if (!window.__appIsHome()) { window.__appGoHome(); return 'handled'; }
                          return 'exit';
                        }
                      } catch (e) {}
                      return 'exit';
                    })()
                    """.trimIndent()
                ) { result ->
                    if (result != "\"handled\"") finish()
                }
            }
        })
    }

    private fun openExternalUri(uri: Uri) {
        val intent = when (uri.scheme?.lowercase()) {
            "http", "https" -> Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
            "mailto" -> Intent(Intent.ACTION_SENDTO, uri)
            else -> return
        }
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // No suitable external application is installed; remain safely inside the app.
        }
    }

    private fun shareApp() {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text))
        }
        startActivity(Intent.createChooser(send, getString(R.string.menu_share)))
    }

    private fun openTtsSettings() {
        try {
            startActivity(Intent("com.android.settings.TTS_SETTINGS"))
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun showTamilVoiceMissingDialog() {
        if (isFinishing || isDestroyed) return
        AlertDialog.Builder(this)
            .setTitle(R.string.tts_missing_title)
            .setMessage(R.string.tts_missing_message)
            .setPositiveButton(R.string.tts_open_settings) { _, _ -> openTtsSettings() }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    override fun onPause() {
        webView?.evaluateJavascript(
            "try{window.speechSynthesis&&window.speechSynthesis.cancel()}catch(e){}",
            null
        )
        webView?.onPause()
        ttsBridge?.stop()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
    }

    override fun onDestroy() {
        ttsBridge?.shutdown()
        webView?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            it.destroy()
        }
        webView = null
        super.onDestroy()
    }
}
