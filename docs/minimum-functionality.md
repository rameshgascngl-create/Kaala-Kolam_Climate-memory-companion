# Minimum Functionality note

Kaala Kolam is not merely a URL packaged in a WebView. The distributable contains the full offline educational application inside the APK and adds Android-native integration deliberately kept small:

- Android 12+ SplashScreen API with the Kaala Kolam kolam motif.
- Adaptive launcher icon plus Android 13+ monochrome themed-icon layer.
- A visible native toolbar overflow menu with About and credits, Share, Text-to-speech settings and Privacy policy.
- A native About and credits screen showing app identity/version, the exact department/creator credit, offline/no-data statement and educational disclaimer.
- Native Android TextToSpeech fallback, origin-restricted with `WebViewCompat.WebMessageListener`, including Tamil-voice availability handling.
- Native Android Back handling integrated with the HTML application's Home/Learn navigation and active animation shutdown.
- Android lifecycle, system insets, external-link routing and WebView renderer-crash recovery.

The educational functions themselves — six sections, 24 Learn topics with animated narrated SVG clips, elder interviews, classroom code pooling, council simulation, prediction/calibration activities and bilingual glossary — are bundled offline and do not depend on a website or network service.
