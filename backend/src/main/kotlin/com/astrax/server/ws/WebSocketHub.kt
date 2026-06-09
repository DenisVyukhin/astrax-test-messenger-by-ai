package com.astrax.server.ws

import com.astrax.server.model.MessageEvent
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import io.ktor.websocket.send
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class WebSocketHub(private val json: Json) {
    private val sessions = ConcurrentHashMap<Long, MutableSet<DefaultWebSocketServerSession>>()

    fun add(chatId: Long, session: DefaultWebSocketServerSession) {
        sessions.computeIfAbsent(chatId) { ConcurrentHashMap.newKeySet() }.add(session)
    }

    fun remove(chatId: Long, session: DefaultWebSocketServerSession) {
        sessions[chatId]?.remove(session)
    }

    suspend fun broadcast(chatId: Long, event: MessageEvent) {
        val payload = json.encodeToString(event)
        sessions[chatId]?.forEach { session ->
            runCatching { session.send(Frame.Text(payload)) }
        }
    }
}
