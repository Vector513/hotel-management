package com.example

import com.example.auth.JwtConfig
import com.example.routes.authRoutes
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*

fun Application.configureSecurity() {

    install(Authentication) {
        jwt("auth-jwt") {
            realm = JwtConfig.realm
            verifier(JwtConfig.getVerifier())
            validate { credential ->
                val username = credential.payload.getClaim("username").asString()
                val role = credential.payload.getClaim("role").asString()
                if (username != null && role != null) JWTPrincipal(credential.payload) else null
            }
        }
    }
}
