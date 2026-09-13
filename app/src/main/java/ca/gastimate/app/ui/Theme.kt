package ca.gastimate.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Fuel-station palette: deep green + cream (light) / asphalt + luminous
// green (dark). Three-mode only (light/dark/system) — per spec.
private val LightScheme = lightColorScheme(
    primary = Color(0xFF2E6E4E),          // deep gas-station green
    onPrimary = Color.White,
    secondary = Color(0xFF526352),
    background = Color(0xFFF7F5F0),      // warm off-white, pump-sign cream
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE7E3DA),
    onBackground = Color(0xFF1B1C1A),
    onSurface = Color(0xFF1B1C1A),
    onSurfaceVariant = Color(0xFF566052),
    outline = Color(0xFFC9C8BF),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF8FD8AE),          // luminous green, dark-mode friendly
    onPrimary = Color(0xFF0B2E1D),
    secondary = Color(0xFFB9CCB9),
    background = Color(0xFF000000),       // pure black
    onBackground = Color(0xFFE3E6DF),
    surface = Color(0xFF000000),          // pure black
    onSurface = Color(0xFFE3E6DF),
    surfaceVariant = Color(0xFF1F241F),   // cards' inner contrast areas
    onSurfaceVariant = Color(0xFFA8B2A6),
    surfaceContainer = Color(0xFF000000),            // nav bar / menus: pure black
    surfaceContainerLow = Color(0xFF1C1C1C),         // cards: dark grey so they
                                                     // separate from the black bg
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerHigh = Color(0xFF000000),
    surfaceContainerHighest = Color(0xFF000000),
    surfaceTint = Color(0xFF000000),
    outline = Color(0xFF3D4239),
)

// Local override beats the system setting.
@Composable
fun GastimateTheme(
    mode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val useDark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    // Dark theme → white status-bar icons; light → dark icons.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDark
        }
    }
    MaterialTheme(
        colorScheme = if (useDark) DarkScheme else LightScheme,
        content = content,
    )
}
