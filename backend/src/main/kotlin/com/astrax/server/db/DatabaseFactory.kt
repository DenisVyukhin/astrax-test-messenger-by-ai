package com.astrax.server.db

import com.astrax.server.config.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init(config: DatabaseConfig) {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.jdbcUrl
            if (config.jdbcUrl.startsWith("jdbc:sqlite")) {
                driverClassName = "org.sqlite.JDBC"
            }
            maximumPoolSize = 10
            isAutoCommit = false
            validate()
        }
        Database.connect(HikariDataSource(hikariConfig))
        transaction {
            SchemaUtils.create(Users, Chats, ChatMembers, Messages)
        }
    }
}
