package routes

import database.dao.CleaningScheduleDao
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import models.CleaningSchedule

fun Route.cleaningRoutes() {
    val dao = CleaningScheduleDao()

    route("/cleaning") {
        get { call.respond(dao.getAll()) }

        get("/{employeeId}") {
            val id = call.parameters["employeeId"]?.toIntOrNull()
            if (id == null) return@get call.respond(HttpStatusCode.BadRequest)
            call.respond(dao.findByEmployee(id))
        }

        post {
            val schedule = call.receive<CleaningSchedule>()
            dao.add(schedule)
            call.respond(HttpStatusCode.Created)
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) return@delete call.respond(HttpStatusCode.BadRequest)
            dao.delete(id)
            call.respond(HttpStatusCode.OK)
        }
    }
}
