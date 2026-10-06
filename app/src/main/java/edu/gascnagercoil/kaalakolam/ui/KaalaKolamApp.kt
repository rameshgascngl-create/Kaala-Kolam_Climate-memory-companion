package edu.gascnagercoil.kaalakolam.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import edu.gascnagercoil.kaalakolam.R
import edu.gascnagercoil.kaalakolam.domain.AppState
import edu.gascnagercoil.kaalakolam.domain.ElderMode
import edu.gascnagercoil.kaalakolam.domain.ElderSession
import edu.gascnagercoil.kaalakolam.domain.EldersDomain
import edu.gascnagercoil.kaalakolam.domain.Interview
import edu.gascnagercoil.kaalakolam.domain.InterviewAnswer
import edu.gascnagercoil.kaalakolam.domain.PredictionState
import edu.gascnagercoil.kaalakolam.domain.ThemeMode
import edu.gascnagercoil.kaalakolam.speech.rememberSpeechController
import edu.gascnagercoil.kaalakolam.ui.theme.PrototypeTheme

private const val ABOUT_ROUTE = "about"

private enum class Destination(
    val route: String,
    @StringRes val label: Int,
    val icon: String,
    val en: String,
    val ta: String,
) {
    HOME("home", R.string.nav_home, "home", "Home", "முகப்பு"),
    LEARN("learn", R.string.nav_learn, "learn", "Learn", "கற்க"),
    ELDERS("elders", R.string.nav_elders, "elder", "Elders", "மூத்தோர்"),
    CLASS("class", R.string.nav_class, "pool", "Class", "வகுப்பு"),
    COUNCIL("council", R.string.nav_council, "council", "Council", "ஊர்சபை"),
    PREDICT("predict", R.string.nav_predict, "predict", "Predict", "கணிப்பு"),
}

private fun localized(lang: String, en: String, ta: String) = if (lang == "ta") ta else en

@Suppress("UNUSED_PARAMETER")
@Composable
fun KaalaKolamApp(
    uiState: AppUiState,
    widthSizeClass: WindowWidthSizeClass,
    onLanguageChange: (String) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onCurrentTabChange: (String) -> Unit = {},
    onBackup: (AppState) -> String,
    onValidateBackup: (String) -> AppState?,
    onRestore: (AppState) -> Unit,
    onReset: () -> Unit,
    onPredictionAnswer: (String, PredictionState) -> Unit = { _, _ -> },
    onClearPredictions: () -> Unit = {},
    onElderSessionChange: (ElderSession) -> Unit = {},
    onCreateElderInterview: (String, Int, Int, String) -> Unit = { _, _, _, _ -> },
    onDeleteElderInterview: (String) -> Unit = {},
    onElderAnswerChange: (String, String, InterviewAnswer) -> Unit = { _, _, _ -> },
    onAdvanceElderQuestion: (String, String, InterviewAnswer?, ElderSession) -> Unit =
        { _, _, _, _ -> },
    onElderReactionChange: (String, String) -> Unit = { _, _ -> },
    initialRoute: String = Destination.HOME.route,
) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route ?: Destination.HOME.route
    val lang = uiState.appState.language
    // Owns TTS lifecycle and the once-per-session missing-Tamil-voice dialog.
    rememberSpeechController()
    var zoom by remember { mutableIntStateOf(0) }
    val baseDensity = LocalDensity.current
    val fontScale = listOf(1f, 1.2f, 1.42f)[zoom]

    fun navigatePrimary(destination: Destination) {
        onCurrentTabChange(destination.route)
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(uiState.appState.currentTab, initialRoute) {
        if (initialRoute == Destination.HOME.route) {
            val restored = Destination.entries.firstOrNull {
                it.route == uiState.appState.currentTab
            }
            val visibleRoute = navController.currentDestination?.route
            if (
                restored != null &&
                visibleRoute != restored.route &&
                visibleRoute != ABOUT_ROUTE
            ) {
                navController.navigate(restored.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    BackHandler(enabled = currentRoute != Destination.HOME.route) {
        if (currentRoute == ABOUT_ROUTE) {
            navController.popBackStack()
        } else {
            onCurrentTabChange(Destination.HOME.route)
            navController.navigate(Destination.HOME.route) {
                popUpTo(Destination.HOME.route) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    CompositionLocalProvider(
        LocalDensity provides Density(baseDensity.density, baseDensity.fontScale * fontScale),
    ) {
        Scaffold(
            modifier = Modifier.semantics { testTagsAsResourceId = true },
            containerColor = PrototypeTheme.palette.ground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                PrototypeTopBar(
                    lang = lang,
                    zoom = zoom,
                    onZoom = { zoom = (zoom + 1) % 3 },
                    onLanguage = { onLanguageChange(if (lang == "ta") "en" else "ta") },
                    onTheme = {
                        onThemeChange(
                            if (uiState.appState.themeMode == ThemeMode.DARK) {
                                ThemeMode.LIGHT
                            } else {
                                ThemeMode.DARK
                            },
                        )
                    },
                    onAbout = { navController.navigate(ABOUT_ROUTE) },
                )
            },
            bottomBar = {
                PrototypeTabs(
                    lang = lang,
                    currentRoute = currentRoute,
                    onNavigate = ::navigatePrimary,
                )
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = initialRoute,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                composable(Destination.HOME.route) {
                    HomeScreen(
                        appState = uiState.appState,
                        onNavigate = { route ->
                            Destination.entries.firstOrNull { it.route == route }?.let(::navigatePrimary)
                        },
                    )
                }
                composable(Destination.LEARN.route) {
                    LearnPrototypeScreen(lang = lang)
                }
                composable(Destination.ELDERS.route) {
                    ElderPrototypeScreen(
                        lang = lang,
                        appState = uiState.appState,
                        onSessionChange = onElderSessionChange,
                        onCreateInterview = onCreateElderInterview,
                        onDeleteInterview = onDeleteElderInterview,
                        onAnswerChange = onElderAnswerChange,
                        onAdvance = onAdvanceElderQuestion,
                        onReactionChange = onElderReactionChange,
                    )
                }
                composable(Destination.CLASS.route) {
                    ClassPoolPrototypeScreen(lang = lang)
                }
                composable(Destination.COUNCIL.route) {
                    CouncilPrototypeScreen(lang = lang)
                }
                composable(Destination.PREDICT.route) {
                    PredictPrototypeScreen(
                        lang = lang,
                        answers = uiState.appState.predictionAnswers,
                        onAnswer = onPredictionAnswer,
                        onReset = onClearPredictions,
                    )
                }
                composable(ABOUT_ROUTE) {
                    AboutScreen(
                        appState = uiState.appState,
                        onBackup = onBackup,
                        onValidateBackup = onValidateBackup,
                        onRestore = onRestore,
                        onReset = onReset,
                    )
                }
            }
        }
    }
}

/**
 * Navigation-free visual harness used by the JVM renderer.
 *
 * It intentionally shares the exact production chrome and screen composables
 * while avoiding NavHost owners that do not exist in layoutlib/Paparazzi.
 * This is not a mock of the visuals: only navigation behaviour is bypassed.
 */
@Composable
fun KaalaKolamVisualFixture(
    uiState: AppUiState,
    visualState: String,
    fidelityRecorder: FidelityRecorder? = null,
) {
    val lang = uiState.appState.language
    val visualInterview = Interview(
        id = "visualelder",
        nickname = if (lang == "ta") "பாட்டி" else "Paati",
        birthDecade = 1,
        place = 0,
        createdAt = 1L,
    )
    val visualAppState = when (visualState) {
        "elders-list", "elders-delete" -> uiState.appState.copy(
            interviews = listOf(visualInterview),
            elderSession = ElderSession(),
        )
        "elders-setup" -> uiState.appState.copy(
            elderSession = ElderSession(mode = ElderMode.SETUP),
        )
        "elders-limit" -> uiState.appState.copy(
            interviews = List(EldersDomain.MAX_INTERVIEWS) { index ->
                Interview(
                    id = "visual$index",
                    nickname = if (lang == "ta") "மூத்தவர் ${index + 1}" else "Elder ${index + 1}",
                    birthDecade = index % 6,
                    place = index % 5,
                    createdAt = index.toLong(),
                )
            },
            elderSession = ElderSession(),
        )
        else -> uiState.appState
    }
    val primaryRoute = when {
        visualState in setOf("learn-topic", "learn-deep", "learn-game", "learn-words") ->
            Destination.LEARN.route
        visualState.startsWith("elders-") -> Destination.ELDERS.route
        else -> visualState
    }
    CompositionLocalProvider(LocalFidelityRecorder provides fidelityRecorder) {
        Scaffold(
            containerColor = PrototypeTheme.palette.ground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
            PrototypeTopBar(
                lang = lang,
                zoom = 0,
                onZoom = {},
                onLanguage = {},
                onTheme = {},
                onAbout = {},
            )
        },
        bottomBar = {
            PrototypeTabs(
                lang = lang,
                currentRoute = primaryRoute,
                onNavigate = {},
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (visualState) {
                Destination.HOME.route -> HomeScreen(
                    appState = uiState.appState,
                    onNavigate = {},
                )
                Destination.LEARN.route -> LearnPrototypeScreen(lang = lang, initialMode = "home")
                "learn-topic" -> LearnPrototypeScreen(lang = lang, initialMode = "topic")
                "learn-deep" -> LearnPrototypeScreen(lang = lang, initialMode = "deep")
                "learn-game" -> LearnPrototypeScreen(lang = lang, initialMode = "game")
                "learn-words" -> LearnPrototypeScreen(lang = lang, initialMode = "words")
                Destination.ELDERS.route, "elders-empty", "elders-list", "elders-setup",
                "elders-delete", "elders-limit" -> ElderPrototypeScreen(
                    lang = lang,
                    appState = visualAppState,
                    initialDeletePendingId = if (visualState == "elders-delete") visualInterview.id else null,
                )
                Destination.CLASS.route -> ClassPoolPrototypeScreen(lang = lang)
                Destination.COUNCIL.route -> CouncilPrototypeScreen(lang = lang)
                Destination.PREDICT.route -> PredictPrototypeScreen(
                    lang = lang,
                    answers = uiState.appState.predictionAnswers,
                )
                else -> error("Unknown visual state: $visualState")
            }
        }
        }
    }
}

@Composable
private fun PrototypeTopBar(
    lang: String,
    zoom: Int,
    onZoom: () -> Unit,
    onLanguage: () -> Unit,
    onTheme: () -> Unit,
    onAbout: () -> Unit,
) {
    val p = PrototypeTheme.palette
    val compact = LocalConfiguration.current.screenWidthDp <= 480
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fidelityTag("chrome.topbar")
            .background(p.ground2)
            .drawBehind {
                drawLine(
                    color = p.line,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height - 1f),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height - 1f),
                    strokeWidth = 1f,
                )
            }
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FidelityTouchTarget(
            tag = "chrome.brand",
            modifier = Modifier.weight(1f),
            onClick = onAbout,
        ) {
            Column(modifier = Modifier.fidelityTag("chrome.brand")) {
                val brandText = localized(lang, "Kaala Kolam", "காலக்கோலம்")
                Text(
                    brandText,
                    color = p.flour,
                    fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.4.sp,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.fidelityTag("chrome.brand-label"),
                    onTextLayout = fidelityTextLayout(
                        tag = "chrome.brand-label",
                        kind = "label",
                        text = brandText,
                    ),
                )
                if (!compact) {
                    Text(
                        localized(
                            lang,
                            "Ask your elders. Test the evidence. Decide together.",
                            "மூத்தோரிடம் கேளுங்கள். சான்றைச் சோதியுங்கள். இணைந்து முடிவெடுங்கள்.",
                        ),
                        color = p.faint,
                        fontSize = 11.84.sp,
                    )
                }
            }
        }
        HeaderTool(
            tag = "tool.text-size",
            text = listOf("Aa", "Aa+", "Aa++")[zoom],
            contentDescription = stringResource(R.string.access_text_size),
            onClick = onZoom,
        )
        HeaderTool(
            tag = "tool.language",
            text = if (lang == "ta") "EN" else "தமிழ்",
            contentDescription = stringResource(
                if (lang == "ta") R.string.access_switch_english else R.string.access_switch_tamil,
            ),
            onClick = onLanguage,
        )
        HeaderTool(
            tag = "tool.theme",
            text = "◐",
            contentDescription = stringResource(R.string.access_toggle_theme),
            onClick = onTheme,
        )
    }
}

@Composable
private fun HeaderTool(
    tag: String,
    text: String,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val p = PrototypeTheme.palette
    FidelityTouchTarget(
        tag = tag,
        modifier = Modifier.semantics { this.contentDescription = contentDescription },
        onClick = onClick,
    ) {
        Surface(
            modifier = Modifier
                .fidelityTag(tag)
                .height(44.dp)
                .widthIn(min = 44.dp),
            shape = RoundedCornerShape(12.dp),
            color = p.ground2,
            border = BorderStroke(1.dp, p.line),
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = text,
                    color = p.flour,
                    onTextLayout = fidelityTextLayout(
                        tag = "$tag.label",
                        kind = "button",
                        text = text,
                    ),
                )
            }
        }
    }
}

@Composable
private fun PrototypeTabs(
    lang: String,
    currentRoute: String,
    onNavigate: (Destination) -> Unit,
) {
    val p = PrototypeTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fidelityTag("chrome.tabbar")
            .background(p.ground2)
            .drawBehind {
                drawLine(
                    color = p.line,
                    start = androidx.compose.ui.geometry.Offset.Zero,
                    end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    strokeWidth = 1f,
                )
            }
            .navigationBarsPadding()
            .padding(6.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Destination.entries.forEach { destination ->
            val selected = currentRoute == destination.route
            FidelityTouchTarget(
                tag = "tab.${destination.route}",
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(destination) },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .fidelityTag("tab.${destination.route}")
                        .background(
                            if (selected) p.ground3 else Color.Transparent,
                            RoundedCornerShape(12.dp),
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                ) {
                    Icon(
                        imageVector = PrototypeIcons.get(destination.icon),
                        contentDescription = localized(lang, destination.en, destination.ta),
                        tint = if (selected) p.turmeric else p.faint,
                        modifier = Modifier.size(24.dp),
                    )
                    PrototypeTabLabel(
                        tag = "tab-label.${destination.route}",
                        text = localized(lang, destination.en, destination.ta),
                        color = if (selected) p.turmeric else p.faint,
                    )
                }
            }
        }
    }
}

@Composable
internal fun PrototypeTabLabel(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    tag: String = "tab-label",
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    val isTamil = text.any { it.code in 0x0B80..0x0BFF }
    val viewportWidthDp = LocalConfiguration.current.screenWidthDp
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val visualFontSizeSp = when {
        isTamil && viewportWidthDp <= 360 -> 8.5f
        isTamil -> 10.5f
        else -> 12f
    }
    val visualLineHeightSp = visualFontSizeSp * 1.55f
    val fittedStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = (visualFontSizeSp / fontScale).sp,
        lineHeight = (visualLineHeightSp / fontScale).sp,
    )
    Text(
        text = text,
        color = color,
        style = fittedStyle,
        maxLines = 1,
        softWrap = false,
        textAlign = TextAlign.Center,
        modifier = Modifier.fidelityTag(tag),
        onTextLayout = fidelityTextLayout(
            tag = tag,
            kind = "label",
            text = text,
            delegate = onTextLayout,
        ),
    )
}
