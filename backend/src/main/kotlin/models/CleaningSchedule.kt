package com.example.models

import kotlinx.serialization.Serializable

@Serializable
enum class DayOfWeek { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }

@Serializable
data class CleaningSchedule(
    val scheduleId: Int,
    val employeeId: Int,
    val floor: Int,
    val dayOfWeek: DayOfWeek
)

@Serializable
data class CreateCleaningScheduleRequest(
    val employeeId: Int,
    val floor: Int,
    val dayOfWeek: DayOfWeek
)

@Serializable
data class UpdateCleaningScheduleRequest(
    val employeeId: Int,
    val floor: Int,
    val dayOfWeek: DayOfWeek
) {
    fun toSchedule(scheduleId: Int): CleaningSchedule = CleaningSchedule(
        scheduleId = scheduleId,
        employeeId = employeeId,
        floor = floor,
        dayOfWeek = dayOfWeek
    )
}