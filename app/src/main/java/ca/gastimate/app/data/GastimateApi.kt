package ca.gastimate.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

sealed class ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>()
    data class Failure(val message: String, val statusCode: Int? = null) : ApiResult<Nothing>()
}

// Bearer-auth client for the Gastimate API. One method per endpoint; never
// throws — network and decode failures come back as ApiResult.Failure with
// a user-showable message.
class GastimateApi(
    baseUrl: String,
    private val apiKey: String,
    // Cache-miss estimates run the full forecast pipeline server-side (~30s
    // measured), and GasBuddy fetches can lag; defaults (10s) abort mid-request.
    private val http: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(60, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build(),
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val root = baseUrl.trimEnd('/')

    /** Tomorrow's estimate; lat/lng preferred, postal fallback (e.g. J3Y). */
    suspend fun estimate(lat: Double?, lng: Double?, postalCode: String? = null): ApiResult<EstimateResponse> =
        call(
            path = "/api/v1/estimate",
            query = buildMap {
                lat?.let { put("lat", it.toString()) }
                lng?.let { put("lng", it.toString()) }
                if (lat == null && lng == null && postalCode != null) put("postal_code", postalCode)
            },
        )

    /** Live station prices around the user (GasBuddy data via the backend). */
    suspend fun stations(lat: Double?, lng: Double?, postalCode: String? = null): ApiResult<StationsResponse> =
        call(
            path = "/api/v1/stations",
            query = buildMap {
                lat?.let { put("lat", it.toString()) }
                lng?.let { put("lng", it.toString()) }
                if (lat == null && lng == null && postalCode != null) put("postal_code", postalCode)
            },
        )

    private suspend inline fun <reified T> call(path: String, query: Map<String, String>): ApiResult<T> =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) {
                return@withContext ApiResult.Failure("API key is not configured.")
            }
            val url = StringBuilder("$root$path").apply {
                if (query.isNotEmpty()) {
                    append(query.entries.joinToString(prefix = "?", separator = "&") { "${it.key}=${it.value}" })
                }
            }.toString()
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $apiKey")
                .get()
                .build()
            try {
                http.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        // FastAPI errors: {"detail": "..."} — surface that text.
                        val detail = json.decodeFromString(ErrorBody.serializer(), body).detail
                        return@withContext ApiResult.Failure(detail ?: "Request failed (${response.code})", response.code)
                    }
                    ApiResult.Success(json.decodeFromString<T>(body))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                ApiResult.Failure("Network error — check your connection and try again.")
            } catch (e: Exception) {
                ApiResult.Failure("Unexpected response from server.")
            }
        }
}

@kotlinx.serialization.Serializable
private data class ErrorBody(val detail: String? = null)
