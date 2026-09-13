package ca.gastimate.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.gastimate.app.data.ApiResult
import ca.gastimate.app.data.EstimateResponse
import ca.gastimate.app.data.GastimateApi
import ca.gastimate.app.data.Station
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToLong

// One screen-state for both tabs: the estimate lives on Home; the stations
// list shares the same location. Favorites are pinned to the top of the
// sorted list. "Not yet located" is distinct from "loading"/"error".
data class GastimateUiState(
    val estimate: EstimateResponse? = null,
    val stations: List<Station> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val located: Boolean = false,
    val lat: Double? = null,
    val lng: Double? = null,
    val loadingEstimate: Boolean = false,
    val loadingStations: Boolean = false,
    val refreshingLocation: Boolean = false,
    val message: String? = null,       // transient user-facing error/info
)

class GastimateViewModel(private val api: GastimateApi) : ViewModel() {
    private val _state = MutableStateFlow(GastimateUiState())
    val state: StateFlow<GastimateUiState> = _state.asStateFlow()

    /** Stations ordered by price, favourites pinned on top. */
    fun orderedStations(): List<Station> {
        val s = _state.value
        return s.stations.sortedWith(
            compareByDescending<Station> { it.station_id in s.favorites }
                .thenBy { it.price }
        )
    }

    fun attachFavorites(favorites: Set<String>) {
        _state.update { it.copy(favorites = favorites) }
    }

    fun toggleFavorite(context: Context, stationId: String) {
        viewModelScope.launch {
            ThemePrefs.toggleFavorite(context, stationId)
        }
    }

    /** Fallback for the pre-location state: watched postal code estimate. */
    fun loadDefault() {
        if (_state.value.estimate != null || _state.value.loadingEstimate) return
        fetchEstimate(lat = null, lng = null, postalCode = DEFAULT_POSTAL)
    }

    /** The bottom button: get a balanced-power fix (cached lastLocation ≤10min
     *  preferred, live fix with 15s timeout as fallback), then refresh both
     *  tabs' data from the new coordinates. Falls back to the default postal
     *  when the backend can't resolve the location (e.g. its GasBuddy access
     *  is Cloudflare-blocked). */
    fun useMyLocation(context: Context) {
        if (_state.value.refreshingLocation) return
        _state.update { it.copy(refreshingLocation = true, message = null) }
        viewModelScope.launch {
            val fix = fetchLocation(context)
            if (fix == null) {
                _state.update {
                    it.copy(refreshingLocation = false, message = "Couldn't get your location.")
                }
            } else {
                _state.update { it.copy(lat = fix.first, lng = fix.second, located = true) }
                fetchEstimate(fix.first, fix.second)
                fetchStations(fix.first, fix.second)
                _state.update { it.copy(refreshingLocation = false) }
            }
        }
    }

    /** Stations tab opened/refreshed: re-query with whatever location we have. */
    fun refreshStations(context: Context) {
        val s = _state.value
        if (s.loadingStations) return
        if (s.lat == null || s.lng == null) { useMyLocation(context); return }
        fetchStations(s.lat, s.lng)
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    private fun fetchEstimate(lat: Double?, lng: Double?, postalCode: String? = null) {
        if (_state.value.loadingEstimate) return
        _state.update { it.copy(loadingEstimate = true, message = null) }
        viewModelScope.launch {
            when (val r = api.estimate(lat, lng, postalCode)) {
                is ApiResult.Success ->
                    _state.update { it.copy(estimate = r.value, loadingEstimate = false, message = null) }
                is ApiResult.Failure ->
                    if (lat != null && lng != null) {
                        // Location-based lookup failed (backend can't reach
                        // GasBuddy from its host, e.g. Cloudflare-blocked):
                        // fall back to the default postal rather than erroring out.
                        when (val fb = api.estimate(null, null, DEFAULT_POSTAL)) {
                            is ApiResult.Success ->
                                _state.update { it.copy(estimate = fb.value, loadingEstimate = false, message = null) }
                            is ApiResult.Failure ->
                                _state.update {
                                    it.copy(loadingEstimate = false, message = "Couldn't load prediction: ${r.message}")
                                }
                        }
                    } else {
                        _state.update {
                            it.copy(loadingEstimate = false, message = "Couldn't load prediction: ${r.message}")
                        }
                    }
            }
        }
    }

    private fun fetchStations(lat: Double?, lng: Double?) {
        if (_state.value.loadingStations) return
        _state.update { it.copy(loadingStations = true, message = null) }
        viewModelScope.launch {
            when (val r = api.stations(lat, lng)) {
                is ApiResult.Success ->
                    // Don't clear an in-flight estimate's "forecasting…" note.
                    _state.update {
                        it.copy(stations = r.value.stations, loadingStations = false,
                                message = if (it.loadingEstimate) it.message else null)
                    }
                is ApiResult.Failure -> {
                    val prev = _state.value.stations
                    if (prev.isEmpty() && lat != null && lng != null) {
                        // Located grid failed (backend host blocked by GasBuddy
                        // + no warmed cache there). The estimate's postal is
                        // always warm — serve those stations rather than none.
                        val fbPostal = _state.value.estimate?.postal_code?.takeIf { it.isNotBlank() } ?: DEFAULT_POSTAL
                        when (val fb = api.stations(null, null, fbPostal)) {
                            is ApiResult.Success ->
                                _state.update {
                                    it.copy(stations = fb.value.stations, loadingStations = false,
                                            message = "Showing prices near ${fb.value.stations.firstOrNull()?.address?.substringAfterLast(", ") ?: "the default area"} — live data for your area is temporarily unavailable.")
                                }
                            is ApiResult.Failure ->
                                _state.update {
                                    it.copy(loadingStations = false,
                                            message = "Station prices aren't available right now. Try again in a few minutes.")
                                }
                        }
                    } else {
                        _state.update {
                            it.copy(
                                loadingStations = false,
                                message = if (prev.isNotEmpty()) "Couldn't refresh station prices — showing the last list."
                                else "Station prices aren't available for this area yet. Try again in a few minutes.",
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun fetchLocation(context: Context): Pair<Double, Double>? {
        return try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            // Fresh-enough cached fix first (≤10 min) — instant in the common case.
            val last = try {
                withTimeoutOrNull(2000) { client.lastLocation.await() }
            } catch (e: SecurityException) {
                return null
            } catch (e: Exception) {
                null
            }
            if (last != null && isFresh(last.elapsedRealtimeNanos)) {
                return last.latitude to last.longitude
            }
            val current = withTimeoutOrNull(15_000) {
                try {
                    client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                } catch (e: SecurityException) {
                    null
                }
            } ?: return null
            current.latitude to current.longitude
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun isFresh(elapsedRealtimeNanos: Long): Boolean =
        (android.os.SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000L in 0..(10 * 60 * 1000L)

    companion object {
        // Backend's watched postal code (J3Y = Montreal South Shore).
        const val DEFAULT_POSTAL = "J3Y"
    }
}
