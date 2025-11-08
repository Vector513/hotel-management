package com.example

import com.example.models.*
import com.example.routes.adminRoutes
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.routing.Routing
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlin.test.assertEquals

class AdminRoutesTest {

    private val json = Json { ignoreUnknownKeys = true }

    // Здесь токен администратора для тестов (можно сгенерировать через тестовый метод)
    private val adminToken = "Bearer your_jwt_token_here"

    @Test
    fun testGetClients() = testApplication {
        application {
            install(Routing) {
                adminRoutes() // здесь внутри DSL Route
            }
        }

        val response = client.get("/admin/clients") {
            header(HttpHeaders.Authorization, adminToken)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val clients: List<Client> = json.decodeFromString(response.bodyAsText())
        println("Clients: $clients")
    }

    @Test
    fun testCreateAndDeleteClient() = testApplication {
        application {
            routing {
                adminRoutes() // вот так правильно подключаем
            }
        }

        // Создаем клиента
        val newClientRequest = CreateClientRequest(
            passportNumber = "AB123456",
            fullName = "Test Client",
            city = "TestCity",
            checkInDate = LocalDate.now(),
            daysReserved = 3,
            roomId = 1
        )

        val createResponse = client.post("/admin/clients") {
            header(HttpHeaders.Authorization, adminToken)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CreateClientRequest.serializer(), newClientRequest))
        }

        assertEquals(HttpStatusCode.Created, createResponse.status)
        val createdData = json.decodeFromString<Map<String, String>>(createResponse.bodyAsText())
        val clientId = createdData["clientId"]!!.toInt()

        // Удаляем клиента
        val deleteResponse = client.delete("/admin/clients/$clientId") {
            header(HttpHeaders.Authorization, adminToken)
        }

        assertEquals(HttpStatusCode.OK, deleteResponse.status)
    }

    @Test
    fun testGetRooms() = testApplication {
        application {
            adminRoutes()
        }

        val response = client.get("/admin/rooms") {
            header(HttpHeaders.Authorization, adminToken)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    // Аналогично можно написать тесты для:
    // - rooms/{id}, post rooms, put rooms/{id}, delete rooms/{id}
    // - invoices
    // - employees
    // - schedules
    // - users

}
