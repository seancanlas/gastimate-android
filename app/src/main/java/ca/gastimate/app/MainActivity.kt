package ca.gastimate.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ca.gastimate.app.data.GastimateApi
import ca.gastimate.app.ui.GastimateTheme
import ca.gastimate.app.ui.GastimateViewModel
import ca.gastimate.app.ui.HomeScreen
import ca.gastimate.app.ui.StationsScreen
import ca.gastimate.app.ui.ThemeMode
import ca.gastimate.app.ui.ThemePrefs
import ca.gastimate.app.ui.ThemeSwitcher
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val locationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                pendingLocationAction?.invoke()
            }
            pendingLocationAction = null
        }

    // The action to run once permission lands; set right before the prompt.
    private var pendingLocationAction: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val api = GastimateApi(
            baseUrl = BuildConfig.API_BASE_URL,
            apiKey = BuildConfig.API_KEY,
        )
        val appContext = applicationContext
        setContent {
            // Theme flows from persisted prefs; changes apply live.
            val themeMode by ThemePrefs.mode(appContext).collectAsState(initial = ThemeMode.SYSTEM)
            GastimateTheme(mode = themeMode) {
                val vm: GastimateViewModel = viewModel(factory = viewModelFactory {
                    initializer { GastimateViewModel(api) }
                })
                // Favorites flow into the VM so the list can pin them on top.
                val favorites by ThemePrefs.favorites(appContext).collectAsState(initial = emptySet())
                LaunchedEffect(favorites) { vm.attachFavorites(favorites) }
                GastimateApp(
                    vm = vm,
                    themeMode = themeMode,
                    onThemeSelected = { mode ->
                        lifecycleScope.launch {
                            ThemePrefs.setMode(appContext, mode)
                        }
                    },
                    onToggleFavorite = { stationId ->
                        lifecycleScope.launch {
                            ThemePrefs.toggleFavorite(appContext, stationId)
                        }
                    },
                    requestLocation = ::requestLocationThen,
                )
            }
        }
    }

    /** Permission gate: run [then] immediately if granted, else prompt once. */
    private fun requestLocationThen(then: () -> Unit) {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            then()
        } else {
            pendingLocationAction = then
            locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GastimateApp(
    vm: GastimateViewModel,
    themeMode: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    onToggleFavorite: (String) -> Unit,
    requestLocation: (then: () -> Unit) -> Unit,
) {
    val state by vm.state.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    val activity = LocalContext.current as ComponentActivity

    // Default content before any location: watched postal code estimate.
    LaunchedEffect(Unit) { vm.loadDefault() }

    // NC-13: cold open and every foreground return use the live location to
    // refresh current + predicted prices. ON_RESUME fires on cold start too,
    // so one observer covers both. Denied permission just re-prompts next
    // time through the requestLocation gate ("ask again"); a failed fix
    // surfaces the existing "Couldn't get your location." message.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                requestLocation { vm.useMyLocation(activity) }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gastimate") },
                actions = {
                    ThemeSwitcher(mode = themeMode, onModeSelected = onThemeSelected)
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavItem(
                    label = "Home",
                    selected = tab == 0,
                    filled = Icons.Filled.Home,
                    outlined = Icons.Outlined.Home,
                    onClick = { tab = 0 },
                    modifier = Modifier.weight(1f),
                )
                NavItem(
                    label = "Gas Prices",
                    selected = tab == 1,
                    filled = Icons.Filled.LocationOn,
                    outlined = Icons.Outlined.LocationOn,
                    onClick = {
                        tab = 1
                        if (!vm.state.value.located) {
                            requestLocation { vm.useMyLocation(activity) }
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (tab) {
                0 -> HomeScreen(state) { requestLocation { vm.useMyLocation(activity) } }
                else -> StationsScreen(state, vm.orderedStations(), onToggleFavorite) {
                    requestLocation { vm.useMyLocation(activity) }
                }
            }
        }
    }
}

// Nav item with no indicator pill and no ripple — the filled vs outlined
// glyph IS the selected state. 64dp min height honors Material touch targets.
@Composable
private fun NavItem(
    label: String,
    selected: Boolean,
    filled: androidx.compose.ui.graphics.vector.ImageVector,
    outlined: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = if (selected) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() }
            .padding(top = 8.dp),
    ) {
        Icon(
            if (selected) filled else outlined,
            contentDescription = if (selected) "$label (selected)" else label,
            tint = tint,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
        )
    }
}
