package com.myt.data.fleet

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TeslaFleetApiTest {
    private val vehicleJson = """
        {
          "response": {
            "drive_state": {
              "speed": 37.5,
              "shift_state": "D",
              "latitude": 37.2636,
              "longitude": 127.0286,
              "heading": 90
            },
            "charge_state": {
              "battery_level": 72,
              "est_battery_range": 180.0,
              "charging_state": "Disconnected"
            },
            "vehicle_state": {
              "locked": true,
              "odometer": 12345.6
            },
            "climate_state": {
              "inside_temp": 22.5,
              "outside_temp": 18.0
            }
          }
        }
    """.trimIndent()

    @Test
    fun fetchVehicleData_mapsGaugeState() = runBlocking {
        val engine = MockEngine { request ->
            assertTrue(request.url.encodedPath.contains("vehicle_data"))
            assertTrue(
                request.url.parameters["endpoints"].orEmpty().contains("location_data"),
                "FW 2023.38+ requires location_data endpoint",
            )
            respond(
                content = vehicleJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val api = TeslaFleetApi(
            httpClient = client,
            baseUrlProvider = { "https://fleet-api.test" },
        )
        val state = api.fetchVehicleData("token", "VIN123456789012345")
        assertEquals(60.4f, state.speedKmh, 0.5f)
        assertEquals(72f, state.socPercent, 0.1f)
        assertEquals(true, state.locked)
    }
}
