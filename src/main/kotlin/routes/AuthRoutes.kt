package routes

import kotlinx.serialization.Serializable
import auth.JwtConfig
import auth.requireRole
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import auth.UserRole
import auth.User

val users = listOf(
    User("admin", "1234", UserRole.ADMIN),
    User("client", "1234", UserRole.CLIENT),
    User("worker", "1234", UserRole.WORKER)
)

@Serializable
data class LoginRequest(val username: String, val password: String)

fun Route.authRoutes() {

    // Эндпоинт логина
    post("/login") {
        application.log.info("POST /login called") // Логируем факт вызова
        val loginRequest = try {
            call.receive<LoginRequest>()

        } catch (e: Exception) {
            application.log.error("Failed to parse login request: ${e.message}")
            call.respondText("Invalid request format")
            return@post
        }
        application.log.info("Login attempt: username=${loginRequest.username}")

        val user = users.find { it.username == loginRequest.username && it.password == loginRequest.password }
        if (user == null) {
            application.log.warn("Invalid login attempt for username=${loginRequest.username}")
            call.respondText("Invalid username or password")
            return@post
        }

        val token = JwtConfig.generateToken(user)
        application.log.info("User ${user.username} logged in successfully")
        call.respond(mapOf("token" to token))
    }

    authenticate("auth-jwt") {

        get("/tasks/admin") {
            application.log.info("GET /tasks/admin called")
            val principal = call.principal<JWTPrincipal>()
            if (!call.requireRole(UserRole.ADMIN, principal)) return@get
            call.respondText("Hello Admin! You can manage tasks.")
        }

        get("/tasks/client") {
            application.log.info("GET /tasks/client called")
            val principal = call.principal<JWTPrincipal>()
            if (!call.requireRole(UserRole.CLIENT, principal)) return@get
            call.respondText("Hello Client! You can view tasks.")
        }

        get("/tasks/worker") {
            application.log.info("GET /tasks/worker called")
            val principal = call.principal<JWTPrincipal>()
            if (!call.requireRole(UserRole.WORKER, principal)) return@get
            call.respondText("Hello Worker! You can complete tasks.")
        }
    }
}
