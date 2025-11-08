// JwtConfig.kt
package com.example.auth

import com.example.models.User
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

object JwtConfig {
    private const val secret = "supersecretkey"
    private const val issuer = "ktor-server"
    private const val audience = "ktor-audience"
    const val realm = "ktor-realm"

    private val algorithm = Algorithm.HMAC256(secret)

    fun generateToken(user: User): String {
        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("username", user.username)
            .withClaim("role", user.role.name) // enum -> String для токена
            .withExpiresAt(Date(System.currentTimeMillis() + 30 * 60 * 1000)) // 30 минут
            .sign(algorithm)
    }

    fun getVerifier() = JWT
        .require(algorithm)
        .withAudience(audience)
        .withIssuer(issuer)
        .build()
}
