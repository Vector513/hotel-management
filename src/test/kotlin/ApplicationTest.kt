package com.example

import com.example.auth.JwtConfig
import com.example.models.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

class AdminRoutesModelTest {

    private fun generateAdminToken(): String {
        val adminUser = User(id = 1, username = "admin", passwordHash = "dummy", role = UserRole.ADMIN)
        return JwtConfig.generateToken(adminUser)
    }

    @Test
    fun testAddRoom() = testApplication {
        application { module() }
        val token = generateAdminToken()
        val room = Room(
            roomId = 0,
            roomNumber = 101,
            floor = 1,
            type = RoomType.SINGLE,
            pricePerDay = BigDecimal("120.00"),
            phoneNumber = "1234567890"
        )

        val response = client.post("/admin/rooms") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(room) // или body = room
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun testAddClient() = testApplication {
        application { module() }
        val token = generateAdminToken()
        val client = Client(
            clientId = 0,
            passportNumber = "AB1234567",
            fullName = "John Doe",
            city = "TestCity",
            checkInDate = LocalDate.now(),
            daysReserved = 3,
            roomId = 101
        )

        val response = client.post("/admin/clients") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(client)
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun testAddEmployee() = testApplication {
        application { module() }
        val token = generateAdminToken()
        val employee = Employee(
            employeeId = 0,
            fullName = "Jane Smith",
            floor = 2
        )

        val response = client.post("/admin/employees") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(employee)
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun testUpdateCleaningSchedule() = testApplication {
        application { module() }
        val token = generateAdminToken()
        val schedule = CleaningSchedule(
            scheduleId = 1,
            employeeId = 1,
            floor = 2,
            dayOfWeek = DayOfWeek.MONDAY
        )

        val response = client.put("/admin/updateSchedule") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(schedule)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun testAddInvoice() = testApplication {
        application { module() }
        val token = generateAdminToken()
        val invoice = Invoice(
            invoiceId = 0,
            clientId = 1,
            totalAmount = BigDecimal("500.00"),
            issueDate = LocalDate.now()
        )

        val response = client.post("/admin/invoices") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(invoice)
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun testAddUser() = testApplication {
        application { module() }
        val token = generateAdminToken()
        val user = User(
            id = 0,
            username = "worker1",
            passwordHash = "hashedPassword",
            role = UserRole.WORKER
        )

        val response = client.post("/admin/users") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(user)
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun testDeleteClient() = testApplication {
        application { module() }
        val token = generateAdminToken()
        val clientId = 1

        val response = client.delete("/admin/clients/$clientId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun testDeleteEmployee() = testApplication {
        application { module() }
        val token = generateAdminToken()
        val employeeId = 1

        val response = client.delete("/admin/fireEmployee/$employeeId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }
}
