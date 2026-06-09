package com.astrax.server

import com.astrax.server.config.loadServerConfig
import com.astrax.server.db.DatabaseFactory
import com.astrax.server.model.ErrorResponse
import com.astrax.server.repository.AstraxRepository
import com.astrax.server.routing.AppException
import com.astrax.server.routing.configureRoutes
import com.astrax.server.security.JwtService
import com.astrax.server.ws.WebSocketHub
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.websocket.WebSockets
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

fun Application.module() {
    val config = loadServerConfig()
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    DatabaseFactory.init(config.database)
    val jwtService = JwtService(config.jwt)
    val repository = AstraxRepository(jwtService)
    val hub = WebSocketHub(json)

    install(ContentNegotiation) {
        json(json)
    }
    install(WebSockets) {
        pingPeriodMillis = 20.seconds.inWholeMilliseconds
        timeoutMillis = 30.seconds.inWholeMilliseconds
        maxFrameSize = 2048
        masking = false
    }
    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        config.corsAllowedHosts.forEach { host ->
            val schemes = if (host.contains(":443") || !host.contains(':')) {
                listOf("https", "http")
            } else {
                listOf("http", "https")
            }
            allowHost(host, schemes = schemes)
        }
    }
    install(StatusPages) {
        exception<AppException> { call, cause ->
            call.respond(cause.status, ErrorResponse(cause.message))
        }
        exception<Throwable> { call, _ ->
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Internal server error"))
        }
    }
    install(Authentication) {
        jwt("auth") {
            realm = config.jwt.realm
            verifier(jwtService.verifier)
            validate { credential ->
                val subject = credential.payload.subject
                if (subject?.toLongOrNull() != null) io.ktor.server.auth.jwt.JWTPrincipal(credential.payload) else null
            }
        }
    }

    configureRoutes(repository, hub, jwtService, json)
}
