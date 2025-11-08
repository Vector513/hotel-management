package com.example.models

import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
enum class RoomType { SINGLE, DOUBLE, TRIPLE }

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
data class CreateRoomRequest(
    val roomNumber: Int,
    val floor: Int,
    val type: RoomType,
    @Serializable(with = BigDecimalSerializer::class)
    val pricePerDay: BigDecimal,
    val phoneNumber: String
)

@Serializable
data class UpdateRoomRequest(
    val roomNumber: Int,
    val floor: Int,
    val type: RoomType,
    @Serializable(with = BigDecimalSerializer::class)
    val pricePerDay: BigDecimal,
    val phoneNumber: String
) {
    fun toRoom(roomId: Int): Room = Room(
        roomId = roomId,
        roomNumber = roomNumber,
        floor = floor,
        type = type,
        pricePerDay = pricePerDay,
        phoneNumber = phoneNumber
    )
}

@Serializable
data class FreeRoomsResponse(
    val totalRooms: Int,
    val freeRoomsCount: Int,
    val freeRooms: List<Room>
)

