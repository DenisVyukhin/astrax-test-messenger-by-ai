package com.astrax.server.db

import org.jetbrains.exposed.sql.Table

object Users : Table("users") {
    val id = long("id").autoIncrement()
    // original-cased login (kept for display)
    val login = varchar("login", 32)
    // normalized (lowercased) login for case-insensitive lookups and uniqueness
    val normalizedLogin = varchar("normalized_login", 32).uniqueIndex()
    val passwordHash = varchar("password_hash", 128)
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}

object Chats : Table("chats") {
    val id = long("id").autoIncrement()
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}

object ChatMembers : Table("chat_members") {
    val chatId = long("chat_id").references(Chats.id)
    val userId = long("user_id").references(Users.id)
    val muted = bool("muted").default(false)
    val blocked = bool("blocked").default(false)
    val lastReadMessageId = long("last_read_message_id").nullable()
    override val primaryKey = PrimaryKey(chatId, userId)
}

object Messages : Table("messages") {
    val id = long("id").autoIncrement()
    val chatId = long("chat_id").references(Chats.id)
    val senderId = long("sender_id").references(Users.id)
    val text = varchar("text", 2000)
    val status = varchar("status", 16)
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}
