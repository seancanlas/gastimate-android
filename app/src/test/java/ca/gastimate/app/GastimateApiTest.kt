package ca.gastimate.app

import ca.gastimate.app.data.ApiResult
import ca.gastimate.app.data.GastimateApi
import ca.gastimate.app.data.PriceFormat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GastimateApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: GastimateApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = GastimateApi(baseUrl = server.url("/").toString(), apiKey = "gas_live_test")
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `estimate sends bearer auth and lat lng and decodes the response`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "postal_code": "H4P",
                  "resolved_from": "latlng",
                  "fuel_type": "regular",
                  "current_avg_price": 196.9,
                  "unit": "cents_per_liter",
                  "predicted_price": 198.6,
                  "price_range": "197.8¢/L - 199.4¢/L",
                  "trend_direction": "RISE",
                  "confidence_score": 0.75,
                  "unknown_field": "ignored"
                }
                """.trimIndent(),
            ).setHeader("Content-Type", "application/json"),
        )

        val result = api.estimate(lat = 45.4959, lng = -73.6733)

        val recorded = server.takeRequest()
        assertEquals("/api/v1/estimate?lat=45.4959&lng=-73.6733", recorded.path)
        assertEquals("Bearer gas_live_test", recorded.getHeader("Authorization"))

        val est = (result as ApiResult.Success).value
        assertEquals(196.9, est.current_avg_price, 0.001)
        assertEquals(198.6, est.predicted_price, 0.001)
        assertEquals("RISE", est.trend_direction)
    }

    @Test
    fun `stations decodes the station payload`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "fuel_type": "regular",
                  "station_count": 1,
                  "stations": [
                    {
                      "station_id": "69759",
                      "name": "Petro-Canada",
                      "brand": "Petro-Canada",
                      "price": 194.9,
                      "unit": "cents_per_liter",
                      "currency": "CAD",
                      "address": "415 Blvd Marcel-Laurin, Saint-Laurent, QC, H4M 2L8",
                      "distance_km": 1.0,
                      "latitude": 45.50,
                      "longitude": -73.67
                    }
                  ]
                }
                """.trimIndent(),
            ).setHeader("Content-Type", "application/json"),
        )

        val result = api.stations(lat = 45.4959, lng = -73.6733)
        val stations = (result as ApiResult.Success).value.stations
        assertEquals(1, stations.size)
        assertEquals("Petro-Canada", stations[0].name)
        assertEquals(194.9, stations[0].price, 0.001)
        assertTrue(stations[0].address.contains("Marcel-Laurin"))
    }

    @Test
    fun `error detail is surfaced from the error body`() = runTest {
        server.enqueue(
            MockResponse().setBody("""{"detail": "Provide postal_code or lat & lng."}""")
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json"),
        )
        val result = api.estimate(lat = null, lng = null)
        assertTrue((result as ApiResult.Failure).message.contains("Provide postal_code"))
    }

    @Test
    fun `price formatting covers both units and trends`() {
        assertEquals("196.9¢/L", PriceFormat.displayPrice(196.9, "cents_per_liter"))
        assertEquals("$2.76/gal", PriceFormat.displayPrice(2.759, "dollars_per_gallon"))
        assertEquals("↑", PriceFormat.trendArrow("RISE"))
        assertEquals("", PriceFormat.trendArrow(null))
    }
}
