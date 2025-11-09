package com.example

import io.ktor.server.application.*
import com.example.database.dao.UserDao
import com.example.models.UserRole

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureTemplating()
    configureSockets()
    configureSerialization()
    configureDatabases()
    configureSecurity()
    configureHTTP() // CORS должен быть установлен до routing
    configureLogging()
    configureRouting() // Routing должен быть последним
}
