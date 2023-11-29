package com.kaajjo.libresudoku

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.kaajjo.libresudoku.core.PreferencesConstants
import com.kaajjo.libresudoku.data.datastore.AppSettingsManager
import com.kaajjo.libresudoku.data.datastore.ThemeSettingsManager
import com.kaajjo.libresudoku.destinations.HomeScreenDestination
import com.kaajjo.libresudoku.destinations.ImportFromFileScreenDestination
import com.kaajjo.libresudoku.destinations.MoreScreenDestination
import com.kaajjo.libresudoku.destinations.StatisticsScreenDestination
import com.kaajjo.libresudoku.destinations.WelcomeScreenDestination
import com.kaajjo.libresudoku.ui.components.navigation_bar.NavigationBarComponent
import com.kaajjo.libresudoku.ui.theme.AppTheme
import com.kaajjo.libresudoku.ui.theme.BoardColors
import com.kaajjo.libresudoku.ui.theme.LibreSudokuTheme
import com.kaajjo.libresudoku.ui.theme.SudokuBoardColorsImpl
import com.kaajjo.libresudoku.ui.util.findActivity
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

val LocalBoardColors = staticCompositionLocalOf { SudokuBoardColorsImpl() }

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var settings: AppSettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val mainViewModel: MainActivityViewModel = hiltViewModel()

            val dynamicColors by mainViewModel.dc.collectAsState(isSystemInDarkTheme())
            val darkTheme by mainViewModel.darkTheme.collectAsState(PreferencesConstants.DEFAULT_DARK_THEME)
            val amoledBlack by mainViewModel.amoledBlack.collectAsState(PreferencesConstants.DEFAULT_AMOLED_BLACK)
            val firstLaunch by mainViewModel.firstLaunch.collectAsState(false)
            val currentTheme by mainViewModel.currentTheme.collectAsState(PreferencesConstants.DEFAULT_SELECTED_THEME)
            LibreSudokuTheme(
                darkTheme = when (darkTheme) {
                    1 -> false
                    2 -> true
                    else -> isSystemInDarkTheme()
                },
                dynamicColor = dynamicColors,
                amoled = amoledBlack,
                appTheme = when (currentTheme) {
                    PreferencesConstants.GREEN_THEME_KEY -> AppTheme.Green
                    PreferencesConstants.BLUE_THEME_KEY -> AppTheme.Blue
                    PreferencesConstants.PEACH_THEME_KEY -> AppTheme.Peach
                    PreferencesConstants.YELLOW_THEME_KEY -> AppTheme.Yellow
                    PreferencesConstants.LAVENDER_THEME_KEY -> AppTheme.Lavender
                    PreferencesConstants.BLACK_AND_WHITE_THEME_KEY -> AppTheme.BlackAndWhite
                    else -> AppTheme.Green
                }
            ) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()

                var bottomBarState by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(navBackStackEntry) {
                    bottomBarState = when (navBackStackEntry?.destination?.route) {
                        StatisticsScreenDestination.route, HomeScreenDestination.route, MoreScreenDestination.route -> true
                        else -> false
                    }
                }
                LaunchedEffect(firstLaunch) {
                    if (firstLaunch) {
                        navController.navigate(
                            route = WelcomeScreenDestination.route,
                            navOptions = navOptions {
                                popUpTo(HomeScreenDestination.route) {
                                    inclusive = true
                                }
                            }
                        )
                    }
                }

                val monetSudokuBoard by mainViewModel.monetSudokuBoard.collectAsStateWithLifecycle(
                    initialValue = PreferencesConstants.DEFAULT_MONET_SUDOKU_BOARD
                )

                val boardColors =
                    if (monetSudokuBoard) {
                        SudokuBoardColorsImpl(
                            foregroundColor = BoardColors.foregroundColor,
                            notesColor = BoardColors.notesColor,
                            altForegroundColor = BoardColors.altForegroundColor,
                            errorColor = BoardColors.errorColor,
                            highlightColor = BoardColors.highlightColor,
                            thickLineColor = BoardColors.thickLineColor,
                            thinLineColor = BoardColors.thinLineColor,
                        )
                    } else {
                        SudokuBoardColorsImpl(
                            foregroundColor = MaterialTheme.colorScheme.onSurface,
                            notesColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            altForegroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            errorColor = BoardColors.errorColor,
                            highlightColor = MaterialTheme.colorScheme.outline,
                            thickLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.55f),
                            thinLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.25f),
                        )
                    }

                CompositionLocalProvider(LocalBoardColors provides boardColors) {
                    Scaffold(
                        bottomBar = {
                            NavigationBarComponent(navController, bottomBarState)
                        },
                        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
                    ) { paddingValues ->
                        DestinationsNavHost(
                            navGraph = NavGraphs.root,
                            navController = navController,
                            startRoute = NavGraphs.root.startRoute,
                            modifier = Modifier.padding(paddingValues)
                        )
                    }
                }
            }
        }
    }
}

@HiltViewModel
class MainActivityViewModel
@Inject constructor(
    themeSettingsManager: ThemeSettingsManager,
    appSettingsManager: AppSettingsManager
) : ViewModel() {
    val dc = themeSettingsManager.dynamicColors
    val darkTheme = themeSettingsManager.darkTheme
    val amoledBlack = themeSettingsManager.amoledBlack
    val firstLaunch = appSettingsManager.firstLaunch
    val currentTheme = themeSettingsManager.currentTheme
    val monetSudokuBoard = themeSettingsManager.monetSudokuBoard
}

@Destination(
    deepLinks = [
        DeepLink(
            uriPattern = "content://",
            mimeType = "*/*",
            action = Intent.ACTION_VIEW
        )
    ]
)
@Composable
fun HandleImportFromFileDeepLink(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val activity = context.findActivity()
        if (activity != null) {
            val intentData = activity.intent.data
            if (intentData != null) {
                navigator.navigate(
                    ImportFromFileScreenDestination(
                        fileUri = intentData.toString(),
                        fromDeepLink = true
                    )
                )
            }
        }
    }
}