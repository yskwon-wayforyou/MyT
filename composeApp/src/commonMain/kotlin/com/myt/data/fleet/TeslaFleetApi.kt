package com.myt.data.fleet

import com.myt.domain.UnitConverter
import com.myt.domain.model.ChargeInfo
import com.myt.domain.model.ConnectionStatus
import com.myt.domain.model.GaugeState
import com.myt.domain.model.Gear
import com.myt.domain.model.NavInfo
import com.myt.domain.model.TirePressures
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class TeslaFleetApi(
    private val httpClient: HttpClient,
    private val baseUrlProvider: () -> String,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val baseUrl: String get() = baseUrlProvider()

    /**
     * FW 2023.38+ requires `location_data` in endpoints to return GPS
     * (shows location-sharing icon on the vehicle UI).
     */
    private val vehicleDataEndpoints =
        "charge_state;climate_state;drive_state;location_data;vehicle_state;vehicle_config"

    suspend fun listVehicles(accessToken: String): List<TeslaVehicleSummary> {
        val response = httpClient.get("$baseUrl/api/1/vehicles") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }.body<TeslaApiResponse<List<TeslaVehicleSummary>>>()
        return response.response.orEmpty()
    }

    suspend fun fetchVehicleData(accessToken: String, vin: String): GaugeState {
        val response = httpClient.get("$baseUrl/api/1/vehicles/$vin/vehicle_data") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            parameter("endpoints", vehicleDataEndpoints)
        }.body<TeslaApiResponse<VehicleDataResponse>>()

        return response.response?.toGaugeState()
            ?: throw VehicleDataUnavailableException("Empty vehicle_data response")
    }

    suspend fun wakeUp(accessToken: String, vin: String) {
        httpClient.post("$baseUrl/api/1/vehicles/$vin/wake_up") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
    }

    suspend fun sendNavigationRequest(accessToken: String, vin: String, destination: String) {
        httpClient.post("$baseUrl/api/1/vehicles/$vin/command/navigation_request") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(
                NavigationRequestBody(
                    timestamp_ms = Clock.System.now().toEpochMilliseconds(),
                    value = destination,
                ),
            )
        }.ensureSuccess("navigation_request")
    }

    /**
     * POST `/api/1/vehicles/{vin}/command/{commandName}`.
     * Signed commands may return 403 until Virtual Key / vehicle-command proxy is ready.
     * @param whichTrunk `rear` / `front` when [commandName] is `actuate_trunk`
     * @param jsonBody raw JSON for commands that need a body (sentry / window / climate keeper)
     */
    suspend fun sendVehicleCommand(
        accessToken: String,
        vin: String,
        commandName: String,
        whichTrunk: String? = null,
        jsonBody: String? = null,
    ) {
        httpClient.post("$baseUrl/api/1/vehicles/$vin/command/$commandName") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            when {
                whichTrunk != null -> {
                    contentType(ContentType.Application.Json)
                    setBody(ActuateTrunkBody(whichTrunk))
                }
                jsonBody != null -> {
                    contentType(ContentType.Application.Json)
                    setBody(jsonBody)
                }
            }
        }.ensureSuccess(commandName)
    }
}

private suspend fun io.ktor.client.statement.HttpResponse.ensureSuccess(label: String) {
    if (status.value in 200..299) return
    val text = runCatching { bodyAsText() }.getOrDefault("")
    throw IllegalStateException("Fleet $label failed: HTTP ${status.value} $text")
}

@Serializable
data class ActuateTrunkBody(
    @SerialName("which_trunk") val whichTrunk: String,
)

@Serializable
private data class NavigationRequestBody(
    val locale: String = "ko-KR",
    val timestamp_ms: Long,
    val value: String,
    val type: String = "share_ext_content_raw",
)

@Serializable
private data class TeslaApiResponse<T>(
    val response: T? = null,
    val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
)

@Serializable
data class TeslaVehicleSummary(
    val id: Long? = null,
    val vin: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    val state: String? = null,
)

@Serializable
private data class VehicleDataResponse(
    @SerialName("drive_state") val driveState: DriveState? = null,
    @SerialName("charge_state") val chargeState: ChargeState? = null,
    @SerialName("vehicle_state") val vehicleState: VehicleState? = null,
    @SerialName("climate_state") val climateState: ClimateState? = null,
    @SerialName("location_data") val locationData: LocationData? = null,
) {
    fun toGaugeState(): GaugeState {
        val speedMph = driveState?.speed
        val speedKmh = speedMph?.let { UnitConverter.miToKm(it) } ?: 0f
        val soc = chargeState?.batteryLevel?.toFloat() ?: 0f
        val rangeKm = chargeState?.estBatteryRange?.let { UnitConverter.miToKm(it) } ?: 0f
        val gear = when ((driveState?.shiftState ?: driveState?.gear)?.uppercase()) {
            "P" -> Gear.PARK
            "R" -> Gear.REVERSE
            "N" -> Gear.NEUTRAL
            "D" -> Gear.DRIVE
            else -> Gear.PARK
        }
        val lat = locationData?.latitude
            ?: driveState?.latitude
            ?: driveState?.nativeLatitude
        val lng = locationData?.longitude
            ?: driveState?.longitude
            ?: driveState?.nativeLongitude
        val heading = (locationData?.heading ?: driveState?.heading)?.toFloat()
        val dest = driveState?.activeRouteDestination
        val milesToArrival = driveState?.activeRouteMilesToArrival
        val minutesToArrival = driveState?.activeRouteMinutesToArrival

        return GaugeState(
            speedKmh = speedKmh,
            gear = gear,
            socPercent = soc,
            rangeKm = rangeKm,
            insideTempC = climateState?.insideTemp?.toFloat(),
            outsideTempC = climateState?.outsideTemp?.toFloat(),
            powerKw = driveState?.power?.toFloat(),
            tires = buildTirePressures(vehicleState),
            navigation = if (dest != null || milesToArrival != null || minutesToArrival != null) {
                NavInfo(
                    destinationName = dest,
                    etaMinutes = minutesToArrival?.toInt()?.coerceAtLeast(1)
                        ?: milesToArrival?.let { mi -> (mi / 30.0 * 60).toInt().coerceAtLeast(1) },
                    distanceKm = milesToArrival?.let { UnitConverter.miToKm(it.toFloat()) },
                    isActive = true,
                )
            } else {
                null
            },
            charging = com.myt.domain.charge.ChargeStateNormalizer.toChargeInfo(
                chargingState = chargeState?.chargingState,
                batteryLevel = chargeState?.batteryLevel,
                chargeLimitSoc = chargeState?.chargeLimitSoc,
                chargerPowerKw = chargeState?.chargerPower,
                chargeRate = chargeState?.chargeRate,
                timeToFullHours = chargeState?.timeToFullCharge,
            ),
            connection = ConnectionStatus.FleetConnected,
            isSleeping = vehicleState?.state?.equals("asleep", ignoreCase = true) == true,
            lastUpdated = Clock.System.now().toEpochMilliseconds(),
            latitude = lat,
            longitude = lng,
            headingDegrees = heading,
            locked = vehicleState?.locked,
            odometerKm = vehicleState?.odometer?.let { UnitConverter.miToKm(it.toFloat()) },
            sentryMode = vehicleState?.sentryMode,
            climateOn = climateState?.isClimateOn,
        )
    }

    private fun buildTirePressures(vehicleState: VehicleState?): TirePressures? {
        // Fleet API tpms_pressure_* is already in bar (not PSI).
        val fl = vehicleState?.tpmsPressureFl?.toFloat() ?: return null
        val fr = vehicleState.tpmsPressureFr?.toFloat() ?: return null
        val rl = vehicleState.tpmsPressureRl?.toFloat() ?: return null
        val rr = vehicleState.tpmsPressureRr?.toFloat() ?: return null
        if (fl <= 0f && fr <= 0f && rl <= 0f && rr <= 0f) return null
        return TirePressures(fl, fr, rl, rr)
    }
}

@Serializable
private data class LocationData(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val heading: Int? = null,
)

@Serializable
private data class DriveState(
    val speed: Float? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("native_latitude") val nativeLatitude: Double? = null,
    @SerialName("native_longitude") val nativeLongitude: Double? = null,
    val heading: Int? = null,
    val gear: String? = null,
    @SerialName("shift_state") val shiftState: String? = null,
    val power: Double? = null,
    @SerialName("active_route_destination") val activeRouteDestination: String? = null,
    @SerialName("active_route_miles_to_arrival") val activeRouteMilesToArrival: Double? = null,
    @SerialName("active_route_minutes_to_arrival") val activeRouteMinutesToArrival: Double? = null,
)

@Serializable
private data class ChargeState(
    @SerialName("battery_level") val batteryLevel: Int? = null,
    @SerialName("est_battery_range") val estBatteryRange: Float? = null,
    @SerialName("charging_state") val chargingState: String? = null,
    @SerialName("charge_rate") val chargeRate: Double? = null,
    @SerialName("charger_power") val chargerPower: Double? = null,
    @SerialName("time_to_full_charge") val timeToFullCharge: Double? = null,
    @SerialName("charge_limit_soc") val chargeLimitSoc: Int? = null,
)

@Serializable
private data class VehicleState(
    val state: String? = null,
    val locked: Boolean? = null,
    val odometer: Double? = null,
    @SerialName("sentry_mode") val sentryMode: Boolean? = null,
    @SerialName("tpms_pressure_fl") val tpmsPressureFl: Double? = null,
    @SerialName("tpms_pressure_fr") val tpmsPressureFr: Double? = null,
    @SerialName("tpms_pressure_rl") val tpmsPressureRl: Double? = null,
    @SerialName("tpms_pressure_rr") val tpmsPressureRr: Double? = null,
)

@Serializable
private data class ClimateState(
    @SerialName("inside_temp") val insideTemp: Double? = null,
    @SerialName("outside_temp") val outsideTemp: Double? = null,
    @SerialName("is_climate_on") val isClimateOn: Boolean? = null,
)
