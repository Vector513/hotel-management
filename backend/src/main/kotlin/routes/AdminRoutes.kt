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

    // Вспомогательная функция для обновления статуса клиентов и создания счетов
    suspend fun ApplicationCall.updateExpiredClientsAndCreateInvoices() {
        val expiredClients = clientDao.getExpiredClients()
        if (expiredClients.isNotEmpty()) {
            val updatedCount = clientDao.updateExpiredResidents()
            application.log.info("Updated $updatedCount clients to non-resident status")
            
            // Создаем счета для клиентов, у которых их еще нет
            expiredClients.forEach { client ->
                val existingInvoices = invoiceDao.findByClient(client.clientId)
                val today = java.time.LocalDate.now()
                val hasRecentInvoice = existingInvoices.any {
                    val daysSinceInvoice = java.time.temporal.ChronoUnit.DAYS.between(it.issueDate, today)
                    daysSinceInvoice <= 30
                }
                
                if (!hasRecentInvoice) {
                    val room = roomDao.findById(client.roomId ?: return@forEach)
                    if (room != null) {
                        val total = room.pricePerDay.multiply(BigDecimal(client.daysReserved))
                        val invoice = Invoice(
                            invoiceId = 0,
                            clientId = client.clientId,
                            totalAmount = total,
                            issueDate = today
                        )
                        try {
                            val invoiceId = invoiceDao.add(invoice)
                            application.log.info("Auto-created invoice $invoiceId for expired client ${client.clientId}")
                        } catch (e: Exception) {
                            application.log.error("Error auto-creating invoice for client ${client.clientId}: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    authenticate("auth-jwt") {
        route("/admin") {

            /** ---------------- Clients ---------------- */
            get("/clients") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@get

                // Обновляем статус клиентов и создаем счета для выехавших
                call.updateExpiredClientsAndCreateInvoices()

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

                // Обновляем статус клиентов и создаем счета для выехавших
                call.updateExpiredClientsAndCreateInvoices()

                // Проверяем лимит мест в номере
                val room = roomDao.findById(request.roomId)
                if (room == null) {
                    call.respond(HttpStatusCode.BadRequest, "Room not found")
                    return@post
                }

                // Получаем текущее количество жильцов в комнате
                val currentResidents = clientDao.getAll().count { 
                    it.roomId == request.roomId && it.isResident 
                }

                // Проверяем, не превышает ли лимит
                val maxCapacity = room.type.getMaxCapacity()
                if (currentResidents >= maxCapacity) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        "Room ${room.roomNumber} (${room.type}) is full. Maximum capacity: $maxCapacity, current residents: $currentResidents"
                    )
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

                // Обновляем статус клиентов и создаем счета для выехавших
                call.updateExpiredClientsAndCreateInvoices()

                // Получаем текущего клиента для проверки старой комнаты
                val existingClient = clientDao.findById(clientId)
                if (existingClient == null) {
                    call.respond(HttpStatusCode.NotFound, "Client not found")
                    return@put
                }

                val updated = try { call.receive<UpdateClientRequest>().toClient(clientId) } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid client data")
                    return@put
                }

                // Проверяем лимит мест в номере, если комната изменилась или клиент становится жильцом
                if (updated.roomId != existingClient.roomId || (!existingClient.isResident && updated.isResident == true)) {
                    val room = roomDao.findById(updated.roomId)
                    if (room == null) {
                        call.respond(HttpStatusCode.BadRequest, "Room not found")
                        return@put
                    }

                    // Получаем текущее количество жильцов в новой комнате
                    // Если клиент уже живет в этой комнате, не считаем его дважды
                    val currentResidents = clientDao.getAll().count { 
                        it.roomId == updated.roomId && it.isResident && it.clientId != clientId
                    }

                    // Если клиент становится жильцом, добавляем его к счету
                    val willBeResident = updated.isResident ?: true
                    val futureResidents = currentResidents + if (willBeResident) 1 else 0

                    // Проверяем, не превышает ли лимит
                    val maxCapacity = room.type.getMaxCapacity()
                    if (futureResidents > maxCapacity) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            "Room ${room.roomNumber} (${room.type}) is full. Maximum capacity: $maxCapacity, would have: $futureResidents"
                        )
                        return@put
                    }
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
                if (clientId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid client ID")
                    return@delete
                }

                // Сначала удаляем пользователя
                val usersDeleted = userDao.deleteByClientId(clientId)
                if (usersDeleted == 0) {
                    println("Warning: no users found for clientId $clientId")
                }

                // Потом удаляем клиента
                val clientDeleted = clientDao.delete(clientId)
                if (!clientDeleted) {
                    call.respond(HttpStatusCode.NotFound, "Client not found")
                    return@delete
                }

                // Удаляем все счета клиента
                invoiceDao.findByClient(clientId).forEach { invoiceDao.delete(it.invoiceId) }

                call.respond(HttpStatusCode.OK, "Client, related user, and invoices deleted successfully")
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

                // Обновляем статус клиентов, у которых истек период проживания
                clientDao.updateExpiredResidents()

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

                // Обновляем статус клиентов и создаем счета для выехавших
                call.updateExpiredClientsAndCreateInvoices()

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

                // Получаем клиента
                val client = clientDao.findById(request.clientId)
                if (client == null) {
                    call.respond(HttpStatusCode.NotFound, "Client not found")
                    return@post
                }

                // Проверяем наличие комнаты
                val roomId = client.roomId
                if (roomId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Client does not have a valid room")
                    return@post
                }

                // Получаем комнату
                val room = roomDao.findById(roomId)
                if (room == null) {
                    call.respond(HttpStatusCode.BadRequest, "Client does not have a valid room")
                    return@post
                }

                // Вычисляем стоимость проживания автоматически
                val total = try {
                    room.pricePerDay.multiply(BigDecimal(client.daysReserved))
                } catch (e: Exception) {
                    application.log.error("Error calculating total: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, "Error calculating total")
                    return@post
                }

                // Используем указанную дату или сегодняшнюю
                val issueDate = request.issueDate ?: java.time.LocalDate.now()

                // Проверяем, не существует ли уже счет за последние 30 дней
                val existingInvoices = invoiceDao.findByClient(request.clientId)
                val hasRecentInvoice = existingInvoices.any {
                    val daysSinceInvoice = java.time.temporal.ChronoUnit.DAYS.between(it.issueDate, issueDate)
                    daysSinceInvoice <= 30
                }

                if (hasRecentInvoice) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        "Invoice already exists for this client. Please check existing invoices."
                    )
                    return@post
                }

                val invoice = Invoice(
                    invoiceId = 0,
                    clientId = request.clientId,
                    totalAmount = total,
                    issueDate = issueDate
                )

                val id = try {
                    invoiceDao.add(invoice)
                } catch (e: Exception) {
                    application.log.error("Error saving invoice: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, "Error creating invoice: ${e.message}")
                    return@post
                }

                application.log.info("Invoice created with id: $id for client: ${request.clientId}")
                call.respond(HttpStatusCode.Created, InvoiceResponse(id, total.toPlainString()))
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

                val clientId = call.parameters["id"]?.toIntOrNull()
                if (clientId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Missing or invalid client ID")
                    return@post
                }

                // Получаем клиента из БД
                val client = clientDao.findById(clientId)
                if (client == null) {
                    call.respond(HttpStatusCode.NotFound, "Client not found")
                    return@post
                }

                // Проверяем наличие комнаты
                val roomId = client.roomId
                if (roomId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Client does not have a valid room")
                    return@post
                }

                // Получаем комнату
                val room = roomDao.findById(roomId)
                if (room == null) {
                    call.respond(HttpStatusCode.BadRequest, "Client does not have a valid room")
                    return@post
                }

                // Проверяем, не существует ли уже счет для этого клиента за текущий период
                val existingInvoices = invoiceDao.findByClient(clientId)
                val today = java.time.LocalDate.now()
                val hasRecentInvoice = existingInvoices.any { 
                    val daysSinceInvoice = java.time.temporal.ChronoUnit.DAYS.between(it.issueDate, today)
                    daysSinceInvoice <= 30 // Проверяем счета за последние 30 дней
                }

                if (hasRecentInvoice) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        "Invoice already exists for this client. Please check existing invoices."
                    )
                    return@post
                }

                // Вычисляем стоимость проживания
                val total = try {
                    room.pricePerDay.multiply(BigDecimal(client.daysReserved))
                } catch (e: Exception) {
                    application.log.error("Error calculating total: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, "Error calculating total")
                    return@post
                }

                // Создаем счет
                val invoice = Invoice(
                    invoiceId = 0,
                    clientId = client.clientId,
                    totalAmount = total,
                    issueDate = today
                )

                val id = try {
                    invoiceDao.add(invoice)
                } catch (e: Exception) {
                    application.log.error("Error saving invoice: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, "Error creating invoice: ${e.message}")
                    return@post
                }

                application.log.info("Invoice created with id: $id for client: $clientId")
                call.respond(HttpStatusCode.Created, InvoiceResponse(id, total.toPlainString()))
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

                val request = try {
                    call.receive<CreateEmployeeRequest>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid employee data")
                    return@post
                }

                // Создаём сотрудника
                val employee = request.toEmployee(0)
                val employeeId = employeeDao.add(employee)

                // Генерируем логин и пароль
                val login = request.fullName.replace(" ", "").lowercase()
                val password = List(8) { ('a'..'z').random() }.joinToString("")

                // Создаём пользователя через UserDao с employeeId
                val user = userDao.create(login, password, UserRole.WORKER, employeeId = employeeId)

                if (user == null) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to create user for employee")
                    return@post
                }

                // Формируем ответ через модель
                call.respond(HttpStatusCode.Created, EmployeeCreatedResponse(
                    employeeId = employeeId,
                    login = login,
                    password = password
                ))
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

                // Удаляем пользователя сначала
                userDao.deleteByEmployeeId(employeeId)

                // Удаляем все расписания сотрудника
                scheduleDao.findByEmployee(employeeId).forEach { scheduleDao.delete(it.scheduleId) }

                // Удаляем сотрудника
                val deletedEmployee = employeeDao.delete(employeeId)
                if (!deletedEmployee) {
                    call.respond(HttpStatusCode.NotFound, "Employee not found")
                    return@delete
                }

                call.respond(HttpStatusCode.OK, "Employee deleted along with schedules and user record")
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
                val principal = call.principal<JWTPrincipal>() ?: return@get
                if (!call.requireRole(UserRole.ADMIN, principal)) return@get
                call.respond(HttpStatusCode.OK, userDao.getAll())
            }

            get("/users/{id}") {
                val principal = call.principal<JWTPrincipal>() ?: return@get
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
                val principal = call.principal<JWTPrincipal>() ?: return@post
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
                if (created != null) call.respond(HttpStatusCode.Created, created)
                else call.respond(HttpStatusCode.InternalServerError, "Failed to create user")
            }

            put("/users/{id}") {
                val principal = call.principal<JWTPrincipal>() ?: return@put
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
                val principal = call.principal<JWTPrincipal>() ?: return@delete
                if (!call.requireRole(UserRole.ADMIN, principal)) return@delete

                val userId = call.parameters["id"]?.toIntOrNull() ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Invalid user ID")
                    return@delete
                }

                val user = userDao.findById(userId) ?: run {
                    call.respond(HttpStatusCode.NotFound, "User not found")
                    return@delete
                }

                if (user.role == UserRole.ADMIN) {
                    call.respond(HttpStatusCode.Forbidden, "Cannot delete other admins")
                    return@delete
                }

                when (user.role) {
                    UserRole.CLIENT -> {
                        user.clientId?.let { clientId ->
                            // Сначала удаляем все счета
                            invoiceDao.findByClient(clientId).forEach { invoiceDao.delete(it.invoiceId) }
                            // Потом удаляем запись пользователя
                            userDao.deleteByClientId(clientId)
                            // И только потом удаляем клиента
                            clientDao.delete(clientId)
                        }
                    }

                    UserRole.WORKER -> {
                        user.employeeId?.let { employeeId ->
                            // Удаляем все расписания
                            scheduleDao.findByEmployee(employeeId).forEach { scheduleDao.delete(it.scheduleId) }
                            // Удаляем запись пользователя
                            userDao.deleteByEmployeeId(employeeId)
                            // И только потом удаляем сотрудника
                            employeeDao.delete(employeeId)
                        } ?: run {
                            call.respond(HttpStatusCode.NotFound, "Employee not found for this user")
                            return@delete
                        }
                    }

                    else -> userDao.delete(userId)
                }

                call.respond(HttpStatusCode.OK, "User and related data deleted successfully")
            }

            /** ---------------- Quarterly Report ---------------- */
            get("/reports/quarterly") {
                val principal = call.principal<JWTPrincipal>()
                if (!call.requireRole(UserRole.ADMIN, principal)) return@get

                // Вычисляем период последнего квартала
                val today = java.time.LocalDate.now()
                val currentMonth = today.monthValue
                val currentYear = today.year

                // Определяем начало и конец последнего квартала
                val quarterStartMonth = when {
                    currentMonth in 1..3 -> 10 // Октябрь предыдущего года
                    currentMonth in 4..6 -> 1  // Январь текущего года
                    currentMonth in 7..9 -> 4  // Апрель текущего года
                    else -> 7 // Июль текущего года
                }

                val quarterStartYear = when {
                    currentMonth in 1..3 -> currentYear - 1
                    else -> currentYear
                }

                val quarterEndMonth = when {
                    currentMonth in 1..3 -> 12 // Декабрь предыдущего года
                    currentMonth in 4..6 -> 3  // Март текущего года
                    currentMonth in 7..9 -> 6  // Июнь текущего года
                    else -> 9 // Сентябрь текущего года
                }

                val quarterEndYear = when {
                    currentMonth in 1..3 -> currentYear - 1
                    else -> currentYear
                }

                val periodStart = java.time.LocalDate.of(quarterStartYear, quarterStartMonth, 1)
                val periodEnd = java.time.LocalDate.of(quarterEndYear, quarterEndMonth, 1)
                    .with(java.time.temporal.TemporalAdjusters.lastDayOfMonth())

                val totalDaysInQuarter = java.time.temporal.ChronoUnit.DAYS.between(periodStart, periodEnd).toInt() + 1

                // Получаем всех клиентов, которые были в этом периоде
                val allClients = clientDao.getAll()
                val clientsInPeriod = allClients.filter { client ->
                    val checkIn = client.checkInDate
                    val checkOut = checkIn.plusDays(client.daysReserved.toLong())
                    // Клиент был в отеле, если его период пересекается с кварталом
                    checkIn <= periodEnd && checkOut >= periodStart
                }

                val totalClients = clientsInPeriod.distinctBy { it.clientId }.size

                // Вычисляем доход из счетов за этот период
                val allInvoices = invoiceDao.getAll()
                val invoicesInPeriod = allInvoices.filter { invoice ->
                    invoice.issueDate >= periodStart && invoice.issueDate <= periodEnd
                }
                val totalRevenue = invoicesInPeriod.fold(BigDecimal.ZERO) { sum, invoice ->
                    sum.add(invoice.totalAmount)
                }

                // Вычисляем занятость номеров
                val allRooms = roomDao.getAll()
                val roomOccupancy = allRooms.map { room ->
                    // Находим всех клиентов, которые жили в этом номере в этом периоде
                    val clientsInRoom = clientsInPeriod.filter { it.roomId == room.roomId }

                    // Вычисляем занятые дни
                    var occupiedDays = 0
                    clientsInRoom.forEach { client ->
                        val checkIn = if (client.checkInDate < periodStart) periodStart else client.checkInDate
                        val checkOut = client.checkInDate.plusDays(client.daysReserved.toLong())
                        val actualCheckOut = if (checkOut > periodEnd) periodEnd else checkOut

                        if (actualCheckOut >= checkIn) {
                            val days = java.time.temporal.ChronoUnit.DAYS.between(checkIn, actualCheckOut).toInt() + 1
                            occupiedDays += days
                        }
                    }

                    // Ограничиваем занятые дни общим количеством дней в квартале
                    occupiedDays = minOf(occupiedDays, totalDaysInQuarter)
                    val freeDays = totalDaysInQuarter - occupiedDays

                    com.example.models.RoomOccupancyInfo(
                        roomId = room.roomId,
                        roomNumber = room.roomNumber,
                        floor = room.floor,
                        type = room.type,
                        occupiedDays = occupiedDays,
                        freeDays = freeDays,
                        totalDays = totalDaysInQuarter
                    )
                }

                val report = com.example.models.QuarterlyReport(
                    periodStart = periodStart.toString(),
                    periodEnd = periodEnd.toString(),
                    totalClients = totalClients,
                    totalRevenue = totalRevenue.toPlainString(),
                    roomOccupancy = roomOccupancy
                )

                call.respond(HttpStatusCode.OK, report)
            }
        }
    }
}



