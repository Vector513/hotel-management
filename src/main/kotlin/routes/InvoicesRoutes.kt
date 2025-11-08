package routes

import database.dao.InvoiceDao
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import models.Invoice

fun Route.invoicesRoutes() {
    val dao = InvoiceDao()

    route("/invoices") {
        get { call.respond(dao.getAll()) }

        get("/{clientId}") {
            val clientId = call.parameters["clientId"]?.toIntOrNull()
            if (clientId == null) return@get call.respond(HttpStatusCode.BadRequest)
            call.respond(dao.findByClient(clientId))
        }

        post {
            val invoice = call.receive<Invoice>()
            dao.add(invoice)
            call.respond(HttpStatusCode.Created)
        }
    }
}
