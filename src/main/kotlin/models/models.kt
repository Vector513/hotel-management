package com.example.models

import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate

data class Room(
    val roomId: Int,
    val roomNumber: Int,
    val floor: Int,
    val type: RoomType,
    val pricePerDay: BigDecimal,
    val phoneNumber: String
) : Serializable

enum class RoomType : Serializable {
    SINGLE,
    DOUBLE,
    TRIPLE
}

data class Client(
    val clientId: Int,
    val passportNumber: String,
    val fullName: String,
    val city: String,
    val checkInDate: LocalDate,
    val daysReserved: Int,
    val roomId: Int
) : Serializable

data class Employee(
    val employeeId: Int,
    val fullName: String,
    val floor: Int
) : Serializable

data class CleaningSchedule(
    val scheduleId: Int,
    val employeeId: Int,
    val floor: Int,
    val dayOfWeek: DayOfWeek
) : Serializable

enum class DayOfWeek : Serializable {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
}

data class Invoice(
    val invoiceId: Int,
    val clientId: Int,
    val totalAmount: BigDecimal,
    val issueDate: LocalDate
) : Serializable

enum class UserRole { ADMIN, WORKER, CLIENT }

data class User(
    val id: Int,
    val username: String,
    val passwordHash: String,
    val role: UserRole
)