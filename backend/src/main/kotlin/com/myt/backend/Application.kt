package com.myt.backend

import com.myt.backend.routes.apiRoutes
import com.myt.backend.routes.authRoutes
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

/** M25 — Phase 2 backend entry (skeleton). */
fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; prettyPrint = true })
    }
    routing {
        get("/health") { call.respond(mapOf("status" to "ok", "service" to "myt-backend")) }
        authRoutes()
        apiRoutes()
    }
}
