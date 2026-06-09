package com.astrax.server.repository

import com.astrax.server.db.ChatMembers
import com.astrax.server.db.Chats
import com.astrax.server.db.Messages
import com.astrax.server.db.Users
import com.astrax.server.model.AuthResponse
import com.astrax.server.model.ChatDto
import com.astrax.server.model.MessageDto
import com.astrax.server.model.MessageStatus
import com.astrax.server.model.UserDto
import com.astrax.server.routing.AppException
import com.astrax.server.security.JwtService
import com.astrax.server.security.PasswordHasher
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class AstraxRepository(private val jwtService: JwtService) {
    fun register(login: String, password: String): AuthResponse = transaction {
        val trimmedLogin = login.trim()
        val normalizedLogin = trimmedLogin.lowercase()
        // prevent duplicates ignoring case (store original case, use normalized column for checks)
        val existing = Users.selectAll().where { Users.normalizedLogin eq normalizedLogin }.singleOrNull()
        if (existing != null) throw AppException(HttpStatusCode.Conflict, "Login is already taken")
        val now = System.currentTimeMillis()
        val id = Users.insert {
            it[Users.login] = trimmedLogin
            it[Users.normalizedLogin] = normalizedLogin
            it[passwordHash] = PasswordHasher.hash(password)
            it[createdAt] = now
        }[Users.id]
        val user = UserDto(id, trimmedLogin)
        AuthResponse(jwtService.createToken(user), user)
    }

    fun login(login: String, password: String): AuthResponse = transaction {
        val normalizedLogin = login.trim().lowercase()
        val row = Users.selectAll().where { Users.normalizedLogin eq normalizedLogin }.singleOrNull()
            ?: throw AppException(HttpStatusCode.Unauthorized, "Invalid login or password")
        val hash = row[Users.passwordHash]
        if (!PasswordHasher.verify(password, hash)) {
            throw AppException(HttpStatusCode.Unauthorized, "Invalid login or password")
        }
        val user = row.toUserDto()
        AuthResponse(jwtService.createToken(user), user)
    }

    fun chats(userId: Long): List<ChatDto> = transaction {
        ChatMembers.selectAll().where { ChatMembers.userId eq userId }
            .map { it[ChatMembers.chatId] }
            .map { chatDto(it, userId) }
            .sortedByDescending { it.lastMessage?.createdAt ?: 0L }
    }

    fun createChat(userId: Long, peerLogin: String): ChatDto = transaction {
        val normalizedLogin = peerLogin.trim().lowercase()
        val peer = Users.selectAll().where { Users.normalizedLogin eq normalizedLogin }.singleOrNull()?.toUserDto()
            ?: throw AppException(HttpStatusCode.NotFound, "User not found")
        if (peer.id == userId) throw AppException(HttpStatusCode.BadRequest, "Cannot create chat with yourself")
        val existing = findDirectChat(userId, peer.id)
        val chatId = existing ?: createDirectChat(userId, peer.id)
        chatDto(chatId, userId)
    }

    fun searchUsers(userId: Long, query: String): List<UserDto> = transaction {
        val normalizedQuery = query.trim().lowercase()
        Users.selectAll()
            .where { (Users.normalizedLogin like "$normalizedQuery%") and (Users.id neq userId) }
            .orderBy(Users.login to SortOrder.ASC)
            .limit(10)
            .map { it.toUserDto() }
    }

    fun messages(chatId: Long, userId: Long): List<MessageDto> = transaction {
        requireMember(chatId, userId)
        val result = Messages.selectAll().where { Messages.chatId eq chatId }
            .orderBy(Messages.createdAt to SortOrder.ASC)
            .limit(200)
            .map { it.toMessageDto() }
        markIncomingRead(chatId, userId)
        result
    }

    fun sendMessage(chatId: Long, userId: Long, text: String): MessageDto = transaction {
        requireMember(chatId, userId)
        val blocked = ChatMembers.selectAll().where { ChatMembers.chatId eq chatId }
            .any { it[ChatMembers.blocked] }
        if (blocked) throw AppException(HttpStatusCode.Forbidden, "Chat is blocked")
        val now = System.currentTimeMillis()
        val id = Messages.insert {
            it[Messages.chatId] = chatId
            it[senderId] = userId
            it[Messages.text] = text
            it[status] = MessageStatus.sent.name
            it[createdAt] = now
        }[Messages.id]
        Messages.selectAll().where { Messages.id eq id }.single().toMessageDto()
    }

    fun setMuted(chatId: Long, userId: Long, enabled: Boolean): ChatDto = transaction {
        requireMember(chatId, userId)
        ChatMembers.update({ (ChatMembers.chatId eq chatId) and (ChatMembers.userId eq userId) }) {
            it[muted] = enabled
        }
        chatDto(chatId, userId)
    }

    fun setBlocked(chatId: Long, userId: Long, enabled: Boolean): ChatDto = transaction {
        requireMember(chatId, userId)
        ChatMembers.update({ (ChatMembers.chatId eq chatId) and (ChatMembers.userId eq userId) }) {
            it[blocked] = enabled
        }
        chatDto(chatId, userId)
    }

    fun deleteChat(chatId: Long, userId: Long) = transaction {
        requireMember(chatId, userId)
        Messages.deleteWhere { Messages.chatId eq chatId }
        ChatMembers.deleteWhere { ChatMembers.chatId eq chatId }
        Chats.deleteWhere { Chats.id eq chatId }
    }

    fun requireChatAccess(chatId: Long, userId: Long) = transaction {
        requireMember(chatId, userId)
    }

    fun markIncomingRead(chatId: Long, userId: Long): List<Long> = transaction {
        requireMember(chatId, userId)
        val incoming = Messages.selectAll().where { (Messages.chatId eq chatId) and (Messages.senderId neq userId) }
            .map { it[Messages.id] }
        val maxId = incoming.maxOrNull()
        if (maxId != null) {
            ChatMembers.update({ (ChatMembers.chatId eq chatId) and (ChatMembers.userId eq userId) }) {
                it[lastReadMessageId] = maxId
            }
            Messages.update({ (Messages.chatId eq chatId) and (Messages.senderId neq userId) }) {
                it[status] = MessageStatus.read.name
            }
        }
        incoming
    }

    private fun findDirectChat(userId: Long, peerId: Long): Long? {
        val userChatIds = ChatMembers.selectAll().where { ChatMembers.userId eq userId }
            .map { it[ChatMembers.chatId] }
        return userChatIds.firstOrNull { chatId ->
            val members = ChatMembers.selectAll().where { ChatMembers.chatId eq chatId }
                .map { it[ChatMembers.userId] }
            members.size == 2 && userId in members && peerId in members
        }
    }

    private fun createDirectChat(userId: Long, peerId: Long): Long {
        val now = System.currentTimeMillis()
        val chatId = Chats.insert {
            it[createdAt] = now
        }[Chats.id]
        ChatMembers.insert {
            it[ChatMembers.chatId] = chatId
            it[ChatMembers.userId] = userId
        }
        ChatMembers.insert {
            it[ChatMembers.chatId] = chatId
            it[ChatMembers.userId] = peerId
        }
        return chatId
    }

    private fun chatDto(chatId: Long, userId: Long): ChatDto {
        val self = ChatMembers.selectAll().where { (ChatMembers.chatId eq chatId) and (ChatMembers.userId eq userId) }
            .singleOrNull()
            ?: throw AppException(HttpStatusCode.Forbidden, "Chat access denied")
        val peerId = ChatMembers.selectAll().where { (ChatMembers.chatId eq chatId) and (ChatMembers.userId neq userId) }
            .single()[ChatMembers.userId]
        val peer = Users.selectAll().where { Users.id eq peerId }.single().toUserDto()
        val lastMessage = Messages.selectAll().where { Messages.chatId eq chatId }
            .orderBy(Messages.createdAt to SortOrder.DESC)
            .limit(1)
            .singleOrNull()
            ?.toMessageDto()
        val lastReadId = self[ChatMembers.lastReadMessageId] ?: 0L
        val unreadCount = Messages.selectAll().where {
            (Messages.chatId eq chatId) and
                (Messages.senderId neq userId) and
                (Messages.id greater lastReadId)
        }.count().toInt()
        return ChatDto(
            id = chatId,
            peer = peer,
            lastMessage = lastMessage,
            unreadCount = unreadCount,
            muted = self[ChatMembers.muted],
            blocked = self[ChatMembers.blocked]
        )
    }

    private fun requireMember(chatId: Long, userId: Long) {
        val member = ChatMembers.selectAll().where { (ChatMembers.chatId eq chatId) and (ChatMembers.userId eq userId) }
            .singleOrNull()
        if (member == null) throw AppException(HttpStatusCode.Forbidden, "Chat access denied")
    }

    private fun ResultRow.toUserDto(): UserDto =
        UserDto(
            id = this[Users.id],
            login = this[Users.login]
        )

    private fun ResultRow.toMessageDto(): MessageDto =
        MessageDto(
            id = this[Messages.id],
            chatId = this[Messages.chatId],
            senderId = this[Messages.senderId],
            text = this[Messages.text],
            createdAt = this[Messages.createdAt],
            status = MessageStatus.valueOf(this[Messages.status])
        )
}
