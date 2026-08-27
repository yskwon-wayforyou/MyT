package com.myt.backend.routes

import com.myt.backend.state.DashboardStateStore
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class VehicleSummary(
    val vin: String,
    val displayName: String,
    val model: String = "Model 3",
)

@Serializable
data class VehicleControlRequest(
    val action: String,
    val value: String? = null,
)

@Serializable
data class VehicleControlResponse(
    val ok: Boolean,
    val action: String,
    val message: String,
)

@Serializable
data class AutomationRule(
    val id: String,
    val name: String,
    val trigger: String,
    val action: String,
    val enabled: Boolean = true,
)

private val automationRules = mutableListOf(
    AutomationRule("auto-1", "충전 완료 알림", "charge_complete", "push"),
    AutomationRule("auto-2", "저온 프리컨디션", "outside_temp_below_5", "climate_on"),
    AutomationRule("auto-3", "출발 전 해동", "weekday_07_00", "defrost"),
    AutomationRule("auto-4", "Sentry 야간", "time_22_00", "sentry_on"),
    AutomationRule("auto-5", "주차 위치 저장", "gear_park", "save_location"),
)

/** M28 / M29 / M32 — MyT REST API (Phase 2 scaffold with demo data). */
fun Route.apiRoutes() {
    route("/api/v1") {
        get("/vehicles") {
            val s = DashboardStateStore.state
            call.respond(listOf(VehicleSummary(vin = s.vin, displayName = s.displayName)))
        }
        get("/vehicles/{vin}") {
            val vin = call.parameters["vin"]
            val s = DashboardStateStore.state
            if (vin != s.vin) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "vehicle_not_found"))
            } else {
                call.respond(
                    mapOf(
                        "vin" to s.vin,
                        "displayName" to s.displayName,
                        "model" to "Model 3",
                        "state" to mapOf(
                            "soc" to s.soc,
                            "speedKmh" to s.speedKmh,
                            "locked" to s.locked,
                            "climateOn" to s.climateOn,
                            "isCharging" to s.isCharging,
                            "rangeKm" to s.rangeKm,
                        ),
                    ),
                )
            }
        }
        post("/vehicles/{vin}/command") {
            val vin = call.parameters["vin"].orEmpty()
            val body = call.receive<VehicleControlRequest>()
            val allowed = setOf("lock", "unlock", "climate_on", "climate_off", "trunk", "frunk", "flash", "honk")
            if (body.action !in allowed) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    VehicleControlResponse(false, body.action, "unsupported action"),
                )
                return@post
            }
            DashboardStateStore.update { current ->
                when (body.action) {
                    "lock" -> current.copy(locked = true)
                    "unlock" -> current.copy(locked = false)
                    "climate_on" -> current.copy(climateOn = true)
                    "climate_off" -> current.copy(climateOn = false)
                    else -> current
                }
            }
            call.respond(
                VehicleControlResponse(
                    ok = true,
                    action = body.action,
                    message = "Accepted for $vin (Phase 2 scaffold — Fleet command proxy pending)",
                ),
            )
        }
        route("/users/{userId}") {
            get {
                call.respond(
                    mapOf(
                        "userId" to call.parameters["userId"],
                        "message" to "User profile — Phase 2",
                    ),
                )
            }
        }
        get("/automations") {
            call.respond(automationRules)
        }
        post("/automations") {
            val rule = call.receive<AutomationRule>()
            automationRules += rule
            call.respond(HttpStatusCode.Created, rule)
        }
        route("/subscriptions") {
            get {
                call.respond(
                    mapOf(
                        "plan" to "free",
                        "message" to "Billing sandbox — Phase 2 (M37)",
                    ),
                )
            }
        }
    }
}
