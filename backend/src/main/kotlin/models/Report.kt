package com.example.models

import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class RoomOccupancyInfo(
    val roomId: Int,
    val roomNumber: Int,
    val floor: Int,
    val type: RoomType,
    val occupiedDays: Int,
    val freeDays: Int,
    val totalDays: Int
)

@Serializable
data class QuarterlyReport(
    val periodStart: String,
    val periodEnd: String,
    val totalClients: Int,
    val totalRevenue: String,
    val roomOccupancy: List<RoomOccupancyInfo>
)

