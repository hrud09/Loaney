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
        
        lifecycleScope.launch {
            delay(3000)
            keepSplashScreen = false
        }

        enableEdgeToEdge()
        setContent {
            val themeMode by settingsRepository.themeModeFlow.collectAsState(initial = 1)
            val isDarkTheme = when (themeMode) {
                1 -> false // Force Light
                2 -> true // Force Dark
                else -> isSystemInDarkTheme() // System default
            }

            LoaneyTheme(darkTheme = isDarkTheme) {
                MainScreen()
            }
        }
    }
}
