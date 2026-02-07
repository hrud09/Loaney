package com.sbs.loaney.ui.theme

import android.app.Activity
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


private val DarkColorScheme = darkColorScheme(
    primary = PrimaryLime,
    onPrimary = Color.Black,
    secondary = SecondaryOrange,
    onSecondary = Color.Black,
    tertiary = TertiaryRed,
    onTertiary = Color.Black,
    background = BlackBG,
    onBackground = Color.White,
    surface = ChipGrey,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF3C3C3E),
    onSurfaceVariant = Color.White,
    error = Color(0xFFFF6B6B),
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLime,
    onPrimary = Color.Black,
    secondary = SecondaryOrange,
    onSecondary = Color.Black,
    tertiary = TertiaryRed,
    onTertiary = Color.Black,
    background = BlackBG, // Force dark mode look even in light mode for consistency with the design request
    onBackground = Color.White,
    surface = ChipGrey,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF3C3C3E),
    onSurfaceVariant = Color.White
)

@Composable
fun LoaneyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disable dynamic color to stick to the art style
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}