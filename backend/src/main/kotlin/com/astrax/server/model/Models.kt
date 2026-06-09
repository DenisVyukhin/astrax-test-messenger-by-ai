package com.astrax.server.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(val login: String, val password: String)

@Serializable
data class AuthResponse(val token: String, val user: UserDto)

@Serializable
data class UserDto(val id: Long, val login: String)

@Serializable
data class CreateChatRequest(val login: String)

@Serializable
data class ChatDto(
    val id: Long,
    val peer: UserDto,
    val lastMessage: MessageDto?,
    val unreadCount: Int,
    val muted: Boolean,
    val blocked: Boolean
)

@Serializable
data class MessageDto(
    val id: Long,
    val chatId: Long,
    val senderId: Long,
    val text: String,
    val createdAt: Long,
    val status: MessageStatus
)

@Serializable
data class SendMessageRequest(val text: String, val clientMessageId: String? = null)

@Serializable
data class ChatFlagRequest(val enabled: Boolean)

@Serializable
data class MessageEvent(
    val type: String,
    val message: MessageDto? = null,
    val chatId: Long? = null,
    val messageIds: List<Long> = emptyList()
)

@Serializable
enum class MessageStatus {
    sending,
    sent,
    read
}

@Serializable
data class ErrorResponse(val message: String)
