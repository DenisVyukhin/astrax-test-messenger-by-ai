package com.astrax.server.security

object Validation {
    private val loginRegex = Regex("^[a-zA-Z0-9_]{3,32}$")
    private val loginQueryRegex = Regex("^[a-zA-Z0-9_]{1,32}$")

    fun login(value: String): String? {
        val normalized = value.trim()
        return normalized.takeIf { loginRegex.matches(it) }
    }

    fun loginQuery(value: String): String? {
        val normalized = value.trim()
        return normalized.takeIf { loginQueryRegex.matches(it) }
    }

    fun password(value: String): String? =
        value.takeIf { it.length in 8..128 }

    fun message(value: String): String? {
        val normalized = value.trim()
        return normalized.takeIf { it.isNotBlank() && it.length <= 2000 }
    }
}
