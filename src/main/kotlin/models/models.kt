package com.example.models

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeString(value.toPlainString())
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        return BigDecimal(decoder.decodeString())
    }
}

object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(value.toString()) // например "2025-11-08"
    }

    override fun deserialize(decoder: Decoder): LocalDate {
        return LocalDate.parse(decoder.decodeString())
    }
}

@Serializable
data class Room(
    val roomId: Int,
    val roomNumber: Int,
    val floor: Int,
    val type: RoomType,
    @Serializable(with = BigDecimalSerializer::class)
    val pricePerDay: BigDecimal,
    val phoneNumber: String
)

@Serializable
enum class RoomType {
    SINGLE,
    DOUBLE,
    TRIPLE
}

@Serializable
data class Client(
    val clientId: Int,
    val passportNumber: String,
    val fullName: String,
    val city: String,
    @Serializable(with = LocalDateSerializer::class)
    val checkInDate: LocalDate,
    val daysReserved: Int,
    val roomId: Int
)

@Serializable
data class Employee(
    val employeeId: Int,
    val fullName: String,
    val floor: Int
)

@Serializable
data class CleaningSchedule(
    val scheduleId: Int,
    val employeeId: Int,
    val floor: Int,
    val dayOfWeek: DayOfWeek
)

@Serializable
enum class DayOfWeek {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
}

@Serializable
data class Invoice(
    val invoiceId: Int,
    val clientId: Int,
    @Serializable(with = BigDecimalSerializer::class)
    val totalAmount: BigDecimal,
    @Serializable(with = LocalDateSerializer::class)
    val issueDate: LocalDate
)
@Serializable
enum class UserRole { ADMIN, WORKER, CLIENT }

@Serializable
data class User(
    val id: Int,
    val username: String,
    val passwordHash: String,
    val role: UserRole
)