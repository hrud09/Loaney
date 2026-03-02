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
    primary = ThemeGreen,
    onPrimary = Color.White,
    secondary = ThemeGreen,
    onSecondary = Color.White,
    tertiary = ThemeGreen,
    onTertiary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = ThemeLightGreen,
    onSurfaceVariant = Color.Black,
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

data class ColorfulAccent(
    val name: String,
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val outline: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color
)

val colorfulAccents = listOf(
    ColorfulAccent( // 0: Purple Neon
        name = "Purple",
        primary = Color(0xFF9B72FF),
        secondary = Color(0xFFFF6B9D),
        background = Color(0xFF1A1428),
        surface = Color(0xFF241E36),
        surfaceVariant = Color(0xFF2E2644),
        outline = Color(0xFF4A3D6B),
        onSurface = Color(0xFFE8DFFF),
        onSurfaceVariant = Color(0xFFBBAADD),
        primaryContainer = Color(0xFF352A5C),
        onPrimaryContainer = Color(0xFFD4C4FF)
    ),
    ColorfulAccent( // 1: Teal Neon
        name = "Teal",
        primary = Color(0xFF4DD9E8),
        secondary = Color(0xFF80FFDB),
        background = Color(0xFF0E1B20),
        surface = Color(0xFF162429),
        surfaceVariant = Color(0xFF1E2F35),
        outline = Color(0xFF2E5058),
        onSurface = Color(0xFFD0F5F0),
        onSurfaceVariant = Color(0xFF99C9C0),
        primaryContainer = Color(0xFF1A3A42),
        onPrimaryContainer = Color(0xFFB8F0EA)
    ),
    ColorfulAccent( // 2: Rose Neon
        name = "Rose",
        primary = Color(0xFFFF6B9D),
        secondary = Color(0xFFFFB3D0),
        background = Color(0xFF1E1218),
        surface = Color(0xFF2A1A22),
        surfaceVariant = Color(0xFF36222C),
        outline = Color(0xFF5A3848),
        onSurface = Color(0xFFFFE0EA),
        onSurfaceVariant = Color(0xFFCCA0B0),
        primaryContainer = Color(0xFF4A2035),
        onPrimaryContainer = Color(0xFFFFB3CC)
    ),
    ColorfulAccent( // 3: Amber Neon
        name = "Amber",
        primary = Color(0xFFFFB74D),
        secondary = Color(0xFFFFD993),
        background = Color(0xFF1C1610),
        surface = Color(0xFF281F15),
        surfaceVariant = Color(0xFF34291C),
        outline = Color(0xFF5C4830),
        onSurface = Color(0xFFFFF0D6),
        onSurfaceVariant = Color(0xFFCCB48A),
        primaryContainer = Color(0xFF4A3520),
        onPrimaryContainer = Color(0xFFFFDDA6)
    ),
    ColorfulAccent( // 4: Lime Neon
        name = "Lime",
        primary = Color(0xFF8AFF65),
        secondary = Color(0xFFB8FF99),
        background = Color(0xFF121A0E),
        surface = Color(0xFF1A2416),
        surfaceVariant = Color(0xFF222E1E),
        outline = Color(0xFF3A5030),
        onSurface = Color(0xFFDCFFD0),
        onSurfaceVariant = Color(0xFFA0CC8A),
        primaryContainer = Color(0xFF254020),
        onPrimaryContainer = Color(0xFFC0FFB0)
    ),
    ColorfulAccent( // 5: Blue Neon
        name = "Blue",
        primary = Color(0xFF5B9BFF),
        secondary = Color(0xFF99C8FF),
        background = Color(0xFF0E141E),
        surface = Color(0xFF151D2A),
        surfaceVariant = Color(0xFF1E2836),
        outline = Color(0xFF304060),
        onSurface = Color(0xFFD4E4FF),
        onSurfaceVariant = Color(0xFF8AAACC),
        primaryContainer = Color(0xFF1C3050),
        onPrimaryContainer = Color(0xFFB8D6FF)
    )
)

private fun colorfulColorScheme(accentIndex: Int): androidx.compose.material3.ColorScheme {
    val accent = colorfulAccents.getOrElse(accentIndex) { colorfulAccents[0] }
    return darkColorScheme(
        primary = accent.primary,
        onPrimary = Color.White,
        secondary = accent.secondary,
        onSecondary = Color.Black,
        tertiary = accent.secondary,
        onTertiary = Color.Black,
        background = accent.background,
        onBackground = accent.onSurface,
        surface = accent.surface,
        onSurface = accent.onSurface,
        surfaceVariant = accent.surfaceVariant,
        onSurfaceVariant = accent.onSurfaceVariant,
        outline = accent.outline,
        primaryContainer = accent.primaryContainer,
        onPrimaryContainer = accent.onPrimaryContainer,
        errorContainer = Color(0xFF4A2030),
        onErrorContainer = Color(0xFFFFB3B3),
        error = SoftNeonError,
        onError = Color.White
    )
}

@Composable
fun LoaneyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorfulTheme: Boolean = false,
    colorfulAccent: Int = 0,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        colorfulTheme -> colorfulColorScheme(colorfulAccent)
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