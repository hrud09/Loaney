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

// ─── Alim Inspired Light Theme ───────────────────────────────────────────────
private val AlimLightColorScheme = lightColorScheme(
    primary             = AlimDark,
    onPrimary           = PureWhite,
    primaryContainer    = AlimDark.copy(alpha = 0.1f),
    onPrimaryContainer  = AlimDark,
    secondary           = AlimGreen,
    onSecondary         = PureWhite,
    secondaryContainer  = AlimGreen.copy(alpha = 0.1f),
    onSecondaryContainer = AlimGreen,
    tertiary            = CoralRose,
    onTertiary          = PureWhite,
    background          = AlimCream,
    onBackground        = AlimDark,
    surface             = AlimWhite,
    onSurface           = AlimDark,
    surfaceVariant      = AlimGray,
    onSurfaceVariant    = AlimDark.copy(alpha = 0.6f),
    outline             = AlimGray,
    outlineVariant      = Color(0xFFCBD5E1),
    error               = CoralRose,
    onError             = PureWhite,
    errorContainer      = CoralRose.copy(alpha = 0.1f),
    onErrorContainer    = CoralRose
)

// ─── Alim Inspired Dark Theme ────────────────────────────────────────────────
private val AlimDarkColorScheme = darkColorScheme(
    primary             = AlimGreen,
    onPrimary           = AlimDark,
    primaryContainer    = AlimGreen.copy(alpha = 0.2f),
    onPrimaryContainer  = AlimGreen,
    secondary           = AlimGreen,
    onSecondary         = AlimDark,
    background          = Color(0xFF0D0D12), // Deeper Black
    onBackground        = AlimCream,
    surface             = Color(0xFF16161E),  // Slightly lighter dark
    onSurface           = AlimCream,
    surfaceVariant      = Color(0xFF23232E),
    onSurfaceVariant    = AlimCream.copy(alpha = 0.6f),
    outline             = Color(0xFF2D2D3A),
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
        darkTheme -> AlimDarkColorScheme
        else -> AlimLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}