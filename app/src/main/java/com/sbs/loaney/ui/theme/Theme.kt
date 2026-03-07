package com.sbs.loaney.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ─── Cyber-Vibrant Light Theme ───────────────────────────────────────────────
private val CyberLightColorScheme = lightColorScheme(
    primary             = CyberIndigo,
    onPrimary           = PureWhite,
    primaryContainer    = CyberIndigo.copy(alpha = 0.1f),
    onPrimaryContainer  = CyberIndigo,
    secondary           = VibrantTeal,
    onSecondary         = PureWhite,
    secondaryContainer  = VibrantTeal.copy(alpha = 0.1f),
    onSecondaryContainer = VibrantTeal,
    tertiary            = CoralRose,
    onTertiary          = PureWhite,
    background          = SoftSlate,
    onBackground        = PureBlack,
    surface             = PureWhite,
    onSurface           = PureBlack,
    surfaceVariant      = Color(0xFFF1F5F9),
    onSurfaceVariant    = Color(0xFF475569),
    outline             = Color(0xFFE2E8F0),
    outlineVariant      = Color(0xFFCBD5E1),
    error               = CoralRose,
    onError             = PureWhite,
    errorContainer      = CoralRose.copy(alpha = 0.1f),
    onErrorContainer    = CoralRose
)

// ─── Cyber-Vibrant Dark Theme ────────────────────────────────────────────────
private val CyberDarkColorScheme = darkColorScheme(
    primary             = CyberIndigo,
    onPrimary           = PureWhite,
    primaryContainer    = Color(0xFF312E81),
    onPrimaryContainer  = Color(0xFFC7D2FE),
    secondary           = VibrantTeal,
    onSecondary         = PureWhite,
    background          = Color(0xFF0F172A), // Deep Slate
    onBackground        = Color(0xFFF8FAFC),
    surface             = Color(0xFF1E293B),
    onSurface           = Color(0xFFF1F5F9),
    surfaceVariant      = Color(0xFF334155),
    onSurfaceVariant    = Color(0xFF94A3B8),
    outline             = Color(0xFF475569),
    error               = CoralRose,
    onError             = PureWhite
)

@Composable
fun LoaneyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> CyberDarkColorScheme
        else -> CyberLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}