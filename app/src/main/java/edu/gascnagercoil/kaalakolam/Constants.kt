package edu.gascnagercoil.kaalakolam

/** The one and only origin the WebView is allowed to load. Keep it constant so localStorage survives updates. */
object Constants {
    const val HOST = "appassets.androidplatform.net"
    const val ORIGIN = "https://$HOST"
    const val START_URL = "$ORIGIN/assets/www/index.html"
}
