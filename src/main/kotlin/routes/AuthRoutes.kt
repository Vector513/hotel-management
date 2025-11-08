// AuthRoutes.kt
package com.example.routes

import kotlinx.serialization.Serializable
import com.example.auth.JwtConfig
import com.example.auth.requireRole
import com.example.models.User
import com.example.models.UserRole
import com.example.database.dao.UserDao
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private val userDao = UserDao() // DAO для работы с таблицей пользователей

@Serializable
data class LoginRequest(val username: String, val password: String)

fun Route.authRoutes() {

    post("/login") {
        application.log.info("POST /login called")

        val loginRequest = try {
            call.receive<LoginRequest>()
        } catch (e: Exception) {
            application.log.error("Failed to parse login request: ${e.message}")
            call.respondText("Invalid request format")
            return@post
        }

        val userEntity = try {
            userDao.findByUsername(loginRequest.username)
        } catch (e: Exception) {
            application.log.error("Database error while fetching user: ${e.message}")
            call.respondText("Internal server error")
            return@post
        }

        if (userEntity == null) {
            application.log.warn("User not found: ${loginRequest.username}")
            call.respondText("Invalid username or password")
            return@post
        }

        if (!userDao.verifyPassword(userEntity, loginRequest.password)) {
            application.log.warn("Incorrect password for username=${loginRequest.username}")
            call.respondText("Invalid username or password")
            return@post
        }

        application.log.info("User role from DB: ${userEntity.role}")

        // Безопасная конверсия роли из строки в enum
        val roleEnum = try {
            userEntity.role // роли в БД уже в капсе
        } catch (e: Exception) {
            application.log.error("Invalid role in DB for user ${userEntity.username}: ${userEntity.role}")
            call.respondText("Server misconfiguration")
            return@post
        }


        val user = User(
            id = userEntity.id,
            username = userEntity.username,
            passwordHash = userEntity.passwordHash,
            role = roleEnum
        )

        val token = try {
            JwtConfig.generateToken(user)
        } catch (e: Exception) {
            application.log.error("Failed to generate JWT for user ${user.username}: ${e.message}")
            call.respondText("Internal server error")
            return@post
        }

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
