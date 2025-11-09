package com.example.routes

import com.example.database.dao.CleaningScheduleDao
import com.example.models.UserRole
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

                // Попробуем извлечь employeeId из токена, но если нет — используем userId
                val employeeId = principal.getClaim("employeeId", Int::class)
                    ?: principal.getClaim("userId", Int::class)

                if (employeeId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Missing employee ID in token")
                    return@get
                }

                val schedules = scheduleDao.findByEmployee(employeeId)
                call.respond(HttpStatusCode.OK, schedules)
            }
        }
    }
}
