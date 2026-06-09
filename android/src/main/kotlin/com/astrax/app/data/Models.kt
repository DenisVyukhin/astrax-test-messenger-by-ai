package com.astrax.app.data

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
    val lastMessage: MessageDto? = null,
    val unreadCount: Int = 0,
    val muted: Boolean = false,
    val blocked: Boolean = false
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
data class ErrorResponse(val message: String)

@Serializable
enum class MessageStatus {
    sending,
    sent,
    read
}
