package com.astrax.server.routing

import io.ktor.http.HttpStatusCode

class AppException(
    val status: HttpStatusCode,
    override val message: String
) : RuntimeException(message)
