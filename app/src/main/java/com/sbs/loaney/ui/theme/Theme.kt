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
    ColorfulAccent( // 0: Purple Glass
        name = "Purple",
        primary = Color(0xFFB08DFF),
        secondary = Color(0xFFFF8EC4),
        background = Color(0xFF1C1830),
        surface = Color(0xFF2D2650),         // frosted violet glass panel
        surfaceVariant = Color(0xFF382F5E),   // slightly lighter glass layer
        outline = Color(0xFF6B5CA0),          // fine glass edge
        onSurface = Color(0xFFF0EAFF),
        onSurfaceVariant = Color(0xFFC8B8F0),
        primaryContainer = Color(0xFF3D3470),
        onPrimaryContainer = Color(0xFFDDD0FF)
    ),
    ColorfulAccent( // 1: Teal Glass
        name = "Teal",
        primary = Color(0xFF5CE6D6),
        secondary = Color(0xFF8AF5E8),
        background = Color(0xFF101E22),
        surface = Color(0xFF1A3840),         // frosted teal glass panel
        surfaceVariant = Color(0xFF22434C),   // lighter glass layer
        outline = Color(0xFF4A8090),          // fine glass edge
        onSurface = Color(0xFFE4FDF8),
        onSurfaceVariant = Color(0xFFA0D8CC),
        primaryContainer = Color(0xFF1E4A55),
        onPrimaryContainer = Color(0xFFC0F5EC)
    ),
    ColorfulAccent( // 2: Rose Glass
        name = "Rose",
        primary = Color(0xFFFF7EAE),
        secondary = Color(0xFFFFC0DA),
        background = Color(0xFF221420),
        surface = Color(0xFF3C2238),         // frosted rose glass panel
        surfaceVariant = Color(0xFF482C44),   // lighter glass layer
        outline = Color(0xFF885070),          // fine glass edge
        onSurface = Color(0xFFFFE8F2),
        onSurfaceVariant = Color(0xFFDDA8C0),
        primaryContainer = Color(0xFF502A48),
        onPrimaryContainer = Color(0xFFFFCCE0)
    ),
    ColorfulAccent( // 3: Amber Glass
        name = "Amber",
        primary = Color(0xFFFFC46B),
        secondary = Color(0xFFFFE0A0),
        background = Color(0xFF201810),
        surface = Color(0xFF3A2E1C),         // frosted amber glass panel
        surfaceVariant = Color(0xFF463824),   // lighter glass layer
        outline = Color(0xFF8A6838),          // fine glass edge
        onSurface = Color(0xFFFFF4E0),
        onSurfaceVariant = Color(0xFFDDC490),
        primaryContainer = Color(0xFF504020),
        onPrimaryContainer = Color(0xFFFFE8B8)
    ),
    ColorfulAccent( // 4: Lime Glass
        name = "Lime",
        primary = Color(0xFF9EFF7E),
        secondary = Color(0xFFC4FFB0),
        background = Color(0xFF141E10),
        surface = Color(0xFF223818),         // frosted lime glass panel
        surfaceVariant = Color(0xFF2C4422),   // lighter glass layer
        outline = Color(0xFF508040),          // fine glass edge
        onSurface = Color(0xFFE8FFE0),
        onSurfaceVariant = Color(0xFFACDD98),
        primaryContainer = Color(0xFF2C4C1C),
        onPrimaryContainer = Color(0xFFD0FFC0)
    ),
    ColorfulAccent( // 5: Blue Glass
        name = "Blue",
        primary = Color(0xFF70AAFF),
        secondary = Color(0xFFA8CCFF),
        background = Color(0xFF101824),
        surface = Color(0xFF1C2E48),         // frosted blue glass panel
        surfaceVariant = Color(0xFF243854),   // lighter glass layer
        outline = Color(0xFF4870A0),          // fine glass edge
        onSurface = Color(0xFFE0EEFF),
        onSurfaceVariant = Color(0xFF98B8DD),
        primaryContainer = Color(0xFF284060),
        onPrimaryContainer = Color(0xFFC0DDFF)
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