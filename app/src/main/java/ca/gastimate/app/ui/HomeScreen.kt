package ca.gastimate.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ca.gastimate.app.data.EstimateResponse
import ca.gastimate.app.data.PriceFormat

@Composable
fun HomeScreen(
    state: GastimateUiState,
    onUseMyLocation: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val est = state.estimate
        if (est != null) {
            // Previous data stays visible during refreshes (incl. long cold ones).
            EstimateCard(est)
            if (state.loadingEstimate) {
                Spacer(Modifier.height(12.dp))
                CircularProgressIndicator(strokeWidth = 2.dp)
            }
        } else if (state.loadingEstimate) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text("Loading today's gas price…")
        } else {
            Text(
                "No price data yet.\nTap the button below to get prices near you.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onUseMyLocation,
            enabled = !state.refreshingLocation,
        ) {
            if (state.refreshingLocation) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.size(8.dp))
            }
            Text(if (state.refreshingLocation) "Finding you…" else if (state.located) "Refresh my location" else "Use my location")
        }

        state.message?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun EstimateCard(est: EstimateResponse) {
    val (currentPrice, currentUnit) = PriceFormat.priceAndUnit(est.current_avg_price, est.unit)
    val (predictedPrice, _) = PriceFormat.priceAndUnit(est.predicted_price, est.unit)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Average Gas Price", style = MaterialTheme.typography.titleMedium)
            Text(
                currentPrice,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(currentUnit, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(24.dp))

            Text("Tomorrow's Predicted Gas Price", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    predictedPrice,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (est.trend_direction != null) {
                    Spacer(Modifier.size(8.dp))
                    Text(
                        PriceFormat.trendArrow(est.trend_direction),
                        style = MaterialTheme.typography.headlineMedium,
                        color = when (est.trend_direction.uppercase()) {
                            "RISE" -> MaterialTheme.colorScheme.error
                            "FALL" -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            est.price_range?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
