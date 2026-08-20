package com.myt.backend.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/** M26 — Auth proxy routes (Phase 2 stub). */
fun Route.authRoutes() {
    route("/auth") {
        get("/oauth/callback") {
            call.respond(mapOf("message" to "OAuth callback — Phase 2 implementation pending"))
        }
        post("/token/refresh") {
            call.respond(mapOf("message" to "Token refresh — Phase 2 implementation pending"))
        }
    }
}
