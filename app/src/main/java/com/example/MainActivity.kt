package com.shopvion.flowgrid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.shopvion.flowgrid.ui.theme.FlowGridTheme
import com.shopvion.flowgrid.ui.theme.cubixPalette

sealed class Screen {
    data object Splash : Screen()
    data object Home : Screen()
    data object Settings : Screen()
    data object LevelSelect : Screen()
    data class Gameplay(val levelNumber: Int) : Screen()
}

class MainActivity : ComponentActivity() {
    private lateinit var audioEngine: AudioEngine
    private lateinit var prefsManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Google Mobile Ads SDK (AdMob) in background
        lifecycleScope.launch(Dispatchers.IO) {
            AdManager.initialize(applicationContext) {
                AdManager.printAllAdIds("MainActivity")
            }
        }

        audioEngine = AudioEngine(this)
        prefsManager = PreferencesManager(this)

        setContent {
            var themeMode by remember { mutableStateOf(prefsManager.getThemeMode()) }
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                PreferencesManager.THEME_LIGHT -> false
                PreferencesManager.THEME_DARK -> true
                else -> systemDark
            }

            FlowGridTheme(darkTheme = isDark) {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

                LaunchedEffect(Unit) {
                    if (prefsManager.isMusicEnabled()) {
                        audioEngine.playMusic(true)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(cubixPalette.background)
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            if (initialState is Screen.Gameplay && targetState is Screen.Gameplay) {
                                EnterTransition.None togetherWith ExitTransition.None
                            } else {
                                fadeIn(animationSpec = tween(250)) togetherWith
                                    fadeOut(animationSpec = tween(250))
                            }
                        },
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            is Screen.Splash -> {
                                SplashScreen(
                                    onNavigateToHome = {
                                        currentScreen = Screen.Home
                                    }
                                )
                            }

                            is Screen.Home -> {
                                HomeScreen(
                                    audioEngine = audioEngine,
                                    prefsManager = prefsManager,
                                    themeMode = themeMode,
                                    onToggleTheme = {
                                        val nextMode = if (isDark) {
                                            PreferencesManager.THEME_LIGHT
                                        } else {
                                            PreferencesManager.THEME_DARK
                                        }
                                        themeMode = nextMode
                                        prefsManager.setThemeMode(nextMode)
                                    },
                                    onPlayClick = {
                                        val unlocked = prefsManager.getUnlockedLevel()
                                        currentScreen = Screen.Gameplay(unlocked)
                                    },
                                    onLevelSelectClick = {
                                        currentScreen = Screen.LevelSelect
                                    },
                                    onSettingsClick = {
                                        currentScreen = Screen.Settings
                                    }
                                )
                            }

                            is Screen.Settings -> {
                                BackHandler {
                                    currentScreen = Screen.Home
                                }
                                SettingsScreen(
                                    audioEngine = audioEngine,
                                    prefsManager = prefsManager,
                                    themeMode = themeMode,
                                    onThemeModeChange = { newMode ->
                                        themeMode = newMode
                                        prefsManager.setThemeMode(newMode)
                                    },
                                    onBackClick = {
                                        currentScreen = Screen.Home
                                    }
                                )
                            }

                            is Screen.LevelSelect -> {
                                BackHandler {
                                    currentScreen = Screen.Home
                                }
                                LevelSelectScreen(
                                    prefsManager = prefsManager,
                                    onLevelSelected = { levelNum ->
                                        currentScreen = Screen.Gameplay(levelNum)
                                    },
                                    onBackClick = {
                                        currentScreen = Screen.Home
                                    }
                                )
                            }

                            is Screen.Gameplay -> {
                                BackHandler {
                                    currentScreen = Screen.LevelSelect
                                }
                                GameplayScreen(
                                    levelNumber = screen.levelNumber,
                                    audioEngine = audioEngine,
                                    prefsManager = prefsManager,
                                    onNavigateBack = {
                                        currentScreen = Screen.LevelSelect
                                    },
                                    onNextLevel = { nextLvl ->
                                        currentScreen = Screen.Gameplay(nextLvl)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::prefsManager.isInitialized && prefsManager.isMusicEnabled()) {
            audioEngine.playMusic(true)
        }
    }

    override fun onPause() {
        super.onPause()
        if (::audioEngine.isInitialized) {
            audioEngine.pauseMusic()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::audioEngine.isInitialized) {
            audioEngine.release()
        }
    }
}
