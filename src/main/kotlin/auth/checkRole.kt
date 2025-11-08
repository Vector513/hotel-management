package auth

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.response.*
import auth.UserRole

suspend fun ApplicationCall.requireRole(role: UserRole, principal: JWTPrincipal?): Boolean {
    val userRole = principal?.payload?.getClaim("role")?.asString()
    return if (userRole != role.name) {
        respondText("Forbidden: ${role.name} only")
        false
    } else true
}
