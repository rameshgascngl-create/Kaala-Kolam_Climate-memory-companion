package edu.gascnagercoil.kaalakolam.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import edu.gascnagercoil.kaalakolam.R
import edu.gascnagercoil.kaalakolam.domain.AppState
import edu.gascnagercoil.kaalakolam.domain.ThemeMode

private const val ABOUT_ROUTE = "about"

private enum class Destination(
    val route: String,
    @StringRes val label: Int,
    val mark: String,
) {
    HOME("home", R.string.nav_home, "H"),
    LEARN("learn", R.string.nav_learn, "L"),
    ELDERS("elders", R.string.nav_elders, "E"),
    CLASS("class", R.string.nav_class, "C"),
    COUNCIL("council", R.string.nav_council, "U"),
    PREDICT("predict", R.string.nav_predict, "P"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KaalaKolamApp(
    uiState: AppUiState,
    widthSizeClass: WindowWidthSizeClass,
    onLanguageChange: (String) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onBackup: (AppState) -> String,
    onValidateBackup: (String) -> AppState?,
    onRestore: (AppState) -> Unit,
    onReset: () -> Unit,
) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route ?: Destination.HOME.route
    val useRail = widthSizeClass != WindowWidthSizeClass.Compact
    var overflowOpen by remember { mutableStateOf(false) }

    fun navigatePrimary(destination: Destination) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BackHandler(enabled = currentRoute != Destination.HOME.route) {
        if (currentRoute == ABOUT_ROUTE) {
            navController.popBackStack()
        } else {
            navController.navigate(Destination.HOME.route) {
                popUpTo(Destination.HOME.route) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), maxLines = 1) },
                actions = {
                    TextButton(
                        onClick = {
                            onLanguageChange(
                                if (uiState.appState.language == "ta") "en" else "ta",
                            )
                        },
                    ) {
                        Text(stringResource(R.string.language_toggle))
                    }
                    TextButton(onClick = { overflowOpen = true }) {
                        Text("⋮")
                    }
                    DropdownMenu(
                        expanded = overflowOpen,
                        onDismissRequest = { overflowOpen = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.theme_system)) },
                            onClick = {
                                overflowOpen = false
                                onThemeChange(ThemeMode.SYSTEM)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.theme_light)) },
                            onClick = {
                                overflowOpen = false
                                onThemeChange(ThemeMode.LIGHT)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.theme_dark)) },
                            onClick = {
                                overflowOpen = false
                                onThemeChange(ThemeMode.DARK)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.about)) },
                            onClick = {
                                overflowOpen = false
                                navController.navigate(ABOUT_ROUTE)
                            },
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (!useRail) {
                CompactNavigation(
                    currentRoute = currentRoute,
                    onNavigate = ::navigatePrimary,
                )
            }
        },
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (useRail) {
                NavigationRail {
                    Destination.entries.forEach { destination ->
                        NavigationRailItem(
                            selected = currentRoute == destination.route,
                            onClick = { navigatePrimary(destination) },
                            icon = { Text(destination.mark) },
                            label = { Text(stringResource(destination.label)) },
                        )
                    }
                }
            }

            NavHost(
                navController = navController,
                startDestination = Destination.HOME.route,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(Destination.HOME.route) {
                    HomeScreen()
                }
                composable(Destination.LEARN.route) {
                    GapListScreen(
                        title = stringResource(R.string.nav_learn),
                        manifest = uiState.gaps,
                        kind = "deepDive",
                    )
                }
                composable(Destination.ELDERS.route) {
                    GapListScreen(
                        title = stringResource(R.string.nav_elders),
                        manifest = uiState.gaps,
                        kind = "eldersCrossCheck",
                    )
                }
                composable(Destination.CLASS.route) {
                    PlaceholderScreen(title = stringResource(R.string.nav_class))
                }
                composable(Destination.COUNCIL.route) {
                    GapListScreen(
                        title = stringResource(R.string.nav_council),
                        manifest = uiState.gaps,
                        kind = "councilDescription",
                    )
                }
                composable(Destination.PREDICT.route) {
                    GapListScreen(
                        title = stringResource(R.string.nav_predict),
                        manifest = uiState.gaps,
                        kind = "predictExplanation",
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

@Composable
private fun CompactNavigation(
    currentRoute: String,
    onNavigate: (Destination) -> Unit,
) {
    var moreOpen by remember { mutableStateOf(false) }
    val direct = listOf(
        Destination.HOME,
        Destination.LEARN,
        Destination.ELDERS,
        Destination.CLASS,
    )
    val moreRoutes = setOf(Destination.COUNCIL.route, Destination.PREDICT.route)

    NavigationBar {
        direct.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { onNavigate(destination) },
                icon = { Text(destination.mark) },
                label = { Text(stringResource(destination.label), maxLines = 1) },
            )
        }
        NavigationBarItem(
            selected = currentRoute in moreRoutes,
            onClick = { moreOpen = true },
            icon = { Text("…") },
            label = { Text(stringResource(R.string.nav_more), maxLines = 1) },
        )
        DropdownMenu(
            expanded = moreOpen,
            onDismissRequest = { moreOpen = false },
        ) {
            listOf(Destination.COUNCIL, Destination.PREDICT).forEach { destination ->
                DropdownMenuItem(
                    text = { Text(stringResource(destination.label)) },
                    onClick = {
                        moreOpen = false
                        onNavigate(destination)
                    },
                )
            }
        }
    }
}
