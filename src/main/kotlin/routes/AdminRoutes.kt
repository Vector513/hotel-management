package com.example.routes

import com.example.models.*
import com.example.database.dao.*
import com.example.auth.requireRole
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.adminRoutes() {
    val clientDao = ClientDao()
    val employeeDao = EmployeeDao()
    val scheduleDao = CleaningScheduleDao()
    val invoiceDao = InvoiceDao()
    val roomDao = RoomDao()
    val userDao = UserDao()

    authenticate("auth-jwt") {
        route("/admin") {

            /** ---------------- Clients ---------------- */
            post("/clients") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@post

                val client = try { call.receive<Client>() } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid client data")
                    return@post
                }

                val id = clientDao.add(client)
                call.respond(HttpStatusCode.Created, "Client added with id $id")
            }

            put("/clients/{id}") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@put

                val clientId = call.parameters["id"]?.toIntOrNull()
                val updated = try { call.receive<Client>() } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid client data")
                    return@put
                }

                val success = if (clientId != null) clientDao.update(clientId, updated) else false
                if (success) call.respond(HttpStatusCode.OK, "Client updated")
                else call.respond(HttpStatusCode.NotFound, "Client not found")
            }

            delete("/clients/{id}") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@delete

                val clientId = call.parameters["id"]?.toIntOrNull()
                if (clientId == null || !clientDao.delete(clientId)) {
                    call.respond(HttpStatusCode.NotFound, "Client not found")
                    return@delete
                }
                call.respond(HttpStatusCode.OK, "Client deleted")
            }

            /** ---------------- Employees ---------------- */
            post("/employees") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@post

                val employee = try { call.receive<Employee>() } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid employee data")
                    return@post
                }

                val id = employeeDao.add(employee)
                call.respond(HttpStatusCode.Created, "Employee added with id $id")
            }

            put("/employees/{id}") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@put

                val employeeId = call.parameters["id"]?.toIntOrNull()
                val updated = try { call.receive<Employee>() } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid employee data")
                    return@put
                }

                val success = if (employeeId != null) employeeDao.update(employeeId, updated) else false
                if (success) call.respond(HttpStatusCode.OK, "Employee updated")
                else call.respond(HttpStatusCode.NotFound, "Employee not found")
            }

            delete("/employees/{id}") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@delete

                val employeeId = call.parameters["id"]?.toIntOrNull() ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Invalid employee ID")
                    return@delete
                }

                // удаляем расписания сотрудника
                scheduleDao.findByEmployee(employeeId).forEach { scheduleDao.delete(it.scheduleId) }

                if (!employeeDao.delete(employeeId)) {
                    call.respond(HttpStatusCode.NotFound, "Employee not found")
                    return@delete
                }

                call.respond(HttpStatusCode.OK, "Employee deleted along with schedules")
            }

            /** ---------------- CleaningSchedule ---------------- */
            post("/schedules") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@post

                val schedule = try { call.receive<CleaningSchedule>() } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid schedule data")
                    return@post
                }

                val id = scheduleDao.add(schedule)
                call.respond(HttpStatusCode.Created, "Schedule added with id $id")
            }

            put("/schedules/{id}") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@put

                val scheduleId = call.parameters["id"]?.toIntOrNull()
                val updated = try { call.receive<CleaningSchedule>() } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid schedule data")
                    return@put
                }

                val success = if (scheduleId != null) scheduleDao.update(scheduleId, updated) else false
                if (success) call.respond(HttpStatusCode.OK, "Schedule updated")
                else call.respond(HttpStatusCode.NotFound, "Schedule not found")
            }

            delete("/schedules/{id}") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@delete

                val scheduleId = call.parameters["id"]?.toIntOrNull()
                if (scheduleId == null || !scheduleDao.delete(scheduleId)) {
                    call.respond(HttpStatusCode.NotFound, "Schedule not found")
                    return@delete
                }

                call.respond(HttpStatusCode.OK, "Schedule deleted")
            }

            /** ---------------- Invoices ---------------- */
            post("/invoices") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@post

                val invoice = try { call.receive<Invoice>() } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid invoice data")
                    return@post
                }

                val id = invoiceDao.add(invoice)
                call.respond(HttpStatusCode.Created, "Invoice added with id $id")
            }

            put("/invoices/{id}") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@put

                val invoiceId = call.parameters["id"]?.toIntOrNull()
                val updated = try { call.receive<Invoice>() } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid invoice data")
                    return@put
                }

                val success = if (invoiceId != null) invoiceDao.update(invoiceId, updated) else false
                if (success) call.respond(HttpStatusCode.OK, "Invoice updated")
                else call.respond(HttpStatusCode.NotFound, "Invoice not found")
            }

            delete("/invoices/{id}") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@delete

                val invoiceId = call.parameters["id"]?.toIntOrNull()
                if (invoiceId == null || !invoiceDao.delete(invoiceId)) {
                    call.respond(HttpStatusCode.NotFound, "Invoice not found")
                    return@delete
                }

                call.respond(HttpStatusCode.OK, "Invoice deleted")
            }

            /** ---------------- Rooms ---------------- */
            post("/rooms") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@post

                val room = try { call.receive<Room>() } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid room data")
                    return@post
                }

                val id = roomDao.add(room)
                call.respond(HttpStatusCode.Created, "Room added with id $id")
            }

            put("/rooms/{id}") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@put

                val roomId = call.parameters["id"]?.toIntOrNull()
                val updated = try { call.receive<Room>() } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid room data")
                    return@put
                }

                val success = if (roomId != null) roomDao.update(roomId, updated) else false
                if (success) call.respond(HttpStatusCode.OK, "Room updated")
                else call.respond(HttpStatusCode.NotFound, "Room not found")
            }

            delete("/rooms/{id}") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@delete

                val roomId = call.parameters["id"]?.toIntOrNull()
                if (roomId == null || !roomDao.delete(roomId)) {
                    call.respond(HttpStatusCode.NotFound, "Room not found")
                    return@delete
                }

                call.respond(HttpStatusCode.OK, "Room deleted")
            }

            /** ---------------- Users ---------------- */
            post("/users") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@post

                val user = try { call.receive<User>() } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid user data")
                    return@post
                }

                if (user.role == UserRole.ADMIN) {
                    call.respond(HttpStatusCode.Forbidden, "Cannot create another admin")
                    return@post
                }

                val created = userDao.create(user.username, user.passwordHash, user.role)
                if (created != null) call.respond(HttpStatusCode.Created, "User created: ${created.username}")
                else call.respond(HttpStatusCode.InternalServerError, "Failed to create user")
            }

            delete("/users/{id}") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@delete

                val userId = call.parameters["id"]?.toIntOrNull()
                val user = if (userId != null) userDao.findById(userId) else null

                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, "User not found")
                    return@delete
                }

                if (user.role == UserRole.ADMIN) {
                    call.respond(HttpStatusCode.Forbidden, "Cannot delete other admins")
                    return@delete
                }

                userDao.delete(userId!!)
                call.respond(HttpStatusCode.OK, "User deleted")
            }
        }
    }
}

