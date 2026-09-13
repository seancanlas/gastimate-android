package ca.gastimate.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ca.gastimate.app.data.PriceFormat
import ca.gastimate.app.data.Station

@Composable
fun StationsScreen(
    state: GastimateUiState,
    orderedStations: List<Station>,
    onToggleFavorite: (String) -> Unit,
    onRequestLocation: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (orderedStations.isEmpty() && state.loadingStations) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Finding stations near you…")
                }
            }
        } else if (orderedStations.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp),
                ) {
                    Text(
                        "No station prices yet.\nShare your location to see nearby stations.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = onRequestLocation) {
                        Text("Use my location")
                    }
                }
            }
        } else {
            val favorites = orderedStations.filter { it.station_id in state.favorites }
            val nearby = orderedStations.filter { it.station_id !in state.favorites }
            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (favorites.isNotEmpty()) {
                    item(key = "favourites-container") {
                        // Favourites: lighter surface, rounded container.
                        // animateItem smooths the section's appearance/reflow.
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    RoundedCornerShape(16.dp),
                                )
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "Favourites",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                            )
                            favorites.forEach { station ->
                                StationCard(station, isFavorite = true, onToggleFavorite)
                            }
                        }
                    }
                }
                if (nearby.isNotEmpty()) {
                    item(key = "hdr-nearby") { SectionHeader("Nearby") }
                    items(nearby, key = { it.station_id }) { station ->
                        // Favouriting moves cards between sections; the lazy
                        // item's animateItem smooths the reflow.
                        StationCard(
                            station = station,
                            isFavorite = false,
                            onToggleFavorite = onToggleFavorite,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 4.dp),
    )
}

@Composable
private fun StationCard(
    station: Station,
    isFavorite: Boolean,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Card(
        modifier = modifier.fillMaxWidth(),
        // Dark: dark-grey fill so cards separate from the pure-black
        // background; light: theme default (white).
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = if (isSystemInDarkTheme()) 0.55f else 1f
            ),
        ),
    ) {
        // Initial card design: two sibling columns — text left, price right.
        // The heart (favorites entry point) tops the right column.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { openInMaps(context, station) },
            ) {
                Text(
                    station.name.ifBlank { station.brand ?: "Gas station" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (station.address.isNotBlank()) {
                    Text(
                        station.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    station.distance_km?.let {
                        Text(
                            "$it km away",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    station.open_status?.let {
                        Text(
                            it.replaceFirstChar { c -> c.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (it.equals("open", true))
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                IconButton(
                    onClick = { onToggleFavorite(station.station_id) },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isFavorite) "Unfavourite ${station.name}"
                        else "Favourite ${station.name}",
                        tint = if (isFavorite) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    PriceFormat.displayPrice(station.price, station.unit),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
                station.cash_price?.let { cash ->
                    Text(
                        PriceFormat.displayPrice(cash, station.unit) + " cash",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// Opens the station in the user's default maps app via a geo: URI.
private fun openInMaps(context: android.content.Context, station: Station) {
    if (station.latitude == 0.0 && station.longitude == 0.0) return
    val label = android.net.Uri.encode(station.name.ifBlank { station.address })
    val uri = android.net.Uri.parse(
        "geo:${station.latitude},${station.longitude}?q=${station.latitude},${station.longitude}($label)"
    )
    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
}
