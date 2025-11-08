package com.example.routes

import com.example.database.dao.InvoiceDao
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

    authenticate("auth-jwt") {
        route("/client") {

            get("/invoices") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()
                val clientId = principal?.payload?.getClaim("clientId")?.asInt()

                if (role != UserRole.CLIENT.name || clientId == null) {
                    call.respond(HttpStatusCode.Forbidden, "Access denied")
                    return@get
                }

                val invoices = invoiceDao.findByClient(clientId)
                call.respond(HttpStatusCode.OK, invoices)
            }
        }
    }
}