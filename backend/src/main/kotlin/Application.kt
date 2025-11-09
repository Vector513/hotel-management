package com.example

import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()
    configureDatabases()
    configureSecurity()
    configureHTTP() // CORS должен быть установлен до routing
    configureLogging()
    configureRouting() // Routing должен быть последним
}
