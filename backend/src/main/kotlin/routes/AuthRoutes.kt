// AuthRoutes.kt
package com.example.routes

import kotlinx.serialization.Serializable
import com.example.auth.JwtConfig
import com.example.auth.requireRole
import com.example.models.User
import com.example.models.UserRole
import com.example.database.dao.UserDao
import com.example.database.dao.ClientDao
import com.example.database.dao.RoomDao
import com.example.database.dao.EmployeeDao
import com.example.database.dao.CleaningScheduleDao
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*

private val userDao = UserDao() // DAO для работы с таблицей пользователей
private val clientDao = ClientDao()
private val roomDao = RoomDao()
private val employeeDao = EmployeeDao()
private val scheduleDao = CleaningScheduleDao()

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
            role = roleEnum,
            clientId = userEntity.clientId,
            employeeId = userEntity.employeeId
        )

        // Получаем ФИО пользователя в зависимости от роли
        var fullName: String? = null
        when (roleEnum) {
            UserRole.CLIENT -> {
                userEntity.clientId?.let { clientId ->
                    val client = clientDao.findById(clientId)
                    fullName = client?.fullName
                }
            }
            UserRole.WORKER -> {
                userEntity.employeeId?.let { employeeId ->
                    val employee = employeeDao.findById(employeeId)
                    fullName = employee?.fullName
                }
            }
            UserRole.ADMIN -> {
                fullName = "Администратор" // Для админа используем роль как ФИО
            }
        }

        val token = try {
            JwtConfig.generateToken(user, user.clientId, user.employeeId, fullName)
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

        // Эндпоинт для получения ФИО уборщика клиента в заданный день недели
        // Доступен всем авторизованным пользователям
        get("/clients/{clientId}/cleaner") {
            application.log.info("GET /clients/{clientId}/cleaner called")

            val clientId = call.parameters["clientId"]?.toIntOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, "Missing or invalid client ID")
                return@get
            }

            val dayOfWeek = call.request.queryParameters["dayOfWeek"] ?: run {
                call.respond(HttpStatusCode.BadRequest, "Missing day of week parameter")
                return@get
            }

            // Получаем клиента
            val client = clientDao.findById(clientId)
            if (client == null) {
                call.respond(HttpStatusCode.NotFound, "Client not found")
                return@get
            }

            // Проверяем, что у клиента есть номер
            if (client.roomId == null) {
                call.respond(HttpStatusCode.BadRequest, "Client is not assigned to a room")
                return@get
            }

            // Получаем номер клиента
            val room = roomDao.findById(client.roomId)
            if (room == null) {
                call.respond(HttpStatusCode.NotFound, "Room not found")
                return@get
            }

            // Ищем расписание уборки для этажа в указанный день недели
            val schedule = scheduleDao.getAll().find { schedule ->
                schedule.dayOfWeek.name.equals(dayOfWeek, ignoreCase = true) &&
                        schedule.floor == room.floor
            }

            if (schedule == null) {
                call.respond(HttpStatusCode.NotFound, "No cleaning schedule found for floor ${room.floor} on $dayOfWeek")
                return@get
            }

            // Получаем информацию о сотруднике
            val employee = employeeDao.findById(schedule.employeeId)
            if (employee == null) {
                call.respond(HttpStatusCode.NotFound, "Employee not found")
                return@get
            }

            application.log.info("Found cleaner for client $clientId: ${employee.fullName}")
            call.respond(HttpStatusCode.OK, mapOf("employeeName" to employee.fullName))
        }

        // Эндпоинт для получения ФИО уборщика номера в заданный день недели
        // Доступен всем авторизованным пользователям
        get("/rooms/{roomId}/cleaner") {
            application.log.info("GET /rooms/{roomId}/cleaner called")

            val roomId = call.parameters["roomId"]?.toIntOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, "Missing or invalid room ID")
                return@get
            }

            val dayOfWeek = call.request.queryParameters["dayOfWeek"] ?: run {
                call.respond(HttpStatusCode.BadRequest, "Missing day of week parameter")
                return@get
            }

            // Получаем номер
            val room = roomDao.findById(roomId)
            if (room == null) {
                call.respond(HttpStatusCode.NotFound, "Room not found")
                return@get
            }

            // Ищем расписание уборки для этажа в указанный день недели
            val schedule = scheduleDao.getAll().find { schedule ->
                schedule.dayOfWeek.name.equals(dayOfWeek, ignoreCase = true) &&
                        schedule.floor == room.floor
            }

            if (schedule == null) {
                call.respond(HttpStatusCode.NotFound, "No cleaning schedule found for floor ${room.floor} on $dayOfWeek")
                return@get
            }

            // Получаем информацию о сотруднике
            val employee = employeeDao.findById(schedule.employeeId)
            if (employee == null) {
                call.respond(HttpStatusCode.NotFound, "Employee not found")
                return@get
            }

            application.log.info("Found cleaner for room $roomId: ${employee.fullName}")
            call.respond(HttpStatusCode.OK, mapOf("employeeName" to employee.fullName))
        }
    }
}
