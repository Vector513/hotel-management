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
import java.math.BigDecimal

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
            get("/clients") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@get

                val clients = clientDao.getAll()
                call.respond(HttpStatusCode.OK, clients)
            }

            get("/clients/{id}") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@get

                val clientId = call.parameters["id"]?.toIntOrNull() ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Missing client ID")
                    return@get
                }

                val client = clientDao.findById(clientId)
                if (client != null) call.respond(HttpStatusCode.OK, client)
                else call.respond(HttpStatusCode.NotFound, "Client not found")
            }

            get("/clients/from") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@get

                val city = call.request.queryParameters["city"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Missing city name")
                    return@get
                }

                val clients = clientDao.getAll().filter {
                    it.city.equals(city, ignoreCase = true)
                }

                call.respond(HttpStatusCode.OK, clients)
            }

            get("/clients/{id}/cleaner") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@get

                val clientId = call.parameters["id"]?.toIntOrNull() ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Missing client ID")
                    return@get
                }

                val dayOfWeek = call.request.queryParameters["dayOfWeek"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Missing day of week")
                    return@get
                }

                val client = clientDao.findById(clientId)
                if (client == null || client.roomId == null) {
                    call.respond(HttpStatusCode.NotFound, "Client not found or not assigned to a room")
                    return@get
                }

                val schedule = scheduleDao.getAll().find { schedule ->
                    schedule.dayOfWeek.name.equals(dayOfWeek, ignoreCase = true) &&
                            schedule.floor == roomDao.findById(client.roomId)!!.floor
                }

                if (schedule == null) {
                    call.respond(HttpStatusCode.NotFound, "No cleaning schedule found for that day")
                    return@get
                }

                val employee = employeeDao.findById(schedule.employeeId)
                if (employee == null) {
                    call.respond(HttpStatusCode.NotFound, "Employee not found")
                    return@get
                }

                call.respond(HttpStatusCode.OK, mapOf("employeeName" to employee.fullName))
            }

            post("/clients") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@post

                val request = try { call.receive<CreateClientRequest>() } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid client data")
                    return@post
                }

                val client = Client(
                    clientId = 0,
                    passportNumber = request.passportNumber,
                    fullName = request.fullName,
                    city = request.city,
                    checkInDate = request.checkInDate,
                    daysReserved = request.daysReserved,
                    roomId = request.roomId,
                    isResident = true
                )

                val clientId = clientDao.add(client)

                // Создаем пользователя для клиента
                val charset = ('a'..'z') + ('0'..'9')
                val password = (1..8).map { charset.random() }.joinToString("")
                val username = "client$clientId"
                userDao.createForClient(clientId, username, password)

//                call.respond(HttpStatusCode.Created, client)
                call.respond(HttpStatusCode.Created, ClientCreatedResponse(
                    clientId = clientId,
                    login = username,
                    password = password
                ))
            }

            put("/clients/{id}") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@put

                val clientId = call.parameters["id"]?.toIntOrNull() ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Missing client ID")
                    return@put
                }

                val updated = try { call.receive<UpdateClientRequest>().toClient(clientId) } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid client data")
                    return@put
                }

                if (clientDao.update(clientId, updated))
                    call.respond(HttpStatusCode.OK, "Client updated")
                else
                    call.respond(HttpStatusCode.NotFound, "Client not found")
            }

            delete("/clients/{id}") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@delete

                val clientId = call.parameters["id"]?.toIntOrNull()
                if (clientId == null || !clientDao.delete(clientId)) {
                    call.respond(HttpStatusCode.NotFound, "Client not found")
                    return@delete
                }

                // Удаляем связанного пользователя
                userDao.deleteByClientId(clientId)

                call.respond(HttpStatusCode.OK, "Client deleted and user removed")
            }

            /** ---------------- Rooms ---------------- */
            get("/rooms") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@get

                call.respond(HttpStatusCode.OK, roomDao.getAll())
            }

            get("/rooms/{id}") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@get

                val roomId = call.parameters["id"]?.toIntOrNull() ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Missing room ID")
                    return@get
                }

                val room = roomDao.findById(roomId)
                if (room != null) call.respond(HttpStatusCode.OK, room)
                else call.respond(HttpStatusCode.NotFound, "Room not found")
            }

            get("/rooms/free") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@get

                val allRooms = roomDao.getAll()
                val occupiedRoomIds = clientDao.getAll().filter { it.isResident }.mapNotNull { it.roomId }.toSet()
                val freeRooms = allRooms.filter { it.roomId !in occupiedRoomIds }

                call.respond(
                    HttpStatusCode.OK,
                    FreeRoomsResponse(
                        totalRooms = allRooms.size,
                        freeRoomsCount = freeRooms.size,
                        freeRooms = freeRooms
                    )
                )
            }

            post("/rooms") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@post

                val request = try { call.receive<CreateRoomRequest>() } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid room data")
                    return@post
                }

                val room = Room(
                    roomId = 0,
                    roomNumber = request.roomNumber,
                    floor = request.floor,
                    type = request.type,
                    pricePerDay = request.pricePerDay,
                    phoneNumber = request.phoneNumber
                )

                val id = roomDao.add(room)
                call.respond(HttpStatusCode.Created, mapOf("roomId" to id))
            }

            put("/rooms/{id}") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@put

                val roomId = call.parameters["id"]?.toIntOrNull() ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Missing room ID")
                    return@put
                }

                val updated = try { call.receive<UpdateRoomRequest>().toRoom(roomId) } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid room data")
                    return@put
                }

                if (roomDao.update(roomId, updated))
                    call.respond(HttpStatusCode.OK, "Room updated")
                else
                    call.respond(HttpStatusCode.NotFound, "Room not found")
            }

            delete("/rooms/{id}") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@delete

                val roomId = call.parameters["id"]?.toIntOrNull()
                if (roomId == null || !roomDao.delete(roomId)) {
                    call.respond(HttpStatusCode.NotFound, "Room not found")
                    return@delete
                }

                // Делаем всех клиентов в комнате не жильцами
                clientDao.setResidentsByRoom(roomId, false)

                call.respond(HttpStatusCode.OK, "Room deleted and residents updated")
            }

            get("/rooms/{Id}/residents") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@get

                val roomId = call.parameters["Id"]?.toIntOrNull() ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Missing or invalid room ID")
                    return@get
                }

                val residents = clientDao.getAll().filter { it.roomId == roomId && it.isResident }
                call.respond(HttpStatusCode.OK, residents)
            }

            /** ---------------- Invoices ---------------- */
            get("/invoices") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@get

                call.respond(HttpStatusCode.OK, invoiceDao.getAll())
            }

            get("/invoices/{id}") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@get

                val invoiceId = call.parameters["id"]?.toIntOrNull() ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Missing invoice ID")
                    return@get
                }

                val invoice = invoiceDao.findById(invoiceId)
                if (invoice != null) call.respond(HttpStatusCode.OK, invoice)
                else call.respond(HttpStatusCode.NotFound, "Invoice not found")
            }

            post("/invoices") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@post

                val request = try { call.receive<CreateInvoiceRequest>() } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid invoice data")
                    return@post
                }

                val invoice = Invoice(
                    invoiceId = 0,
                    clientId = request.clientId,
                    totalAmount = request.totalAmount,
                    issueDate = request.issueDate
                )

                val id = invoiceDao.add(invoice)
                call.respond(HttpStatusCode.Created, mapOf("invoiceId" to id))
            }

            put("/invoices/{id}") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@put

                val invoiceId = call.parameters["id"]?.toIntOrNull() ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Missing invoice ID")
                    return@put
                }

                val updated = try { call.receive<UpdateInvoiceRequest>().toInvoice(invoiceId) } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid invoice data")
                    return@put
                }

                if (invoiceDao.update(invoiceId, updated))
                    call.respond(HttpStatusCode.OK, "Invoice updated")
                else
                    call.respond(HttpStatusCode.NotFound, "Invoice not found")
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

            post("/clients/{id}/invoice") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@post

                // Получаем clientId и логируем
                val clientId = call.parameters["id"]?.toIntOrNull()
                if (clientId == null) {
                    println("Missing or invalid client ID")
                    call.respond(HttpStatusCode.BadRequest, "Missing or invalid client ID")
                    return@post
                }
                println("Received clientId: $clientId")

                // Получаем клиента из БД
                val client = clientDao.findById(clientId)
                if (client == null) {
                    println("Client not found for id: $clientId")
                    call.respond(HttpStatusCode.NotFound, "Client not found")
                    return@post
                }
                println("Found client: $client")

                // Проверяем наличие комнаты
                val roomId = client.roomId
                if (roomId == null) {
                    println("Client does not have a room assigned")
                    call.respond(HttpStatusCode.BadRequest, "Client does not have a valid room")
                    return@post
                }

                // Получаем комнату
                val room = roomDao.findById(roomId)
                if (room == null) {
                    println("Room not found for roomId: $roomId")
                    call.respond(HttpStatusCode.BadRequest, "Client does not have a valid room")
                    return@post
                }
                println("Found room: $room")

                // Вычисляем стоимость проживания
                val total = try {
                    room.pricePerDay.multiply(BigDecimal(client.daysReserved))
                } catch (e: Exception) {
                    println("Error calculating total: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, "Error calculating total")
                    return@post
                }
                println("Calculated total: $total")

                // Создаем счет
                val invoice = Invoice(
                    invoiceId = 0,
                    clientId = client.clientId,
                    totalAmount = total,
                    issueDate = java.time.LocalDate.now()
                )

                val id = try {
                    invoiceDao.add(invoice)
                } catch (e: Exception) {
                    println("Error saving invoice: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, "Error creating invoice")
                    return@post
                }

                println("Invoice created with id: $id")
                call.respond(HttpStatusCode.Created, mapOf("invoiceId" to id, "amount" to total))
            }



            /** ---------------- Employees ---------------- */
            get("/employees") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@get

                call.respond(HttpStatusCode.OK, employeeDao.getAll())
            }

            get("/employees/{id}") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@get

                val employeeId = call.parameters["id"]?.toIntOrNull() ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Missing employee ID")
                    return@get
                }

                val employee = employeeDao.findById(employeeId)
                if (employee != null) call.respond(HttpStatusCode.OK, employee)
                else call.respond(HttpStatusCode.NotFound, "Employee not found")
            }

            post("/employees") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@post

                val request = try { call.receive<CreateEmployeeRequest>() } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid employee data")
                    return@post
                }

                val employee = Employee(
                    employeeId = 0,
                    fullName = request.fullName,
                    floor = request.floor
                )

                val id = employeeDao.add(employee)
                call.respond(HttpStatusCode.Created, mapOf("employeeId" to id))
            }

            put("/employees/{id}") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@put

                val employeeId = call.parameters["id"]?.toIntOrNull() ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Missing employee ID")
                    return@put
                }

                val updated = try { call.receive<UpdateEmployeeRequest>().toEmployee(employeeId) } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid employee data")
                    return@put
                }

                if (employeeDao.update(employeeId, updated))
                    call.respond(HttpStatusCode.OK, "Employee updated")
                else
                    call.respond(HttpStatusCode.NotFound, "Employee not found")
            }

            delete("/employees/{id}") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@delete

                val employeeId = call.parameters["id"]?.toIntOrNull() ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Invalid employee ID")
                    return@delete
                }

                scheduleDao.findByEmployee(employeeId).forEach { scheduleDao.delete(it.scheduleId) }

                if (!employeeDao.delete(employeeId)) {
                    call.respond(HttpStatusCode.NotFound, "Employee not found")
                    return@delete
                }

                call.respond(HttpStatusCode.OK, "Employee deleted along with schedules")
            }

            /** ---------------- CleaningSchedule ---------------- */
            get("/schedules") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@get

                call.respond(HttpStatusCode.OK, scheduleDao.getAll())
            }

            get("/schedules/{id}") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@get

                val scheduleId = call.parameters["id"]?.toIntOrNull() ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Missing schedule ID")
                    return@get
                }

                val schedule = scheduleDao.findById(scheduleId)
                if (schedule != null) call.respond(HttpStatusCode.OK, schedule)
                else call.respond(HttpStatusCode.NotFound, "Schedule not found")
            }

            post("/schedules") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@post

                val request = try { call.receive<CreateCleaningScheduleRequest>() } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid schedule data")
                    return@post
                }

                val schedule = CleaningSchedule(
                    scheduleId = 0,
                    employeeId = request.employeeId,
                    floor = request.floor,
                    dayOfWeek = request.dayOfWeek
                )

                val id = scheduleDao.add(schedule)
                call.respond(HttpStatusCode.Created, mapOf("scheduleId" to id))
            }

            put("/schedules/{id}") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@put

                val scheduleId = call.parameters["id"]?.toIntOrNull() ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Missing schedule ID")
                    return@put
                }

                val updated = try { call.receive<UpdateCleaningScheduleRequest>().toSchedule(scheduleId) } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid schedule data")
                    return@put
                }

                if (scheduleDao.update(scheduleId, updated))
                    call.respond(HttpStatusCode.OK, "Schedule updated")
                else
                    call.respond(HttpStatusCode.NotFound, "Schedule not found")
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

            /** ---------------- Users ---------------- */
            get("/users") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@get

                call.respond(HttpStatusCode.OK, userDao.getAll())
            }

            get("/users/{id}") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@get

                val userId = call.parameters["id"]?.toIntOrNull() ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Missing user ID")
                    return@get
                }

                val user = userDao.findById(userId)
                if (user != null) call.respond(HttpStatusCode.OK, user)
                else call.respond(HttpStatusCode.NotFound, "User not found")
            }

            post("/users") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@post

                val request = try { call.receive<CreateUserRequest>() } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid user data")
                    return@post
                }

                if (request.role == UserRole.ADMIN) {
                    call.respond(HttpStatusCode.Forbidden, "Cannot create another admin")
                    return@post
                }

                val created = userDao.create(request.username, request.password, request.role)
                if (created != null) call.respond(HttpStatusCode.Created, mapOf("userId" to created.id))
                else call.respond(HttpStatusCode.InternalServerError, "Failed to create user")
            }

            put("/users/{id}") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@put

                val userId = call.parameters["id"]?.toIntOrNull() ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Missing user ID")
                    return@put
                }

                val updated = try { call.receive<UpdateUserRequest>().toUser(userId) } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid user data")
                    return@put
                }

                if (userDao.update(userId, updated))
                    call.respond(HttpStatusCode.OK, "User updated")
                else
                    call.respond(HttpStatusCode.NotFound, "User not found")
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



