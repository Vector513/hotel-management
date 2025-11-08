package com.example.models

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class Client(
    val clientId: Int,
    val passportNumber: String,
    val fullName: String,
    val city: String,
    @Serializable(with = LocalDateSerializer::class)
    val checkInDate: LocalDate,
    val daysReserved: Int,
    val roomId: Int,
    val isResident: Boolean = true
)

@Serializable
data class CreateClientRequest(
    val passportNumber: String,
    val fullName: String,
    val city: String,
    @Serializable(with = LocalDateSerializer::class)
    val checkInDate: LocalDate,
    val daysReserved: Int,
    val roomId: Int
)

@Serializable
data class ClientCreatedResponse(
    val clientId: Int,
    val login: String,
    val password: String
)

@Serializable
data class UpdateClientRequest(
    val passportNumber: String,
    val fullName: String,
    val city: String,
    @Serializable(with = LocalDateSerializer::class)
    val checkInDate: LocalDate,
    val daysReserved: Int,
    val roomId: Int,
    val isResident: Boolean? = null
) {
    fun toClient(clientId: Int, existingResident: Boolean = true): Client = Client(
        clientId = clientId,
        passportNumber = passportNumber,
        fullName = fullName,
        city = city,
        checkInDate = checkInDate,
        daysReserved = daysReserved,
        roomId = roomId,
        isResident = isResident ?: existingResident
    )
}