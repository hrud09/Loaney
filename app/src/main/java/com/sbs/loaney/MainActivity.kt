package com.sbs.loaney

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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

    companion object {
        /** Set by LoanReminderWorker's "Send reminder" notification action. */
        const val EXTRA_REMIND_LOAN_ID = "remind_loan_id"

        /** Set by the home screen widget's Give/Take buttons. "LEND" or "BORROW". */
        const val EXTRA_ADD_LOAN_TYPE = "add_loan_type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }

        super.onCreate(savedInstanceState)

        val remindLoanId = intent?.getLongExtra(EXTRA_REMIND_LOAN_ID, -1L)
            ?.takeIf { it > 0L }
        val addLoanType = intent?.getStringExtra(EXTRA_ADD_LOAN_TYPE)

        enableEdgeToEdge()
        setContent {
            // POST_NOTIFICATIONS is declared in the manifest but was never requested at
            // runtime, so on Android 13+ every loan reminder was being dropped with a
            // silently-swallowed SecurityException.
            RequestNotificationPermission()

            val themeMode by settingsRepository.themeModeFlow.collectAsState(initial = 1)
            val accentColor by settingsRepository.accentColorFlow.collectAsState(initial = 0)
            val isDarkTheme = when (themeMode) {
                1 -> false // Force Light
                2 -> true // Force Dark
                else -> isSystemInDarkTheme() // System default
            }
            
            LaunchedEffect(isDarkTheme) {
                // Blurred off-white: ~85% opacity of #F7F7F7
                val offWhiteBlurred = android.graphics.Color.parseColor("#D9F7F7F7")
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkTheme) {
                        androidx.activity.SystemBarStyle.dark(android.graphics.Color.parseColor("#D91A1D2E"))
                    } else {
                        androidx.activity.SystemBarStyle.light(offWhiteBlurred, offWhiteBlurred)
                    }
                )
            }
            
            val onboardingCompleted by settingsRepository.onboardingCompletedFlow.collectAsState(initial = null)

            // Keep the splash screen visible until we know if onboarding is completed
            LaunchedEffect(onboardingCompleted) {
                if (onboardingCompleted != null) {
                    delay(300) // Small delay for smooth transition
                    keepSplashScreen = false
                }
            }

            LoaneyTheme(darkTheme = isDarkTheme) {
                if (onboardingCompleted != null) {
            val startDest = androidx.compose.runtime.remember(onboardingCompleted) {
                if (onboardingCompleted == true) {
                    com.sbs.loaney.ui.navigation.Screen.Home.route
                } else {
                    com.sbs.loaney.ui.navigation.Screen.Onboarding.route
                }
            }
            MainScreen(
                startDestination = startDest,
                remindLoanId = remindLoanId,
                addLoanType = addLoanType
            )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Declining is fine; reminders just stay silent. */ }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
