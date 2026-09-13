package ca.gastimate.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// GET /api/v1/estimate — today's local average + tomorrow's prediction.
@Serializable
data class EstimateResponse(
    val postal_code: String? = null,
    val resolved_from: String? = null,
    val fuel_type: String? = null,
    val current_avg_price: Double,
    val unit: String,
    val predicted_price: Double,
    val price_range: String? = null,
    val trend_direction: String? = null,
    val confidence_score: Double? = null,
)

// GET /api/v1/stations — live GasBuddy station prices near a location.
@Serializable
data class StationsResponse(
    val fuel_type: String? = null,
    val station_count: Int = 0,
    val stations: List<Station> = emptyList(),
)

@Serializable
data class AddressParts(
    val line1: String = "",
    val city: String = "",
    val region: String = "",
    val postal_code: String = "",
    val country: String = "",
)

@Serializable
data class Station(
    val station_id: String = "",
    val name: String = "",
    val brand: String? = null,
    val brand_image: String? = null,
    val price: Double,
    val cash_price: Double? = null,
    val formatted_price: String? = null,
    val unit: String = "",
    val currency: String = "",
    val address: String = "",
    val address_parts: AddressParts? = null,
    val distance_km: Double? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val last_updated: String? = null,
    val open_status: String? = null,
    val phone: String? = null,
    val star_rating: Double? = null,
)

// Unit-aware price formatting shared by both screens:
// dollars_per_gallon "2.759" -> "$2.76", cents_per_liter "196.9" -> "196.9¢/L".
object PriceFormat {
    // Locale.US so prices never render with a comma decimal separator.
    fun displayPrice(price: Double, unit: String): String =
        if (unit.contains("gallon")) "$${"%.2f".format(java.util.Locale.US, price)}/gal"
        else "${"%.1f".format(java.util.Locale.US, price)}¢/L"

    fun priceAndUnit(price: Double, unit: String): Pair<String, String> =
        if (unit.contains("gallon")) "$${"%.2f".format(java.util.Locale.US, price)}" to "per gallon"
        else "${"%.1f".format(java.util.Locale.US, price)}¢" to "per litre"

    fun trendArrow(trend: String?): String = when (trend?.uppercase()) {
        "RISE" -> "↑"
        "FALL" -> "↓"
        "STABLE" -> "→"
        else -> ""
    }
}
