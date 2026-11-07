package io.github.hdghg.sevenhandshakes

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF263238),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF263238),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF2E7D32),
    onSecondary = Color.White,
    tertiary = Color(0xFFCCCCCC),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB0BEC5),
    onPrimary = Color(0xFF1C262B),
    primaryContainer = Color(0xFF37474F),
    onPrimaryContainer = Color(0xFFECEFF1),
    secondary = Color(0xFF81C784),
    onSecondary = Color(0xFF0B3D0E),
    tertiary = Color(0xFF5F6368),
)

/** App theme. Follows the system light/dark setting unless [darkTheme] is given. */
@Composable
fun MainTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors) {
        content()
    }
}
