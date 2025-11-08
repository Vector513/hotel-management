package com.example.routes

import com.example.database.dao.EmployeeDao
import com.example.models.Employee
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*

fun Route.employeesRoutes() {
    val dao = EmployeeDao()

    route("/employees") {
        get { call.respond(dao.getAll()) }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) return@get call.respond(HttpStatusCode.BadRequest)
            val emp = dao.findById(id)
            if (emp != null) call.respond(emp) else call.respond(HttpStatusCode.NotFound)
        }

        post {
            val employee = call.receive<Employee>()
            dao.add(employee)
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
