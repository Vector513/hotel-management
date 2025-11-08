package routes

import database.dao.RoomDao
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import models.Room

fun Route.roomsRoutes() {
    val roomDao = RoomDao()

    route("/rooms") {

        get {
            val rooms = roomDao.getAll()
            call.respond(rooms)
        }

//        get("/{id}") {
//            val id = call.parameters["id"]?.toIntOrNull()
//            if (id == null) {
//                call.respond(HttpStatusCode.BadRequest, "Invalid room ID")
//                return@get
//            }
//
//            val room = roomDao.findById(id)
//            if (room != null)
//                call.respond(room)
//            else
//                call.respond(HttpStatusCode.NotFound, "Room not found")
//        }
//
//        post {
//            val room = call.receive<Room>()
//            roomDao.create(room)
//            call.respond(HttpStatusCode.Created, "Room added successfully")
//        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid room ID")
                return@delete
            }
            roomDao.delete(id)
            call.respond(HttpStatusCode.OK, "Room deleted")
        }
    }
}
