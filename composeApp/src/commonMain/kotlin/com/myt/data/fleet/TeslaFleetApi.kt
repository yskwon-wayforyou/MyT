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
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class TeslaFleetApi(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun listVehicles(accessToken: String): List<TeslaVehicleSummary> {
        val response = httpClient.get("$baseUrl/api/1/vehicles") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }.body<TeslaApiResponse<List<TeslaVehicleSummary>>>()
        return response.response.orEmpty()
    }

    suspend fun fetchVehicleData(accessToken: String, vin: String): GaugeState {
        val response = httpClient.get("$baseUrl/api/1/vehicles/$vin/vehicle_data") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }.body<TeslaApiResponse<VehicleDataResponse>>()

        return response.response?.toGaugeState()
            ?: error("Empty vehicle_data response")
    }

    suspend fun wakeUp(accessToken: String, vin: String) {
        httpClient.post("$baseUrl/api/1/vehicles/$vin/wake_up") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
    }

    suspend fun sendNavigationRequest(accessToken: String, vin: String, destination: String) {
        httpClient.post("$baseUrl/api/1/vehicles/$vin/command/navigation_request") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            // TODO: signed Vehicle Command Protocol body for production
        }
    }
}

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
) {
    fun toGaugeState(): GaugeState {
        val speedMph = driveState?.speed
        val speedKmh = speedMph?.let { UnitConverter.miToKm(it) } ?: 0f
        val soc = chargeState?.batteryLevel?.toFloat() ?: 0f
        val rangeKm = chargeState?.estBatteryRange?.let { UnitConverter.miToKm(it) } ?: 0f
        val gear = when (driveState?.gear?.uppercase()) {
            "P" -> Gear.PARK
            "R" -> Gear.REVERSE
            "N" -> Gear.NEUTRAL
            "D" -> Gear.DRIVE
            else -> Gear.PARK
        }
        val lat = driveState?.latitude
        val lng = driveState?.longitude
        val heading = driveState?.heading?.toFloat()
        val dest = driveState?.activeRouteDestination
        val milesToArrival = driveState?.activeRouteMilesToArrival

        return GaugeState(
            speedKmh = speedKmh,
            gear = gear,
            socPercent = soc,
            rangeKm = rangeKm,
            insideTempC = climateState?.insideTemp?.toFloat(),
            outsideTempC = climateState?.outsideTemp?.toFloat(),
            powerKw = driveState?.power?.toFloat(),
            tires = buildTirePressures(vehicleState),
            navigation = dest?.let {
                NavInfo(
                    destinationName = it,
                    etaMinutes = milesToArrival?.let { mi ->
                        // rough ETA placeholder until MinutesToArrival field wired
                        (mi / 30.0 * 60).toInt().coerceAtLeast(1)
                    },
                    distanceKm = milesToArrival?.let { UnitConverter.miToKm(it.toFloat()) },
                )
            },
            charging = ChargeInfo(
                isCharging = chargeState?.chargingState == "Charging",
                chargeRateKw = chargeState?.chargeRate?.toFloat(),
                timeToFullMinutes = chargeState?.timeToFullCharge?.let { (it * 60).toInt() },
            ),
            connection = ConnectionStatus.FleetConnected,
            isSleeping = vehicleState?.state?.equals("asleep", ignoreCase = true) == true,
            lastUpdated = Clock.System.now().toEpochMilliseconds(),
            latitude = lat,
            longitude = lng,
            headingDegrees = heading,
        )
    }

    private fun psiToBar(psi: Float): Float = psi * 0.0689476f

    private fun buildTirePressures(vehicleState: VehicleState?): TirePressures? {
        val fl = vehicleState?.tpmsPressureFl?.toFloat()?.let { psiToBar(it) } ?: return null
        val fr = vehicleState.tpmsPressureFr?.toFloat()?.let { psiToBar(it) } ?: return null
        val rl = vehicleState.tpmsPressureRl?.toFloat()?.let { psiToBar(it) } ?: return null
        val rr = vehicleState.tpmsPressureRr?.toFloat()?.let { psiToBar(it) } ?: return null
        return TirePressures(fl, fr, rl, rr)
    }
}

@Serializable
private data class DriveState(
    val speed: Float? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val heading: Int? = null,
    val gear: String? = null,
    val power: Double? = null,
    @SerialName("active_route_destination") val activeRouteDestination: String? = null,
    @SerialName("active_route_miles_to_arrival") val activeRouteMilesToArrival: Double? = null,
)

@Serializable
private data class ChargeState(
    @SerialName("battery_level") val batteryLevel: Int? = null,
    @SerialName("est_battery_range") val estBatteryRange: Float? = null,
    @SerialName("charging_state") val chargingState: String? = null,
    @SerialName("charge_rate") val chargeRate: Double? = null,
    @SerialName("time_to_full_charge") val timeToFullCharge: Double? = null,
)

@Serializable
private data class VehicleState(
    val state: String? = null,
    @SerialName("tpms_pressure_fl") val tpmsPressureFl: Double? = null,
    @SerialName("tpms_pressure_fr") val tpmsPressureFr: Double? = null,
    @SerialName("tpms_pressure_rl") val tpmsPressureRl: Double? = null,
    @SerialName("tpms_pressure_rr") val tpmsPressureRr: Double? = null,
)

@Serializable
private data class ClimateState(
    @SerialName("inside_temp") val insideTemp: Double? = null,
    @SerialName("outside_temp") val outsideTemp: Double? = null,
)
