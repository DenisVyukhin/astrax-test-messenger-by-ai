package com.astrax.app.data

import com.astrax.app.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class ApiClient(private val tokenStore: TokenStore) {
    private val baseUrl = BuildConfig.ASTRAX_BASE_URL.trimEnd('/')
    private val socketUrl = baseUrl
        .replaceFirst("https://", "wss://")
        .replaceFirst("http://", "ws://")
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        install(WebSockets)
        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
            tokenStore.token()?.let { bearerAuth(it) }
        }
    }

    suspend fun register(login: String, password: String): AuthResponse =
        client.post("/auth/register") { setBody(AuthRequest(login, password)) }.parsed()

    suspend fun login(login: String, password: String): AuthResponse =
        client.post("/auth/login") { setBody(AuthRequest(login, password)) }.parsed()

    suspend fun chats(): List<ChatDto> =
        client.get("/chats").parsed()

    suspend fun searchUsers(query: String): List<UserDto> =
        client.get("/users/search?query=$query").parsed()

    suspend fun createChat(login: String): ChatDto =
        client.post("/chats") { setBody(CreateChatRequest(login)) }.parsed()

    suspend fun messages(chatId: Long): List<MessageDto> =
        client.get("/chats/$chatId/messages").parsed()

    suspend fun sendMessage(chatId: Long, text: String): MessageDto =
        client.post("/chats/$chatId/messages") { setBody(SendMessageRequest(text)) }.parsed()

    suspend fun setMuted(chatId: Long, enabled: Boolean): ChatDto =
        client.patch("/chats/$chatId/mute") { setBody(ChatFlagRequest(enabled)) }.parsed()

    suspend fun setBlocked(chatId: Long, enabled: Boolean): ChatDto =
        client.patch("/chats/$chatId/block") { setBody(ChatFlagRequest(enabled)) }.parsed()

    suspend fun deleteChat(chatId: Long) {
        client.delete("/chats/$chatId").ensureSuccess()
    }

    suspend fun chatSocket(chatId: Long): DefaultClientWebSocketSession =
        client.webSocketSession("$socketUrl/ws/chats/$chatId") {
            tokenStore.token()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }

    private suspend inline fun <reified T> HttpResponse.parsed(): T {
        ensureSuccess()
        return body()
    }

    private suspend fun HttpResponse.ensureSuccess() {
        if (status.value in 200..299) return
        val text = bodyAsText()
        val message = runCatching { json.decodeFromString<ErrorResponse>(text).message }.getOrNull()
        throw IllegalStateException(message ?: "HTTP ${status.value}: ${status.description}")
    }
}
