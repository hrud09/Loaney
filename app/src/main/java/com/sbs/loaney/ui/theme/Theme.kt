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


private val LightColorScheme = lightColorScheme(
    primary = NeonLime,
    onPrimary = Color.Black,
    secondary = SkyBlue,
    onSecondary = Color.Black,
    tertiary = CoralRed,
    onTertiary = Color.White,
    background = DashboardBg,
    onBackground = TextMainDark,
    surface = Color.White,
    onSurface = TextMainDark,
    surfaceVariant = DarkCardBg, // Used for dark cards on the light bg
    onSurfaceVariant = TextMainLight,
    outline = SubtleBorder,
    error = CoralRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = NeonLime,
    onPrimary = Color.Black,
    secondary = SkyBlue,
    onSecondary = Color.Black,
    tertiary = CoralRed,
    onTertiary = Color.White,
    background = Color(0xFF1E1E22), // Deep dark background
    onBackground = Color.White,
    surface = Color(0xFF2C2C2E), // Elevated dark cards
    onSurface = Color.White,
    surfaceVariant = Color(0xFF38383A), // Even lighter dark surfaces
    onSurfaceVariant = Color.LightGray,
    outline = Color.DarkGray,
    error = CoralRed,
    onError = Color.White
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