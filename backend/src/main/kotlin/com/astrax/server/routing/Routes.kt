package com.astrax.server.routing

import com.astrax.server.model.AuthRequest
import com.astrax.server.model.ChatFlagRequest
import com.astrax.server.model.CreateChatRequest
import com.astrax.server.model.ErrorResponse
import com.astrax.server.model.MessageEvent
import com.astrax.server.model.SendMessageRequest
import com.astrax.server.repository.AstraxRepository
import com.astrax.server.security.JwtService
import com.astrax.server.security.RateLimiter
import com.astrax.server.security.Validation
import com.astrax.server.security.userId
import com.astrax.server.ws.WebSocketHub
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json

fun Application.configureRoutes(
    repository: AstraxRepository,
    hub: WebSocketHub,
    jwtService: JwtService,
    json: Json
) {
    val authLimiter = RateLimiter(8, 60_000)
    val messageLimiter = RateLimiter(30, 60_000)

    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }

        route("/auth") {
            post("/register") {
                val ip = call.request.local.remoteHost
                if (!authLimiter.allow("register:$ip")) throw AppException(HttpStatusCode.TooManyRequests, "Too many requests")
                val request = call.receive<AuthRequest>()
                val login = Validation.login(request.login)
                    ?: throw AppException(HttpStatusCode.BadRequest, "Login must be 3-32 letters, digits or underscores")
                val password = Validation.password(request.password)
                    ?: throw AppException(HttpStatusCode.BadRequest, "Password must be 8-128 characters")
                call.respond(repository.register(login, password))
            }

            post("/login") {
                val ip = call.request.local.remoteHost
                if (!authLimiter.allow("login:$ip")) throw AppException(HttpStatusCode.TooManyRequests, "Too many requests")
                val request = call.receive<AuthRequest>()
                val login = Validation.login(request.login)
                    ?: throw AppException(HttpStatusCode.BadRequest, "Invalid login")
                val password = Validation.password(request.password)
                    ?: throw AppException(HttpStatusCode.BadRequest, "Invalid password")
                call.respond(repository.login(login, password))
            }
        }

        authenticate("auth") {
            get("/users/search") {
                val query = call.request.queryParameters["query"]
                    ?.let(Validation::loginQuery)
                    ?: throw AppException(HttpStatusCode.BadRequest, "Invalid query")
                call.respond(repository.searchUsers(call.userId(), query))
            }

            get("/chats") {
                call.respond(repository.chats(call.userId()))
            }

            post("/chats") {
                val request = call.receive<CreateChatRequest>()
                val login = Validation.login(request.login)
                    ?: throw AppException(HttpStatusCode.BadRequest, "Invalid login")
                call.respond(HttpStatusCode.Created, repository.createChat(call.userId(), login))
            }

            get("/chats/{id}/messages") {
                val chatId = call.parameters["id"]?.toLongOrNull()
                    ?: throw AppException(HttpStatusCode.BadRequest, "Invalid chat id")
                call.respond(repository.messages(chatId, call.userId()))
            }

            post("/chats/{id}/messages") {
                val chatId = call.parameters["id"]?.toLongOrNull()
                    ?: throw AppException(HttpStatusCode.BadRequest, "Invalid chat id")
                val userId = call.userId()
                if (!messageLimiter.allow("message:$userId")) throw AppException(HttpStatusCode.TooManyRequests, "Too many messages")
                val request = call.receive<SendMessageRequest>()
                val text = Validation.message(request.text)
                    ?: throw AppException(HttpStatusCode.BadRequest, "Invalid message")
                val message = repository.sendMessage(chatId, userId, text)
                hub.broadcast(chatId, MessageEvent(type = "message", message = message, chatId = chatId))
                call.respond(HttpStatusCode.Created, message)
            }

            patch("/chats/{id}/mute") {
                val chatId = call.parameters["id"]?.toLongOrNull()
                    ?: throw AppException(HttpStatusCode.BadRequest, "Invalid chat id")
                val request = call.receive<ChatFlagRequest>()
                call.respond(repository.setMuted(chatId, call.userId(), request.enabled))
            }

            patch("/chats/{id}/block") {
                val chatId = call.parameters["id"]?.toLongOrNull()
                    ?: throw AppException(HttpStatusCode.BadRequest, "Invalid chat id")
                val request = call.receive<ChatFlagRequest>()
                call.respond(repository.setBlocked(chatId, call.userId(), request.enabled))
            }

            delete("/chats/{id}") {
                val chatId = call.parameters["id"]?.toLongOrNull()
                    ?: throw AppException(HttpStatusCode.BadRequest, "Invalid chat id")
                repository.deleteChat(chatId, call.userId())
                call.respond(HttpStatusCode.NoContent)
            }
        }

        webSocket("/ws/chats/{id}") {
            val chatId = call.parameters["id"]?.toLongOrNull()
            if (chatId == null) {
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid chat id"))
                return@webSocket
            }
            val userId = call.request.headers[HttpHeaders.Authorization]
                ?.removePrefix("Bearer ")
                ?.let { token -> runCatching { jwtService.verifier.verify(token).subject.toLongOrNull() }.getOrNull() }
            if (userId == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
                return@webSocket
            }
            val access = runCatching { repository.requireChatAccess(chatId, userId) }
            if (access.isFailure) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Chat access denied"))
                return@webSocket
            }
            hub.add(chatId, this)
            val readIds = repository.markIncomingRead(chatId, userId)
            if (readIds.isNotEmpty()) {
                hub.broadcast(chatId, MessageEvent(type = "read", chatId = chatId, messageIds = readIds))
            }
            try {
                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    if (!messageLimiter.allow("message:$userId")) {
                        send(Frame.Text(json.encodeToString(ErrorResponse.serializer(), ErrorResponse("Too many messages"))))
                        continue
                    }
                    val request = runCatching { json.decodeFromString(SendMessageRequest.serializer(), frame.readText()) }.getOrNull()
                        ?: continue
                    val text = Validation.message(request.text) ?: continue
                    val message = repository.sendMessage(chatId, userId, text)
                    hub.broadcast(chatId, MessageEvent(type = "message", message = message, chatId = chatId))
                }
            } finally {
                hub.remove(chatId, this)
            }
        }
    }
}
