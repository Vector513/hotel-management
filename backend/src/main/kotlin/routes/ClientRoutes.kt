package com.example.routes

import com.example.database.dao.InvoiceDao
import com.example.database.dao.ClientDao
import com.example.database.dao.UserDao
import com.example.models.UserRole
import io.ktor.http.*
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.clientRoutes() {
    val invoiceDao = InvoiceDao()
    val clientDao = ClientDao()
    val userDao = UserDao()

    authenticate("auth-jwt") {
        route("/client") {

            get("/invoices") {
                val principal = call.principal<JWTPrincipal>() ?: run {
                    call.respond(HttpStatusCode.Unauthorized, "Missing token")
                    return@get
                }

                val role = principal.getClaim("role", String::class)
                val userId = principal.getClaim("userId", Int::class)

                if (role != UserRole.CLIENT.name || userId == null) {
                    call.respond(HttpStatusCode.Forbidden, "Access denied")
                    return@get
                }

                val user = userDao.findById(userId) ?: run {
                    call.respond(HttpStatusCode.NotFound, "User not found")
                    return@get
                }

                val client = clientDao.findByUser(user) ?: run {
                    call.respond(HttpStatusCode.NotFound, "Client profile not found")
                    return@get
                }

                val invoices = invoiceDao.findByClient(client.clientId)
                call.respond(HttpStatusCode.OK, invoices)
            }
        }
    }
}
