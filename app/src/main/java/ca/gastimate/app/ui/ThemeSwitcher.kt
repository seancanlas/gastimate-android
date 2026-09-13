package ca.gastimate.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ca.gastimate.app.R

// Top-right theme button: shows the mode in effect — sun for light, moon for
// dark. "Follow phone settings" mirrors whatever the phone is currently on.
@Composable
fun ThemeSwitcher(
    mode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    // System mode shows the phone's current mode's icon.
    val effectiveDark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val currentIcon = if (effectiveDark) R.drawable.ic_theme_dark else R.drawable.ic_theme_light
    IconButton(onClick = { open = true }) {
        Icon(
            painter = painterResource(currentIcon),
            contentDescription = "Theme: " + when (mode) {
                ThemeMode.LIGHT -> "light"
                ThemeMode.DARK -> "dark"
                ThemeMode.SYSTEM -> "follow phone settings"
            },
            modifier = Modifier.size(22.dp),
        )
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        ThemeMode.entries.forEach { m ->
            DropdownMenuItem(
                text = { Text(when (m) {
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.DARK -> "Dark"
                    ThemeMode.SYSTEM -> "Use phone settings"
                }) },
                leadingIcon = {
                    val dark = when (m) {
                        ThemeMode.DARK -> true
                        ThemeMode.LIGHT -> false
                        ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    }
                    Icon(
                        painter = painterResource(
                            if (dark) R.drawable.ic_theme_dark else R.drawable.ic_theme_light
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = {
                    open = false
                    onModeSelected(m)
                },
            )
        }
    }
}
