package com.myt.backend.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class TokenRefreshRequest(
    val refreshToken: String,
)

@Serializable
data class TokenRefreshResponse(
    val accessToken: String,
    val expiresInSec: Int = 3600,
    val tokenType: String = "Bearer",
    val message: String = "Phase 2 auth proxy sandbox",
)

/** M26 — Auth proxy routes (Phase 2 sandbox). */
fun Route.authRoutes() {
    route("/auth") {
        get("/oauth/callback") {
            val code = call.request.queryParameters["code"]
            call.respond(
                mapOf(
                    "message" to "OAuth callback received",
                    "codePresent" to (code != null),
                    "next" to "exchange_code_for_tokens",
                ),
            )
        }
        post("/token/refresh") {
            val body = runCatching { call.receive<TokenRefreshRequest>() }.getOrNull()
            if (body == null || body.refreshToken.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "refreshToken required"))
                return@post
            }
            call.respond(
                TokenRefreshResponse(
                    accessToken = "sandbox-access-${body.refreshToken.take(8)}",
                ),
            )
        }
        get("/virtual-key/status") {
            call.respond(
                mapOf(
                    "registered" to false,
                    "message" to "Virtual key vault — Phase 2",
                ),
            )
        }
    }
}
