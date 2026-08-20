package com.myt.backend.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/** M28 — MyT REST API routes (Phase 2 stub). */
fun Route.apiRoutes() {
    route("/api/v1") {
        get("/vehicles") {
            call.respond(emptyList<Any>())
        }
        route("/users/{userId}") {
            get {
                call.respond(mapOf("message" to "User API — Phase 2"))
            }
        }
        route("/subscriptions") {
            get {
                call.respond(mapOf("message" to "Billing — Phase 2 (M37)"))
            }
        }
    }
}
