package routes

import auth.UserRole
import auth.requireRole
import database.dao.ClientDao
import database.dao.EmployeeDao
import database.dao.CleaningScheduleDao
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import models.CleaningSchedule

fun Route.adminRoutes() {
    val clientDao = ClientDao()
    val employeeDao = EmployeeDao()
    val scheduleDao = CleaningScheduleDao()

    authenticate("auth-jwt") { // ✅ Все admin эндпоинты требуют JWT
        route("/admin") {

            /**
             *  Освобождение номера клиента (удаляем клиента из БД)
             *  Пример: DELETE /admin/clearRoom/5
             */
            delete("/clearRoom/{id}") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@delete

                val clientId = call.parameters["id"]?.toIntOrNull()
                if (clientId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid client ID")
                    return@delete
                }

                val client = clientDao.findById(clientId)
                if (client == null) {
                    call.respond(HttpStatusCode.NotFound, "Client not found")
                    return@delete
                }

                clientDao.delete(clientId)
                call.respond(HttpStatusCode.OK, "Client removed, room is now free")
            }

            /**
             *  Увольнение сотрудника
             *  Пример: DELETE /admin/fireEmployee/3
             */
            delete("/fireEmployee/{id}") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@delete

                val employeeId = call.parameters["id"]?.toIntOrNull()
                if (employeeId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid employee ID")
                    return@delete
                }

                val employee = employeeDao.findById(employeeId)
                if (employee == null) {
                    call.respond(HttpStatusCode.NotFound, "Employee not found")
                    return@delete
                }

                // Удаляем все связанные записи о расписании уборок
                val schedules = scheduleDao.findByEmployee(employeeId)
                schedules.forEach { scheduleDao.delete(it.scheduleId) }

                // Удаляем самого сотрудника
                employeeDao.delete(employeeId)

                call.respond(HttpStatusCode.OK, "Employee #$employeeId fired and schedules removed")
            }

            /**
             *  Изменение расписания уборки
             *  Пример: PUT /admin/updateSchedule
             *  Тело запроса (JSON):
             *  {
             *      "scheduleId": 1,
             *      "employeeId": 2,
             *      "floor": 4,
             *      "dayOfWeek": "MONDAY"
             *  }
             */
            put("/updateSchedule") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@put

                val updated = try {
                    call.receive<CleaningSchedule>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid schedule data format")
                    return@put
                }

                val existing = scheduleDao.findById(updated.scheduleId)
                if (existing == null) {
                    call.respond(HttpStatusCode.NotFound, "Schedule not found")
                    return@put
                }

                val success = scheduleDao.update(updated.scheduleId, updated)

                if (success) {
                    call.respond(HttpStatusCode.OK, "Schedule #${updated.scheduleId} updated successfully")
                } else {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to update schedule")
                }
            }
        }
    }
}
