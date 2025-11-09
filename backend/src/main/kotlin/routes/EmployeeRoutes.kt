package com.example.routes

import com.example.database.dao.CleaningScheduleDao
import com.example.models.UserRole
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal

fun Route.employeeRoutes() {
    val scheduleDao = CleaningScheduleDao()

    authenticate("auth-jwt") {
        route("/employee") {

            /** ---------------- Своё расписание ---------------- */
            get("/mySchedule") {
                val principal = call.principal<JWTPrincipal>() ?: run {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@get
                }

                val role = principal.getClaim("role", String::class)
                if (role != UserRole.WORKER.name) {
                    call.respond(HttpStatusCode.Forbidden, "Access denied")
                    return@get
                }

                // Извлекаем employeeId из токена
                val employeeId = principal.getClaim("employeeId", Int::class)

                if (employeeId == null) {
                    application.log.warn("Employee ID not found in token for user ${principal.getClaim("username", String::class)}")
                    call.respond(HttpStatusCode.BadRequest, "Missing employee ID in token. Please log in again.")
                    return@get
                }

                application.log.info("Fetching schedule for employee ID: $employeeId")
                val schedules = scheduleDao.findByEmployee(employeeId)
                application.log.info("Found ${schedules.size} schedule entries for employee $employeeId")
                call.respond(HttpStatusCode.OK, schedules)
            }
        }
    }
}
