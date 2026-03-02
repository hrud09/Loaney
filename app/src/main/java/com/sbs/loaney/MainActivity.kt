package com.sbs.loaney

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.sbs.loaney.ui.screens.MainScreen
import com.sbs.loaney.ui.theme.LoaneyTheme
import com.sbs.loaney.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import javax.inject.Inject
import androidx.compose.foundation.isSystemInDarkTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var keepSplashScreen = true

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val themeMode by settingsRepository.themeModeFlow.collectAsState(initial = 1)
            val accentColor by settingsRepository.accentColorFlow.collectAsState(initial = 0)
            val isDarkTheme = when (themeMode) {
                1 -> false // Force Light
                2 -> true // Force Dark
                3 -> false // Colorful (handled separately)
                else -> isSystemInDarkTheme() // System default
            }
            val isColorful = themeMode == 3
            
            val onboardingCompleted by settingsRepository.onboardingCompletedFlow.collectAsState(initial = null)

            // Keep the splash screen visible until we know if onboarding is completed
            LaunchedEffect(onboardingCompleted) {
                if (onboardingCompleted != null) {
                    delay(300) // Small delay for smooth transition
                    keepSplashScreen = false
                }
            }

            LoaneyTheme(darkTheme = isDarkTheme, colorfulTheme = isColorful, colorfulAccent = accentColor) {
                if (onboardingCompleted != null) {
                    val startDest = if (onboardingCompleted == true) {
                        com.sbs.loaney.ui.navigation.Screen.Home.route
                    } else {
                        com.sbs.loaney.ui.navigation.Screen.Onboarding.route
                    }
                    MainScreen(startDestination = startDest)
                }
            }
        }
    }
}
