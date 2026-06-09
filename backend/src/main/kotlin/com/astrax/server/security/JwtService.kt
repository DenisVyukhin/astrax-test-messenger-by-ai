package com.astrax.server.security

import com.astrax.server.config.JwtConfig
import com.astrax.server.model.UserDto
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

class JwtService(private val config: JwtConfig) {
    private val algorithm = Algorithm.HMAC256(config.secret)

    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .build()

    fun createToken(user: UserDto): String {
        val now = System.currentTimeMillis()
        return JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject(user.id.toString())
            .withClaim("login", user.login)
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + config.ttlMillis))
            .sign(algorithm)
    }
}
