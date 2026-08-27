package com.myt.data.fleet

import com.myt.config.TeslaConfig
import com.myt.platform.CryptoPlatform
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Parameters
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class TeslaOAuthClient(
    private val httpClient: HttpClient,
    private val configProvider: () -> TeslaConfig,
) {
    private val config: TeslaConfig get() = configProvider()
    private var pendingCodeVerifier: String? = null

    @OptIn(ExperimentalEncodingApi::class)
    fun buildAuthorizationUrl(state: String): String {
        require(config.redirectUri.isNotBlank()) {
            "OAuth redirectUri missing — set tesla.oauth.redirect.uri"
        }
        val verifier = generateCodeVerifier()
        pendingCodeVerifier = verifier
        val challenge = codeChallenge(verifier)
        val params = buildList {
            add("client_id" to encode(config.clientId))
            // Tesla Fleet OAuth requires an allow-listed redirect_uri (typically https://…).
            // Custom schemes like myt:// often surface as "redirect_url was not provided".
            add("redirect_uri" to encode(config.redirectUri))
            add("response_type" to "code")
            add("scope" to encode(config.scopes))
            add("state" to encode(state))
            add("code_challenge" to encode(challenge))
            add("code_challenge_method" to "S256")
        }.joinToString("&") { "${it.first}=${it.second}" }
        return "${config.authorizeUrl}?$params"
    }

    suspend fun exchangeAuthorizationCode(code: String): TeslaTokenResponse {
        val verifier = pendingCodeVerifier
            ?: error("OAuth code_verifier missing — restart login")
        pendingCodeVerifier = null
        require(config.redirectUri.isNotBlank()) {
            "OAuth redirectUri missing — set tesla.oauth.redirect.uri"
        }

        return httpClient.post(config.tokenUrl) {
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("grant_type", "authorization_code")
                        append("client_id", config.clientId)
                        append("client_secret", config.clientSecret)
                        append("code", code)
                        append("redirect_uri", config.redirectUri)
                        append("code_verifier", verifier)
                        append("audience", config.fleetApiBase)
                    },
                ),
            )
        }.body()
    }

    suspend fun refreshAccessToken(refreshToken: String): TeslaTokenResponse =
        httpClient.post(config.tokenUrl) {
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("grant_type", "refresh_token")
                        append("client_id", config.clientId)
                        append("client_secret", config.clientSecret)
                        append("refresh_token", refreshToken)
                        append("audience", config.fleetApiBase)
                    },
                ),
            )
        }.body()

    suspend fun fetchPartnerToken(): TeslaTokenResponse =
        httpClient.post(config.tokenUrl) {
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("grant_type", "client_credentials")
                        append("client_id", config.clientId)
                        append("client_secret", config.clientSecret)
                        append("audience", config.fleetApiBase)
                        append("scope", config.scopes)
                    },
                ),
            )
        }.body()

    @OptIn(ExperimentalEncodingApi::class)
    private fun generateCodeVerifier(): String {
        val bytes = CryptoPlatform.secureRandomBytes(32)
        return Base64.UrlSafe.encode(bytes).trimEnd('=')
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun codeChallenge(verifier: String): String {
        val digest = CryptoPlatform.sha256(verifier.encodeToByteArray())
        return Base64.UrlSafe.encode(digest).trimEnd('=')
    }

    private fun encode(value: String): String =
        value.encodeToByteArray().joinToString("") { byte ->
            when {
                byte in 'a'.code.toByte()..'z'.code.toByte() ||
                    byte in 'A'.code.toByte()..'Z'.code.toByte() ||
                    byte in '0'.code.toByte()..'9'.code.toByte() ||
                    byte == '-'.code.toByte() ||
                    byte == '_'.code.toByte() ||
                    byte == '.'.code.toByte() ||
                    byte == '~'.code.toByte() -> byte.toInt().toChar().toString()
                // Prefer %20 over + for query-string compatibility with Tesla authorize.
                byte == ' '.code.toByte() -> "%20"
                else -> "%${byte.toUByte().toString(16).uppercase().padStart(2, '0')}"
            }
        }
}

@Serializable
data class TeslaTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("token_type") val tokenType: String? = null,
)
