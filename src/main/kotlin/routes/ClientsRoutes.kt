package routes

import database.dao.ClientDao
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import models.Client

fun Route.clientsRoutes() {
    val clientDao = ClientDao()

    route("/clients") {
        get {
            call.respond(clientDao.getAll())
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            val client = id?.let { clientDao.findById(it) }
            if (client != null) call.respond(client)
            else call.respond(HttpStatusCode.NotFound)
        }

        post {
            val client = call.receive<Client>()
            clientDao.add(client)
            call.respond(HttpStatusCode.Created)
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@delete
            }
            clientDao.delete(id)
            call.respond(HttpStatusCode.OK)
        }
    }
}
