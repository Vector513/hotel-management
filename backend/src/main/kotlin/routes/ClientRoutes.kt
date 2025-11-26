package com.example.routes

import com.example.database.dao.InvoiceDao
import com.example.database.dao.ClientDao
import com.example.database.dao.UserDao
import com.example.database.dao.RoomDao
import com.example.models.UserRole
import com.example.models.InvoiceResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.math.BigDecimal

fun Route.clientRoutes() {
    val invoiceDao = InvoiceDao()
    val clientDao = ClientDao()
    UserDao()
    val roomDao = RoomDao()

    authenticate("auth-jwt") {
        route("/client") {

            get("/invoices") {
                val principal = call.principal<JWTPrincipal>() ?: run {
                    call.respond(HttpStatusCode.Unauthorized, "Missing token")
                    return@get
                }

                val role = principal.getClaim("role", String::class)
                val clientId = principal.getClaim("clientId", Int::class)

                if (role != UserRole.CLIENT.name || clientId == null) {
                    call.respond(HttpStatusCode.Forbidden, "Access denied")
                    return@get
                }

                val invoices = invoiceDao.findByClient(clientId)
                call.respond(HttpStatusCode.OK, invoices)
            }

            // Клиент может запросить счет
            post("/requestInvoice") {
                val principal = call.principal<JWTPrincipal>() ?: run {
                    call.respond(HttpStatusCode.Unauthorized, "Missing token")
                    return@post
                }

                val role = principal.getClaim("role", String::class)
                val clientId = principal.getClaim("clientId", Int::class)

                if (role != UserRole.CLIENT.name || clientId == null) {
                    call.respond(HttpStatusCode.Forbidden, "Access denied")
                    return@post
                }

                // Получаем клиента
                val client = clientDao.findById(clientId)
                if (client == null) {
                    call.respond(HttpStatusCode.NotFound, "Client not found")
                    return@post
                }

                // Проверяем наличие комнаты
                val roomId = client.roomId

                // Получаем комнату
                val room = roomDao.findById(roomId)
                if (room == null) {
                    call.respond(HttpStatusCode.BadRequest, "Your room is not found")
                    return@post
                }

                // Проверяем, не существует ли уже счет за последние 30 дней
                val existingInvoices = invoiceDao.findByClient(clientId)
                val today = java.time.LocalDate.now()
                val hasRecentInvoice = existingInvoices.any {
                    val daysSinceInvoice = java.time.temporal.ChronoUnit.DAYS.between(it.issueDate, today)
                    daysSinceInvoice <= 30
                }

                if (hasRecentInvoice) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        "You already have an invoice. Please check your invoices list."
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
                val invoice = com.example.models.Invoice(
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

                application.log.info("Invoice created by client request with id: $id for client: $clientId")
                call.respond(HttpStatusCode.Created, InvoiceResponse(id, total.toPlainString()))
            }
        }
    }
}
