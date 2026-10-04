package edu.gascnagercoil.kaalakolam

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.google.android.material.appbar.MaterialToolbar

/**
 * Native shell for the finished offline HTML application.
 *
 * The page is served only from the stable appassets HTTPS origin so DOM storage survives updates.
 * The shell requests no network permission; external links are handed to the operating system.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var rootView: View
    private lateinit var toolbar: MaterialToolbar
    private var webView: WebView? = null
    private var ttsBridge: TtsBridge? = null
    private var darkPageTheme = true

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
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        rootView = findViewById(R.id.root)
        toolbar = findViewById(R.id.toolbar)
        applyInsets()
        configureToolbar()
        applyChromeTheme(dark = true)

        val wv = findViewById<WebView>(R.id.webview)
        webView = wv
        setupWebView(wv)
        installBackHandling()

        if (savedInstanceState == null || wv.url == null) {
            wv.loadUrl(Constants.START_URL)
        }
    }

    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun configureToolbar() {
        toolbar.visibility = if (BuildConfig.SHOW_NATIVE_BAR) View.VISIBLE else View.GONE
        refreshToolbarMenu()
    }

    private fun refreshToolbarMenu() {
        toolbar.menu.clear()
        toolbar.inflateMenu(R.menu.main_menu)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_about -> startActivity(Intent(this, AboutActivity::class.java))
                R.id.action_share -> shareApp()
                R.id.action_tts_settings -> openTtsSettings()
                R.id.action_privacy -> openExternalUri(Uri.parse(getString(R.string.privacy_url)))
                else -> return@setOnMenuItemClickListener false
            }
            true
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(wv: WebView) {
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        wv.setBackgroundColor(Color.parseColor("#10263A"))

        with(wv.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            setSupportMultipleWindows(false)
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            textZoom = 100
            setGeolocationEnabled(false)
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(wv.settings, false)
        }

        attachPageStateBridges(wv)
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

    /**
     * Theme and locale messages are accepted only from the stable appassets origin.
     * No JavaScript interface is exposed to arbitrary pages.
     */
    private fun attachPageStateBridges(wv: WebView) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) ||
            !WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
        ) return

        val origins = setOf(Constants.ORIGIN)
        WebViewCompat.addWebMessageListener(wv, "AndroidTheme", origins) { _, message, _, _, _ ->
            val dark = message.data != "light"
            runOnUiThread { applyChromeTheme(dark) }
        }
        WebViewCompat.addWebMessageListener(wv, "AndroidLocale", origins) { _, message, _, _, _ ->
            val language = if (message.data == "ta") "ta" else "en"
            runOnUiThread { applyAppLocale(language) }
        }
        WebViewCompat.addDocumentStartJavaScript(
            wv,
            """
            (function(){
              if(window.__kkNativeStateWatch)return;
              window.__kkNativeStateWatch=true;
              function sendTheme(){try{if(typeof AndroidTheme!=='undefined')AndroidTheme.postMessage(document.documentElement.dataset.theme||'dark')}catch(e){}}
              function sendLocale(){try{if(typeof AndroidLocale!=='undefined')AndroidLocale.postMessage(document.documentElement.lang==='ta'?'ta':'en')}catch(e){}}
              function send(){sendTheme();sendLocale()}
              new MutationObserver(send).observe(document.documentElement,{attributes:true,attributeFilter:['data-theme','lang']});
              document.addEventListener('DOMContentLoaded',send,{once:true});
              setTimeout(send,0);
            })();
            """.trimIndent(),
            origins
        )
    }

    private fun applyAppLocale(language: String) {
        val target = if (language == "ta") "ta" else "en"
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() == target) return
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(target))
        toolbar.post { refreshToolbarMenu() }
    }

    private fun applyChromeTheme(dark: Boolean) {
        darkPageTheme = dark
        val darkGround = Color.parseColor("#10263A")
        val darkToolbar = Color.parseColor("#16334B")
        val lightGround = Color.parseColor("#EEF3F2")
        val darkText = Color.parseColor("#F4EFE6")
        val lightText = Color.parseColor("#10263A")
        val ground = if (dark) darkGround else lightGround
        val toolbarColour = if (dark) darkToolbar else lightGround
        val foreground = if (dark) darkText else lightText

        rootView.setBackgroundColor(ground)
        webView?.setBackgroundColor(ground)
        toolbar.setBackgroundColor(toolbarColour)
        toolbar.setTitleTextColor(foreground)
        toolbar.navigationIcon?.setTint(foreground)
        toolbar.overflowIcon?.setTint(foreground)

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        } else {
            window.statusBarColor = ground
            // Android 7.0/7.1 has no dark navigation-bar icons. Keep a dark solid bar in
            // light mode on API 24-25 so the system's light buttons remain visible.
            window.navigationBarColor = if (!dark && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                darkGround
            } else {
                ground
            }
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshToolbarMenu()
        applyChromeTheme(darkPageTheme)
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
