package com.astrax.server.config

import com.typesafe.config.ConfigFactory
import kotlin.time.Duration.Companion.milliseconds

data class JwtConfig(
    val issuer: String,
    val audience: String,
    val realm: String,
    val secret: String,
    val ttlMillis: Long
)

data class DatabaseConfig(val jdbcUrl: String)

data class ServerConfig(
    val jwt: JwtConfig,
    val database: DatabaseConfig,
    val corsAllowedHosts: List<String>
)

fun loadServerConfig(): ServerConfig {
    val config = ConfigFactory.load()
    val root = config.getConfig("astrax")
    val jwt = root.getConfig("jwt")
    val database = root.getConfig("database")
    val secret = if (jwt.hasPath("secret")) jwt.getString("secret") else "dev-only-change-this-secret"
    val jdbcUrl = if (database.hasPath("jdbcUrl")) database.getString("jdbcUrl") else "jdbc:sqlite:astrax.db"
    val corsHosts = System.getenv("ASTRAX_CORS_HOSTS")
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: listOf(
            "localhost:8080",
            "127.0.0.1:8080",
            "10.0.2.2:8080"
        )
    return ServerConfig(
        jwt = JwtConfig(
            issuer = jwt.getString("issuer"),
            audience = jwt.getString("audience"),
            realm = jwt.getString("realm"),
            secret = secret,
            ttlMillis = jwt.getLong("ttlMillis").milliseconds.inWholeMilliseconds
        ),
        database = DatabaseConfig(jdbcUrl),
        corsAllowedHosts = corsHosts
    )
}
